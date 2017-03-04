package dns.server.behaviours;

import java.util.Calendar;

import dns.server.agents.DNSServerAgent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class DNSServerAgent_CreateNewHost extends Behaviour {

	private static final long serialVersionUID = -493879327263389940L;

	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
			MessageTemplate.MatchOntology("NEWHOST"));

	@Override
	public void action() {
		
		ACLMessage msg = myAgent.receive(mt);
		if (msg != null) {
			System.out.println("DNS Server "+myAgent.getAID().getLocalName()+" - received msg to add a new host on the system.");
			System.out.println("DNS Server "+myAgent.getAID().getLocalName()+"- the host's name is "+msg.getContent().split("\\s+")[0]+" and address is: "+msg.getContent().split("\\s+")[1]);
			
			// Provo ad aggiungere l'host alla tabella
			((DNSServerAgent)myAgent).addHost(msg.getContent().split("\\s+")[0], msg.getContent().split("\\s+")[1]);
			System.out.println("DNS Server "+myAgent.getAID().getLocalName()+" - host added successfully.");
		} else
			block();
	}

	@Override
	public boolean done() {
		return false;
	}

}
