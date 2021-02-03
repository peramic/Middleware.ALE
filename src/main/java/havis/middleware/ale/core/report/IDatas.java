package havis.middleware.ale.core.report;

/**
 * Interface for cycle datas
 */
public interface IDatas extends Cloneable {

    /**
     * Clears data
     */
    void clear();

    /**
     * Rotates data
     */
    void rotate();

    /**
     * Resets data
     */
    void reset();
    
    /**
     * Returns a member wise cone of the datas object
     * @return The datas object clone
     */
    IDatas clone();

    void dispose();
}