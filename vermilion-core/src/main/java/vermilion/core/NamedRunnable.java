package vermilion.core;

/**
 * Adds a name to a Runnable.
 * 
 * @author andy
 *
 */
public interface NamedRunnable extends Runnable {

    public String getName();
    
    public void setName(String name);
}
