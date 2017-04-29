package mas.behaviours;

import org.graphstream.graph.Node;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;

public class PickTreasureBehaviour extends SimpleBehaviour{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7514677342893958274L;

	private int state = 0;
	
	public PickTreasureBehaviour(mas.abstractAgent myagent) {
		super(myagent);
	}

	/**
	 * @param treasure : le tresor qu'on va chercher
	 * @return l'AID de l'agent qu'on va contacter
	 */
	private AID searchCoalition(String treasure){		
		//TODO
		//recuperer AgentList dans cleverAgent
		
		return null;
	}
	
	private String chooseTreasure(){
		//TODO
		//recuperer AgentList et Treasures dans cleverAgent
		
		return null;
	}
	
	
	public void action() {
		// TODO choisir un tresor a ramasser
		// mettre sont type dans les pages jaunes si pas fait
		//chercher une nouvelle coalition
		//chercher l'agent concerné
		
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
