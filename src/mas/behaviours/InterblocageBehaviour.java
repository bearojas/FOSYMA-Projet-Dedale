package mas.behaviours;

import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import mas.agents.CleverAgent;

public class InterblocageBehaviour extends SimpleBehaviour{

	private static final long serialVersionUID = 1L;
	private final int waitingTime = 5;
	private int cptWait = 0;
	
	public InterblocageBehaviour(final mas.abstractAgent myagent) {
		super(myagent);
	}
	//TODO
	// faire des states comme dans ExchangeMap?
	@Override
	public void action() {
		//attendre le message de la personne qui nous bloque
		final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);			
		ACLMessage answer = this.myAgent.receive(msgTemplate);
		while(cptWait<waitingTime && answer==null){
			block(1500);
			cptWait++;
			answer = this.myAgent.receive(msgTemplate);
		}
		//si temps d'attente trop long on revient à Explore
		if(cptWait==waitingTime){
			((CleverAgent)super.myAgent).setInterblocage(false);
		} else {
			//TODO
			// on echange les map
			
		}
	}

	@Override
	public boolean done() {
		return !((CleverAgent)super.myAgent).isInterblocage();
	}

}
