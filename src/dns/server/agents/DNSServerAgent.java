package dns.server.agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import dns.server.behaviours.*;
import dns.tables.Host;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class DNSServerAgent extends Agent {

	private static final long serialVersionUID = 7187628017305918395L;
	
	private ArrayList<Host> hostTable = new ArrayList<Host>();
	
	@Override
    protected void setup() {
		
		System.out.println("DNSServer "+getAID().getLocalName()+" started.");
        
		/*
		 * Registrazione al DF...
		 */
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd  = new ServiceDescription();
        sd.setType("DNSSERVER");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {  
            DFService.register(this, dfd);
            System.out.println("DNSServer "+getAID().getLocalName()+" registered for zone "+getAID().getLocalName().charAt(0)+".");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            System.out.println("!!ERROR!! Registration of DNSServer to DF failed! System may not work properly.");
        }
        
        /*
         * Inizializzazione della tabella contenente gli host che potrà risolvere a seconda
         * del suo indirizzo e della sua zona (i DNSServerAgent hanno una parte degli host risolvibili
         * nella loro zona)
         */
        
        try { 
        	BufferedReader br = new BufferedReader(new FileReader("tldhosts.txt"));
        	String line = null;
        	ArrayList<String> myTLDs = new ArrayList<String>();
        	while ((line = br.readLine()) != null) {
        		/*
        		 * Trovo i TLD di competenza, verificando quali sono deputati a questo server...
        		 */
        		if (line.split("\\s+")[1].equalsIgnoreCase(getAID().getLocalName()))
        			myTLDs.add(line.split("\\s+")[0]);
        	}
	        br.close();
	        
        	br = new BufferedReader(new FileReader("dnshosts.txt"));
	        line = null;
	        while ((line = br.readLine()) != null) {
        		// Anche questa avrà bisogno dei timestamp... TODO
	        	/*
	        	 * ...e carico nella tabella degli host solo quelli con il TLD corrispondente
	        	 */
	        	for (int i = 0; i<myTLDs.size(); i++)
	        		if (line.split("\\s+")[0].split("\\.")[1].equalsIgnoreCase(myTLDs.get(i)))
	        			hostTable.add(new Host(line.split("\\s+")[0], line.split("\\s+")[1]));
	        }
	        br.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
        
        this.addBehaviour(new DNSServerAgent_ResolveName());
        this.addBehaviour(new DNSServerAgent_CreateNewHost());
    }

	public ArrayList<Host> getHostTable() {
		return hostTable;
	}
	
	public boolean addHost(String hostName, Calendar timeStamp, String address) {
		return hostTable.add(new Host(hostName, address));
	}

}
