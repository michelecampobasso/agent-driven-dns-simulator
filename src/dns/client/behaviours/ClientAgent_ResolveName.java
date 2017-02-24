package dns.client.behaviours;

import java.util.ArrayList;
import java.util.Random;

import dns.client.agents.ClientAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ClientAgent_ResolveName extends TickerBehaviour {

	private static final long serialVersionUID = 5628523830884886480L;
	
	private String hostToResolve;
	private ACLMessage request;
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;
	private boolean pendingRequest;
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
			MessageTemplate.MatchOntology("RESOLVE"));
	private DFAgentDescription[] rootServerDescriptor;
	
	
	public ClientAgent_ResolveName(Agent a, long period) {
		super(a, period);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onStart() {
		
		template = new DFAgentDescription();
	    sd = new ServiceDescription();
	    sd.setType("ROOTSERVER");
	    template.addServices(sd);
	    all = new SearchConstraints();
	    all.setMaxResults(new Long(-1));
	}
	

	@Override
	protected void onTick() {
		
		/*
		 * Periodicamente il Client, lancia una richiesta verso il RootServer per risolvere 
		 * l'indirizzo di un host. Se non conosce il RootServer (non cambia mai, unico - supponiamo
		 * essere dato dal provider), allora lancia una ricerca sul DF.
		 */
		if (!pendingRequest) {
			request = new ACLMessage(ACLMessage.REQUEST);
			if (rootServerDescriptor == null || rootServerDescriptor.length==0) {
				try {
					rootServerDescriptor = DFService.search(myAgent, template, all);
				} catch (final FIPAException fe) {
					fe.printStackTrace();
				}
			}
		
		    /*
		     * Se ho trovato il RootSever e sono stati caricati tutti gli host, 
		     * allora posso richiedere la risoluzione del nome dell'host.
		     */
			ArrayList<String> hosts = ((ClientAgent)myAgent).getAllHosts();
		    if (rootServerDescriptor != null && rootServerDescriptor.length != 0 && hosts.size()!=0) {
		    	System.out.println("Client "+myAgent.getAID().getLocalName()+" - found RootServer "+rootServerDescriptor[0].getName().getLocalName());
		    	hostToResolve = hosts.get(new Random().nextInt(hosts.size()));
	    		request.addReceiver(rootServerDescriptor[0].getName());
	    		request.setOntology("RESOLVE");
	    		request.setContent(hostToResolve);
	    		System.out.println("Client "+myAgent.getAID().getLocalName()+" - sending request to " + rootServerDescriptor[0].getName().getLocalName() + " to resolve " + hostToResolve+"..." );
	    		this.myAgent.send(request);
	    		pendingRequest=true;
	    	} /*else
		        System.out.println("No suitable services found, retrying in 10 seconds...");*/
		}
		/*
		 * Se ho gi� lanciato una richiesta, il client attende la ricezione una risposta.
		 * TODO: timeout massimo?
		 */
		else {
			ACLMessage msg = myAgent.receive(mt);
    		if (msg != null) {
    			System.out.println("Client - address of host "+hostToResolve+" is "+msg.getContent()+".");
		    	pendingRequest=false;
    		}
    		else
				block();
		}
		
	}

}
