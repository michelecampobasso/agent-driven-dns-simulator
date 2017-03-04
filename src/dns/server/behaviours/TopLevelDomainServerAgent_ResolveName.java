package dns.server.behaviours;

import java.util.ArrayList;
import java.util.Random;

import dns.server.agents.TopLevelDomainServerAgent;
import dns.tables.TLDTable;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class TopLevelDomainServerAgent_ResolveName extends Behaviour {

	private static final long serialVersionUID = -616014726181122196L;
	
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
			MessageTemplate.MatchOntology("RESOLVE"));

	private ACLMessage request;
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;

	@Override
	public void action() {

		/*
		 * Ricevo la richiesta dal RootServer.
		 */
		ACLMessage msg = myAgent.receive(mt);
    	if (msg != null) {
	    	System.out.println("TLD Server "+myAgent.getAID().getLocalName()+" - received request to resolve "+msg.getContent()+"'s address.");
	    	/*
	    	 * Ottengo la tabella contenente i riferimenti ai DNS in base al TLD
	    	 */
	    	TLDTable TLDTable = ((TopLevelDomainServerAgent)myAgent).getTLDTable();
	    	boolean rightZone = true;
	    	/*
	    	 * Estraggo i DNS che risolvono quel TLD in zona...
	    	 */
	    	String TLD = msg.getContent().split("\\.")[1];
	    	ArrayList<String> DNSNames = TLDTable.getAddressesFromTLDByZone(TLD, msg.getSender().getLocalName().charAt(0));
	    	/*
	    	 * Se non ce ne sono, prendo tutti i DNS che risolvono quel TLD
	    	 */
	    	if (DNSNames.size()==0) {
	    		DNSNames = TLDTable.getAddressesFromTLD(TLD);
	    		rightZone = false;
	    	}
	    	if (DNSNames.size()!=0) {
	    		Random rnd = new Random();
				template = new DFAgentDescription();
			    sd = new ServiceDescription();
			    // Ne scelgo uno...
			    sd.setName(DNSNames.get(rnd.nextInt(DNSNames.size())));
			    template.addServices(sd);
			    all = new SearchConstraints();
			    all.setMaxResults(new Long(-1));
		    	
			    DFAgentDescription[] result = null;
			    try {
			    	// ...e ne recupero il riferimento
			        result = DFService.search(myAgent, template, all);
			        AID DNSServer = result[0].getName();
		        	if (rightZone)
		        		System.out.println("TLD Server "+myAgent.getAID().getLocalName()+" - DNS Server found for zone "+msg.getSender().getLocalName().charAt(0)+". Forwarding request...");
		        	else
		        		System.out.println("TLD Server "+myAgent.getAID().getLocalName()+" - DNS Server for zone "+msg.getSender().getLocalName().charAt(0)+" currently down. Forwarding request to "+DNSServer.getLocalName()+"...");
		        	request = new ACLMessage(ACLMessage.REQUEST);
			    	/*
			    	 * Inoltro la richiesta al DNS trovato specificando come AID a cui rispondere quello del client
			    	 */
			    	request.setContent(msg.getContent());
		        	request.addReceiver(DNSServer);
		        	request.setSender(msg.getSender());
		    		request.setOntology("RESOLVE");
		    		System.out.println("TLD Server "+myAgent.getAID().getLocalName()+" - sending request to " + DNSServer.getLocalName() + " to resolve host..." );
		    		this.myAgent.send(request);
			    } catch (final FIPAException fe) {
			        fe.printStackTrace();
			    }
	    	} else {
	    		System.err.println("TLD Server - no DNS Servers available. System is not working, create a new DNS ASAP.");
	    		/*
	    		 * Unlocking Client...
	    		 */
	    		ACLMessage unlock = new ACLMessage(ACLMessage.INFORM);
	    		unlock.setContent("");
	    		unlock.addReceiver(msg.getSender());
	    		unlock.setOntology("RESOLVE");
	    		myAgent.send(unlock);
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
