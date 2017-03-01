package dns.theinternet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import dns.tables.Host;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class TheInternetAgent_AllHosts extends Behaviour {

	private static final long serialVersionUID = 4392831449438034866L;

	private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
			MessageTemplate.MatchOntology("ALLHOSTSPLEASE"));
	@Override
	public void action() {
		
		ACLMessage msg = myAgent.receive(mt);
		if (msg != null) {
			
			System.out.println("The Internet - received msg to forward all hosts.");
			
			ArrayList<Host> hostTable = new ArrayList<Host>();
			BufferedReader br;
			try {
				String str = null;
				br = new BufferedReader(new FileReader("added_hosts.txt"));
				while ((str = br.readLine())!=null)
					hostTable.add(new Host(str.split("\\s+")[0], str.split("\\s+")[1]));
				br.close();
				str = null;
				br = new BufferedReader(new FileReader("dnshosts.txt"));
				while ((str = br.readLine())!=null)
					hostTable.add(new Host(str.split("\\s+")[0], str.split("\\s+")[1]));
				br.close();
				
				ACLMessage reply = msg.createReply();
				reply.setContentObject(hostTable);
				reply.setOntology("ALLHOSTSPLEASE");
				reply.setPerformative(ACLMessage.INFORM);
				myAgent.send(reply);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else
			block();
		
		
	}

	@Override
	public boolean done() {
		return false;
	}

}
