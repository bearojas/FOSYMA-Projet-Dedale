package mas.behaviours;

import java.util.ArrayList;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class InscriptionBehaviour extends OneShotBehaviour{
	
	/**
	 * An agent registers its service (explore) with the DF
	 */
	private static final long serialVersionUID = 1L;
	
	public InscriptionBehaviour(mas.abstractAgent myagent) {
		super(myagent);
	}
	@Override
	public void action() {
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(super.myAgent.getAID()); /* getAID est l'AID de l'agent qui veut s'enregistrer*/
		ServiceDescription sd  = new ServiceDescription();
		sd.setType( "explorer" ); /* service qu'on propose*/
		sd.setName(super.myAgent.getLocalName() );
		dfd.addServices(sd);
		try {
		DFService.register(super.myAgent, dfd );
		} catch (FIPAException fe) { fe.printStackTrace(); }
		
		//System.out.println("AID:"+super.myAgent.getAID());
		
	}

}
