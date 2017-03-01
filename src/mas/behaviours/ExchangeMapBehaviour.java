package mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import env.Attribute;
import env.Couple;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ExchangeMapBehaviour extends SimpleBehaviour {
	
	/**
	 * An agent tries to contact other agents around it, giving its position
	 * If it gets an answer, the agent sends its current map to every agent who replied
	 * And it merges every map received with its own  
	 * 
	 */

	private static final long serialVersionUID = 9088209402507795289L;
	private int state = 0;
	private Graph myGraph ;
	private ArrayList<AID> agentList;
	private ArrayList <AID> receivers;
	private final int nbWaitAnswer = 5;
	private int cptWait = 0;
	
	public ExchangeMapBehaviour(final mas.abstractAgent myagent, Graph graph, ArrayList<AID> agentList){
		super(myagent);
		this.agentList = agentList;
		receivers = new ArrayList <AID>();
		myGraph = graph ;
		myGraph.setStrict(false);
	}
	
	// fonction de transformation d'un graphe vers une HashMap
	// cl� de la HashMap : identifiant du noeud
	// valeur pour chaque cl� : les voisins du noeud, l'�tat du noeud et les observations de ce noeud
	public HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>> graphToHashmap(Graph graphToSend){
		HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>> finalMap = new HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>>();
		//pour chaque noeud: cr�er la cl�, liste de voisins vide, et observation
		for (Node n : graphToSend){
			finalMap.put(n.getId(), new Couple(new ArrayList(), new Couple(n.getAttribute("state"),n.getAttribute("content"))));
		}
		//pour chaque arc, on r�cup�re le noeud source #e.getNode0()#,
		//dans la hashMap � cette cl� on r�cup�re la liste des voisins #getLeft()#
		//et on y ins�re le noeud destination #add(e.getNode1())#
		for(Edge e : graphToSend.getEachEdge()){
			finalMap.get(e.getNode0().getId()).getLeft().add(e.getNode1().getId());
		}
		return finalMap;
	}
	
	//fonction de transformation d'une HashMap vers un graphe
	public Graph hashmapToGraph(HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>> hMapReceived){
		Graph finalGraph = new SingleGraph("");
		finalGraph.setStrict(false);
		for (Entry<String, Couple<List<String>, Couple<String, List<Attribute>>>> entry : hMapReceived.entrySet()){
			//creation du noeud (si il existe deja retourne le noeud existant) 
			Node n = finalGraph.addNode(entry.getKey()) ;
			//ajout de l'attribut (modification si deja present)
			n.addAttribute("state", entry.getValue().getRight().getLeft());
			n.addAttribute("content", entry.getValue().getRight().getRight());
			
			// ajout des arcs, donc parcours des voisins 
			for (String neighborId : entry.getValue().getLeft()){
				// si l'arc existe deja, ne rien faire
				if ( finalGraph.getEdge(n.getId()+neighborId)== null && finalGraph.getEdge(neighborId+n.getId())==null){
					finalGraph.addNode(neighborId);
					finalGraph.addEdge(n.getId()+neighborId, n.getId(), neighborId);
				}
			}
		}
		return finalGraph ;
	}
	
	// fonction permettant de concatener 2 graphes, et donc de mettre � jour ses informations
	public void graphsFusion(Graph graphReceived){
		for (Node n : graphReceived){
			Node old_node = myGraph.getNode(n.getId());
			// si on ignorait l'existence de ce noeud, on l'ajoute � notre graphe ainsi que ses attributs
			if (old_node == null){
				Node new_node = myGraph.addNode(n.getId());
				new_node.addAttribute("state", n.getAttribute("state"));
				new_node.addAttribute("content", n.getAttribute("content"));
				
			} else { // si le noeud existait, on compare les attributs
				//si ce noeud a ete explore, on le marque closed (ne change rien s'il l'�tait deja)
				if (n.getAttribute("state").equals("closed"))
					old_node.setAttribute("state", "closed");

				List<Attribute> obs = n.getAttribute("content");
				List<Attribute> old_obs = old_node.getAttribute("content");
				
				// si il y avait un tr�sor, on garde la plus petite quantit� restante de ce tr�sor
				//on regarde les possibles attributs pour ce noeud dans les nouvelles observations 
				for(Attribute a : obs){
					if(a.getName().equals("Treasure")){
						int i = old_obs.indexOf("Treasure");
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
			
			// on parcourt tous les arcs du graphReceived
			for(Edge e : graphReceived.getEachEdge()){
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
		switch(state){
		case 0:
			//agent gives its current position
			String myPosition=((mas.abstractAgent)this.myAgent).getCurrentPosition();
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());

			if (myPosition!=""){
				System.out.println("Agent "+this.myAgent.getLocalName()+ " is trying to reach its friends");
				msg.setContent(myPosition);

				//on ajoute tous les agents � la liste des destinataires
				for(AID id: agentList){
					msg.addReceiver(id);
				}
				((mas.abstractAgent)this.myAgent).sendMessage(msg);
				
				state++;
			}			
			
		case 1:
			// regardes sa boite aux lettres et attends un message de réponse (timeout)
			
			final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);			
			final ACLMessage answer = this.myAgent.receive(msgTemplate);
			
			if (answer  != null) {
				System.out.println(this.myAgent.getLocalName()+"<----Result received from "+answer.getSender().getLocalName()+" ,content= "+answer.getContent());
				receivers.add((AID) answer.getSender());

			}else{
				// si limite de r�ponses attendues atteint 
				if(receivers.size() >= agentList.size()){
					state++;
				}
				else{
					if(cptWait >= nbWaitAnswer){
						state++;
						cptWait = 0;
					}
					else{
						block(1000);
						cptWait++;
					}
				}
					
					
			}
			
		case 2:
			//convertir le graph en map spécifique a chaque agent qui a répondu et l'envoyer
			for(AID c : receivers){
				ACLMessage mapMsg = new ACLMessage(ACLMessage.INFORM);
				mapMsg.setSender(this.myAgent.getAID());
				mapMsg.addReceiver(c);
				HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>> mapToSend = graphToHashmap(myGraph);
				
				try {
					mapMsg.setContentObject(mapToSend);
				} catch (IOException e) {
					System.out.println("could not create the message with mapToSend");
					e.printStackTrace();
				}
				((mas.abstractAgent)this.myAgent).sendMessage(mapMsg);
				
			}
			state++;
			
		case 3:
			// on attend les graph des autres
			// on convertit les maps reçues en graphe
			// et on fusionne chaque graph avec le notre
			final MessageTemplate msgTemp = MessageTemplate.MatchPerformative(ACLMessage.INFORM);			
			final ACLMessage mapReceived = this.myAgent.receive(msgTemp);
			
			if (mapReceived != null) {
				System.out.println(this.myAgent.getLocalName()+"<----Result received a map from "+mapReceived.getSender().getLocalName());
				try {
					HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>> hmap;
					hmap = (HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>>) mapReceived.getContentObject();
					Graph receivedGraph = hashmapToGraph(hmap);
					graphsFusion(receivedGraph);
					receivers.remove(mapReceived.getSender());
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				

			}else{
				// si limite de r�ponses attendues atteint 
				if( receivers.isEmpty()){
					state++;
				}
				else{
					if(cptWait >= nbWaitAnswer){
						state++;
					}
					else{
						block(1000);
						cptWait++;
					}
				}
			}
				
			
		}
		
	}

	@Override
	public boolean done() {
		return state == 4;
	}

}
