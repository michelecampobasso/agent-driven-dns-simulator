package dns.server.agents;

import dns.server.behaviours.*;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class RootServerAgent extends Agent {
	

	private static final long serialVersionUID = 6094472589305908106L;    

    @Override
    protected void setup() {
        System.out.println("Root Server started.");
        
        /* 
         * Registrazione al DF...
         */
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd  = new ServiceDescription();
        sd.setType("ROOTSERVER");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {  
            DFService.register(this, dfd);
            System.out.println("Root Server " + getAID().getLocalName() + " registered.");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            System.err.println("!!ERROR!! Registration of RootServerAgent to DF failed! System may not work properly.");
        }
        
        //this.addBehaviour(new RootServer_ClosestZone());
        this.addBehaviour(new RootServer_ResolveName());
        this.addBehaviour(new RootServer_CreateNewHost());
    }
    
    @Override
	public void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			System.err.println("Problems while deregistering the RootServer "+getAID().getLocalName()+". System may not work properly.");
			e.printStackTrace();
		}
	}

}
