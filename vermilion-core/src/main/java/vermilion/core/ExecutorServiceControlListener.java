package vermilion.core;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;

/**
 * A {@linkplain Service.Listener} implementation that manage an execution
 * service in response to Service notifications.
 * 
 * @author andy
 * 
 */
class ExecutorServiceControlListener implements Listener {

    private final ExecutorService executor;

    /**
     * Construct a new ExecutorServiceControlListener.
     * 
     * @param executor
     *            the {@link ExecutorService} to manage.
     */
    @Inject
    ExecutorServiceControlListener(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * This implementation does nothing.
     */
    @Override
    public void starting() {
    }

    /**
     * This implementation does nothing.
     */
    @Override
    public void running() {
    }

    /**
     * Shuts down the ExecutorService if it is not already terminated.
     * 
     * @see ExecutorService#isTerminated()
     * @see ExecutorService#shutdown()
     */
    @Override
    public void stopping(State from) {
        if (!executor.isTerminated()) {
            executor.shutdown();
        }
    }

    /**
     * Shuts down the ExecutorService if it is not already terminated.
     * 
     * @see ExecutorService#isTerminated()
     * @see ExecutorService#shutdown()
     */
    @Override
    public void terminated(State from) {
        if (!executor.isTerminated()) {
            executor.shutdown();
        }
    }

    /**
     * This implementation does nothing.
     */
    @Override
    public void failed(State from, Throwable failure) {
    }

}
