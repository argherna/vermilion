package vermilion.management;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import vermilion.core.Listeners;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Manages all JMX-based services.
 * 
 * <p>
 * <strong>Implementation note</strong>: A (static) JDK logger
 * {@linkplain Class#getSimpleName() named} for this class logs certain
 * information at {@linkplain Level.WARNING}.
 * </p>
 * 
 * @author andy
 * 
 */
public class JmxService extends AbstractService {

    private static final Logger logger = Logger.getLogger(JmxService.class
            .getName());

    private volatile ExecutorService internalExecutor;

    private final Lock lock = new ReentrantLock();

    private final CountDownLatch latch = new CountDownLatch(1);

    private final BlockingQueue<Runnable> taskQueue;

    private MBeanServer mbs;

    private final List<ObjectInstance> objectInstances;

    /**
     * Constructs a new JmxService instance.
     * 
     * @param taskQueue
     *            the task queue used by the other services.
     */
    @Inject
    public JmxService(BlockingQueue<Runnable> taskQueue) {
        this.taskQueue = taskQueue;
        objectInstances = Lists.newArrayList();
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
                    mbs = ManagementFactory.getPlatformMBeanServer();
                    TaskControllerImpl taskController = new TaskControllerImpl(
                            TaskController.class, taskQueue);
                    ObjectName serviceFactoryBeanName = new ObjectName(
                            TaskController.OBJECT_NAME);
                    objectInstances.add(mbs.registerMBean(taskController,
                            serviceFactoryBeanName));
                    notifyStarted();
                } catch (MalformedObjectNameException
                        | NotCompliantMBeanException
                        | MBeanRegistrationException
                        | InstanceAlreadyExistsException e) {
                    notifyFailed(e);
                } finally {
                    lock.unlock();
                }
                doRun();
            }
        });
    }

    void doRun() {
        if (isRunning()) {
            try {
                // Keeps service running while avoiding busy-wait loop.
                latch.await();
            } catch (InterruptedException e) {
                logger.warning("Execution has been interrupted. Propagating interrupt to caller");
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected void doStop() {
        logger.warning("Unregistering MBeans.");
        for (ObjectInstance objectInstance : objectInstances) {
            try {
                mbs.unregisterMBean(objectInstance.getObjectName());
            } catch (MBeanRegistrationException | InstanceNotFoundException e) {
                logger.warning(String.format(
                        "Problem unregistering MBean %s: %s", objectInstance
                                .getObjectName().toString(), e.getMessage()));
            }
        }
        notifyStopped();
        latch.countDown();
    }

}
