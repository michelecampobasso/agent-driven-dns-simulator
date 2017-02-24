package dns.client.behaviours;

import dns.client.agents.ClientAgent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ClientAgent_GetNewHost extends Behaviour {

	private static final long serialVersionUID = 8419198928550138489L;

	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
			MessageTemplate.MatchOntology("NEWHOST"));
	
	@Override
	public void action() {
		
		ACLMessage msg = myAgent.receive(mt);
		if (msg != null) {
			System.out.println("Client - received infos about a new host ("+msg.getContent().split("\\s+")[0]+"). Reading...");
			((ClientAgent)myAgent).addHost(msg.getContent().split("\\s+")[0]);
	    	System.out.println("Client - host added.");
		}
		else
			block();
		
	}

	@Override
	public boolean done() {
		return false;
	}

}
