import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;


public class RegulationWindowTest {

	public static void main(String[] args) throws Exception {

		long startTime = System.currentTimeMillis();
		
		if(args.length<4)
		{
			System.out.println("Error! Missing input arguments.");
			printUsage();
			return;
		}
		
		String dataPath = args[0];
		String noncompliantWindowPath = args[1];
		double anomalyStandardSummationThreshold = Double.parseDouble(args[2]);
		int windowLength = Integer.parseInt(args[3]);
		
		//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
		//MMMMMMMMMMMMMMMM	Start reading from data file and loading it into arraylist			MMMMMMMMMMM
		//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
		System.out.println("started reading input file..\n");
		ArrayList<EngineReading> allReadings = new ArrayList<EngineReading>();
		
		try {
			FileReader file_to_read = new FileReader(dataPath);
			BufferedReader bfRead =  new BufferedReader(file_to_read);
			
			
			String inLine = "";
			int count = 0;
			while(((inLine=bfRead.readLine())!=null) && (!inLine.equals("")))
			{
				String [] values = inLine.split(",");
				EngineReading r = new EngineReading();
				r.Iteration = Long.parseLong(values[0]);
				r.day = Integer.parseInt(values[4]);
				r.month = Integer.parseInt(values[3]);
				r.year = Integer.parseInt(values[2]);
				r.Latitude = Double.parseDouble(values[8]);
				r.Longitude = Double.parseDouble(values[9]);
				r.SCRoutgps = Double.parseDouble(values[118]);
				if(r.SCRoutgps<0) r.SCRoutgps = 0;
				r.SCRoutppm = Double.parseDouble(values[45]);
				if(r.SCRoutppm<0) r.SCRoutppm = 0;
				r.Bkpwr = Double.parseDouble(values[112]);
				if(r.Bkpwr<0) r.Bkpwr = 0;
				r.pass = Integer.parseInt(values[128]);
				
				
				if(r.Bkpwr<0)
				{
					r.Bkpwr=0;
				}
				
				//reading the co-occurrence pattern dimensions
				r.patternDimensionsValues = new String[values.length];
				for(int j=0;j<values.length;j++)
				{
					r.patternDimensionsValues[j] = values[j];
				}
				allReadings.add(r);
				System.out.println("Read line #"+count);
				count++;
			}
			bfRead.close();
			
			//Now identify all anomalous windows and output them in the output file
		} catch (IOException e) {
			System.out.println("Error! Cannot open/close data file.");
			printUsage();
			e.printStackTrace();
			return;
		}
		
		System.out.println("Done reading input file..");
		System.out.println("Number of points = "+allReadings.size());
		//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
		//MMMMMMMMMMMMMMMM	Find all non-compliant windows			MMMMMMMMMMM
		//MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
		
		System.out.println("Starting to finding non-compliant windows..\n");
		
		int totalAnomaliesCount = 0;
		
		//Find all anomalous windows of length between Lmin and Lmax and store them in Linked hash table in order of length
		LinkedHashMap<String, TemporalWindow> anomalousWindows = null;
		
		
		anomalousWindows = findAllAnomalousWindowsWithStandardThreshold(allReadings,anomalyStandardSummationThreshold,windowLength);
		
		totalAnomaliesCount = anomalousWindows.size();

		System.out.println("Finished finding non-compliant windows..\n");
		
		System.out.println("Total anomalies count = "+totalAnomaliesCount);
		System.out.println("**********************************");
		//MMMMMMMMMMMMMMMMMMM	Write output file containing all non-compliant windowsMMMMMMMMMMMMMMMMMMMM
		
		System.out.println("Started writing output file..\n");
		try {
			FileWriter file_to_write;
			file_to_write = new FileWriter(noncompliantWindowPath);
			BufferedWriter bfWrite =  new BufferedWriter(file_to_write);
			bfWrite.write("Number of points = "+allReadings.size()+"\n");
			bfWrite.write("Number of non-compliant windows = "+totalAnomaliesCount+"\n");
			//Loop through hash table and output all anomalous windows
			for(String key : anomalousWindows.keySet())
			{
				TemporalWindow window = anomalousWindows.get(key);
				bfWrite.write("<"+window.startIteration+","+window.endIteration+">\n");	
			}
			
			
			bfWrite.write("\n");
			long time = System.currentTimeMillis() - startTime;
			bfWrite.write("Elapsed time = "+(time/1000.0)+ "sec\n");
			System.out.println("Elapsed time = "+(time/1000.0)+ "sec");
			bfWrite.close();
			System.out.println("Finished writing output file..\n");
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
		System.out.println("2- Path of the output file containing the non-compliant windows.");
		System.out.println("3- Non-compliance regulation standard threshold.");
		System.out.println("4- Window length.");
		System.out.println("This program assumes that the number of input parmeters in the data file is always constant and same to the number of parameters in the augmenteddatafile");
	}

	static LinkedHashMap<String, TemporalWindow> findAllAnomalousWindowsWithStandardThreshold(ArrayList<EngineReading> allReadings,double anomalyStandardThreshold,int windowLength)
	{
		LinkedHashMap<String, TemporalWindow> anomalousWindows = new LinkedHashMap<String, TemporalWindow>();
		double bkpwrSum = 0;
		double SCRoutgpsSum = 0;
		boolean isSCROFF = false;
		//double maxNOX = -1;
			
		for(int i=0,j=i+windowLength-1;((i<=allReadings.size()-windowLength) && (j<allReadings.size()));i++,j++)
		{
					//check if current window is anomalous
					//However, for a window to be considered, it should satisfy the following conditions:
					//1- does not contain any value where SCRoutppm>3000
					//2- start and end of window should be in the same year, month, day and pass
					//3- (End Iteration - start Iteration  + 1)should be  = windowLength
				
					
					if((allReadings.get(i).pass!=allReadings.get(j).pass) || (allReadings.get(i).day!=allReadings.get(j).day) || (allReadings.get(i).month!=allReadings.get(j).month) || (allReadings.get(i).year!=allReadings.get(j).year))
						continue; //to move the start of the window forward to the new pass, day, month or year.
					 
					if(allReadings.get(j).Iteration-allReadings.get(i).Iteration+1==windowLength)
					{
						bkpwrSum = 0;
						SCRoutgpsSum = 0;
						isSCROFF = false;
					
						for(int k=i;k<=j;k++)
						{
							
							if(allReadings.get(k).SCRoutppm>3000)
							{
								isSCROFF = true;
								break;
							}
							
							SCRoutgpsSum+=allReadings.get(k).SCRoutgps;
							bkpwrSum+=allReadings.get(k).Bkpwr;
						}
					
						if((!isSCROFF) &&  (bkpwrSum>0)) //check if anomalous
						{
							//MMMMMMMMMMMM	DEBUG	MMMMMMMMMMMMMMMM/
							/*if(bkpwrSum<=0)
							{
								System.out.println("SCRoutgpsSum for current window ="+SCRoutgpsSum+" and will divide it by bkpwrSum.\n");
								System.out.println("bkpwrSum for current window ="+bkpwrSum+" and will divide by it.\n");
							}
							*/
							//MMMMMMMMMMMM	END DEBUG	MMMMMMMMMMMMMMMM/
							double NOxInGPKWHr = (SCRoutgpsSum*3600)/bkpwrSum;
							//if(maxNOX<NOxInGPKWHr)
								//maxNOX = NOxInGPKWHr;
							if(NOxInGPKWHr>anomalyStandardThreshold)
							{
								anomalousWindows.put(Integer.toString(i)+","+Integer.toString(j), new TemporalWindow(allReadings.get(i).Iteration,allReadings.get(j).Iteration));
								//since window was anomalous, add 1 to this window length count
							}
						}
					}
		}
	
		//System.out.println("Max NOX in gm/kw-hr = "+maxNOX);
		return anomalousWindows;
		
	}
	
}
