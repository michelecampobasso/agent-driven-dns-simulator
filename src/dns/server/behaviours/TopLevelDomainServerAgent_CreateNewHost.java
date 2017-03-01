package dns.server.behaviours;

import java.io.IOException;
import java.util.ArrayList;
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
			ArrayList<String> chosenDNSs = new ArrayList<String>();
			System.out.println(myAgent.getAID().getLocalName());
			table.toPrint();
			
			/* 
			 * Flag: stanno venendo assegnati dei TLD a dei DNS. Per consistenza, segnalo che questa operazione 
			 * contiene delle modifiche alla mappatura di ciò che i DNS risolvono e di conseguenza avviso gli altri pari.
			 */
			boolean responsibilyChanged = false;
			
			/*
			 * Non ci sono DNS che si occupino di quel TLD in zona, dunque prendo tutti i DNS di zona e
			 * ne assegno metà (arrotondata all'intero superiore se dispari) alla risoluzione di quel TLD.
			 */
			if (DNSServerAddresses == null || DNSServerAddresses.size() == 0) {
				responsibilyChanged = true;
				DNSServerAddresses = table.getAllAddressesByZone(myAgent.getAID().getLocalName().charAt(0));
				int DNSQuantity = DNSServerAddresses.size()%2 == 0 ? DNSServerAddresses.size()/2 : (DNSServerAddresses.size()/2)+1;
				Collections.shuffle(DNSServerAddresses);
				
				for (int i=0; i<DNSQuantity; i++) {
					if (!table.addHost(TLD, DNSServerAddresses.get(i)))
						System.out.println("TLD Server "+myAgent.getLocalName()+" - error while adding the host!");
					else {
						System.out.println("TLD Server "+myAgent.getLocalName()+" - new TLD, adding it and picking "+DNSServerAddresses.get(i)+" as responsable.");
						chosenDNSs.add(DNSServerAddresses.get(i));
					}
				}
			}
			else {
				/*
				 * Se ne ho almeno uno, mi assicuro che siano sufficienti secondo il principio di 
				 * almeno metà DNS (+1 se dispari) in zona.
				 */
				chosenDNSs = DNSServerAddresses; //Copio intanto quelli che ho
				ArrayList<String> allDNSinZone = table.getAllAddressesByZone(myAgent.getAID().getLocalName().charAt(0));
				int DNSQuantity = allDNSinZone.size()%2 == 0 ? allDNSinZone.size()/2 : (allDNSinZone.size()/2)+1;
				/*
				 * Non ci sono DNS sufficienti che si occupano di risolvere quel TLD, 
				 * li aggiungo casualmente dal pool di tutti i DNS della zona. 
				 */
				if (DNSQuantity > chosenDNSs.size()) {
					responsibilyChanged = true;
					Collections.shuffle(allDNSinZone);
					for (int i = 0; chosenDNSs.size() < DNSQuantity; i++) {
						if (!table.addHost(TLD, allDNSinZone.get(i)))
							System.out.println("TLD Server "+myAgent.getLocalName()+" - error while adding the host!");
						else {
							System.out.println("TLD Server "+myAgent.getLocalName()+" - new TLD, adding it and picking "+allDNSinZone.get(i)+" as responsable.");
							chosenDNSs.add(allDNSinZone.get(i));
						}
					}
				}
			}
			/*
			 * Itero su tutti i DNS deputati ad avere l'informazione sul nuovo host:
			 * prima ottengo l'indirizzo dal DF...
			 */
			
			for (int i = 0; i<chosenDNSs.size(); i++) {
				template = new DFAgentDescription();
			    sd = new ServiceDescription();
			    sd.setType("DNSSERVER");
			    sd.setName(chosenDNSs.get(i));
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
					myAgent.send(proposal);
				}
			}
			
			/*
			 * Se c'è stato un cambiamento di ciò che un DNS può risolvere, mando ai pari la lista 
			 * dei DNS a cui è stato propagato il nuovo host in modo da non creare richieste duplicate.
			 */
			
			if (responsibilyChanged) {
				template = new DFAgentDescription();
			    sd = new ServiceDescription();
			    sd.setType("TLDSERVER");
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
				 * Invio a tutti i TLD (tranne me) la lista dei DNS che risolvono il TLD specificato come ConversationId
				 */
				if (result != null) {
					for (int i = 0; i<result.length; i++) {
						if (!result[i].getName().getLocalName().equalsIgnoreCase(myAgent.getLocalName())) {
							try {
								ACLMessage inform = new ACLMessage(ACLMessage.PROPAGATE);
								inform.addReceiver(result[i].getName());
								inform.setOntology("NEWTLD");
								inform.setContentObject(chosenDNSs);
								inform.setConversationId(msg.getContent().split("\\s+")[0].split("\\.")[1]);
								myAgent.send(inform);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			
			/* 
			 * Finito di ciclare e di designare i DNS che risolvono quel TLD, posso avvisare
			 * il RootServer che può andare avanti.
			 */
			
			ACLMessage done = new ACLMessage(ACLMessage.CONFIRM);
			done.setOntology("NEWHOST");
			done.addReceiver(msg.getSender());
			myAgent.send(done);
		}

		else
			block();

	}

	@Override
	public boolean done() {
		return false;
	}

}
