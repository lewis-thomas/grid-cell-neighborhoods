package ManhattanDistance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ScanFlagFill {

    private static Logger logger = LoggerFactory.getLogger(ScanFlagFill.class);
    /**
     * find all neighbors of true values in 2 dimensional array within
     * distanceThreshold Manhattan Distance of a flagged (true) value
     * by filling around every flag via flagNeighbors
     * @param array a 2 dimensional array that is grid shaped with flagged values set to true
     * @param neighbors a 2 dimensional array to track fills to avoid double counting
     * @param distanceThreshold a number of Manhattan Distance steps to walk for neighbors
     * @return count of cells falling within distanceThreshold of true values in array
     */
    public static int flagFill(FlagValues flagData, int [][] neighbors, int distanceThreshold){
        logger.info("flagging with flag fill");
        int neighborCount = 0;
        for (int i = 0; i < flagData.flags.size(); i++) {
            neighborCount += flagNeighbors(flagData.flags.get(i).row(), flagData.flags.get(i).col(), distanceThreshold, neighbors);
        }
        return neighborCount;
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
}
