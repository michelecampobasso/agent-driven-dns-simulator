package dns.client.agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import dns.client.behaviours.*;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class ClientAgent extends Agent {

	private static final long serialVersionUID = 271669436955865796L;
	private ArrayList<String> hosts = new ArrayList<String>();
	
    @Override
    protected void setup() {
        
        // Carico gli hosts...
 		try { 
 			BufferedReader br = new BufferedReader(new FileReader("hosts.txt"));
 	        String line = null;
 	        while ((line = br.readLine()) != null)
 	        	hosts.add(line);
 	        br.close();
 	        // ...anche quelli aggiunti se questo agente sta venendo avviato in un secondo momento
 	        br = new BufferedReader(new FileReader("added_hosts.txt"));
 	        line = null;
 	        while ((line = br.readLine()) != null)
 	        	hosts.add(line.split("\\s+")[0]);
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
