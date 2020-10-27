import java.awt.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class DiscovererMain {
	//int countTrue = 0;
	static long usedMemory;
	static long usedMemoryByDataAndWindows;
	public static void main(String[] args) {
	
		usedMemory = 0;
		usedMemoryByDataAndWindows = 0;
		//This should be the format of the input file (which is the same output file from the DiscretizerMain program)
		//First line contains the number of data rows in file (int)
		//Second line contains the number of dimensions of Q' (pattern dimensions)
		//Third line contains the details of the Q' dimensions semi-colon separated: The details of each dimension are comma separated
		//and has the following form: dimension-name, min-value, max-value, interval-width, index-of-column-in-this-output-file-starting-from-zero
		//Fourth line contains the header of the column names, comma separated
		//Fifth line is the first data row and all data values are comma separated, each data row is on a separate line
		
		if(args.length<9)
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
		String outputFileName = Double.toString(anomalyStandardSummationThreshold)+"_"+Integer.toString(lag)+"_"+Double.toString(anomalyPercentageThreshold)+"_"+Double.toString(kFunctionThreshold)+"_"+Integer.toString(windowLength)+"_"+Integer.toString(minLength)+"_"+Integer.toString(maxLength);
		
		/****************************************************************************************************/
		/*********************************		Load Data From File		*************************************/
		/****************************************************************************************************/
		System.out.println("Loading data from file..\n");
		PatternDimension [] dimensionsInfo = null;
		EngineReading [] allReadings = null;
		//Start reading data file and loading data into memory
		try {
			FileReader file_to_read = new FileReader(dataPath);
		
			BufferedReader bfRead =  new BufferedReader(file_to_read);
			String inLine = "";
			//Read first line: number of data rows in file
			int rowsNum = Integer.parseInt(bfRead.readLine());
			allReadings = new EngineReading[rowsNum];
			
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
				allReadings[rowIndex] = new EngineReading();
				
				allReadings[rowIndex].Iteration = Long.parseLong(values[0]);
				allReadings[rowIndex].year = Integer.parseInt(values[1]);
				allReadings[rowIndex].month = Integer.parseInt(values[2]);
				allReadings[rowIndex].day = Integer.parseInt(values[3]);
				allReadings[rowIndex].Latitude = Double.parseDouble(values[4]);
				allReadings[rowIndex].Longitude = Double.parseDouble(values[5]);
				allReadings[rowIndex].RelativeError = Double.parseDouble(values[6]);
				allReadings[rowIndex].NOxTheoryppm = Double.parseDouble(values[7]);
				allReadings[rowIndex].Bkpwr = Double.parseDouble(values[8]);
				allReadings[rowIndex].pass = Integer.parseInt(values[9]);
				allReadings[rowIndex].patternDimensionsValues = new String[dimensionsNum];
				
				for(int j=0;j<dimensionsNum;j++)
				{
					
					allReadings[rowIndex].patternDimensionsValues[j] = values[j+10]; //because there are 10 basic parameters before the pattern dimensions in the file
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
		
		long nowUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		if(nowUsed>usedMemory)
			usedMemory = nowUsed;
		usedMemoryByDataAndWindows = usedMemory;
		
		long start = System.currentTimeMillis();
		
		
		/****************************************************************************************************/
		/*********************************	Find All Anomalous Windows	*************************************/
		/****************************************************************************************************/
		System.out.println("Finding anomalous windows..\n");
		
		int totalAnomaliesCount = 0;
		
		//Find all anomalous windows of length between Lmin and Lmax and store them in Linked hash table in order of length
		LinkedHashMap<String, TemporalWindow> anomalousWindows = null;

		if (anomalyPercentageThreshold<0)
			System.out.println("=======1=========");
		else
			System.out.println("=======2========");

		if(anomalyPercentageThreshold<0)
			anomalousWindows = findAllAnomalousWindowsWithStandardThreshold(allReadings,anomalyStandardSummationThreshold,windowLength);
		else
			anomalousWindows = findAllAnomalousWindowsWithSummationAndPercentageThresholds(allReadings,anomalyStandardSummationThreshold,anomalyPercentageThreshold,windowLength);

		totalAnomaliesCount = anomalousWindows.size();
		
		System.out.println("Total anomalies count = "+totalAnomaliesCount);
		for (String key : anomalousWindows.keySet()) {
			System.out.println(key + "::" +anomalousWindows.get(key).startIteration + " " + anomalousWindows.get(key).endIteration);
		}
		System.out.println(anomalousWindows);
		System.out.println("**********************************");
		
		
		int outputPatterns = 0;
		HashMap<String, String> enumeratedPatternsMap = null;
		
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
			bfWrite.write("Total number of pattern 1dimensions = "+dimensionsInfo.length+"\n");
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
			usedMemoryByDataAndWindows = usedMemory;
		/****************************************************************************************************/
		/*********************	Evaluate all Possible co-occurrences for each window	*********************/
		/****************************************************************************************************/
		//ArrayList to store top 20 patterns in descending order in terms of the Ripley's K-function value			
		ArrayList<OutputPattern> topKFunctionValuePatterns = new ArrayList<OutputPattern>();

		AllStatesGraph stateGraphs = new AllStatesGraph(dimensionsInfo.length);
		if(countingMethod==2)
			stateGraphs.preprocessTimeSeriesIntoStateGraphs(allReadings);
	
		for(int patLen = minLength;patLen<=maxLength;patLen++) //cancelled for now
		{
			enumeratedPatternsMap = new HashMap<String, String>();
			System.out.println("Discovering patterns of length = "+patLen+" from range ["+minLength+" , "+maxLength+"]\n");
			System.out.println("**************************************************************************************");
			
			int minStart = -1; //to avoid enumerating a set of state sequences at the same indices multiple times if they fall within lag distance of more than one anomalous window
			int windowCount = 0;
			for(String key : anomalousWindows.keySet())
			 {
				windowCount++;
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
		        	//Avoid enumerating patterns with indices beyond the start and end of time series
		        	if(from<0 || minStart>=from || to>=allReadings.length)
		        		continue;
		        	
	        		minStart = from;
		        	
		        	//Avoid enumerating patterns that are in a different pass than the current anomalous window
		        	if((allReadings[from].pass!=allReadings[windowFrom].pass) || (allReadings[from].day!=allReadings[windowFrom].day) || (allReadings[from].month!=allReadings[windowFrom].month) || (allReadings[from].year!=allReadings[windowFrom].year) || (allReadings[windowFrom].Iteration-allReadings[from].Iteration>lag))
						continue; //to move the start of the window forward to the new pass, day, month or year.
					 
		        	//Enumerate all possible combinations of pattern/co-occurrence dimensions for calculating the support of patterns in the window [from,to]
			        int numOfAllPossibleCooccurrences = (int)(Math.pow(2, dimensionsInfo.length));

			        for (int i =  numOfAllPossibleCooccurrences - 1; i >=1; i--) //generate all possible co-occurrences
			        {
			            ArrayList<Integer> includedDimensions = new ArrayList<Integer>();
			            for (int j=0;j<dimensionsInfo.length;j++) 
			            {
			                if ((i & ((int)(Math.pow(2, j))))>0) //compare dimension j
			                { 
			                    includedDimensions.add(j);
			                }
			            }
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
		            	if(pattern.equals(""))
		            		System.out.println("Empty pattern at window indexes ["+windowFrom+","+windowTo+"] and lag = "+lag+" and pattern at indexes ["+from+", "+to+"]\n");
		            	if(!enumeratedPatternsMap.containsKey(pattern))
		            	{
		            		enumeratedPatternsMap.put(pattern, null);
				            //Calculate patternCount and patternWithAnomalyCount 
				            //i.e. count the number of occurrences of this pattern in the whole time series allReadings
				           int [] counts;
		            		if(countingMethod==1)
		            			counts = countPatternUsingLinearScan(allReadings, anomalousWindows, includedDimensions, windowFrom, windowTo, from, to, lag);
		            		else
		            			counts = stateGraphs.countPatternUsingStateGraphs(allReadings, anomalousWindows, includedDimensions, windowFrom, windowTo, from, to, lag);
		            		
				            //MMMMMMMMMMM	DEBUG	MMMMMMMMMMMMMMMMMMMM//
				            //if(i==numOfAllPossibleCooccurrences-1)
				            	//System.out.println("Super pattern count for this window = "+patternCount+"\n");
			            	//MMMMMMMMMMM	DEBUG	MMMMMMMMMMMMMMMMMMMM//
				            
				            /********	Calculate K-function for current pattern	******************/
				            
				            //denominator = aomalyCount * patternCount
				        	double patternSupport = counts[0]/((double)allReadings.length);
				        	//Discard pattern if its support does not exceed pattern support threshold
				        	if(patternSupport<patternSupportThreshold)
				        		continue;
				        	
				            double KFunctionDenominator = ((double)counts[0]) * (totalAnomaliesCount); 
				            if(KFunctionDenominator>0 && allReadings.length>0)
				            {
				            	double kValue = (allReadings.length * ((double)counts[1]))/(KFunctionDenominator);
				            
				            	double anomalySupport = (totalAnomaliesCount)/((double)allReadings.length);
				            	double patternWithAnomalySupport = counts[1]/((double)allReadings.length);;
				            	if(kValue>kFunctionThreshold)
					            {
					            	outputPatterns++;
					            	//Output the pattern
					            	bfWrite.write("Co-occurrence Pattern is:\n"+pattern);
						            //Output counts for this pattern
					            	bfWrite.write("Count(pattern) = "+counts[0]+" ,Count(anomaly) = "+totalAnomaliesCount+" ,Count(pattern,anomaly) = "+counts[1]+" ,Count(Distinct pattern,anomaly) = "+counts[2]+"\n");
						            //Output supports for this pattern (i.e. divide counts by length of time series
					            	bfWrite.write("Support(pattern)="+patternSupport+" ,Support(anomaly) = "+anomalySupport+" ,Support(pattern,anomaly) = "+patternWithAnomalySupport+"\n");
						            //Output Ripley's k-function of this pattern
					            	bfWrite.write("Ripley's K function value = "+kValue+"\n");
					            	double confidence = counts[2]/((double)counts[0]);
					            	bfWrite.write("Confidence-like measure = "+confidence+"\n");
					            	bfWrite.write("**************************************************************************\n");
					            	bfWrite.flush();
					            	
					            	//see if we need to add this pattern to the top-k list
					            	OutputPattern p = new OutputPattern(pattern, counts[0], counts[1], counts[2], totalAnomaliesCount, kValue);
					            	insertIntoTopKRipleysFunctionListIfNeeded(topKFunctionValuePatterns,p);
					            }
				            }
				                

		            	}//end if pattern is not already enumerated in enumeratedPatternsMap
		            	else //pattern already enumerated
		            	{
		            		if(i ==  numOfAllPossibleCooccurrences-1)
		            			break; //no need to enumerate any subset patterns of this super pattern in the current from-to interval since super pattern is already enumerated
		            		
		            	}
			        } //end for all possible co-occurrences of this window
			        
		        }// end for all lag values 
		        	
		    }//end for each anomalous window in LinkedHashMap
			
			System.gc(); 
			nowUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			if(nowUsed>usedMemory)
				usedMemory = nowUsed;

		}//end for each pattern length between Lmin and Lmax
		
		bfWrite.write("# of output patterns = "+outputPatterns+"\n");
		System.out.println("# of output patterns = "+outputPatterns+"\n");
		
		//Print top-k Ripley's K-function value patterns
		String outputFileNameForTopKRipleysPatterns = "TopKpatternsForRipleysK_"+Double.toString(anomalyStandardSummationThreshold)+"_"+Double.toString(anomalyPercentageThreshold)+"_"+Double.toString(kFunctionThreshold)+"_"+Integer.toString(minLength)+"_"+Integer.toString(maxLength);
		printTopKPatterns(outputPath,outputFileNameForTopKRipleysPatterns,topKFunctionValuePatterns,allReadings.length,lag);
		
		} catch (IOException e) {
			System.out.println("Error! cannot open output file.");
			e.printStackTrace();
			return;
		}
		
		
		long time = System.currentTimeMillis() -start;
		System.out.println("Elapsed time = "+(time/1000.0)+ "sec");
		
		System.out.println("Total Used Memory = "+usedMemory+ " Bytes");
		System.out.println("Used Memory By Data and Windows = "+usedMemoryByDataAndWindows+ " Bytes");
		
		try {
			bfWrite.write("Total Used Memory = "+usedMemory+ " Bytes\n");
			bfWrite.write("Used Memory By Data and Windows = "+usedMemoryByDataAndWindows+ " Bytes\n");
			
			bfWrite.write("Elapsed time = "+(time/1000.0)+ "sec");
			bfWrite.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
    		if(topKFunctionValuePatterns.size()<20) //then insert this pattern into top k list
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
    		else //top k list contains more than 20 patterns
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

	static LinkedHashMap<String, TemporalWindow> findAllAnomalousWindowsWithStandardThreshold(EngineReading[] allReadings,double anomalyStandardThreshold,int windowLength)
	{
		LinkedHashMap<String, TemporalWindow> anomalousWindows = new LinkedHashMap<String, TemporalWindow>();
		//double bkpwrSum = 0;
		//double SCRoutgpsSum = 0;
		double RelErrorSum=0;
		boolean isSCROFF = false;
		//double maxNOX = -1;



		for(int i=0,j=i+windowLength-1;((i<=allReadings.length-windowLength) && (j<allReadings.length));i++,j++)
		{
					//check if current window is anomalous
					//However, for a window to be considered, it should satisfy the following conditions:
					//1- does not contain any value where SCRoutppm>3000
					//2- start and end of window should be in the same year, month, day and pass
					//3- (End Iteration - start Iteration  + 1)should be  = windowLength
				
					
					if((allReadings[i].pass!=allReadings[j].pass) || (allReadings[i].day!=allReadings[j].day) || (allReadings[i].month!=allReadings[j].month) || (allReadings[i].year!=allReadings[j].year))
						continue; //to move the start of the window forward to the new pass, day, month or year.

					if(allReadings[j].Iteration-allReadings[i].Iteration+1==windowLength)
					{
						//bkpwrSum = 0;
						//SCRoutgpsSum = 0;
						RelErrorSum=0;
						isSCROFF = false;
					
						for(int k=i;k<=j;k++)
						{

								if(allReadings[k].RelativeError>40) //threshold
							{
								isSCROFF = true;
								break;
							}
							
							//SCRoutgpsSum+=allReadings[k].SCRoutgps;
							//bkpwrSum+=allReadings[k].Bkpwr;
							RelErrorSum+=allReadings[k].RelativeError;
						}
					
						if((!isSCROFF) &&  (RelErrorSum>0)) //check if anomalous
						{
							//MMMMMMMMMMMM	DEBUG	MMMMMMMMMMMMMMMM/
							/*if(bkpwrSum<=0)
							{
								System.out.println("SCRoutgpsSum for current window ="+SCRoutgpsSum+" and will divide it by bkpwrSum.\n");
								System.out.println("bkpwrSum for current window ="+bkpwrSum+" and will divide by it.\n");
							}
							*/
							//MMMMMMMMMMMM	END DEBUG	MMMMMMMMMMMMMMMM/
							//double NOxInGPKWHr = (SCRoutgpsSum*3600)/bkpwrSum;
							double RelErrorAvg=RelErrorSum/windowLength;
							//if(maxNOX<NOxInGPKWHr)
								//maxNOX = NOxInGPKWHr;
							if(RelErrorAvg>anomalyStandardThreshold)
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
	
	static LinkedHashMap<String, TemporalWindow> findAllAnomalousWindowsWithSummationAndPercentageThresholds(EngineReading[] allReadings,double anomalyStandardThreshold,double anomalyPercentageThreshold, int windowLength)
	{
		LinkedHashMap<String, TemporalWindow> anomalousWindows = new LinkedHashMap<String, TemporalWindow>();
		
		//double bkpwrSum = 0;
		//double SCRoutgpsSum = 0;
		double RelErrorSum=0;
		boolean isSCROFF = false;
		int countTrue = 0;	// newly added
		for(int i=0,j=i+windowLength-1;((i<=allReadings.length-windowLength) && (j<allReadings.length));i++,j++)
		{
					//check if current window is anomalous
					//However, for a window to be considered, it should satisfy the following conditions:
					//1- does not contain any value where SCRoutppm>3000
					//2- start and end of window should be in the same year, month, day and pass
					//3- (End Iteration - start Iteration  + 1)should be  = windowLength
				
					
					if((allReadings[i].pass!=allReadings[j].pass) || (allReadings[i].day!=allReadings[j].day) || (allReadings[i].month!=allReadings[j].month) || (allReadings[i].year!=allReadings[j].year))
						continue; //to move the start of the window forward to the new pass, day, month or year.
					 
					if(allReadings[j].Iteration-allReadings[i].Iteration+1==windowLength)
					{
						RelErrorSum=0;
						//bkpwrSum = 0;
						//SCRoutgpsSum = 0;
						isSCROFF = false;
					
						//double max = allReadings[i].SCRoutgps, min = allReadings[i].SCRoutgps;
						for(int k=i;k<=j;k++)
						{
							if(allReadings[k].NOxTheoryppm>3000)
							{
								isSCROFF = true;
								break;
							}
							
							//if(allReadings[k].SCRoutgps>max)
								//max = allReadings[k].SCRoutgps;
							//if(allReadings[k].SCRoutgps<min)
								//min = allReadings[k].SCRoutgps;
							//SCRoutgpsSum+=allReadings[k].SCRoutgps;
							//bkpwrSum+=allReadings[k].Bkpwr;
							RelErrorSum+=allReadings[k].RelativeError;
						}
						
						if((!isSCROFF) &&  (RelErrorSum>0) && (allReadings[i].NOxTheoryppm>0) && (allReadings[j].NOxTheoryppm>allReadings[i].NOxTheoryppm)) //check if anomalous and percentage of Nox increased
						{
							double increasePercentage = ((allReadings[j].NOxTheoryppm-allReadings[i].NOxTheoryppm)/allReadings[i].NOxTheoryppm)*100;
							//double increasePercentage = ((max-min)/min)*100;
							double RelErrorAvg = RelErrorSum/windowLength;

							boolean isAnomalous = false;
							if(anomalyStandardThreshold>=0 && anomalyPercentageThreshold>=0 && (RelErrorAvg>anomalyStandardThreshold) && (increasePercentage>anomalyPercentageThreshold))
								isAnomalous = true;
							else if((anomalyStandardThreshold<0) && (anomalyPercentageThreshold>=0) && (increasePercentage>anomalyPercentageThreshold))
								isAnomalous = true;
							if(isAnomalous)
							{
								anomalousWindows.put(Integer.toString(i)+","+Integer.toString(j), new TemporalWindow(allReadings[i].Iteration,allReadings[j].Iteration));
								//since window was anomalous, add 1 to this window length count
							}
							/*
							if(anomalyStandardThreshold>=0 && anomalyPercentageThreshold>=0 && (NOxInGPKWHr>anomalyStandardThreshold) && (increasePercentage>anomalyPercentageThreshold))
								countTrue += 1;
							else if((anomalyStandardThreshold<0) && (anomalyPercentageThreshold>=0) && (increasePercentage>anomalyPercentageThreshold))
								countTrue += 1;*/

						}
				}
			}
		System.out.println("countTrue:");
		System.out.println(countTrue);
		return anomalousWindows;
	}
	
	private static int[] countPatternUsingLinearScan(EngineReading[] allReadings, LinkedHashMap<String, TemporalWindow> anomalousWindows, ArrayList<Integer> includedDimensions, int windowFrom, int windowTo, int from, int to, int lag)
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

	
	static void printUsage()
	{
		System.out.println("Please enter all the following arguments on the command line, space separated");
		System.out.println("*****************************************************************************");
		System.out.println("1- Path of the input discretized data file including the file name.");
		System.out.println("2- Path of the output patterns file exclusing the file name");
		System.out.println("3- Anomalous Window standard threshold (summation threshold),Percentage threshold,Anomalous Window Length in seconds\n");
		System.out.println("To omit applying any of the two thresholds please use a negative value in its place. Note that you cannot use negative values for both\n");
		System.out.println("4- Lag in seconds (i.e. an integer lag value between the start of pattern and the start of an anomalous window).");
		System.out.println("5- Ripley's K function threshold.");
		System.out.println("6- Pattern Support Threshold");
		System.out.println("7- Minimum window length (Lmin).");
		System.out.println("8- Maximum window length (Lmax).");
		System.out.println("9- Integer indicating the pattern counting method to be used (i.e. 1 for linear scan and 2 for using state graphs).");
	}
	
}

