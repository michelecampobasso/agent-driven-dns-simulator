package dns.server.behaviours;

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
	public void action() {
		
		/*
		 * Ricevo il messaggio dal Client...
		 */
		ACLMessage msg = myAgent.receive(mt);
    	if (msg != null) {
	    	System.out.println("Root Server - received request to resolve "+msg.getContent()+"'s address.");
	   		
	    	/*
	    	 * Il RootServer richiede ogni volta quali sono i TLDServer... TODO tabella?
	    	 */
			template = new DFAgentDescription();
		    sd = new ServiceDescription();
		    sd.setType("TLDSERVER");
		    template.addServices(sd);
		    all = new SearchConstraints();
		    all.setMaxResults(new Long(-1));
	    	
		    DFAgentDescription[] result = null;
		    try {
		        result = DFService.search(myAgent, template, all);
		        AID TLDServer = new AID();
		        /*
		         * Il TLDServer è uno per zona, lo recupero.
		         */
		        for (int i = 0; i < result.length; ++i) {
		        	if (result[i].getName().getLocalName().charAt(0)==(msg.getSender().getLocalName().charAt(0)))
		            	TLDServer = result[i].getName();
		        }
		        if (result.length!=0) {
		        	/*
		        	 * Trovato il TLDServer di pertinenza, inoltro la richiesta di risoluzione host.
		        	 */
		        	System.out.println("Root Server - TLD Server found for zone "+msg.getSender().getLocalName().charAt(0)+". Forwarding request...");
			    	request = new ACLMessage(ACLMessage.REQUEST);
			    	request.setContent(msg.getContent());
			    	request.setSender(msg.getSender());
		        	request.addReceiver(TLDServer);
		    		request.setOntology("RESOLVE");
		    		System.out.println("Root Server - sending request to " + TLDServer.getLocalName() + " to resolve the host..." );
		    		this.myAgent.send(request);
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
		// TODO Auto-generated method stub
		return false;
	}

}
