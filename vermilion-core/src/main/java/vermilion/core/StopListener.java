package vermilion.core;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Runnable that is meant to be used during shutdown.
 * 
 * <p>
 * This runnable will log a message at {@linkplain Level.WARNING} that a service
 * has been stopped.
 * </p>
 * 
 * <p>
 * <strong>Implementation note</strong>: A (static) JDK logger
 * {@linkplain Class#getSimpleName() named} for this class logs the stop message
 * at {@linkplain Level.WARNING}.
 * </p>
 * 
 * @author andy
 * 
 */
class StopListener implements Runnable {

    private static final Logger logger = Logger.getLogger(StopListener.class
            .getSimpleName());

    private final String serviceName;

    /**
     * @param serviceName
     *            name of the service to report on.
     */
    public StopListener(String serviceName) {
        super();
        this.serviceName = serviceName;
    }

    /**
     * Logs a message at {@linkplain Level.WARNING} that says the service has
     * stopped.
     */
    @Override
    public void run() {
        logger.warning(String.format("%s stopped.", serviceName));
    }

}
