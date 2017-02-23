package dns.client.agents;

import dns.client.behaviours.*;
import dns.tables.TLDLatencyTable;
import jade.core.Agent;

public class ClientAgent extends Agent {

	private static final long serialVersionUID = 271669436955865796L;
	public TLDLatencyTable closestTLDs;
	
    @Override
    protected void setup() {
    	
        System.out.println("Client started.");
        //this.addBehaviour(new ClientAgent_GetTLDs(this, 10000));
        this.addBehaviour(new ClientAgent_ResolveName(this, 5000)); //This can run only when the other behaviour has completed TODO implement delays
    }
    
    public void setTLDs(TLDLatencyTable t) {
    	closestTLDs = t;
    }
	
}
