
public class EngineReadingNoxModel {

	public long Iteration;
	public int year;
	public int month;
	public int day;
	public double Latitude;
	public double Longitude;
	public int pass;
	
	public double SCRinppm;
	public double noxByModel;
	public double relativeError;
	public double absoluteError;
	
	public String [] patternDimensionsValues; //the values corresponding to the dimensions of the pattern (after discretization into events) in the order they are stored in the dimensionsInfo[] array in the MainDriver class
	

}

