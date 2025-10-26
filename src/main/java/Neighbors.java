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
    public static final String FIELD_DISTANCE_THRESHOLD = "distanceThreshold";
    public static final String FIELD_PERFORM_TEST = "performTest";
    public static final boolean PERFORM_TEST_DEFAULT = false;
    public static final String FIELD_DATA = "data";


    public static void main(String[] args) {
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
        boolean performTest = jsonObject.getBoolean(FIELD_PERFORM_TEST, PERFORM_TEST_DEFAULT);
        JsonArray dataArray = jsonObject.getJsonArray(FIELD_DATA);
        logger.info("distance threshold: " + distanceThreshold);
        int[][] array = parseJsonArray(dataArray);

        if (array.length == 0 || distanceThreshold < 0) {
            System.out.println("please supply a JSON file with a 'distanceThreshold' integer >=0 and a 'data' 2 dimenisonal array");
            return;
        }
        System.out.println("found neighbor count " + getNeighbors(array, distanceThreshold,  performTest));
    }

    // set all neighbors within Manhattan Distance {distanceThreshold} to true
    // return a count of all set to true (not counting those already true)
    public static int flagNeighbors(int targetRow, int targetCol, int distanceThreshold, boolean[][] neighbors) {
        // walk the square around the location of size distanceThreshold,
        // if Manhattan Distance of x,y from targetCol, targetRow > distanceThreshold skip
        // if the neighbors array x,y is false, set to true and add to flaggedCount
        int flaggedCount = 0;
        for (int row = Math.max(targetRow-distanceThreshold, 0); row < Math.min(neighbors.length, targetRow + distanceThreshold+1); row++) {
            for (int col = Math.max(targetCol-distanceThreshold,0); col < Math.min(neighbors[row].length, targetCol + distanceThreshold+1); col++) {
                int manhattanDistance = Math.abs(col-targetCol) + Math.abs(row-targetRow);
                logger.debug("position " + col + " " + row + " distance " + manhattanDistance + " threshold " + distanceThreshold + " skip ? " + (manhattanDistance > distanceThreshold));
                if (manhattanDistance > distanceThreshold) continue;
                if (!neighbors[row][col]) {
                    neighbors[row][col]=true;
                    flaggedCount++;
                }
            }
        }
        return flaggedCount;

    }

    // given an input 2D array generate an output 2D array with the same dimensions
    // with all cells that fall within N steps of any positive values in the array 1 else 0
    // return the number of cells that fall within N steps of any positive values in the array
    public static int getNeighbors(int [][] array, int distanceThreshold, boolean performTest) {
        // generate a 2D array with the same dimensions to track
        int neighborCount = 0;
        boolean[][] neighbors = new boolean[array.length][array[0].length];
        logger.info("input array:\n" + printArray(array, true));
        for (int row = 0; row < array.length; row++) {
            for (int col = 0; col < array[row].length; col++) {
                // >0 is a flag
                if (array[row][col]>0) {
                    neighborCount += flagNeighbors(row, col, distanceThreshold, neighbors);
                }
            }
        }
        printNeighbors(neighbors);
        if (performTest) {
            logger.info("hasError? " + testResults(distanceThreshold, array, neighbors));
        }
        return neighborCount;
    }

    /**
     * parses JsonArray into a standard array
     * Assumes the array is not jagged
     *
     * @param jsonArray the input two-dimensional JsonArray
     * @return a standard two-dimensional array of integers
     */
    public static int[][] parseJsonArray(JsonArray jsonArray) {
        StringBuilder sb = new StringBuilder();
        if (jsonArray.size()==0) return new int[0][0];
        int height = jsonArray.size();
        int width = jsonArray.getJsonArray(0).size();
        int[][] array = new int[height][width];
        logger.info("parsing JSON array with height "+height+" width "+width);
        for (int row = 0; row <height; row++) {
            JsonArray rowData = jsonArray.getJsonArray(row);
            for (int col = 0; col < width; col++) {
                array[row][col] = rowData.getInt(col);
            }
        }
        logger.info("input data:\n" + printArray(array, false));
        return array;
    }

    // visualization function - show array
    // if asBinary = true then normalized to 1 for positive values, else 0
    public static String printArray(int[][] neighbors, boolean asBinary) {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < neighbors.length; row++) {
            for (int col = 0; col < neighbors[row].length; col++) {
                sb.append (!asBinary ? neighbors[row][col] + " " : neighbors[row][col] > 0 ? "1" : "0");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // visualization function - show all neighbors
    public static void printNeighbors(boolean[][] neighbors) {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < neighbors.length; row++) {
            for (int col = 0; col < neighbors[row].length; col++) {
                sb.append (neighbors[row][col] ? "1" : "0");
            }
            sb.append("\n");
        }
        logger.info("neighbors:\n" + sb.toString());
    }

    // check if a location should be flagged
    // return if the location is within distanceThreshold Manhattan distance of a positive value
    public static boolean shouldBeFlagged(int targetRow, int targetCol, int distanceThreshold, int[][] neighbors) {
        // walk the square around the location of size distanceThreshold,
        // if Manhattan Distance of x,y from targetCol, targetRow > distanceThreshold continue
        // if the neighbors array x,y is false, set to true and add to flaggedCount
        for (int y = Math.max(targetRow-distanceThreshold, 0); y < Math.min(neighbors.length, targetRow + distanceThreshold+1); y++) {
            for (int x = Math.max(targetCol-distanceThreshold,0); x < Math.min(neighbors[y].length, targetCol + distanceThreshold+1); x++) {
                int manhattanDistance = Math.abs(x-targetCol) + Math.abs(y-targetRow);
                logger.debug("position " + x + " " + y + " distance " + manhattanDistance + " threshold " + distanceThreshold + " skip ? " + (manhattanDistance > distanceThreshold));
                if (manhattanDistance > distanceThreshold) continue;
                if (neighbors[y][x]>0) {
                    return true;
                }
            }
        }
        return false;
    }

    // walk the flagged array and check each value
    // if it is within distanceThreshold Manhattan distance of a positive value in the neighbors array
    // then it must be flagged, and if not within th threshold it must not be flagged
    public static boolean testResults(int distanceThreshold, int[][] neighbors, boolean[][] flagged) {
        StringBuilder sb = new StringBuilder();
        boolean hasError = false;

        for (int row = 0; row < neighbors.length; row++) {
            for (int col = 0; col < neighbors[row].length; col++) {
                int val = (flagged[row][col] ? 1 : 0) | (shouldBeFlagged(row, col, distanceThreshold, neighbors) ? 2 : 0);
                sb.append ("" + val);
                hasError = hasError || val ==1 || val == 2;
            }
            sb.append("\n");
        }
        logger.info("test results array:\n" + sb.toString());
        return hasError;
    }
}
