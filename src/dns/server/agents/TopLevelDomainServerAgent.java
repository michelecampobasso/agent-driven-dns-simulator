package dns.server.agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import dns.server.behaviours.*;
import dns.tables.TLDTable;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class TopLevelDomainServerAgent extends Agent{

	private static final long serialVersionUID = 1814120770306117557L;
	//private int zone;
	
	private MessageTemplate mtTLD = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
			MessageTemplate.MatchOntology("YOURTABLEPLEASE"));
	private MessageTemplate mtDNS = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
			MessageTemplate.MatchOntology("YOURTLDPLEASE"));
	
	// Questa tabella contiene i riferimenti ai tld ed i rispettivi dns che li risolvono
	private TLDTable TLDTable = null;
	
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;
	
	@SuppressWarnings("unchecked")
	@Override
    protected void setup() {
		
        System.out.println("TopLevelDomainServer "+getAID().getLocalName()+" started.");
        
		if (getArguments()[0].toString().equalsIgnoreCase("post")) {
			/*
			 * Post inizializzazione: il TLD appena creato è stato aggiunto successivamente.
			 * Non deve avvenire la lettura da files in quanto la configurazione del sistema
			 * potrebbe essere cambiata nel mentre.
			 * 
			 * Due casi: 
			 * 		1) Esiste già un TLD in zona, quindi questa è pura replicazione;
			 * 		2) Non esiste un TLD in zona, vanno risolti tutti i subordinati e mappate
			 * 			le loro competenze per TLD.
			 * 
			 */
			template = new DFAgentDescription();
		    sd = new ServiceDescription();
		    sd.setType("TLDSERVER");
		    template.addServices(sd);
		    all = new SearchConstraints();
		    all.setMaxResults(new Long(-1));
		    
		    /*
		     * Comincio col controllare se esiste un pari in zona e, in tal caso,
		     * clono la sua tabella.
		     */
		    DFAgentDescription[] result = null;
		    try {
		        result = DFService.search(this, template, all);
		        if (result.length!=0) {
		        	for (int i = 0; i<result.length; i++) {
		        		// Recupero un TLD della stessa zona
		        		if (result[i].getName().getLocalName().charAt(0)==getAID().getLocalName().charAt(0)) {
		        			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		        			request.setOntology("YOURTABLEPLEASE");
		        			request.addReceiver(result[i].getName());
		        			send(request);
		        			/*
		        			 * Posso permettermi di utilizzare una blockingReceive dal momento che ho chiesto al
		        			 * DF quali sono gli host e sicuramente sono vivi. Tuttavia, ci metto un timeout massimo.
		        			 */
		        			ACLMessage response = blockingReceive(mtTLD, 5000);
		        			if (response != null) {
		        				TLDTable = (TLDTable)response.getContentObject();
		        				break;
		        			}
		        		}
		        	}
		        }
		    } catch (final FIPAException fe) {
		        fe.printStackTrace();
		    } catch (UnreadableException ue) {
		    	ue.printStackTrace();
		    }
		    /*
	         * Se invece non ho trovato un TLD della stessa zona, allora devo interrogare i subordinati
	         * per sapere cosa risolvono e costruire la mia tabella.
	         */
		    if (TLDTable == null) {
		    	
		    	TLDTable = new TLDTable();
		    	
			    template = new DFAgentDescription();
			    sd = new ServiceDescription();
			    sd.setType("DNSSERVER");
			    template.addServices(sd);
			    all = new SearchConstraints();
			    all.setMaxResults(new Long(-1));
			    
			    result = null;
			    try {
			        result = DFService.search(this, template, all);
			        if (result.length!=0) {
			        	for (int i = 0; i<result.length; i++) {
			        		// Recupero TUTTI i DNS
			        		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			        		request.setOntology("YOURTLDPLEASE");
			        		request.addReceiver(result[i].getName());
			        		send(request);
			        			
			        		ACLMessage response = blockingReceive(mtDNS, 5000);
			        		if (response != null) {
			        			ArrayList<String> resolvedTLDs = (ArrayList<String>)response.getContentObject();
			        			for (int j = 0; j<resolvedTLDs.size(); j++) 
			        				TLDTable.addHost(resolvedTLDs.get(j), result[i].getName().getLocalName());
			        		}
			        	}
			        }
			    } catch (final FIPAException fe) {
			        fe.printStackTrace();
			    } catch (UnreadableException ue) {
			    	ue.printStackTrace();
			    }
		    }
        }
        else {
	        /*
	         * Inizializzazione: inizializzo la tabella prendendo solo i DNSServer della stessa zona
	         * del TopLevelDomainServer in questione (stesso prefisso)
	         */
        	TLDTable = new TLDTable();
	        
	        try { 
				BufferedReader br = new BufferedReader(new FileReader("tldhosts.txt"));
		        String line = null;
		        while ((line = br.readLine()) != null) {
		        	/*
		        	 * Aggiungo tutti gli hosts, anche quelli delle altre zone
		        	 */
		        	TLDTable.addHost(line.split("\\s+")[0], line.split("\\s+")[1]);
		        }
		        br.close();
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
        }
        
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
        
        this.addBehaviour(new TopLevelDomainServerAgent_ResolveName());
        this.addBehaviour(new TopLevelDomainServerAgent_CreateNewHost());
        this.addBehaviour(new TopLevelDomainServerAgent_DeletedDNS());
        this.addBehaviour(new TopLevelDomainServerAgent_YourTablePlease());
        this.addBehaviour(new TopLevelDomainServerAgent_NewTLD());
        this.addBehaviour(new TopLevelDomainServerAgent_NewDNS());
    }	

	/*public int getZone() {
		return zone;
	}*/
	
	@Override
	public void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			System.out.println("Problems while deregistering the TLDServer "+getAID().getLocalName()+". System may not work properly.");
			e.printStackTrace();
		}
	}
	
	public TLDTable getTLDTable() {
		return TLDTable;
	}
	
	public boolean updateTLDTableEntry(String TLD, String address, Calendar timestamp, int position) {
		TLDTable.deleteHostByTLD(TLD, address);
		return TLDTable.addHost(TLD, address);
	}
	
	public boolean updateTLDTable(ArrayList<String> hosts, String TLD) {
		ArrayList<String> alreadyPresentDNS = TLDTable.getAddressesFromTLD(TLD);
		boolean added = false;
		for (int i=0; i<hosts.size(); i++)
			if (!alreadyPresentDNS.contains(hosts.get(i))) {
				TLDTable.addHost(TLD, hosts.get(i));
				added = true;
			}
		return added;
	}
	
	public void addTLDsForHost(ArrayList<String> TLDs, String host) {
		for (int i = 0; i<TLDs.size(); i++)
			TLDTable.addHost(TLDs.get(i), host);
	}
}
