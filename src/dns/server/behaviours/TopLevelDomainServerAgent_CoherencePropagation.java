package dns.server.behaviours;

import java.io.IOException;
import java.util.ArrayList;

import dns.server.agents.TopLevelDomainServerAgent;
import dns.tables.TLDTable;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class TopLevelDomainServerAgent_CoherencePropagation extends TickerBehaviour {

	private static final long serialVersionUID = 5658996793604322502L;

	public TopLevelDomainServerAgent_CoherencePropagation(Agent a, long period) {
		super(a, period);
	}
	
	private boolean pendingRequest = false;
	private ACLMessage coherenceCheck;
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;
	
	@Override
	public void onStart() {
		
		coherenceCheck = new ACLMessage(ACLMessage.PROPAGATE);
		template = new DFAgentDescription();
	    sd = new ServiceDescription();
	    sd.setType("TLDSERVER");
	    template.addServices(sd);
	    all = new SearchConstraints();
	    all.setMaxResults(new Long(-1));

	}

	@Override
	protected void onTick() {
		
		/*
		 * Obtaining peers from other zones by querying DF.
		 */
			
		DFAgentDescription[] result = null;
	    try {
	        /*
	         * 6- Query the DF about the service you look for.
	         */
	        result = DFService.search(myAgent, template, all);
	        AID[] TLDServer = new AID[result.length];
	        for (int i = 0; i < result.length; ++i) {
	            /*
	             * 7- Collect the TLD of other zones
	             */
	        	if (!result[i].getName().getLocalName().substring(0,1).equals(myAgent.getLocalName().substring(0,1)))
	            	TLDServer[i] = result[i].getName();
	        }
	        if (result.length!=0)
	        	System.out.println("TLDServer - CheckPeerForCoherence - peers found.");
	    } catch (final FIPAException fe) {
	        fe.printStackTrace();
	    }
	
	    /*
	     * If we found at least one agent offering the desired service,
	     * we try to buy the book using a custom FSM-like behaviour.
	     */
	    if (result != null && result.length != 0) {
	    	ArrayList<Integer> processedZones = new ArrayList<Integer>();
	    	TLDTable table = ((TopLevelDomainServerAgent)myAgent).getTLDTable();
	    	for (int i = 0; i<result.length; i++) {
	    		// Invio la richiesta di controllo ad un singolo TLD per zona.
	    		int zone = Integer.parseInt(result[i].getName().getLocalName().substring(0,1));
	    		if (!processedZones.contains(zone)) {
	    			coherenceCheck.addReceiver(result[i].getName());
	    			coherenceCheck.setOntology("COHERENCE");
	    			try {
						coherenceCheck.setContentObject(table);
					} catch (IOException e) {
						coherenceCheck.setContent("");
					}
		    		System.out.println("Sending hosts table to " + result[0].getName().getName() + "..." );
		    		this.myAgent.send(coherenceCheck);
		    		processedZones.add(zone);
		    		pendingRequest = true;
	    		}
	    	}
    	} else
	        System.out.println("No peers found, retrying in 60 seconds...");
	}
}
