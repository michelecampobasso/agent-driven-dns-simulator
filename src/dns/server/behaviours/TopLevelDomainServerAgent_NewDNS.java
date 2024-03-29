package dns.server.behaviours;

import java.util.ArrayList;

import dns.server.agents.TopLevelDomainServerAgent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class TopLevelDomainServerAgent_NewDNS extends Behaviour {

	private static final long serialVersionUID = -6022751488319781679L;

	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
			MessageTemplate.MatchOntology("NEWDNS"));

	@SuppressWarnings("unchecked")
	@Override
	public void action() {
		
		/*
		 * Ricevuto il messaggio contenente informazioni riguardanti quali TLD sono risolti da un nuovo DNS.
		 */
		ACLMessage msg = myAgent.receive(mt);
		if (msg != null) {
			try {
				ArrayList<String> TLDs = (ArrayList<String>) msg.getContentObject();
				((TopLevelDomainServerAgent)myAgent).addTLDsForHost(TLDs, msg.getSender().getLocalName());
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
