package dns.tables;

public class Host {

	private String hostName;
	private String hostAddress;
	
	public Host (String name, String address) {
		hostName = name;
		hostAddress = address;
	}
	
	public String getName() {
		return hostName;
	}
	
	public String getAddress() {
		return hostAddress;
	}
}
