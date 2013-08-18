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
}
