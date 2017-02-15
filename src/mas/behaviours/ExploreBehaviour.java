package mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.algorithm.Dijkstra;

import env.Attribute;
import env.Couple;
import jade.core.behaviours.Behaviour;


public class ExploreBehaviour extends Behaviour {
	
	private static final long serialVersionUID = 9088209402507795289L;
	private boolean finished = false;
	private Graph graph ;
	private List<Node> chemin = new ArrayList<Node>();
	private ArrayList<String> opened ;
	
	public ExploreBehaviour(final mas.abstractAgent myagent){
		super(myagent);
		graph = new SingleGraph("");
		opened = new ArrayList<String>();
		//atention: cache l'exception IdAlreadyInUse
		graph.setStrict(false);
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
					if(n.getAttribute("state") == null || !n.getAttribute("state").equals("closed")){
						neighbors.add(idNeighbor);
						n.addAttribute("state", "opened");
						if(!opened.contains(idNeighbor)) 
							opened.add(idNeighbor);
					}
					
					graph.addEdge(myPosition+idNeighbor, root, n);
				}
			}
			System.out.println(opened.toString());
			
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
			//si on n'a plus de noeud ouverts, l'exploration est finie
			if(opened.isEmpty()){
				finished = true;
				System.out.println("exploration finie");
			}
			else{
				//si on a un chemin a suivre
				if(chemin.size() != 0){
					Node next = chemin.remove(0);
					((mas.abstractAgent)this.myAgent).moveTo(next.getId());
				}
				else{
					//si on a un voisin ouvert on y va :)
					if(neighbors.size()!= 0){
						((mas.abstractAgent)this.myAgent).moveTo(neighbors.get(0));
					}
					else{
						//on cherche le noeud ouvert le plus proche
						Dijkstra dijk = new Dijkstra(Dijkstra.Element.NODE, null, null);
						dijk.init(graph);
						dijk.setSource(root);
						dijk.compute();
						
						int min = Integer.MAX_VALUE;
						Path shortest = null;
						
						for(String id : opened){
							double l = dijk.getPathLength(graph.getNode(id));
							if(l < min){
								min = (int) l;
								shortest = dijk.getPath(graph.getNode(id));
							}
						}
						
						
						chemin = shortest.getNodePath();
						System.out.println(chemin.toString());
						chemin.remove(0);
						Node next = chemin.remove(0);
						
						((mas.abstractAgent)this.myAgent).moveTo(next.getId());
						
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
