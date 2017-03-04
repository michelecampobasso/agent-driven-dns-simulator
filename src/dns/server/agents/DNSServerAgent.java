package dns.server.agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import dns.server.behaviours.*;
import dns.tables.Host;
import helper.ArrayListUtils;
import helper.HashGenerator;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class DNSServerAgent extends Agent {

	private static final long serialVersionUID = 7187628017305918395L;
	
	private ArrayList<Host> hostTable = new ArrayList<Host>();
	private ArrayList<Host> cachedHostTable;
	
	private DFAgentDescription template;
	private ServiceDescription sd;
	private SearchConstraints all;
	
	private MessageTemplate mtInternet = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
			MessageTemplate.MatchOntology("ALLHOSTSPLEASE"));
	private MessageTemplate mtDNS = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
			MessageTemplate.MatchOntology("DNSHOSTS"));
	
	@SuppressWarnings("unchecked")
	@Override
    protected void setup() {
		
		System.out.println("DNSServer "+getAID().getLocalName()+" started.");
		
		if (getArguments()[0].toString().equalsIgnoreCase("post")) {
			/*
			 * Post inizializzazione: il DNS appena creato è stato aggiunto successivamente.
			 * Generalmente, non deve avvenire la lettura da files in quanto la configurazione del sistema
			 * potrebbe essere cambiata nel mentre.
			 * 
			 * Quattro casi: 
			 * 		Non esistono DNS in zona:
			 * 			1)  chiedo ad un DNS dell'altra zona di farmi dare la sua tabella: questo comportamento 
			 * 				è dovuto al fatto che se si prendesse tutti i nomi di tutti i DNS dell'altra zona 
			 * 				sarebbe solamente un throughput in più per la rete,	dal momento che comunque uno dei due 
			 * 				avrà il carico doppio (attuale e nuovo); la scelta del DNS da clonare è data dal conteggio
			 * 				di quali ce ne sono meno (poca ridondanza a discapito di più ridondanza);
			 * 			2)  non esiste un DNS neanche nell'altra zona: si ricorre alla cache creata dall'agente
			 * 				TheInternet (si suppone che se il sistema va in crash completo abbia possibilità di ritornare
			 * 				a funzionare) e ai soliti files di configurazione per ottenere una visione completa degli
			 * 				host in rete;
			 * 		Esistono DNS in zona: ottengo tutti gli host che posso ottenere in zona e controllo 
			 * 		attraverso la cache se sono completi:
			 * 			3)  se sono completi, si tratta di un server di pura ridondanza e clono uno di questi;
			 * 			4)  se non lo sono, risolvo solamente gli host che mancano e li aggiungo.
			 * 
			 * Recap:
			 * 		Esistono DNS in zona: confronto l'unione dei loro dati con la cache internet:
			 * 			1) Se è completo, allora vedo quale server è meno ridondante e lo clono;
			 * 			2) Se non è completo, carico la differenza;
			 * 		Non esistono DNS in zona:
			 * 			3) Se sono completi, clono quello con ridondanza minore;
			 * 			4) Controllo se dall'altra parte i dati sono completi: se non lo sono, carico la differenza;
			 * 			5) Se non ne esistono, instanzio un nuovo DNS con metà degli host. 
			 * 				(se li caricassi tutti, dopo si avrebbero problemi per tutti gli altri casi)
			 * 			
			 * 	In tutti i casi sopra menzonati, avviso i TLD di zona delle nuove competenze.
			 * 			 
			 */
			template = new DFAgentDescription();
		    sd = new ServiceDescription();
		    sd.setType("THEINTERNET");
		    template.addServices(sd);
		    all = new SearchConstraints();
		    all.setMaxResults(new Long(-1));
		    
		    /* 
		     * Innanzitutto ottengo il riferimento all'agente TheInternet...
		     */
		    DFAgentDescription[] result = null;
		    try {
		        result = DFService.search(this, template, all);
		        if (result.length!=0) {
			        /*
				     * ...ed estraggo la lista degli hosts.
				     */
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
		    
		    if (cachedHostTable != null) {
		    	/*
		    	 * Allora possiamo andare.
		    	 * Controllo la presenza o meno di DNS in zona.
		    	 */
		    	template = new DFAgentDescription();
			    sd = new ServiceDescription();
			    sd.setType("DNSSERVER");
			    template.addServices(sd);
			    all = new SearchConstraints();
			    all.setMaxResults(new Long(-1));
			    ArrayList<AID> DNSs = new ArrayList<AID>();
			    result = null;
			    try {
			        result = DFService.search(this, template, all);
			        if (result.length!=0)
			        	// Ottengo tutti i DNS.
			        	for (int i = 0; i<result.length; i++)
			        		if (!result[i].equals(getAID().getLocalName()))
			        			DNSs.add(result[i].getName());
			    } catch (final FIPAException fe) {
			        fe.printStackTrace();
			    }
			    ArrayList<AID> localDNSs = new ArrayList<AID>();
			    for (int i = 0; i<DNSs.size(); i++) 
			    	// Prendo solo i DNS di zona.
			    	if (DNSs.get(i).getLocalName().charAt(0)==getAID().getLocalName().charAt(0))
			    		localDNSs.add(DNSs.get(i));
			    /*
			     * Split casi: ci sono DNS in zona o no?
			     */
			    if (localDNSs.size()==0) {
			    	/*
			    	 * Non ci sono DNS in zona.
			    	 * 		3) Se nell'altra zona sono completi, clono quello con ridondanza minore;
			    	 * 		4) Se non lo sono, carico la differenza;
			    	 * 		5) Se non ne esistono, instanzio un nuovo DNS con metà degli host. 
			    	 * 			(se li caricassi tutti, dopo si avrebbero problemi per tutti gli altri casi)
			    	 */
			    	
			    	if (DNSs.size()==0) {
			    		/*
			    		 * CASO 5: Non esistono hosts e dunque ne instanzio la metà sul nuovo DNS.
			    		 */
			    		ArrayList<String> TLDs = new ArrayList<String>();
			    		for (int i = 0; i<cachedHostTable.size(); i++) 
			    			if (!TLDs.contains(cachedHostTable.get(i).getName().split("\\.")[1]))
			    				TLDs.add(cachedHostTable.get(i).getName().split("\\.")[1]);
			    		
			    		Collections.shuffle(TLDs);
			    		int j = 0;
			    		while (j<TLDs.size()/2) {
			    			for (int i = 0; i< cachedHostTable.size(); i++) 
			    				if (TLDs.get(j).equalsIgnoreCase(cachedHostTable.get(i).getName().split("\\.")[1]))
			    					hostTable.add(cachedHostTable.get(i));
			    			j++;
			    		}
			    		System.out.println(getAID().getLocalName() +" - half of cached hosts taken.");
			    	} else {
			    		/*
			    		 * CASI 3 E 4:
			    		 * Esistono altri DNS nell'altra zona.
			    		 * Controllo se i loro dati sono completi.
			    		 */
			    		ArrayList<Host> otherZoneHosts = new ArrayList<Host>();
			    		/*
			    		 * Creo una struttura dati per contare la ridondanza dei server delle altre zone (quanti cloni
			    		 * ci sono di ogni server?). In questo modo, se dovrò clonarne uno, scelgo quello con minor ridondanza.
			    		 */
			    		HashMap<String, Occurrence> redoundancyCheck = new HashMap<String, Occurrence>();
			    		for (int i = 0; i<DNSs.size(); i++) {
			    			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			    			request.setOntology("DNSHOSTS");
			    			request.addReceiver(DNSs.get(i));
			    			send(request);
			    			ArrayList<Host> tempHosts = null;
			    			try {
								 tempHosts = (ArrayList<Host>)blockingReceive(mtDNS, 5000).getContentObject();
							} catch (UnreadableException e) {
								e.printStackTrace();
							}
			    			if (tempHosts != null) {
			    				String hostsHash = null;
								try {
									hostsHash = HashGenerator.checksum(tempHosts);
								} catch (NoSuchAlgorithmException | IOException e) {
									e.printStackTrace();
								}
			    				System.out.println("Calculated hash:"+hostsHash);
			    				for (int j = 0; j<tempHosts.size(); j++)
			    					System.out.println(tempHosts.get(j).getAddress() + tempHosts.get(j).getName());
			    				if (redoundancyCheck.containsKey(hostsHash)) {
			    					Occurrence current = redoundancyCheck.get(hostsHash);
		    						current.qty++;
		    						redoundancyCheck.put(hostsHash, current);
			    				} else
			    					redoundancyCheck.put(hostsHash, new Occurrence(DNSs.get(i),1));

			    				otherZoneHosts = ArrayListUtils.addDifference(tempHosts, otherZoneHosts);
			    			}
			    		}
			    		
			    		/*
			    		 * Ottenuta la lista degli hosts presenti nell'altra zona, vedo se questa è completa o meno:
			    		 * se l'host non è presente nell'altra zona, lo aggiungo alla tabella del nuovo DNS.
			    		 * 
			    		 * CASO 4: Calcolo la differenza tra cache e quello che c'è nell'altra zona e la aggiungo alla
			    		 * tabella degli hosts del nuovo DNS.
			    		 */
			    		
			    		hostTable = ArrayListUtils.calculateDifference(cachedHostTable, otherZoneHosts);

			    		/*
			    		 * Se c'era una differenza tra le due zone, ho già il DNS inizializzato, altrimenti se l'altra
			    		 * zona è completa, la hostTable è ancora vuota e posso clonarla dal DNS con meno ridondanza:
			    		 */
			    		if (hostTable.size()==0) {
			    			/*
			    			 * CASO 3:
			    			 * I DNS dell'altra zona hanno informazioni complete, quindo clono quello con ridondanza inferiore.
			    			 */
			    			ArrayList<String> hashes = new ArrayList<String>();
			    			Object[] objHashes = redoundancyCheck.keySet().toArray();
			    			for (int i = 0; i < objHashes.length; i++)
			    				hashes.add((String)objHashes[i]);
			    			int occourrencies = redoundancyCheck.get(hashes.get(0)).qty;
			    			AID addressToClone = redoundancyCheck.get(hashes.get(0)).address;
			    			System.out.println("Occourr: "+occourrencies+" Address: "+addressToClone.getLocalName());
			    			for (int i = 1; i<hashes.size(); i++) {
			    				if (redoundancyCheck.get(hashes.get(i)).qty<occourrencies)
			    					addressToClone = redoundancyCheck.get(hashes.get(i)).address;
			    				System.out.println("Occourr: "+redoundancyCheck.get(hashes.get(i)).qty+" Address: "+redoundancyCheck.get(hashes.get(i)).address.getLocalName());
			    			}
			    			/*
			    			 * Trovato quello con ridondanza inferiore, lo clono.
			    			 */
			    			
			    			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			    			request.setOntology("DNSHOSTS");
			    			request.addReceiver(addressToClone);
			    			send(request);
			    			try {
								 hostTable = (ArrayList<Host>)blockingReceive(mtDNS, 5000).getContentObject();
								 System.out.println(getAID().getLocalName() +" - cloned DNS from the other zone with lowest redoundancy.");
							} catch (UnreadableException e) {
								System.err.println(getAID().getLocalName() +" - error while retrieving hostTable from the chosen DNS.");
								e.printStackTrace();
							}
			    		} else
			    			System.out.println(getAID().getLocalName() +" - populated table with the difference between cache and other zone.");
			    	}
			    }
			    else {
			    	/*
			    	 * Esistono DNS in zona: confronto l'unione dei loro dati con la cache internet:
			    	 *		1) Se è completo, allora vedo quale server è meno ridondante e lo clono;
			    	 *		2) Se non è completo, carico la differenza;
			    	 * 		 
			    	 */
			    	ArrayList<Host> peersHosts = new ArrayList<Host>();
		    		/*
		    		 * Creo una struttura dati per contare la ridondanza dei server delle altre zone (quanti cloni
		    		 * ci sono di ogni server?). In questo modo, se dovrò clonarne uno, scelgo quello con minor ridondanza.
		    		 */
		    		HashMap<Integer, Occurrence> redoundancyCheck = new HashMap<Integer, Occurrence>();
		    		for (int i = 0; i<localDNSs.size(); i++) {
		    			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		    			request.setOntology("DNSHOSTS");
		    			request.addReceiver(localDNSs.get(i));
		    			send(request);
		    			ArrayList<Host> tempHosts = null;
		    			try {
							 tempHosts = (ArrayList<Host>)blockingReceive(mtDNS, 5000).getContentObject();
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
		    			if (tempHosts != null) {
		    				int hostsHash = tempHosts.hashCode();
		    				if (redoundancyCheck.containsKey(hostsHash)) {
		    					Occurrence current = redoundancyCheck.get(hostsHash);
	    						current.qty++;
	    						redoundancyCheck.put(hostsHash, current);
		    				} else
		    					redoundancyCheck.put(hostsHash, new Occurrence(localDNSs.get(i),1));

		    				peersHosts = ArrayListUtils.addDifference(tempHosts, peersHosts);
		    			}
		    		}
		    		/*
		    		 * Ottenuta la lista completa degli hosts dei pari di zona, vedo se questa è completa o meno:
		    		 * se l'host non è presente in zona, lo aggiungo alla tabella del nuovo DNS.
		    		 * 
		    		 * CASO 2: Calcolo la differenza tra cache e quello che c'è in zona e la aggiungo alla
		    		 * tabella degli hosts del nuovo DNS.
		    		 */
		    		
		    		hostTable = ArrayListUtils.calculateDifference(cachedHostTable, peersHosts);
		    		
		    		/*
		    		 * Se c'era una differenza tra cache e host presenti in zona, ho già il DNS inizializzato, altrimenti se
		    		 * la zona è completa, la hostTable è ancora vuota e posso clonarla dal DNS con meno ridondanza:
		    		 */
		    		if (hostTable.size()==0) {
		    			/*
		    			 * CASO 1:
		    			 * I DNS in zona hanno informazioni complete, quindo clono quello con ridondanza inferiore.
		    			 */
		    			ArrayList<Integer> hashes = new ArrayList<Integer>();
		    			Object[] objHashes = redoundancyCheck.keySet().toArray();
		    			for (int i = 0; i < objHashes.length; i++)
		    				hashes.add((int)objHashes[i]);
		    			int occourrencies = redoundancyCheck.get(hashes.get(0)).qty;
		    			AID addressToClone = redoundancyCheck.get(hashes.get(0)).address;
		    			for (int i = 1; i<hashes.size(); i++) 
		    				if (redoundancyCheck.get(hashes.get(i)).qty<occourrencies)
		    					addressToClone = redoundancyCheck.get(hashes.get(i)).address;
		    			/*
		    			 * Trovato quello con ridondanza inferiore, lo clono.
		    			 */
		    			
		    			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		    			request.setOntology("DNSHOSTS");
		    			request.addReceiver(addressToClone);
		    			send(request);
		    			try {
							 hostTable = (ArrayList<Host>)blockingReceive(mtDNS, 5000).getContentObject();
							 System.out.println(getAID().getLocalName() +" - cloned DNS in zone with lowest redoundancy.");
						} catch (UnreadableException e) {
							System.err.println(getAID().getLocalName() +" - error while retrieving hostTable from the chosen DNS.");
							e.printStackTrace();
						}
		    		} else
		    			System.out.println(getAID().getLocalName() +" - populated table with the difference between cache and this zone.");
			    }
			    
			    System.out.println("HostTable of the new DNS:");
			    for (int i=0; i<hostTable.size(); i++)
			    	System.out.println(hostTable.get(i).getAddress() + " "+ hostTable.get(i).getName());
			    
			    /*
			     * Avendo riempito la tabella degli hosts, devo comunicare ai TLD le nuove competenze.
			     */
			    ArrayList<String> TLDs = new ArrayList<String>();
			    for (int i = 0; i< hostTable.size(); i++) 
			    	if (!TLDs.contains((hostTable.get(i).getName().split("\\.")[1])))
			    		TLDs.add(hostTable.get(i).getName().split("\\.")[1]);
			    
			    template = new DFAgentDescription();
			    sd = new ServiceDescription();
			    sd.setType("TLDSERVER");
			    template.addServices(sd);
			    all = new SearchConstraints();
			    all.setMaxResults(new Long(-1));
			    result = null;
			    try {
			        result = DFService.search(this, template, all);
			        if (result.length!=0) {
			        	for (int i = 0; i<result.length; i++) {
			        		ACLMessage request = new ACLMessage(ACLMessage.INFORM);
						    request.setOntology("NEWDNS");
						    request.setContentObject(TLDs);
						    request.addReceiver(result[i].getName());
						    send(request);
			        	}
			        }
			    } catch (final FIPAException e) {
			    	e.printStackTrace();
			    } catch (IOException e) {
					e.printStackTrace();
				}
		    }
		}
		
        /*
         * Inizializzazione della tabella contenente gli host che potrà risolvere a seconda
         * del suo indirizzo e della sua zona (i DNSServerAgent hanno una parte degli host risolvibili
         * nella loro zona)
         */
		else {
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
		}
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
            System.err.println("!!ERROR!! Registration of DNSServer to DF failed! System may not work properly.");
        }
        
        this.addBehaviour(new DNSServerAgent_ResolveName());
        this.addBehaviour(new DNSServerAgent_CreateNewHost());
        this.addBehaviour(new DNSServerAgent_Realignment());
        this.addBehaviour(new DNSServerAgent_YourTLDPlease());
        this.addBehaviour(new DNSServerAgent_GetAllHosts());
    }
	
	@Override
	public void takeDown() {
		
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		SearchConstraints all;
		
		sd.setType("TLDSERVER");
	    template.addServices(sd);
	    all = new SearchConstraints();
	    all.setMaxResults(new Long(-1));
	    
	    DFAgentDescription[] result = null;
	    try {
	        result = DFService.search(this, template, all);
	        for (int i = 0; i < result.length; ++i) { 
	        	ACLMessage deregister = new ACLMessage(ACLMessage.CANCEL);
	        	deregister.setOntology("TAKEDOWN");
				deregister.addReceiver(result[i].getName());
				send(deregister);
	        }
			DFService.deregister(this);
		} catch (FIPAException e) {
			System.err.println("Problems while deregistering the DNS Server "+getAID().getLocalName()+". System may not work properly.");
			e.printStackTrace();
		}
	}	

	public ArrayList<Host> getHostTable() {
		return hostTable;
	}
	
	public boolean addHost(String hostName, String address) {
		boolean isPresent = false;
		for (int i = 0; i < hostTable.size(); i++)
			if (hostTable.get(i).getAddress().equals(address) && hostTable.get(i).getName().equals(hostName))
				isPresent = true;
		if (!isPresent)
			hostTable.add(new Host(hostName, address));
		return !isPresent;
	}
	
	private class Occurrence {
		
		public int qty;
		public AID address;
		
		public Occurrence(AID addr, int q) {
			address = addr;
			qty = q;
		}
	}

}
