package helper;

import java.util.ArrayList;

import dns.tables.Host;

public class ArrayListUtils {

	public static ArrayList<Host> addDifference(ArrayList<Host> tempHosts, ArrayList<Host> comulativeHosts) {

		/*
		 * Adds from tempHosts all the hosts not contained into comulativeHosts into a new ArrayList,
		 * plus the already present into comulativeHosts
		 */
		ArrayList<Host> retval = comulativeHosts;
		for (int j =0; j<tempHosts.size(); j++) {
			boolean contained = false;
			for (int k = 0; k<comulativeHosts.size(); k++) {
				if (tempHosts.get(j).getAddress().equals(comulativeHosts.get(k).getAddress()) &&
						tempHosts.get(j).getName().equals(comulativeHosts.get(k).getName())) {
					contained = true;
					break;
				}
			}
			if (!contained)
				retval.add(tempHosts.get(j));
		}
		return retval;
	}
	
	public static ArrayList<Host> calculateDifference(ArrayList<Host> total, ArrayList<Host> subtract) {
		
		/*
		 * Calculates the differences between total and subtract and returns the new ArrayList. 
		 */
		
		ArrayList<Host> retval = new ArrayList<Host>();
		for (int j =0; j<total.size(); j++) {
			boolean contained = false;
			for (int k = 0; k<subtract.size(); k++) {
				if (total.get(j).getAddress().equals(subtract.get(k).getAddress()) &&
						total.get(j).getName().equals(subtract.get(k).getName())) {
					contained = true;
					break;
				}
			}
			if (!contained)
				retval.add(total.get(j));
		}
		return retval;
	}
}
