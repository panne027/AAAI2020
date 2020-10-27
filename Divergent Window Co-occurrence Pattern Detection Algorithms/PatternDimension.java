import java.util.ArrayList;


public class PatternDimension {
	public String name;
	public int indexInInputFile; //index of column in data file as read from the header file
	public double intervalWidth;
	//public char eventsSymbol; //the char symbol representing the events defined for this dimension e.g. s for speed events
	public double minValue;
	public double maxValue;	
	public ArrayList<EventInterval> intervals;
	
	public PatternDimension()
	{
		name = "";
		indexInInputFile = -1;
		intervalWidth = -1;
		minValue= 0;
		maxValue = 0;
		intervals = new ArrayList<EventInterval>();
	}

}

