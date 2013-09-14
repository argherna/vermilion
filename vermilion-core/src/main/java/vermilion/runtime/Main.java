package vermilion.runtime;

import java.lang.reflect.Proxy;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import vermilion.core.Listeners;
import vermilion.core.NamedRunnable;
import vermilion.core.TaskExecutionService;
import vermilion.management.JmxService;
import vermilion.management.LoggingStateTransition;
import vermilion.management.QueueInvocationHandler;
import vermilion.management.StateTransition;
import vermilion.webapi.WebAppServerService;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service.State;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * @param args
     */
    public static void main(String[] args) {
        Thread shutdownHook = null;
        try {
            final BlockingQueue<NamedRunnable> tasks = Queues
                    .newLinkedBlockingQueue();
            final StateTransition stateTransition = (StateTransition) Proxy
                    .newProxyInstance(
                            LoggingStateTransition.class.getClassLoader(),
                            new Class<?>[] { StateTransition.class },
                            new LoggingStateTransition());
            
            @SuppressWarnings("unchecked")
            final BlockingQueue<NamedRunnable> taskQueue = (BlockingQueue<NamedRunnable>) Proxy
                    .newProxyInstance(
                            QueueInvocationHandler.class.getClassLoader(),
                            new Class<?>[] { BlockingQueue.class },
                            new QueueInvocationHandler(tasks, stateTransition));

            final String executionServiceName = "Execution Service";
            final TaskExecutionService tes = new TaskExecutionService(
                    taskQueue, null, Runtime.getRuntime().availableProcessors());
            tes.addListener(Listeners
                    .createLoggingServiceListener(executionServiceName),
                    MoreExecutors.sameThreadExecutor());

            final String jmxServiceName = "Jmx Service";
            final JmxService jmx = new JmxService(taskQueue, stateTransition);
            jmx.addListener(
                    Listeners.createLoggingServiceListener(jmxServiceName),
                    MoreExecutors.sameThreadExecutor());

            final String wasServiceName = "Web Application Service";
            final WebAppServerService was = new WebAppServerService();
            was.addListener(
                    Listeners.createLoggingServiceListener(wasServiceName),
                    MoreExecutors.sameThreadExecutor());

            Runtime.getRuntime().addShutdownHook(
                    shutdownHook = new Thread(new Runnable() {
                        public void run() {
                            State tesState = tes.stopAndWait();
                            if (tesState == State.TERMINATED) {
                                logger.warning(String.format("%s shutdown OK.",
                                        executionServiceName));
                            }
                            State jmxState = jmx.stopAndWait();
                            if (jmxState == State.TERMINATED) {
                                logger.warning(String.format("%s shutdown OK.",
                                        jmxServiceName));
                            }
                            State wasState = was.stopAndWait();
                            if (wasState == State.TERMINATED) {
                                logger.warning(String.format("%s shutdown OK.",
                                        wasServiceName));
                            }
                        }
                    }));
            tes.start();
            jmx.start();
            was.start();
        } catch (RuntimeException ex) {
            LogRecord record = new LogRecord(Level.SEVERE, "Runtime failure");
            record.setThrown(ex);
            logger.log(record);
        } finally {

            if (shutdownHook != null) {
                try {
                    shutdownHook.join();
                } catch (InterruptedException e) {
                    LogRecord record = new LogRecord(Level.WARNING,
                            "Failed to join shutdown hook, application shutdown may cause errors.");
                    record.setThrown(e);
                    logger.log(record);
                }
            }
        }
    }

}
