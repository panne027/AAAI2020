# Research:

## Co-location and Co-occurence in class:
- CSCI 5715 November 12 lecture (@8 min)
- Association rules do not work for co-location
- Finding correlation between all pairs is very high
- Apriori algorithm prunes such problems in supermarkets  
- Support function is monotonic (upper bounded) by one of its constituents
- Can simply eliminate eliminate a lot of combinations by using support thresholds to eliminate single items (then can increase one by one for items that have support above)
- Support is the basis of apriori algorithm
	- Apriori has support based pruning using monotonicity
	- Even conditional probability is used to understand the directionality of such relationships
- Even when making triplets, if any of the pair in those triplets have thrown out before, then do not make that triplet

## Ripley's / cross-k function (Spatial Statistics):
- [Pokemon Blog](https://blog.jlevente.com/understanding-the-cross-k-function/)
- Point patterns [clustered, dispersed, random], compares with complete spatial randomness
- Bivariate: Reports the number of type j events within a given radius of type i events 
- `i:controls | j:cases`
- Poisson distribution: no of events in discrete time interval or region. 
	- [Poisson Ref](https://www.youtube.com/watch?v=cPOChr_kuQs&ab_channel=zedstatistics)
	- Require only one parameter (lambda) / no of events per hour or per region
	- Assumptions: Events are independent | Rate at which event occur is constant
- Random process: K(r) = pie * r^2 (for spatial random, poisson homogenous distribution
- [Monte Carlo Simulation](https://www.youtube.com/watch?v=OgO1gpXSUzU&ab_channel=MITOpenCourseWare)
	- Using inferential statistics to say something about the population, depending upon randomly drawn sample
	- Samples drawn have to be random for this property to work
	- Confidence increases with more number of samples of drawn (evidence), due to variance
	- We require more number of samples to have more confidence when variance (variability) is high

## NWC Paper:
- If a target variable is undesirable, what were temporal signatures associated with it in other explanatory variables.
- [Apriori Algorithm](https://www.youtube.com/watch?v=WGlMlS_Yydk&ab_channel=AugmentedStartups):  Items that are bought together 
- Spatio-temporal statistics measured using non-monotonic functions such as cross-k
- In such scenarios, monotonic functions like apriori do not make sense

- Definitions:
	- *Types of windows:* Divergent window and Non-compliance window
	- *No of windows:* (total no of points - window_length), as it is consecutive
		- But one window cannot be across two different trip files
		- Also, if values missing, that window becomes invalid
	- *Event :* Discretising continuous explanable variables
	- *MET :* time window where explanable variables and target variables are recorded
	- *Event sequence :* sequences of events (both for explanable and target variables)
	- *NWC :* Value of Zonal function over a target variable in a MET (**Multi-variant event trajectory**) is greater than a certain threshold over a predefined time interval of length L.
	- *NWC Pattern :* All candidate patterns (event sequences) from either one or many explanable variables, that occur with or in delta time interval of non-compliant window. 
		- Dim(C) for dimentionality of patterns
		- Length(C) for length of these sequences = Length of non-compliant window

- Temporal Cross-K:
	- T: Total time of run
	- K = (T * temporal cardinality of the join set of C)/ (|Wn| * |C|)
	- |Wn| : number of non-compliant windows in all MET
	- |C| : no of pattern instances in all MET
	- temporal join set : no of times, C_i precedes Wn_j within a time of delta
	- Support : |C| / T (i.e. total number of times candidate pattern occurs in MET / Total time of MET)

### Naive Approach to solving NWC:
- Find all NWC of same length as first one using *sliding window* in all METs
	- For each NWC, enumerate all temporal windows starting at delta before NWC
		- For each of these temporal windows, enumerate all candidate patterns
			- For each pattern, calculate its cardinality in entire MET with a single scan
			- In this single scan, intersection with other NWCs is also calculated
			- If after single scan, the support for pattern satisfies *minsupp*, it's temporal cross-K is calculated
			- If it's cross-K above user specified threshold, then it is a pattern with strong association

- No NWCs or candidate pattern is allowed to overlap two different METs.
- Patterns in MET of one vehicle not to be associated with NW in MET of other vehicles.
