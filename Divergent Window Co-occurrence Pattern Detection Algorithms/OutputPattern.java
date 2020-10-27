
public class OutputPattern {
	
	
	public String pattern;
	public  int patternCount;
	public int patternWithAnomalyCount;
	public int anomalyCount;
	public int distinctPatternWithAnomalyCount;
	public double kValue;
	
	public OutputPattern( String pat,int pCount,int pWithACount, int distinctPWithACount, int aCount,double k)
	{
		pattern = pat;
		patternCount =pCount ;
		patternWithAnomalyCount = pWithACount;
		anomalyCount = aCount;
		distinctPatternWithAnomalyCount =distinctPWithACount; 
		kValue = k;
	}

}
