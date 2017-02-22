package mas.behaviours;

import java.util.ArrayList;

import mas.agents.CleverAgent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public class GetAgentBehaviour extends OneShotBehaviour {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public GetAgentBehaviour(mas.abstractAgent myagent){
		super(myagent);
	}
	@Override
	public void action() {
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd  = new ServiceDescription();
		sd.setType( "explorer" ); /* service souhaité */
		dfd.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(super.myAgent, dfd);
			ArrayList<AID> agentList = new ArrayList<AID>();
			for (DFAgentDescription ad : result){
				agentList.add(ad.getName());
			}
			((CleverAgent)super.myAgent).setAgentList(agentList);
			
			//System.out.println("liste d'agents:"+agentList.toString());
			
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		
	}

}
