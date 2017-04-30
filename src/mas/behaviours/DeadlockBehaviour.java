package mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import mas.agents.CleverAgent;
import mas.agents.Data;

public class DeadlockBehaviour extends SimpleBehaviour{

	/**
	 * Gere les cas d'interblocages apres avoir fini l'exploration
	 */
	private static final long serialVersionUID = 8484925065523685732L;
	private int state = 0;
	private int exit_value = 0;
	private AID agent ; 
	private final int waitingTime = 7;
	private int cptWait = 0;
	private List<String> otherAgentPath ;
	private List<Node> cheminCarrefour ;
	
	public DeadlockBehaviour(mas.abstractAgent myagent) {
		super(myagent);
	}

	public void action() {

		exit_value = 0 ;
		state = ((CleverAgent)super.myAgent).getInterblocageState();
		
		switch(state){
			case 0:
				System.out.println(super.myAgent.getLocalName()+": Je suis en interblocage");
				
				//attendre le message de l'agent avec qui on est en interblocage
				final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.FAILURE);			
				ACLMessage answer = this.myAgent.receive(msgTemplate);
				
				// le message recu contiendra : notre position_la position de l'autre
				if(answer != null && answer.getContent().equals(((mas.abstractAgent)this.myAgent).getCurrentPosition()+"_"+((CleverAgent)this.myAgent).getChemin().get(0))){
					agent = answer.getSender(); 
					cptWait=0;
					((CleverAgent)this.myAgent).setInterblocageState(state+1);
				} else {
					answer = null;
					block(1500);
					cptWait++;
				}
				//si temps d'attente trop long on revient � Explore
				if(cptWait==waitingTime && answer==null){
					System.out.println("Temps d'attente trop long pour "+this.myAgent.getLocalName()+" qui quitte l'interblocage");
					((CleverAgent)this.myAgent).setInterblocage(false);
				}
				break ;				
				
			case 1:
				List<Node> carrefour = calculDistanceCarrefour() ;
				// dans l'ordre alphabetique le premier agent est celui qui devra envoyer le message avec sa distance ...
				if(super.myAgent.getLocalName().compareTo(agent.getLocalName())<0){
					ACLMessage message = new ACLMessage(ACLMessage.INFORM_REF);
					message.setSender(super.myAgent.getAID()); message.addReceiver(agent);
					try {
						List<String> idChemin = convertNodeToId(((CleverAgent)super.myAgent).getChemin());
						Data<Integer, List<String>, Integer, Integer> toSend = new Data<Integer, List<String>, Integer, Integer>(carrefour.size(), idChemin, null, null);
						message.setContentObject(toSend);
					} catch (IOException e) {
						e.printStackTrace();
					}
					((mas.abstractAgent)this.myAgent).sendMessage(message);
					try {
						System.out.println(super.myAgent.getLocalName()+" envoie sa distance carrefour "+message.getContentObject().toString()+" a "+agent.getLocalName());
					} catch (UnreadableException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					//...puis attendre la reponse de l'agent2
					message = null ;
					do {
						message = super.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF));
						cptWait++;
						block(1000);
					} while(message ==null&& cptWait<=5*waitingTime);
					
					cptWait=0;
					if(message != null && message.getContent().equals("not you")){
						((CleverAgent)super.myAgent).setInterblocageState(5);
					}else{
						if(message==null){
							System.out.println(myAgent.getLocalName().toString()+" ne sait pas qui doit bouger !");
							((CleverAgent)super.myAgent).setInterblocageState(6);
							((CleverAgent)super.myAgent).setInterblocage(false);
						} else {
							try {
								otherAgentPath =( (Data<String,List<String>,Integer,Integer>) message.getContentObject()).getSecond() ;
							} catch (UnreadableException e) {
								e.printStackTrace();
							}
							cheminCarrefour = carrefour;
							((CleverAgent)super.myAgent).setInterblocageState(4);
						}
					}
						
				} 
				else {
					// agent2 attend la distance de l'autre ...
					ACLMessage distanceMsg ;
					do{
						distanceMsg = super.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF));
						cptWait++;
						block(1000);
					} while(distanceMsg ==null && cptWait<=5*waitingTime);
					cptWait=0;
					if(distanceMsg==null){
						System.out.println(myAgent.getLocalName().toString()+" n'a pas re�u la distance!");
						((CleverAgent)super.myAgent).setInterblocageState(6);
						((CleverAgent)super.myAgent).setInterblocage(false);
					} else {
						//...puis envoie qui doit bouger
						int otherDistance=0;
						try {
							otherDistance = ((Data<Integer, List<String>,Integer, Integer>)distanceMsg.getContentObject()).getFirst();
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
						ACLMessage decideWhoMoves = new ACLMessage(ACLMessage.INFORM_REF);
						decideWhoMoves.setSender(super.myAgent.getAID()); decideWhoMoves.addReceiver(agent);
						
						if(otherDistance < (int)carrefour.size()){
							List<String> idChemin = convertNodeToId(((CleverAgent)super.myAgent).getChemin());
							Data<String, List<String>,Integer,Integer> toSend = new Data<String,List<String>,Integer,Integer>("you",idChemin,null,null);
							try {
								decideWhoMoves.setContentObject(toSend);
							} catch (IOException e) {
								e.printStackTrace();
							}
							((mas.abstractAgent)this.myAgent).sendMessage(decideWhoMoves);
							System.out.println(super.myAgent.getLocalName()+" envoie 'c est a toi de bouger' a "+agent.getLocalName());
							
							((CleverAgent)super.myAgent).setInterblocageState(5);
						} else {
							decideWhoMoves.setContent("not you");
							((mas.abstractAgent)this.myAgent).sendMessage(decideWhoMoves);
							System.out.println(super.myAgent.getLocalName()+" envoie 'c est a moi d bouger' a "+agent.getLocalName());
							try {
								otherAgentPath =( (Data<Integer,List<String>,Integer,Integer>) distanceMsg.getContentObject()).getSecond() ;
							} catch (UnreadableException e) {
								e.printStackTrace();
							}
							cheminCarrefour = carrefour;
							((CleverAgent)super.myAgent).setInterblocageState(4);
						}
					}
				}
				break;
				
				
				
			case 2:
				// il faut ajouter un noeud voisin au noeud carrefour pour le d�placement
				// le noeud ne doit pas etre sur le chemin de l'autre agent
				System.out.println(myAgent.getLocalName().toString()+" va jusqu'au carrefour");
				String idCarrefour = (cheminCarrefour.isEmpty())? ((mas.abstractAgent)super.myAgent).getCurrentPosition() : cheminCarrefour.get(cheminCarrefour.size()-1).getId() ; 
				
				Iterator<Node> carrefourIterator = ((CleverAgent)super.myAgent).getGraph().getNode(idCarrefour).getNeighborNodeIterator();
				ArrayList<Node> neighbours = new ArrayList<Node>();
				Node neighbour;
				
				Random r= new Random();
				int index;
				
				while (carrefourIterator.hasNext()){
					neighbours.add(carrefourIterator.next());
				}
				
				do{
					index = r.nextInt(neighbours.size());
					neighbour = neighbours.get(index);
				}while(otherAgentPath.contains(neighbour.getId()));
				
				cheminCarrefour.add(neighbour);
				
				
				((CleverAgent)super.myAgent).setChemin(cheminCarrefour);
				//signaler � l'autre qu'il peut bouger ?
				ACLMessage youCanMoveMsg = new ACLMessage(ACLMessage.CONFIRM);
				youCanMoveMsg.setSender(super.myAgent.getAID()); youCanMoveMsg.addReceiver(agent);
				youCanMoveMsg.setContent("move");
				((mas.abstractAgent)this.myAgent).sendMessage(youCanMoveMsg);
				System.out.println(myAgent.getLocalName().toString()+" dit que "+agent.getLocalName().toString()+" peut bouger");
				((CleverAgent)super.myAgent).setInterblocageState(6);
				((CleverAgent)super.myAgent).setInterblocage(false);
				break ;
				
			case 3 : //L'AGENT ATTEND QUE L'AUTRE BOUGE
				ACLMessage moveAnswer = super.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
				if(moveAnswer!=null){
					//attend un peu avant de bouger
					block(1000);
					System.out.println(myAgent.getLocalName().toString()+" peut bouger !");
					((CleverAgent)super.myAgent).setInterblocageState(0);
					((CleverAgent)super.myAgent).setInterblocage(false);
				}
				break;

				
					
			default:
				break;
		
		}
		
		
	}

	public List<Node> calculDistanceCarrefour() {
		
		String pos = ((mas.abstractAgent)super.myAgent).getCurrentPosition();
		Graph graph = ((CleverAgent)super.myAgent).getGraph() ;
		int min = Integer.MAX_VALUE;
		Node closest = null;
		
		//Dijkstra
		Dijkstra dijk = new Dijkstra(Dijkstra.Element.NODE, null, null);
		dijk.init(graph);
		dijk.setSource(graph.getNode(pos));
		dijk.compute();
		
		for (Node n : graph){
			if(n.getDegree() >= 3){
				double length = dijk.getPathLength(n);
				if(length < min){
					min = (int) length ;
					closest = n ;
				}
			}
		}

		List<Node> shortPath = dijk.getPath(closest).getNodePath();
		shortPath.remove(0);
		
		return shortPath ;

	}
	
	public List<String> convertNodeToId(List<Node> path){
		List<String> idChemin = new ArrayList<String>() ;
		for (Node n : path){
			idChemin.add(n.getId());
		}
		
		return idChemin;
	}

	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}
}
