package dns.server.behaviours;

import java.util.ArrayList;

import dns.server.agents.DNSServerAgent;
import dns.tables.Host;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class DNSServerAgent_ResolveName extends Behaviour {

	private static final long serialVersionUID = 3886042025785546954L;
	
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
			MessageTemplate.MatchOntology("RESOLVE"));
	private MessageTemplate infoMT = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
			MessageTemplate.MatchOntology("NEEDINFO"));

	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;
	
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
	   		 * Nel caso in cui a questo DNS sia stato deputato il ruolo di risolvere gli host con un dato TLD
	   		 * e questo non possegga le informazioni per risolvere tale host, allora dovr� chiedere ai suoi pari
	   		 * l'indirizzo di quell'host.
	   		 */
	   		if (hostAddress == "") {
	   			System.out.println("DNS Server "+myAgent.getLocalName()+ " - missing "+msg.getContent()+"'s address, querying to peers...");
		   		template = new DFAgentDescription();
			    sd = new ServiceDescription();
			    sd.setType("DNSSERVER");
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
					for (int i = 0; i<result.length; i++) {
						if (!result[i].getName().getLocalName().equalsIgnoreCase(myAgent.getLocalName())) {
							ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
							request.setContent(msg.getContent());
							request.addReceiver(result[i].getName());
							request.setOntology("NEEDINFO");
							myAgent.send(request);
							/*
							 * Si fa uso di una blockingReceive in quanto si � certi che i DNS ottenuti dal DF 
							 * sono sicuramente vivi.
							 */
							ACLMessage hostInfo = myAgent.blockingReceive(infoMT, 10000);
							// Il DNS contattato ha le informazioni di risoluzione di quel dato host:
							if (hostInfo != null && !hostInfo.getContent().equalsIgnoreCase("noinfo")) {
								hostAddress = hostInfo.getContent();
								hostTable.add(new Host(msg.getContent(), hostAddress));
								System.out.println("DNS Server "+myAgent.getLocalName()+ " - "+msg.getContent()+"'s address found.");
								break;
							}
						}
					}
				}
	   		}
	   		
	   		/*
	   		 * Inoltro la risposta al client. Nota che il receiver � sempre stato il sender,
	   		 * in quanto ad ogni passaggio � stato aggiornato il sender con quello del client.
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
		return false;
	}

}
