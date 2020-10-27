import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;


public class SmartMinerCO2 {
	
	static int prunedNodesWithUBmax;
	static int prunedNodesWithUBmin;
	static int prunedNodesPreviouslyEnumerated;
	static int outputPatterns;
	static int prunedNodesForMinSingletonSup;
	static long usedMemory;
	static long usedMemoryByDataAndWindowsAndIndex;
	static long countingTime;
	static long LatticeTime;

	public static void main(String[] args) {
	
		//This should be the format of the input file (which is the same output file from the DiscretizerMain program)
		//First line contains the number of data rows in file (int)
		//Second line contains the number of dimensions of Q' (pattern dimensions)
		//Third line contains the details of the Q' dimensions semi-colon separated: The details of each dimension are comma separated
		//and has the following form: dimension-name, min-value, max-value, interval-width, index-of-column-in-this-output-file-starting-from-zero
		//Fourth line contains the header of the column names, comma separated
		//Fifth line is the first data row and all data values are comma separated, each data row is on a separate line
		countingTime = LatticeTime = 0;
		usedMemory = 0;
		usedMemoryByDataAndWindowsAndIndex = 0;
		prunedNodesWithUBmax = 0;
		prunedNodesWithUBmin = 0;
		prunedNodesPreviouslyEnumerated= 0;
		outputPatterns = 0;
		prunedNodesForMinSingletonSup = 0;
		if(args.length<10)
		{
			System.out.println("Error! Missing input arguments.");
			printUsage();
			return;
		}
		
		String dataPath = args[0];
		String outputPath = args[1];
		String [] anomalyThresholds = args[2].split(",");
		if(anomalyThresholds.length<3)
		{
			System.out.println("Error! Please specify the two anomaly threshold values and a window length, and use a negative value for the threshold you want to ommit if any.");
			printUsage();
			return;
		}
		double anomalyStandardSummationThreshold = Double.parseDouble(anomalyThresholds[0]);
		double anomalyPercentageThreshold = Double.parseDouble(anomalyThresholds[1]);
		int windowLength = Integer.parseInt(anomalyThresholds[2]);
		if(anomalyPercentageThreshold<0 && anomalyStandardSummationThreshold<0)
		{
			System.out.println("Error! Cannot use a negative value for both anomaly thresholds!");
			printUsage();
			return;
		}
		int lag = Integer.parseInt(args[3]);
		double kFunctionThreshold = Double.parseDouble(args[4]);
		double patternSupportThreshold = Double.parseDouble(args[5]);
		int minLength = Integer.parseInt(args[6]);
		int maxLength = Integer.parseInt(args[7]);
		int countingMethod = Integer.parseInt(args[8]);
		int isMinSupPruningON = Integer.parseInt(args[9]);
		if(countingMethod<1 ||countingMethod>2)
		{
			System.out.println("Error! Counting method should either be 1 for linear scan or 2 for using state graph counting!");
			printUsage();
			return;
		}
		if(isMinSupPruningON<0 ||isMinSupPruningON>1)
		{
			System.out.println("Error! The flag for appplying minsup pruning using singletons should either be 1 (to prune) or 0 (to turn it off)");
			printUsage();
			return;
		}
		String outputFileName = "SmartMinerCO2_"+Double.toString(anomalyStandardSummationThreshold)+"_"+Integer.toString(lag)+"_"+Double.toString(anomalyPercentageThreshold)+"_"+Double.toString(kFunctionThreshold)+"_"+Integer.toString(windowLength)+"_"+Integer.toString(minLength)+"_"+Integer.toString(maxLength);
		
		/****************************************************************************************************/
		/*********************************		Load Data From File		*************************************/
		/****************************************************************************************************/
		System.out.println("Loading data from file..\n");
		PatternDimension [] dimensionsInfo = null;
		EngineReadingCO2 [] allReadings = null;
		//Start reading data file and loading data into memory
		try {
			FileReader file_to_read = new FileReader(dataPath);
		
			BufferedReader bfRead =  new BufferedReader(file_to_read);
			String inLine = "";
			//Read first line: number of data rows in file
			int rowsNum = Integer.parseInt(bfRead.readLine());
			allReadings = new EngineReadingCO2[rowsNum];
			
			//Read second line: number of dimensions of Q' (pattern dimensions)
			int dimensionsNum = Integer.parseInt(bfRead.readLine());
			
			//Read third line: details of the Q' dimensions semi-colon separated: The details of each dimension are comma separated
			//and has the following form: dimension-name, min-value, max-value, interval-width, index-of-column-in-this-output-file-starting-from-zero
			inLine = bfRead.readLine();
			dimensionsInfo = new PatternDimension[dimensionsNum];
			String [] dimensionsStrings = inLine.split(";");
			for(int i=0;i<dimensionsNum;i++)
			{
				dimensionsInfo[i] = new PatternDimension();
				String [] currentDimension = dimensionsStrings[i].split(",");
				dimensionsInfo[i].name = currentDimension[0];
				dimensionsInfo[i].minValue = Double.parseDouble(currentDimension[1]);
				dimensionsInfo[i].maxValue = Double.parseDouble(currentDimension[2]);
				dimensionsInfo[i].intervalWidth = Double.parseDouble(currentDimension[3]);
				dimensionsInfo[i].indexInInputFile = Integer.parseInt(currentDimension[4]);
				if(currentDimension.length>5) //then there are explicit intervals
				{
					String [] intervals = currentDimension[5].split("#");
					for(int l=0;l<intervals.length;l++)
					{
						String [] currentIntervalRanges= intervals[l].split("-");
						dimensionsInfo[i].intervals.add(new EventInterval(Double.parseDouble(currentIntervalRanges[0]),Double.parseDouble(currentIntervalRanges[1])));
					}
				
				}
			}
			
			//Read fourth line: the header containing the column names, comma separated
			String header = bfRead.readLine();
			
			//Start reading rows from data file, data values of a single row are comma separated
			int rowIndex = 0;
			while((inLine=bfRead.readLine())!=null)
			{
				String [] values = inLine.split(",");
				allReadings[rowIndex] = new EngineReadingCO2();
				
				allReadings[rowIndex].Iteration = Long.parseLong(values[0]);
				allReadings[rowIndex].year = Integer.parseInt(values[1]);
				allReadings[rowIndex].month = Integer.parseInt(values[2]);
				allReadings[rowIndex].day = Integer.parseInt(values[3]);
				allReadings[rowIndex].Latitude = Double.parseDouble(values[4]);
				allReadings[rowIndex].Longitude = Double.parseDouble(values[5]);
				//allReadings[rowIndex].SCRoutppm = Double.parseDouble(values[6]);
				//allReadings[rowIndex].SCRoutgps = Double.parseDouble(values[7]);
				allReadings[rowIndex].co2gps = Double.parseDouble(values[6]);
				allReadings[rowIndex].Bkpwr = Double.parseDouble(values[7]);
				allReadings[rowIndex].pass = Integer.parseInt(values[8]);
				allReadings[rowIndex].patternDimensionsValues = new String[dimensionsNum];
				
				for(int j=0;j<dimensionsNum;j++)
				{
					
					allReadings[rowIndex].patternDimensionsValues[j] = values[j+9]; //because there are 10 basic parameters before the pattern dimenisons in the file
				}
				rowIndex++;
			}//end while not end of file
			
			bfRead.close();
			
			
		} catch (IOException e) {
			System.out.println("Error! Cannot open/close data file.");
			printUsage();
			e.printStackTrace();
			return;
		}
		
		
		//augment output file name with pattern dimensions names:
		for(int i = 0;i<dimensionsInfo.length;i++)
		{
			outputFileName+="_"+dimensionsInfo[i].name;
		}
		outputFileName+=".txt";
		System.out.println("Done loading data.\n");
		System.out.println("**********************************");
		
		long nowUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		if(nowUsed>usedMemory)
			usedMemory = nowUsed;
		usedMemoryByDataAndWindowsAndIndex = usedMemory;
		
		long start = System.currentTimeMillis();
		
		/****************************************************************************************************/
		/*********************************	Find All Anomalous Windows	*************************************/
		/****************************************************************************************************/
		System.out.println("Finding anomalous windows..\n");
		
		int totalAnomaliesCount = 0;
		
		//Find all anomalous windows of length between Lmin and Lmax and store them in Linked hash table in order of length
		LinkedHashMap<String, TemporalWindow> anomalousWindows = null;
		
		if(anomalyPercentageThreshold<0)
			anomalousWindows = findAllAnomalousWindowsWithStandardThreshold(allReadings,anomalyStandardSummationThreshold,windowLength);
		else
			anomalousWindows = findAllAnomalousWindowsWithSummationAndPercentageThresholds(allReadings,anomalyStandardSummationThreshold,anomalyPercentageThreshold,windowLength);
		
		totalAnomaliesCount = anomalousWindows.size();
		
		System.out.println("Total anomalies count = "+totalAnomaliesCount);
		System.out.println("**********************************");
		
		
		
		//open output file
		FileWriter file_to_write;
		BufferedWriter bfWrite;
		try {
			file_to_write = new FileWriter(outputPath+outputFileName);
			bfWrite =  new BufferedWriter(file_to_write);
			bfWrite.write("Total anomalies count = "+totalAnomaliesCount+"\n");
			bfWrite.write("Total number of data points = "+allReadings.length+"\n");
			/*************************************************************************************************/
			/**************************	Write details of pattern dimensions	to file	**************************/
			/*************************************************************************************************/
			bfWrite.write("Total number of pattern dimensions = "+dimensionsInfo.length+"\n");
			for(int i=0;i<dimensionsInfo.length;i++)
			{
				if(i==0)
				{
					bfWrite.write(dimensionsInfo[i].name+","+dimensionsInfo[i].minValue+","+dimensionsInfo[i].maxValue+","+dimensionsInfo[i].intervalWidth+","+(i+10)); //10 here because we are assuming there are already 10 fixed parameters we read in addition to the pattern dimensions (i.e. time, loc, date, Nox in gps and ppm, bkpwr)
					if(dimensionsInfo[i].intervals.size()>0)
					{
						for(int k=0;k<dimensionsInfo[i].intervals.size();k++)
						{
							if(k==0)
								bfWrite.write(","+dimensionsInfo[i].intervals.get(k).start+"-"+dimensionsInfo[i].intervals.get(k).end);
							else
								bfWrite.write("#"+dimensionsInfo[i].intervals.get(k).start+"-"+dimensionsInfo[i].intervals.get(k).end);
						}
					}
				}
				else
				{
					bfWrite.write(";"+dimensionsInfo[i].name+","+dimensionsInfo[i].minValue+","+dimensionsInfo[i].maxValue+","+dimensionsInfo[i].intervalWidth+","+(i+10)); //10 here because we are assuming there are already 10 fixed parameters we read in addition to the pattern dimensions (i.e. time, loc, date, Nox in gps and ppm, bkpwr)
					if(dimensionsInfo[i].intervals.size()>0)
					{
						for(int k=0;k<dimensionsInfo[i].intervals.size();k++)
						{
							if(k==0)
								bfWrite.write(","+dimensionsInfo[i].intervals.get(k).start+"-"+dimensionsInfo[i].intervals.get(k).end);
							else
								bfWrite.write("#"+dimensionsInfo[i].intervals.get(k).start+"-"+dimensionsInfo[i].intervals.get(k).end);
						}
					}
				}
			}
			bfWrite.write("\n");
			
			
			nowUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			if(nowUsed>usedMemory)
				usedMemory = nowUsed;
			usedMemoryByDataAndWindowsAndIndex = usedMemory;
			
		/****************************************************************************************************/
		/*********************			Discover all Co-occurrence patterns				*********************/
		/****************************************************************************************************/
		
		ArrayList<OutputPattern> topKFunctionValuePatterns = mineCooccurrencePatterns(minLength, maxLength,totalAnomaliesCount,anomalousWindows, lag, allReadings,dimensionsInfo,patternSupportThreshold,kFunctionThreshold,bfWrite,countingMethod,isMinSupPruningON);
		
		//Print top-k Ripley's K-function value patterns
		String outputFileNameForTopKRipleysPatterns =  "SmartMinerCO2_TopKpatternsForRipleysK_"+Double.toString(anomalyStandardSummationThreshold)+"_"+Double.toString(anomalyPercentageThreshold)+"_"+Double.toString(kFunctionThreshold)+"_"+Integer.toString(minLength)+"_"+Integer.toString(maxLength);
		printTopKPatterns(outputPath,outputFileNameForTopKRipleysPatterns,topKFunctionValuePatterns,allReadings.length,lag);
		
		} catch (IOException e) {
			System.out.println("Error! cannot open output file.");
			e.printStackTrace();
			return;
		}
		
		
		long time = System.currentTimeMillis() -start;
		System.out.println("Elapsed time = "+(time/1000.0)+ "sec");
		
		
		try {
			bfWrite.write("Elapsed time = "+(time/1000.0)+ "sec");
			bfWrite.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}//end Main function
	
	
	private static ArrayList<OutputPattern> mineCooccurrencePatterns(int minLength, int maxLength, int totalAnomaliesCount, LinkedHashMap<String, TemporalWindow> anomalousWindows, int lag, EngineReadingCO2[] allReadings, PatternDimension[] dimensionsInfo, double patternSupportThreshold, double kFunctionThreshold, BufferedWriter bfWrite, int countingMethod, int isMinSupPruningON )
	{
		
		
		HashMap<String, String> enumeratedPatternsMap = null;
		
		//ArrayList to store top 100 patterns in descending order in terms of the Ripley's K-function value			
		ArrayList<OutputPattern> topKFunctionValuePatterns = new ArrayList<OutputPattern>();

		
		//Pre-processing time series into state graphs if it will be used in counting
		AllStatesGraph stateGraphs = new AllStatesGraph(dimensionsInfo.length);
		if(countingMethod==2)
			stateGraphs.preprocessTimeSeriesIntoStateGraphs(allReadings);
		
		//Creating dimensionsGraph to be used at every lag distance from each anomalous window
		DimensionsGraph dimGraph = new DimensionsGraph(dimensionsInfo.length);
		dimGraph.initialize();
		//dimGraph.printGraph();
		
		try {
				
			/****************************************************************************************************/
			/*********************	Evaluate all Possible co-occurrences for each window	*********************/
			/****************************************************************************************************/
			

			for(int patLen = minLength;patLen<=maxLength;patLen++)
			{
				enumeratedPatternsMap = new HashMap<String, String>();
				System.out.println("Discovering patterns of length = "+patLen+" from range ["+minLength+" , "+maxLength+"]\n");
				System.out.println("**************************************************************************************");
				
				int minStart = -1; //to avoid enumerating a set of state sequences at the same indices multiple times if they fall within lag distance of more than one anomalous window
				int windowCount = 0;
				for(String key : anomalousWindows.keySet())
				 {
					windowCount++;
					//System.out.println("# of output patterns so far = "+outputPatterns);
					//System.out.println("# of pruned nodes with UBmax so far = "+prunedNodesWithUBmax);
					//System.out.println("# of pruned nodes with UBmin so far = "+prunedNodesWithUBmin);
					//System.out.println("# of pruned nodes with min singleton support = "+prunedNodesForMinSingletonSup);
					//System.out.println("# of pruned nodes previously enumerate so far = "+prunedNodesPreviouslyEnumerated+"\n");
					System.out.println("Processing Window "+windowCount+" out of "+totalAnomaliesCount+" for patterns of length "+patLen);
					 String [] indexes = key.split(",");
			        int windowFrom = Integer.parseInt(indexes[0]);
			        int windowTo = Integer.parseInt(indexes[1]);
			        
			        //Enumerate all patterns which started 0 to lag seconds before the start of this anomalous window
			        for(int lagIndex = lag;lagIndex>=0;lagIndex--)
			        {
			        	//start of pattern
			        	int from = windowFrom - lagIndex;
			        	//end of pattern
			        	int to = from + patLen -1;
			        	//Avoid enumerating patterns with indices beyond the start and end of timeseries
			        	if(from<0 || minStart>=from || to>=allReadings.length)
			        		continue;
			        	
		        		minStart = from;
			        	
			        	//Avoid enumerating patterns that are in a different pass than the current anomalous window
			        	if((allReadings[from].pass!=allReadings[windowFrom].pass) || (allReadings[from].day!=allReadings[windowFrom].day) || (allReadings[from].month!=allReadings[windowFrom].month) || (allReadings[from].year!=allReadings[windowFrom].year) || (allReadings[windowFrom].Iteration-allReadings[from].Iteration>lag))
							continue; //to move the start of the window forward to the new pass, day, month or year.
						 
			        	//MMMMMMMMMMMMM			Enumerate all possible combinations of pattern dimensions	MMMMMMMMMMMMM//
			        					        
			        	// create a deep copy of dimensions graph for the lag distance from the anomalous window
			        	DimensionsGraph curDimGraph = dimGraph.deepClone();
	
			        	//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM//
			        	//MMMMMMMMMMMM	 generate all singleton patterns of the window [from,to] and	MMMMMMMMMMMM// 
			        	//MMMMMMMMMMMM 		update counts in curDimGraph.leavesPatternWithAnomalyCount	MMMMMMMMMMMM//
			        	//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM//
			        	for (int i =  0; i <dimensionsInfo.length; i++) 
				        {
				            enumerateSingleton(i, windowFrom, windowTo, from, to, lag, anomalousWindows,allReadings,dimensionsInfo,enumeratedPatternsMap,curDimGraph,countingMethod,stateGraphs,patternSupportThreshold,totalAnomaliesCount,topKFunctionValuePatterns,kFunctionThreshold,bfWrite);
				        }
			        	
			        	//Start top-down Breadth first traversal of graph
			        	LinkedList<DimensionNode> queue = new LinkedList<DimensionNode>();
			        	queue.add(curDimGraph.root);
			        	while(queue.size()>0)
			        	{
			        		DimensionNode n = queue.removeFirst(); //to make a linked list work like a queue
			        		enumerate_with_UB_pruning(curDimGraph,n,windowFrom, windowTo, from, to, lag, anomalousWindows,allReadings,dimensionsInfo,enumeratedPatternsMap,countingMethod,stateGraphs,patternSupportThreshold,totalAnomaliesCount,topKFunctionValuePatterns,kFunctionThreshold,bfWrite,queue,isMinSupPruningON);
			        	}
			        	
			        					        
			        }// end for all lag values 
			        	
			    }//end for each anomalous window in LinkedHashMap

				System.gc();
				long nowUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				if(nowUsed>usedMemory)
					usedMemory = nowUsed;

	
			}//end for each pattern length between Lmin and Lmax
			
			bfWrite.write("# of output patterns = "+outputPatterns+"\n");
			System.out.println("# of output patterns = "+outputPatterns+"\n");
			
			bfWrite.write("# of pruned nodes with UBmax = "+prunedNodesWithUBmax+"\n");
			System.out.println("# of pruned nodes with UBmax = "+prunedNodesWithUBmax+"\n");
			
			bfWrite.write("# of pruned nodes with UBmin = "+prunedNodesWithUBmin+"\n");
			System.out.println("# of pruned nodes with UBmin = "+prunedNodesWithUBmin+"\n");
			
			bfWrite.write("# of pruned nodes with min singleton support <minsup= "+prunedNodesForMinSingletonSup+"\n");
			System.out.println("# of pruned nodes with min singleton support <minsup = "+prunedNodesForMinSingletonSup+"\n");
			
			bfWrite.write("Total Used Memory = "+usedMemory+ " Bytes\n");
			bfWrite.write("Used Memory By Data and Windows = "+usedMemoryByDataAndWindowsAndIndex+ " Bytes\n");
			System.out.println("Total Used Memory = "+usedMemory+ " Bytes");
			System.out.println("Used Memory By Data and Windows = "+usedMemoryByDataAndWindowsAndIndex+ " Bytes");
			
			bfWrite.write("# of pruned nodes because they were enumerated in previous window = "+prunedNodesPreviouslyEnumerated+"\n");
			System.out.println("# of pruned nodes because they were enumerated in previous window = "+prunedNodesPreviouslyEnumerated+"\n");
			
		}catch (IOException e) {
			System.out.println("Error! SmartMinerCO2 could not out pattern information to output file!");
			e.printStackTrace();
		}
			
		return topKFunctionValuePatterns;
	}


	private static void enumerate_with_UB_pruning(DimensionsGraph curDimGraph,
		DimensionNode node, int windowFrom, int windowTo, int from, int to,
		int lag, LinkedHashMap<String, TemporalWindow> anomalousWindows,
		EngineReadingCO2[] allReadings, PatternDimension[] dimensionsInfo,
		HashMap<String, String> enumeratedPatternsMap, int countingMethod,
		AllStatesGraph stateGraphs, double patternSupportThreshold,
		int totalAnomaliesCount,
		ArrayList<OutputPattern> topKFunctionValuePatterns,
		double kFunctionThreshold, BufferedWriter bfWrite, LinkedList<DimensionNode> queue, int isMinSupPruningON) 
	{
		
		if(node==curDimGraph.root)
		{
			//then no need to calculate UBmin, but directly calculate K-value, and also calculate UBmax using
			//the actual count of the root pattern as the denominator of UBmax
			ArrayList<Integer> includedDimensions = new ArrayList<Integer>();
			Iterator<String> itr = node.dimensions.iterator();
			while(itr.hasNext())
			{
				includedDimensions.add(Integer.parseInt(itr.next()));
			}
		
			String pattern = expandPattern(allReadings,includedDimensions, from, to,dimensionsInfo);
			
			
			if(!enumeratedPatternsMap.containsKey(pattern))
	    	{
			
	    		int [] counts;
				if(countingMethod==1)
					counts = countPatternUsingLinearScan(allReadings, anomalousWindows, includedDimensions, windowFrom, windowTo, from, to, lag);
				else
					counts = stateGraphs.countPatternUsingStateGraphs(allReadings, anomalousWindows, includedDimensions, windowFrom, windowTo, from, to, lag);
				
			
				
				enumeratedPatternsMap.put(pattern, Integer.toString(counts[0])+","+Integer.toString(counts[1]));
				curDimGraph.root.superPatternCount = counts[0];
				 
				int maxSingletonWithAnomalyCount = 0;
				for(int i=0;i<includedDimensions.size();i++)
				{
					int singletonWithAnomalyCount = curDimGraph.leavesPatternWithAnomalyCount[includedDimensions.get(i)];
					if(singletonWithAnomalyCount>maxSingletonWithAnomalyCount)
						maxSingletonWithAnomalyCount = singletonWithAnomalyCount;
				}
				
				double UBmax = (((double)allReadings.length)/totalAnomaliesCount) * ((((double)(maxSingletonWithAnomalyCount)))/(counts[0]));
				
				
				if(UBmax<=kFunctionThreshold) //pattern and its descendants cannot exceed K-function threshold so prune them all
				{
					//all descendants of root are pruned
					//DEBUG//
					//System.out.println("Pruned root with UBmax = "+UBmax);
					//System.out.println("Pattern size = "+node.dimensions.size()+". They are: "+node.dimensions.toString());
					int prunedNum = ((int)Math.pow(2, dimensionsInfo.length)) -2;
					if(prunedNum>0)
						prunedNodesWithUBmax+=prunedNum;
					//DEBUG//
					return;
				}
				else 
				{
					double patternSupport = counts[0]/((double)(allReadings.length));
					if(patternSupport>=patternSupportThreshold)
						//calculate cross-k-function for this pattern
						calculateKFunctionAndOutputIfPatternComplies(counts,allReadings.length,totalAnomaliesCount,bfWrite, topKFunctionValuePatterns,kFunctionThreshold, pattern,patternSupport);
					
					//add all descendant nodes to queue since this root node is their only parent, and propagate to them the superPatternCount
					if(node.dimensions.size()>2) //since children of parents with dim size = 2 are singletons and are already enumerated 
					{
						for(int i=0;i<node.children.size();i++)
						{
							node.children.get(i).superPatternCount = counts[0];
							queue.add(node.children.get(i));
						}
					}
				} //else UBmax greater than K-function threshold
				
	    	}//end if pattern is not already enumerated in enumeratedPatternsMap
			else //else if pattern already enumerated do not do anything in this [from,to] and return directly
			{
				//all descendants of root are pruned, but singletons were already enumerated so they are subtracted
				//DEBUG//
				int prunedNum = ((int)Math.pow(2, dimensionsInfo.length)) -1 - dimensionsInfo.length;
				if(prunedNum>0)
					prunedNodesPreviouslyEnumerated+=prunedNum;
				//DEBUG//
				return;
				
			}
		}//if node was root node of curDimGraph
		else //not root node
		{
			//Create includedDimensions array of node
			ArrayList<Integer> includedDimensions = new ArrayList<Integer>();
			Iterator<String> itr = node.dimensions.iterator();
			while(itr.hasNext())
			{
				includedDimensions.add(Integer.parseInt(itr.next()));
			}

			int minSingletonCount = 0;
			if(isMinSupPruningON==1)
			{
				//calculate minSingletonCount to be used in pruning based on min support of singletons of a pattern
				minSingletonCount =  curDimGraph.leavesPatternCount[includedDimensions.get(0)];
				for(int i=1;i<includedDimensions.size();i++)
				{
					int singletonCount = curDimGraph.leavesPatternCount[includedDimensions.get(i)];
					if(singletonCount<minSingletonCount)
						minSingletonCount = singletonCount;
				}

			}
			//Find Min and Max singletonPatternWithAnomaly counts
			int maxSingletonWithAnomalyCount, minSingletonWithAnomalyCount;
			maxSingletonWithAnomalyCount = minSingletonWithAnomalyCount = curDimGraph.leavesPatternWithAnomalyCount[includedDimensions.get(0)];
			for(int i=1;i<includedDimensions.size();i++)
			{
				int singletonWithAnomalyCount = curDimGraph.leavesPatternWithAnomalyCount[includedDimensions.get(i)];
				if(singletonWithAnomalyCount>maxSingletonWithAnomalyCount)
					maxSingletonWithAnomalyCount = singletonWithAnomalyCount;
				if(singletonWithAnomalyCount<minSingletonWithAnomalyCount)
						minSingletonWithAnomalyCount = singletonWithAnomalyCount;
			}
			

			//calculate UBmax
			double UBmax = (((double)allReadings.length)/totalAnomaliesCount) * ((((double)(maxSingletonWithAnomalyCount)))/(node.superPatternCount));
			if(UBmax<=kFunctionThreshold) //pattern and its descendants cannot exceed K-function threshold so prune them all
			{
				//DEBUG//
				//System.out.println("Pruned with UBmax = "+UBmax);
				//System.out.println("Pattern size = "+node.dimensions.size()+". They are: "+node.dimensions.toString());
				//System.out.println("Dimensions size of pruned node (and its descendants are also pruned)= "+node.dimensions.size());
				//DEBUG//
				pruneAllDescendants(node);
			}
			else //UBmax > kFunctionThreshold
			{
				//calculate UBmin
				double UBmin = (((double)allReadings.length)/totalAnomaliesCount) * ((((double)(minSingletonWithAnomalyCount)))/(node.superPatternCount));
				if(UBmin<=kFunctionThreshold)
				{
					//DEBUG//
					prunedNodesWithUBmin++;
					//System.out.println("Pruned with UBmin = "+UBmin);
					//System.out.println("Dimensions size of pruned node= "+node.dimensions.size());
					//DEBUG//
					
					//decrement no of parents not-visited for all child nodes and add the child node to queue if this was the last parent
					//Also propagate the superPatternCount to all children
					if(node.dimensions.size()>2) //since children of parents with dim size = 2 are singletons and are already enumerated
					{
						for(int i=0;i<node.children.size();i++)
						{
							if(!node.children.get(i).isPruned)
							{
								DimensionNode child = node.children.get(i);
								if(child.superPatternCount<node.superPatternCount)
									child.superPatternCount = node.superPatternCount;
								child.parentsNum--;
								if(child.parentsNum<=0) //this was last visited parent of this child node
								{
									queue.add(child);
								}
							}//end if child was not pruned
						}//end for all children
					}
					
				}//end of YBmin<=kFunctionThreshold
				else //no pruning took place
				{
					double minSingletonSupport = minSingletonCount/((double)(allReadings.length)); //minSingletonSupport = current pattern maxPatternSupport
					if((isMinSupPruningON==1) && (minSingletonSupport<patternSupportThreshold))
					{
						//DEBUG//
						//System.out.println("minSup pruning of 1 node");
						//DEBUG//
						//prune pattern because it won't pas minsup threshold but enumerate children
						
						prunedNodesForMinSingletonSup++;
						//decrement no of parents not-visited for all child nodes and add the child node to queue if this was the last parent
						//Also propagate the superPatternCount to all children
						if(node.dimensions.size()>2) //since children of parents with dim size = 2 are singletons and are already enumerated
						{
							for(int i=0;i<node.children.size();i++)
							{
								if(!node.children.get(i).isPruned)
								{
									DimensionNode child = node.children.get(i);
									if(child.superPatternCount<node.superPatternCount)
										child.superPatternCount = node.superPatternCount;
									child.parentsNum--;
									if(child.parentsNum<=0) //this was last visited parent of this child node
									{
										queue.add(child);
									}
								}//end if child was not pruned
							}//end for all children
			
						}
					}
					else
					{
						//enumerate pattern and children
						String pattern = expandPattern(allReadings,includedDimensions, from, to,dimensionsInfo);
						
						
						if(!enumeratedPatternsMap.containsKey(pattern)) //pattern not yet enumerated
				    	{
				    		int [] counts;
							if(countingMethod==1)
								counts = countPatternUsingLinearScan(allReadings, anomalousWindows, includedDimensions, windowFrom, windowTo, from, to, lag);
							else
								counts = stateGraphs.countPatternUsingStateGraphs(allReadings, anomalousWindows, includedDimensions, windowFrom, windowTo, from, to, lag);
							
							enumeratedPatternsMap.put(pattern, Integer.toString(counts[0])+","+Integer.toString(counts[1]));
					
							//DEBUG//
							//System.out.println("UBmax = "+UBmax+" versus UBmin = "+UBmin+" versus K-func = "+kValue);
							//System.out.println("Pattern size = "+node.dimensions.size()+". They are: "+node.dimensions.toString());	
							//System.out.println("max(Sing,A) ="+maxSingletonWithAnomalyCount+" , min(Sing,A) ="+minSingletonWithAnomalyCount+" , count(c,A) ="+counts[1]);
							//System.out.println("count(superPattern of c) ="+node.superPatternCount+" , count(c) ="+counts[0]+"\n");
							//System.out.println("estimated ratio ="+(minSingletonWithAnomalyCount/((double)(node.superPatternCount)))+" vs. real ratio ="+(counts[1]/((double)(counts[0])))+"\n");
							//DEBUG//
							double patternSupport = counts[0]/((double)(allReadings.length));
							if(patternSupport>=patternSupportThreshold)
								//calculate cross-k-function for this pattern
								calculateKFunctionAndOutputIfPatternComplies(counts,allReadings.length,totalAnomaliesCount,bfWrite, topKFunctionValuePatterns,kFunctionThreshold, pattern,patternSupport);
							
							//decrement no of parents not-visited for all child nodes and add the child node to queue if this was the last parent
							//Also propagate the superPatternCount to all children
							if(node.dimensions.size()>2) //since children of parents with dim size = 2 are singletons and are already enumerated
							{
								for(int i=0;i<node.children.size();i++)
								{
									if(!node.children.get(i).isPruned)
									{
										DimensionNode child = node.children.get(i);
										if(child.superPatternCount<counts[0])
											child.superPatternCount = counts[0];
										child.parentsNum--;
										if(child.parentsNum<=0) //this was last visited parent of this child node
										{
											queue.add(child);
										}
									}//end if child was not pruned
								}//end for all children
				
							}
						}//end if pattern not already enumerated
						else //pattern was found in enumerated list
						{
							pruneDescendantsBecausePreviouslyEnumerated(node);
						}

					}
					
				}//end else no pruning takes place
		
			}//end else UBmax>=kFunctionThreshold

		}//end else if not root node
	}

	private static void pruneDescendantsBecausePreviouslyEnumerated(
			DimensionNode node) {
		//This method propagates through all descendant nodes of the given node and sets their isPruned flag to true

		if(node.isPruned)
			return;
		else
		{
			node.isPruned = true;
			//DEBUG//
			prunedNodesPreviouslyEnumerated++;
			//DEBUG//
			if(node.dimensions.size()>2) //because children of parents of size 2 are singletons and already enumerated
			{
				for(int i=0;i<node.children.size();i++)
				{
					if(!node.children.get(i).isPruned)
					{
						node.children.get(i).isPruned = true;
						//DEBUG//
						prunedNodesPreviouslyEnumerated++;
						//DEBUG//
						pruneFurtherDescendantsRecursivelyBecausePreviouslyEnumerated(node.children.get(i));
					}
					
				}
			}
		}
	}


	private static void pruneFurtherDescendantsRecursivelyBecausePreviouslyEnumerated(
			DimensionNode node) {
		if(node.dimensions.size()>2) //because children of parents of size 2 are singletons and already enumerated
		{
			for(int i=0;i<node.children.size();i++)
			{
				if(!node.children.get(i).isPruned)
				{
					node.children.get(i).isPruned = true;
					//DEBUG//
					prunedNodesPreviouslyEnumerated++;
					//DEBUG//
					pruneFurtherDescendantsRecursivelyBecausePreviouslyEnumerated(node.children.get(i));
				}
			}
		}
		
		
	}


	private static void pruneAllDescendants(DimensionNode node) {
		//This method propagates through all descendant nodes of the given node and sets their isPruned flag to true

		if(node.isPruned)
			return;
		else
		{
			node.isPruned = true;
			//DEBUG//
			prunedNodesWithUBmax++;
			//DEBUG//
			if(node.dimensions.size()>2)
			{
				for(int i=0;i<node.children.size();i++)
				{
					if(!node.children.get(i).isPruned)
					{
						node.children.get(i).isPruned = true;
						//DEBUG//
						prunedNodesWithUBmax++;
						//DEBUG//
						pruneFurtherDescendantsRecursively(node.children.get(i));
					}
					
				}
			}
		}
		
	}


	private static void pruneFurtherDescendantsRecursively(DimensionNode node)
	{
		if(node.dimensions.size()>2)
		{
			for(int i=0;i<node.children.size();i++)
			{
				if(!node.children.get(i).isPruned)
				{
					node.children.get(i).isPruned = true;
					//DEBUG//
					prunedNodesWithUBmax++;
					//DEBUG//
					pruneFurtherDescendantsRecursively(node.children.get(i));
				}
			}
		}
	}


	private static void enumerateSingleton(int dim, int windowFrom, int windowTo, int from, int to, int lag,
			LinkedHashMap<String, TemporalWindow> anomalousWindows, EngineReadingCO2[] allReadings, PatternDimension[] dimensionsInfo, HashMap<String, String> enumeratedPatternsMap, DimensionsGraph curDimGraph, int countingMethod, AllStatesGraph stateGraphs, double minsup, int totalAnomaliesCount, ArrayList<OutputPattern> topKFunctionValuePatterns, double kFunctionThreshold, BufferedWriter bfWrite)
	{
		String pattern = expandSingletonPattern(allReadings, dim,from,to,dimensionsInfo); 
		if(enumeratedPatternsMap.containsKey(pattern)) //already enumerated
		{
			prunedNodesPreviouslyEnumerated++;
			String countsStr = enumeratedPatternsMap.get(pattern);
			String[] counts = countsStr.split(",");
			//counts[0] is number of pattern occurrences, counts[1] is number of pattern occurrences with anomalies
			curDimGraph.leavesPatternWithAnomalyCount[dim] = Integer.parseInt(counts[1]);
			curDimGraph.leavesPatternCount[dim] = Integer.parseInt(counts[0]);
			
		}
		else //pattern was not previously enumerated
		{
			
			//get count(pattern) and count(pattern,anomaly)
			int [] counts;
			ArrayList<Integer> includedDimensions = new ArrayList<Integer>();
			includedDimensions.add(dim);
			if(countingMethod==1)
				counts = countPatternUsingLinearScan(allReadings, anomalousWindows, includedDimensions, windowFrom, windowTo, from, to, lag);
			else
				counts = stateGraphs.countPatternUsingStateGraphs(allReadings, anomalousWindows, includedDimensions, windowFrom, windowTo, from, to, lag);
			
			curDimGraph.leavesPatternCount[dim] = counts[0];
			curDimGraph.leavesPatternWithAnomalyCount[dim] = counts[1];
			enumeratedPatternsMap.put(pattern, Integer.toString(counts[0])+","+Integer.toString(counts[1]));
			double patternSupport = counts[0]/((double)(allReadings.length)); 
			if(patternSupport>=minsup)
			{
				//calculate cross-k-function for this pattern
				calculateKFunctionAndOutputIfPatternComplies(counts,allReadings.length,totalAnomaliesCount,bfWrite, topKFunctionValuePatterns,kFunctionThreshold, pattern,patternSupport);
			}
		}
		
	}


	private static String expandSingletonPattern(EngineReadingCO2[] allReadings,
			int dim, int from, int to, PatternDimension[] dimensionsInfo)
	{
	
		String pattern = "";
    	pattern+=dimensionsInfo[dim].name+":\t";
		for(int patternIndex = from; patternIndex<=to;patternIndex++)
		{
			pattern+=allReadings[patternIndex].patternDimensionsValues[dim];
			if(patternIndex!=to)
				pattern+=" ";
			
		}
		pattern+="\n";
		return pattern;
	}


	private static String expandPattern(EngineReadingCO2[] allReadings,
			ArrayList<Integer> includedDimensions, int from, int to, PatternDimension[] dimensionsInfo)
	{
		
	    String pattern = "";
    	for(int dim = 0;dim<includedDimensions.size();dim++)
		{
    		pattern+=dimensionsInfo[includedDimensions.get(dim)].name+":\t";
    		for(int patternIndex = from; patternIndex<=to;patternIndex++)
    		{
    			pattern+=allReadings[patternIndex].patternDimensionsValues[includedDimensions.get(dim)];
    			if(patternIndex!=to)
    				pattern+=" ";
    			
    		}
    		pattern+="\n";
    	}
    	return pattern;
   
	}

	private static void calculateKFunctionAndOutputIfPatternComplies(
			int[] counts, int length, int totalAnomaliesCount,
			BufferedWriter bfWrite,
			ArrayList<OutputPattern> topKFunctionValuePatterns,
			double kFunctionThreshold, String pattern, double patternSupport) {
	
        double KFunctionDenominator = ((double)counts[0]) * (totalAnomaliesCount); 
        if(KFunctionDenominator>0 && length>0)
        {
        	double kValue = (length * ((double)(counts[1])))/(KFunctionDenominator);
        	double anomalySupport = (totalAnomaliesCount)/((double)length);
        	double patternWithAnomalySupport = counts[1]/((double)length);;
        	if(kValue>kFunctionThreshold)
            {
            	outputPatterns++;
            	try {
            	//Output the pattern
            	bfWrite.write("Co-occurrence Pattern is:\n"+pattern);
	            //Output counts for this pattern
				bfWrite.write("Count(pattern) = "+counts[0]+" ,Count(anomaly) = "+totalAnomaliesCount+" ,Count(pattern,anomaly) = "+counts[1]+" ,Count(Distinct pattern,anomaly) = "+counts[2]+"\n");
				
	            //Output supports for this pattern (i.e. divide counts by length of time series
            	
				bfWrite.write("Support(pattern)="+patternSupport+" ,Support(anomaly) = "+anomalySupport+" ,Support(pattern,anomaly) = "+patternWithAnomalySupport+"\n");
				
	            //Output Ripley's k-function of this pattern
            	bfWrite.write("Ripley's K function value = "+kValue+"\n");
            	double confidence = (counts[2])/((double)(counts[0]));
            	bfWrite.write("Confidence-like measure = "+confidence+"\n");
            	bfWrite.write("**************************************************************************\n");
            	bfWrite.flush();
            	
            	} catch (IOException e) {
            		System.out.println("Error! Could not output pattern to file!");
					e.printStackTrace();
				}
            	//see if we need to add this pattern to the top-k list
            	OutputPattern p = new OutputPattern(pattern, counts[0], counts[1], counts[2], totalAnomaliesCount, kValue);
            	insertIntoTopKRipleysFunctionListIfNeeded(topKFunctionValuePatterns,p);
            	
            }
        }
	}


	private static int[] countPatternUsingLinearScan(EngineReadingCO2[] allReadings, LinkedHashMap<String, TemporalWindow> anomalousWindows, ArrayList<Integer> includedDimensions, int windowFrom, int windowTo, int from, int to, int lag)
	{
			
        //i.e. count the number of occurrences of this pattern in the whole time series allReadings
		//returns an integer array of length 3. 
		//First integer is count of pattern
		//Second integer is count of pattern with anomaly
		//Third integer is count of distinct pattern with anomaly
		
		int [] counts = new int[3];
		counts[0] = counts[1] = counts[2] = 0;
		
		for (int j=0;j<allReadings.length-(to-from);j++) //compare starting from every step in the time instant
        {
        	int timeSeriesPointer = j;
        	boolean isFound=true;
        	                                                                     
        	for(int patternIndex = from; patternIndex<=to;patternIndex++) //compare all time instants/letters in the pattern
        	{
        		for(int dim = 0;dim<includedDimensions.size();dim++) //compare each dimension in the pattern
        		{
        			if(!(allReadings[patternIndex].patternDimensionsValues[includedDimensions.get(dim)].equalsIgnoreCase(allReadings[timeSeriesPointer].patternDimensionsValues[includedDimensions.get(dim)])))
        			{
        				isFound = false;
        				break;
        			}
        		}
        		if(!isFound) break;
        		timeSeriesPointer++;
        	}
        	if(isFound)
        	{
        		counts[0]++;
        		//Search for anomalous windows within lag distance from the start of the pattern and update patternWithAnomalyCount accordingly
        		boolean didPatternOccurWithAnomaly = false;
        		for(int d = 0;d<=lag;d++)
        		{
        			String windowKey = Integer.toString(j+d)+","+Integer.toString(j+d+windowTo-windowFrom);
	            	if(anomalousWindows.containsKey(windowKey)) //corresponding window is anomalous 
	            	{
	            		counts[1]++;
	            		didPatternOccurWithAnomaly = true;
	            		
	            	}
        		}
        		if(didPatternOccurWithAnomaly)
            		counts[2]++;
        	}
        
        }
		
		return counts;
	}


	private static void printTopKPatterns(String outputPath,String outputFileNameForTopKRipleysPatterns, ArrayList<OutputPattern> topKFunctionValuePatterns, int timeSeriesLength, int lag) 
	{
		// Print top k patterns
		FileWriter file_to_write;
		BufferedWriter bfWrite;
		try {
			file_to_write = new FileWriter(outputPath+outputFileNameForTopKRipleysPatterns);
			bfWrite =  new BufferedWriter(file_to_write);
			for(int i=0;i<topKFunctionValuePatterns.size();i++)
			{
				double patternSupport = topKFunctionValuePatterns.get(i).patternCount/((double)timeSeriesLength);
            	double anomalySupport = topKFunctionValuePatterns.get(i).anomalyCount/((double)timeSeriesLength);
            	double patternWithAnomalySupport =topKFunctionValuePatterns.get(i).patternWithAnomalyCount/((double)timeSeriesLength);;
           
				bfWrite.write("Co-occurrence Pattern is:\n"+topKFunctionValuePatterns.get(i).pattern);
	            //Output counts for this pattern
            	bfWrite.write("Count(pattern) = "+topKFunctionValuePatterns.get(i).patternCount+" ,Count(anomaly) = "+topKFunctionValuePatterns.get(i).anomalyCount+" ,Count(pattern,anomaly) = "+topKFunctionValuePatterns.get(i).patternWithAnomalyCount+" ,Count(Distinct pattern,anomaly) = "+topKFunctionValuePatterns.get(i).distinctPatternWithAnomalyCount+"\n");
	            //Output supports for this pattern (i.e. divide counts by length of time series
            	bfWrite.write("Support(pattern)="+patternSupport+" ,Support(anomaly) = "+anomalySupport+" ,Support(pattern,anomaly) = "+patternWithAnomalySupport+"\n");
	            //Output Ripley's K function of this pattern
            	bfWrite.write("Ripley's K function value = "+topKFunctionValuePatterns.get(i).kValue+"\n");
            	double confidence = topKFunctionValuePatterns.get(i).distinctPatternWithAnomalyCount/((double)topKFunctionValuePatterns.get(i).patternCount);
            	bfWrite.write("Confidence-like measure = "+confidence+"\n");
            	bfWrite.write("**************************************************************************\n");
            
			}
			bfWrite.close();
		}
		catch(IOException e)
		{
			System.out.println("Error printing top-k patterns output file!\n");
			e.printStackTrace();
			return;
		}
		
	}

	private static void insertIntoTopKRipleysFunctionListIfNeeded(ArrayList<OutputPattern> topKFunctionValuePatterns, OutputPattern p) 
	{
		if(topKFunctionValuePatterns.size()==0)
    		topKFunctionValuePatterns.add(p);
    	else
    	{
    		if(topKFunctionValuePatterns.size()<100) //then insert this pattern into top k list
    		{
    			int itr;
    			for(itr = 0;itr<topKFunctionValuePatterns.size();itr++)
    			{
    				if(p.kValue>topKFunctionValuePatterns.get(itr).kValue)
    				{
    					topKFunctionValuePatterns.add(itr, p);
    					break;
    				}
    			}
    			if(itr==topKFunctionValuePatterns.size()) //insert it at the end
    				topKFunctionValuePatterns.add(p);
    		}
    		else //top k list contains more than 100 patterns
    		{
    			//if pattern k-value is smaller than or equal to the smallest, ignore it
    			//else insert it into its sorted location and remove the last pattern in the top-k list
    			if(p.kValue>topKFunctionValuePatterns.get(topKFunctionValuePatterns.size()-1).kValue)
    			{
    				int itr;
    				for(itr = 0;itr<topKFunctionValuePatterns.size();itr++)
        			{
        				if(p.kValue>topKFunctionValuePatterns.get(itr).kValue)
        				{
        					topKFunctionValuePatterns.add(itr, p);
        					topKFunctionValuePatterns.remove(topKFunctionValuePatterns.size()-1); //remove last pattern from top-k list
        					break;
        				}
        			}
    			}
    		}
    	}
	}

	
	static LinkedHashMap<String, TemporalWindow> findAllAnomalousWindowsWithStandardThreshold(EngineReadingCO2[] allReadings,double anomalyStandardThreshold,int windowLength)
	{
		LinkedHashMap<String, TemporalWindow> anomalousWindows = new LinkedHashMap<String, TemporalWindow>();
		double bkpwrSum = 0;
		double co2gpsSum = 0;
		//boolean isSCROFF = false;
		//double maxNOX = -1;
			
		for(int i=0,j=i+windowLength-1;((i<=allReadings.length-windowLength) && (j<allReadings.length));i++,j++)
		{
					//check if current window is anomalous
					//However, for a window to be considered, it should satisfy the following conditions:
					//1- start and end of window should be in the same year, month, day and pass
					//2- (End Iteration - start Iteration  + 1)should be  = windowLength
				
					
					if((allReadings[i].pass!=allReadings[j].pass) || (allReadings[i].day!=allReadings[j].day) || (allReadings[i].month!=allReadings[j].month) || (allReadings[i].year!=allReadings[j].year))
						continue; //to move the start of the window forward to the new pass, day, month or year.
					 
					if(allReadings[j].Iteration-allReadings[i].Iteration+1==windowLength)
					{
						bkpwrSum = 0;
						co2gpsSum = 0;
					
						for(int k=i;k<=j;k++)
						{
							co2gpsSum+=allReadings[k].co2gps;
							bkpwrSum+=allReadings[k].Bkpwr;
						}
					
						if(bkpwrSum>0) //check if anomalous
						{
							//MMMMMMMMMMMM	DEBUG	MMMMMMMMMMMMMMMM/
							/*if(bkpwrSum<=0)
							{
								System.out.println("SCRoutgpsSum for current window ="+SCRoutgpsSum+" and will divide it by bkpwrSum.\n");
								System.out.println("bkpwrSum for current window ="+bkpwrSum+" and will divide by it.\n");
							}
							*/
							//MMMMMMMMMMMM	END DEBUG	MMMMMMMMMMMMMMMM/
							double CO2InGPKWHr = (co2gpsSum*3600)/bkpwrSum;
							//if(maxNOX<NOxInGPKWHr)
								//maxNOX = NOxInGPKWHr;
							if(CO2InGPKWHr>anomalyStandardThreshold)
							{
								anomalousWindows.put(Integer.toString(i)+","+Integer.toString(j), new TemporalWindow(allReadings[i].Iteration,allReadings[j].Iteration));
								//since window was anomalous, add 1 to this window length count
							}
						}
					}
		}
	
		//System.out.println("Max NOX in gm/kw-hr = "+maxNOX);
		return anomalousWindows;
		
	}
	
	static LinkedHashMap<String, TemporalWindow> findAllAnomalousWindowsWithSummationAndPercentageThresholds(EngineReadingCO2[] allReadings,double anomalyStandardThreshold,double anomalyPercentageThreshold, int windowLength)
	{
		LinkedHashMap<String, TemporalWindow> anomalousWindows = new LinkedHashMap<String, TemporalWindow>();
		
		double bkpwrSum = 0;
		double co2gpsSum = 0;
		
		for(int i=0,j=i+windowLength-1;((i<=allReadings.length-windowLength) && (j<allReadings.length));i++,j++)
		{
					//check if current window is anomalous
					//However, for a window to be considered, it should satisfy the following conditions:
					//1- start and end of window should be in the same year, month, day and pass
					//2- (End Iteration - start Iteration  + 1)should be  = windowLength
				
					
					if((allReadings[i].pass!=allReadings[j].pass) || (allReadings[i].day!=allReadings[j].day) || (allReadings[i].month!=allReadings[j].month) || (allReadings[i].year!=allReadings[j].year))
						continue; //to move the start of the window forward to the new pass, day, month or year.
					 
					if(allReadings[j].Iteration-allReadings[i].Iteration+1==windowLength)
					{
						bkpwrSum = 0;
						co2gpsSum = 0;
					
						//double max = allReadings[i].SCRoutgps, min = allReadings[i].SCRoutgps;
						for(int k=i;k<=j;k++)
						{
							
							//if(allReadings[k].SCRoutgps>max)
								//max = allReadings[k].SCRoutgps;
							//if(allReadings[k].SCRoutgps<min)
								//min = allReadings[k].SCRoutgps;
							co2gpsSum+=allReadings[k].co2gps;
							bkpwrSum+=allReadings[k].Bkpwr;
						}
						
						if((bkpwrSum>0) && (allReadings[i].co2gps>0) && (allReadings[j].co2gps>allReadings[i].co2gps)) //check if anomalous and percentage of Co2 increased
						{
							double increasePercentage = ((allReadings[j].co2gps-allReadings[i].co2gps)/allReadings[i].co2gps)*100;
							//double increasePercentage = ((max-min)/min)*100;
							double CO2InGPKWHr = (co2gpsSum*3600)/bkpwrSum;
							boolean isAnomalous = false;
							if(anomalyStandardThreshold>=0 && anomalyPercentageThreshold>=0 && (CO2InGPKWHr>anomalyStandardThreshold) && (increasePercentage>anomalyPercentageThreshold))
								isAnomalous = true;
							else if((anomalyStandardThreshold<0) && (anomalyPercentageThreshold>=0) && (increasePercentage>anomalyPercentageThreshold))
								isAnomalous = true;
							if(isAnomalous)
							{
								anomalousWindows.put(Integer.toString(i)+","+Integer.toString(j), new TemporalWindow(allReadings[i].Iteration,allReadings[j].Iteration));
								//since window was anomalous, add 1 to this window length count
							}
						}
				}
			}
		return anomalousWindows;
	}
	
	static void printUsage()
	{
		System.out.println("Please enter all the following arguments on the command line, space separated");
		System.out.println("*****************************************************************************");
		System.out.println("1- Path of the input discretized data file including the file name.");
		System.out.println("2- Path of the output patterns file exclusing the file name");
		System.out.println("3- Anomalous Window standard threshold (summation threshold),Percentage threshold,Anomalous Window Length in seconds\n");
		System.out.println("To ommit applying any of the two thresholds please use a negative value in its place. Note that you cannot use negative values for both\n");
		System.out.println("4- Lag in seconds (i.e. an integer lag value between the start of pattern and the start of an anomalous window).");
		System.out.println("5- Ripley's K function threhold.");
		System.out.println("6- Pattern Support Threshold");
		System.out.println("7- Minimum window length (Lmin).");
		System.out.println("8- Maximum window length (Lmax).");
		System.out.println("9- Integer indicating the pattern counting method to be used (i.e. 1 for linear scan and 2 for using state graphs).");
		System.out.println("10- Integer which is either 1 to turn ON minsup pruning using minimum support among all singletons of a pattern, or 0 to turn it OFF.");
	}

}
