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
	
	private MessageTemplate mtResp = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM), 
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
		        	 * 
		        	 * ATTENZIONE: sequenzializzo l'operazione di informare i TLD del nuovo host.
		        	 * Questo dal momento che, nel caso si aggiunga un host con un nuovo TLD nel sistema, 
		        	 * devo dare ai TLD il tempo di propagare le informazioni ai DNS. Se così non fosse, rischierei
		        	 * di avere informazioni replicate all'interno dei DNS e probabilmente tutti verrebbero scelti casualmente
		        	 * come i designati a risolvere quel dato TLD. Per fare questo, uso una blockingReceive
		        	 * temporizzata per assicurarmi che sia avvenuto tutto correttamente. 
		        	 */
		        	for (int i = 0; i < result.length; ++i) {
			        	System.out.println("Root Server - TLD Server "+result[i].getName().getLocalName()+" found. Propagating new host...");
				    	ACLMessage proposal = new ACLMessage(ACLMessage.INFORM);
				    	proposal.setContent(msg.getContent());
				    	proposal.addReceiver(result[i].getName());
				    	proposal.setOntology("NEWHOST");
			    		myAgent.send(proposal);
			    		
			    		/*
			    		 *  Non ho bisogno di leggere il messaggio in quanto la sua semantica
			    		 *  è racchiusa già nella sua forma.
			    		 */
			    		myAgent.blockingReceive(mtResp, 4000);
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
