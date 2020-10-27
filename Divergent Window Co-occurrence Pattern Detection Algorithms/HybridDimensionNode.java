import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;


public class HybridDimensionNode {
	public LinkedHashSet<String> dimensions;
	public int isPruned; 
	// isPruned  = 0 by default (unpruned)
	//isPruned = 1 if it is pruned by min support threshold
	//isPruned = 2 if it is pruned by upper bounds or previously enumerated in another window
	//isPruned = 3 if it pruned using both upper bounds and minsupport
	public ArrayList<HybridDimensionNode> children;
	public int parentsNum;
	public int superPatternCount;
	
	public int childrenNum;
	public ArrayList<HybridDimensionNode> parents;
	
	
	public HybridDimensionNode()
	{
		dimensions = new LinkedHashSet<String>();
		isPruned = 0;
		children = new ArrayList<HybridDimensionNode>();
		parentsNum = 0;
		superPatternCount = 1;
		
		///added
		parents = new ArrayList<HybridDimensionNode>();
		childrenNum = 0;
		///
	}
	

	public HybridDimensionNode(HybridDimensionNode n)
	{
		isPruned = n.isPruned;
		parentsNum = n.parentsNum;
		childrenNum = n.childrenNum;
		superPatternCount = n.superPatternCount;
		
		dimensions = new LinkedHashSet<String>();
		Iterator<String> itr = n.dimensions.iterator();
		while(itr.hasNext())
		{
			String s = itr.next();
			dimensions.add(s);
		}
		children = new ArrayList<HybridDimensionNode>(); //note that neighbors will be copied in the clone algorithm but not here
		parents = new ArrayList<HybridDimensionNode>(); //note that neighbors will be copied in the clone algorithm but not here
	}
	
	public void setDimensions(int [] dims)
	{
		for(int i=0;i<dims.length;i++)
			dimensions.add(Integer.toString(dims[i]));
	}
	
	
}
