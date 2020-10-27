import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;


public class DimensionNode {
	public LinkedHashSet<String> dimensions;
	public boolean isPruned;
	public ArrayList<DimensionNode> children;
	public int parentsNum;
	public int superPatternCount;
	
	public DimensionNode()
	{
		dimensions = new LinkedHashSet<String>();
		isPruned = false;
		children = new ArrayList<DimensionNode>();
		parentsNum = 0;
		superPatternCount = 1;
	}
	

	public DimensionNode(DimensionNode n)
	{
		isPruned = n.isPruned;
		parentsNum = n.parentsNum;
		superPatternCount = n.superPatternCount;
		
		dimensions = new LinkedHashSet<String>();
		Iterator<String> itr = n.dimensions.iterator();
		while(itr.hasNext())
		{
			String s = itr.next();
			dimensions.add(s);
		}
		children = new ArrayList<DimensionNode>(); //note that neighbors will be copied in the clone algorithm but not here
	}
	
	public void setDimensions(int [] dims)
	{
		for(int i=0;i<dims.length;i++)
			dimensions.add(Integer.toString(dims[i]));
	}
	
	
}
