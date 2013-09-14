package vermilion.management;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import vermilion.core.NamedRunnable;
import vermilion.core.NamedRunnable.NamedRunnableState;

/**
 * Wraps the {@link NamedRunnable#run() run} method by executing calls to a
 * {@link StateTransition} instance.
 * 
 * @author andy
 * 
 */
class StatefulNamedRunnable implements InvocationHandler {

    private final NamedRunnable task;

    private final StateTransition stateTransition;

    /**
     * Constructs a new instance of this class.
     * 
     * @param task
     *            the NamedRunnable to wrap.
     * @param stateTransition
     *            the StateTransition instance.
     */
    public StatefulNamedRunnable(NamedRunnable task,
            StateTransition stateTransition) {
        super();
        this.task = task;
        this.stateTransition = stateTransition;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        Class<?> voidReturnType = method.getReturnType();
        if ("run".equals(method.getName())) {
            Throwable caught = null;
            stateTransition.started(task);
            try {
                task.run();
            } catch (Throwable t) {
                caught = t;
            } finally {
                stateTransition.stopping(task);
                if (caught != null) {
                    if (caught instanceof InterruptedException) {
                        stateTransition.abandonded(task);
                    } else {
                        stateTransition.failed(task);
                    }
                } else {
                    stateTransition.completed(task);
                }
                stateTransition.stopped(task);
                if (caught != null) {
                    throw caught;
                }
            }
            return voidReturnType;
        } else if ("getName".equals(method.getName())) {
            return task.getName();
        } else if ("getExecutionId".equals(method.getName())) {
            return task.getExecutionId();
        } else if ("setName".equals(method.getName())) {
            String taskname = (String) args[0];
            task.setName(taskname);
            return voidReturnType;
        } else if ("setExecutionId".equals(method.getName())) {
            Integer execId = (Integer) args[0];
            task.setExecutionId(execId);
            return voidReturnType;
        } else if ("getState".equals(method.getName())) {
            return task.getState();
        } else if ("setState".equals(method.getName())) {
            NamedRunnableState state = (NamedRunnableState) args[0];
            task.setState(state);
            return voidReturnType;
        } else {
            return method.invoke(proxy, args);
        }

    }
}
