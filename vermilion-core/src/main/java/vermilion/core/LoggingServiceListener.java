package vermilion.core;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;

/**
 * A {@linkplain Service.Listener} that logs an event.
 * 
 * <p>
 * This Listener uses the JDK logging package. The name of the
 * {@linkplain Logger} for this listener is {@code serviceListener}. Log
 * messages all contain the name of the task whose events are being logged.
 * </p>
 * 
 * @author andy
 * @see Logger
 */
class LoggingServiceListener implements Listener {

    private static final Logger serviceListenerLogger = Logger
            .getLogger("serviceListener");

    private final String taskName;

    /**
     * Constructs a new {@link LoggingServiceListener}.
     * 
     * @param taskName
     *            the name of the task logging events.
     */
    LoggingServiceListener(String taskName) {
        this.taskName = taskName;
    }

    /**
     * Logs a startup message at {@linkplain Level.CONFIG}.
     */
    @Override
    public void starting() {
        serviceListenerLogger.config(String.format("%s \u2192 starting.",
                taskName));
    }

    /**
     * Logs a running message at {@linkplain Level.CONFIG}.
     */
    @Override
    public void running() {
        serviceListenerLogger.config(String.format("%s \u2192 running.",
                taskName));
    }

    /**
     * Logs a stopping message at {@linkplain Level.WARNING}.
     */
    @Override
    public void stopping(State from) {
        serviceListenerLogger.warning(String.format("%s %s \u2192 stopping.",
                taskName, from.name()));
    }

    /**
     * Logs a terminated message at {@linkplain Level.WARNING}.
     */
    @Override
    public void terminated(State from) {
        serviceListenerLogger.warning(String.format("%s %s \u2192 terminated.",
                taskName, from.name()));
    }

    /**
     * Logs a failed message at {@linkplain Level.SEVERE}.
     * 
     * <p>
     * The given Throwable, class that threw the Throwable, and the current
     * Thread Id are logged.
     * </p>
     * 
     * @see Thread#getId()
     */
    @Override
    public void failed(State from, Throwable failure) {
        LogRecord logRecord = new LogRecord(Level.SEVERE, "Failure!");
        logRecord.setThrown(failure);
        logRecord.setThreadID((int) Thread.currentThread().getId());
        logRecord.setSourceClassName(failure.getStackTrace()[0].getClassName());
        logRecord.setLoggerName(serviceListenerLogger.getName());

        serviceListenerLogger.log(logRecord);
    }

}
