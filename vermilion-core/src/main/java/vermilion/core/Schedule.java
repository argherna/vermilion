package vermilion.core;

import java.util.concurrent.TimeUnit;

/**
 * Collecting parameter type for task execution scheduling.
 * 
 * @author andy
 * 
 */
public interface Schedule {

    /**
     * The initial delay tells the service how many TimeUnits to wait before
     * executing a task for the first time.
     * 
     * @return the initial delay.
     */
    public Long getInitialDelay();

    /**
     * The interval tells the service how many TimeUnits to wait between
     * executions of a task.
     * 
     * @return the interval.
     */
    public Long getInterval();

    /**
     * The TimeUnit for the initial delay and interval.
     * 
     * @return the TimeUnit for the initial delay and interval.
     */
    public TimeUnit getTimeUnit();
}
