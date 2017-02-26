package dns.tables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

public class TLDTable implements Serializable {

	private static final long serialVersionUID = 2856143241037514122L;
	private ArrayList<TLDNameAndDate> TLDs;
	private ArrayList<String> Addresses;

	public TLDTable() {
		TLDs = new ArrayList<TLDNameAndDate>();
		Addresses = new ArrayList<String>();
		
	}

	public boolean addHost(String TLD, Calendar timeStamp, String address) {
		return (TLDs.add(new TLDNameAndDate(TLD, timeStamp)) &&	Addresses.add(address));
	}

	public void deleteHostByTLD(String TLD, String address) {
		for (int i = 0; i < TLDs.size(); i++)
			if (TLDs.get(i).TLD.equals(TLD) && Addresses.get(i).equals(address)) {
				TLDs.remove(i);
				Addresses.remove(i);
			}
	}
	
	public void deleteHost(String address) {
		for (int i=0; i<Addresses.size(); i++)
			if (Addresses.get(i).equals(address)) {
				Addresses.remove(i);
				TLDs.remove(i);
				i--;
			}
	}

	public ArrayList<String> getAllAddresses() {
		return Addresses;
	}
	
	public TLDNameAndDate getTLD(int i) {
		return TLDs.get(i);
	}
	
	public String getAddress(int i) {
		return Addresses.get(i);
	}

	public ArrayList<TLDNameAndDate> getAllTLD() {
		return TLDs;
	}

	public ArrayList<String> getAddressesFromTLD(String TLD) {
		ArrayList<String> retval = new ArrayList<String>();

		for (int i = 0; i < TLDs.size(); i++)
			if (TLDs.get(i).TLD.equals(TLD))
				retval.add(Addresses.get(i));
		return retval;
	}
	
	public ArrayList<String> getAddressesFromTLDByZone(String TLD, char zone) {
		ArrayList<String> retval = new ArrayList<String>();

		for (int i = 0; i < TLDs.size(); i++)
			if (TLDs.get(i).TLD.equals(TLD) && Addresses.get(i).charAt(0)==zone)
				retval.add(Addresses.get(i));
		return retval;

	}
	
	public String getAddressFromTLD(String TLD) {
		for (int i=0;i<TLDs.size(); i++)
			if (TLDs.get(i).TLD.equalsIgnoreCase(TLD))
				return Addresses.get(i);
		return "";
	}
	
	public int getSize() {
		return Addresses.size();
	}
	
	public class TLDNameAndDate implements Serializable {
		
		private static final long serialVersionUID = 2793203510412665612L;
		public String TLD;
		public Calendar timeStamp;
		
		public TLDNameAndDate (String t, Calendar ts) {
			TLD = t;
			timeStamp = ts;
		}
	}

	
}
