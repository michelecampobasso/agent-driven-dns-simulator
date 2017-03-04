package dns.server.behaviours;

import java.util.ArrayList;
import java.util.Random;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RootServer_ResolveName extends Behaviour {

	private static final long serialVersionUID = 4751499738564455696L;
	
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
													MessageTemplate.MatchOntology("RESOLVE"));

	private ACLMessage request;
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;
	
	@Override
	public void onStart() {
		
		template = new DFAgentDescription();
	    sd = new ServiceDescription();
	    sd.setType("TLDSERVER");
	    template.addServices(sd);
	    all = new SearchConstraints();
	    all.setMaxResults(new Long(-1));
	}

	@Override
	public void action() {
		
		/*
		 * Ricevo il messaggio dal Client...
		 */
		ACLMessage msg = myAgent.receive(mt);
    	if (msg != null) {
	    	System.out.println("Root Server - received request to resolve "+msg.getContent()+"'s address.");
	   		
	    	/*
	    	 * Il RootServer richiede ogni volta quali sono i TLDServer al DF: in questo modo
	    	 * ho una conoscenza corretta di quali sono i subordinati attivi.
	    	 */
			
		    DFAgentDescription[] result = null;
		    try {
		        result = DFService.search(myAgent, template, all);
		        ArrayList<AID> TLDServers = new ArrayList<AID>();
		        boolean rightZone = true;
		        /*
		         * Recupero i TLD Server di zona.
		         */
		        for (int i = 0; i < result.length; ++i) 
		        	if (result[i].getName().getLocalName().charAt(0)==(msg.getSender().getLocalName().charAt(0)))
		            	TLDServers.add(result[i].getName());
		        /*
		         * Se i TLD in zona non sono disponibili, prendo i 
		         * TLD di un'altra zona.
		         */
		        if (TLDServers.size()==0) { 
		        	for (int i = 0; i < result.length; ++i) 
			        	if (result[i].getName().getLocalName().charAt(0)!=(msg.getSender().getLocalName().charAt(0)))
			            	TLDServers.add(result[i].getName());
		        	rightZone = false;
		        }
		        if (TLDServers.size()!=0) {
		        	/*
		        	 * Trovato almeno un TLDServer da interrogare, inoltro la richiesta di risoluzione host.
		        	 */
		        	Random rnd = new Random();
		        	AID TLDServer = TLDServers.get(rnd.nextInt(TLDServers.size()));
		        	if (rightZone)
		        		System.out.println("Root Server - TLD Server found for zone "+msg.getSender().getLocalName().charAt(0)+". Forwarding request...");
		        	else
		        		System.out.println("Root Server - TLD Server for zone "+msg.getSender().getLocalName().charAt(0)+" currently down. Forwarding request to "+TLDServer.getLocalName()+"...");
			    	request = new ACLMessage(ACLMessage.REQUEST);
			    	request.setContent(msg.getContent());
			    	request.setSender(msg.getSender());
		        	request.addReceiver(TLDServer);
		    		request.setOntology("RESOLVE");
		    		System.out.println("Root Server - sending request to " + TLDServer.getLocalName() + " to resolve the host..." );
		    		this.myAgent.send(request);
		        } else {
		        	System.err.println("Root Server - no TLD Servers available. System is not working, create a new TLD ASAP.");
		        	ACLMessage unlock = new ACLMessage(ACLMessage.INFORM);
		    		unlock.setContent("");
		    		unlock.addReceiver(msg.getSender());
		    		unlock.setOntology("RESOLVE");
		    		myAgent.send(unlock);
		        }
		    } catch (final FIPAException fe) {
		        fe.printStackTrace();
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
