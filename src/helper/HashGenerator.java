package helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashGenerator {

	public static String checksum(Object obj) throws IOException, NoSuchAlgorithmException {

	    if (obj == null) {
	      return BigInteger.ZERO.toString();   
	    }

	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(baos);
	    oos.writeObject(obj);
	    oos.close();

	    MessageDigest m = MessageDigest.getInstance("SHA1");
	    m.update(baos.toByteArray());

	    return new BigInteger(1, m.digest()).toString();
	    
	}
}
