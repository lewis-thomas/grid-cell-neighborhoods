package ManhattanDistance;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * reads JsonArray input into FlagValues translating positive values into flags
 */
class GridReader {
    private static Logger logger = LoggerFactory.getLogger(GridReader.class);
    private static final String FIELD_DISTANCE_THRESHOLD = "distanceThreshold";
    private static final String FIELD_DENSITY = "density";
    private static final String FIELD_DATA = "data";
    private static final double DENSITY_TUNE_FACTOR = 5.0;

    public static FlagValues parseJsonNeighborData(String jsonData, boolean performTest) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonData));
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();
        int distanceThreshold = jsonObject.getInt(FIELD_DISTANCE_THRESHOLD, -1);
        String densityParam = jsonObject.getString(FIELD_DENSITY, "test");
        JsonArray dataArray = jsonObject.getJsonArray(FIELD_DATA);
        logger.info("distance threshold: " + distanceThreshold);
        if (dataArray.size()==0) return new FlagValues(0,0,densityParam, distanceThreshold, performTest);
        int rows = dataArray.size();
        int cols = dataArray.getJsonArray(0).size();
        FlagValues flagValues = new FlagValues(rows,cols, densityParam, distanceThreshold, performTest);
        logger.debug("useArray: " + flagValues.useArray + " useList: " + flagValues.useList);

        return parseJsonArray(dataArray, flagValues);
    }
    /**
     * parses JsonArray into FlagValues
     * Assumes the array is not jagged
     * handles both floating point and integer values translating positive numbers into 1
     * and non-positive numbers to 0
     * maintains a count of found positive values in class variable flagCount
     * @param jsonArray the input two-dimensional JsonArray
     * @param needsArray whether we need to transform input into a two-dimensional array for processing
     * @return a FlagValues object that can contain
     *  an array of flagged values or
     *  a list of flagged value positions or
     *  both
     * */
    private static FlagValues parseJsonArray(JsonArray jsonArray,
                                             FlagValues flagValues) {
        logger.debug("input data:\n" + jsonArray.toString().replaceAll("],", "],\n"));
        StringBuilder sb = new StringBuilder();
        logger.info("parsing JSON array with rows "+ flagValues.rowCount + " cols " + flagValues.colCount);
        for (int row = 0; row < flagValues.rowCount; row++) {
            JsonArray rowData = jsonArray.getJsonArray(row);
            for (int col = 0; col < flagValues.colCount; col++) {
                if (rowData.getJsonNumber(col).doubleValue()>0) {
                    flagValues.addPoint(row, col);
                }
            }
        }
        return flagValues;
    }

}
