package dns.server.behaviours;

import java.util.ArrayList;

import dns.server.agents.DNSServerAgent;
import dns.tables.Host;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class DNSServerAgent_ResolveName extends Behaviour {

	private static final long serialVersionUID = 3886042025785546954L;
	
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
			MessageTemplate.MatchOntology("RESOLVE"));

	

	@Override
	public void action() {

		/*
		 * Ricevo la richiesta dal TLDServer.
		 */
		ACLMessage msg = myAgent.receive(mt);
    	if (msg != null) {
	    	System.out.println("DNS server "+myAgent.getAID().getLocalName()+" - received request to resolve "+msg.getContent()+"'s address.");
	   		ArrayList<Host> hostTable = ((DNSServerAgent)myAgent).getHostTable();
	   		String hostAddress = "";
	   		/*
	   		 * Recupero l'indirizzo confrontando il contenuto della richiesta
	   		 * con in nome dell'host presente nella tabella.
	   		 */
	   		for (int i = 0; i< hostTable.size(); i++)
	   			if (hostTable.get(i).getName().equalsIgnoreCase(msg.getContent()))
	   				hostAddress = hostTable.get(i).getAddress();
	   		/*
	   		 * Inoltro la risposta al client. Nota che il receiver è sempre stato il sender,
	   		 * in quanto ad ogni passaggio è stato aggiornato il sender con quello del client.
	   		 */
	   		ACLMessage result = new ACLMessage(ACLMessage.INFORM);
	   		result.setContent(hostAddress);
	   		result.setOntology("RESOLVE");
	   		result.addReceiver(msg.getSender());
	   		myAgent.send(result);
    	}
    	else 
    		block();
		
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}

}
