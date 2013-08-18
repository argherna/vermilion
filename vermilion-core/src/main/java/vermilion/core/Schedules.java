package vermilion.core;

import java.util.concurrent.TimeUnit;

/**
 * Static factories for {@link Schedule} implementations.
 * 
 * @author andy
 * 
 */
public class Schedules {

    private Schedules() {
        // empty constructor
    }

    /**
     * Returns a default Schedule implementation.
     * 
     * @param initialDelay
     *            number of timeUnits to wait before the first execution of this
     *            job.
     * @param interval
     *            number of timeUnits between executions of this job after the
     *            initial delay.
     * @param timeUnit
     *            a {@link TimeUnit} for initial delay and interval.
     * @return a Schedule.
     */
    public static Schedule createSchedule(Long initialDelay, Long interval,
            TimeUnit timeUnit) {
        return new DefaultScheduleImpl(initialDelay, interval, timeUnit);
    }
}
