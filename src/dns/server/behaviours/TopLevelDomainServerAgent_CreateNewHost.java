package dns.server.behaviours;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

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
			System.out.println("TLD Server "+myAgent.getLocalName()+" - received msg to add a new host on the system.");
			System.out.println("TLD Server "+myAgent.getLocalName()+" - the host's name is "+msg.getContent().split("\\s+")[0]+" and address is: "+msg.getContent().split("\\s+")[1]);
			
			/*
	    	 * Questo behaviour viene lanciato post inizializzazione, quindi non devo cercare
	    	 * nuovamente i DNS ma utilizzo quelle che sono presenti nell'agente. 
	    	 * Ci sono due casi:
	    	 * 		1) il TLD non contiene il riferimento e deve scegliere dei DNS che si occuperanno di risolverlo;
	    	 * 		2) il TLD contiene già dei riferimenti a dei DNS che risolvono quel particolare TLD;
	    	 * 			2a) se non ce ne sono abbastanza, l'informazione viene replicata su più DNS.
	    	 */
			
			TLDTable table = ((TopLevelDomainServerAgent)myAgent).getTLDTable();
			
			String TLD = msg.getContent().split("\\s+")[0].split("\\.")[1];
			ArrayList<String> DNSServerAddresses = table.getAddressesFromTLDByZone(TLD, myAgent.getAID().getLocalName().charAt(0));
			
			/*
			 * Non ci sono DNS che si occupino di quel TLD in zona, dunque prendo tutti i DNS di zona e
			 * ne assegno metà (arrotondata all'intero superiore se dispari) alla risoluzione di quel TLD.
			 */
			if (DNSServerAddresses == null || DNSServerAddresses.size() == 0) {
				DNSServerAddresses = table.getAllAddressesByZone(myAgent.getAID().getLocalName().charAt(0));
				int DNSQuantity = DNSServerAddresses.size()%2 == 0 ? DNSServerAddresses.size()/2 : (DNSServerAddresses.size()/2)+1;
				Collections.shuffle(DNSServerAddresses);
				
				for (int i=0; i<DNSQuantity; i++) {
					if (!table.addHost(TLD, Calendar.getInstance(), DNSServerAddresses.get(i)))
						System.out.println("TLD Server "+myAgent.getLocalName()+" - error while adding the host!");
					System.out.println("TLD Server "+myAgent.getLocalName()+" - new TLD, adding it and picking "+DNSServerAddresses.get(i)+" as responsable.");
				}
			}
			/*
			 * Se ne ho almeno uno, mi assicuro che siano sufficienti secondo il principio di 
			 * almeno metà DNS (+1 se dispari) in zona.
			 */
			ArrayList<String> allDNSinZone = table.getAllAddressesByZone(myAgent.getAID().getLocalName().charAt(0));
			int DNSQuantity = allDNSinZone.size()%2 == 0 ? allDNSinZone.size()/2 : (allDNSinZone.size()/2)+1;
			/*
			 * Non ci sono DNS sufficienti che si occupano di risolvere quel TLD, 
			 * li aggiungo casualmente dal pool di tutti i DNS della zona. 
			 */
			if (DNSQuantity > allDNSinZone.size()) {
				Collections.shuffle(allDNSinZone);
				int i = 0;
				while (DNSQuantity+i-allDNSinZone.size()>0) {
					DNSServerAddresses.add(allDNSinZone.get(i));
					i++;
				}
			}
			
			/*
			 * Itero su tutti i DNS deputati ad avere l'informazione sul nuovo host:
			 * prima ottengo l'indirizzo dal DF...
			 */
			for (int i = 0; i<DNSServerAddresses.size(); i++) {
				template = new DFAgentDescription();
			    sd = new ServiceDescription();
			    sd.setType("DNSSERVER");
			    sd.setName(DNSServerAddresses.get(i));
			    template.addServices(sd);
			    all = new SearchConstraints();
			    all.setMaxResults(new Long(-1));
			    
				DFAgentDescription[] result = null;
				try {
					result = DFService.search(myAgent, template, all);
				} catch (FIPAException e) {
					e.printStackTrace();
				}
		        
				if (result != null) {
				/*
				 *  ...e dopo gli inoltro il nuovo host.
				 */
			    	ACLMessage proposal = new ACLMessage(ACLMessage.INFORM);
			    	proposal.setContent(msg.getContent());
			    	proposal.addReceiver(result[0].getName());
			    	proposal.setOntology("NEWHOST");
					System.out.println("TLD Server "+myAgent.getLocalName()+" - forwarding host to add to "+result[0].getName().getLocalName()+".");
					this.myAgent.send(proposal);
				}
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
