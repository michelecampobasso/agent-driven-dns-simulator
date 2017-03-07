package dns.client.agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import dns.client.behaviours.*;
import dns.tables.Host;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ClientAgent extends Agent {

	private static final long serialVersionUID = 271669436955865796L;
	private ArrayList<String> hosts = new ArrayList<String>();

	private MessageTemplate mtInternet = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
			MessageTemplate.MatchOntology("ALLHOSTSPLEASE"));
	
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;
	
	
    @SuppressWarnings("unchecked")
	@Override
    protected void setup() {
    	
    	if (getArguments()[0].toString().equalsIgnoreCase("post")) {
    		
    		/*
    		 * Vado alla ricerca dell'agente TheInternet per ottenere la cache di tutti gli hosts conosciuti.
    		 */
    		
    		template = new DFAgentDescription();
		    sd = new ServiceDescription();
		    sd.setType("THEINTERNET");
		    template.addServices(sd);
		    all = new SearchConstraints();
		    all.setMaxResults(new Long(-1));
		    
		    DFAgentDescription[] result = null;
		    ArrayList<Host> cachedHostTable = null;
		    try {
		        result = DFService.search(this, template, all);
		        if (result.length!=0) {
		        	ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
				    request.setOntology("ALLHOSTSPLEASE");
				    request.addReceiver(result[0].getName());
				    send(request);
				    
				    ACLMessage response = blockingReceive(mtInternet, 5000);
				    cachedHostTable = (ArrayList<Host>) response.getContentObject();
		        }
		    } catch (final FIPAException fe) {
		        fe.printStackTrace();
		    } catch (UnreadableException e) {
				e.printStackTrace();
			}
		    
		    /*
		     * Carico nell'agente tutti gli hostnames creati dall'agente TheInternet.
		     */
		    if (cachedHostTable != null)
		    	for (Host h : cachedHostTable)
		    		hosts.add(h.getName());
		
    	}
    	// Carico gli hosts...
 		try { 
 			BufferedReader br = new BufferedReader(new FileReader("hosts.txt"));
 	        String line = null;
 	        while ((line = br.readLine()) != null)
 	        	hosts.add(line);
 	        br.close();
 	        
 		}
 		catch (FileNotFoundException e) {
 			e.printStackTrace();
 		}
 		catch (IOException e) {
 			e.printStackTrace();
 		}
 		/*
		 * Registrazione al DF...
		 */
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd  = new ServiceDescription();
        sd.setType("CLIENT");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {  
            DFService.register(this, dfd);
            System.out.println("Client "+getAID().getLocalName()+" registered for zone "+getAID().getLocalName().charAt(0)+".");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            System.err.println("!!ERROR!! Registration of Client to DF failed! System may not work properly.");
        }
        
        this.addBehaviour(new ClientAgent_ResolveName(this, 5000));
        this.addBehaviour(new ClientAgent_GetNewHost());
    }
    
    @Override
	public void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			System.err.println("Problems while deregistering the Client "+getAID().getLocalName()+". System may not work properly.");
			e.printStackTrace();
		}
	}
    
    public ArrayList<String> getAllHosts() {
    	return hosts;
    }
    
    public void addHost(String hostName) {
    	hosts.add(hostName);
    }
	
}
