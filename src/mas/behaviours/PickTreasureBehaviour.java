package mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import env.Attribute;
import env.Couple;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import mas.abstractAgent;
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
		Graph graph = ((CleverAgent)myAgent).getGraph(); 
		HashMap<AID, ArrayList<String>> agentList = ((CleverAgent)myAgent).getAgentList();
		/*cette fonction est appelé une fois qu'on a ramassé trésor
		 * s'il ne reste rien :  on retourne l'id de l'agent qui a la plus grosse capacité juste après nous
		 * s'il reste des pieces :
		 * 	on parcout l'ensemble des agents :
		 * 		si un agent peut prendre tout tout seul (=) on le retourne et on break
		 * 		sinon si la somme de plusieurs agents peuvent le ramasser (=) on retourne l'agent qui a la plus grande capacité dans cet ensemble 
		 */
		int remain = (Integer)((List<Attribute>) graph.getNode(treasure).getAttribute("content")).get(0).getValue();
		String typeTreasure = ((List<Attribute>) graph.getNode(treasure).getAttribute("content")).get(0).getName();
		AID nextAgent = null; //retourne null si aucun agent ne peut aller chercher le trésor? Non, il faut alerter un autre agent
		
		if(remain == 0){
			int bigger = 0;
			for(AID aid : agentList.keySet()){
				int value = Integer.parseInt(agentList.get(aid).get(1));
				if (value>bigger){
					bigger = value ;
					nextAgent=aid;
				}
				((CleverAgent)myAgent).setTreasureToFind(""); // il n'y a plus de trésor a chercher
				//mise a jour du trésor
				//graph.getNode(((mas.abstractAgent)this.myAgent).getCurrentPosition()).setAttribute("content", values);;
			}
		} else{
			//formation de toutes les coalitions possibles (ArrayList<ArrayList<String>>) pour ce type de tresor
			ArrayList<ArrayList<AID>> coalitions = new ArrayList<ArrayList<AID>>();
			//coalitions à 1 agent
			for(AID aid : agentList.keySet()){
				//si l'agent est de type ce trésor ou type inconnu on l'ajoute aux coalitions
				if(agentList.get(aid).get(2).equals(typeTreasure)|| agentList.get(aid).get(2).equals("")){
					//si l'agent correspond exactement a la quantite, c'est lui qu'il faut prévenir
					if(Integer.parseInt(agentList.get(aid).get(1))==remain){
						nextAgent = aid ;
						return nextAgent;
					} else {
						ArrayList<AID> col = new ArrayList<AID>(); col.add(aid);
						coalitions.add(col);
					}
				}
			}
			//coalitions multi agents qu'avec les agents qui sont du bon type
			if(!coalitions.isEmpty()){
				int i=2;
				int nbAgents = coalitions.size();
				//formation coalitions à i agents
				while(i<= nbAgents){
					ArrayList<ArrayList<AID>> tmp = new ArrayList<ArrayList<AID>>();
					for (ArrayList<AID> coal : coalitions){
						//on prend une coalition et on la concatène avec une autre si la coalition n'est pas deja dans la liste
						for(ArrayList<AID> coal2 : coalitions){
							if(! coal.equals(coal2)){
								ArrayList<AID> newCoal = new ArrayList<AID>();
								newCoal.addAll(coal);
								for(int j=0; j<coal2.size();j++){
									if(! coal.contains(coal2.get(j)))
										newCoal.add(coal2.get(j));
								}
								if(! tmp.contains(newCoal))
									tmp.add(newCoal);
							}
						}
					}
					coalitions.addAll(tmp);
					i++;
				}
				
				//retourner la meilleure coalition
				ArrayList<AID> bestCoa = getBestCoalition(coalitions, remain);
				//parmi elle, prendre l'agent de plus grosse capacite
				int bigger = 0;
				for(AID aid : bestCoa){
					int value = Integer.parseInt(agentList.get(aid).get(1));
					if (value>bigger){
						bigger = value ;
						nextAgent=aid;
					}
				}
				
				
			}else {
				//aucun agent ne correspond au type du tresor : chercher le prochain plus gros agent
				int bigger = 0;
				for(AID aid : agentList.keySet()){
					int value = Integer.parseInt(agentList.get(aid).get(1));
					if (value>bigger){
						bigger = value ;
						nextAgent=aid;
					}
				}
			}
		}
		
		return nextAgent;
	}
	
	
	
	
	
	/**
	 * retourne la meilleure coalition pour le montant de tresor indiqué
	 * @param coalitions
	 * @param quantite
	 */
	private ArrayList<AID> getBestCoalition(ArrayList<ArrayList<AID>> coalitions, int quantite){
		HashMap<AID, ArrayList<String>> agentList = ((CleverAgent)myAgent).getAgentList();
		ArrayList<ArrayList<AID>> equalCoalition = new ArrayList<ArrayList<AID>>();
		ArrayList<ArrayList<AID>> moreCoalition = new ArrayList<ArrayList<AID>>();
		ArrayList<ArrayList<AID>> lessCoalition = new ArrayList<ArrayList<AID>>() ;
		
		for (ArrayList<AID> coalition : coalitions) {
			//faire la somme des capacites des membres de la coalition
			int sum_coa = 0;
			for (int i =0; i<coalition.size(); i++){
				sum_coa += Integer.parseInt(agentList.get(coalition.get(i)).get(1));
			}
			//ajouter la coalition au bon groupe
			if(sum_coa == quantite)
				equalCoalition.add(coalition);
			if(sum_coa > quantite)
				moreCoalition.add(coalition);
			if(sum_coa<quantite)
				lessCoalition.add(coalition);
		}
		ArrayList<AID> bestCoa = new ArrayList<AID>();
		int taille = 999999;
		if(! equalCoalition.isEmpty()){
			//si une coalition peut ramasser exactement, prendre celle de plus petite taille
			for(ArrayList<AID> c : equalCoalition){
				if (c.size()<taille){
					taille=c.size();
					bestCoa = c;
				}
			}
			return bestCoa;
			
		} else if(! moreCoalition.isEmpty()){
			//si aucune coalition ne peut ramasser exactement, prendre ceux qui peuvent ramasser  de plus petite taille
			for(ArrayList<AID> c : moreCoalition){
				if (c.size()<taille){
					taille=c.size();
					bestCoa = c;
				}
			}
			return bestCoa;
			
		} else {
			//si aucune coalition ne peut ramasser le trésor en entier, prendre celle de plus petite taille qui prendrace qu'elle peut
			for(ArrayList<AID> c : lessCoalition){
				if (c.size()<taille){
					taille=c.size();
					bestCoa = c;
				}
			}
			return bestCoa;
		}
	}
	
	
	
	/**
	 * Choisir le trésor à aller chercher
	 * @return la position du trésor à chercher
	 */
	private String chooseTreasure(){
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
		// mettre son type dans les pages jaunes si pas fait
		//chercher une nouvelle coalition
		//chercher l'agent concerné
		
		
		state = ((CleverAgent)myAgent).getPickingState();
		String myPos = ((mas.abstractAgent)this.myAgent).getCurrentPosition();
		Graph myGraph = ((CleverAgent)this.myAgent).getGraph();
		
		switch(state){
			case 0:
				//choisir trésor ...
				System.out.println(myAgent.getLocalName().toString()+" se prepare à chercher trésor");
				
				String pos_tresor = ((CleverAgent)myAgent).getTreasureToFind();
				if(pos_tresor.equals(""))
					pos_tresor = chooseTreasure();
				((CleverAgent)myAgent).setTreasureToFind(pos_tresor);
				System.out.println(myAgent.getLocalName().toString()+" a ciblé le trésor en position "+pos_tresor);
				
				((CleverAgent)myAgent).setPickingState(state+1);
				break; 
				
			case 1:
				//...aller jusqu'au trésor
				String dest = ((CleverAgent)this.myAgent).getTreasureToFind();

				path = searchPath(myPos, dest);
				
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
					//mettre a jour le graph
					List<Couple<String,List<Attribute>>> lobs=((mas.abstractAgent)this.myAgent).observe();
					//on determine l'indice de la position courante dans lobs 
					int posIndex = 0;
					for(int i = 0; i < lobs.size(); i++){
						if(lobs.get(i).getLeft() == myPos){
							posIndex = i;
							break;
						}
					}
					List<Attribute> lattribute= lobs.get(posIndex).getRight();
					myGraph.getNode(myPos).setAttribute("content",lattribute);
					((CleverAgent)myAgent).setGraph(myGraph);
					//si on ignorait son type le mettre à jour
					if(((CleverAgent)myAgent).getType().equals("")){
						//si on a pris on est de ce type
						if(taken > 0){
							((CleverAgent)myAgent).setType(a.getName());
						} else {
							//si on a rien pris mais qu'il y a avait bien quelque chose !
							if((Integer)a.getValue()!=0){
								if(a.getName().equals("Treasure"))
									((CleverAgent)myAgent).setType("Diamonds");
								else
									((CleverAgent)myAgent).setType("Treasure");
							}
						}
						//l'indiquer dans le DF
//						DFAgentDescription dfd = new DFAgentDescription();
//						dfd.setName(super.myAgent.getAID()); 
//						ServiceDescription sd = new ServiceDescription();
//						sd.setType("type");
//						sd.setName(((CleverAgent)myAgent).getType());
						System.out.println(myAgent.getLocalName().toString()+" est de type "+((CleverAgent)myAgent).getType());
//						dfd.addServices(sd);
//						try {
//							DFService.register(super.myAgent, dfd );
//						} catch (FIPAException fe) { fe.printStackTrace(); }
					}
					((CleverAgent)myAgent).setPickingState(state+1);
				}	
				else{
					//TODO: cas interblocage
				}
				break;
				
				
			case 3:
				//...et chercher une coalition pour ce trésor
				System.out.println(myAgent.getLocalName().toString()+" cherche une coalition");
				AID agentToReach = searchCoalition(((CleverAgent)myAgent).getTreasureToFind());
				((CleverAgent)myAgent).setAgentToReach(agentToReach);
				System.out.println("Meilleure coalition : "+agentToReach.getLocalName().toString());
				String pos_nextAgent = ((CleverAgent)myAgent).getAgentList().get(agentToReach).get(0);
				System.out.println(myAgent.getLocalName().toString()+" va contacter "+agentToReach.getLocalName().toString()+" se trouvant en "+pos_nextAgent);
				
				//se placer au noeud voisin 
				String near;
				myGraph.getNode(pos_nextAgent);
				if(myGraph.getNode(pos_nextAgent).getNeighborNodeIterator().hasNext())
					near = myGraph.getNode(pos_nextAgent).getNeighborNodeIterator().next().getId();
				else
					near = pos_nextAgent;
				path = searchPath(myPos, near);
				((CleverAgent)myAgent).setPickingState(state+1);
				break ;
				
			case 4:
				//... se déplacer jusqu'au noeud voisin de la position de l'agent
				//si le prochain noeud du chemin a ete atteint
				if(!path.isEmpty() && myPos.equals(path.get(0).getId())){
					path.remove(0);
					if(!path.isEmpty())
						((mas.abstractAgent)this.myAgent).moveTo(path.get(0).getId());
				}
				//si le noeud initial a ete atteint
				else if(path.isEmpty()){
					System.out.println(myAgent.getLocalName().toString()+" est prêt de celui qu'il veut contacter ");
					((CleverAgent) super.myAgent).setPickingState(state+1);
				}	
				else{
					//TODO: cas interblocage
				}
				break;
				
			case 5 :
				//on contacte l'agent : on lui envoie la liste des infos des agents ET le tresor modifié
				// sauf que dans cette liste on a remplacé la case de l'agent contacté par notre case
				AID agent = ((CleverAgent)myAgent).getAgentToReach();
				System.out.println(myAgent.getLocalName().toString()+" va contacter "+agent.getLocalName().toString());
				
				HashMap<AID, ArrayList<String>> agentList = ((CleverAgent)myAgent).getAgentList();
				agentList.remove(agent);
				ArrayList<String> my_info = new ArrayList<String>(Arrays.asList(((CleverAgent)myAgent).getFirstPosition(),((mas.abstractAgent)myAgent).getBackPackFreeSpace()+"",((CleverAgent)myAgent).getType()));
				agentList.put(myAgent.getAID(),my_info );
				
				//on envoie d'abord la liste
				ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				msg.setSender(myAgent.getAID()); msg.addReceiver(agent);
				try {
					msg.setContentObject(agentList);
				} catch (IOException e) {
					e.printStackTrace();
				}
				((mas.abstractAgent)myAgent).sendMessage(msg);
				//puis les infos sur le trésor 
				msg = new ACLMessage(ACLMessage.CFP);
				msg.setSender(myAgent.getAID()); msg.addReceiver(agent);
				msg.setContent(((CleverAgent)myAgent).getTreasureToFind());
				
				((mas.abstractAgent)myAgent).sendMessage(msg);
				
				((CleverAgent)myAgent).setPickingState(state+1);
				break ;
				
			case 6 : 
				System.out.println(myAgent.getLocalName().toString()+" va rentrer chez lui");
				((CleverAgent) super.myAgent).setComingbackState(6);
				((CleverAgent)myAgent).setPickingState(state+1);
				break ;
				
			default:
				break;
			
		}
		
	}


	public boolean done() {
		return state == 7;
	}
}
