package dns.tables;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;

import jade.core.AID;

public class PrimaryTable implements Serializable {

	private static final long serialVersionUID = -5784073345574600842L;

	public Calendar lastModified;
	public HashMap<String, AID[]> table;
	
	public PrimaryTable() {
		lastModified = Calendar.getInstance();
		table = new HashMap<String, AID[]>();
	}
	
	public boolean isPresent (String firstLevelDomain) {
		if (table.get(firstLevelDomain).equals(null))
			return false;
		else
			return true;
	}
	
	public void addSecondLevelResolvers (String firstLevelDomain, AID[] resolvers) {
		table.put(firstLevelDomain, resolvers);
	}
	
	public AID[] getSecondLevelResolvers (String firstLevelDomain) {
		return table.get(firstLevelDomain);
	}
}
