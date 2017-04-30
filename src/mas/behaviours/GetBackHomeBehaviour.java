package mas.behaviours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import jade.lang.acl.UnreadableException;
import mas.abstractAgent;
import mas.agents.CleverAgent;

public class GetBackHomeBehaviour extends SimpleBehaviour{


	/**
	 * the agent go back to its initial position and registers its new service (collect) with the DF
	 */
	private static final long serialVersionUID = 2139427080509307681L;
	private int state = 0;
	private int exitValue = 0;
	private boolean moved = false;
	private List<Node> path;
	
	public GetBackHomeBehaviour(mas.abstractAgent myagent) {
		super(myagent);
	}
	
	public void action() {

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
				((CleverAgent) super.myAgent).setChemin(path);
				((CleverAgent) super.myAgent).setComingbackState(state+1);
				break;
				
			case 1:
				//si le prochain noeud du chemin a ete atteint
				if(!path.isEmpty() && myPos.equals(path.get(0).getId())){
					path.remove(0);
					if(!path.isEmpty()){
						System.out.println(myAgent.getLocalName().toString()+" va en "+path.get(0).getId());
						((mas.abstractAgent)this.myAgent).moveTo(path.get(0).getId());
					}
						
				}
				//si le noeud initial a ete atteint
				else if(path.isEmpty()){
					//si c'est la premiere fois qu'on est dans ce behaviour
					if(((CleverAgent)myAgent).getType().equals(""))
						((CleverAgent) super.myAgent).setComingbackState(state+1);
					else //sinon
						((CleverAgent) super.myAgent).setComingbackState(5);
				}	
				else{
					//d'abord on cherche un autre chemin 
					System.out.println("je cherche un nouveau chemin pour rentrer chez moi");
					Graph tmpGraph = ((CleverAgent)this.myAgent).getGraph();
					String destination = ((CleverAgent)this.myAgent).getFirstPosition();
					List<Node> newpath = new ArrayList<Node>();
					if(!destination.equals(path.get(0).getId())){
						tmpGraph.removeNode(path.get(0));			
						Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.NODE, null, null);
						dijkstra.init(tmpGraph);
						dijkstra.setSource(tmpGraph.getNode(myPos));
						dijkstra.compute();

						newpath = dijkstra.getPath(tmpGraph.getNode(destination)).getNodePath();
						if(!newpath.isEmpty()){
							path = newpath;
							((CleverAgent) super.myAgent).setChemin(path);
						}
							
					}
					//interblocage
					if(newpath.isEmpty() || destination.equals(path.get(0).getId())){
						System.out.println("interblocage");
						((CleverAgent)this.myAgent).setInterblocage(true);	
						((CleverAgent)this.myAgent).setInterblocageState(0);
						String myPosition = ((abstractAgent)this.myAgent).getCurrentPosition();
						System.out.println("INTERBLOCAGE pour agent "+myAgent.getName());
						final ACLMessage mess = new ACLMessage(ACLMessage.FAILURE);
						mess.setSender(this.myAgent.getAID()); mess.setContent(path.get(0).getId()+"_"+myPosition); //le noeud qui nous bloque_oï¿½ on est
						
						Set<AID> cles = ((CleverAgent)this.myAgent).getAgentList().keySet();		
						for (AID aid : cles){
							mess.addReceiver(aid);
						}
						((mas.abstractAgent)this.myAgent).sendMessage(mess);
						((CleverAgent)this.myAgent).setAgentsNearby(new ArrayList<AID>());
						exitValue = 1;
					}

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
				sd.setName(super.myAgent.getLocalName());
				capacity.setType( "capacity:"+((CleverAgent)myAgent).getBackPackFreeSpace() ); 
				capacity.setName(super.myAgent.getLocalName());
				pos_init.setType("home:"+((CleverAgent)myAgent).getFirstPosition()); 
				pos_init.setName(super.myAgent.getLocalName() );
				dfd.addServices(sd);
				dfd.addServices(capacity);
				dfd.addServices(pos_init);
				System.out.println(myAgent.getLocalName()+"enregistre pos: "+((CleverAgent)myAgent).getFirstPosition() +" et cap: "+((CleverAgent)myAgent).getBackPackFreeSpace());
				try {
					DFService.register(super.myAgent, dfd );
				} catch (FIPAException fe) { fe.printStackTrace(); }
				
				((CleverAgent) super.myAgent).setComingbackState(state+1);
				break;
			
			case 3: //on attend la fin de l'exploration des autres
				final ACLMessage block = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));		
				if(block!=null){
					//si il recoit un message d'interblocage de quelqu'un qui explore
					moved = true;
					//exitValue = 1;
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
						boolean MaJ = true;
						//s'assurer que les infos sur les agents sont completes
						HashMap<AID, ArrayList<String>> agentList = ((CleverAgent)this.myAgent).getAgentList();
						for(AID key : agentList.keySet()){
							ArrayList<String> infos = agentList.get(key);
							if(infos.isEmpty() || infos.get(0).equals("")){
								//s'il manque des infos, consulter DF
								DFAgentDescription template = new DFAgentDescription();
								ServiceDescription sv = new ServiceDescription();
								sv.setType("collect");
								template.addServices(sv);
								try{
									result = DFService.search(super.myAgent, template);
									for(int i=0; i<result.length; i++){
										DFAgentDescription df = result[i];
										AID provider = df.getName();
										if(provider.equals(key)){
											Iterator it = df.getAllServices();
											while(it.hasNext()){
												ServiceDescription s =(ServiceDescription) it.next();
												if(s.getType().contains("home")){
													String posinit = s.getType();
													String[] tokens = posinit.split("[:]");
													infos.set(0, tokens[1]);													
												}
												if(s.getType().contains("capacity")){
													String cap = s.getType();
													String[] tokens = cap.split("[:]");
													infos.set(1, tokens[1]);												
												}
											}
										}
										
									}
									
									//si toujours pas trouve
									if(infos.get(0).equals(""))
										MaJ = false;
								} catch (Exception e) {
									e.printStackTrace();
								}
								agentList.replace(key, infos);
							}
						}
						((CleverAgent)myAgent).setAgentList(agentList);
						if(MaJ){
							((CleverAgent) super.myAgent).setComingbackState(state+1);
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
				// chercher parmis les autres agents la plus grande capacite
				int capacite;
				int cap_max = 0;
				HashMap<AID, ArrayList<String>> agentList = ((CleverAgent)this.myAgent).getAgentList();
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
						((CleverAgent) super.myAgent).setComingbackState(6);
						((CleverAgent) super.myAgent).setPickingState(0);
						exitValue = 2;
					} 
				}
				else{
					//l'agent a la meilleure capacite
					System.out.println(myAgent.getLocalName().toString()+" i'm the best");
					((CleverAgent) super.myAgent).setComingbackState(6);
					((CleverAgent) super.myAgent).setPickingState(0);
					exitValue = 2;
				}
				break;
				
				
			case 5:
				//attente: on regarde la boite aux lettres (messages d'interblocages et de missions) et les pages jaunes
				
				final ACLMessage task = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));				
				final ACLMessage blockMsg = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.FAILURE));		
				
				if(task!=null){
					// un agent nous contacte pour nous donner une tache a effectuer
					//dois nous envoyer agentList a jour et liste des tresors
					System.out.println(myAgent.getLocalName().toString()+" a reçu un message de "+task.getSender().getLocalName().toString());
					try {
						HashMap<AID, ArrayList<String>> newAgentList = (HashMap<AID, ArrayList<String>>) task.getContentObject();
						
						((CleverAgent)myAgent).setAgentList(newAgentList);
						
						ACLMessage info = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CFP));
						while(info==null){
							 info = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CFP));
						}
						System.out.println(myAgent.getLocalName().toString()+" a reçu le trésor a chercher");
						((CleverAgent)myAgent).setTreasureToFind(info.getContent());
						
						((CleverAgent)myAgent).setPickingState(0);
						((CleverAgent)myAgent).setComingbackState(6);
						exitValue = 2;
						
					} catch (UnreadableException e) {e.printStackTrace();}
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
		return state == 6 || exitValue == 1;
	}

}
