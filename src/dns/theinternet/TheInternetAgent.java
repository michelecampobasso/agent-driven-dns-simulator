package dns.theinternet;

import jade.core.Agent;

public class TheInternetAgent extends Agent {

	private static final long serialVersionUID = -1410245168690341537L;
	
	@Override
    protected void setup() {
    	
        System.out.println("The Internet started.");
        this.addBehaviour(new TheInternetAgent_CreateNewHost(this, 40000));
    }
}
