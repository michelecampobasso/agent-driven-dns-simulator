package dns.server.behaviours;

import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RootServer_CreateNewHost extends Behaviour {

	private static final long serialVersionUID = 622439419499729724L;
	
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
			MessageTemplate.MatchOntology("NEWHOST"));

	private ACLMessage msg;
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;
	
	@Override
	public void onStart() {
		
		template = new DFAgentDescription();
	    sd = new ServiceDescription();
	    sd.setType("TLDSERVER");
	    template.addServices(sd);
	    all = new SearchConstraints();
	    all.setMaxResults(new Long(-1));
	}
	
	@Override
	public void action() {
		
		msg = myAgent.receive(mt);
		if (msg != null) {
			System.out.println("RootServer - received msg to add a new host on the system.");
			System.out.println("RootServer - the host's name is "+msg.getContent().split("\\s+")[0]+" and address is: "+msg.getContent().split("\\s+")[1]);
			
			/*
	    	 * Il RootServer richiede ogni volta quali sono i TLDServer.
	    	 */
			
		    DFAgentDescription[] result = null;
		    try {
		        result = DFService.search(myAgent, template, all);
		        if (result.length!=0) {
		        	/*
		        	 * Invio il nuovo host ad ogni TLD...
		        	 */
		        	for (int i = 0; i < result.length; ++i) {
			        	System.out.println("Root Server - TLD Server "+result[i].getName().getLocalName()+" found. Propagating new host...");
				    	ACLMessage proposal = new ACLMessage(ACLMessage.INFORM);
				    	proposal.setContent(msg.getContent());
				    	proposal.addReceiver(result[i].getName());
				    	proposal.setOntology("NEWHOST");
			    		this.myAgent.send(proposal);
			        }
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
		return false;
	}

}
