	import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class ValuePrinterMain {

		public static void main(String[] args) throws Exception {

		if(args.length<5)
		{
			System.out.println("Error! Missing input arguments.");
			printUsage();
			return;
		}
		
		String dataPath = args[0];
		String headerPath = args[1];
		String outputPath = args[2];
		int inputDimensionsNum = Integer.parseInt(args[3]);
		
		// Allocate array to store the information about Q' dimensions and iteration and anomaly dimensions
		PatternDimension [] dimensionsInfo = new PatternDimension[inputDimensionsNum];
		PatternDimension iterationDim = new PatternDimension();
		iterationDim.name = "Iteration";
		PatternDimension latitudeDim = new PatternDimension();
		iterationDim.name = "Latitude";
		PatternDimension longitudeDim = new PatternDimension();
		iterationDim.name = "Longitude";		
		PatternDimension SCRoutgpsDim = new PatternDimension();
		SCRoutgpsDim.name = "SCRoutgps";
		PatternDimension bkPwrDim = new PatternDimension();
		bkPwrDim.name = "Bkpwr";
		PatternDimension dayDim = new PatternDimension();
		bkPwrDim.name = "Day";
		PatternDimension monthDim = new PatternDimension();
		bkPwrDim.name = "Month";
		PatternDimension yearDim = new PatternDimension();
		bkPwrDim.name = "Year";
		PatternDimension passDim = new PatternDimension();
		bkPwrDim.name = "Pass";
		PatternDimension SCRoutppmDim = new PatternDimension();
		bkPwrDim.name = "SCRoutppm";
				
		String [] inputDimensions = args[4].split(";");
		if(inputDimensions.length<inputDimensionsNum || inputDimensions.length>inputDimensionsNum)
		{
			System.out.println("Error! Number of input dimensions is not equal to the number of dimension names entered.");
			printUsage();
			return;
		}
		
		
		//Reading and parsing input dimensions of Q'
		//char alphabet = 'A';
		
		for(int i=0;i<inputDimensions.length;i++)
		{
			String [] dimLine = inputDimensions[i].split(",");
			if(dimLine.length<3)
			{
				System.out.println("Error! Input dimension cannot be parsed correctly.");
				printUsage();
				return;
			}
			dimensionsInfo[i] = new PatternDimension();
			dimensionsInfo[i].name = dimLine[0];
			dimensionsInfo[i].minValue = Double.parseDouble(dimLine[1]);
			dimensionsInfo[i].maxValue = Double.parseDouble(dimLine[2]);
			dimensionsInfo[i].intervalWidth = Double.parseDouble(dimLine[3]);
			
			if(dimensionsInfo[i].intervalWidth<0) //explicit event intervals are given
			{
				//read the explicit intervals given
				String allIntervals = dimLine[4];
				String [] intervalsStrings = allIntervals.split("#");
				for(int intervalIndex = 0; intervalIndex<intervalsStrings.length;intervalIndex++)
				{
					String [] intervalValues = intervalsStrings[intervalIndex].split("-");
					double start = Double.parseDouble(intervalValues[0]);
					double end = Double.parseDouble(intervalValues[1]);
					dimensionsInfo[i].intervals.add(new EventInterval(start,end));
				}
			}
			
		}
		
		
		//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
		//MMMMMMMMMMMMMMMM	Start Reading from file the Relevant Header columns							MMMMMMMMMMM
		//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
		// We only care for the (Iteration) dimension as well as dimensions in Q' (that were input
		// by user in dimensionsInfo[]) in addition to the output NOx  (SCRoutgps) and brake power (Bkpwr) dimensions.
		
		
		try {
			FileReader file_to_read = new FileReader(headerPath);
		
			BufferedReader bfRead =  new BufferedReader(file_to_read);
			
			String inLine = "";
			if((inLine=bfRead.readLine())!=null)
			{
				String [] columnNames = inLine.split(",");
				for(int i = 0;i<columnNames.length;i++)
				{
					if(columnNames[i].equalsIgnoreCase("Iteration"))
						iterationDim.indexInInputFile = i;
					else if(columnNames[i].equalsIgnoreCase("Day"))
					{
						dayDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("Month"))
					{
						monthDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("Year"))
					{
						yearDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("Pass"))
					{
						passDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("SCRoutppm"))
					{
						SCRoutppmDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("Latitude"))
					{
						latitudeDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("Longitude"))
					{
						longitudeDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("SCRoutgps"))
					{
						SCRoutgpsDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("Bkpwr"))
					{
						bkPwrDim.indexInInputFile = i;
					}
					//I removed the else statement to allow a parameter to be in both the first describing parameters
					//and again in the discretized pattern dimensions parameters					
					for(int j=0;j<dimensionsInfo.length;j++)
					{
						if(columnNames[i].equalsIgnoreCase(dimensionsInfo[j].name))
						{
							dimensionsInfo[j].indexInInputFile = i;
							break;
						}
					}
					
					
				}
				bfRead.close();
				
			}
			else
			{
				System.out.println("Error! Cannot read header line from header file.");
				bfRead.close();
				return;
			}
		
		} catch (IOException e) {
			System.out.println("Error! Cannot open/close data file.");
			printUsage();
			e.printStackTrace();
			return;
		}
		 
		//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
		//MMMMMMMMMMMMMMMM	Start reading from data file and discretizing the events and then	MMMMMMMMMMM
		//MMMMMMMMMMMMMMMM			storing them in memory and in output file					MMMMMMMMMMM
		//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
		ArrayList<EngineReading> allReadings = new ArrayList<EngineReading>();
		
		try {
			FileReader file_to_read = new FileReader(dataPath);
			BufferedReader bfRead =  new BufferedReader(file_to_read);
			
			
			String inLine = "";
			while(((inLine=bfRead.readLine())!=null) && (!inLine.equals("")))
			{
				String [] values = inLine.split(",");
				EngineReading r = new EngineReading();
				r.Iteration = Long.parseLong(values[iterationDim.indexInInputFile]);
				r.day = Integer.parseInt(values[dayDim.indexInInputFile]);
				r.month = Integer.parseInt(values[monthDim.indexInInputFile]);
				r.year = Integer.parseInt(values[yearDim.indexInInputFile]);
				r.Latitude = Double.parseDouble(values[latitudeDim.indexInInputFile]);
				r.Longitude = Double.parseDouble(values[longitudeDim.indexInInputFile]);
				r.SCRoutgps = Double.parseDouble(values[SCRoutgpsDim.indexInInputFile]);
				if(r.SCRoutgps<0) r.SCRoutgps = 0;
				r.SCRoutppm = Double.parseDouble(values[SCRoutppmDim.indexInInputFile]);
				if(r.SCRoutppm<0) r.SCRoutppm = 0;
				r.Bkpwr = Double.parseDouble(values[bkPwrDim.indexInInputFile]);
				if(r.Bkpwr<0) r.Bkpwr = 0;
				r.pass = Integer.parseInt(values[passDim.indexInInputFile]);
				
				//reading the co-occurrence pattern dimensions
				r.patternDimensionsValues = new String[dimensionsInfo.length];
				for(int j=0;j<dimensionsInfo.length;j++)
				{
						double val = Double.parseDouble(values[dimensionsInfo[j].indexInInputFile]);
						//convert double value into a discrete event and save it in r.patternDimensionsValues[j]
						//Note that we did not use the max value in assigning event numbers but we only rely on given interval width
						if(dimensionsInfo[j].name.equalsIgnoreCase("SCRoutppm") || dimensionsInfo[j].name.equalsIgnoreCase("SCRoutgps") || dimensionsInfo[j].name.equalsIgnoreCase("Bkpwr") || dimensionsInfo[j].name.equalsIgnoreCase("SCRinppm") || dimensionsInfo[j].name.equalsIgnoreCase("SCRingps"))
						{
							if(val<0) val = 0;
						}
						
						r.patternDimensionsValues[j] = Double.toString(val);
								
				}
				allReadings.add(r);
			}
			bfRead.close();
		} catch (IOException e) {
			System.out.println("Error! Cannot open/close data file.");
			printUsage();
			e.printStackTrace();
			return;
		}
		
		//MMMMMMMMMMMMMMMMMMM	Write discretized output file	MMMMMMMMMMMMMMMMMMMM
		
		try {
			FileWriter file_to_write;
			file_to_write = new FileWriter(outputPath);
			BufferedWriter bfWrite =  new BufferedWriter(file_to_write);
			
			//This is the format of the output file
			//First line contains the number of data rows in file (int)
			//Second line contains the number of dimensions of Q' (pattern dimensions)
			//Third line contains the details of the Q' dimensions semi-colon separated: The details of each dimension are comma separated
			//and has the following form: dimension-name, min-value, max-value, interval-width, index-of-column-in-this-output-file-starting-from-zero
			//Fourth line contains the header of the column names, comma separated
			//Fifth line is the first data row and all data values are comma separated, each data row is on a separate line
			
			//Line 1:
			bfWrite.write(allReadings.size()+"\n");
			
			
			//Output random values
			
			Random randomGenerator = new Random();
		    
			bfWrite.write("*****************	5 sec windows	********************\n");
			//generating 10 random windows of length 5 sec
			int windowLength = 5;
			for (int i = 0; i < 10; i++)
			{
		      int randomInt = randomGenerator.nextInt(allReadings.size());
		      //window is now [randomInt,randomInt+windowLength-1]
		      bfWrite.write("Window ["+randomInt+","+(randomInt+windowLength-1)+"]\n");
		      bfWrite.write("SCRoutgps: ");
		      for(int j=randomInt;j<randomInt+windowLength;j++)
		    	  bfWrite.write(Double.toString(allReadings.get(j).SCRoutgps)+"\t");
		      bfWrite.write("\n");
		      bfWrite.write("BKPwr: ");
		      for(int j=randomInt;j<randomInt+windowLength;j++)
		    	  bfWrite.write(Double.toString(allReadings.get(j).Bkpwr)+"\t");
		      bfWrite.write("\n");
		      
		      for(int k=0;k<dimensionsInfo.length;k++)
		      {
		    	  bfWrite.write(dimensionsInfo[k].name+": ");
			      for(int j=randomInt;j<randomInt+windowLength;j++)
			    	 bfWrite.write(allReadings.get(j).patternDimensionsValues[k]+"\t");
			      bfWrite.write("\n");  
		      }
		      bfWrite.write("****************************************************************************\n");
			}
		      
		      bfWrite.write("*****************	15 sec windows	********************\n");
		    
		  	//generating 10 random windows of length 5 sec
			
		      windowLength = 15;
				for (int i = 0; i < 10; i++)
				{
			      int randomInt = randomGenerator.nextInt(allReadings.size());
			      //window is now [randomInt,randomInt+windowLength-1]
			      bfWrite.write("Window ["+randomInt+","+(randomInt+windowLength-1)+"]\n");
			      bfWrite.write("SCRoutgps: ");
			      for(int j=randomInt;j<randomInt+windowLength;j++)
			    	  bfWrite.write(Double.toString(allReadings.get(j).SCRoutgps)+"\t");
			      bfWrite.write("\n");
			      bfWrite.write("BKPwr: ");
			      for(int j=randomInt;j<randomInt+windowLength;j++)
			    	  bfWrite.write(Double.toString(allReadings.get(j).Bkpwr)+"\t");
			      bfWrite.write("\n");
			      
			      for(int k=0;k<dimensionsInfo.length;k++)
			      {
			    	  bfWrite.write(dimensionsInfo[k].name+": ");
				      for(int j=randomInt;j<randomInt+windowLength;j++)
				    	 bfWrite.write(allReadings.get(j).patternDimensionsValues[k]+"\t");
				      bfWrite.write("\n");  
			      }
			      bfWrite.write("****************************************************************************\n");
			      
				}
		      
			
		
			bfWrite.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		
		
		
	
		
	}
	
	static void printUsage()
	{
		System.out.println("Please enter all the following arguments on the command line, space separated");
		System.out.println("*****************************************************************************");
		System.out.println("1- Path of the input data file.");
		System.out.println("2- Path of the column header file: a csv file containing comma separated column names.");
		System.out.println("3- Path of the output file containing the discretized event values for the dimensions in Q'.");
		System.out.println("4- Number of dimensions in set Q'.");
		System.out.println("5- Dimensions of Q' in the input data file, separated from each other by semi-colons: each dimension contains:  the name of the parameter,min-value,max-value,interval width,list of intervals if intervals are to beprovided explicitly. e.g. (all info of single dimension should be comma separated).\n");
		System.out.println(" In case of explicitly providing intervals, interval width should be -1 and list of intervals should be separated by # and start and end of interval should be separated by -\n");
		System.out.println(" For example: list of intervals should look like 0-2#2-4#5.5-6.5\n");
		System.out.println(" Important: If explicit intervals are given they should cover all range of values in data from min to max and so the last interval should extend past the max value because each interval is open ended");
	}

}
