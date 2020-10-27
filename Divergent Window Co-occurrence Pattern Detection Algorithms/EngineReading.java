
public class EngineReading {

	public long Iteration;
	public int year;
	public int month;
	public int day;
	public double Latitude;
	public double Longitude;
	public double SCRoutppm;
	public double SCRoutgps;
	public double Bkpwr;
	public int pass;
	
	public String [] patternDimensionsValues; //the values corresponding to the dimensions of the pattern (after discretization into events) in the order they are stored in the dimensionsInfo[] array in the MainDriver class
	public double NOxActual;
	public double RelativeError;
	public double NOxTheoryppm;
    public double AbsError;

    public void parseLine(String line)
	{
		String [] values = line.split(",");
		Iteration = Long.parseLong(values[0]);
		year = Integer.parseInt(values[1]);
		month = Integer.parseInt(values[2]);
		day = Integer.parseInt(values[3]);
		Latitude = Double.parseDouble(values[4]);
		Longitude = Double.parseDouble(values[5]);
		NOxTheoryppm = Double.parseDouble(values[6]);
		RelativeError = Double.parseDouble(values[7]);
		AbsError = Double.parseDouble(values[8]);
		pass = Integer.parseInt(values[9]);
	
		//TODO: parse pattern dimensions into array
	}
}

