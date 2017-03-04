package dns.server.behaviours;

import java.io.IOException;
import java.util.ArrayList;

import dns.server.agents.DNSServerAgent;
import dns.tables.Host;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class DNSServerAgent_YourTLDPlease extends Behaviour {

	private static final long serialVersionUID = -4587310888290680877L;
	
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
			MessageTemplate.MatchOntology("YOURTLDPLEASE"));

	@Override
	public void action() {
		
		/*
		 * Ricevo la richiesta di inoltrare l'elenco dei TLD da me risolti.
		 */
		ACLMessage msg = myAgent.receive(mt);
    	if (msg != null) {
	    	System.out.println("DNS server "+myAgent.getAID().getLocalName()+" - received request to give my TLDs to "+msg.getSender().getLocalName()+".");
	    	
	    	ArrayList<Host> hostTable = ((DNSServerAgent)myAgent).getHostTable();
	    	ArrayList<String> TLDs = new ArrayList<String>();
	    	for (int i=0; i<hostTable.size(); i++) 
	    		if (!TLDs.contains(hostTable.get(i)))
	    			TLDs.add(hostTable.get(i).getName().split("\\.")[1]);
	    	
	    	
	    	ACLMessage reply = msg.createReply();
	   		reply.setOntology("YOURTLDPLEASE");
	   		reply.setPerformative(ACLMessage.INFORM);
	   		try {
				reply.setContentObject(TLDs);
			} catch (IOException e) {
				e.printStackTrace();
			}
	   		myAgent.send(reply);
    	} else
    		block();
	   	
	}

	@Override
	public boolean done() {
		return false;
	}
}
