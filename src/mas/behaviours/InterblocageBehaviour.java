package mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

public class InterblocageBehaviour extends SimpleBehaviour{

	private static final long serialVersionUID = 1L;
	private final int waitingTime = 5;
	private int cptWait = 0;
	private AID agent ; 
	private List<String> otherAgentPath ;
	private List<Node> cheminCarrefour ;
	/**
	 * exit_value : 0 -> Explore
	 * 				1 -> ExchangeMap
	 */
	private int exit_value = 0;

	
	public InterblocageBehaviour(final mas.abstractAgent myagent) {
		super(myagent);
	}
	//TODO
	/*
	 * SANS PRISE EN COMPTE DES TRESORS
	 * state 0 : attente d'un message d'interblocage puis passe en 1
	 * state 1 : echange des map à tous les agents en interblocage 
	 * state 2 : -> si regle le conflit, interblocage = false pour les deux
	 * 			 -> sinon on passe en state 3
	 * state intermediaire : définition des priorités ?
	 * state 3 : chacun regarde la plus courte distance à un carrefour
	 * 			 les agents envoient leur distance à un Agent coordinateur
	 * 			 le coordinateur décide qui bouge 
	 * 			 si doit bouger -> state 4
	 * 			 sinon state 5
	 * state 4 : on doit bouger : setChemin avec le chemin qu'on a trouvé jusqu'au carrefour
	 * 			 interblocage = false et on change le interblocage de l'autre agent aussi
	 * 			 on revient dans Explore
	 * state 5 : attente jusqu'au changement de interblocage par l'autre et on revient dans Explore
	 */
	@Override
	public void action() {
		
		exit_value = 0 ;
		int state = ((CleverAgent)super.myAgent).getInterblocageState();
		
		switch(state) {
		
			case 0 : // ATTENTE DU MESSAGE DE L'AGENT EN INTERBLOCAGE AVEC NOUS
				System.out.println(super.myAgent.getLocalName()+": Je suis en interblocage");
				//attendre le message de l'agent avec qui on est en interblocage
				final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);			
				ACLMessage answer = this.myAgent.receive(msgTemplate);
				// le message reçu contiendra : notre position_la position de l'autre
				if(answer != null && answer.getContent().equals(((mas.abstractAgent)super.myAgent).getCurrentPosition()+"_"+((CleverAgent)super.myAgent).getChemin().get(0))){
					agent = answer.getSender(); 
					((CleverAgent)super.myAgent).setInterblocageState(state+1);
				} else {
					answer = null;
					block(1500);
					cptWait++;
				}
				//si temps d'attente trop long on revient à Explore
				if(cptWait==waitingTime && answer==null){
					((CleverAgent)super.myAgent).setInterblocage(false);
				}
				break ;
				
			case 1: //ECHANGE DES GRAPHES		
				// on passe directement à l'étape 3 de ExchangeMap car on communique avec l'agent avec qui on est en interblocage
				((CleverAgent)super.myAgent).setCommunicationState(3);
				ArrayList <AID> list = new ArrayList<AID>(); list.add(agent);
				((CleverAgent)super.myAgent).setAgentsNearby(list);
				((CleverAgent)super.myAgent).setInterblocageState(state+1);
				exit_value=1;		
				break;
				
			case 2 : //VOIR SI L'INTERBLOCAGE EST REGLE
				//regarder les maps voir si le noeud destination n'est plus interessant (SANS TRESOR)
				List<Node> chemin = ((CleverAgent)super.myAgent).getChemin();
				Node dest =chemin.get(chemin.size()-1);
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setSender(super.myAgent.getAID()); msg.addReceiver(agent);
				
				if (((CleverAgent)super.myAgent).getGraph().getNode(dest.getId()).getAttribute("state").equals("closed")){
					//a ce stade, un des deux agents peut etre débloqué
					// si un des deux est débloqué ça va peut etre débloquer l'autre apres le deplacement du premier 
					// donc des que un est débloqué -> débloquer l'autre
					// if je suis débloqué : 
					// 		- envoie un message "good"					
					msg.setContent("good"); 	
				}
					// else je ne suis pas débloqué :
					// 		- envoie un message "bad"
				else {
					msg.setContent("bad"); 
				}
				
				super.myAgent.send(msg);
				// attendre réception message
				ACLMessage response ;
				do {
					response = super.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				} while(response == null || (response!=null && response.getSender() !=agent)) ;
				//if bad et moi aussi : state 3  else : finish
				if(response.getContent().equals("bad") && msg.getContent().equals("bad"))
					((CleverAgent)super.myAgent).setInterblocageState(state+1);
				else 
					((CleverAgent)super.myAgent).setInterblocage(false);
				
				break ;
				
			case 3 : // ECHANGE DES DISTANCES AU CARREFOUR LE PLUS PROCHE (2 messages)
				//les deux agents sont présents et n'ont pas réglé leur interblocage
				// calcul de la distance au carrefour le plus proche ( carrefour[0] = distance ; carrefour[1] : le chemin )
				List<Node> carrefour = calculDistanceCarrefour() ;
				// dans l'ordre alphabétique le premier agent est celui qui devra envoyer le message avec sa distance ...
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
					super.myAgent.send(message);
					//...puis attendre la réponse de l'agent2
					message = null ;
					do {
						message = super.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF));
					} while(message ==null);
					
					if(message.getContent().equals("not you")){
						((CleverAgent)super.myAgent).setInterblocageState(5);
					}else{
						try {
							otherAgentPath =( (Data<String,List<String>,Integer,Integer>) message.getContentObject()).getSecond() ;
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
						cheminCarrefour = carrefour;
						((CleverAgent)super.myAgent).setInterblocageState(4);
					}
						
				} 
				
				else {
					// agent2 attend la distance de l'autre ...
					ACLMessage distanceMsg ;
					do{
						distanceMsg = super.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF));
					} while(distanceMsg ==null);
					
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
						super.myAgent.send(decideWhoMoves);
						((CleverAgent)super.myAgent).setInterblocageState(5);
					} else {
						decideWhoMoves.setContent("not you");
						super.myAgent.send(decideWhoMoves);
						try {
							otherAgentPath =( (Data<Integer,List<String>,Integer,Integer>) decideWhoMoves.getContentObject()).getSecond() ;
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
						cheminCarrefour = carrefour;
						((CleverAgent)super.myAgent).setInterblocageState(4);
					}				
				}
				
				break ;
				
			case 4 : //L'AGENT DOIT BOUGER
				// il faut ajouter un noeud voisin au noeud carrefour pour le déplacement
				// le noeud ne doit pas etre sur le chemin de l'autre agent
				String idCarrefour = (cheminCarrefour.isEmpty())? ((mas.abstractAgent)super.myAgent).getCurrentPosition() : cheminCarrefour.get(cheminCarrefour.size()-1).getId() ; 
				
				Iterator<Node> carrefourIterator = ((CleverAgent)super.myAgent).getGraph().getNode(idCarrefour).getNeighborNodeIterator();
				while (carrefourIterator.hasNext()){
					Node neighbour = carrefourIterator.next();
					if(!otherAgentPath.contains(neighbour.getId())){
						cheminCarrefour.add(neighbour);
						break;
					}
				}
				
				((CleverAgent)super.myAgent).setChemin(cheminCarrefour);
				//signaler à l'autre qu'il peut bouger ?
				ACLMessage youCanMoveMsg = new ACLMessage(ACLMessage.CONFIRM);
				youCanMoveMsg.setSender(super.myAgent.getAID()); youCanMoveMsg.addReceiver(agent);
				youCanMoveMsg.setContent("move");
				super.myAgent.send(youCanMoveMsg);
				((CleverAgent)super.myAgent).setInterblocageState(6);
				((CleverAgent)super.myAgent).setInterblocage(false);
				break ;
				
			case 5 : //L'AGENT ATTEND QUE L'AUTRE BOUGE
				ACLMessage moveAnswer = super.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
				if(moveAnswer!=null){
					//attend un peu avant de bouger
					block(1000);
					((CleverAgent)super.myAgent).setInterblocageState(6);
					((CleverAgent)super.myAgent).setInterblocage(false);
				}
				break;
				
			default :
				break;
		}
	}
	
	
	
	
	/**
	 * Calcule la distance entre l'agent et le carrefour (noeud à au moins 3 branches) le plus proche.
	 * @return un tableau contenant la distance du plus court chemin à un carrefour et ce chemin
	 */
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

	
	
	/**
	 * Convertit une liste de noeuds en liste d'identifiant de ces noeuds
	 * @param path : la liste de noeuds à convertir
	 * @return la liste des identifiants
	 */
	public List<String> convertNodeToId(List<Node> path){
		List<String> idChemin = new ArrayList<String>() ;
		for (Node n : path){
			idChemin.add(n.getId());
		}
		
		return idChemin;
	}
	
	
	
	@Override
	public int onEnd() {
		cptWait = 0;
		return exit_value;
	}
	
	@Override
	public boolean done() {
		return (!((CleverAgent)super.myAgent).isInterblocage()) || exit_value==1;
	}

}
