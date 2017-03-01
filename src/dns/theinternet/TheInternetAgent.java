package dns.theinternet;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class TheInternetAgent extends Agent {

	private static final long serialVersionUID = -1410245168690341537L;
	
	@Override
    protected void setup() {
    	
        System.out.println("The Internet started.");
        
        /*
		 * Registrazione al DF...
		 */
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd  = new ServiceDescription();
        sd.setType("THEINTERNET");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {  
            DFService.register(this, dfd);
            System.out.println("The Internet is here, folks!");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            System.out.println("!!ERROR!! Registration of The Internet to DF failed! System may not work properly.");
        }
        
        this.addBehaviour(new TheInternetAgent_CreateNewHost(this, 40000));
        this.addBehaviour(new TheInternetAgent_AllHosts());
    }
}
