package helper;

public class LatencyGenerator {

	public static long latency(char zoneSender, char zoneReceiver) {
		
		/*
		 *  Si sceglie come latenza per il trasporto dei messaggi tra zone diverse 
		 *  mediamente tre volte quello del trasporto nella stessa zona. 
		 */
		int latency;
		if (zoneSender == zoneReceiver)
			latency = 100 + (int) (Math.random() * 400);
		else 
			latency = 300 + (int) (Math.random() * 1200);
		return latency;
	}
	
	public static long latencySameZone() {
		return (100 + (int) (Math.random() * 400));
	}

	public static long latencyOtherZone() {
		return (300 + (int) (Math.random() * 1200));
	}
}
