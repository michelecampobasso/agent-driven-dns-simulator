package dns.tables;

import java.io.Serializable;
import java.util.ArrayList;

public class TLDTable implements Serializable {

	private static final long serialVersionUID = 2856143241037514122L;
	private ArrayList<String> TLDs;
	private ArrayList<String> Addresses;

	public TLDTable() {
		TLDs = new ArrayList<String>();
		Addresses = new ArrayList<String>();
		
	}
	
	public void addHost(String TLD, String address) {
		TLDs.add(TLD);
		Addresses.add(address);
	}

	public void deleteHostByTLD(String TLD, String address) {
		for (int i = 0; i < TLDs.size(); i++)
			if (TLDs.get(i).equals(TLD) && Addresses.get(i).equals(address)) {
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
	
	public ArrayList<String> getAllAddressesByZone(char zone) {
		ArrayList<String> addresses = new ArrayList<String>();
		for (int i = 0; i < Addresses.size(); i++)
			if (Addresses.get(i).charAt(0)==zone) 
				if (!addresses.contains(Addresses.get(i)))
						addresses.add(Addresses.get(i));
		return addresses;
	}
	
	public String getTLD(int i) {
		return TLDs.get(i);
	}
	
	public String getAddress(int i) {
		return Addresses.get(i);
	}

	public ArrayList<String> getAllTLD() {
		return TLDs;
	}

	public ArrayList<String> getAddressesFromTLD(String TLD) {
		ArrayList<String> retval = new ArrayList<String>();

		for (int i = 0; i < TLDs.size(); i++)
			if (TLDs.get(i).equals(TLD))
				retval.add(Addresses.get(i));
		return retval;
	}
	
	public ArrayList<String> getAddressesFromTLDByZone(String TLD, char zone) {
		ArrayList<String> retval = new ArrayList<String>();

		for (int i = 0; i < TLDs.size(); i++)
			if (TLDs.get(i).equals(TLD) && Addresses.get(i).charAt(0)==zone)
				retval.add(Addresses.get(i));
		return retval;
	}
	
	public ArrayList<String> getAddressesFromTLDByOtherZone(String TLD, char zone) {
		ArrayList<String> retval = new ArrayList<String>();

		for (int i = 0; i < TLDs.size(); i++)
			if (TLDs.get(i).equals(TLD) && Addresses.get(i).charAt(0)!=zone)
				retval.add(Addresses.get(i));
		return retval;

	}
	
	public String getAddressFromTLD(String TLD) {
		for (int i=0;i<TLDs.size(); i++)
			if (TLDs.get(i).equalsIgnoreCase(TLD))
				return Addresses.get(i);
		return "";
	}
	
	public int getSize() {
		return Addresses.size();
	}
	
	public void toPrint() {
		for (int i = 0; i<Addresses.size(); i++) {
			System.out.println("Address: "+Addresses.get(i)+ " TLD: "+TLDs.get(i));
		}
	}
}
