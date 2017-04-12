package mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.Graphs;

import env.Attribute;
import env.Couple;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import mas.agents.CleverAgent;


public class ExploreBehaviour extends SimpleBehaviour {
	
	private static final long serialVersionUID = 9088209402507795289L;
	private boolean finished = false;
	private Graph graph ;
	private List<Node> chemin;
	private ArrayList<String> opened ;
	private int step = 0;
	private final int MAX_STEP = 5;
	/**
	 * exit value : 0 -> explore
	 * 				1 -> communication Time 
	 * 				2 -> communication avec 1 agent
	 * 				3 -> interblocage sur un chemin
	 */
	private int exitValue = 0;
	
	public ExploreBehaviour(final mas.abstractAgent myagent){
		super(myagent);
		graph = ((CleverAgent) myagent).getGraph();
		opened = ((CleverAgent) myagent).getOpened();
		chemin = ((CleverAgent) myagent).getChemin();
		//attention: cache l'exception IdAlreadyInUse
		this.graph.setStrict(false);
	}
	

	/**
	 * fonction de recherche du plus court chemin vers le noeud ouvert le plus proche
	 * @param myGraph : le graphe ï¿½ parcourir
	 * @param root : noeud racine
	 * @param open : liste des noeuds non visitï¿½s
	 * @return le plus court chemin sans la racine vers le noeud ouvert le plus proche
	 */
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
		List<Node> shortPath = shortest.getNodePath();
		shortPath.remove(0);
		return shortPath ;
	}

	
	
	@Override
	public void action() {
	
		exitValue= 0 ;
		
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
			// list of agent's AID : agents who have already visited the node
			List<AID> agentsWhoKnowNode = new ArrayList<AID>()  ;
			agentsWhoKnowNode.add(myAgent.getAID());
//			for(Attribute a: lattribute){
//				System.out.println("name:"+a.getName());
//				System.out.println("valeur:"+a.getValue());
//			}
			
			System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);
			
			// create node from current position
			// delete current node from opened
			Node root = graph.addNode(myPosition);
			root.addAttribute("state", "closed");
			root.addAttribute("content",lattribute);
			root.addAttribute("haveBeenThere", agentsWhoKnowNode);
			opened.remove(myPosition);
			
			//add all neighbors of current node 
			//add them to Opened if not already closed 
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

//			System.out.println("noeuds ouverts: "+opened.toString());
			
			//Little pause to allow you to follow what is going on
//			try {
//				System.out.println("Press a key to allow the agent "+this.myAgent.getLocalName() +" to execute its next move");
//				System.in.read();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

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
			
			//If there is a message in the inbox (not an "accecpt com" message)
			//save the sender and finish this behaviour
			final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);			
			final ACLMessage msg = this.myAgent.receive(msgTemplate);
			ArrayList<AID> lastCom = ((CleverAgent)super.myAgent).getLastCom();
			//si l'expéditeur est qq'un avec qui on a communiqué récemment, ignorer
			if(msg != null && !msg.getContent().equals("ok") && !lastCom.subList(0, lastCom.size()/4).contains(msg.getSender())){
				ArrayList<AID> sender = new ArrayList<AID>();
				sender.add((AID) msg.getSender());
				((CleverAgent) super.myAgent).setAgentsNearby(sender);
				((CleverAgent) super.myAgent).setCommunicationState(2);
				refreshAgent();
				step = 0;
				finished = true;
				exitValue = 2;
				System.out.println(this.myAgent.getLocalName()+" is in Explore and has a new message in the mailbox "+msg.getContent());
			}
			
			//tous les MAX_STEP temps, on ï¿½change la map a ceux proches de nous			
			else if(step>=MAX_STEP){
				((CleverAgent) super.myAgent).setCommunicationState(0);
				refreshAgent();
				step = 0;
				finished = true ;
				exitValue = 1;
				System.out.println("COMMUNICATION TIME for "+myAgent.getName());
				
			} else {
				//si on n'a plus de noeuds ouverts, l'exploration est finie
				if(opened.isEmpty()){
					finished = true;
					System.out.println("Exploration finie: "+graph.getNodeCount()+"noeuds");
				}
				else{
					step++;
					//si on a un chemin a suivre
					if(chemin.size() != 0){
						Node next = chemin.remove(0);
						//TODO
						/*
						 * si on a pas pu se déplacer il y a un agent qui nous bloque
						 * rentrer en communication avec lui
						 * dans interblocage state : 0 -> attente d'un message d'interblocage aussi
						 */
						if(!((mas.abstractAgent)this.myAgent).moveTo(next.getId())){
							chemin.add(0,next); //pour conserver le chemin en entier, le noeud bloqué est donc le premier du chemin et destination le dernier
							((CleverAgent)super.myAgent).setInterblocage(true);	
							((CleverAgent)super.myAgent).setInterblocageState(0);
							refreshAgent();
							System.out.println("INTERBLOCAGE pour agent "+myAgent.getName()+" qui veut aller en "+next.getId());
							final ACLMessage mess = new ACLMessage(ACLMessage.PROPOSE);
							mess.setSender(this.myAgent.getAID()); mess.setContent(next.getId()+"_"+myPosition); //le noeud qui nous boque_où on est
							for (AID aid : ((CleverAgent)super.myAgent).getAgentList()){
								mess.addReceiver(aid);
							}
							this.myAgent.send(mess);
							exitValue = 3;
							finished = true ;
						}
						// tant qu'on n'a pas pu se dï¿½placer....
//						while(!((mas.abstractAgent)this.myAgent).moveTo(next.getId())){
//							// creation d'un graphe temporaire qui oblige ï¿½ chercher un autre chemin sans passer par le noeud bloquï¿½
//							System.out.println("recherche d'un chemin qui ne passe pas par "+next.getId());
//							
//							Graph tempGraph = Graphs.clone(graph);
//							tempGraph.removeNode(next);	
//							chemin = search(tempGraph,root, opened);
//							next = chemin.remove(0);
//							
//						}
					}
					else{
						//si on a un voisin ouvert 
						if(neighbors.size()!= 0){
//							System.out.println("VOISINS "+neighbors.toString());
							int i =0 ;
							Node next = graph.getNode(neighbors.get(i));
							System.out.println(myAgent.getLocalName()+" va en "+ next.getId());
							//TODO
							// si on ne peut pas aller vers son voisin
							while(!((mas.abstractAgent)this.myAgent).moveTo(next.getId())){
								i++ ;
//								// si on a fait toute la liste des voisins, on est en interblocage
//								if( i >= neighbors.size()){
//									((CleverAgent)super.myAgent).setInterblocageState(0);
//									((CleverAgent)super.myAgent).setInterblocage(true);	
//									refreshAgent();
//									System.out.println("INTERBLOCAGE pour agent "+myAgent.getName()+" qui veut aller en "+next.getId());
//									final ACLMessage mess = new ACLMessage(ACLMessage.PROPOSE);
//									mess.setSender(this.myAgent.getAID()); mess.setContent(next.getId()+"_"+myPosition); //le noeud qui nous boque_où on est
//									for (AID aid : ((CleverAgent)super.myAgent).getAgentList()){
//										mess.addReceiver(aid);
//									}
//									this.myAgent.send(mess);
//									exitValue = 3;
//									finished = true ;
//									//s'échanger les cartes ?
//								}
								if( i >= neighbors.size()){
									chemin = search(graph, root, opened);
									next = chemin.remove(0);
								} else { // soit on prend le voisin suivant
									next =graph.getNode(neighbors.get(i));
								}
							}
							
						}
						else{
							// si pas de voisins
							//on cherche le noeud le plus proche						
							chemin = search(graph, root, opened);
							Node next = chemin.remove(0); // on enlï¿½ve le noeud vers lequel on va aller pour ne pas le garder dans le chemin ï¿½ faire
							
							while(!((mas.abstractAgent)this.myAgent).moveTo(next.getId())){
								System.out.println("recherche d'un chemin qui ne passe pas par "+next.getId());
								// creation d'un graphe temporaire qui oblige ï¿½ chercher un autre chemin sans passer par le noeud bloquï¿½
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
		
	}
	  
	public void refreshAgent(){
		((CleverAgent) super.myAgent).setGraph(graph);
		((CleverAgent) super.myAgent).setChemin(chemin);
		((CleverAgent) super.myAgent).setOpened(opened);
	}
	
	public int onEnd(){
		return exitValue;
	}
	
	@Override
	public boolean done() {
		return finished;
	}


}
