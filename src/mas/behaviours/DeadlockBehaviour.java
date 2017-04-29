package mas.behaviours;

import jade.core.behaviours.SimpleBehaviour;

public class DeadlockBehaviour extends SimpleBehaviour{

	/**
	 * Gere les cas d'interblocages apres avoir fini l'exploration
	 */
	private static final long serialVersionUID = 8484925065523685732L;
	private int state = 0;
	
	public DeadlockBehaviour(mas.abstractAgent myagent) {
		super(myagent);
	}

	public void action() {
		// TODO Auto-generated method stub
		
		switch(state){
			case 0:
				
				
			case 1:
				
				
				
			case 2:
				
			
			case 3:
				
				
				
			default:
				break;
		
		}
		
		
	}


	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}
}
