package mas.behaviours;

import jade.core.behaviours.SimpleBehaviour;

public class GetBackHomeBehaviour extends SimpleBehaviour{


	/**
	 * the agent go back to its initial position and registers its new service (collect) with the DF
	 */
	private static final long serialVersionUID = 2139427080509307681L;
	private int state = 0;
	
	public GetBackHomeBehaviour(mas.abstractAgent myagent) {
		super(myagent);
	}

	public void action() {
		// TODO revenir au point initial (recherche de chemin)
		//traiter interblocages 
		//changer le service a "collect" dans les pages jaunes
		//attente: reste immobile et regarder boite aux lettres et regarder pages jaunes 
		// traiter le cas de l'agent avec plus grande capacite une fois que tout le monde soit en "collect"
		
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
