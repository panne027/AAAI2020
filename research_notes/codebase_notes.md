# Code:
## DiscretizerMain:
- *Input*:
	- Parameters by Harish: 
		- *Abs Path*/Discretizer100kC34NVC.csv, 
		- *Abs Path*/Discretizer100kC34Columns.csv, 
		- *Abs Path*/out100kC7Delta1NVC.csv, 
		- 7, 
		- engrpm,800,1800,100,11;EGRkgph,0,250,25,12;Airinkgph,100,900,80,13;RailMPa,90,200,11,14;IntakeT,40,80,4,15;IntakekPa,0,200,20,16;Fuelconskgph,0,45,4,17;

	- Path to run in personal system: `/Users/anirudhagarwal/Documents/Fall 2020/AAAI2020`
	- `Test20kDiscretizercolumns2.csv`: Column name file
(**Iteration, Year, Month, Day, Latitude, Longitude, RelativeError, NOxTheoryppm, 
AbsError, pass, engrpm, EGRkgph, MSPhum, EngTq**)

	- `Test100kDiscretizer2train.csv`: OBD file

- *Output*:
	- `out100ktest34.csv`

- *Rough code flow:*
	**Initializing Variables**
	- args: data_path,column_path,outpath, 7 (input dimensions), (7 configurations separated by ;)
	- Gives output of **discretizedEvents**
	- Initialises an array of class `PatternDimension` called *dimensionsInfo* (length being that of *inputDimensionsNum*:
		- No behavior
		- By default property values ("", -1, -1, 0, 0, empty_arraylist)
		- Properties (name, indexInInputFile [index of column in data file], intervalwidth, min and max values, ArrayList of class `EventInterval`):
			- No behavior
			- Property (start, end) // open-end interval (value of end not in interval)
	- PatternDimension variables (property name) defined for all data columns till *pass* (10 columns)
	- Variable *inputDimensions* stores all ';' seperated system arguments
	- Check if length of *inputDimensions* == 3rd system argument
	- Processing each *inputDimensions* in a for loop:
		- Each is split into 5 parts, in an array called *dimLine* (length has to be atleast 4)
		- Using this loop to fill previously defined PatternDimension array called *dimensionsInfo*
		- first 4 values of system argument are (**name, minVale, maxValue and intervalwidth, colnumber**)
		- If system arguments *dimLine* had length > 5 (meant that **explicit event intervals**) were provided.
		- Intervals are provided as 5th part of *dimLine* i.e. last system argument, and they follow the format (,a-b#c-d#d-6), which are further assigned to *intervals* property of *dimensionsInfo[i]*
	- Meanwhile all columns names are read from the columns file in a loop
		- In this loop, assign *indexInInputFile* property of PatternDimension variables of all data columns
		- Also, remember the data columns for each explicit PatternDimension variables were not created initially (they were a part of *dimensionsInfo* array)
		- In this loop, the *indexInInputFile* property is also defined for which ever data columns (after the *pass* column) provided in system arguments, that is also present in columns file.

	**Begin Discretizing**
	- To start discretizing events create an arrayList of class `EngineReading` called *allReadings*:
		- Each instances parses and stores 10 data column values (till *pass*)
	- A loop is run to read and parse each line from training data:
		- all 10 data columns are read and parsed (three error columns are **clipped at 0**)
		- An *EngineReading* instance stores all values as properties
		- A loop is run for a property called *patternDimensionsValues*:
			- All other data columns (provided in system arguments and saved in *dimensionsInfo* variable) are then read and parsed from data line
			- If: dimensions among (*NOxTheoryppm, SCRoutgps, Bkpwr, SCRinppm, SCRingps*) and value < 0, then it is clipped at 0
			- If: intervals not explicitly defined (size of intervals property = 0) for that dimension (can tell by *intervals* property of *dimensionsInfo*)
				- then value assigned to *patternDimensionsValues[j]* = (val - min_value) / intervalwidth
			- Else If: *intervalwidth* for a dimension defined as negative (means that explicit intervals need to be defined instead of interval width)
				- In this case a loop is run for each value in dimensionsInfo property *intervals*
					- If for any of EventInterval, the val in >= start and < end, then that index of interval becomes equal to *patternDimensionsValues[j]*
				- If value does not lie within any of intervals, then throw an error
			- Else: (Both intervalwidth and intervals are defined)
				- if: val < first_interval.start
					- *patternDimensionsValues[j]* = (val - min_value) / intervalwidth
				- elif: val >= last_interval.end
					- temp = (val - last_interval.end) / intervalwidth
					- *patternDimensionsValues[j]* = `temp + Num_of_intervals + ceil(first_interval.start - min_value) / intervalwidth`
				- else: (some explicit interval would contain the value)
					- Run a loop through all intervals to figure out interval for which val >= start and val < end
						- If found such an interval, then *patternDimensionsValues[j]* = inteval_index +  ceil(first_interval.start - min_value) / intervalwidth
					- if no matches are found in any of the intervals, throw an error
		- Finally, add the EngineReading instance to the ArrayList *allReadings*

	**Writing output**
	- First row: N


## HybridMiner:
- *Input*:
	- Parameters by Harish: 
		- *Abs Path*/out100kC7Delta1NVC.csv: Path of the input discretized data file including the file name.
		- *Abs Path*/: Path of the output patterns file exclusing the file name
		- -1,10,3: Anomalous Window standard threshold (summation threshold),Percentage threshold,Anomalous Window Length in seconds
		(To ommit applying any of the two thresholds please use a negative value in its place. Note that **you cannot use negative values for both**)
		- 1: Lag in seconds (i.e. an integer lag value between the start of pattern and the start of an anomalous window).
		- 1: Ripley's K function threshold.
		- 0.004: Pattern Support Threshold
		- 3: Minimum window length (Lmin).
		- 3: Maximum window length (Lmax). 
		- 1: Integer indicating the pattern counting method to be used (i.e. 1 for linear scan and 2 for using state graphs).
		- 1: Integer which is either 1 to calculate upper bounds with leaves only, or 0 to calculate upper bounds using the joinset counts of the last enumerated level from the bottom.

- *CodeFlow*:
	- Variables to store first three args are: *dataPath*, *outputPath*, & *(anomalyStandardSummationThreshold, anomalyPercentageThreshold, windowLength)*
	- Variables storing other seven arguments are: *lag*, *kFunctionThreshold*, *patternSupportThreshold*, *minLength*, *maxLength*, *countingMethod* & *boundWithLeavesOnly*.
	- Validating values for *(anomalyStandardSummationThreshold, anomalyPercentageThreshold)* both not being -ve, *countingMethod* b/w [1,2] & *boundWithLeavesOnly* b/w [0,1].
	- *boundWithLeavesOnly* = 1 i.e. (BoundWithLeafNodesOnly) & *boundWithLeavesOnly* = 0 i.e. (BoundWithLastBottomLevel)

	**Reading data**:
	- An arrayList created of class `EngineReading` called *allReadings* of length = *rowsNum*
	- Parses and reads all Q dimensions, in an array of class `PatternDimension` called *dimensionsInfo*, with length = *dimensionsNum* 
		- Initialises for each Q dimention following attributes of class `PatternDimension`:
		- *(name, minValue, maxValue, intervalWidth, indexInInputFile)*
		- If length for each Q dimension is > 5 (as split by ';'), then it contains explicit intervals separated by '#' and divide start and end with '-'
		- For these explicit intervals, the *intervals* property is filled with instances of *EventInterval* (using start and end of each interval)
	- Not a loop is run to read all data rows in the array *allReadings*:
		- For each element of array, an instance of `EngineReading` is created
		- Stores 10 values: *(Iteration, year, month, day , Latitude, Longitude, RelativeError, NOxTHeoryppm, AbsError, pass, patternDimensionValues)*
		- The property *patternDimensionValues* is actually an array of string with length = *dimensionsNum* (Storing all Q dimension values at the end)
	- Outfile name is augment with names of all Q dimensions
	- Memory usage is monitored with variables *usedMemory* & *usedMemoryByDataAndWindowsAndIndex* (Using API `Runtime.getRuntime().totalMemory()`)


	**Finding Anomalous Windows:**
	- *totalAnomaliesCount* to maintain count of anomalous windows obtained
	- *anomalousWindows*: LinkedHashMap of String key and value of class `TemporalWindow`
		- Class just stores two long properties with constructor (*startIteration* & *endIteration*)
	- There are two options to finding and saving anomalous windows:
		- if *anomalyPercentageThreshold* < 0 | **findAllAnomalousWindowsWithStandardThreshold():**
			- Created an emtpy LinkedHashMap<String, `TemporalWindow`> call *anomalousWindows*
			- A loop is run covering all windowsfrom start to end of readings:
				- To be considered for anomalous / divergent window, should pass criteria:
					- 1. does not contain any value where SCRoutppm>3000
					- 2. start and end of window should be in the same year, month, day and pass
					- 3. (End Iteration - start Iteration  + 1)should be  = windowLength
				- Check for same pass, day, month and year for start (i) and end (j) of each window
				- Also checks if none of iterations in the window are missing to be considered
				- For all readings in one window, value of NOxTheoryppm compared with *3000*
					- If true for any value, then variable *isSCROFF* is made True
					- Also, for each value in window *AbsErrorSum*, *RelErrorSum*, *SCRoutgpsSum* & *bkpwrSum* are calculated.
				- Then, for the window if *isSCROFF* is False and *AbsErrorSum* > 0:
					- *AbsErrorAvg* is calculated with windowlength as denominator
					- That window is declared anonymous and value stored in *anomalousWindows* as <i,j: TemporalWindow(i.Iteration, j.Iteration)>
			- All *anomalousWindows* are returned from the function

		- else: **findAllAnomalousWindowsWithSummationAndPercentageThresholds():** 
			- Created an emtpy LinkedHashMap<String, `TemporalWindow`> call *anomalousWindows*
			- Exact four conditions as checked as above for window to be anomalous
			- Exactly like above for all readings in one window, value of NOx compared with *3000*
				- If true for any value, then variable *isSCROFF* is made True
				- *SCRoutgpsSum*: older operation *DOUBT*
				- New operation: num of readings counted in window for which *AbsError* > *anomalyPercentageThreshold*
			- If for all readings in a window *AbsError* > *anomalyPercentageThreshold*:
				- *AbsErrorSum* is calculated for all readings
			- **DOUBT** If for a window 4 conditions are True i.e. *isSCROFF* is False, *AbsErrorSum* > 0, First AbsError value of window > 0 and for all readings *AbsError* > *anomalyPercentageThreshold*
				- *AbsErrorAvg* is calculated as average of *AbsErrorSum* 
				- If both *anomalyStandardThreshold* & *anomalyPercentageThreshold* > 0 and *AbsErrorAvg* is greater than both
					- Then Window is anomalous
				- Else If*anomalyStandardThreshold* < 0 & *anomalyPercentageThreshold* > 0  and *AbsErrorAvg* is greater than latter
					- Then also Window is anomalous
				- Only anomalous windows are then added to *anomalousWindows*
			- All *anomalousWindows* are returned from the function

		- *printanomalouswindows* is called and only start i.e. (i) of of each anomalous window is saved in an output file

	**Preprocessing patterns**
	- Before this step, all details such as anomalous window count, Number of data points and No of Q dimensions, follwed by details of Q dimensions are written in the *outputFileName*
	- Updated *usedMemoryByDataAndWindowsAndIndex* variable if *nowUsed* > *usedMemory* till now
	- `mineCooccurrencePatterns` function is called to find co-occurences:
		- Form an emtpy Hashmap *enumeratedPatternsMap*
		- *topKFunctionValuePatterns* is an ArrayList to store top 100 pattern in decreasing order of Ripley's K-function value, of tupe `OutputPattern`:
			- Stores 6 values: *pattern*, *patternCount*, *patternWithAnomalyCount*, *anomalyCount*, *distinctPatternWithAnomalyCount*, *kValue*
		- Instance of `AllStatesGraph` is created to preprocess time-series into state graphs:
			- Constructor takes in input the number of Q-dimensions and declares an empty ArrayList called *stateGraphs* of type `<HashMap<String,ArrayList<Integer>>>`
			- Thus, each state in state graph has a HashMap
			- If *countingMethod*==2, means using state graphs instead of linear scans i.e. *preprocessTimeSeriesIntoStateGraphs()*:
				- for each transition **i-j**, where j=i+1 in reading
					- for each of Q dimension
						- If key (`i-j`) not present in HashMap of that particular Q dimension, then just initialise an ArrayList<Integer> *timelist* and add `i` i.e. start of transition
						- else, just retrieve that value from HashMap and add `i` in already existing *timelist*
			- Also an instance of `HybridDimensionsGraph` is created called *dimGraph*:
				- Constructor takes number of Q dimensions as input
					- Properties *root* & *graphNodes* are assigned = null
					- Properties *nextTopLevel* & *nextBottomLevel* are assigned = -1
					- Properties *leavesPatternWithAnomalyCount* & *leavesPatternCount* are int arrays of size = num of Q dimensions, with 0 values assigned
				- *initialize()* function of this class is called:
					- creates all nodes and parent-child links in the dimensionsGraph
					- *nextTopLevel* = num of Q dimensions - 1 | *nextBottomLevel* = 0
					- *graphNodes* is an Arraylist, each element being an Arraylist of type `HybridDimensionNode`:
						- *dimensions*: LinkedHashSet<String>, *superPatternCount*, *parentsNum*, *childrenNum*, *(parents, children)*: ArrayList<HybridDimensionNode>
						- *isPruned*: (1 if it is pruned by min support threshold | 2 if it is pruned by upper bounds or previously enumerated in another window | 3 if it pruned using both upper bounds and minsupport)
					- *root* is instantiated as `HybridDimensionNode` and it's *dimensions* property are set to index of Q dimensions, using *setDimensions()*

		**Mining Patterns for windows of all lengths**
			- Assuming *dimGraph* is organised as a tree actually
			- for each pattern length
				- for each anomalous window
					- for each pattern from log to start of anomalous window
						- prevent patterns that are outside the bounds of the time-series
						- Also, prevents patterns from different passes and different rides
						- *dimGraph.graphNodes* have levels as `{0:nC1, 1:nC2, ..., n-1:nCn}`
						- for only the singleton patterns i.e. level 0, the function `enumerateSingleton()` is called:
							- all dimensions of node are added in *includedDimensions*
							- the singleton patterns are expanded as (name  space_separated_values) in the variable *pattern*
							- If pattern is already part of *enumeratedPatternsMap*
								- counts are obtain {0: number of pattern occurrences, 1: number of pattern occurrences with anomalies}
								- `support = pattern_occurences / all_readings`
								- If it is below minimum_support, call `pruneAllParents` on that node and return True
									- WRITE ABOUT FUNCTION HERE
							- else, pattern has not been enumerated yet
								- There are two ways of counting the patterns 1) Linear Scan 2) Using State Graphs 
								- Both return 3 counts [1) count of pattern, 2) count of pattern with anomaly, 3) count of distinct pattern with anomaly]
									- In `countPatternUsingStateGraphs()`, first gets all occurences of pattern in state graphs using *from* & *from+1*
										- On each of these occurences, it is compared to entire pattern
										- In these occurences also find if there is an anomalous window within the lag, and enumerate counts for distinct and normal co-occurence of pattern with the anomalous windows
								- Store counts in *curDimGraph.leavesPatternCount* and the pattern along with it's counts in *enumeratedPatternsMap*
								- If support for pattern is > min_support: `calculateKFunction()` else `pruneAllParents()`
									- NEED TO WRITE ABOUT BOTH OF THESE FUNCTIONS

		**Pruning**:
			- After all singleton patterns have been enumerated
			- Top-dwon traversal is performed, with root going first, all *graphNodes* are iterated one by one at each level starting from root @ *nextTopLevel*
			- Each node at each level is sent into `enumerate_with_UB_pruning()` till *nextTopLevel* > *nextBottomLevel*
			- At the same time, each node in bottom-up traversal is sent into `enumerate_with_minsupp_pruning()` @ *nex tBottomLevel*
			- `enumerate_with_UB_pruning()`:
				- Takes one node as input
				- If *ispruned* flag equals 2: pruned by UBmax, not UBmin
				- If node is root:
					- `expandPattern()` is called which basically expands all dimension at that node as `name from_to_values \n next_name from_to_values ...`
					- If *enumeratedPatternsMap* still does not contain expanded pattern of the node
						- counts are obtained for the expanded pattern with the two options available
						- pattern is added to *enumeratedPatternsMap*
						- Also, *curDimGraph.root.superPatternCount* is updated with count[0]
						- Using *leavesPatternWithAnomalyCount* from the time of enumerating all the singleton patterns, *maxSingletonWithAnomalyCount* is found as max of all dimension
						- `UBmax = (all_readings/anomalous_windows) * (maxSingletonWithAnomalyCount / pattern_count_for_node)`
						- if `(UBmax<=kFunctionThreshold)`: means pattern and its descendants cannot exceed K-function threshold so prune them all
						- else:
							- UBmax greater than K-function threshold
							- Calculate pattern_support as `pattern_count / all_readings`
							- if pattern_support > *patternSupportThreshold*
								- `calculateKFunctionAndOutputIfPatternComplies()` is called for this pattern to calculate the values of K-function
									- KFunctionDenominator = pattern_count * anomalous_windows
									- If KFunctionDenominator>0: `Kvalue = (all_readings * count of pattern with anomaly) / KFunctionDenominator`
									- Anomalysupport = anomalous_windows / all_readings
									- patternWithAnomalySupport = count of pattern with anomaly / all_readings
									- If KValue > kFunctionThreshold: then pattern is added among the output patterns
										- `confidence = count of distinct pattern with anomaly / pattern_count`
								- Also, the *superPatternCount* for descendant nodes is updated of the node
					- else:
						- pattern already enumerated, do not do anything and return
				- else:
					- Node is not root
					- Check if `node.isPruned` == 1 i.e. through minsupport:
						- only for nodes that have more than two dimensions (as nodes with two dimensions have children as leaves)
							- Go through all the children of the node
								- If any of the children are not pruned by (either of upper bounds)
									- If for any of child *superPatternCount* < *node.superPatternCount*:
										- Reassign the child's supperPatternCount to that of the node
							- return back
					- else:
						- Means node has not been pruned by minsupport or UBmax
						- Of all dimensions of the node, *maxSingletonWithAnomalyCount* & *minSingletonWithAnomalyCount* are assigned using `curDimGraph.leavesPatternWithAnomalyCount` for all dimensions
						- Caculation of **UBMax**:
							- `if(boundWithLeavesOnly==1 || curDimGraph.nextBottomLevel<2)`:
								- UBMax = (all_readings / anomalous_windows) * (maxSingletonWithAnomalyCount / node.superPatternCount)
							- else:
								- *subsetsList* is defined using the function `printAllSizeKSubsets()` and is basically a combination of all *inputdimensions* of size *curDimGraph.nextBottomLevel*
								- 

- *Output*:
	- number of dimensions of Q (variables to be considered)
	- Has the following form: *dimension-name, min-value, max-value, interval-width, index-of-column-in-this-output-file-starting-from-zero*
	- Input argument *anomalyThresholds*: -1, 10, 3

