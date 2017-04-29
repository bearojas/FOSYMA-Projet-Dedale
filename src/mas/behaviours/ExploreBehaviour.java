package mas.behaviours;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import mas.agents.CleverAgent;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import env.Attribute;
import env.Couple;


public class ExploreBehaviour extends SimpleBehaviour {
	
	private static final long serialVersionUID = 9088209402507795289L;
	private boolean finished = false;
	private Graph graph ;
	private List<Node> chemin;
	private ArrayList<String> opened ;
	private int step = 0;
	private int immo = 0;
	private final int MAX_STEP = 10;
	//TODO A SUPPRIMER mais on peut le laisser en fait
	private int FIN=0;
	/**
	 * exit value : 0 -> explore
	 * 				2 -> communication Time 
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
	 * @param myGraph : le graphe � parcourir
	 * @param root : noeud racine
	 * @param open : liste des noeuds non visit�s
	 * @return le plus court chemin sans la racine vers le noeud ouvert le plus proche
	 */
	public List<Node> search(Graph myGraph,Node root, ArrayList<String> open){

		Dijkstra dijk = new Dijkstra(Dijkstra.Element.NODE, null, null);
		dijk.init(myGraph);
		dijk.setSource(root);
		dijk.compute();

		int min = Integer.MAX_VALUE;
		String shortest = null;
		
		for(String id : open){
			double l = dijk.getPathLength(myGraph.getNode(id));
			if(l < min){
				min = (int) l;
				shortest = id ;
			}
		}	
		List<Node> shortPath = dijk.getPath(myGraph.getNode(shortest)).getNodePath();
		//shortPath.remove(0);
		return shortPath ;
	}
	
	/**
	 * D�placement de l'agent qui suit un chemin : traite les interblocages
	 */
	public void followPath(){
		String myPosition=((mas.abstractAgent)this.myAgent).getCurrentPosition();

		if(myPosition.equals(chemin.get(0).getId()))
			chemin.remove(0);

		Node next = chemin.get(0);
		//TODO
		/*
		 * si on a pas pu se d�placer il y a un agent qui nous bloque
		 * rentrer en communication avec lui
		 * dans interblocage state : 0 -> attente d'un message d'interblocage aussi
		 */
		if(!((CleverAgent) this.myAgent).getMoved()){
			//chemin.add(0,next); //pour conserver le chemin en entier, le noeud bloqu� est donc le premier du chemin et destination le dernier
			
			((CleverAgent)this.myAgent).setInterblocage(true);	
			((CleverAgent)this.myAgent).setInterblocageState(0);
			refreshAgent();
			System.out.println("INTERBLOCAGE pour agent "+myAgent.getName()+" qui veut aller en "+next.getId());
			final ACLMessage mess = new ACLMessage(ACLMessage.PROPOSE);
			mess.setSender(this.myAgent.getAID()); mess.setContent(next.getId()+"_"+myPosition); //le noeud qui nous bloque_o� on est
			
			Set<AID> cles = ((CleverAgent)this.myAgent).getAgentList().keySet();		
			for (AID aid : cles){
				mess.addReceiver(aid);
			}
			((mas.abstractAgent)this.myAgent).sendMessage(mess);
			((CleverAgent)this.myAgent).setAgentsNearby(new ArrayList<AID>());
			exitValue = 3;
			step = 0;
			immo = 0;
			finished = true ;
		}
		else{
			((mas.abstractAgent)this.myAgent).moveTo(next.getId());
		}
			
	}

	
	
	@Override
	public void action() {
	
		exitValue= 0 ;
		
		String myPosition=((mas.abstractAgent)this.myAgent).getCurrentPosition();
		
		graph = ((CleverAgent) myAgent).getGraph();
		opened = ((CleverAgent) myAgent).getOpened();
		chemin = ((CleverAgent) myAgent).getChemin();
		//attention: cache l'exception IdAlreadyInUse
		graph.setStrict(false);
		
		//si on a pas changé de position et on était déjà en exloration -> on a pas pu se deplacer
		if(((CleverAgent)super.myAgent).getLastPosition().equals(myPosition) && step >  0)
			((CleverAgent)super.myAgent).setMoved(false);
		//mise a jour de la position
		else{
			((CleverAgent)super.myAgent).setLastPosition(myPosition);
			((CleverAgent)super.myAgent).setMoved(true);
			immo = 0;
		}
		
		if (myPosition!=""){

			//List of observable from the agent's current position
			List<Couple<String,List<Attribute>>> lobs=((mas.abstractAgent)this.myAgent).observe();
			if(!opened.isEmpty())
				System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);
			
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
			
			
			// create node from current position
			// delete current node from opened
			Node root = graph.addNode(myPosition);
			root.addAttribute("state", "closed");
			root.addAttribute("content",lattribute);
			
			//mise a jour de liste des tresors et diamants
			if(lattribute != null){
				for(Attribute a: lattribute){
					if(a.getName().equals("Treasure")){
						ArrayList<String> treasures = ((CleverAgent)this.myAgent).getTreasures();
						if(!treasures.contains(root.getId()))
							treasures.add(root.getId());
						((CleverAgent)this.myAgent).setTreasures(treasures);
					}
					else if(a.getName().equals("Diamonds")){
						ArrayList<String> diamonds = ((CleverAgent)this.myAgent).getDiamonds();
						if(!diamonds.contains(root.getId()))
							diamonds.add(root.getId());
						((CleverAgent)this.myAgent).setDiamonds(diamonds);
					}
				}
			}
			
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
					System.out.println("Value of the treasure on the current position: "+a.getName());
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
			
			//If there is a message in the inbox of someone trying to exchange maps
			//save the sender and finish this behaviour
			final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);			
			final ACLMessage msg = this.myAgent.receive(msgTemplate);
			
			//If someone is blocked by this agent
//			final MessageTemplate msgTemp = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);			
//			final ACLMessage blockMsg = this.myAgent.receive(msgTemp);
			
			// TODO: lastCom
			//ArrayList<AID> lastCom = ((CleverAgent)super.myAgent).getLastCom();
			
			//si l'exp�diteur est qq'un avec qui on a communiqu� r�cemment, ignorer
			//if(msg != null && !msg.getContent().equals("ok") && !lastCom.subList(0, lastCom.size()/4).contains(msg.getSender())){
			
			if(msg != null && !msg.getContent().equals("ok")){
				ArrayList<AID> sender = new ArrayList<AID>();
				sender.add((AID) msg.getSender());
				((CleverAgent) super.myAgent).setAgentsNearby(sender);
				((CleverAgent) super.myAgent).setCommunicationState(2);
				refreshAgent();
				
				String content = msg.getContent();
				HashMap<AID, ArrayList<String>> agentList = ((CleverAgent)this.myAgent).getAgentList();
				//si je n'ai pas sa position initiale (et sa capacite)
				if( agentList.get(sender.get(0)).get(0) == "" && !content.equals("ok")){
					String[] tokens = content.split("[:]");
					agentList.get(sender.get(0)).set(0, tokens[0]); //position
					agentList.get(sender.get(0)).set(1, tokens[1]); //capacite
					((CleverAgent)this.myAgent).setAgentList(agentList);
					System.err.println(this.myAgent.getLocalName()+" a initialise pos: "+tokens[0]+" et cap: "+tokens[1]+" de "+sender.get(0).getLocalName());
				}
								
				step = 0;
				finished = true;
				exitValue = 2;
				System.out.println(this.myAgent.getLocalName()+" is in Explore and has a new message in the mailbox "+msg.getContent());
			}
			
			
//			//TODO: facon de vider la boite aux lettres de messages d'interblocage 
//			//si on est en interblocage on change de behaviour sinon on ignore
//			else if(blockMsg != null){
//				if(chemin.size() != 0 && blockMsg.getContent().equals( myPosition+"_"+chemin.get(0))){
//					((CleverAgent)this.myAgent).setInterblocageState(1); //on passe directement en 1					
//					((CleverAgent)this.myAgent).setInterblocage(true);	
//					
//					AID sender = blockMsg.getSender();
//
//					ArrayList<AID> receiver = new ArrayList<AID>();
//					receiver.add(sender);
//					((CleverAgent)this.myAgent).setAgentsNearby(receiver);
//					
//					System.out.println("INTERBLOCAGE avec "+sender.toString()+" pour agent "+myAgent.getName()+" qui veut aller en "+chemin.get(0));
//					final ACLMessage mess = new ACLMessage(ACLMessage.PROPOSE);
//					mess.setSender(this.myAgent.getAID()); mess.setContent(chemin.get(0)+"_"+myPosition); //le noeud qui nous bloque_ou on est
//					mess.addReceiver(sender);
//					((mas.abstractAgent)this.myAgent).sendMessage(mess);
//
//					refreshAgent();
//					exitValue = 3;
//					step = 0;
//					immo = 0;
//					finished = true ;
//					
//				}
//				//si pas en interblocage mais l'autre agent est dans un voisin, on enleve ce voisin pour eviter d'y aller
//				else{
//					for(String v: neighbors){
//						if( blockMsg.getContent().equals( myPosition+"_"+v)){
//							neighbors.remove(v);
//							break;
//						}
//					}
//					
//				}
//				
//			}
					
			//tous les MAX_STEP temps, on echange la map a ceux proches de nous			
			else if(step>=MAX_STEP){
				((CleverAgent) super.myAgent).setCommunicationState(0);
				refreshAgent();
				step = 0;
				immo = 0;
				finished = true ;
				exitValue = 2;
				System.out.println("COMMUNICATION TIME for "+myAgent.getName());
				
			} else {
				//si on n'a plus de noeuds ouverts, l'exploration est finie
				if(opened.isEmpty()){
					if(FIN == 0)
					//finished = true;
						System.err.println("Exploration finie pour "+myAgent.getLocalName()+": "+graph.getNodeCount()+"noeuds");
					block(10000);
					Random r= new Random();
					int moveId=r.nextInt(lobs.size());
					((mas.abstractAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());
					FIN++;
					//this.myAgent.doDelete();
				}
				else{
					step++;
					//si on a un chemin a suivre
					//et que ce chemin ne reduit pas a ma position
					if(chemin.size() > 1 || (!chemin.isEmpty() && !chemin.get(0).getId().equals(myPosition))){
						followPath();
					}
					else{
						chemin = new ArrayList<Node>();
						//si on a un voisin ouvert 
						if(neighbors.size()!= 0){
							Random r= new Random();
							int i = r.nextInt(neighbors.size());
							Node next = graph.getNode(neighbors.get(i));
							System.out.println(myAgent.getLocalName()+" va en "+ next.getId());
							immo++;

							//TODO:si on a essaye trop de fois de bouger vers un voisin ouvert -> chercher un autre chemin	
							if(!((CleverAgent)super.myAgent).getMoved() && immo > 4){							
								chemin = search(graph, root, opened);
								followPath();			
							}
							else{
								((mas.abstractAgent)this.myAgent).moveTo(next.getId());
								refreshAgent();
							}
							
						}
						else{
							// si pas de voisins
							//on cherche le noeud le plus proche						
							chemin = search(graph, root, opened);
							System.out.println(this.myAgent.getLocalName()+" : Je cherche un nouveau chemin");
							followPath();
							
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
		refreshAgent();
		return exitValue;
	}
	
	@Override
	public boolean done() {
		return finished;
	}


}
