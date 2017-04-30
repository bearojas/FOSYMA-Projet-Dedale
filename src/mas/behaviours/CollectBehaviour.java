package mas.behaviours;

import jade.core.behaviours.FSMBehaviour;

public class CollectBehaviour extends FSMBehaviour{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6120190365233798700L;

	public CollectBehaviour(final mas.abstractAgent myagent) {
		super(myagent);
		
		//STATES
		registerFirstState(new GetBackHomeBehaviour(myagent), "GOHOME");
		registerState(new DeadlockBehaviour(myagent), "DEADLOCK");
		registerState(new PickTreasureBehaviour(myagent), "PICK");
		
		//TODO: transitions a verifier
		//TRANSITIONS
		registerDefaultTransition("GOHOME", "GOHOME", new String[]{"GOHOME"});
		registerTransition("GOHOME", "DEADLOCK", 1, new String[]{"DEADLOCK"});
		registerTransition("GOHOME", "PICK", 2, new String[]{"PICK"});
		
		registerDefaultTransition("DEADLOCK","GOHOME", new String[]{"GOHOME"});
		
		registerDefaultTransition("PICK","GOHOME", new String[]{"GOHOME"});
		
		
	}

}
