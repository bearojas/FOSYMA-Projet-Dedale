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
				System.out.println(myAgent.getLocalName().toString()+" rentre chez lui");
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
					if(!path.isEmpty())
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
				// ajout de la capacité initiale et position 
				try {
					DFService.deregister(super.myAgent);
				} catch (FIPAException e) {
					e.printStackTrace();
				}				
				DFAgentDescription dfd = new DFAgentDescription();
				dfd.setName(super.myAgent.getAID()); 
				ServiceDescription sd  = new ServiceDescription();
				ServiceDescription capacity = new ServiceDescription();
				ServiceDescription pos_init = new ServiceDescription();
				sd.setType( "collect" ); 
				sd.setName(super.myAgent.getLocalName() );
				capacity.setType( "capacity" ); 
				capacity.setName(((CleverAgent)myAgent).getBackPackFreeSpace()+"" );
				pos_init.setType( "home" ); 
				pos_init.setName(((CleverAgent)myAgent).getFirstPosition() );
				dfd.addServices(sd);
				dfd.addServices(capacity);
				dfd.addServices(pos_init);
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
				//interroger les pages jaunes pour voir s'il reste des agents en Exploration
				DFAgentDescription descrip = new DFAgentDescription();
				ServiceDescription service  = new ServiceDescription();
				service.setType( "explorer" ); 
				descrip.addServices(service);
				try {
					DFAgentDescription[] result = DFService.search(super.myAgent, descrip);
					//si tout le monde a finit l'exploration
					if(result.length == 0){
						System.out.println("Tout le monde a fini !");
						//s'assurer que les infos sur les agents sont completes
						HashMap<AID, ArrayList<String>> agentList = ((CleverAgent)this.myAgent).getAgentList();
						for(AID key : agentList.keySet()){
							ArrayList<String> infos = agentList.get(key);
							if(infos.isEmpty() || infos.get(0).equals("")){
								//s'il manque des infos, consulter DF
								System.out.println("il manque des infos sur "+key.getLocalName().toString());
								descrip = new DFAgentDescription();
								descrip.setName(key);
								try{
									result = DFService.search(super.myAgent, descrip);
									while(result[0].getAllServices().hasNext()){
										ServiceDescription s =(ServiceDescription) result[0].getAllServices().next();
										if(s.getType().equals("home"))
											infos.set(0, s.getName());
										if(s.getType().equals("capacity"))
											infos.set(1, s.getName());
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
								agentList.replace(key, infos);
							}
						}
						((CleverAgent)myAgent).setAgentList(agentList);
						
						// chercher parmi les autres agents la plus grande capacite
						int capacite;
						int cap_max = 0;
						agentList = ((CleverAgent)this.myAgent).getAgentList();
						System.out.println("Pour "+myAgent.getLocalName().toString()+" "+agentList.toString());
						for(Entry<AID, ArrayList<String>> entry : agentList.entrySet()){
							capacite = Integer.parseInt(entry.getValue().get(1));
							if( cap_max < capacite){
								cap_max = capacite;
							}
						}
						System.out.println("La plus grande capacité chez les autres : "+cap_max);
						if(((abstractAgent)this.myAgent).getBackPackFreeSpace() < cap_max){
							System.out.println(myAgent.getLocalName().toString()+" I'm not the best");
							//l'agent n'a pas la capacite max
							((CleverAgent) super.myAgent).setComingbackState(state+1);								
						}
						else if(((abstractAgent)this.myAgent).getBackPackFreeSpace() == cap_max){
							//TODO: que faire en cas d'egalite? ordre alphabetique?
							//chercher les autres agents qui ont la meme capacite que moi
							boolean max= true;
							for (AID k : agentList.keySet()){
								ArrayList<String> other = agentList.get(k);
								if(Integer.parseInt(other.get(1)) == cap_max){
									//si un autre agent a aussi la capacite max,mais est avant moi dans ordre alphabetique je n'y vais pas
									if(myAgent.getLocalName().compareTo(k.getLocalName())< 0){
										max = false;
										((CleverAgent) super.myAgent).setComingbackState(state+1);
										break;
									}
										
								}
							}
							if(max == true){
								System.out.println(myAgent.getLocalName().toString()+" FIRST");
								//l'agent est premier dans ordre alphabetique
								((CleverAgent) super.myAgent).setComingbackState(5);
								exitValue = 2;
							} 
						}
						else{
							//l'agent a la meilleure capacite
							System.out.println(myAgent.getLocalName().toString()+" i'm the best");
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
