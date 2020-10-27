import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;


public class DimensionsGraph {
	
	DimensionNode root;
	int [] leavesPatternWithAnomalyCount;
	int [] leavesPatternCount;
	
	public DimensionsGraph(int dimNum)
	{
		root = null;
		leavesPatternWithAnomalyCount = new int[dimNum];
		leavesPatternCount = new int[dimNum];
		for(int i=0;i<dimNum;i++)
		{
			leavesPatternWithAnomalyCount[i] = 0;
			leavesPatternCount[i] = 0;
		}
	}
	
	public void initialize()
	{
		//this method creates all nodes and parent-child links in the dimensionsGraph
		int dimNum = leavesPatternWithAnomalyCount.length;
		int [] arr = new int[dimNum];
		for(int i=0;i<dimNum;i++)
		{
			arr[i] = i;
		}
		
		//Create root Node
		root = new DimensionNode();
		root.setDimensions(arr);
		
		ArrayList<DimensionNode> previousLevel = new ArrayList<DimensionNode>();
		previousLevel.add(root);
		ArrayList<DimensionNode> currentLevel;
		for(int r=dimNum-1;r>0;r--)
		{
			currentLevel = printCombination(arr, dimNum, r);
			/*if previousLevel contained only one node, then it is root node so link it to all nodes in current level
			 * otherwise ,create parent-child relations between previous level and current level
			 */
			if(previousLevel.size()==1) //i.e. previous level is root node
			{
				//add each node of currentLevel as child of root node
				for(int i=0;i<currentLevel.size();i++)
				{
					root.children.add(currentLevel.get(i));
					currentLevel.get(i).parentsNum++;
				}
			}
			else //previous level is NOT root node
			{
				for(int i=0;i<previousLevel.size();i++)
				{
					for(int j=0;j<currentLevel.size();j++)
					{
						boolean isChild = true;
						//if node in previous level and node in current level share a dimension, then node in current level is a child for the node in previous level
						Iterator<String> itr = currentLevel.get(j).dimensions.iterator(); 
						while(itr.hasNext())
						{
							String dStr = itr.next();
							if(!(previousLevel.get(i).dimensions.contains(dStr)))
							{
								isChild = false;
								break;
							}
						}
						if(isChild)
						{
							previousLevel.get(i).children.add(currentLevel.get(j));
							currentLevel.get(j).parentsNum++;
						}
							
					}
				}
			}
			
			
			//Copy currentLevel to previousLevel
			previousLevel = currentLevel; //this should work since the printCombination method allocates a new arrayList for currentLevel each time
		}
		  
	}
	
	
	// The main function that prints all combinations of size r
		// in arr[] of size n. This function mainly uses combinationUtil()
		static ArrayList<DimensionNode> printCombination(int arr[], int n, int r)
		{
			ArrayList<DimensionNode> currentLevel = new ArrayList<DimensionNode>();
			
		    // A temporary array to store all combination one by one
		    int [] data = new int[r];
		 
		    // Print all combination using temporary array 'data[]'
		    combinationUtil(arr, data, 0, n-1, 0, r,currentLevel);
		    return currentLevel;
		}
		 
		/* arr[]  ---> Input Array
		   data[] ---> Temporary array to store current combination
		   start & end ---> Staring and Ending indexes in arr[]
		   index  ---> Current index in data[]
		   r ---> Size of a combination to be printed */
		static void combinationUtil(int arr[], int data[], int start, int end, int index, int r,ArrayList<DimensionNode> currentLevel)
		{
		    // Current combination is ready to be printed, print it
		    if (index == r)
		    {
		    	DimensionNode n = new DimensionNode();
		    	n.setDimensions(data);
		    	currentLevel.add(n);
		    	/*for (int j=0; j<r; j++)
		            System.out.print(data[j]);
		        System.out.println("\n");
		        */
		        return;
		    }
		 
		    // replace index with all possible elements. The condition
		    // "end-i+1 >= r-index" makes sure that including one element
		    // at index will make a combination with remaining elements
		    // at remaining positions
		    for (int i=start; i<=end && end-i+1 >= r-index; i++)
		    {
		        data[index] = arr[i];
		        combinationUtil(arr, data, i+1, end, index+1, r, currentLevel);
		    }
		}

	
	public DimensionsGraph deepClone()
	{
		DimensionsGraph graphCopy = new DimensionsGraph(leavesPatternWithAnomalyCount.length);
		if(root==null)
		{
			 return graphCopy;
		}
		
		
		HashMap<DimensionNode, DimensionNode> map = new HashMap<DimensionNode, DimensionNode>();
		
		 LinkedList<DimensionNode> q = new LinkedList<DimensionNode>(); //queue for performing BFS
		 q.add(root); 
		 graphCopy.root = new DimensionNode(root);
		 map.put(root, graphCopy.root);
	
		 while (q.size()>0)
		 {
	        DimensionNode node = q.removeFirst(); //to make a linked list work like a queue
	        int n = node.children.size();
	        for (int i = 0; i < n; i++) 
	        {
	            DimensionNode neighbor = node.children.get(i);
	          
	            // a copy already exists
	            if (map.containsKey(neighbor))
	            {
	            	map.get(node).children.add(map.get(neighbor));
	            }
	            else
	            {   // no copy exists
	            	DimensionNode p = new DimensionNode(neighbor);
	                map.get(node).children.add(p);
	                map.put(neighbor, p);
	                q.add(neighbor);
	            }
	        }
	    }
	 
	    return graphCopy;
	}
	
	

	
	public void printGraph()
	{
		
		HashMap<String,String> map = new HashMap<String,String>();
		printNodeWithDescendants(root,map);
	}
	
	private void printNodeWithDescendants(DimensionNode n,HashMap<String,String> map)
	{
		//Node label i.e. dimensions string
		String lab = "";
		Iterator<String> itr = n.dimensions.iterator();
		while(itr.hasNext())
			lab+=itr.next()+",";
		if(map.containsKey(lab))
			return;
		else
			map.put(lab, null);
		lab+="\t";
		
		
		String members = "";
		if(n.isPruned==true)
			members+="isPruned:true\t";
		else
			members+="isPruned:false\t";
		
		members+="parentsNum: "+Integer.toString(n.parentsNum)+"\tsuperPatternCount: "+Integer.toString(n.superPatternCount)+"\n";
		
		//print labels of all children in a following line
		String children = "Children: ";
		for(int i=0;i<n.children.size();i++)
		{
			
			Iterator<String> itr2 = n.children.get(i).dimensions.iterator();
			while(itr2.hasNext())
				children+=itr2.next()+",";
			children+="\t";
		}
		children+="\n\n";
		System.out.println(lab+members+children);
		
		for(int i=0;i<n.children.size();i++)
		{
			
			printNodeWithDescendants(n.children.get(i), map);
		}
	}
	
	
}
