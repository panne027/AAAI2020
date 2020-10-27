import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class AllStatesGraphNoxModel {
	
	public ArrayList<HashMap<String,ArrayList<Integer>>> stateGraphs;
	/* This is an arraylist of Hashmaps, each pattern  dimension has a hashmap for it in this array.
	 * Each hash map stores as keys the transitions between pairs of states e.g. s1-s2 that appeared 
	 * consecutively in time for the corresponding dimension. The value of the key is an arraylist that 
	 * contains all the indexes of the time instants at which this edge started to occur 
	 * (i.e. index of the start state in the allReadings array) in increasing order (i.e. order of insertion)
	 */
	
	public int dimensionsNum; //number of pattern dimensions
	

	public AllStatesGraphNoxModel( int dimNum)
	{
		dimensionsNum = dimNum;
		stateGraphs = new ArrayList<HashMap<String,ArrayList<Integer>>>();
		//created a hash map for each pattern dimension
		for(int i=1;i<=dimNum;i++)
		{
			stateGraphs.add(new HashMap<String, ArrayList<Integer>>());
		}
	}
	
	public void preprocessTimeSeriesIntoStateGraphs(EngineReadingNoxModel[] allReadings)
	{
		for(int i=0, j=i+1;j<allReadings.length;i++,j++) //pass by every two consecutive readings
		{
			for(int d = 0;d<dimensionsNum;d++) //pass by all dimensions for those readings
			{
				String key = allReadings[i].patternDimensionsValues[d]+"-"+allReadings[j].patternDimensionsValues[d];
				if(stateGraphs.get(d).containsKey(key))
				{
					ArrayList<Integer> timeList = stateGraphs.get(d).get(key);
					timeList.add(i);
					//TODO:
					//do we need to reset the value of the key with the arraylist again? I don't think so
				}
				else
				{
					ArrayList<Integer> timeList = new ArrayList<Integer>();
					timeList.add(i);
					stateGraphs.get(d).put(key, timeList);
				}
			}
		}
	}
	
	public int[] countPatternUsingStateGraphs(EngineReadingNoxModel[] allReadings, LinkedHashMap<String, TemporalWindow> anomalousWindows, ArrayList<Integer> includedDimensions, int windowFrom, int windowTo, int from, int to, int lag)
	{
		//returns an integer array of length 3. 
		//First integer is count of pattern
		//Second integer is count of pattern with anomaly
		//Third integer is count of distinct pattern with anomaly
		
		int [] counts = new int[3];
		counts[0] = counts[1] = counts[2] = 0;

		//Retrieve all time instants of the first edge in first included dimension from the state graph of that dimension
		int dim = includedDimensions.get(0);
		String key = allReadings[from].patternDimensionsValues[dim]+"-"+allReadings[from+1].patternDimensionsValues[dim];
		ArrayList<Integer> startTimeList = stateGraphs.get(dim).get(key);
		if(startTimeList==null)
			return counts;
		
		/* Start expanding each of the time instants to see if it forms the whole pattern, and if it does
		 * then add to patternCount and inspect the number of anomalies near this pattern instance.
		 * For each time instant, start expanding across the different dimensions first then across the time series
		 */
		for(int i=0;i<startTimeList.size();i++)
		{
			//Inspect all time instants of all dimensions
			boolean isFound = true;
			int timeSeriesPointer = startTimeList.get(i);
			for(int patternIndex = from; patternIndex<=to;patternIndex++) //compare all time instants/letters in the pattern
        	{
				if(timeSeriesPointer>=allReadings.length)
				{
					isFound = false;
					break;
				}
				for(dim = 0;dim<includedDimensions.size();dim++) //compare each dimension in the pattern
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
        			String windowKey = Integer.toString(startTimeList.get(i)+d)+","+Integer.toString(startTimeList.get(i)+d+windowTo-windowFrom);
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

}

	
