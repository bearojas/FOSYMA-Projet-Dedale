package mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.SingleSelectionModel;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import env.Attribute;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import mas.agents.CleverAgent;
import mas.agents.Data;

public class ExchangeMapBehaviour extends SimpleBehaviour {
	
	/**
	 * An agent tries to contact other agents around it, giving its position
	 * If it gets an answer, the agent sends its current map to every agent who replied
	 * And it merges every map received with its own  
	 * 
	 */

	private static final long serialVersionUID = 9088209402507795289L;
	private int state;
	private Graph myGraph ;
	private ArrayList <AID> receivers = new ArrayList <AID>();
	private final int nbWaitAnswer = 5;
	private int cptWait = 0;
	private ArrayList<ACLMessage> msgs = new ArrayList<ACLMessage>();
	
	public ExchangeMapBehaviour(final mas.abstractAgent myagent){
		super(myagent);
		myGraph = ((CleverAgent) myagent).getGraph();
		state = 0;
		myGraph.setStrict(false);
	}	
	
	public ExchangeMapBehaviour(final mas.abstractAgent myagent, int state){
		super(myagent);
		myGraph = ((CleverAgent) myagent).getGraph();
		this.state = state;
		myGraph.setStrict(false);
	}
	

	
	/**
	 *  fonction de transformation d'un graphe vers une HashMap
	 *  clï¿½ de la HashMap : identifiant du noeud
	 *  valeur pour chaque clï¿½ : les voisins du noeud, l'ï¿½tat du noeud et les observations de ce noeud	
	 * @param graphToSend :  graph to send to other agents
	 * @param recever : the recever of this message AID
	 * @return a hashmap representing the graph
	 */
	public HashMap<String,Data<List<String>,String, List<Attribute>, List<AID>>> graphToHashmap(Graph graphToSend, AID receiver){
		HashMap<String,Data<List<String>,String, List<Attribute>, List<AID>>> finalMap = new HashMap<String,Data<List<String>,String, List<Attribute>,List<AID>>>();
		//pour chaque noeud: creer la cle, liste de voisins vide, et observation et agents ayant deja visite ce noeud
		for (Node n : graphToSend){
			//si l'agent apparait dans la liste du noeud et que ce n'est pas un noeud trésor, on peut ne pas envoyer ce noeud
			ArrayList<AID> lVisitor = n.getAttribute("haveBeenThere")==null?new ArrayList<AID>():n.getAttribute("haveBeenThere");
			List<Attribute> lattribute = n.getAttribute("content")==null?new ArrayList<Attribute>(): n.getAttribute("content");

			if( lVisitor.indexOf(receiver) == -1 || ((List<Attribute>) lattribute).indexOf("TREASURE")!=-1){
				// avant d'envoyer on ajoute directement le nom  de l'agent qui va recevoir le noeud dans la liste des agents ayant deja visite le noeud
				lVisitor.add(receiver);
				finalMap.put(n.getId(), new Data(new ArrayList<String>(),(String)n.getAttribute("state"),lattribute,lVisitor));
				//Quand on a envoyé un noeud, il faut modifier le sien en ajoutant l'agent à qui on l'a envoyé pour s'en souvenir
				n.setAttribute("haveBeenThere", lVisitor);
			}
		}
		//pour chaque arc, on rï¿½cupï¿½re le noeud source #e.getNode0()#,
		//dans la hashMap ï¿½ cette clï¿½ on rï¿½cupï¿½re la liste des voisins #getLeft()#
		//et on y insï¿½re le noeud destination #add(e.getNode1())#
		for(Edge e : graphToSend.getEachEdge()){
			// si le noeud allait etre envoye, on ajoute ses voisins
			if(finalMap.containsKey(e.getNode0().getId()))
				finalMap.get(e.getNode0().getId()).getFirst().add(e.getNode1().getId());
		}
		return finalMap;
	}
	
	/**
	 * fonction de transformation d'une HashMap vers un graphe
	 * @param receivedHmap
	 * @return a graph created from the map
	 */
	public Graph hashmapToGraph(HashMap<String, Data<List<String>,String, List<Attribute>,List<AID>>> receivedHmap){
		Graph finalGraph = new SingleGraph("");
		finalGraph.setStrict(false);
		for (Entry<String, Data<List<String>,String, List<Attribute>,List<AID>>> entry : receivedHmap.entrySet()){
			//creation du noeud (si il existe deja retourne le noeud existant) 
			Node n = finalGraph.addNode(entry.getKey()) ;
			//ajout de l'attribut (modification si deja present)
			n.addAttribute("state", entry.getValue().getSecond());
			n.addAttribute("content", entry.getValue().getThird());
			n.addAttribute("haveBeenThere", entry.getValue().getLast());
			
			// ajout des arcs, donc parcours des voisins 
			for (String neighborId : entry.getValue().getFirst()){
				// si l'arc existe deja, ne rien faire
				if ( finalGraph.getEdge(n.getId()+neighborId)== null && finalGraph.getEdge(neighborId+n.getId())==null){
					finalGraph.addNode(neighborId);
					finalGraph.addEdge(n.getId()+neighborId, n.getId(), neighborId);
				}
			}
		}
		return finalGraph ;
	}
	
	/**
	 * fonction permettant de concatener 2 graphes, et donc de mettre ï¿½ jour ses informations 
	 * @param receivedGraph
	 */
	public void graphsFusion(Graph receivedGraph){
		for (Node n : receivedGraph){
			Node old_node = myGraph.getNode(n.getId());
			// si on ignorait l'existence de ce noeud, on l'ajoute ï¿½ notre graphe ainsi que ses attributs
			if (old_node == null){
				Node new_node = myGraph.addNode(n.getId());
				new_node.addAttribute("state", (String)n.getAttribute("state"));
				new_node.addAttribute("content", (n.getAttribute("content")==null)? new ArrayList<Attribute>(): n.getAttribute("content"));
				new_node.addAttribute("haveBeenThere",(n.getAttribute("haveBeenThere")==null)? new ArrayList<AID>(): n.getAttribute("haveBeenThere") );
				
			} else { // si le noeud existait, on compare les attributs
				//si ce noeud a ete explore, on le marque closed (ne change rien s'il l'ï¿½tait deja)
				if (n.getAttribute("state")==null || n.getAttribute("state").equals("closed"))
					old_node.setAttribute("state", "closed");
				
				List<AID>lOld_node = ( ((List<AID>)old_node.getAttribute("haveBeenThere"))==null)?new ArrayList<AID>():((List<AID>)old_node.getAttribute("haveBeenThere"));
				List<AID>lnew_node = ( ((List<AID>)n.getAttribute("haveBeenThere"))==null)?new ArrayList<AID>():((List<AID>)n.getAttribute("haveBeenThere"));

				for (AID a : lnew_node){
					if ( lOld_node.indexOf(a) == -1 )
						lOld_node.add(a);
				}
				old_node.setAttribute("haveBeenThere", lOld_node );
				
				List<Attribute> obs = n.getAttribute("content");
				List<Attribute> old_obs = old_node.getAttribute("content");
				
				// si il y avait un trï¿½sor, on garde la plus petite quantitï¿½ restante de ce trï¿½sor
				//on regarde les possibles attributs pour ce noeud dans les nouvelles observations 
				if(obs != null){
					for(Attribute a : obs){
						if(a.getName().equals("Treasure")){
							int i =-1;
							if (old_obs!= null)
								i = old_obs.indexOf("Treasure");
							if(i == -1){
								old_node.setAttribute("content", obs);

							}
							else{
								int oldTreasureValue = (int) old_obs.get(i).getValue();
								if((int)a.getValue() < oldTreasureValue ){
									old_obs.get(i).setValue(a.getValue());
								}
							}	
						}
							
					}
				}
			}
			
			// on parcourt tous les arcs du receivedGraph
			for(Edge e : receivedGraph.getEachEdge()){
				String id0 = e.getNode0().getId();
				String id1 = e.getNode1().getId();
				//si l'arc n'existe pas, on l'ajoute 
				if(myGraph.getEdge(id0+id1) == null && myGraph.getEdge(id1+id0) == null)
					myGraph.addEdge(id0+id1, id0, id1);
			}
		}
	}
	
	@Override
	public void action() {
		
		System.out.println("Agent "+this.myAgent.getLocalName()+" state: "+state);
		
		switch(state){
			case 0:
				//agent gives its current position	
				String myPosition=((mas.abstractAgent)this.myAgent).getCurrentPosition();
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				msg.setSender(this.myAgent.getAID());
				
				if (myPosition!=""){
					System.out.println("Agent "+this.myAgent.getLocalName()+ " is trying to reach its friends");
					msg.setContent(myPosition);
	
					//on ajoute tous les agents ï¿½ la liste des destinataires (evite les doublons)
					for(AID id: ((CleverAgent)super.myAgent).getAgentList()){
						if(receivers.indexOf(id)==-1)
							msg.addReceiver(id);				
					}
					((mas.abstractAgent)this.myAgent).sendMessage(msg);
				}
				state++;
				break ;
						
				//fusion de case 0 et case 1 
				// car avant n'envoyait qu'un message du coup si quelqu'un arrivait après l'envoi
				// il était jamais contacté mais l'autre oui et bloquait
					//OU ALORS : envoie un seul message
					// si on reoit un message on réenvoie un message request : ok
					// comme ça si quelqu'un arrive après et qu'on a reçu son message c'est bon
				// MAIS bloque quand meme :
				// Agt 1 envoie 1 message, puis 2 avant que Agt2 ne dise qu'il en a reçu un :
				// du coup agt 2 aura 2 message de ag1 à traiter : pas bon
					// PROBLEME
					//Explo3 is in Explore and has a new message in the mailbox 1_8
					//Agent Explo3 state: 0 pk passe pas en state 2 ?
					
			case 1 :
					// regarde sa boite aux lettres et attend un message de rÃ©ponse (timeout)	
					final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);			
					final ACLMessage answer = this.myAgent.receive(msgTemplate);
				
					if (answer  != null) {
						System.out.println(this.myAgent.getLocalName()+"<----Result received from "+answer.getSender().getLocalName()+" ,content= "+answer.getContent());
						if(receivers.indexOf(answer.getSender())==-1){
							receivers.add((AID) answer.getSender());
							ACLMessage okMsg = new ACLMessage(ACLMessage.REQUEST);
							okMsg.setContent("ok"); okMsg.setSender(this.myAgent.getAID());
							okMsg.addReceiver(answer.getSender());
							myAgent.send(okMsg);
						}
						System.out.println(receivers.toString());
					}
					// si limite de rï¿½ponses attendues atteint 
					if(receivers.size() >= ((CleverAgent)super.myAgent).getAgentList().size()-1){
						state=3;
						cptWait=0;
					}
					// si temps d'attente atteint
					else if(cptWait >= nbWaitAnswer){
						if(receivers.isEmpty()){
							//si personne n'a répondu dans les temps on envoie un message d'annulation
							final ACLMessage finalComm = new ACLMessage(ACLMessage.CANCEL);
							finalComm.setContent("end communication");
							finalComm.setSender(this.myAgent.getAID());
							for(AID aid: receivers){
								finalComm.addReceiver(aid);				
							}
							((mas.abstractAgent)this.myAgent).sendMessage(finalComm);
							System.out.println(myAgent.getLocalName()+" annule");
							cptWait=0;
							state = 5;
						}
						else{
							state = 3;
							cptWait = 0;
						}
					}
					else{
						System.out.println(this.myAgent.getLocalName()+" attend un signe");
						block(1500);
						cptWait++;
					}
				
				break;
				
			case 2:
				// Agent 1 essaye de communiquer 1...5 fois
				//à la 5eme Agent 2 passe dans les environs et intercepte le message
				//Agent 2 s'interrompt et passe en state 2
				// mais Agent1 est reparti et s'est trop éloigné -> ne reçoit pas le message 
				// Agent 2 passe en 3 et 4 et reste coincé
				//********************
				//SOLUTION ?
				// au dernier tour de communication, Agent peut envoyer un message "end"
				// dans le case 3 ou 4 si on reçoit un message "end" de notre contact
				// on supprime ce contact de la liste de recevers
				receivers = ((CleverAgent) this.myAgent).getAgentsNearby();
				((CleverAgent) this.myAgent).setAgentsNearby(new ArrayList<AID>());
				
				//agent gives its current position	
				String myPos=((mas.abstractAgent)this.myAgent).getCurrentPosition();
				ACLMessage msge = new ACLMessage(ACLMessage.REQUEST);
				msge.setSender(this.myAgent.getAID());
				
				if (myPos!=""){
					msge.setContent(myPos);
	
					//on ajoute les agents qui ont envoyÃ© de message 
					for(AID id: receivers){
						msge.addReceiver(id);				
					}
					System.out.println("Agent "+this.myAgent.getLocalName()+ " in state 2 is trying to reach "+receivers.toString());
					((mas.abstractAgent)this.myAgent).sendMessage(msge);
					
					ACLMessage cancelMsg = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
					while(cancelMsg !=null){
						System.out.println(myAgent.getLocalName()+" a recu un message d'annulation de "+cancelMsg.getSender().getLocalName());
						receivers.remove(cancelMsg.getSender());
						cancelMsg = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
					}
					
					state++;
				}
				break;
				
			case 3:
				//convertir le graph en map spÃ©cifique a chaque agent qui a rÃ©pondu et l'envoyer
				
				//si on a reçu message d'annulation
				ACLMessage cancelMsg = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
				while(cancelMsg !=null){
					System.out.println(myAgent.getLocalName()+" a recu un message d'annulation de "+cancelMsg.getSender().getLocalName());
					receivers.remove(cancelMsg.getSender());
					cancelMsg = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
				}
				
				for(AID c : receivers){
					ACLMessage mapMsg = new ACLMessage(ACLMessage.INFORM);
					mapMsg.setSender(this.myAgent.getAID());
					mapMsg.addReceiver(c);
					HashMap<String,Data<List<String>,String, List<Attribute>, List<AID>>> mapToSend = graphToHashmap(myGraph, c);
					
					System.out.println("Agent "+this.myAgent.getLocalName()+" sends a map to "+c.getLocalName());
					
					try {
						mapMsg.setContentObject(mapToSend);
					} catch (IOException e) {
						System.out.println("could not create the message with mapToSend");
						e.printStackTrace();
					}
					((mas.abstractAgent)this.myAgent).sendMessage(mapMsg);
					
				}
				state++;
				break;
				
			case 4:
				// on attend les graph des autres
				// on convertit les maps reÃ§ues en graphe
				// et on fusionne chaque graph avec le notre

				// attendre toutes les maps des agents contactés
				while(msgs.size()< receivers.size()){
					ACLMessage tmp = (myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
					if (tmp!=null)
						msgs.add(tmp);
//					System.out.println(myAgent.getLocalName()+" NB_MSG = "+msgs.size()+"  RECEVERS "+receivers.size()+"  "+receivers.toString());
					block(1000);
	
					//si on reçoit message d'annulation
					ACLMessage cancel = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CANCEL));

					while(cancel !=null){
						System.out.println(myAgent.getLocalName()+" a recu un message d'annulation de "+cancel.getSender().getLocalName());
						receivers.remove(cancel.getSender());
						cancel = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
					}
				}
				//pour chaque map reçue
				for(ACLMessage receivedMap : msgs){		
					
					if (receivedMap != null) {
						System.out.println(this.myAgent.getLocalName()+"<----Received a map from "+receivedMap.getSender().getLocalName());
						try {
							HashMap<String, Data<List<String>, String, List<Attribute>, List<AID>>> hmap;		
							hmap = ((HashMap<String, Data<List<String>, String, List<Attribute>, List<AID>>>) receivedMap.getContentObject());
							
							Graph receivedGraph = hashmapToGraph(hmap);
							graphsFusion(receivedGraph);
							receivers.remove(receivedMap.getSender());
							refreshAgent();
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
						
					}
//					}else{
//						// si limite de rï¿½ponses attendues atteint 
//						if( receivers.isEmpty()){
//							refreshAgent();
//							cptWait=0;
//							//state++;
//						}
//						else{
//							if(cptWait >= nbWaitAnswer){
//								refreshAgent();
////								state++;
//							}
//							else{
//								block(1000);
//								cptWait++;
//							}
//						}
//					}		
				}
				cptWait=0;
				msgs.clear();
				state++;
				break;
				
			default: 
				break;
		}		
	}
	
	public void refreshAgent(){
		((CleverAgent) super.myAgent).setGraph(myGraph);
	}

	@Override
	public boolean done() {
		if(state == 5){
			cptWait = 0;
			state = 0;
			return true;
		}
		return false;
	}

}
