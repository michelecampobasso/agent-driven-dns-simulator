package dns.tables;

import java.io.Serializable;

public class Host implements Serializable {

	private static final long serialVersionUID = 7733275415164562330L;

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
