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
	//To reset behaviours already visited
	private final String[] toReset = {"EXPLORE", "EXCHANGEMAP_1", "EXCHANGEMAP_N"};

	public MainBehaviour(final mas.abstractAgent myagent) {
		super(myagent);
		
		registerFirstState(new ExploreBehaviour(myagent), "EXPLORE");
		registerState(new ExchangeMapBehaviour(myagent, 2), "EXCHANGEMAP_1");
		registerState(new ExchangeMapBehaviour(myagent), "EXCHANGEMAP_N");

		
//		registerTransition("EXPLORE", "EXCHANGEMAP_N",0, toReset);
//		registerTransition("EXPLORE", "EXCHANGEMAP_1", 2, toReset);
//		registerDefaultTransition("EXCHANGEMAP_N", "EXPLORE",toReset);
//		registerDefaultTransition("EXCHANGEMAP_1", "EXPLORE", toReset);
		
		
		registerTransition("EXPLORE", "EXCHANGEMAP_N",0, new String[]{"EXCHANGEMAP_N"});
		registerTransition("EXPLORE", "EXCHANGEMAP_1", 2, new String[]{"EXCHANGEMAP_1"});
		registerDefaultTransition("EXCHANGEMAP_N", "EXPLORE",new String[]{"EXPLORE"});
		registerDefaultTransition("EXCHANGEMAP_1", "EXPLORE", new String[]{"EXPLORE"});
		
	}
	

}
