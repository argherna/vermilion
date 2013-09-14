package vermilion.management;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import vermilion.core.NamedRunnable;

/**
 * StateTransion that logs method calls.
 * 
 * <p>
 * <strong>Implementation note</strong>: A (static) JDK logger
 * {@linkplain Class#getName() named} for this class logs the stop message at
 * {@linkplain Level.FINE}.
 * </p>
 * 
 * @author andy
 * 
 */
public class LoggingStateTransition implements InvocationHandler {

    private static final Logger logger = Logger
            .getLogger(LoggingStateTransition.class.getName());

    private final SequenceGenerator seqgen = new SequenceGenerator();

    /**
     * Logs a message to the class logger when a method is invoked.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        String methodName = method.getName();
        Class<?> voidReturnType = method.getReturnType();
        NamedRunnable task = (NamedRunnable) args[0];
        if ("starting".equals(methodName)) {
            logger.fine(task.getName() + ", "
                    + NamedRunnable.NamedRunnableState.STARTING);
            task.setExecutionId(seqgen.next());
            task.setState(NamedRunnable.NamedRunnableState.STARTING);
        } else if ("started".equals(methodName)) {
            logger.fine(task.getName() + ": " + task.getExecutionId() + ", "
                    + NamedRunnable.NamedRunnableState.STARTED);
            task.setState(NamedRunnable.NamedRunnableState.STARTED);
        } else if ("stopping".equals(methodName)) {
            logger.fine(task.getName() + ": " + task.getExecutionId() + ", "
                    + NamedRunnable.NamedRunnableState.STOPPING);
            task.setState(NamedRunnable.NamedRunnableState.STOPPING);
        } else if ("stopped".equals(methodName)) {
            logger.fine(task.getName() + ": " + task.getExecutionId() + ", "
                    + NamedRunnable.NamedRunnableState.STOPPED);
            task.setState(NamedRunnable.NamedRunnableState.STOPPED);
        } else if ("completed".equals(methodName)) {
            logger.fine(task.getName() + " :" + task.getExecutionId() + ", "
                    + NamedRunnable.NamedRunnableState.COMPLETED);
            task.setState(NamedRunnable.NamedRunnableState.COMPLETED);
        } else if ("failed".equals(methodName)) {
            logger.fine(task.getName() + ": " + task.getExecutionId() + ", "
                    + NamedRunnable.NamedRunnableState.FAILED);
            task.setState(NamedRunnable.NamedRunnableState.FAILED);
        } else if ("abandoned".equals(methodName)) {
            logger.fine(task.getName() + ": " + task.getExecutionId() + ", "
                    + NamedRunnable.NamedRunnableState.ABANDONED);
            task.setState(NamedRunnable.NamedRunnableState.ABANDONED);
        }

        return voidReturnType;

    }
}
