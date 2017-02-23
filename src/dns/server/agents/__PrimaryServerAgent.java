package dns.server.agents;


import java.util.Calendar;

import dns.tables.Host;
import dns.tables.PrimaryTable;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class __PrimaryServerAgent extends Agent {

	private static final long serialVersionUID = 1L;
	private static final String hostsFile = "connectedHosts.txt";
	
	// Contenente gli indirizzi di chi risolve i domini di secondo livello in base ai domini di primo livello
	private PrimaryTable primaryTable; 
	
	// Contenente gli indirizzi che sono stati indicizzati
	private Host connectedHosts;
	
	private class ServerRequestHandlerBehaviour extends Behaviour {

		private static final long serialVersionUID = 1L;
		// We only want those requests that come from hosts.
		private final MessageTemplate requestTemplate = MessageTemplate.and(
	            		MessageTemplate.MatchOntology("host-request"), 
	            		MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

	    @Override
	    public void action() {

	        ServerRequestHandlerBehaviour.this.log("Waiting for REQUEST messages from hosts...");
	        final ACLMessage msg = this.myAgent.receive(this.requestTemplate);
	        if (msg != null) {
	        	// Request received
	        	ServerRequestHandlerBehaviour.this.log("Received REQUEST '" + msg.getReplyWith()
	                    + "' from '" + msg.getSender().getName() + "'.");
	        	
	        	// Resolving second level resolver
	        	final String hostToResolve = msg.getContent();
	        	final String firstLevelDomain = hostToResolve.split(".")[2];
	        	AID[] secondLevelDomainResolver = primaryTable.table.get(firstLevelDomain);
	        	
	        	// Creating the request 
	        	if (!secondLevelDomainResolver.equals(null)) {
	        		ServerRequestHandlerBehaviour.this.log("Found the second level domain(s)...");
	        		final ACLMessage secondLevel = new ACLMessage(ACLMessage.REQUEST);
	        		secondLevel.setSender(msg.getSender());
	        		secondLevel.setContent(hostToResolve);
	        		secondLevel.setConversationId(msg.getConversationId());
	        		secondLevel.setOntology(msg.getOntology());
	        		
	        		// Broadcasting to all the possibile secondary level servers
	        		for (final AID receiver : secondLevelDomainResolver)
	        			secondLevel.addReceiver(receiver);
	        		
	        		// Sending the request...
	        		ServerRequestHandlerBehaviour.this.log("Sending the request...");
	        		this.myAgent.send(secondLevel);
	        	}
	        	else {
	        		ServerRequestHandlerBehaviour.this.log("No second level domain found! Failed...");
	        		final ACLMessage reply = msg.createReply();
	        		reply.setPerformative(ACLMessage.REFUSE);
	        		reply.setContent("not-found");
	        	}
	        }

		}

		@Override
		public boolean done() {
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
	
	private class ServerInitializationBehaviour extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;
		private final MessageTemplate peerTableTemplateMessage = MessageTemplate.and(
	            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
	            MessageTemplate.MatchOntology("table-init"));

		/* 
		 * La action attende in totale 10 secondi per assicurarsi di ottenere la tabella corretta
		 * anche da host lontani o sovraccarichi. La sincronia di tale behaviour non ci preoccupa
		 * in quanto è assimilabile al tempo che il componente entri a far parte del sistema in maniera
		 * coerente ed integrata.
		 */
		
	    @Override
	    public void action() {
	    	
	    	Calendar startTime = Calendar.getInstance();
	    	Calendar endTime = startTime;
	    	endTime.add(Calendar.SECOND, 10);
	    	
	    	ServerInitializationBehaviour.this.log("Waiting for INFORM messages from peers...");
	        
	    	while (endTime.after(Calendar.getInstance())) {
		    	final ACLMessage msg = this.myAgent.receive(this.peerTableTemplateMessage);
		        if (msg != null) {
		        	// Table received
		        	ServerInitializationBehaviour.this.log("Received INFORM '" + msg.getReplyWith()
		                    + "' from '" + msg.getSender().getName() + "'.");
		        	try {
						primaryTable = (PrimaryTable) msg.getContentObject();
					} catch (UnreadableException e) {
						System.out.println(e.getMessage());
						ServerInitializationBehaviour.this.log("!Error while attempting to read primaryTable '" + msg.getReplyWith()
	                    + "' from '" + msg.getSender().getName() + "'.");
					}

		        	/* Tuttavia non riesco a gestire tutte le casistiche dovute alla contemporaneità...
		        	 * Assumiamo che sia gestito il caso in cui un nuovo server si sia appena iscritto, ne sia stato
		        	 * dismesso uno ed aggiunto un altro in un lasso di tempo talmente basso da non aver consentito
		        	 * la propagazione delle due informazioni per tempo.
		        	 * A ha rimosso dalla sua lista un server dismesso a t0, B ha aggiunto alla sua lista un server
		        	 * a t1 ma ancora non ha elaborato la rimozione del vecchio server, C che è il nuovo server ha ricevuto
		        	 * già la tabella da A e la sostituirà nel momento in cui riceverà quella più aggiornata da B che pur
		        	 * non contiene la rimozione del vecchio server.
		        	 */
		        }
	    	}

		}

		@Override
		public void onStart() {
			
			// Begin asking for the table
	    	final ACLMessage getTablesRequestMessage = new ACLMessage(ACLMessage.REQUEST);
	    	for (final AID primaryServer : __PrimaryServerAgent.this.primaryServers) {
                __PrimaryServerAgent.this.log("Sending REQUEST for primary DNS table to agent '"
                        + primaryServer.getName() + "'...");
                getTablesRequestMessage.addReceiver(primaryServer);
	    	}
	    	
	    	getTablesRequestMessage.setOntology("primary-table-request");
	    	this.myAgent.send(getTablesRequestMessage);
	    	
		}
		
		@Override
		public int onEnd() {
			return 0;
		}
		
	    private void log(final String msg) {
	        System.out.println("[" + this.getAgent().getName() + "]: " + msg);
	    }
	}

	// TODO
	//private class TickerBehaviour extends Ticker
	//questa classe dovrà controllare periodicamente la lista dei server primari e secondari
	//per ora, la lista la metto qui nell'agente stesso, ma dovrà essere manipolata da questo behaviour
	    
	private AID[] primaryServers; // NON PUO' ESSERE POPOLATA ALL'AVVIO DEL SISTEMA!!
	
	@Override
    protected void setup() {

	    this.log("I'm started.");
	    this.primaryTable = new PrimaryTable();
	    
	    //Sarà il caso di inizializzare qualcuno? O il bello è proprio questo, che non devo farlo?
	    //this.bootCatalogue();
	    //this.printCatalogue();
	    
	    // Registration of the Agent
	    final DFAgentDescription dfd = new DFAgentDescription();
	    dfd.setName(this.getAID());

	    // Filling Service description
	    final ServiceDescription sd = new ServiceDescription();
	    sd.setType("primary-dns");
	    sd.setName("JADE-primary-dns");
	    
	    // Agent - Service coupling
	    dfd.addServices(sd);
	    try {
	    	// Register the service on the DF
	    	this.log("Registering '" + sd.getType() + "' service named '"
	                + sd.getName() + "'" + "to the default DF...");
	        DFService.register(this, dfd);
	    } catch (final FIPAException fe) {
	        fe.printStackTrace();
	    }
	}
	
	private void log(final String msg) {
        System.out.println("[" + this.getName() + "]: " + msg);
    }
	
	//IS THIS A TICKER?
	private class NetScanner extends Behaviour {
	
		@Override
		public void onStart() {
			// Con questo metodo io devo risolvere quali sono gli AID[] designati a risolvere
			// i domini di secondo livello
			// Classe per mantenere queste informazioni?
		}
		
		@Override
		public void action() {
		/*	List<String> hostList = new ArrayList<String>();
			try {
				 hostList = Files.readAllLines(Paths.get(hostsFile));
			} catch (IOException e) {
				e.getMessage();
			}
			String host[] = (String[]) hostList.toArray();
			for (int i=0; i<host.length; i++) {
				// Check se questo host non è ancora stato mappato nel sistema
				String hostName = host[i].split(" ")[0];
				String hostAddress = host[i].split(" ")[1];
				if (connectedHosts.getAddressByName(hostName).equals(null)) {
					// L'host non è stato ancora indicizzato
					// Assumo che il dominio di primo livello esiste (.it)
					AID[] secondLevelResolvers = primaryTable.getSecondLevelResolvers(hostName.split(".")[1]);
					// TODO manda hostname e hostAddress ai resolver di secondo livello
					connectedHosts.addServer(connectedHosts.new Entry(hostName, hostAddress));
				}
			}*/
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
