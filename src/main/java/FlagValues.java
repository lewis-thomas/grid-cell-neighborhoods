package FlagNeighbors;

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
    public boolean[][] array;
    public ArrayList<GridCoordinate> flags;
    public int flagCount;
    public final boolean useArray;
    public final boolean useList;
    public final int rowCount;
    public final int colCount;
    public FlagValues(int rowCount, int colCount, boolean useArray, boolean useList) {
        this.flags = new ArrayList<GridCoordinate>();
        this.array = new boolean[rowCount][colCount];
        this.useArray = useArray;
        this.useList = useList;
        this.flagCount = 0;
        this.colCount = colCount;
        this.rowCount = rowCount;
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
