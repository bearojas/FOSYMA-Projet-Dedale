package mas.behaviours;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import mas.agents.CleverAgent;
import mas.agents.Data;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class InterblocageBehaviour extends SimpleBehaviour{

	private static final long serialVersionUID = 1L;
	private final int waitingTime = 7;
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
	
	public void setAgent(AID agent) {
		this.agent = agent;
	}
	//TODO
	/*
	 * SANS PRISE EN COMPTE DES TRESORS
	 * state 0 : attente d'un message d'interblocage puis passe en 1
	 * state 1 : echange des map ï¿½ tous les agents en interblocage 
	 * state 2 : -> si regle le conflit, interblocage = false pour les deux
	 * 			 -> sinon on passe en state 3
	 * state intermediaire : dï¿½finition des prioritï¿½s ?
	 * state 3 : chacun regarde la plus courte distance ï¿½ un carrefour
	 * 			 les agents envoient leur distance ï¿½ un Agent coordinateur
	 * 			 le coordinateur dï¿½cide qui bouge 
	 * 			 si doit bouger -> state 4
	 * 			 sinon state 5
	 * state 4 : on doit bouger : setChemin avec le chemin qu'on a trouvï¿½ jusqu'au carrefour
	 * 			 interblocage = false et on change le interblocage de l'autre agent aussi
	 * 			 on revient dans Explore
	 * state 5 : attente jusqu'au changement de interblocage par l'autre et on revient dans Explore
	 */
	@Override
	public void action() {
		
		exit_value = 0 ;
		int state = ((CleverAgent)super.myAgent).getInterblocageState();
		
		switch(state) {
		
			case 0 : 
				//si on a des vieux maps on les efface
				if(cptWait==0){
					ACLMessage mapMessage;
					do{
						mapMessage = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
					}while(mapMessage!=null);
				}
				
				// ATTENTE DU MESSAGE DE L'AGENT EN INTERBLOCAGE AVEC NOUS
				System.out.println(super.myAgent.getLocalName()+": Je suis en interblocage");
				
				//attendre le message de l'agent avec qui on est en interblocage
				final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);			
				ACLMessage answer = this.myAgent.receive(msgTemplate);
				
				// le message reï¿½u contiendra : notre position_la position de l'autre
				if(answer != null && answer.getContent().equals(((mas.abstractAgent)this.myAgent).getCurrentPosition()+"_"+((CleverAgent)this.myAgent).getChemin().get(0))){
					agent = answer.getSender(); 
					cptWait=0;
					((CleverAgent)this.myAgent).setInterblocageState(state+1);
				} else {
					answer = null;
					block(1500);
					cptWait++;
				}
				//si temps d'attente trop long on revient ï¿½ Explore
				if(cptWait==waitingTime && answer==null){
					System.out.println("Temps d'attente trop long pour "+this.myAgent.getLocalName()+" qui quitte l'interblocage");
					((CleverAgent)this.myAgent).setInterblocage(false);
				}
				break ;
				
			case 1: //ECHANGE DES GRAPHES
				
				//TODO: si on a recu un message d'interblocage dans Explore -> initialiser agent
				if(!((CleverAgent)this.myAgent).getAgentsNearby().isEmpty()){
					agent = ((CleverAgent)this.myAgent).getAgentsNearby().get(0);
				}
				
				// on passe directement ï¿½ l'ï¿½tape 3 de ExchangeMap car on communique avec l'agent avec qui on est en interblocage
				System.out.println(this.myAgent.getLocalName()+" en interblocage avec "+agent.getLocalName());
				((CleverAgent)this.myAgent).setCommunicationState(3);
				ArrayList <AID> list = new ArrayList<AID>(); list.add(agent);
				((CleverAgent)this.myAgent).setAgentsNearby(list);
				((CleverAgent)this.myAgent).setInterblocageState(state+1);
				exit_value=1;		
				break;
				
			case 2 : //VOIR SI L'INTERBLOCAGE EST REGLE
				//regarder les maps voir si le noeud destination n'est plus interessant (SANS TRESOR)
				List<Node> chemin = ((CleverAgent)this.myAgent).getChemin();
				Node dest =chemin.get(chemin.size()-1);
				ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
				msg.setSender(this.myAgent.getAID()); msg.addReceiver(agent);
				
				
				if (((CleverAgent)this.myAgent).getGraph().getNode(dest.getId()).getAttribute("state").equals("closed")){
					//a ce stade, un des deux agents peut etre dï¿½bloquï¿½
					// si un des deux est dï¿½bloquï¿½ ï¿½a va peut etre dï¿½bloquer l'autre apres le deplacement du premier 
					// donc des que un est dï¿½bloquï¿½ -> dï¿½bloquer l'autre
					// if je suis dï¿½bloquï¿½ : 
					// 		- envoie un message "good"					
					msg.setContent("good"); 	
				}
					// else je ne suis pas dï¿½bloquï¿½ :
					// 		- envoie un message "bad"
				else {
					msg.setContent("bad"); 
				}
				
				((mas.abstractAgent)this.myAgent).sendMessage(msg);
				System.out.println(super.myAgent.getLocalName()+" envoie un message "+msg.getContent()+" a "+agent.getLocalName());
				// attendre rï¿½ception message
				ACLMessage response = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
				
				//TODO: rajouter timeout  
				// timeout pose pb si deux agents arrivent dans ce case, un envoie bad, attends puis quitte
				// l'autre envoie bad, a reçu bad et part tout seul dans les next case
				while( (response == null || !(response.getSender().equals(agent))&& cptWait<=3*waitingTime)){
					response = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
					cptWait++;
					block(1000);
					if(response!=null)
						System.out.println("case 2 interblocage: "+super.myAgent.getLocalName()+" a recu "+response.getSender().getLocalName());
				}

				//if bad et moi aussi : state 3  else : finish
				if( response != null && response.getContent().equals("bad") && msg.getContent().equals("bad")){
					cptWait=0;
					((CleverAgent)this.myAgent).setInterblocageState(state+1);
					System.out.println("passage au case 3: ECHANGE DE DISTANCES AU CARREFOUR");
				}
					
				else{ 
					System.out.println("FIN de l'INTERBLOCAGE");
					((CleverAgent)this.myAgent).setInterblocage(false);
					((CleverAgent)this.myAgent).setInterblocageState(0);
					((CleverAgent)this.myAgent).setChemin(new ArrayList<Node>());
				}
				
				break ;
				
			case 3 : // ECHANGE DES DISTANCES AU CARREFOUR LE PLUS PROCHE (2 messages)
				//les deux agents sont prï¿½sents et n'ont pas rï¿½glï¿½ leur interblocage
				// calcul de la distance au carrefour le plus proche ( carrefour[0] = distance ; carrefour[1] : le chemin )
				List<Node> carrefour = calculDistanceCarrefour() ;
				// dans l'ordre alphabï¿½tique le premier agent est celui qui devra envoyer le message avec sa distance ...
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
					//...puis attendre la rï¿½ponse de l'agent2
					message = null ;
					do {
						message = super.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF));
						cptWait++;
						block(1000);
					} while(message ==null&& cptWait<=3*waitingTime);
					
					cptWait=0;
					if(message != null && message.getContent().equals("not you")){
						((CleverAgent)super.myAgent).setInterblocageState(5);
					}else{
						if(message==null){
							System.out.println(myAgent.getLocalName().toString()+" n'a pas reçu le chemin de l'autre !");
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
					} while(distanceMsg ==null && cptWait<=3*waitingTime);
					cptWait=0;
					if(distanceMsg==null){
						System.out.println(myAgent.getLocalName().toString()+" n'a pas reçu qui doit bouger!");
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
				
				break ;
				
			case 4 : //L'AGENT DOIT BOUGER
				// il faut ajouter un noeud voisin au noeud carrefour pour le dï¿½placement
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
				//signaler ï¿½ l'autre qu'il peut bouger ?
				ACLMessage youCanMoveMsg = new ACLMessage(ACLMessage.CONFIRM);
				youCanMoveMsg.setSender(super.myAgent.getAID()); youCanMoveMsg.addReceiver(agent);
				youCanMoveMsg.setContent("move");
				((mas.abstractAgent)this.myAgent).sendMessage(youCanMoveMsg);
				System.out.println(myAgent.getLocalName().toString()+" dit que "+agent.getLocalName().toString()+" peut bouger");
				((CleverAgent)super.myAgent).setInterblocageState(6);
				((CleverAgent)super.myAgent).setInterblocage(false);
				break ;
				
			case 5 : //L'AGENT ATTEND QUE L'AUTRE BOUGE
				ACLMessage moveAnswer = super.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
				if(moveAnswer!=null){
					//attend un peu avant de bouger
					block(1000);
					System.out.println(myAgent.getLocalName().toString()+" peut bouger !");
					((CleverAgent)super.myAgent).setInterblocageState(0);
					((CleverAgent)super.myAgent).setInterblocage(false);
				}
				break;
				
			default :
				break;
		}
	}
	
	
	
	
	/**
	 * Calcule la distance entre l'agent et le carrefour (noeud ï¿½ au moins 3 branches) le plus proche.
	 * @return un tableau contenant la distance du plus court chemin ï¿½ un carrefour et ce chemin
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
	 * @param path : la liste de noeuds ï¿½ convertir
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
