package mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import env.Attribute;
import env.Couple;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;


public class ExploreBehaviour extends Behaviour {

	private static final long serialVersionUID = 9088209402507795289L;
	private boolean finished = false;
	private HashMap<String,Couple<List<String>,List<Attribute>>> opened ; 
	private HashMap<String,Couple<List<String>,List<Attribute>>> closed ;
	
	
	public ExploreBehaviour(final mas.abstractAgent myagent){
		super(myagent);
		opened = new HashMap();
		closed = new HashMap();
	}

	@Override
	public void action() {

		String myPosition=((mas.abstractAgent)this.myAgent).getCurrentPosition();
		
		if (myPosition!=""){


			//List of observable from the agent's current position
			List<Couple<String,List<Attribute>>> lobs=((mas.abstractAgent)this.myAgent).observe();//myPosition
			
			//list of attribute associated to the currentPosition
			int posIndex = 0;
			for(int i = 0; i < lobs.size(); i++){
				if(lobs.get(i).getLeft() == myPosition){
					posIndex = i;
					break;
				}
			}
			List<Attribute> lattribute= lobs.get(posIndex).getRight();
			
			System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);
		
			//on recupere tous les voisins du noeud courant
			//on les ajoute a la liste des ouverts s'il ne sont pas deja fermes
			ArrayList<String> neighbors = new ArrayList<String>();
			
			for(int i=0; i < lobs.size(); i++){
				if(posIndex != i){
					neighbors.add(lobs.get(i).getLeft());
					if(closed.containsKey(lobs.get(i).getLeft()) == false){
						opened.put(lobs.get(i).getLeft(), new Couple(new ArrayList(), lobs.get(i).getRight()));
				
					}
				}
			}
			//on rajoute le noeud courant a la liste des fermes et on l'enleve des ouverts
			closed.put(myPosition,new Couple(neighbors, lattribute));
			opened.remove(myPosition);
			
			System.out.println("Fermés : "+closed.toString());
			System.out.println("Ouverts : "+opened.toString());
			
			//Little pause to allow you to follow what is going on
			try {
				System.out.println("Press a key to allow the agent "+this.myAgent.getLocalName() +" to execute its next move");
				System.in.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			//A MODIFIER:

			//example related to the use of the backpack for the treasure hunt
			Boolean b=false;
			for(Attribute a:lattribute){
				switch (a) {
				case TREASURE:
					System.out.println("My current backpack capacity is:"+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					System.out.println("Value of the treasure on the current position: "+a.getValue());
					System.out.println("The agent grabbed :"+((mas.abstractAgent)this.myAgent).pick());
					System.out.println("the remaining backpack capacity is: "+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					System.out.println("The value of treasure on the current position: (unchanged before a new call to observe()): "+a.getValue());
					b=true;
					break;

				default:
					break;
				}
			}

			//If the agent picked (part of) the treasure
			if (b){
				List<Couple<String,List<Attribute>>> lobs2=((mas.abstractAgent)this.myAgent).observe();//myPosition
				System.out.println("lobs after picking "+lobs2);
			}
			//chercher chemin via ses voisins
			int j = 0;
			String nextId = "";
			while(nextId == "" && j < neighbors.size()){
				if(opened.containsKey(neighbors.get(j))){
					nextId = neighbors.get(j);
				}
				j++;
			}
			if(nextId != ""){
				// déplacement vers prochain voisin ouvert
				((mas.abstractAgent)this.myAgent).moveTo(nextId);
			}
//			else{
//				//fonction chercher chemin plus eloigné
//			}
		}
		
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return finished;
	}

	
	
	
}
