package dns.server.behaviours;

import dns.server.agents.TopLevelDomainServerAgent;
import dns.tables.TLDTable;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class TopLevelDomainServerAgent_CoherenceReceive extends Behaviour {

	private static final long serialVersionUID = 6780904822556012545L;

	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE), 
			MessageTemplate.MatchOntology("COHERENCE"));
	private ACLMessage coherenceCheck;
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;
	private TLDTable TLDTable;
	
	@Override
	public void onStart() {
	
		coherenceCheck = new ACLMessage(ACLMessage.REQUEST);
		template = new DFAgentDescription();
		sd = new ServiceDescription();
		sd.setType("ROOTSERVER");
		template.addServices(sd);
		all = new SearchConstraints();
		all.setMaxResults(new Long(-1));
	
	}
	
	@Override
	public void action() {
		// TODO Auto-generated method stub
		/*
		 * Attendo la ricezione di un messaggio contentente la tabella che mi serve, se ho tutto io, eventualmente aggiorno,
		 * se non ho tutto io mando agli altri pari della mia stessa zona
		 */
		
		ACLMessage msg = myAgent.receive(mt);
		if (msg != null) {
			System.out.println("TLDServer - received tables from peer. Reading...");
    	
			try {
				TLDTable = (TLDTable) msg.getContentObject();
			} catch (UnreadableException e) {
				// TODO Auto-generated catch block
				if (msg.getContent().equals(null))
					TLDTable = null;
				System.out.println("!!ERROR!! TLDTable not received... EMMO'?");
			}
	    	
	    	System.out.println("Client - TLDTable received. Reading...");
	    	TLDTable agentTLDTable = ((TopLevelDomainServerAgent)myAgent).getTLDTable();
	    	for (int i=0; i<TLDTable.getSize(); i++)
	    		// Controllo che il valore corrente sia presente nella tabella dell'agente
	    		for (int j=0; j<agentTLDTable.getSize(); j++)
	    			// Se c'è match tra indirizzo e TLD...
	    			if (TLDTable.getAddress(i).equals(agentTLDTable.getAddress(j)) && TLDTable.getTLD(i).TLD.equalsIgnoreCase(agentTLDTable.getTLD(j).TLD))
	    				// Controllo che 
	    				if (TLDTable.getTLD(i).timeStamp.after(agentTLDTable.getTLD(j).timeStamp))
	    					((TopLevelDomainServerAgent)myAgent).updateTLDTableEntry(TLDTable.getTLD(i).TLD, TLDTable.getAddress(i), TLDTable.getTLD(i).timeStamp, j);
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
