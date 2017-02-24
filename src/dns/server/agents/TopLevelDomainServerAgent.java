package dns.server.agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;

import dns.server.behaviours.*;
import dns.tables.TLDTable;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class TopLevelDomainServerAgent extends Agent{

	private static final long serialVersionUID = 1814120770306117557L;
	//private int zone;
	
	// Questa tabella contiene i riferimenti ai tld ed i rispettivi dns che li risolvono
	private TLDTable TLDTable = new TLDTable();
	
	@Override
    protected void setup() {
		
        System.out.println("TopLevelDomainServer "+getAID().getLocalName()+" started.");
        
        /*
         * Registrazione al DF...
         */
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd  = new ServiceDescription();
        sd.setType("TLDSERVER");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {  
            DFService.register(this, dfd);
            System.out.println("TopLevelDomainServer "+getAID().getLocalName()+" registered for zone "+getAID().getLocalName().charAt(0)+".");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            System.out.println("!!ERROR!! Registration of TopLevelDomainServer to DF failed! System may not work properly.");
        }
        
        /*
         * Inizializzo la tabella prendendo solo i DNSServer della stessa zona
         * del TopLevelDomainServer in questione (stesso prefisso)
         */
        
        try { 
			BufferedReader br = new BufferedReader(new FileReader("tldhosts.txt"));
	        String line = null;
	        while ((line = br.readLine()) != null) {
	        	/*
	        	 * Prendo quelli che hanno lo stesso prefisso
	        	 */
	        	if (line.split("\\s+")[1].charAt(0)==getAID().getLocalName().charAt(0)) {
	        		TLDTable.addHost(line.split("\\s+")[0], Calendar.getInstance(), line.split("\\s+")[1]);
	        	}
	        }
	        br.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
        
        this.addBehaviour(new TopLevelDomainServerAgent_ResolveName());
        //this.addBehaviour(new TopLevelDomainServerAgent_CoherencePropagation(this, 60000));
        this.addBehaviour(new TopLevelDomainServerAgent_CreateNewHost());
    }	

	/*public int getZone() {
		return zone;
	}*/
	
	public TLDTable getTLDTable() {
		return TLDTable;
	}
	
	public boolean updateTLDTableEntry(String TLD, String address, Calendar timestamp, int position) {
		TLDTable.deleteHost(TLD, address);
		return TLDTable.addHost(TLD, timestamp, address);
	}
}
