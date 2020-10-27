
public class EngineReadingCO2 {

	public long Iteration;
	public int year;
	public int month;
	public int day;
	public double Latitude;
	public double Longitude;
	//public double SCRoutppm;
	//public double SCRoutgps;
	public double Bkpwr;
	public double co2gps;
	
	public int pass;
	
	public String [] patternDimensionsValues; //the values corresponding to the dimensions of the pattern (after discretization into events) in the order they are stored in the dimensionsInfo[] array in the MainDriver class
	
	/*public void parseLine(String line)
	{
		String [] values = line.split(",");
		Iteration = Long.parseLong(values[0]);
		Latitude = Double.parseDouble(values[1]);
		Longitude = Double.parseDouble(values[2]);
		//SCRoutgps = Double.parseDouble(values[3]);
		co2gps = Double.parseDouble(values[3]);
		Bkpwr = Double.parseDouble(values[4]);
	
		//TODO: parse pattern dimensions into array
	}
	*/
}

