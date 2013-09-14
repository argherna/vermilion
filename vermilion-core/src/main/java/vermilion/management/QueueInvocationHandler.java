package vermilion.management;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import vermilion.core.NamedRunnable;

/**
 * Handles state transitions of tasks when they are polled from the Queue.
 * 
 * @author andy
 * 
 */
public class QueueInvocationHandler implements InvocationHandler {

    private final BlockingQueue<NamedRunnable> tasks;

    private final StateTransition stateTransition;

    /**
     * Construct a new instance of QueueInvocationHandler.
     * 
     * @param tasks
     *            BlockingQueue of NamedRunnables.
     * @param stateTransition
     *            the StateTransition.
     */
    public QueueInvocationHandler(BlockingQueue<NamedRunnable> tasks,
            StateTransition stateTransition) {
        this.tasks = checkNotNull(tasks, "Task queue is null.");
        this.stateTransition = checkNotNull(stateTransition,
                "State transition is null.");
    }

    /**
     * When {@linkplain BlockingQueue#poll() polling} for a task, mark it's
     * state as {@link TaskState#STARTING}. Otherwise invoke the method on the
     * queue taking no other action.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        String invoked = method.getName();
        if ("poll".equals(invoked)) {
            Long timeout = (Long) args[0];
            TimeUnit unit = (TimeUnit) args[1];
            NamedRunnable task = tasks.poll(timeout, unit);
            if (task != null) {
                stateTransition.starting(task);
            }
            return task;
        } else {
            return method.invoke(tasks, args);
        }
    }

}
