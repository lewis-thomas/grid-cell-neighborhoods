import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neighbors {

    private static Logger logger = LoggerFactory.getLogger(Neighbors.class);
    private static final String FIELD_DISTANCE_THRESHOLD = "distanceThreshold";
    private static final String FIELD_DATA = "data";
    private static int flagCount = 0;

    /**
     * Reads in an input array of numbers, and a distance threshold,
     * uses env variable performTest to determine whether to validate results
     * Parses the array to a grid of boolean values and calculates the number
     * of squares that are within Manhattan Distance of any positive values from the input array
     * outputs the result to the console
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
        boolean performTest = System.getenv("PERFORM_TEST").equals("true");
        JsonArray dataArray = jsonObject.getJsonArray(FIELD_DATA);
        logger.info("distance threshold: " + distanceThreshold);
        boolean[][] array = parseJsonArray(dataArray);

        if (array.length == 0 || distanceThreshold < 0) {
            System.out.println("please supply a JSON file with a 'distanceThreshold' " +
                    "integer >=0 and a 'data' 2 dimenisonal array");
            return;
        }
        int neighborCount = getNeighbors(array, distanceThreshold, flagCount, performTest);
        System.out.println("found neighbor count " + neighborCount);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        long durationMillis = duration / 1_000_000;
        System.out.println("elapsed time in ms:" + durationMillis);
    }

    /**
     * calculates whether array is "dense" meaning many overlaps and a fill
     * approach is likely to be slower than searching for neighbors due to many overlaps
     * @param array a 2 dimensional array that is grid shaped with flagged values set to true
     * @param arrayFlagCount count of flagged values in array
     * @param distanceThreshold a number of Manhattan Distance steps to walk for neighbors
     * @return whether or not the array is considered dense
     */
    private static boolean arrayIsDense (boolean[][] array, int distanceThreshold, int arrayFlagCount) {
        int gridSize = array.length * array[0].length;
        double density = 1.0 * arrayFlagCount * 2 * distanceThreshold * (distanceThreshold +1 ) / gridSize ;
        boolean isDense = density > distanceThreshold;
        logger.info("density = arrayFlagCount * 2 * distanceThreshold * (distanceThreshold +1 ) / gridSize ");
        logger.info("arrayFlagCount : " + arrayFlagCount + " distanceThreshold: " + distanceThreshold + " = "
                + arrayFlagCount * distanceThreshold + " gridSize: " + gridSize);
        logger.info("density: " + density);
        logger.info("isDense: " + isDense);
        return isDense;
    }

    /**
     * Set all neighbors within distanceThreshold of the target Manhattan Distance to true
     * Walks the square around the location of size distanceThreshold,
     * for 0 only the square itself
     * If Manhattan Distance of x,y from targetCol, targetRow > distanceThreshold skip
     * uses a neighbors integer array to avoid duplicates and optimize searching
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
                int newVal = distanceThreshold - manhattanDistance + 1;
                if (neighborVal < newVal) {
                    neighbors[row][col]=newVal;
                    flaggedCount+= neighborVal == 0 ? 1 : 0;
                }
                else if (neighborVal > 1) {
                    col += neighborVal - 1;
                }
            }
        }
        return flaggedCount;
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
    private static int getNeighbors(boolean [][] array, int distanceThreshold, int arrayFlagCount,
                                    boolean performTest) {
        // generate a 2D array with the same dimensions to track
        int[][] neighbors = new int[array.length][array[0].length];
        logger.info("input array:\n" + printArray(array));
        boolean isDense = arrayIsDense(array, distanceThreshold, arrayFlagCount);
        int neighborCount = isDense ? flagSearch(array, distanceThreshold) : flagFill(array, neighbors, distanceThreshold);
        if (performTest) {
            logger.info("executing test");
            int altCount = !isDense ? flagSearch(array, distanceThreshold) : flagFill(array, neighbors, distanceThreshold);
            if (altCount != neighborCount) {
                logger.error("Primary " + neighborCount +" and Alternate " + altCount +" counts do not match");
            }
            logger.info("Alternate Count: " + altCount);
        }
        logger.info("neighbors array:\n" + printArray(neighbors));
        return neighborCount;
    }

    /**
     * find all neighbors of true values in 2 dimensional array within
     * distanceThreshold Manhattan Distance of a flagged (true) value
     * by filling around every flag via flagFill
     * @param array a 2 dimensional array that is grid shaped with flagged values set to true
     * @param neighbors a 2 dimensional array to track fills to avoid double counting
     * @param distanceThreshold a number of Manhattan Distance steps to walk for neighbors
     * @return count of cells falling within distanceThreshold of true values in array
     */
    private static int flagFill(boolean [][] array, int [][] neighbors, int distanceThreshold){
        int neighborCount = 0;
        for (int row = 0; row < array.length; row++) {
            for (int col = 0; col < array[row].length; col++) {
                if (array[row][col]) {
                    neighborCount += flagNeighbors(row, col, distanceThreshold, neighbors);
                }
            }
        }
        return neighborCount;
    }

    /**
     * parses JsonArray into a standard array
     * Assumes the array is not jagged
     * handles both floating point and integer values
     *
     * @param jsonArray the input two-dimensional JsonArray
     * @return a standard two-dimensional array of boolean
     * */
    private static boolean[][] parseJsonArray(JsonArray jsonArray) {
        logger.info("input data:\n" + jsonArray.toString().replaceAll("],", "],\n"));
        StringBuilder sb = new StringBuilder();
        if (jsonArray.size()==0) return new boolean[0][0];
        int height = jsonArray.size();
        int width = jsonArray.getJsonArray(0).size();
        boolean[][] array = new boolean[height][width];
        flagCount = 0;
        logger.info("parsing JSON array with height "+height+" width "+width);
        for (int row = 0; row <height; row++) {
            JsonArray rowData = jsonArray.getJsonArray(row);
            for (int col = 0; col < width; col++) {
                array[row][col] = rowData.getJsonNumber(col).doubleValue() > 0;
                flagCount+= array[row][col] ? 1 : 0;
            }
        }
        return array;
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
}
