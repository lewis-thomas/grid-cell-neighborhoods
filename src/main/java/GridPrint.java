package ManhattanDistance;

/**
 * visualization helper - prints of grid to console
 */
class GridPrint {
    
    /**
     * prints the array to a string as integers for visualization
     * Assumes the array is not jagged
     *
     * @param neighbors the two-dimensional int array
     * @return a string of integers with lines = row count, each lines length = col count
     * */
    public static String printArray(int[][] neighbors) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");

        for (int row = 0; row < neighbors.length; row++) {
            for (int col = 0; col < neighbors[row].length; col++) {
                sb.append ("" + neighbors[row][col]);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
