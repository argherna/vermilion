package vermilion.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import vermilion.management.StateTransition;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * A Service that will poll a queue for a {@linkplain NamedRunnable} instance
 * and execute it.
 * 
 * <p>
 * This Service serves as a "consumer" in the producer-consumer model.
 * Internally, it shares a queue with other producer Service instances that will
 * submit tasks to this queue.
 * </p>
 * 
 * <p>
 * Instances of this class can be configured through the constructor to have an
 * Executor pool of a given size. A good value for this is the number of
 * available processors which can be obtained via the {@linkplain Runtime}
 * class.
 * </p>
 * 
 * <p>
 * <strong>Implementation note</strong>: A (static) JDK logger named
 * "taskExecutor" logs certain information at {@linkplain Level.FINER},
 * {@linkplain Level.INFO}, and {@linkplain Level.WARNING}.
 * </p>
 * 
 * @author andy
 * @see Runtime#availableProcessors()
 */
public class TaskExecutionService extends AbstractService {

    private static final Logger taskExecLogger = Logger
            .getLogger("taskExecutor");

    private final BlockingQueue<NamedRunnable> taskQueue;

    private volatile ExecutorService internalExecutor;

    private final ListeningExecutorService taskExecutorService;

    private final Set<ListenableFuture<?>> tasks = Sets.newHashSet();

    private final Lock lock = new ReentrantLock();

    /**
     * Determines which tasks have completed and removes them from the internal
     * Set of tasks.
     */
    private final Runnable tasksEvictor = new Runnable() {

        @Override
        public void run() {
            lock.lock();

            Set<ListenableFuture<?>> toEvict = Sets.newHashSet();

            try {
                for (ListenableFuture<?> task : tasks) {
                    if (task.isDone()) {
                        toEvict.add(task);
                    }
                }

                if (toEvict.size() > 0) {
                    tasks.removeAll(toEvict);
                }
            } finally {
                lock.unlock();
                if (taskExecLogger.isLoggable(Level.FINE)) {
                    taskExecLogger.fine(String.format(
                            "Evicted %d completed tasks.", toEvict.size()));
                }
                toEvict.clear();
            }
        }

    };

    private final ScheduledExecutorService evictorService = Executors
            .newSingleThreadScheduledExecutor();

    /**
     * Construct a new TaskExecutionService.
     * 
     * @param taskQueue
     *            the task queue.
     * @param execPoolSize
     *            size of the pool of {@linkplain Executor}s.
     */
    @Inject
    public TaskExecutionService(BlockingQueue<NamedRunnable> taskQueue,
            StateTransition stateTransition, int execPoolSize) {
        super();
        this.taskQueue = checkNotNull(taskQueue, "Task queue is null.");
        taskExecutorService = MoreExecutors.listeningDecorator(Executors
                .newFixedThreadPool(execPoolSize));
    }

    @Override
    protected void doStart() {
        internalExecutor = Executors.newSingleThreadExecutor();
        addListener(
                Listeners
                        .createExecutorServiceControlListener(internalExecutor),
                MoreExecutors.sameThreadExecutor());
        internalExecutor.execute(new Runnable() {

            @Override
            public void run() {
                lock.lock();
                try {
                    evictorService.scheduleAtFixedRate(tasksEvictor, 10l, 10l,
                            TimeUnit.SECONDS);
                    notifyStarted();
                } catch (Throwable t) {
                    notifyFailed(t);
                    if (t instanceof RuntimeException) {
                        throw t;
                    } else {
                        throw new RuntimeException(t);
                    }
                } finally {
                    lock.unlock();
                }
                doRun();
            }
        });
    }

    void doRun() {
        while (isRunning()) {
            try {
                NamedRunnable task = taskQueue.poll(500, TimeUnit.MILLISECONDS);
                if (task != null) {
                    
                    
                    ListenableFuture<?> taskFuture = taskExecutorService
                            .submit(task);
                    taskFuture.addListener(new Runnable() {
                        public void run() {
                            taskExecLogger.info("Task completed.");
                        }
                    }, MoreExecutors.sameThreadExecutor());
                    lock.lock();
                    try {
                        tasks.add(taskFuture);
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                taskExecLogger
                        .warning("Execution has been interrupted. Propagating interrupt to caller.");
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected void doStop() {

        internalExecutor.execute(new Runnable() {

            @Override
            public void run() {

                try {
                    lock.lock();

                    try {

                        if (tasks.size() > 0) {
                            for (ListenableFuture<?> task : tasks) {
                                if (!task.isDone()) {
                                    task.cancel(false);
                                }
                            }
                        }
                        tasks.clear();

                        if (!taskExecutorService.isShutdown()) {
                            taskExecutorService.shutdown();
                        }

                        if (!evictorService.isShutdown()) {
                            evictorService.shutdown();
                        }

                    } finally {
                        lock.unlock();
                    }
                    notifyStopped();
                } catch (Throwable t) {
                    notifyFailed(t);
                    throw new RuntimeException(t);
                }
            }
        });
    }
}
