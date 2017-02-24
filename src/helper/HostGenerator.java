package helper;

import java.util.Random;

public class HostGenerator {

	public static String generateHostName() {
        String SALTCHARS = "qwertyuiopasdfghjklzxcvbnm";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 12) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        String[] TLDs = { ".it", ".com", ".edu", ".fr", ".es", ".net" };
        saltStr = saltStr + TLDs[rnd.nextInt(6)];
        return saltStr;
    }
	
	public static String generateHostAddress() {
		Random r = new Random();
		String retval = r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
		while (retval.charAt(0)!='1' && retval.charAt(0)!='2')
			retval = r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
		return retval;
		
	}
}
