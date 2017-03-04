package dns.server.behaviours;

import java.io.IOException;

import dns.server.agents.DNSServerAgent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class DNSServerAgent_GetAllHosts extends Behaviour {

	private static final long serialVersionUID = -4444828602824654871L;

	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
			MessageTemplate.MatchOntology("DNSHOSTS"));
	@Override
	public void action() {

		/*
		 * Ricevo il messaggio dal pari...
		 */
		ACLMessage msg = myAgent.receive(mt);
    	if (msg != null) {
	    	System.out.println("DNS Server - received request to give my table to "+msg.getSender().getLocalName()+".");
	    	try {
	    		/*
	    		 * ...e gli mando l'intera tabella degli hosts conosciuti. 
	    		 */
	    		ACLMessage reply = msg.createReply();
	    		reply.setOntology("DNSHOSTS");
				reply.setContentObject(((DNSServerAgent)myAgent).getHostTable());
				reply.setPerformative(ACLMessage.INFORM);
				myAgent.send(reply);
			} catch (IOException e) {
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
