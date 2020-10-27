import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;


public class HybridDimensionsGraph {
	
	HybridDimensionNode root;
	int [] leavesPatternWithAnomalyCount;
	int [] leavesPatternCount;
	
	//The additional parameters for hybrid approach
	ArrayList<ArrayList<HybridDimensionNode>> graphNodes;
	int nextTopLevel; // points to the index of the level to be enumerated next from top in graphNodes ArrayList
	int nextBottomLevel; //points to the index of the level to be enumerated next from bottom in the graphNodes ArrayList
	
	public HybridDimensionsGraph(int dimNum)
	{
		root = null;
		leavesPatternWithAnomalyCount = new int[dimNum];
		leavesPatternCount = new int[dimNum];
		for(int i=0;i<dimNum;i++)
		{
			leavesPatternWithAnomalyCount[i] = 0;
			leavesPatternCount[i] = 0;
		}
		///Added
		nextTopLevel = nextBottomLevel = -1;
		graphNodes = null;
		///
	}
	
	public void initialize()
	{
		//this method creates all nodes and parent-child links in the dimensionsGraph
		int dimNum = leavesPatternWithAnomalyCount.length;
		
		///Added
		nextTopLevel = dimNum-1;
		nextBottomLevel = 0;
		graphNodes = new ArrayList<ArrayList<HybridDimensionNode>>();
		for(int i=0;i<dimNum;i++)
			graphNodes.add(new ArrayList<HybridDimensionNode>());
		///
		int [] arr = new int[dimNum];
		for(int i=0;i<dimNum;i++)
		{
			arr[i] = i;
		}
		
		//Create root Node
		root = new HybridDimensionNode();
		root.setDimensions(arr);
		
		
		ArrayList<HybridDimensionNode> previousLevel = new ArrayList<HybridDimensionNode>();
		previousLevel.add(root);
		///
		graphNodes.get(dimNum-1).add(root);
		///
		ArrayList<HybridDimensionNode> currentLevel;
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
					///Also initialize the childrenNum and parents array in each HybridDimensionNode
					root.childrenNum++;
					currentLevel.get(i).parents.add(root);
					///
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
							///Also initialize the childrenNum and parents array in each HybridDimensionNode
							previousLevel.get(i).childrenNum++;
							currentLevel.get(j).parents.add(previousLevel.get(i));
							///
						}
							
					}
				}
			}
			
			///add current level to graphNodes
			graphNodes.get(r-1).addAll(currentLevel);
			///
			
			//Copy currentLevel to previousLevel
			previousLevel = currentLevel; //this should work since the printCombination method allocates a new arrayList for currentLevel each time
		}
		  
	}
	
	
	// The main function that prints all combinations of size r
		// in arr[] of size n. This function mainly uses combinationUtil()
		static ArrayList<HybridDimensionNode> printCombination(int arr[], int n, int r)
		{
			ArrayList<HybridDimensionNode> currentLevel = new ArrayList<HybridDimensionNode>();
			
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
		static void combinationUtil(int arr[], int data[], int start, int end, int index, int r,ArrayList<HybridDimensionNode> currentLevel)
		{
		    // Current combination is ready to be printed, print it
		    if (index == r)
		    {
		    	HybridDimensionNode n = new HybridDimensionNode();
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

	
	public HybridDimensionsGraph deepClone()
	{
		//References:
		//http://stackoverflow.com/questions/14536702/algorithm-to-clone-a-graph
		//http://leetcode.com/2012/05/clone-graph-part-i.html
		
		HybridDimensionsGraph graphCopy = new HybridDimensionsGraph(leavesPatternWithAnomalyCount.length);
		int dimNum = leavesPatternWithAnomalyCount.length;
		
		///Added
		graphCopy.nextTopLevel = dimNum-1;
		graphCopy.nextBottomLevel = 0;
		
		if(root==null)
		{
			 return graphCopy;
		}
		
		HashMap<HybridDimensionNode, HybridDimensionNode> map = new HashMap<HybridDimensionNode, HybridDimensionNode>();
		
		 LinkedList<HybridDimensionNode> q = new LinkedList<HybridDimensionNode>(); //queue for performing BFS
		 q.add(root); 
		 graphCopy.root = new HybridDimensionNode(root);
		 map.put(root, graphCopy.root);
	
		 while (q.size()>0)
		 {
	        HybridDimensionNode node = q.removeFirst(); //to make a linked list work like a queue
	        int n = node.children.size();
	        for (int i = 0; i < n; i++) 
	        {
	            HybridDimensionNode neighbor = node.children.get(i);
	          
	            // a copy already exists
	            if (map.containsKey(neighbor))
	            {
	            	map.get(node).children.add(map.get(neighbor));
	            	/// added: Need to fill the parents array of each node
	            	map.get(neighbor).parents.add(map.get(node));
	            }
	            else
	            {   // no copy exists
	            	HybridDimensionNode p = new HybridDimensionNode(neighbor);
	                map.get(node).children.add(p);
	                /// added: Need to fill the parents array of each node
	            	p.parents.add(map.get(node));
	            	
	                map.put(neighbor, p);
	                q.add(neighbor);
	            }
	        }
	    }
		 
		/// added: need to create graph nodes to contain pointers of cloned nodes
		graphCopy.graphNodes = new ArrayList<ArrayList<HybridDimensionNode>>();
		
		for(int i=0;i<dimNum;i++)
			graphCopy.graphNodes.add(new ArrayList<HybridDimensionNode>());
		
		for(int i=0;i<this.graphNodes.size();i++) //pass by all levels in original graphNodes ArrayList
		{
			for(int j=0;j<this.graphNodes.get(i).size();j++) //pass by all nodes of this level in original graphNodes ArrayList
			{
				//add the cloned (i.e. mapped) copy of this node to the graphNodes of the cloned object to be returned
				graphCopy.graphNodes.get(i).add(map.get(this.graphNodes.get(i).get(j)));
			}
		}
		
	    return graphCopy;
	}
	
	
	
	public void printGraph() //TBD: not updated for hybrid approach yet
	{
		
		HashMap<String,String> map = new HashMap<String,String>();
		printNodeWithDescendants(root,map);
	}
	
	private void printNodeWithDescendants(HybridDimensionNode n,HashMap<String,String> map) //TBD: not updated for hybrid approach yet
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
		if(n.isPruned!=0)
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
