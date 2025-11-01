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

    /**
     * reads jsonData string finding positive values and adding those coordinates
     * into a FlagValues object
     * @param jsonData
     * @return a FlagValues object with grid dimensions, flag coordinates,
     *  density, and distanceThreshold
     */
    public static FlagValues parseJsonNeighborData(String jsonData) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonData));
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();
        int distanceThreshold = jsonObject.getInt(FIELD_DISTANCE_THRESHOLD, -1);
        String densityParam = jsonObject.getString(FIELD_DENSITY, "test");
        JsonArray dataArray = jsonObject.getJsonArray(FIELD_DATA);
        logger.info("distance threshold: " + distanceThreshold);
        if (dataArray.size()==0) return new FlagValues(0,0,densityParam, distanceThreshold);
        int rows = dataArray.size();
        int cols = dataArray.getJsonArray(0).size();
        FlagValues flagValues = new FlagValues(rows,cols, densityParam, distanceThreshold);

        return parseJsonArray(dataArray, flagValues);
    }
    /**
     * parses JsonArray into FlagValues
     * Assumes the array is not jagged
     * handles both floating point and integer values
     * adds positive value coordinates to a FlagValues object
     * @param jsonArray the input two-dimensional JsonArray
     * @param needsArray whether we need to transform input into a two-dimensional array for processing
     * @return a FlagValues object that contains flagged value positions
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
