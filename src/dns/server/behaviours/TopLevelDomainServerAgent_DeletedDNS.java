package dns.server.behaviours;

import dns.server.agents.TopLevelDomainServerAgent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class TopLevelDomainServerAgent_DeletedDNS extends Behaviour {

	private static final long serialVersionUID = -682126602444862328L;

	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CANCEL), 
			MessageTemplate.MatchOntology("TAKEDOWN"));

	@Override
	public void action() {
		
		ACLMessage msg = myAgent.receive(mt);
    	if (msg != null) {
    		((TopLevelDomainServerAgent)myAgent).getTLDTable().deleteHost(msg.getSender().getLocalName());
    	} else
    		block();
	}

	@Override
	public boolean done() {
		return false;
	}

}
