package ManhattanDistance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  find all neighbors of true values in 2 dimensional array within
 *  distanceThreshold Manhattan Distance of a flagged (true) value
 *  by making a series of scans flagging every square next to a flagged value
 *  optimized for non sparse arrays because it operates against the entire array
 *  rather than individual flags
 */
class ScanMultiPass {
    private static Logger logger = LoggerFactory.getLogger(ScanMultiPass.class);

    /**
     * Find all neighbors of true values in 2 dimensional array within
     * distanceThreshold Manhattan Distance of a flagged (true) value
     * For each flagged element we add a value distanceThreshold + 1 to the neighbors array
     * in the corresponding location
     * then we make repeated passes flagScanOne flagging all neighbors of flagged values
     * If all values in the array flagged exit
     * @param flagData
     * @param neighbors
     * @param distanceThreshold
     * @return
     */
    public static int flagScan(FlagValues flagData, int[][] neighbors) {
        logger.info("flagging with scan multipass");
        StringBuilder sb = new StringBuilder();
        boolean hasError = false;
        int neighborCount = 0;
        int gridSize = neighbors.length * neighbors[0].length;
        for (int i = 0; i < flagData.flags.size(); i++) {
            neighbors[flagData.flags.get(i).row()][flagData.flags.get(i).col()] = flagData.distanceThreshold + 1;
        }
        neighborCount = flagData.flags.size();

        logger.info("initial array neighbor count "+ neighborCount);
        for (int i = flagData.distanceThreshold; i > 0; i--) {
            neighborCount+= flagScanOne(neighbors, i);
            logger.info("after scan " + (flagData.distanceThreshold - i + 1) + " neighbor count "+ neighborCount);
            if (neighborCount == gridSize) {
                logger.info("all elements flagged, exiting");
                break;
            }
        }
        return neighborCount;
    }
    /**
     * scan entire two dimensional array
     * for each point if it does not have value set check if there is a neighbor that is flagged.
     * Set the value of each cell to one less than the value of its neighbor
     * @param neighbors
     * @param distanceThreshold
     * @return the amount of newly flagged cells
     */
    private static int flagScanOne(int[][] neighbors, int distanceThreshold) {
        boolean hasError = false;
        int neighborCount = 0;
        for (int row = 0; row < neighbors.length; row++) {
            for (int col = 0; col < neighbors[row].length; col++) {
                if (neighbors[row][col] > distanceThreshold) continue;
                boolean neighborFound = (row > 0 && neighbors[row-1][col] == distanceThreshold + 1) ||
                        (row < neighbors.length -1 && neighbors[row+1][col] == distanceThreshold + 1) ||
                        (col > 0 && neighbors[row][col-1] == distanceThreshold + 1) ||
                        (col < neighbors[row].length -1 && neighbors[row][col+1]  == distanceThreshold + 1);
                neighbors[row][col] = neighborFound ? distanceThreshold : 0;
                neighborCount += neighborFound ? 1 : 0;
            }
        }
        return neighborCount;
    }
}
