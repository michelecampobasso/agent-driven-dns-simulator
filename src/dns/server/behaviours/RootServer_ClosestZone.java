package dns.server.behaviours;

import java.io.IOException;

import dns.tables.TLDLatencyTable;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RootServer_ClosestZone extends Behaviour {

	private static final long serialVersionUID = 1L;
	private TLDLatencyTable closestTLDs; 
	
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
													MessageTemplate.MatchOntology("CLOSESTTLD"));

	@Override
	public void action() {
		
    	ACLMessage msg = myAgent.receive(mt);
    	if (msg != null) {
	    	System.out.println("Root server "+myAgent.getAID().getLocalName()+" - received request to closest TLD. Dispatching...");
	    	int zone = Integer.parseInt(msg.getSender().getLocalName().split("\\.")[0]);
	   		closestTLDs = TLDLatencyTable.getHostOptions(zone);
	    	
	   		ACLMessage reply = msg.createReply();
			try {
				reply.setContentObject(closestTLDs);
			} catch (IOException e) {
				reply.setContent("");
			}
	    	reply.setPerformative(ACLMessage.INFORM);
			myAgent.send(reply);
	    	System.out.println("Root server "+myAgent.getAID().getLocalName()+" - closest TLD sent.");
    	}
    	else 
    		block();
	}
	
	@Override
	public boolean done() {
		return false;
	}

}
