package dns.client.behaviours;

import dns.client.agents.ClientAgent;
import dns.tables.TLDLatencyTable;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ClientAgent_GetTLDs extends TickerBehaviour {

	public ClientAgent_GetTLDs(Agent a, long period) {
		super(a, period);
	}

	private static final long serialVersionUID = -4022720263602880658L;

	// closestTLDs must be visible to the whole agent...
	private TLDLatencyTable closestTLDs;
	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
			MessageTemplate.MatchOntology("CLOSESTTLD"));
	private boolean pendingRequest = false;
	private ACLMessage request;
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;

	@Override
	public void onStart() {

		request = new ACLMessage(ACLMessage.REQUEST);
		template = new DFAgentDescription();
		sd = new ServiceDescription();
		sd.setType("ROOTSERVER");
		template.addServices(sd);
		all = new SearchConstraints();
		all.setMaxResults(new Long(-1));
	}

	@Override
	protected void onTick() {

		if (!pendingRequest) {

			DFAgentDescription[] result = null;
			try {
				result = DFService.search(myAgent, template, all);
				AID[] RootServer = new AID[result.length];
				for (int i = 0; i < result.length; ++i) {
					RootServer[i] = result[i].getName();
					System.out.println("Client - found RootServer " + RootServer[i].getName());
				}
			} catch (final FIPAException fe) {
				fe.printStackTrace();
			}

			if (result != null && result.length != 0) {
				request.addReceiver(result[0].getName());
				request.setOntology("CLOSESTTLD");
				System.out
						.println("Sending request to " + result[0].getName().getName() + " to know the closest TLD...");
				this.myAgent.send(request);
				pendingRequest = true;
			} else
				System.err.println("No suitable services found, retrying in 10 seconds...");
		}

		// pendingRequest == true
		else {
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println("Client - received infos about closest TLD. Reading...");

				try {
					closestTLDs = (TLDLatencyTable) msg.getContentObject();
				} catch (UnreadableException e) {
					if (msg.getContent().equals(null))
						closestTLDs = null;
					System.err.println("!!ERROR!! TLDLatencyTable not received...");
				}

				System.out.println("Client - closest TLD received.");
				this.stop();
			} else
				block();
		}
	}

	@Override
	public int onEnd() {

		((ClientAgent) myAgent).setTLDs(closestTLDs);
		return 0;
	}

}
