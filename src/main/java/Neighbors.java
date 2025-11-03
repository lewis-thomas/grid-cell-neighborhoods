package ManhattanDistance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;

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
     * Parses the array to boolean values and calculates the number
     * of squares that are within Manhattan Distance of any positive values from the input array
     * outputs the result to the console
     * Contains an algorithm optimized for sparse arrays and an algorithm optimized for dense arrays
     *
     * @param args a string filename of a JSON file containing
     *             a "data" 2 dimensional array
     *             a "distanceThreshold" integer > 0
     *             an optional "density" that can be "sparse", "dense" or by default "test"
     *             which determines which algorithm is used (if PERFORM_TEST both are still run)
     *
     *             [optional] -perform_test trigger both algorithms and confirm they generate the same result
     *             [optional] -mem_saver execute the dense version with a chunk approach saving memory
     */
    public static void main(String[] args) {
        long startTime = System.nanoTime();
        if (args.length == 0) {
            System.out.println("Please enter a JSON file path with a 'distanceThreshold'" +
                    "integer >=0 and a 'data' 2 dimenisonal array");
            return;
        }
        int argCount = args.length;
        String filePath = args[0];
        logger.debug("argCount " + argCount);
        HashSet<String> argSet = new HashSet<>();
        for (int i = 0; i < argCount; i++) {
            argSet.add(args[i]);
        }
        boolean performTest = argSet.contains("-perform_test");
        boolean memSaver = argSet.contains("-mem_saver");
        logger.info("performTest: " + performTest);
        logger.info("memSaver: " + memSaver);

        String jsonData = null;
        try {
            jsonData = Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }
        FlagValues flagData = GridReader.parseJsonNeighborData(jsonData);
        if (flagData.distanceThreshold < 0) {
            System.out.println("please supply a JSON file with a 'distanceThreshold' " +
                    "integer >=0 and a 'data' 2 dimenisonal array");
            return;
        }
        int neighborCount = getNeighbors(flagData, performTest, memSaver);
        System.out.println("found neighbor count " + neighborCount);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        long durationMillis = duration / 1_000_000;
        System.out.println("elapsed time in ms:" + durationMillis);
        long totalMemory = Runtime.getRuntime().totalMemory(); // Total memory allocated to the JVM
        long freeMemory = Runtime.getRuntime().freeMemory();   // Free memory within the allocated JVM space
        long usedMemory = totalMemory - freeMemory;             // Used memory within the allocated JVM space
        System.out.println("totalMemory: " + totalMemory);
        System.out.println("usedMemory: " + usedMemory);
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
     * @param gridSize size of a 2 dimensional array with flagged values
     * @param distanceThreshold a number of Manhattan Distance steps to walk for neighbors
     * @param arrayFlagCount count of flagged values in array
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
        logger.info("DENSITY_TUNE_FACTOR = " + DENSITY_TUNE_FACTOR);
        logger.info("isDense: == density > DENSITY_TUNE_FACTOR: " + isDense);
        return isDense;
    }

    /**
     * find all neighbors of true values in 2 dimensional array within
     * distanceThreshold Manhattan Distance of a flagged (true) value
     * the algorithm can be specified as part of the flag data, or
     * determined by calling a function
     * for dense arrays search for flags from every point via flagSearch
     * for sparse arrays fill around every flag via flagFill
     * both algorithms use a two dimensional array of integers to track progress and
     * avoid double counting
     * @param flagData contains
     *                 a list of coordinates of flagged points
     *                 distanceThreshold a number of Manhattan Distance steps to walk for neighbors
     *                 density if we should use dense algorithm, sparse, or test
     *                 colCount grid column length
     *                 rowCount grid row length
     *                 flagCount count of flagged values
     * @param performTest if a validation test should be performed
     * @param memSaver if the dense algorithm should use a chunked based approach saving memory
     * @return count of cells falling within distanceThreshold of true values in array
     */
    private static int getNeighbors(FlagValues flagData,
                                    boolean performTest,
                                    boolean memSaver) {
        boolean isSparse;
        if (flagData.density == FlagValues.Density.TEST) {
            isSparse = !arrayIsDense(flagData.colCount * flagData.rowCount,
                    flagData.distanceThreshold, flagData.flagCount);
        } else {
            isSparse = flagData.density == FlagValues.Density.SPARSE;
            logger.info ("assuming grid density is " + (isSparse ? "sparse" : "dense"));
        }

        int neighborCount = isSparse ? ScanFlagFill.flagFill(flagData) :
                ScanMultiPass.flagScan(flagData, memSaver);
        if (performTest) {
            logger.info("executing test");
            int altCount = isSparse ? ScanMultiPass.flagScan(flagData, memSaver) :
                    ScanFlagFill.flagFill(flagData);
            if (altCount != neighborCount) {
                logger.error("Primary " + neighborCount +" and Alternate " + altCount +" counts do not match");
            }
            logger.info("Alternate Count: " + altCount);
        }
        return neighborCount;
    }
}
