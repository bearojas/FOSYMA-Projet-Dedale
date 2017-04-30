package mas.behaviours;

import jade.core.behaviours.OneShotBehaviour;
import mas.abstractAgent;
import mas.agents.CleverAgent;

public class EndBehaviour extends OneShotBehaviour{

	/**
	 * fin de la phase d'exploration -> debut de la phase de collecte
	 */
	private static final long serialVersionUID = -4963831358497269726L;

	@Override
	public void action() {
		this.myAgent.addBehaviour(new CollectBehaviour((abstractAgent) this.myAgent));		
	}

}
