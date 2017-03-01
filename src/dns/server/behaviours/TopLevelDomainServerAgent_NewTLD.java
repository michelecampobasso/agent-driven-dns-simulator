package dns.server.behaviours;

import java.util.ArrayList;

import dns.server.agents.TopLevelDomainServerAgent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class TopLevelDomainServerAgent_NewTLD extends Behaviour {

	private static final long serialVersionUID = 7190734753047776540L;
	
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE),
			MessageTemplate.MatchOntology("NEWTLD"));

	@SuppressWarnings("unchecked")
	@Override
	public void action() {
		
		ACLMessage msg = myAgent.receive(mt);
		if (msg != null) {
			try {
				ArrayList<String> chosenDNSs = (ArrayList<String>) msg.getContentObject();
				((TopLevelDomainServerAgent)myAgent).updateTLDTable(chosenDNSs, msg.getConversationId());
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
		else
			block();
	}

	@Override
	public boolean done() {
		return false;
	}

}
