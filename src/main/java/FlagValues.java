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
    public boolean[][] array;
    public ArrayList<GridCoordinate> flags;
    public int flagCount;
    public final boolean useArray;
    public final boolean useList;
    public final int rowCount;
    public final int colCount;
    public final int distanceThreshold;
    public final Density density;
    public FlagValues(int rowCount, int colCount, String densityParam,
                      int distanceThreshold, boolean performTest) {
        this.flags = new ArrayList<GridCoordinate>();
        this.array = new boolean[rowCount][colCount];
        this.density = densityParam.equals("sparse") ? Density.SPARSE :
                densityParam.equals("dense") ? Density.DENSE : Density.TEST;
        this.useArray = density == Density.DENSE || density == density.TEST || performTest;
        this.useList = density == Density.SPARSE || density == density.TEST || performTest;
        this.flagCount = 0;
        this.colCount = colCount;
        this.rowCount = rowCount;
        this.distanceThreshold = distanceThreshold;
    }
    public void addPoint(int row, int col) {
        if (useList) {
            flags.add(new GridCoordinate(row, col));
        }
        if (useArray) {
            array[row][col] = true;
        }
        flagCount++;
    }
}
