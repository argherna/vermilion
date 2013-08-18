package vermilion.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;


import com.google.common.util.concurrent.AbstractScheduledService;

/**
 * Service that queues NamedRunnable tasks per a given Schedule.
 * 
 * <p>
 * This Service serves as a "producer" in the producer-consumer model.
 * Internally, it shares a queue with other producer Service instances that will
 * submit tasks to this queue, and a consumer Service instance that will poll
 * the queue for tasks to execute.
 * </p>
 * 
 * <p>
 * A Schedule cannot be changed once an instance of this class is constructed.
 * If the schedule for a running task must change:
 * <ol>
 * <li>Stop this Service.</li>
 * <li>Instantiate a new instance of this Service with the changed Schedule.</li>
 * <li>Start the new Service instance.</li>
 * </ol>
 * </p>
 * 
 * <p>
 * <strong>Implementation note</strong>: A (static) JDK logger named
 * "queueService" logs certain information at {@linkplain Level.FINER},
 * {@linkplain Level.INFO}, and {@linkplain Level.WARNING}.
 * </p>
 * 
 * @author andy
 * 
 */
public class ScheduledQueuingService extends AbstractScheduledService {

    private static final Logger schdQueueLogger = Logger
            .getLogger("queueService");

    private final BlockingQueue<Runnable> taskQueue;

    private int retryCount = 3;

    private final NamedRunnable task;

    private final Schedule schedule;

    /**
     * Constructs a new ScheduledQueingService.
     * 
     * @param schedule
     *            the Schedule by which the task should be put onto the task
     *            queue.
     * @param task
     *            the task.
     * @param taskQueue
     *            the task queue.
     * 
     * @throws NullPointerException
     *             if any of these parameters are <code>null</code>.
     */
    @Inject
    public ScheduledQueuingService(Schedule schedule, NamedRunnable task,
            BlockingQueue<Runnable> taskQueue) {
        super();
        this.schedule = checkNotNull(schedule, "Schedule can't be null.");
        this.task = checkNotNull(task, "Task can't be null.");
        this.taskQueue = checkNotNull(taskQueue, "Task queue can't be null.");
    }

    @Override
    protected void runOneIteration() throws Exception {
        schdQueueLogger.finer("Queuing task.");
        boolean taskAccepted = false;
        int attempts = 0;
        while (!taskAccepted && attempts < retryCount) {
            taskAccepted = taskQueue.offer(task, 500, TimeUnit.MILLISECONDS);
            if (!taskAccepted) {
                schdQueueLogger
                        .info("Failed to put task into the queue, retrying.");
                attempts++;
            }
        }

        if (!taskAccepted) {
            schdQueueLogger
                    .warning("Task was not queued for execution. Skipping run.");
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(schedule.getInitialDelay(),
                schedule.getInterval(), schedule.getTimeUnit());
    }

    public NamedRunnable getTask() {
        return task;
    }
}
