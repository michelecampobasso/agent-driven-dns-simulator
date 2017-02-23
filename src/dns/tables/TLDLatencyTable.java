package dns.tables;

import java.io.Serializable;

public class TLDLatencyTable implements Serializable {

	private static final long serialVersionUID = -3214551049703549982L;

	@SuppressWarnings("unused")
	private static int[] latencies = new int[] { 100, 200, 500 };
	private static int[] zones;
	
	@SuppressWarnings("static-access")
	private TLDLatencyTable(int[] z) {
		this.zones = z;
	}
	
	public static TLDLatencyTable getHostOptions(int address) {
		if (address >= 0 && address < 50)
			//Zone A
			zones = new int[] { 1, 2, 3 };
		if (address >=50 && address < 100) 
			//Zone B
			zones = new int[] { 2, 3, 1 };
		if (address >=100 && address < 150)
			//Zone C
			zones = new int[] { 3, 1, 2 };
		return new TLDLatencyTable(zones);
	};
	
	public int[] getZones() {
		return zones;
	}
}
