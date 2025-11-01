package ManhattanDistance;

import java.util.ArrayList;

/**
 * holds results of the array parse
 * we need to track
 * the values converted to boolean and a count of flags
 * the dense search is more optimal with a two dimensional array
 * the sparse search would be more optimal with a list of points
 * but the difference is negligible
 */
public class FlagValues {
    public record GridCoordinate(int row, int col) {}
    public enum Density{ DENSE, SPARSE, TEST};
    public ArrayList<GridCoordinate> flags;
    public int flagCount;
    public final int rowCount;
    public final int colCount;
    public final int distanceThreshold;
    public final Density density;

    /**
     * constructor, initializes based on input parameters
     * @param rowCount - input array row count
     * @param colCount - input array column count
     * @param densityParam - whether the data should be considered dense, sparse, or we test
     * @param distanceThreshold - the number of steps to walk from flagged values
     */
    public FlagValues(int rowCount, int colCount, String densityParam,
                      int distanceThreshold) {
        this.flags = new ArrayList<GridCoordinate>();
        this.density = densityParam.equals("sparse") ? Density.SPARSE :
                densityParam.equals("dense") ? Density.DENSE : Density.TEST;
        this.flagCount = 0;
        this.colCount = colCount;
        this.rowCount = rowCount;
        this.distanceThreshold = distanceThreshold;
    }

    /**
     * adds a point to our list of grid coordinates
     * @param row
     * @param col
     */
    public void addPoint(int row, int col) {
        flags.add(new GridCoordinate(row, col));
        flagCount++;
    }
}
