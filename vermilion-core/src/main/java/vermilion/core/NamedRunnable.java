package vermilion.core;

/**
 * Adds a name to a Runnable.
 * 
 * @author andy
 * 
 */
public interface NamedRunnable extends Runnable {

    public static enum NamedRunnableState {
        STARTING, STARTED, STOPPING, STOPPED, COMPLETED, FAILED, ABANDONED;
    };

    public String getName();

    public void setName(String name);

    public void setExecutionId(Integer executionId);

    public Integer getExecutionId();

    public NamedRunnableState getState();

    public void setState(NamedRunnableState state);

}
