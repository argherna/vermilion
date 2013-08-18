package vermilion.core;

import java.util.concurrent.TimeUnit;


/**
 * A default Schedule implementation.
 * 
 * @author andy
 * 
 */
class DefaultScheduleImpl implements Schedule {

    private final Long interval;

    private final Long initialDelay;

    private final TimeUnit timeUnit;

    /**
     * Construct a new DefaultScheduleImpl using the given parameters.
     * @param initialDelay
     *            the number of TimeUnits to wait before the first execution.
     * @param interval
     *            the number of TimeUnits between executions.
     * @param timeUnit
     *            the TimeUnits.
     */
    DefaultScheduleImpl(Long initialDelay, Long interval,
            TimeUnit timeUnit) {
        this.interval = interval;
        this.initialDelay = initialDelay;
        this.timeUnit = timeUnit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getInterval() {
        return interval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getInitialDelay() {
        return initialDelay;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}