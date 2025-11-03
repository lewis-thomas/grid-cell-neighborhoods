package ManhattanDistance;

import java.util.Arrays;
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
    public static final int MEM_SAVER_ROWS = 500;
    private static Logger logger = LoggerFactory.getLogger(ScanMultiPass.class);
    private record ScanRowsResult(int neighborCount, int flagIndex) {}

    public static int flagScan(FlagValues flagData, boolean memSaver) {
        return memSaver ? flagScan(flagData, MEM_SAVER_ROWS) : flagScan (flagData);
    }

        /**
         * Find all neighbors of true values in 2 dimensional array within
         * distanceThreshold Manhattan Distance of a flagged (true) value
         * For each flagged element we add a value distanceThreshold + 1 to the neighbors array
         * in the corresponding location
         * then we make repeated passes flagScanOne flagging all neighbors of flagged values
         * If all values in the array flagged exit
         * @param flagData an object containing
         *                grid coordinates of flagged values set to true
         *                distanceThreshold a number of Manhattan Distance steps to walk for neighbors
         * @return count of all neighbors within distanceThreshold Manhattan Distance of a flagged (true) value
         */
    public static int flagScan(FlagValues flagData) {
        logger.info("flagging with scan multipass");
        int[][] neighbors = new int[flagData.rowCount][flagData.colCount];
        StringBuilder sb = new StringBuilder();
        boolean hasError = false;
        int neighborCount = 0;
        int gridSize = neighbors.length * neighbors[0].length;
        for (int i = 0; i < flagData.flags.size(); i++) {
            neighbors[flagData.flags.get(i).row()][flagData.flags.get(i).col()] = flagData.distanceThreshold + 1;
        }
        neighborCount = flagData.flags.size();

        logger.info("initial array neighbor count "+ neighborCount);
        logger.debug(GridPrint.printArray(neighbors));
        for (int i = flagData.distanceThreshold; i > 0; i--) {
            neighborCount+= flagScanOne(neighbors, i);
            logger.info("after scan " + (flagData.distanceThreshold - i + 1) + " neighbor count "+ neighborCount);
            logger.debug(GridPrint.printArray(neighbors));
            if (neighborCount == gridSize) {
                logger.info("all elements flagged, exiting");
                break;
            }
        }
        return neighborCount;
    }

    /**
     * Find all neighbors of true values in 2 dimensional array within
     * distanceThreshold Manhattan Distance of a flagged (true) value
     * For each flagged element we add a value distanceThreshold + 1 to the neighbors array
     * in the corresponding location
     * then we make repeated passes flagScanOne flagging all neighbors of flagged values
     * If all values in the array flagged exit
     * @param flagData an object containing
     *                grid coordinates of flagged values set to true
     *                distanceThreshold a number of Manhattan Distance steps to walk for neighbors
     * @return count of all neighbors within distanceThreshold Manhattan Distance of a flagged (true) value
     */
    public static int flagScan(FlagValues flagData, int maxRows) {
        logger.info("flagging with scan multipass maxRows " + maxRows);
        StringBuilder sb = new StringBuilder();
        boolean hasError = false;
        int neighborCount = 0;
        int startRow = 0;
        int maxRowsForGrid = Math.min(flagData.rowCount, maxRows);
        int[][] neighbors = new int[maxRowsForGrid][flagData.colCount];
        int flagStartIndex = 0;
        int neighborOffset = 0;
        while (startRow < flagData.rowCount - 1) {
            if (startRow > 0) {
                logger.debug("calling neighborShift with startRow: " +
                        startRow + " discardRows " + (flagData.distanceThreshold));
                neighborCount -= neighborShift(neighbors, flagData.distanceThreshold);
                logger.debug("neighborCount after neighborShift " + neighborCount);
            }
            int curMaxRows = Math.min(maxRowsForGrid, flagData.rowCount - startRow + neighborOffset);
//            int lastNonDiscardedRow = Math.max(curMaxRows, maxRowsForGrid) + startRow;
            int lastNonDiscardedRow = startRow + maxRowsForGrid - flagData.distanceThreshold - neighborOffset -1;
            logger.debug("calling flagScanRows: flagData.rowCount =" + flagData.rowCount +
                    " startRow = " + startRow + " maxRows = " + curMaxRows +
                    " lastNonDiscardedRow = " + lastNonDiscardedRow +
                    " maxRowsForGrid " + maxRowsForGrid);
            ScanRowsResult rowResult = flagScanRows(flagData, neighbors, neighborOffset,
                    startRow, curMaxRows, lastNonDiscardedRow, flagStartIndex);
            startRow += maxRows - (flagData.distanceThreshold) - neighborOffset;
            logger.debug ("new start row " + startRow);
            logger.debug ("flagScan setting flagStartIndex " + rowResult.flagIndex);
            neighborCount += rowResult.neighborCount;
            flagStartIndex = rowResult.flagIndex;
            neighborOffset = 1;
        }
        return neighborCount;
    }

    /**
     * iterate the flagData flags for startRow through endRow
     * @param flagData
     * @param neighbors
     * @param startRow
     * @param endRow
     * @param flagStartIndex
     * @return
     */
    private static ScanRowsResult flagScanRows(FlagValues flagData,
                                               int[][] neighbors,
                                               int neighborOffset,
                                               int startRow, int rowCount, int lastNonDiscardedRow,
                                               int flagStartIndex) {
        logger.debug("flagScanRows neigborOffset: " + neighborOffset + " startRow " + startRow +
                " rowCount " + rowCount + " flagStartIndex " + flagStartIndex +
                " lastNonDiscardedRow " + lastNonDiscardedRow);
        int flagIndex = flagStartIndex;
        int flagIndexToReturn = flagIndex;
        while (flagIndex < flagData.flags.size()) {
            int row = flagData.flags.get(flagIndex).row();
            int neighborRowIndex = row - startRow + neighborOffset;
            logger.debug("flagIndex " + flagIndex + " has value "
                    + flagData.flags.get(flagIndex).row() + "," + flagData.flags.get(flagIndex).col());
            if (neighborRowIndex >= rowCount) break;
            if (row <= lastNonDiscardedRow) {
                logger.debug("set flagIndexToReturn = flagIndex + 1");
                        flagIndexToReturn = flagIndex + 1;
            }
            logger.debug("neighborRowIndex = row " + row + "- startRow" + startRow + " + neighborOffset " +
                    neighborOffset + " = " + neighborRowIndex);
            neighbors[neighborRowIndex][flagData.flags.get(flagIndex).col()] = flagData.distanceThreshold + 1;
            flagIndex++;
        }
        int neighborCount = flagIndex - flagStartIndex;
        int gridSize = neighbors.length * neighbors[0].length;

        logger.info("initial array neighbor count "+ neighborCount);
        logger.debug(GridPrint.printArray(neighbors));
        for (int i = flagData.distanceThreshold; i > 0; i--) {
            neighborCount+= flagScanOneForRows(neighbors, i, rowCount);
            logger.info("after scan " + (flagData.distanceThreshold - i + 1) + " neighbor count "+ neighborCount);
            logger.debug(GridPrint.printArray(neighbors));
            if (neighborCount == gridSize) {
                logger.info("all elements flagged, exiting");
                break;
            }
        }
        return new ScanRowsResult (neighborCount, flagIndexToReturn);
    }

    /**
     * scan entire two dimensional array
     * for each point if it does not have value set check if there is a neighbor that is flagged.
     * Set the value of each cell to one less than the value of its neighbor
     * @param neighbors the grid used for tracking what is getting set to true
     * @param distanceThreshold the maximum Manhattan Distance to check 0 being self
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

    /**
     * scan entire two dimensional array
     * for each point if it does not have value set check if there is a neighbor that is flagged.
     * Set the value of each cell to one less than the value of its neighbor
     * @param neighbors the grid used for tracking what is getting set to true
     * @param distanceThreshold the maximum Manhattan Distance to check 0 being self
     * @param firstPass flag for if we are on the first pass otherwise the top row is already populated
     * @return the amount of newly flagged cells
     */
    private static int flagScanOneForRows(int[][] neighbors, int distanceThreshold, int rowCount) {
        boolean hasError = false;
        int neighborCount = 0;
        logger.debug("flagScanOneForRows rowCount " + rowCount);
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < neighbors[row].length; col++) {
                if (neighbors[row][col] > distanceThreshold) continue;
                boolean neighborFound = (row > 0 && neighbors[row-1][col] == distanceThreshold + 1) ||
                        (row < neighbors.length -1 && neighbors[row+1][col] == distanceThreshold + 1) ||
                        (col > 0 && neighbors[row][col-1] == distanceThreshold + 1) ||
                        (col < neighbors[row].length -1 && neighbors[row][col+1]  == distanceThreshold + 1);
                int prevVal = neighbors[row][col];
                neighbors[row][col] = neighborFound ? distanceThreshold : prevVal;
                neighborCount += neighborFound && prevVal == 0 ? 1 : 0;
            }
        }
        return neighborCount;
    }
    /**
     *  For very large data sets we only hold a portion of the array in memory. We reuse the array by processing a portion
     *  then assign the bottom row of the old neighbors array to the top of the new one to allow more processing
     *  clear all other rows
     * @param neighbors two dimensional array to hold calculations for a portion of the result
     * @param discardRows count of rows only partially calculated
     */
    private static int neighborShift(int [][] neighbors, int discardRows) {
        int endRow = neighbors.length -1 - discardRows;
        logger.debug("before neighborshift\n " + GridPrint.printArray(neighbors));
        System.arraycopy(neighbors[endRow], 0, neighbors[0], 0, neighbors[endRow].length);
        logger.debug("after arraycopy\n " + GridPrint.printArray(neighbors));
        for (int i = endRow ; i >= 1 ; i--) {
            Arrays.fill(neighbors[i],0);
        }
        logger.debug("after Arrays.fill\n " + GridPrint.printArray(neighbors));
        int flagsToDiscard = 0;
        for (int row = endRow; row < neighbors.length; row++) {
            for (int col = 0; col < neighbors[row].length; col++) {
                flagsToDiscard += neighbors[row][col] > 0 ? 1 : 0;
                neighbors[row][col] = 0;
            }
        }
        logger.debug("after neighborshift\n " + GridPrint.printArray(neighbors));
        return flagsToDiscard;
    }
}
