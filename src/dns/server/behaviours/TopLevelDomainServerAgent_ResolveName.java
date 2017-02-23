package dns.server.behaviours;

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
	    	/*
	    	 * Estraggo il DNS che risolve quel TLD...
	    	 */
	    	String DNSName = TLDTable.getAddressFromTLD(msg.getContent().split("\\.")[1]);
	    	if (DNSName.equals(""))
	    		System.out.println("VOID DNS RECEIVED! - TopLevelDomainServerAgent_ResolveName");
			template = new DFAgentDescription();
		    sd = new ServiceDescription();
		    sd.setName(DNSName);
		    template.addServices(sd);
		    all = new SearchConstraints();
		    all.setMaxResults(new Long(-1));
	    	
		    DFAgentDescription[] result = null;
		    try {
		        result = DFService.search(myAgent, template, all);
		        AID DNSServer = new AID();
		        for (int i = 0; i < result.length; ++i) {
		        	/*
		        	 * ...e ne recupero il riferimento, controllando la correttezza della zona.
		        	 */
		        	if (result[i].getName().getLocalName().charAt(0)==(msg.getSender().getLocalName().charAt(0)))
		            	DNSServer = result[i].getName();
		        }

		        System.out.println("TLD Server "+myAgent.getAID().getLocalName()+" - DNS Server found for zone "+msg.getSender().getLocalName().charAt(0)+". Forwarding request...");
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
