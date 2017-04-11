package mas.behaviours;

import java.util.ArrayList;
import java.util.List;

import org.graphstream.graph.Node;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import mas.agents.CleverAgent;

public class InterblocageBehaviour extends SimpleBehaviour{

	private static final long serialVersionUID = 1L;
	private final int waitingTime = 5;
	private int cptWait = 0;
	private AID agent ; 
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
		
		int state = ((CleverAgent)super.myAgent).getInterblocageState();
		
		switch(state) {
		
			case 0 :
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
				
			case 1:
				//ECHANGE DES GRAPHES
				// on passe directement à l'étape 3 de ExchangeMap car on communique avec l'agent avec qui on est en interblocage
				((CleverAgent)super.myAgent).setCommunicationState(3);
				ArrayList <AID> list = new ArrayList<AID>(); list.add(agent);
				((CleverAgent)super.myAgent).setAgentsNearby(list);
				((CleverAgent)super.myAgent).setInterblocageState(state+1);
				exit_value=1;		
				break;
				
			case 2 :
				//regarder les maps voir si le noeud destination n'est plus interessant
				List<Node> chemin = ((CleverAgent)super.myAgent).getChemin();
				Node dest =chemin.get(chemin.size());
				if (((CleverAgent)super.myAgent).getGraph().getNode(dest.getId()).getAttribute("state").equals("closed")){
					((CleverAgent)super.myAgent).setInterblocage(false);
				}else 
					((CleverAgent)super.myAgent).setInterblocageState(state+1);
				break ;
				
			case 3 :
				//a ce stade, un des deux agents peut etre débloqué
				break ;
		}
	}

	@Override
	public int onEnd() {
		return exit_value;
	}
	
	@Override
	public boolean done() {
		return (!((CleverAgent)super.myAgent).isInterblocage()) || exit_value==1;
	}

}
