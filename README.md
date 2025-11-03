# Neighbors
A Manhattan Distance Calculator.

## Overview

Reads a JSON file consisting of a distanceThreshold and a data array.
Returns the number of cells that fall within distanceThreshold steps of any positive values in the array.

Example: 
```json
{"distanceThreshold":1,
"data": [
[-1, -2, -3, -4],
[-1, -2, -3, -4],
[-4, 5, -6, -7],
[-7, -8, -9, -3]
]
}
```
result: 5

## Build and Run

```
mvn clean package && mvn exec:java -Dexec.args=data5.json
```

### Build and Run With Debug Logging

```bash
mvn clean package && mvn exec:java -Dexec.args=data5.json -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

### Build and Run With no Info Logs

```bash
mvn clean package && mvn exec:java -Dexec.args=data5.json -Dorg.slf4j.simpleLogger.defaultLogLevel=warn
```

### Test

Test by running a series of files with perform_test on which will log an error 
if the two algorithms offer a different answer

```bash
mvn clean package
mvn exec:java -Dexec.args="data1.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data2.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data3.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data4.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data5.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data6.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data7.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data8.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data9.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data10.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data11.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn exec:java -Dexec.args="data12.json -perform_test" -Dorg.slf4j.simpleLogger.defaultLogLevel=error
```
## Solution overview
First data is read in as JSON, and all positive values are added to a coordinate list.
A determination is then made whether the grid should be processed as dense, or sparse.

It is possible to force processing as either dense or sparse by 
adding a density value to the input json file
```json
"density": "dense"
```
or
```json
"density": "sparse"
```
If this is not passed in a calculation will be made based on the proportion
of flagged values to grid size to determine which algorithm to call.

The coordinate list, grid dimensions, and distanceThreshold are passed to
the algorithm used.

A command line argument -perform_test can be used to force both algorithms to run
and then log an error if the results are not identical

## Algorithms in use
There are two algorithms in use, one for general purpose use, and one for very sparse arrays. 

### Multi-Pass Scan
The general purpose solution faster except on very sparse arrays 
performs multiple passes to find cells that fall within distanceThreshold 
steps of any positive values in the input array.

First an integer grid is created with dimensions matching the input grid. 
On the first pass all items from the list of coordinates for flagged values
are set to distanceThreshold + 1.

```json
{"distanceThreshold":2,
  "data": [
    [-1, -2, -3, -4],
    [-1, -2, -3, -4],
    [-4, 5, -6, -7],
    [-7, -8, -9, -3]
  ]
}
```
In this case with distanceThreshold = 2 
set all positive value coordinates to 3.

```text
0000
0000
0300
0000
```
Then make distanceThreshold scans, and for each cell with a zero
check above, below, left, right. Set the cell's value to one less than the
value found.

```text
0000
0200
2320
0200
```
in this case we have a final pass to complete

```text
0100
1210
2321
1210
```
The result is the number of cells that have a non zero value, 11.

#### Memory Saver version
For very large arrays of data we can save memory
at a moderate performance cost by passing `-mem_saver` option.
The multi-pass scan will use a smaller array and process chunks of data.

Two issues to highlight with the chunked version are, 
first it is possible that a flag at the top of one chunk 
has a neighbor at the bottom of the previous chunk.
To ensure that the bottom rows in one chunk are
flagged by the top rows in the next chunk extra rows 
equal to the distanceThreshold are added to the calculation
thus updating the bottom rows in the upper chunk. 
Those rows are currently discarded as they are only partially calculated.

The second issue is to ensure that values from the bottom of 
one chunk propagate to the next. This is done by copying the last row
of the completed chunk into the next chunk array. The values in that row
are not counted, but are used for calculation.

### FlagFill

The second algorithm available walks around each flagged coordinate.
It scans a square of size distanceThreshold around each coordinate,
for distanceThreshold 0 only the square itself. 
It flags all cells within Manhattan Distance of the target,
using integer markings to enable optimizations such as having nearby
neighbors skip flagged areas.
