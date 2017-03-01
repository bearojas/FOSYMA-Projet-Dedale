package mas.behaviours;

import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;

import java.util.ArrayList;
import java.util.List;

import mas.agents.CleverAgent;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class MainBehaviour extends FSMBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public MainBehaviour(final mas.abstractAgent myagent) {
		super(myagent);
		registerFirstState(new ExploreBehaviour(myagent,((CleverAgent) myagent).getGraph(), ((CleverAgent) myagent).getChemin(), ((CleverAgent) myagent).getOpened()), "EXPLORE");
		registerState(new ExchangeMapBehaviour(myagent, ((CleverAgent) myagent).getGraph(), ((CleverAgent) myagent).getAgentList()), "EXCHANGEMAP");
		//registerState(new , "");
		
		registerDefaultTransition("EXPLORE", "EXCHANGEMAP");
		registerDefaultTransition("EXCHANGEMAP", "EXPLORE");
	}
	

}
