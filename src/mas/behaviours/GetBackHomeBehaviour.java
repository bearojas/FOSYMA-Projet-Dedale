package mas.behaviours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

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

public class GetBackHomeBehaviour extends SimpleBehaviour{


	/**
	 * the agent go back to its initial position and registers its new service (collect) with the DF
	 */
	private static final long serialVersionUID = 2139427080509307681L;
	private int state = 0;
	private int exitValue = 0;
	private List<Node> path;
	
	public GetBackHomeBehaviour(mas.abstractAgent myagent) {
		super(myagent);
	}
	
	public void action() {
		// TODO 
		//traiter interblocages 
		//attente: reste immobile et regarder boite aux lettres et regarder pages jaunes 
		// traiter le cas de l'agent avec plus grande capacite une fois que tout le monde soit en "collect"
		
		String myPos = ((mas.abstractAgent)this.myAgent).getCurrentPosition();
		state = ((CleverAgent) super.myAgent).getComingbackState();
		
		switch(state){
			//chercher un chemin au noeud initial
			case 0:
				Graph myGraph = ((CleverAgent)this.myAgent).getGraph();
				String dest = ((CleverAgent)this.myAgent).getFirstPosition();
				
				Dijkstra dijk = new Dijkstra(Dijkstra.Element.NODE, null, null);
				dijk.init(myGraph);
				dijk.setSource(myGraph.getNode(myPos));
				dijk.compute();

				path = dijk.getPath(myGraph.getNode(dest)).getNodePath();				
				((CleverAgent) super.myAgent).setComingbackState(state+1);
				break;
				
			case 1:
				//si le prochain noeud du chemin a ete atteint
				if(!path.isEmpty() && myPos.equals(path.get(0).getId())){
					path.remove(0);
					((mas.abstractAgent)this.myAgent).moveTo(path.get(0).getId());
				}
				//si le noeud initial a ete atteint
				else if(path.isEmpty()){
					((CleverAgent) super.myAgent).setComingbackState(state+1);
				}	
				else{
					//TODO: cas interblocage
				}
				break;
				
			case 2:
				//changement du service a "collect" dans les pages jaunes
				try {
					DFService.deregister(super.myAgent);
				} catch (FIPAException e) {
					e.printStackTrace();
				}				
				DFAgentDescription dfd = new DFAgentDescription();
				dfd.setName(super.myAgent.getAID()); 
				ServiceDescription sd  = new ServiceDescription();
				sd.setType( "collect" ); 
				sd.setName(super.myAgent.getLocalName() );
				dfd.addServices(sd);
				try {
					DFService.register(super.myAgent, dfd );
				} catch (FIPAException fe) { fe.printStackTrace(); }
				
				((CleverAgent) super.myAgent).setComingbackState(state+1);
				break;
			
			case 3: //on attend la fin de l'exploration des autres
				final ACLMessage block = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.FAILURE));		
				if(block!=null){
					exitValue = 1;
					//TODO: deadlock
				}		
				//interroger les pages jaunes
				DFAgentDescription descrip = new DFAgentDescription();
				ServiceDescription service  = new ServiceDescription();
				service.setType( "explorer" ); 
				descrip.addServices(service);
				try {
					DFAgentDescription[] result = DFService.search(super.myAgent, descrip);
					//si tout le monde a finit l'exploration
					if(result.length == 0){
						int capacite;
						int cap_max = 0;
						HashMap<AID, ArrayList<String>> agentList = ((CleverAgent)this.myAgent).getAgentList();
						for(Entry<AID, ArrayList<String>> entry : agentList.entrySet()){
							capacite = Integer.parseInt(entry.getValue().get(1));
							if( cap_max < capacite){
								cap_max = capacite;
							}
						}
						if(((abstractAgent)this.myAgent).getBackPackFreeSpace() < cap_max){
							//l'agent n'a pas la capacite max
							((CleverAgent) super.myAgent).setComingbackState(state+1);								
						}
						else if(((abstractAgent)this.myAgent).getBackPackFreeSpace() == cap_max){
							//TODO: que faire en cas d'egalite? ordre alphabetique?
						}
						else{
							//l'agent a la meilleure capacite
							((CleverAgent) super.myAgent).setComingbackState(5);
							exitValue = 2;
						}
				
					}		
					else{
						block(3000);
					}
				} catch (FIPAException e) {
					e.printStackTrace();
				}
				break;
			case 4:
				//attente: on regarde la boite aux lettres (messages d'interblocages et de missions) et les pages jaunes
				final ACLMessage task = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));				
				final ACLMessage blockMsg = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.FAILURE));		
				
				if(task!=null){
					//TODO: un agent nous contacte pour nous donner une tache a effectuer
					//dois nous envoyer agentList a jour et liste des tresors 
				}
				
				if(blockMsg!=null){
					exitValue = 1;
					//TODO: deadlock
				}
				break;
					
				
			default:
				break;
		
		}		
		
	}

	public int onEnd(){
		return exitValue;
	}

	public boolean done() {
		return state == 5;
	}

}
