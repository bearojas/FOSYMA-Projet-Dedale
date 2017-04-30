package mas.behaviours;

import java.util.ArrayList;
import java.util.List;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import env.Attribute;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import mas.agents.CleverAgent;

public class PickTreasureBehaviour extends SimpleBehaviour{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7514677342893958274L;

	private int state = 0;
	private List<Node> path ;
	
	public PickTreasureBehaviour(mas.abstractAgent myagent) {
		super(myagent);
	}

	/**
	 * @param treasure : le tresor qu'on va chercher
	 * @return l'AID de l'agent qu'on va contacter
	 */
	private AID searchCoalition(String treasure){		
		//TODO
		//recuperer AgentList dans cleverAgent
		Graph graph = ((CleverAgent)myAgent).getGraph();
		
		
		return null;
	}
	
	
	/**
	 * Choisir le trésor à aller chercher
	 * @return la position du trésor à chercher
	 */
	private String chooseTreasure(){
		//TODO
		//recuperer AgentList et Treasures dans cleverAgent
		ArrayList<String> treasure = ((CleverAgent)myAgent).getTreasures();
		ArrayList<String> diamonds = ((CleverAgent)myAgent).getDiamonds();
		Graph graph = ((CleverAgent)myAgent).getGraph();
		
		String best =  null;
		int moneyMax = 0; 
		
		if( ((CleverAgent)myAgent).getType().equals("Treasure")){
			// si mon agent ne peut ramasser que des trésors
			for (String id : treasure){
				//on cherche le plus gros trésor
				Node n = graph.getNode(id);
				int value = (Integer)((List<Attribute>) n.getAttribute("content")).get(0).getValue();
				if( value > moneyMax){
					moneyMax = value;
					best = id;
				}
			}
		}
		if( ((CleverAgent)myAgent).getType().equals("Diamonds")){
			//si mon agent ne peut ramasser que des diamants
			for (String id : diamonds){
				//on cherche le plus gros trésor de diamants
				Node n = graph.getNode(id);
				int value = (Integer)((List<Attribute>) n.getAttribute("content")).get(0).getValue();
				if( value > moneyMax){
					moneyMax = value;
					best = id;
				}
			}
		}
		if( ((CleverAgent)myAgent).getType().equals("")){
			// si mon agent ne connait pas son type
			treasure.addAll(diamonds); 
			for (String id : treasure ){
				//on cherche le plus gros trésor parmi les deux categories
				Node n = graph.getNode(id);
				int value = (Integer)((List<Attribute>) n.getAttribute("content")).get(0).getValue();
				if( value > moneyMax){
					moneyMax = value;
					best = id;
				}
			}
		}
 		
		return best;
	}
	
	
	/**
	 * recherche de chemin
	 * @param root
	 * @param dest
	 * @return
	 */
	public List<Node> searchPath(String root, String dest){
		Graph myGraph = ((CleverAgent)this.myAgent).getGraph();

		Dijkstra dijk = new Dijkstra(Dijkstra.Element.NODE, null, null);
		dijk.init(myGraph);
		dijk.setSource(myGraph.getNode(root));
		dijk.compute();

		List<Node> path = dijk.getPath(myGraph.getNode(dest)).getNodePath();

		return path ;
	}	
	
	
	
	
	
	public void action() {
		// TODO choisir un tresor a ramasser
		// mettre sont type dans les pages jaunes si pas fait
		//chercher une nouvelle coalition
		//chercher l'agent concerné
		
		state = ((CleverAgent)myAgent).getPickingState();
		String myPos = ((mas.abstractAgent)this.myAgent).getCurrentPosition();
		Graph myGraph = ((CleverAgent)this.myAgent).getGraph();
		
		switch(state){
			case 0:
				//choisir trésor ...
				System.out.println(myAgent.getLocalName().toString()+" se prepare à chercher trésor");
				
				String pos_tresor = chooseTreasure();
				((CleverAgent)myAgent).setTreasureToFind(pos_tresor);
				System.out.println(myAgent.getLocalName().toString()+" a ciblé le trésor en position "+pos_tresor);
				
				((CleverAgent)myAgent).setPickingState(state+1);
				break; 
				
			case 1:
				//...aller jusqu'au trésor
				String dest = ((CleverAgent)this.myAgent).getTreasureToFind();
				
				Dijkstra dijk = new Dijkstra(Dijkstra.Element.NODE, null, null);
				dijk.init(myGraph);
				dijk.setSource(myGraph.getNode(myPos));
				dijk.compute();

				path = dijk.getPath(myGraph.getNode(dest)).getNodePath();
				((CleverAgent)myAgent).setPickingState(state+1);
				break ;
				
			case 2 :
				//si le prochain noeud du chemin a ete atteint
				if(!path.isEmpty() && myPos.equals(path.get(0).getId())){
					path.remove(0);
					if(!path.isEmpty())
						((mas.abstractAgent)this.myAgent).moveTo(path.get(0).getId());
				}
				//si le noeud tresor a ete atteint
				else if(path.isEmpty()){
					Attribute a = ((List<Attribute>) myGraph.getNode(((CleverAgent)myAgent).getTreasureToFind()).getAttribute("content")).get(0);
					System.out.println("My current backpack capacity is:"+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					System.out.println("Value of the treasure on the current position: "+a.getValue());
					System.out.println("Type of the treasure on the current position: "+a.getName());
					int taken = ((mas.abstractAgent)this.myAgent).pick();
					System.out.println("The agent grabbed :"+taken);
					System.out.println("the remaining backpack capacity is: "+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					
					//si on ignorait son type le mettre à jour
					if(((CleverAgent)myAgent).getType().equals("")){
						if(taken > 0){
							((CleverAgent)myAgent).setType(a.getName());
						} else {
							if(a.getName().equals("Treasure"))
								((CleverAgent)myAgent).setType("Diamonds");
							else
								((CleverAgent)myAgent).setType("Treasure");
						}
						//l'indiquer dans le DF
						DFAgentDescription dfd = new DFAgentDescription();
						dfd.setName(super.myAgent.getAID()); 
						ServiceDescription sd = new ServiceDescription();
						sd.setType("type");
						sd.setName(((CleverAgent)myAgent).getType());
						System.out.println(myAgent.getLocalName().toString()+" est de type "+((CleverAgent)myAgent).getType());
						dfd.addServices(sd);
						try {
							DFService.register(super.myAgent, dfd );
						} catch (FIPAException fe) { fe.printStackTrace(); }
					}
					((CleverAgent)myAgent).setPickingState(state+1);
				}	
				else{
					//TODO: cas interblocage
				}
				break;
				
				
			case 3:
				//...etchercher une coalition pour ce trésor
				System.out.println(myAgent.getLocalName().toString()+" cherche une coalition");
				//searchCoalition(pos_tresor);
			
			case 4:
				
				
				
			default:
				break;
			
		}
		
	}


	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}
}
