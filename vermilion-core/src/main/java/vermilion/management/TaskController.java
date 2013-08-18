package vermilion.management;

import java.util.concurrent.TimeUnit;

import javax.management.MXBean;

import com.google.common.util.concurrent.Service;

/**
 * MXBean type for managing tasks through ScheduledQueingService instances via
 * JMX.
 * 
 * @author andy
 * 
 */
@MXBean
public interface TaskController {

    /**
     * The object name used to register instances of ServiceFactory.
     */
    public static final String OBJECT_NAME = String.format("%s:type=%s",
            TaskController.class.getPackage().getName(),
            TaskController.class.getSimpleName());

    /**
     * Instantiates and starts a Scheduled task.
     * 
     * @param taskName
     *            name of the task.
     * @param taskClassname
     *            class name of the NamedRunnable implementation.
     * @param initialDelay
     *            number of time units to wait before the first execution.
     * @param interval
     *            execution interval.
     * @param timeunit
     *            time units for the execution interval. Value must be one of
     *            the values of TimeUnit.
     * @return an Integer that can be used to identify this task.
     * @see TimeUnit
     */
    public Integer startTask(String taskName, String taskClassname,
            Long initialDelay, Long interval, String timeunit);

    /**
     * Removes the named task.
     * 
     * <p>
     * The given task will not be scheduled for subsequent runs. Per the
     * {@linkplain Service} contract, when a Service is stopped, it cannot be
     * started again.
     * </p>
     * 
     * @param taskName
     *            the name of the task to stop.
     */
    public void stopTask(String taskName);

    /**
     * Removes the task with the given Id.
     * 
     * <p>
     * The given task will not be scheduled for subsequent runs. Per the
     * {@linkplain Service} contract, when a Service is stopped, it cannot be
     * started again.
     * </p>
     * 
     * @param taskId
     *            the Id of the task to stop.
     */
    public void stopTask(Integer taskId);

    /**
     * Queues the task with the given name immediately for execution.
     * 
     * @param taskName
     *            the name of the task to queue.
     */
    public void queueImmediately(String taskName);

    /**
     * Returns an n by 2 array of Objects.
     * 
     * <p>
     * The first array index is the task Id as an {@link Integer}, the second is
     * the task name as a {@link String}.
     * </p>
     */
    public Object[][] tasks();
}
