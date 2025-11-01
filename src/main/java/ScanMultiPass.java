package ManhattanDistance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ScanMultiPass {
    private static Logger logger = LoggerFactory.getLogger(ScanMultiPass.class);

    public static int flagScan(boolean[][] array, int[][] neighbors, int distanceThreshold) {
        StringBuilder sb = new StringBuilder();
        boolean hasError = false;
        int neighborCount = 0;
        for (int row = 0; row < array.length; row++) {
            for (int col = 0; col < array[row].length; col++) {
                if (array[row][col]) {
                    neighbors[row][col] = distanceThreshold + 1;
                    neighborCount++;
                }
            }
        }
        logger.info("initial array neighbor count "+ neighborCount);
        for (int i = distanceThreshold; i > 0; i--) {
            neighborCount+= flagScanOne(neighbors, i);
            logger.info("after scan neighbor count "+ neighborCount);
        }
        return neighborCount;
    }

    private static int flagScanOne(int[][] neighbors, int distanceThreshold) {
        boolean hasError = false;
        int neighborCount = 0;
        for (int row = 0; row < neighbors.length; row++) {
            for (int col = 0; col < neighbors[row].length; col++) {
                int upVal = row > 0 ? neighbors[row-1][col] : 0;
                int downVal = row < neighbors.length -1 ? neighbors[row+1][col] : 0;
                int leftVal = col > 0 ? neighbors[row][col-1] : 0;
                int rightVal = col < neighbors[row].length -1 ? neighbors[row][col+1] : 0;
                int maxVal = Math.max(Math.max(upVal, downVal), Math.max(leftVal, rightVal));
                int prevVal = neighbors[row][col];
                if (maxVal -1 > prevVal) {
                    neighbors[row][col] = maxVal-1;
                    neighborCount+= prevVal == 0 ? 1 : 0;
                }
            }
        }
        return neighborCount;
    }
}
