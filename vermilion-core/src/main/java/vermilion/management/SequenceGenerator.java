package vermilion.management;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple sequence generator.
 * 
 * @author andy
 * 
 */
class SequenceGenerator {

    private final int startValue;

    private final int increment;

    private int value;

    private final Lock lock = new ReentrantLock();

    /**
     * Construct a new instance of this SequenceGenerator starting at 0 and
     * incrementing by 1.
     */
    SequenceGenerator() {
        this(0, 1);
    }

    /**
     * Construct a new instance of this SequenceGenerator starting at the given
     * start value and incrementing by 1.
     * 
     * @param startValue
     *            the start value for this SequenceGenerator.
     */
    SequenceGenerator(int startValue) {
        this(startValue, 1);
    }

    /**
     * Construct a new instance of this SequenceGenerator starting at the given
     * start value and incrementing by given increment.
     * 
     * @param startValue
     *            the start value for this SequenceGenerator.
     * @param increment
     *            the value to increment for the next value.
     */
    SequenceGenerator(int startValue, int increment) {
        this.startValue = startValue;
        this.increment = increment;

        value = startValue;
    }

    /**
     * Increments the sequence generator.
     * 
     * @return the next value for the sequence after adding this instance's
     *         increment.
     */
    Integer next() {
        lock.lock();

        try {
            return Integer.valueOf(value += increment);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the next value of the sequence without incrementing the sequence.
     * 
     * @return the next value of this sequence without incrementing.
     */
    Integer peekNext() {
        lock.lock();

        try {
            int peek = value + increment;
            return Integer.valueOf(peek);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 
     * @return the start value for this SequenceGenerator.
     */
    Integer getStartValue() {
        return Integer.valueOf(startValue);
    }

    /**
     * 
     * @return the increment for this SequenceGenerator.
     */
    Integer getIncrement() {
        return Integer.valueOf(increment);
    }
}
