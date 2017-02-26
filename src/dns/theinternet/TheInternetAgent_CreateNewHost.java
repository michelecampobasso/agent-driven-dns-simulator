package dns.theinternet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import helper.HostGenerator;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class TheInternetAgent_CreateNewHost extends TickerBehaviour {

	private static final long serialVersionUID = -5262364798466245344L;

	private ACLMessage inform;
	private DFAgentDescription template;
	private DFAgentDescription templateClient;
	private ServiceDescription sd;
	private ServiceDescription sdClient;
	private SearchConstraints all;
	
	private DFAgentDescription[] rootServerDescriptor;
	private DFAgentDescription[] clientDescriptor;
	
	private String newHost;
	private String newHostAddress;
	
	public TheInternetAgent_CreateNewHost(Agent a, long period) {
		super(a, period);
	}
	
	@Override
	public void onStart() {
		
		template = new DFAgentDescription();
	    sd = new ServiceDescription();
	    sd.setType("ROOTSERVER");
	    template.addServices(sd);
	    
		templateClient = new DFAgentDescription();
	    sdClient = new ServiceDescription();
	    sdClient.setType("CLIENT");
	    templateClient.addServices(sdClient);

	    all = new SearchConstraints();
	    all.setMaxResults(new Long(-1));
	    
	    // Svuoto il file degli host aggiunti dalla sessione precedente
	    BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter("added_hosts.txt"));
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	}

	@Override
	protected void onTick() {

		if (rootServerDescriptor == null || rootServerDescriptor.length==0) {
			try {
				rootServerDescriptor = DFService.search(myAgent, template, all);
			} catch (final FIPAException fe) {
				fe.printStackTrace();
			}
		}
	
	    /*
	     * Se ho trovato il RootSever, allora posso richiedere la risoluzione del nome dell'host.
	     */
	    if (rootServerDescriptor != null && rootServerDescriptor.length != 0) {
	    	System.out.println("The Internet - found RootServer "+rootServerDescriptor[0].getName().getLocalName());
	    	
	    	/*
	    	 * Creazione di un nuovo host ed invio informazione al RootServer
	    	 */
	    	newHost = HostGenerator.generateHostName();
	    	newHostAddress = HostGenerator.generateHostAddress();
	    	inform = new ACLMessage(ACLMessage.INFORM);
	    	inform.addReceiver(rootServerDescriptor[0].getName());
	    	inform.setOntology("NEWHOST");
    		inform.setContent(newHost+" "+newHostAddress);
    		System.out.println("The Internet - sending host "+ inform.getContent() + " to add to " + rootServerDescriptor[0].getName().getLocalName() + "..." );
    		this.myAgent.send(inform);
    	
	    
		    /*
		     * Scrivo sul file degli hosts il nuovo host aggiunto alla rete.
		     */
		    
			BufferedWriter bw;
			try {
				bw = new BufferedWriter(new FileWriter("added_hosts.txt", true));
				bw.append(newHost+" "+newHostAddress);
				bw.newLine();
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		    /*
		     * Informo i client attivi del nuovo host aggiunto.
		     */
		    try {
				clientDescriptor = DFService.search(myAgent, templateClient, all);
			} catch (final FIPAException fe) {
				fe.printStackTrace();
			}
		    
		    if (clientDescriptor != null && clientDescriptor.length != 0) {
		    	System.out.println("The Internet - found Client(s) to inform about the new host...");
		    	
		    	/*
		    	 * Creazione di un nuovo host ed invio ai client attivi
		    	 */
		    	for (int i=0; i<clientDescriptor.length; i++) {
			    	inform = new ACLMessage(ACLMessage.INFORM);
			    	inform.addReceiver(clientDescriptor[i].getName());
			    	inform.setOntology("NEWHOST");
			    	inform.setContent(newHost);
		    		System.out.println("The Internet - sending host "+ newHost + " to add to " + clientDescriptor[i].getName().getLocalName() + "..." );
		    		this.myAgent.send(inform);
		    	}
	    	}
	    }
	}
}