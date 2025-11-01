package ManhattanDistance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads in an input array of numbers, and a distance threshold
 * calculates the number of squares that are within Manhattan Distance
 * of any positive values from the input array
 * outputs the result to the console
 */
public class Neighbors {

    private static Logger logger = LoggerFactory.getLogger(Neighbors.class);
    private static final double DENSITY_TUNE_FACTOR = .02;

    /**
     * Reads in an input array of numbers, and a distance threshold,
     * uses env variable performTest to determine whether to validate results
     * Parses the array to a grid of boolean values and calculates the number
     * of squares that are within Manhattan Distance of any positive values from the input array
     * outputs the result to the console
     * Contains an algorithm optimized for sparse arrays and an algorithm optimized for dense arrays
     * If PERFORM_TEST=true the function will run both algorithms and confirm they generate the same result
     * @param args a string filename of a JSON file containing
     *             a "data" 2 dimensional array
     *             a "distanceThreshold" integer > 0
     *             an optional "density" that can be "sparse", "dense" or by default "test"
     *             which determines which algorithm is used (if PERFORM_TEST both are still run)
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
        boolean performTest = System.getenv().getOrDefault("PERFORM_TEST", "false").equals("true");
        FlagValues flagData = GridReader.parseJsonNeighborData(jsonData, performTest);
        if (flagData.array.length == 0 || flagData.distanceThreshold < 0) {
            System.out.println("please supply a JSON file with a 'distanceThreshold' " +
                    "integer >=0 and a 'data' 2 dimenisonal array");
            return;
        }
        int neighborCount = getNeighbors(flagData, performTest);
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
        boolean isDense = density > DENSITY_TUNE_FACTOR;
        logger.info("distanceThreshold: " + distanceThreshold + " arrayFlagCount : " + arrayFlagCount
                + " gridSize " + gridSize);
        logger.info("theoretical best coverage = 2 * distanceThreshold * (distanceThreshold +1 )  " +
                (2 * distanceThreshold * (distanceThreshold +1 ) ));
        logger.info("density = coverage / gridSize ");
        logger.info("density: " + density);
        logger.info("density threshold = DENSITY_TUNE_FACTOR = " +
                DENSITY_TUNE_FACTOR + " = " );
        logger.info("isDense: " + isDense);
        return isDense;
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
    private static int getNeighbors(FlagValues flagData,
                                    boolean performTest) {
        // generate a 2D array with the same dimensions to track
        int[][] neighbors = new int[flagData.rowCount][flagData.colCount];
        logger.debug("input array:\n" + printArray(flagData.array));
        boolean isSparse;
        if (flagData.density == FlagValues.Density.TEST) {
            isSparse = !arrayIsDense(flagData.colCount * flagData.rowCount,
                    flagData.distanceThreshold, flagData.flagCount);
        } else { // either sparse or dense
            isSparse = flagData.density == FlagValues.Density.SPARSE;
            logger.info ("assuming grid density is " + (isSparse ? "sparse" : "dense"));
        }

        int neighborCount = isSparse ? ScanFlagFill.flagFill(flagData, neighbors, flagData.distanceThreshold) :
                ScanMultiPass.flagScan(flagData.array, neighbors, flagData.distanceThreshold);
        if (performTest) {
            logger.info("executing test");
            neighbors = new int[flagData.rowCount][flagData.colCount];
            int altCount = isSparse ? ScanMultiPass.flagScan(flagData.array, neighbors, flagData.distanceThreshold) :
                    ScanFlagFill.flagFill(flagData, neighbors, flagData.distanceThreshold);
            if (altCount != neighborCount) {
                logger.error("Primary " + neighborCount +" and Alternate " + altCount +" counts do not match");
            }
            logger.info("Alternate Count: " + altCount);
        }
        logger.debug("neighbors array:\n" + printArray(neighbors));
        return neighborCount;
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
}
