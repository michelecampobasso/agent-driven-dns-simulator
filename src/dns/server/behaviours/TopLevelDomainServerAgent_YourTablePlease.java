package dns.server.behaviours;

import java.io.IOException;

import dns.server.agents.TopLevelDomainServerAgent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class TopLevelDomainServerAgent_YourTablePlease extends Behaviour {

	private static final long serialVersionUID = -179582543920949648L;

	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
			MessageTemplate.MatchOntology("YOURTABLEPLEASE"));
	
	@Override
	public void action() {
		
		ACLMessage msg = myAgent.receive(mt);
    	if (msg != null) {
	    	System.out.println("TLD server "+myAgent.getAID().getLocalName()+" - received request to give my table to "+msg.getSender().getLocalName()+".");
	   		ACLMessage reply = msg.createReply();
	   		reply.setOntology("YOURTABLEPLEASE");
	   		reply.setPerformative(ACLMessage.INFORM);
	   		try {
				reply.setContentObject(((TopLevelDomainServerAgent)myAgent).getTLDTable());
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
