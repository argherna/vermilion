package vermilion.core;

import java.util.logging.Logger;

/**
 * Example NamedRunnable implementation.
 * 
 * @author andy
 * 
 */
public class SimpleTask implements NamedRunnable {

    private static final Logger taskLogger = Logger.getLogger("taskLogger");

    private String name;

    private Integer executionId;

    private NamedRunnableState state = NamedRunnableState.STOPPED;

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Logs a message at {@linkplain Level.INFO} that the task is executing.
     */
    @Override
    public void run() {
        taskLogger.info(String.format("executing %s", name));
    }

    /**
     * Sets the execution Id.
     * 
     */
    @Override
    public void setExecutionId(Integer executionId) {
        this.executionId = executionId;
    }

    @Override
    public Integer getExecutionId() {
        return executionId;
    }

    @Override
    public NamedRunnableState getState() {
        return state;
    }

    @Override
    public void setState(NamedRunnableState state) {
        this.state = state;
    }
}
