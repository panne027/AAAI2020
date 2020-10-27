import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;


public class AnomalousWindowValuePrinterMain {
	
	public static void main(String[] args) {
	
		//This should be the format of the input file (which is the same output file from the DiscretizerMain program)
		//First line contains the number of data rows in file (int)
		//Second line contains the number of dimensions of Q' (pattern dimensions)
		//Third line contains the details of the Q' dimensions semi-colon separated: The details of each dimension are comma separated
		//and has the following form: dimension-name, min-value, max-value, interval-width, index-of-column-in-this-output-file-starting-from-zero
		//Fourth line contains the header of the column names, comma separated
		//Fifth line is the first data row and all data values are comma separated, each data row is on a separate line
		
		if(args.length<6)
		{
			System.out.println("Error! Missing input arguments.");
			printUsage();
			return;
		}
		
		String dataPath = args[0];
		String outputPath = args[1];
		String [] anomalyThresholds = args[2].split(",");
		if(anomalyThresholds.length<2)
		{
			System.out.println("Error! Please specify the two anomaly threshold values, and use a negative value for the one you want to ommit if any.");
			printUsage();
			return;
		}
		double anomalyStandardSummationThreshold = Double.parseDouble(anomalyThresholds[0]);
		double anomalyPercentageThreshold = Double.parseDouble(anomalyThresholds[1]);
		if(anomalyPercentageThreshold<0 && anomalyStandardSummationThreshold<0)
		{
			System.out.println("Error! Cannot use a negative value for both anomaly thresholds!");
			printUsage();
			return;
		}
		double correlationThreshold = Double.parseDouble(args[3]);
		int minLength = Integer.parseInt(args[4]);
		int maxLength = Integer.parseInt(args[5]);
		
		if(minLength!=maxLength)
		{
			System.out.println("Error! Lmin must be same as Lmax!");
			printUsage();
			return;	
		}
		String outputFileName = "randomAnomalousWindowsOfLength_"+Integer.toString(minLength)+"_"+Double.toString(anomalyStandardSummationThreshold)+"_"+Double.toString(anomalyPercentageThreshold)+"_"+Double.toString(correlationThreshold);
		
		/****************************************************************************************************/
		/*********************************		Load Data From File		*************************************/
		/****************************************************************************************************/
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
				if(dimensionsInfo[i].intervalWidth<0)
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
					
					allReadings[rowIndex].patternDimensionsValues[j] = values[j+10]; //because there are 10 basic parameters before the pattern dimenisons in the file
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
		
	
		outputFileName+=".txt";
		long start = System.currentTimeMillis();
		
		/****************************************************************************************************/
		/*********************************	Find All Anomalous Windows	*************************************/
		/****************************************************************************************************/
		//Allocate array anomalousWindowCounts which stores the counts of each anomalous window length
		int [] anomalousWindowCounts = new int[maxLength - minLength+1];
		
		for(int i=0;i<anomalousWindowCounts.length;i++)
			anomalousWindowCounts[i] = 0;
		
		//Find all anomalous windows of length between Lmin and Lmax and store them in Linked hash table in order of length
		LinkedHashMap<String, TemporalWindow> anomalousWindows = null;
		if(anomalyPercentageThreshold<0)
			anomalousWindows = findAllAnomalousWindowsWithStandardThreshold(allReadings,anomalyStandardSummationThreshold,minLength,maxLength,anomalousWindowCounts);
		else
			anomalousWindows = findAllAnomalousWindowsWithSummationAndPercentageThresholds(allReadings,anomalyStandardSummationThreshold,anomalyPercentageThreshold,minLength,maxLength,anomalousWindowCounts);
		
		//MMMMMMMMMMMMMMMM	DEBUG	MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
		//DEBUG: printing counts of each anomalous window length
		int totalAnomaliesCount = 0;
		for(int i=0;i<anomalousWindowCounts.length;i++)
		{	
			totalAnomaliesCount+=anomalousWindowCounts[i];
			System.out.println("# of anomlous windows of length "+(i+minLength)+" = "+anomalousWindowCounts[i]+"\n");
		}
		System.out.println("**********************************");
		System.out.println("Total anomalies count = "+totalAnomaliesCount);
		//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
		
		
		int outputPatterns = 0;
		HashMap<String, String> enumeratedPatternsMap = new HashMap<String, String>();
		
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
			
			/****************************************************************************************************/
			/*********************	Evaluate all Possible co-occurrences for each window	*********************/
			/****************************************************************************************************/
		int num =10; //generate 10 random windows of length minLen
		int [] randomlySelectedWindows = new int [num];
		Random generator = new Random();
		for(int i=0;i<num;i++)
		{
			randomlySelectedWindows[i] = generator.nextInt(totalAnomaliesCount);
		}
		 bfWrite.write("10 Anomalous Windows of length = "+minLength+"\n");
		 bfWrite.write("****************************************************************************\n");
		int windowCount = 0;
		for(String key : anomalousWindows.keySet())
		 {
			
			String [] indexes = key.split(",");
	        int from = Integer.parseInt(indexes[0]);
	        int to = Integer.parseInt(indexes[1]);
	        boolean isSelected = false;
	        for(int i=0;i<num;i++)
	        {
	        	if(randomlySelectedWindows[i]==windowCount)
	        	{
	        		isSelected = true;
	        		break;
	        	}
	        }
	        
	        if(isSelected) //output window info
	        {
	        	
			    
			      //window is now [from,to]
			      bfWrite.write("Window ["+from+","+to+"]\n");
			      bfWrite.write("SCRoutgps: ");
			      for(int j=from;j<=to;j++)
			    	  bfWrite.write(Double.toString(allReadings[j].SCRoutgps)+"\t");
			      bfWrite.write("\n");
			      bfWrite.write("BKPwr: ");
			      for(int j=from;j<=to;j++)
			    	  bfWrite.write(Double.toString(allReadings[j].Bkpwr)+"\t");
			      bfWrite.write("\n");
			      
			      for(int k=0;k<dimensionsInfo.length;k++)
			      {
			    	  bfWrite.write(dimensionsInfo[k].name+": ");
				      for(int j=from;j<=to;j++)
				    	 bfWrite.write(allReadings[j].patternDimensionsValues[k]+"\t");
				      bfWrite.write("\n");  
			      }
			      bfWrite.write("****************************************************************************\n");
			      
	        }
	        
	        windowCount++;
	       }//end for each anomalous window in LinkedHashMap
		 //System.out.println("# of output patterns = "+outputPatterns+"\n");
		
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error! cannot open output file.");
			e.printStackTrace();
			return;
		}
		
		
		
		try {
		
			bfWrite.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	static LinkedHashMap<String, TemporalWindow> findAllAnomalousWindowsWithStandardThreshold(EngineReading[] allReadings,double anomalyStandardThreshold,int minLength,int maxLength, int [] anomalousWindowCounts)
	{
		LinkedHashMap<String, TemporalWindow> anomalousWindows = new LinkedHashMap<String, TemporalWindow>();
		double RelErrorSum = 0;
		//double SCRoutgpsSum = 0;
		boolean isSCROFF = false;
		for(int windowLength = minLength; windowLength<=maxLength;windowLength++)
		{
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
							
							if(allReadings[k].NOxTheoryppm>3000)
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
							double RelErrorAvg = RelErrorSum/windowLength;
							if(RelErrorAvg>anomalyStandardThreshold)
							{
								anomalousWindows.put(Integer.toString(i)+","+Integer.toString(j), new TemporalWindow(allReadings[i].Iteration,allReadings[j].Iteration));
								//since window was anomalous, add 1 to this window length count
								anomalousWindowCounts[windowLength-minLength]++;
							}
						}
					}
			}
		}
		return anomalousWindows;
		
	}
	
	static LinkedHashMap<String, TemporalWindow> findAllAnomalousWindowsWithSummationAndPercentageThresholds(EngineReading[] allReadings,double anomalyStandardThreshold,double anomalyPercentageThreshold, int minLength,int maxLength, int [] anomalousWindowCounts)
	{
		LinkedHashMap<String, TemporalWindow> anomalousWindows = new LinkedHashMap<String, TemporalWindow>();
		
		//double bkpwrSum = 0;
		double RelErrorSum = 0;
		boolean isSCROFF = false;
		for(int windowLength = minLength; windowLength<=maxLength;windowLength++)
		{
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
					
						double max = allReadings[i].NOxTheoryppm, min = allReadings[i].NOxTheoryppm;
						for(int k=i;k<=j;k++)
						{
							
							if(allReadings[k].NOxTheoryppm>3000)
							{
								isSCROFF = true;
								break;
							}
							
							if(allReadings[k].SCRoutgps>max)
								max = allReadings[k].SCRoutgps;
							if(allReadings[k].SCRoutgps<min)
								min = allReadings[k].SCRoutgps;
							RelErrorSum+=allReadings[k].RelativeError;
							//bkpwrSum+=allReadings[k].Bkpwr;
						}
						
						if((!isSCROFF) &&  (RelErrorSum>0) && (allReadings[i].RelativeError>0) && (allReadings[j].RelativeError>allReadings[i].RelativeError)) //check if anomalous
						{
							double increasePercentage = ((allReadings[j].RelativeError-allReadings[i].RelativeError)/allReadings[i].RelativeError)*100;
							//double increasePercentage = ((max-min)/min)*100;
							//double NOxInGPKWHr = (SCRoutgpsSum*3600)/bkpwrSum;
							double RelErrorAvg=RelErrorSum/windowLength;
							boolean isAnomalous = false;
							if(anomalyStandardThreshold>=0 && anomalyPercentageThreshold>=0 && (RelErrorAvg>anomalyStandardThreshold) && (increasePercentage>anomalyPercentageThreshold))
								isAnomalous = true;
							else if((anomalyStandardThreshold<0) && (anomalyPercentageThreshold>=0) && (increasePercentage>anomalyPercentageThreshold))
								isAnomalous = true;
							if(isAnomalous)
							{
								anomalousWindows.put(Integer.toString(i)+","+Integer.toString(j), new TemporalWindow(allReadings[i].Iteration,allReadings[j].Iteration));
								//since window was anomalous, add 1 to this window length count
								anomalousWindowCounts[windowLength-minLength]++;
							}
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
		System.out.println("3- Anomalous Window standard threshold (summation threshold),Percentage threshold.\n");
		System.out.println("To ommit applying any of the two thresholds please use a negative value in its place. Note that you cannot use negative values for both\n");
		System.out.println("4- Correlation threhold.");
		System.out.println("5- Minimum window length (Lmin).");
		System.out.println("6- Maximum window length (Lmax).");
	}
	
}
