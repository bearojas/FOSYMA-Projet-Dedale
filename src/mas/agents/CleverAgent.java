package mas.agents;

import jade.core.AID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mas.abstractAgent;
import mas.behaviours.GetAgentBehaviour;
import mas.behaviours.InscriptionBehaviour;
import mas.behaviours.MainBehaviour;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import env.Environment;

public class CleverAgent extends abstractAgent{

	/**
	 * This agent registers its service (explore) with the DF
	 * It then proceeds to explore the environment
	 * The agent tries to communicate with others agents, to exchange information
	 * and to coordinate with them
	 */
	private static final long serialVersionUID = -1784844593772918359L;
	private Graph graph = new SingleGraph("");
	private List<Node> chemin = new ArrayList<Node>();
	private ArrayList<AID> agentsNearby = new ArrayList<AID>();
	private ArrayList<String> opened = new ArrayList<String>();
	
	private ArrayList<String> treasures = new ArrayList<String>();
	private ArrayList<String> diamonds = new ArrayList<String>();
	
	// {aid : (position_initiale, capacite_courante, type) }
	private HashMap<AID, ArrayList<String>> agentList = new HashMap<AID, ArrayList<String>>();
	private String firstPosition;
	
	private int communicationState = 0;
	private boolean interblocage = false;	
	private int interblocageState = 0;
	private boolean moved = true;
	private String lastPosition = "";
	
	//derniers agents avec qui ont a communiqué
	private ArrayList<AID> lastCom= new ArrayList<AID>();
	
	
	public Graph getGraph() {
		return graph;
	}

	public void setGraph(Graph graph) {
		this.graph = graph;
	}

	public List<Node> getChemin() {
		return chemin;
	}

	public void setChemin(List<Node> chemin) {
		this.chemin = chemin;
	}
	
	public HashMap<AID, ArrayList<String>> getAgentList() {
		return agentList;
	}

	public void setAgentList(HashMap<AID, ArrayList<String>> agentList) {
		this.agentList = agentList;
	}
	
	public String getFirstPosition() {
		return firstPosition;
	}

	public void setFirstPosition(String firstPosition) {
		this.firstPosition = firstPosition;
	}

	public ArrayList<AID> getAgentsNearby() {
		return agentsNearby;
	}

	public void setAgentsNearby(ArrayList<AID> agentsNearby) {
		this.agentsNearby = agentsNearby;
	}

	public ArrayList<String> getOpened() {
		return opened;
	}

	public void setOpened(ArrayList<String> opened) {
		this.opened = opened;
	}

	
	public ArrayList<String> getTreasures() {
		return treasures;
	}

	public void setTreasures(ArrayList<String> treasures) {
		this.treasures = treasures;
	}

	public ArrayList<String> getDiamonds() {
		return diamonds;
	}

	public void setDiamonds(ArrayList<String> diamonds) {
		this.diamonds = diamonds;
	}

	public int getCommunicationState() {
		return communicationState;
	}

	public void setCommunicationState(int communicationState) {
		this.communicationState = communicationState;
	}
	
	public boolean isInterblocage() {
		return interblocage;
	}

	public void setInterblocage(boolean interblocage) {
		this.interblocage = interblocage;
	}
	
	public ArrayList<AID> getLastCom() {
		return lastCom;
	}

	public void setLastCom(ArrayList<AID> lastCom) {
		this.lastCom = lastCom;
	}
	
	protected void setup(){

		super.setup();

		//get the parameters given into the object[]. In the current case, the environment where the agent will evolve
		final Object[] args = getArguments();
		if(args[0]!=null){

			deployAgent((Environment) args[0]);

		}else{
			System.err.println("Malfunction during parameter's loading of agent"+ this.getClass().getName());
			System.exit(-1);
		}
		
		doWait(2000);
		
		//Add the behaviours
		addBehaviour(new InscriptionBehaviour(this));
		addBehaviour(new GetAgentBehaviour(this));
		addBehaviour(new MainBehaviour(this));

		System.out.println("the agent "+this.getLocalName()+ " is started");

	}

	/**
	 * This method is automatically called after doDelete()
	 */
	protected void takeDown(){

	}

	public int getInterblocageState() {
		return interblocageState;
	}

	public void setInterblocageState(int interblocageState) {
		this.interblocageState = interblocageState;
	}

	public String getLastPosition() {
		return lastPosition;
	}

	public void setLastPosition(String lastPosition) {
		this.lastPosition = lastPosition;
	}

	public boolean getMoved() {
		return moved;
	}

	public void setMoved(boolean moved) {
		this.moved = moved;
	}



}
