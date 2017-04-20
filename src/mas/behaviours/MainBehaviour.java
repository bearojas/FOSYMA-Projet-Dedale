package mas.behaviours;

import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;

import java.util.ArrayList;
import java.util.List;

import mas.agents.CleverAgent;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class MainBehaviour extends FSMBehaviour{

	private static final long serialVersionUID = 1L;
	
	public MainBehaviour(final mas.abstractAgent myagent) {
		super(myagent);
		
		//STATE
		registerFirstState(new ExploreBehaviour(myagent), "EXPLORE");
		registerState(new ExchangeMapBehaviour(myagent), "EXCHANGEMAP");
		registerState(new InterblocageBehaviour(myagent), "INTERBLOCAGE");
		
		//TRANSITION
		registerDefaultTransition("EXPLORE", "EXPLORE", new String[]{"EXPLORE"});

		registerTransition("EXPLORE", "EXCHANGEMAP", 2, new String[]{"EXCHANGEMAP"});
		registerTransition("EXPLORE", "INTERBLOCAGE", 3, new String[]{"INTERBLOCAGE"});
		
		registerDefaultTransition("EXCHANGEMAP", "EXPLORE", new String[]{"EXPLORE"});
		registerTransition("EXCHANGEMAP", "INTERBLOCAGE", 1, new String[]{"INTERBLOCAGE"});
		
		registerDefaultTransition("INTERBLOCAGE", "EXPLORE", new String[]{"EXPLORE"});
		registerTransition("INTERBLOCAGE", "EXCHANGEMAP", 1, new String[]{"EXCHANGEMAP"});


		
	}
	

}
