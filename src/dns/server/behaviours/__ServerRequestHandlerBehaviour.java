package dns.server.behaviours;

import java.util.HashMap;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class __ServerRequestHandlerBehaviour extends Behaviour {

	private static final long serialVersionUID = 1L;

	private final MessageTemplate mt = MessageTemplate
            .MatchPerformative(ACLMessage.REQUEST);
	
	private HashMap<String, AID[]> addressTable = new HashMap<String, AID[]>();
	
    

    @Override
    public void action() {

        __ServerRequestHandlerBehaviour.this.log("Waiting for REQUEST messages from hosts...");
        final ACLMessage msg = this.myAgent.receive(this.mt);
        if (msg != null) {
        	// Request received
        	__ServerRequestHandlerBehaviour.this.log("Received REQUEST '" + msg.getReplyWith()
                    + "' from '" + msg.getSender().getName() + "'.");
        	
        	// Resolving second level resolver
        	final String hostToResolve = msg.getContent();
        	final String firstLevelDomain = hostToResolve.split(".")[2];
        	AID[] secondLevelDomainResolver = addressTable.get(firstLevelDomain);
        	
        	// Creating the request 
        	if (!secondLevelDomainResolver.equals(null)) {
        		__ServerRequestHandlerBehaviour.this.log("Found the second level domain(s)...");
        		final ACLMessage secondLevel = new ACLMessage(ACLMessage.REQUEST);
        		secondLevel.setSender(msg.getSender());
        		secondLevel.setContent(hostToResolve);
        		secondLevel.setConversationId(msg.getConversationId());
        		secondLevel.setOntology(msg.getOntology());
        		
        		// Broadcasting to all the possibile secondary level servers
        		for (final AID receiver : secondLevelDomainResolver)
        			secondLevel.addReceiver(receiver);
        		
        		// Sending the request...
        		__ServerRequestHandlerBehaviour.this.log("Sending the request...");
        		this.myAgent.send(secondLevel);
        	}
        	else {
        		__ServerRequestHandlerBehaviour.this.log("No second level domain found! Failed...");
        		final ACLMessage reply = msg.createReply();
        		reply.setPerformative(ACLMessage.REFUSE);
        		reply.setContent("not-found");
        	}
        }

	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
		
		/* Gestirà una richiesta da parte di un dummy agent che gli dirà
		 * di terminare. Prima di interrompersi, dovrà assicurarsi di aver inviato
		 * le informazioni circa gli ultimi host che ha inserito e che potrebbero non essere
		 * state propagate correttamente.
		 */
	}
	
	@Override
	public void onStart() {
		/* Performa una richiesta verso i pari al fine di ottenere le tabelle del sistema di nomi complete. 
		 * Una volta ottenute le informazioni necessarie, il metodo termina ed il behaviour entra in 
		 * listening mode.
		 */
	}
	
	@Override
	public int onEnd() {
		
		return 0;
	}
	
    private void log(final String msg) {
        System.out.println("[" + this.getAgent().getName() + "]: " + msg);
    }


}
