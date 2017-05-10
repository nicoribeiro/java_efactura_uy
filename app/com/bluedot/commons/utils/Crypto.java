package com.bluedot.commons.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Encryption algorithm support
 * 
 * Shared secret keys are used to perform the Rijndael algorithm back and forth.
 * 
 * Control character remotion is applied to decryption result defending against
 * eventual errors.
 * 
 * UTF-8 Charset encoding is assumed.
 * 
 * @author gonzox
 */
public class Crypto
{
	private static SecureRandom rnd = new SecureRandom();

    private static final int MAX_SEED = 99999999;
    private static final int HASH_ITERATIONS = 1000;
    
	/** Rijndael 128 */
	private static String TRANSFORMATION = "AES/CBC/PKCS7Padding";

	static
	{
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Computes the md5 sum of the input <code>String</code> parsed as UTF-8
	 * 
	 * @param string
	 * @return The bytes of the md5 hash for the input string.
	 */
	public static byte[] md5(String string)
	{
		MD5Digest md5 = new MD5Digest();
		byte[] keyBytes = string.getBytes(Charset.forName("UTF-8"));
		byte[] result = new byte[16];
		md5.update(keyBytes, 0, keyBytes.length);
		md5.doFinal(result, 0);
		return result;
	}

	/**
	 * Rijndael 128 (AES) Decryption.
	 * 
	 * @param input
	 * @param base64
	 *            true if the input String is Base64 encoded
	 * @return
	 */
	public static String decrypt(String input, boolean base64, String KEY, String IV)
	{
		String result = null;
		byte[] bytesToDecrypt;

		if (base64)
			bytesToDecrypt = Base64.decodeBase64(input.getBytes(Charset.forName("UTF-8")));
		else
			bytesToDecrypt = input.getBytes(Charset.forName("UTF-8"));

		try
		{
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(md5(KEY), "AES"), new IvParameterSpec(md5(IV)));

			byte[] resultBytes = cipher.doFinal(bytesToDecrypt);

			result = removeControlChars(new String(resultBytes, Charset.forName("UTF-8")));

		} catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		} catch (NoSuchPaddingException e)
		{
			e.printStackTrace();
		} catch (InvalidKeyException e)
		{
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e)
		{
			e.printStackTrace();
		} catch (IllegalBlockSizeException e)
		{
			e.printStackTrace();
		} catch (BadPaddingException e)
		{
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Rijndael 128 (AES) Encryption.
	 * 
	 * @param input
	 * @param base64
	 *            true if the resulting String needs to be encoded as Base64
	 * @return
	 */
	public static String encrypt(String input, boolean base64, String KEY, String IV)
	{
		String result = null;
		byte[] bytesToEncrypt;

		bytesToEncrypt = input.getBytes(Charset.forName("UTF-8"));

		try
		{
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(md5(KEY), "AES"), new IvParameterSpec(md5(IV)));

			byte[] resultBytes = cipher.doFinal(bytesToEncrypt);

			result = base64 ? new String(Base64.encodeBase64(resultBytes), Charset.forName("UTF-8")) : new String(resultBytes, Charset.forName("UTF-8"));

		} catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		} catch (NoSuchPaddingException e)
		{
			e.printStackTrace();
		} catch (InvalidKeyException e)
		{
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e)
		{
			e.printStackTrace();
		} catch (IllegalBlockSizeException e)
		{
			e.printStackTrace();
		} catch (BadPaddingException e)
		{
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Removes control characters from a String
	 * 
	 * @param text
	 * @return
	 */
	private static String removeControlChars(String text)
	{
		String filteredText = text;

		if (filteredText != null)
		{
			filteredText = filteredText.replaceAll("\\p{Cc}", " ").replaceAll("\\s{2,}", " ");
		}
		return filteredText;
	}

	public static String Hash256(String text)throws Exception
    {
    	
 
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(text.getBytes());
 
        byte byteData[] = md.digest();
 
        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
         sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
 
        return sb.toString();
    }
	
	/**
     * Encrypt byte array.
     */
    public final static byte[] encrypt(byte[] source, String algorithm)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.reset();
        md.update(source);
        return md.digest();
    }

    /**
     * Encrypt string
     */
    public final static String encrypt(String source, String algorithm)
            throws NoSuchAlgorithmException {
        byte[] resByteArray = encrypt(source.getBytes(), algorithm);
        return toHexString(resByteArray);
    }

    /**
     * Encrypt string using MD5 algorithm
     */
    public final static String encryptMD5(String source) {
        if (source == null) {
            source = "";
        }

        String result = "";
        try {
            result = encrypt(source, "MD5");
        } catch (NoSuchAlgorithmException ex) {
            // this should never happen
            throw new RuntimeException(ex);
        }
        return result;
    }
    
    /**
     * Get hex string from byte array
     */
    public final static String toHexString(byte[] res) {
        StringBuilder sb = new StringBuilder(res.length << 1);
        for (int i = 0; i < res.length; i++) {
            String digit = Integer.toHexString(0xFF & res[i]);
            if (digit.length() == 1) {
                sb.append('0');
            }
            sb.append(digit);
        }
        return sb.toString().toUpperCase();
    }
    
    private static String encryptSalted(String password, String salt) {
        String hash = salt + password;
        for (int i = 0; i < HASH_ITERATIONS; i++) {
            hash = encryptMD5(hash);
        }
        return salt + ":" + hash;
    }

    /**
     * Encrypts the password using a salt concatenated with the password 
     * and a series of MD5 steps.
     */
    public static String encryptSalted(String password) {
        String seed = Integer.toString(rnd.nextInt(MAX_SEED));

        return encryptSalted(password, seed);
    }
    
    public static String HMACSHA256(String message, String secret) throws IOException
	{
		Mac mac = null;
		try
		{
			mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
		} catch (Throwable t)
		{
			throw new IOException(t);
		}

		return new String(Hex.encodeHex(mac.doFinal(message.getBytes())));

	}

}
