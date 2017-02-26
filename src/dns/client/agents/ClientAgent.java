package dns.client.agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import dns.client.behaviours.*;
import dns.tables.TLDLatencyTable;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class ClientAgent extends Agent {

	private static final long serialVersionUID = 271669436955865796L;
	private ArrayList<String> hosts = new ArrayList<String>();
	public TLDLatencyTable closestTLDs;
	
    @Override
    protected void setup() {
    	
        System.out.println("Client started.");
        
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
            System.out.println("!!ERROR!! Registration of Client to DF failed! System may not work properly.");
        }
        
     // Carico gli hosts...
 		try { 
 			BufferedReader br = new BufferedReader(new FileReader("hosts.txt"));
 	        String line = null;
 	        while ((line = br.readLine()) != null)
 	        	hosts.add(line);
 	        br.close();
 	        // anche quelli aggiunti se questo agente sta venendo avviato in un secondo momento
 	        br = new BufferedReader(new FileReader("added_hosts.txt"));
 	        line = null;
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
        
        this.addBehaviour(new ClientAgent_ResolveName(this, 5000)); //This can run only when the other behaviour has completed TODO implement delays
        this.addBehaviour(new ClientAgent_GetNewHost());
        //this.addBehaviour(new ClientAgent_GetTLDs(this, 10000));
    }
    
    @Override
	public void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			System.out.println("Problems while deregistering the Client "+getAID().getLocalName()+". System may not work properly.");
			e.printStackTrace();
		}
	}
    
    
    public void setTLDs(TLDLatencyTable t) {
    	closestTLDs = t;
    }
    
    public ArrayList<String> getAllHosts() {
    	return hosts;
    }
    
    public void addHost(String hostName) {
    	hosts.add(hostName);
    }
	
}
