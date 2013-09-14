package vermilion.management;

import vermilion.core.NamedRunnable;

/**
 * Methods to transition the state of a named task.
 * 
 * <p>
 * State transitions are meant to go in this order:
 * <ol>
 * <li>{@linkplain #starting(NamedRunnable) Starting} which sets the execution
 * id.</li>
 * <li>{@linkplain #started(NamedRunnable) Started}: when given the execution id
 * and name will mark the task as having started.</li>
 * <li>{@linkplain #stopping(NamedRunnable) Stopping}: when given the execution
 * id and name will mark the task as about to stop.</li>
 * <li>{@linkplain #stopped(NamedRunnable) Stopped}: when given the execution id
 * and name will mark the task as stopped.</li>
 * <li>{@linkplain #completed(NamedRunnable) Completed}: when given the
 * execution id and name will mark the task as having completed successfully.</li>
 * <li>{@linkplain #failed(NamedRunnable) Failed}: when given the execution id
 * and name will mark the task has having failed, meaning an unrecoverable error
 * occurred.</li>
 * <li>{@linkplain #abandonded(NamedRunnable) Abandonded}: when given the
 * execution id and name will mark the task has having not completed due to a
 * cancellation of execution either by the system or by an operator.</li>
 * </ol>
 * </p>
 * 
 * @author andy
 * 
 */
public interface StateTransition {

    /**
     * Mark the named task as
     * {@linkplain NamedRunnable.NamedRunnableState#ABANDONED}.
     * 
     * @param task
     *            the task whose state is transitioning.
     */
    void abandonded(NamedRunnable task);

    /**
     * Mark the named task as
     * {@linkplain NamedRunnable.NamedRunnableState#COMPLETED}.
     * 
     * @param task
     *            the task whose state is transitioning.
     */
    void completed(NamedRunnable task);

    /**
     * Mark the named task as
     * {@linkplain NamedRunnable.NamedRunnableState#FAILED}.
     * 
     * @param task
     *            the task whose state is transitioning.
     */
    void failed(NamedRunnable task);

    /**
     * Mark the named task as
     * {@linkplain NamedRunnable.NamedRunnableState#STARTING}.
     * 
     * @param task
     *            the task whose state is transitioning.
     */
    void starting(NamedRunnable task);

    /**
     * Mark the named task as
     * {@linkplain NamedRunnable.NamedRunnableState#STARTED}.
     * 
     * @param task
     *            the task whose state is transitioning.
     */
    void started(NamedRunnable task);

    /**
     * Mark the named task as
     * {@linkplain NamedRunnable.NamedRunnableState#STOPPING}.
     * 
     * @param task
     *            the task whose state is transitioning.
     */
    void stopping(NamedRunnable task);

    /**
     * Mark the named task as
     * {@linkplain NamedRunnable.NamedRunnableState#STOPPED}.
     * 
     * @param task
     *            the task whose state is transitioning.
     */
    void stopped(NamedRunnable task);
}
