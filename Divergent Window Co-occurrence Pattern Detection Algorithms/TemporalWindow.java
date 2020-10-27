
public class TemporalWindow {

	long startIteration;
	long endIteration;
	
	TemporalWindow()
	{
		startIteration = -1;
		endIteration = -1;
	}
	
	TemporalWindow(long start, long end)
	{
		startIteration = start;
		endIteration = end;
	}
}
