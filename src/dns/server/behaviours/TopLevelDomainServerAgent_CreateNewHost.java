package dns.server.behaviours;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import dns.server.agents.TopLevelDomainServerAgent;
import dns.tables.TLDTable;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class TopLevelDomainServerAgent_CreateNewHost extends Behaviour {

	private static final long serialVersionUID = 78331654909620029L;

	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
			MessageTemplate.MatchOntology("NEWHOST"));
	
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;

	@Override
	public void action() {

		ACLMessage msg = myAgent.receive(mt);
		if (msg != null) {
			System.out.println("TLDServer "+myAgent.getLocalName()+" - received msg to add a new host on the system.");
			System.out.println("TLDServer "+myAgent.getLocalName()+" - the host's name is "+msg.getContent().split("\\s+")[0]+" and address is: "+msg.getContent().split("\\s+")[1]);
			
			/*
	    	 * Questo behaviour viene lanciato post inizializzazione, quindi non devo cercare
	    	 * nuovamente i DNS ma utilizzo quelle che sono presenti nell'agente. 
	    	 * Ci sono due casi: 
	    	 * 		1) il TLD contiene già il riferimento ad un DNS che risolve quel particolare TLD;
	    	 * 		2) il TLD non contiene il riferimento e deve scegliere un DNS che si occuperà di risolverlo.
	    	 */
			
			TLDTable table = ((TopLevelDomainServerAgent)myAgent).getTLDTable();
			
			String TLD = msg.getContent().split("\\s+")[0].split("\\.")[1];
			String DNSAddress = table.getAddressFromTLD(TLD);
			
			if (DNSAddress == null || DNSAddress == "") {
				ArrayList<String> DNSServerAddresses = table.getAllAddresses();
				Random rnd = new Random();
				DNSAddress = DNSServerAddresses.get(rnd.nextInt(DNSServerAddresses.size()));
				if (!table.addHost(TLD, Calendar.getInstance(), DNSAddress))
					System.out.println("TLDServer "+myAgent.getLocalName()+" - error while adding the host!");
				System.out.println("TLDServer - new TLD, adding it and picking "+DNSAddress+" as responsable.");
			}
			// Se ce l'ho già salto direttamente qui.
			template = new DFAgentDescription();
		    sd = new ServiceDescription();
		    sd.setType("DNSSERVER");
		    sd.setName(DNSAddress);
		    template.addServices(sd);
		    all = new SearchConstraints();
		    all.setMaxResults(new Long(-1));
		    
			DFAgentDescription[] result = null;
			try {
				result = DFService.search(myAgent, template, all);
			} catch (FIPAException e) {
				e.printStackTrace();
			}
			
			/*
			 * TODO se non posso risolvere questo TLD in zona, devo riferirlo ad un altro DNS
			 */
	        
			if (result != null) {
			// ...e gli inoltro il nuovo host.
		    	ACLMessage proposal = new ACLMessage(ACLMessage.INFORM);
		    	proposal.setContent(msg.getContent());
		    	proposal.addReceiver(result[0].getName());
		    	proposal.setOntology("NEWHOST");
				System.out.println("TLD Server "+myAgent.getLocalName()+" - forwarding host to add to "+result[0].getName().getLocalName()+".");
				this.myAgent.send(proposal);
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
