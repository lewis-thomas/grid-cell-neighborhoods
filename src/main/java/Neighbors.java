import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads in an input array of numbers, and a distance threshold
 * calculates the number of squares that are within Manhattan Distance
 * of any positive values from the input array
 * outputs the result to the console
 */
public class Neighbors {


    private enum Density{ DENSE, SPARSE, TEST};
    private static Logger logger = LoggerFactory.getLogger(Neighbors.class);
    private static final String FIELD_DISTANCE_THRESHOLD = "distanceThreshold";
    private static final String FIELD_DATA = "data";
    private static final double DENSITY_TUNE_FACTOR = 5.0;

    private record GridCoordinate(int row, int col) {}
    /**
     * holds results of the array parse
     * we need to track
     * the values converted to boolean and a count of flags
     * the dense search is more optimal with a two dimensional array
     * the sparse search would be more optimal with a list of points
     * but the difference is negligible
     */
    private static class FlagValues {
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

    /**
     * Reads in an input array of numbers, and a distance threshold,
     * uses env variable performTest to determine whether to validate results
     * Parses the array to a grid of boolean values and calculates the number
     * of squares that are within Manhattan Distance of any positive values from the input array
     * outputs the result to the console
     * Contains an algorithm optimized for sparse arrays and an algorithm optimized for dense arrays
     * by default it will calculate which algorithm should be used. It is possible to force
     * this function to use the sparse algorithm via ASSUME_SPARSE=true or the dense algorithm
     * via ASSUME_DENSE=true
     * If PERFORM_TEST=true the function will run both algorithms and confirm they generate the same result
     * @param args a string filename of a JSON file containing
     *             a "data" 2 dimensional array
     *             a "distanceThreshold" integer > 0
     */
    public static void main(String[] args) {
        long startTime = System.nanoTime();
        if (args.length == 0) {
            System.out.println("Please enter a JSON file path with neighbor data");
            return;
        }
        String filePath = args[0];
        String jsonData = null;
        try {
            jsonData = Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }
        JsonReader jsonReader = Json.createReader(new StringReader(jsonData));
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();
        int distanceThreshold = jsonObject.getInt(FIELD_DISTANCE_THRESHOLD, -1);
        boolean performTest = System.getenv().getOrDefault("PERFORM_TEST", "false").equals("true");
        boolean assumeSparse = System.getenv().getOrDefault("ASSUME_SPARSE", "false").equals("true");
        boolean assumeDense = System.getenv().getOrDefault("ASSUME_DENSE", "false").equals("true");
        Density density = assumeSparse ? Density.SPARSE : assumeDense ? Density.DENSE : Density.TEST;
        JsonArray dataArray = jsonObject.getJsonArray(FIELD_DATA);
        logger.info("distance threshold: " + distanceThreshold);
        FlagValues flagData = parseJsonArray(dataArray, performTest || !assumeSparse, performTest || !assumeDense);

        if (flagData.array.length == 0 || distanceThreshold < 0) {
            System.out.println("please supply a JSON file with a 'distanceThreshold' " +
                    "integer >=0 and a 'data' 2 dimenisonal array");
            return;
        }
        int neighborCount = getNeighbors(flagData, distanceThreshold, performTest, density);
        System.out.println("found neighbor count " + neighborCount);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        long durationMillis = duration / 1_000_000;
        System.out.println("elapsed time in ms:" + durationMillis);
    }

    /**
     * calculates whether array is "dense" meaning many overlaps and a fill
     * approach is likely to be slower than searching for neighbors due to many overlaps
     * the density of the grid is based on the neighbors by Manhattan Distance being an arithmetic series
     * of 2 * distanceThreshold * (distanceThreshold +1 )
     * a grid density of 1 implies in theory every cell could have a flagged neighbor if
     * the flags were spaced evenly and none went off the grid
     * we then apply a tuning factor based on the relative speed of our dense vs sparse algorithms
     * to achieve best possible performance across varied grid densities
     * @param array a 2 dimensional array that is grid shaped with flagged values set to true
     * @param arrayFlagCount count of flagged values in array
     * @param distanceThreshold a number of Manhattan Distance steps to walk for neighbors
     * @return whether or not the array is considered dense
     */
    private static boolean arrayIsDense (int gridSize, int distanceThreshold, int arrayFlagCount) {
        double density = 1.0 * arrayFlagCount * 2 * distanceThreshold * (distanceThreshold +1 ) / gridSize ;
        boolean isDense = density > distanceThreshold * DENSITY_TUNE_FACTOR;
        logger.info("distanceThreshold: " + distanceThreshold + " arrayFlagCount : " + arrayFlagCount
                + " gridSize " + gridSize);
        logger.info("theoretical best coverage = 2 * distanceThreshold * (distanceThreshold +1 )  " +
                (2 * distanceThreshold * (distanceThreshold +1 ) ));
        logger.info("density = coverage / gridSize ");
        logger.info("density: " + density);
        logger.info("density threshold = distanceThreshold * DENSITY_TUNE_FACTOR = " + distanceThreshold +
                " * " + DENSITY_TUNE_FACTOR + " = " + (distanceThreshold * DENSITY_TUNE_FACTOR));
        logger.info("isDense: " + isDense);
        return isDense;
    }

    /**
     * Set all neighbors within distanceThreshold of the target Manhattan Distance to true
     * Walks the square around the location of size distanceThreshold,
     * for 0 only the square itself
     * If Manhattan Distance of x,y from targetCol, targetRow > distanceThreshold skip
     * Uses a neighbors integer array to avoid duplicates and optimize searching
     * It checks the marked value of each square, and if the marked value > distanceRemaining
     * then marked value -1 squares can be skipped.
     * It marks entries with distanceRemaining so that future passes can skip
     * @param targetRow target location row
     * @param targetCol target location column
     * @param distanceThreshold the maximum Manhattan Distance to check 0 being self
     * @param neighbors the grid used for tracking what is getting set to true
     * @return count of all set to true (not counting those already true)
     */
    private static int flagNeighbors(int targetRow, int targetCol,
                                     int distanceThreshold, int[][] neighbors) {
        int flaggedCount = 0;
        final int minRow = Math.max(targetRow-distanceThreshold, 0);
        final int maxRow = Math.min(neighbors.length, targetRow + distanceThreshold+1);
        final int minCol = Math.max(targetCol-distanceThreshold,0);
        final int maxCol = Math.min(neighbors[0].length, targetCol + distanceThreshold+1);
        logger.debug("flagNeighbors " + targetRow + "," + targetCol + " checking " + minRow + ","
                + minCol + " - " + (maxRow -1) + "," + (maxCol-1));
        for (int row = minRow; row < maxRow; row++) {
            for (int col = minCol; col < maxCol; col++) {
                int manhattanDistance = Math.abs(col-targetCol) + Math.abs(row-targetRow);
                logger.debug("target " + targetRow + "," + targetCol + " checking " + row + "," + col + " distance " + manhattanDistance + " threshold " + distanceThreshold + " skip ? " + (manhattanDistance > distanceThreshold) + " neighborVal " + neighbors[row][col]);
                if (manhattanDistance > distanceThreshold) continue;
                int neighborVal = neighbors[row][col];
                int distanceRemaining = distanceThreshold - manhattanDistance + 1;
                if (neighborVal < distanceRemaining) {
                    neighbors[row][col]=distanceRemaining;
                    flaggedCount+= neighborVal == 0 ? 1 : 0;
                }
                else if (neighborVal > 1) {
                    col += neighborVal - 1;
                }
            }
        }
        return flaggedCount;
    }

    private static int flagScan(boolean[][] array, int[][] neighbors, int distanceThreshold) {
        StringBuilder sb = new StringBuilder();
        boolean hasError = false;
        int neighborCount = 0;
        for (int row = 0; row < array.length; row++) {
            for (int col = 0; col < array[row].length; col++) {
                neighborCount += shouldBeFlagged(row, col, distanceThreshold, array) ? 1 : 0;
            }
        }
        return neighborCount;
    }

    private static int flagScanOne(boolean[][] array, int[][] neighbors, int distanceThreshold) {
        boolean hasError = false;
        int neighborCount = 0;
        for (int row = 0; row < array.length; row++) {
            for (int col = 0; col < array[row].length; col++) {
                neighborCount += shouldBeFlagged(row, col, distanceThreshold, array) ? 1 : 0;
            }
        }
        return neighborCount;
    }

    /**
     * Executes an alternate method via shouldBeFlagged for calculating neighbors
     * and compare to the flagged array
     * builds a visualization string where
     *  0 indicates a correctly non-flagged point
     *  1 indicates an incorrectly flagged point
     *  2 indicates an incorrectly non flagged point
     *  3 indicates a correctly flagged point
     * @param distanceThreshold the neighbor distance threshold
     * @param neighbors the original 2d array of booleans
     * @param flagged the calculated result
     * @return boolean
     */
    private static int flagSearch(boolean[][] array, int distanceThreshold) {
        boolean hasError = false;
        int neighborCount = 0;
        for (int row = 0; row < array.length; row++) {
            for (int col = 0; col < array[row].length; col++) {
                neighborCount += shouldBeFlagged(row, col, distanceThreshold, array) ? 1 : 0;
            }
        }
        return neighborCount;
    }

    /**
     * find all neighbors of true values in 2 dimensional array within
     * distanceThreshold Manhattan Distance of a flagged (true) value
     * by filling around every flag via flagNeighbors
     * @param array a 2 dimensional array that is grid shaped with flagged values set to true
     * @param neighbors a 2 dimensional array to track fills to avoid double counting
     * @param distanceThreshold a number of Manhattan Distance steps to walk for neighbors
     * @return count of cells falling within distanceThreshold of true values in array
     */
    private static int flagFill(FlagValues flagData, int [][] neighbors, int distanceThreshold){
        int neighborCount = 0;
        for (int i = 0; i < flagData.flags.size(); i++) {
            neighborCount += flagNeighbors(flagData.flags.get(i).row, flagData.flags.get(i).col, distanceThreshold, neighbors);
        }
        return neighborCount;
    }

    /**
     * find all neighbors of true values in 2 dimensional array within
     * distanceThreshold Manhattan Distance of a flagged (true) value
     * for dense arrays search for flags from every point via flagSearch
     * for sparse arrays fill around every flag via flagFill
     * @param array a 2 dimensional array that is grid shaped with flagged values set to true
     * @param distanceThreshold a number of Manhattan Distance steps to walk for neighbors
     * @param arrayFlagCount count of flagged values in array
     * @param performTest if a validation test should be performed
     * @return count of cells falling within distanceThreshold of true values in array
     */
    private static int getNeighbors(FlagValues flagData, int distanceThreshold,
                                    boolean performTest, Density density) {
        // generate a 2D array with the same dimensions to track
        int[][] neighbors = new int[flagData.rowCount][flagData.colCount];
        logger.info("input array:\n" + printArray(flagData.array));
        boolean isSparse;
        if (density == density.TEST) {
            isSparse = !arrayIsDense(flagData.colCount * flagData.rowCount,
                    distanceThreshold, flagData.flagCount);
        } else { // either sparse or dense
            isSparse = density == Density.SPARSE;
            logger.info ("assuming grid density is " + (isSparse ? "sparse" : "dense"));
        }
        int[][] neighbors;
        if (isSparse || performTest) {
            neighbors = new int[flagData.rowCount][flagData.colCount]
        }

        int neighborCount = isSparse ? flagFill(flagData, neighbors, distanceThreshold) : flagSearch(flagData.array, distanceThreshold);
        if (performTest) {
            logger.info("executing test");
            int altCount = isSparse ? flagSearch(flagData.array, distanceThreshold) : flagFill(flagData, neighbors, distanceThreshold);
            if (altCount != neighborCount) {
                logger.error("Primary " + neighborCount +" and Alternate " + altCount +" counts do not match");
            }
            logger.info("Alternate Count: " + altCount);
        }
        logger.info("neighbors array:\n" + printArray(neighbors));
        return neighborCount;
    }

    /**
     * parses JsonArray into a standard array of booleans
     * Assumes the array is not jagged
     * handles both floating point and integer values translating positive numbers into 1
     * and non-positive numbers to 0
     * maintains a count of found positive values in class variable flagCount
     * @param jsonArray the input two-dimensional JsonArray
     * @return a standard two-dimensional array of boolean
     * */
    private static FlagValues parseJsonArray(JsonArray jsonArray,
                                                     boolean useArray, boolean useList) {
        logger.info("input data:\n" + jsonArray.toString().replaceAll("],", "],\n"));
        StringBuilder sb = new StringBuilder();
        if (jsonArray.size()==0) return new FlagValues(0,0,useArray, useList);
        int rows = jsonArray.size();
        int cols = jsonArray.getJsonArray(0).size();
        FlagValues results = new FlagValues(rows,cols, useArray, useList);
        logger.info("parsing JSON array with rows "+rows+" cols " + cols);
        logger.debug("useArray: " + useArray + " useList: " + useList);
        for (int row = 0; row <rows; row++) {
            JsonArray rowData = jsonArray.getJsonArray(row);
            for (int col = 0; col < cols; col++) {
                if (rowData.getJsonNumber(col).doubleValue()>0) {
                    results.addPoint(row, col);
                }
            }
        }
        return results;
    }


    /**
     * prints the array to a string as 1 or 0
     * Assumes the array is not jagged
     *
     * @param neighbors the input two-dimensional boolean array
     * @return a string of 0s and 1s with lines = row count, each lines length = col count
     * */
    private static String printArray(boolean[][] neighbors) {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < neighbors.length; row++) {
            for (int col = 0; col < neighbors[row].length; col++) {
                sb.append (neighbors[row][col] ? "1" : "0");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * prints the array to a string as integers
     * Assumes the array is not jagged
     *
     * @param neighbors the two-dimensional int array
     * @return a string of integers with lines = row count, each lines length = col count
     * */
    private static String printArray(int[][] neighbors) {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < neighbors.length; row++) {
            for (int col = 0; col < neighbors[row].length; col++) {
                sb.append ("" + neighbors[row][col]);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * check if a target location should be flagged by walking Manhattan distance checking for
     * a flag value
     * we walk a grid, skipping elements outside Manhattan distance,
     * checking all entries within Manhattan distance and exiting if we find a flag
     * @param targetRow the target location row
     * @param targetCol the target location column
     * @param distanceThreshold the maximum distance to walk
     * @param neighbors the grid of flagged values
     * @return boolean whether the location should be flagged
     */
    private static boolean shouldBeFlagged(int targetRow, int targetCol, int distanceThreshold, boolean[][] neighbors) {
        for (int y = Math.max(targetRow-distanceThreshold, 0); y < Math.min(neighbors.length, targetRow + distanceThreshold+1); y++) {
            for (int x = Math.max(targetCol-distanceThreshold,0); x < Math.min(neighbors[y].length, targetCol + distanceThreshold+1); x++) {
                int manhattanDistance = Math.abs(x-targetCol) + Math.abs(y-targetRow);
                logger.debug("position " + x + " " + y + " distance " + manhattanDistance + " threshold " + distanceThreshold + " skip ? " + (manhattanDistance > distanceThreshold));
                if (manhattanDistance > distanceThreshold) continue;
                if (neighbors[y][x]) {
                    return true;
                }
            }
        }
        return false;
    }
}
