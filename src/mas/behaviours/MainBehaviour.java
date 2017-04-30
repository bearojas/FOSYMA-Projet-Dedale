package mas.behaviours;

import jade.core.behaviours.FSMBehaviour;


public class MainBehaviour extends FSMBehaviour{

	private static final long serialVersionUID = 1L;
	
	public MainBehaviour(final mas.abstractAgent myagent) {
		super(myagent);
		
		//STATES
		registerFirstState(new ExploreBehaviour(myagent), "EXPLORE");
		registerState(new ExchangeMapBehaviour(myagent), "EXCHANGEMAP");
		registerState(new InterblocageBehaviour(myagent), "INTERBLOCAGE");
		registerLastState(new EndBehaviour(), "END");
		
		//TRANSITIONS
		registerDefaultTransition("EXPLORE", "EXPLORE", new String[]{"EXPLORE"});

		registerTransition("EXPLORE", "EXCHANGEMAP", 2, new String[]{"EXCHANGEMAP"});
		registerTransition("EXPLORE", "INTERBLOCAGE", 3, new String[]{"INTERBLOCAGE"});
		registerTransition("EXPLORE", "END", 4);
		
		registerDefaultTransition("EXCHANGEMAP", "EXPLORE", new String[]{"EXPLORE"});
		registerTransition("EXCHANGEMAP", "INTERBLOCAGE", 1, new String[]{"INTERBLOCAGE"});
		
		registerDefaultTransition("INTERBLOCAGE", "EXPLORE", new String[]{"EXPLORE"});
		registerTransition("INTERBLOCAGE", "EXCHANGEMAP", 1, new String[]{"EXCHANGEMAP"});


		
	}
	

}
