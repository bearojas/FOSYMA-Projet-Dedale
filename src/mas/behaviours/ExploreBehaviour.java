package mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.algorithm.Dijkstra;

import env.Attribute;
import env.Couple;
import jade.core.behaviours.SimpleBehaviour;


public class ExploreBehaviour extends SimpleBehaviour {
	
	private static final long serialVersionUID = 9088209402507795289L;
	private boolean finished = false;
	private Graph graph ;
	private List<Node> chemin;
	private ArrayList<String> opened ;
	
	public ExploreBehaviour(final mas.abstractAgent myagent, Graph graph, List<Node> chemin, ArrayList<String> opened){
		super(myagent);
		this.graph = graph;
		this.opened = opened;
		this.chemin = chemin;
		//atention: cache l'exception IdAlreadyInUse
		this.graph.setStrict(false);
	}
	

	//fonction de recherche du plus court chemin vers le noeud ouvert le plus proche
	// renvoie le plus court chemin sans la racine
	public List<Node> search(Graph myGraph,Node root, ArrayList<String> open){

		Dijkstra dijk = new Dijkstra(Dijkstra.Element.NODE, null, null);
		dijk.init(myGraph);
		dijk.setSource(root);
		dijk.compute();

		int min = Integer.MAX_VALUE;
		Path shortest = null;
		
		for(String id : open){
			double l = dijk.getPathLength(myGraph.getNode(id));
			if(l < min){
				min = (int) l;
				shortest = dijk.getPath(myGraph.getNode(id));
			}
		}	
		List<Node> shortPath=shortest.getNodePath();
		shortPath.remove(0);
		return shortPath ;
	}

	
	@Override
	public void action() {
	
		String myPosition=((mas.abstractAgent)this.myAgent).getCurrentPosition();

		
		if (myPosition!=""){

			//List of observable from the agent's current position
			List<Couple<String,List<Attribute>>> lobs=((mas.abstractAgent)this.myAgent).observe();
			
			//on determine l'indice de la position courante dans lobs 
			int posIndex = 0;
			for(int i = 0; i < lobs.size(); i++){
				if(lobs.get(i).getLeft() == myPosition){
					posIndex = i;
					break;
				}
			}
			//list of attribute associated to the currentPosition
			List<Attribute> lattribute= lobs.get(posIndex).getRight();
			
//			for(Attribute a: lattribute){
//				System.out.println("name:"+a.getName());
//				System.out.println("valeur:"+a.getValue());
//			}
			
			System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);
			
			// create root node
			//  delete current node from opened
			Node root = graph.addNode(myPosition);
			root.addAttribute("state", "closed");
			root.addAttribute("content",lattribute);
			opened.remove(myPosition);
			
			//on ajoute tous les voisins du noeud courant
			//on les ajoute a la liste des ouverts s'il ne sont pas deja fermes
			ArrayList<String> neighbors = new ArrayList<String>();
			
			for(int i=0; i < lobs.size(); i++){
				if(posIndex != i){
					String idNeighbor = lobs.get(i).getLeft();
					Node n = graph.addNode(idNeighbor);
					//n.addAttribute("ui.label", n.getId());
					
					if(n.getAttribute("state") == null || !n.getAttribute("state").equals("closed")){
						neighbors.add(idNeighbor);
						n.addAttribute("state", "opened");
						if(!opened.contains(idNeighbor)) 
							opened.add(idNeighbor);
					}
					
					graph.addEdge(myPosition+idNeighbor, root, n);
				}
			}

			System.out.println("noeuds ouverts: "+opened.toString());
			
			//Little pause to allow you to follow what is going on
			try {
				System.out.println("Press a key to allow the agent "+this.myAgent.getLocalName() +" to execute its next move");
				System.in.read();
			} catch (IOException e) {
				e.printStackTrace();
			}

			//example related to the use of the backpack for the treasure hunt
			Boolean b=false;
			for(Attribute a:lattribute){
				switch (a) {
				case TREASURE:
					System.out.println("My current backpack capacity is:"+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					System.out.println("Value of the treasure on the current position: "+a.getValue());
					System.out.println("The agent grabbed :"+((mas.abstractAgent)this.myAgent).pick());
					System.out.println("the remaining backpack capacity is: "+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					System.out.println("The value of treasure on the current position: (unchanged before a new call to observe()): "+a.getValue());
					b=true;
					break;

				default:
					break;
				}
			}

			//If the agent picked (part of) the treasure
			if (b){
				List<Couple<String,List<Attribute>>> lobs2=((mas.abstractAgent)this.myAgent).observe();//myPosition
				System.out.println("lobs after picking "+lobs2);
			}
			//si on n'a plus de noeuds ouverts, l'exploration est finie
			if(opened.isEmpty()){
				finished = true;
				System.out.println("Exploration finie: "+graph.getNodeCount()+"noeuds");
			}
			else{
				//si on a un chemin a suivre
				if(chemin.size() != 0){
					Node next = chemin.remove(0);
					// tant qu'on n'a pas pu se d�placer....
					while(!((mas.abstractAgent)this.myAgent).moveTo(next.getId())){
						// creation d'un graphe temporaire qui oblige � chercher un autre chemin sans passer par le noeud bloqu�
						System.out.println("recherche d'un chemin qui ne passe pas par "+next.getId());
						
						Graph tempGraph = Graphs.clone(graph);
						tempGraph.removeNode(next);	
						chemin = search(tempGraph,root, opened);
						next = chemin.remove(0);
						
					}
				}
				else{
					//si on a un voisin ouvert 
					if(neighbors.size()!= 0){
						System.out.println("VOISINS "+neighbors.toString());
						int i =0 ;
						Node next = graph.getNode(neighbors.get(i));
						System.out.println("Je vais en "+ next.getId());
						// si on ne peut pas aller vers son voisin
						// soit on prend le voisin suivant
						// soit, si on a fait toute la liste des voisins, on fait une recherche de chemin
						while(!((mas.abstractAgent)this.myAgent).moveTo(next.getId())){
							i++ ;
							if( i >= neighbors.size()){
								chemin = search(graph, root, opened);
								next = chemin.remove(0);
							} else {
								next =graph.getNode(neighbors.get(i));
								//graph.a
							}
						}
						
					}
					else{
						// si pas de voisins
						//on cherche le noeud le plus proche						
						chemin = search(graph, root, opened);
						Node next = chemin.remove(0); // on enl�ve le noeud vers lequel on va aller pour ne pas le garder dans le chemin � faire
						
						while(!((mas.abstractAgent)this.myAgent).moveTo(next.getId())){
							System.out.println("recherche d'un chemin qui ne passe pas par "+next.getId());
							// creation d'un graphe temporaire qui oblige � chercher un autre chemin sans passer par le noeud bloqu�
							Graph tempGraph = Graphs.clone(graph);							
							tempGraph.removeNode(next);
							chemin = search(tempGraph,root, opened);
							next = chemin.remove(0);
							
						}
						
					}					
				}

			}
			
		}
		
	}
	
	
	@Override
	public boolean done() {
		return finished;
	}


}
