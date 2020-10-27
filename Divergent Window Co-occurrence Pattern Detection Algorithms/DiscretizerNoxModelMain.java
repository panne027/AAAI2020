import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class DiscretizerNoxModelMain {

	public static void main(String[] args) throws Exception {

		if(args.length<5)
		{
			System.out.println("Error! Missing input arguments.");
			printUsage();
			return;
		}
		
		String dataPath = args[0];
		String headerPath = args[1];
		String discretizedEventsPath = args[2];
		int inputDimensionsNum = Integer.parseInt(args[3]);
		
		// Allocate array to store the information about Q' dimensions and iteration and anomaly dimensions
		PatternDimension [] dimensionsInfo = new PatternDimension[inputDimensionsNum];
		PatternDimension iterationDim = new PatternDimension();
		iterationDim.name = "Iteration";
		PatternDimension latitudeDim = new PatternDimension();
		iterationDim.name = "Latitude";
		PatternDimension longitudeDim = new PatternDimension();
		iterationDim.name = "Longitude";		
		
		PatternDimension dayDim = new PatternDimension();
		dayDim.name = "Day";
		PatternDimension monthDim = new PatternDimension();
		monthDim.name = "Month";
		PatternDimension yearDim = new PatternDimension();
		yearDim.name = "Year";
		PatternDimension passDim = new PatternDimension();
		passDim.name = "Pass";
		
		PatternDimension SCRinppmDim = new PatternDimension();
		SCRinppmDim.name = "SCRinppm";
		
		PatternDimension noxByModelDim = new PatternDimension();
		noxByModelDim.name = "NoxByModel";
		
		PatternDimension relativeErrorDim = new PatternDimension();
		relativeErrorDim.name = "RelativeError";
		
		PatternDimension absoluteErrorDim = new PatternDimension();
		absoluteErrorDim.name = "AbsoluteError";
		
		
		//PatternDimension SCRoutgpsDim = new PatternDimension();
		//SCRoutgpsDim.name = "SCRoutgps";
		
		
		//PatternDimension bkPwrDim = new PatternDimension();
		//bkPwrDim.name = "Bkpwr";
		
	
		
		//PatternDimension SCRinppmDim = new PatternDimension();
		//SCRoutppmDim.name = "SCRinppm";
				
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
			if(dimLine.length<4)
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
			
			if(dimLine.length>4) //explicit event intervals are given
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
			/*
			if(alphabet>'Z')
			{
				System.out.println("Error! Note that the number of Q' dimensions is greater than capital alphabet letters.");
				return;
			}
			dimensionsInfo[i].eventsSymbol = alphabet;
			alphabet++;
			*/
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
					else if(columnNames[i].equalsIgnoreCase("Latitude"))
					{
						latitudeDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("Longitude"))
					{
						longitudeDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("SCRinppm"))
					{
						SCRinppmDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("NoxByModel"))
					{
						noxByModelDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("RelativeError"))
					{
						relativeErrorDim.indexInInputFile = i;
					}
					else if(columnNames[i].equalsIgnoreCase("AbsoluteError"))
					{
						absoluteErrorDim.indexInInputFile = i;
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
		ArrayList<EngineReadingNoxModel> allReadings = new ArrayList<EngineReadingNoxModel>();
		
		try {
			FileReader file_to_read = new FileReader(dataPath);
			BufferedReader bfRead =  new BufferedReader(file_to_read);
			
			
			String inLine = "";
			while(((inLine=bfRead.readLine())!=null) && (!inLine.equals("")))
			{
				String [] values = inLine.split(",");
				EngineReadingNoxModel r = new EngineReadingNoxModel();
				r.Iteration = Long.parseLong(values[iterationDim.indexInInputFile]);
				r.day = Integer.parseInt(values[dayDim.indexInInputFile]);
				r.month = Integer.parseInt(values[monthDim.indexInInputFile]);
				r.year = Integer.parseInt(values[yearDim.indexInInputFile]);
				r.Latitude = Double.parseDouble(values[latitudeDim.indexInInputFile]);
				r.Longitude = Double.parseDouble(values[longitudeDim.indexInInputFile]);
				//r.SCRoutgps = Double.parseDouble(values[SCRoutgpsDim.indexInInputFile]);
				//if(r.SCRoutgps<0) r.SCRoutgps = 0;
				r.pass = Integer.parseInt(values[passDim.indexInInputFile]);
				
				r.SCRinppm = Double.parseDouble(values[SCRinppmDim.indexInInputFile]);
				if(r.SCRinppm<0) r.SCRinppm = 0;
				
				r.noxByModel = Double.parseDouble(values[noxByModelDim.indexInInputFile]);
				r.relativeError = Double.parseDouble(values[relativeErrorDim.indexInInputFile]);
				r.absoluteError = Double.parseDouble(values[absoluteErrorDim.indexInInputFile]);

				
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
						if(dimensionsInfo[j].intervals.size()==0) //no explicit intervals are specified
						{
							int currentEvent = ((int) ((val - dimensionsInfo[j].minValue)/dimensionsInfo[j].intervalWidth));
							r.patternDimensionsValues[j] = Integer.toString(currentEvent);
						}
						else if(dimensionsInfo[j].intervalWidth<0) //use explicit intervals only instead of the interval width
						{
							int k;
							for(k = 0;k<dimensionsInfo[j].intervals.size();k++)
							{
								if(val>=dimensionsInfo[j].intervals.get(k).start && val<dimensionsInfo[j].intervals.get(k).end)
								{
									r.patternDimensionsValues[j] = Integer.toString(k);
									break;
								}
							}
							if (k==dimensionsInfo[j].intervals.size()) //this means that the value was not matched to any interval
							{
								throw new Exception("Value = "+val+"of dimension "+dimensionsInfo[j].name+" cannot be found in the explicitly supplied intervals!");
							}
						}
						else //both interval width and explicit intervals are specified
						{
							if(val<dimensionsInfo[j].intervals.get(0).start) //val is smaller than first explicit interval
							{
								int currentEvent = ((int) ((val - dimensionsInfo[j].minValue)/dimensionsInfo[j].intervalWidth));
								r.patternDimensionsValues[j] = Integer.toString(currentEvent);
							}
							else if(val>=dimensionsInfo[j].intervals.get(dimensionsInfo[j].intervals.size()-1).end) //val is larger than last explicit interval notice than end of interval is open so we added the equal sign
							{
								
								int currentEvent = ((int) ((val-dimensionsInfo[j].intervals.get(dimensionsInfo[j].intervals.size()-1).end)/dimensionsInfo[j].intervalWidth));
								/*add number of explicit intervals and intervals before start of first explicit interval*/
								currentEvent += dimensionsInfo[j].intervals.size() + ((int)(Math.ceil((dimensionsInfo[j].intervals.get(0).start - dimensionsInfo[j].minValue)/dimensionsInfo[j].intervalWidth))); 
								r.patternDimensionsValues[j] = Integer.toString(currentEvent);
							}
							else //then some explicit interval must contain val
							{
								int k;
								for(k = 0;k<dimensionsInfo[j].intervals.size();k++)
								{
									if(val>=dimensionsInfo[j].intervals.get(k).start && val<dimensionsInfo[j].intervals.get(k).end)
									{
										int currentEvent = k + ((int)(Math.ceil((dimensionsInfo[j].intervals.get(0).start - dimensionsInfo[j].minValue)/dimensionsInfo[j].intervalWidth)));
										r.patternDimensionsValues[j] = Integer.toString(currentEvent);
										break;
									}
								}
								if (k==dimensionsInfo[j].intervals.size()) //this means that the value was not matched to any interval
								{
									throw new Exception("Value = "+val+"of dimension "+dimensionsInfo[j].name+" cannot be found in the explicitly supplied intervals!");
								}
							}
						}
						
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
			file_to_write = new FileWriter(discretizedEventsPath);
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
			//Line 2:
			bfWrite.write(dimensionsInfo.length+"\n");
			//Line 3:
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
			
			//Line 4: Write header containing column names
			bfWrite.write("Iteration,Year,Month,Day,Latitude,Longitude,pass,SCRinppm,Noxbymodel,Relativeerror,Absoluteerror");
			for(int i=0;i<dimensionsInfo.length;i++)
			{
				bfWrite.write(","+dimensionsInfo[i].name);
			}
			bfWrite.write("\n");
	
			//Line 5 and more: start writing data records to output discretized file
			for(int i=0;i<allReadings.size();i++)
			{
				EngineReadingNoxModel r = allReadings.get(i);
				bfWrite.write(r.Iteration+","+r.year+","+r.month+","+r.day+","+r.Latitude+","+r.Longitude+","+r.pass+","+r.SCRinppm+","+r.noxByModel+","+r.relativeError+","+r.absoluteError);
				for(int j=0;j<dimensionsInfo.length;j++)
					bfWrite.write(","+r.patternDimensionsValues[j]);
				bfWrite.write("\n");
				
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
		System.out.println("5- Dimensions of Q' in the input data file, separated from each other by semi-colons: each dimension contains:  the name of the parameter,min-value,max-value,interval width,list of intervals if intervals are to be provided explicitly. e.g. (all info of single dimension should be comma separated).\n");
		System.out.println("In case of explicitly providing intervals, interval width should be -1 if only explicit intervals should be used or it can be given a numeric value if the explicit intervals will start from a value large than min value or stop at a value smaller than max value.\n");
		System.out.println("The explicit list of intervals should be separated by # and start and end of interval should be separated by -\n");
		System.out.println("For example: list of intervals should look like 0-2#2-4#5.5-6.5\n");
		System.out.println("Important: If explicit intervals are given they should cover all range of values in data from min to max and so the last interval should extend past the max value because each interval is open ended");
	}

}
