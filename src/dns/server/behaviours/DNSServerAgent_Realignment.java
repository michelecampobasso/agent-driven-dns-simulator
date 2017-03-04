package dns.server.behaviours;

import java.util.ArrayList;

import dns.server.agents.DNSServerAgent;
import dns.tables.Host;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class DNSServerAgent_Realignment extends Behaviour {

	private static final long serialVersionUID = 2101594915938927039L;
	
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
			MessageTemplate.MatchOntology("NEEDINFO"));
	
	@Override
	public void action() {
		ACLMessage msg = myAgent.receive(mt);
		if (msg != null) {
			/*
			 * Ricerco l'host nella tabella...
			 */
			ArrayList<Host> hostTable = ((DNSServerAgent)myAgent).getHostTable();
			String hostAddress = "";
			for (int i=0; i<hostTable.size(); i++) {
				System.out.println(i);
				if (hostTable.get(i).getName().equalsIgnoreCase(msg.getContent())) {
					hostAddress = hostTable.get(i).getAddress();
					break;
				}
			}
			/*
			 * Se l'ho trovato lo mando, altrimenti comunico di continuare a cercare.
			 */
			if (hostAddress == "")
				hostAddress = "noinfo";
			ACLMessage reply = msg.createReply();
			reply.setContent(hostAddress);
			reply.setPerformative(ACLMessage.INFORM);
			reply.setOntology("NEEDINFO");
			myAgent.send(reply);
		} else
			block();
	}

	@Override
	public boolean done() {
		return false;
	}
}
