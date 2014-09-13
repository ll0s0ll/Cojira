/*
	RTMP.java
	
	Copyright (C) 2014  Shun ITO <movingentity@gmail.com>
	
	This file is part of Cojira.

	Cojira is free software; you can redistribute it and/or
	modify it under the terms of the GNU General Public License
	as published by the Free Software Foundation; either version 2
	of the License, or (at your option) any later version.
	
	Cojira is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
    along with Cojira.  If not, see <http://www.gnu.org/licenses/>.
*/


package mypackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import javax.microedition.io.SocketConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.media.Control;
import javax.microedition.media.protocol.ContentDescriptor;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.protocol.SourceStream;

import net.rim.device.api.crypto.ARC4Key;
import net.rim.device.api.crypto.ARC4PseudoRandomSource;
import net.rim.device.api.crypto.CryptoTokenException;
import net.rim.device.api.crypto.CryptoUnsupportedOperationException;
import net.rim.device.api.crypto.DHCryptoSystem;
import net.rim.device.api.crypto.DHKeyAgreement;
import net.rim.device.api.crypto.DHKeyPair;
import net.rim.device.api.crypto.DHPublicKey;
import net.rim.device.api.crypto.HMAC;
import net.rim.device.api.crypto.HMACKey;
import net.rim.device.api.crypto.InvalidCryptoSystemException;
import net.rim.device.api.crypto.PRNGDecryptor;
import net.rim.device.api.crypto.PRNGEncryptor;
import net.rim.device.api.crypto.SHA256Digest;
import net.rim.device.api.crypto.UnsupportedCryptoSystemException;
import net.rim.device.api.io.SharedInputStream;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.ByteArrayUtilities;

public class RTMP extends DataSource
{
	private static final int RTMP_SIG_SIZE          = 1536;
	private static final int SHA256_DIGEST_LENGTH   = 32;
	private static final int RTMP_DEFAULT_CHUNKSIZE = 128;  //140
	
	private static final double TRANSACTION_ID_CONNECT = 1.0;
	private static final double TRANSACTION_ID_CREATESTREAM = 2.0;
	
	private MyApp _app;
	
	private ConnectionFactory _factory;
	private SocketConnection sc;
   	private InputStream dis;
   	private OutputStream os;
   	private PRNGDecryptor decryptis;
   	private PRNGEncryptor cryptos;
   	

    private byte[] nocopyba;
    private int aacAudioDataLength;
    private ByteArrayInputStream stream;
    
    private String authkey;
    private int serverWindowAcknowledgementSize = 2500000;
    private int totalReadSize = 0;
    private int reportedReadSize = 0;
    private int ClientBW = 0;
    private double streamID;
    
    
	// 32 Genuine Adobe Flash Media Server 001
	private static final String GenuineFPKey2 = "47656E75696E6520"+
												"41646F626520466C"+
												"61736820506C6179"+
												"657220303031F0EE"+
												"C24A8068BEE82E00"+
												"D0D1029E7E576EEC"+
												"5D2D29806FAB93B8"+
												"E636CFEB31AE"; // 62
	
	// 36 Genuine Adobe Flash Media Server 001 
	private static final String GenuineFMSKey2 = "47656E75696E6520"+
												 "41646F626520466C"+
												 "617368204D656469"+
												 "6120536572766572"+
												 "20303031F0EEC24A"+
												 "8068BEE82E00D0D1"+ 
												 "029E7E576EEC5D2D"+
												 "29806FAB93B8E636"+ 
												 "CFEB31AE";  // 68    	
	
	
	// Genuine Adobe Flash Player 001
	final byte[] GenuineFPKey = new byte[] {
    		(byte)0x47, (byte)0x65, (byte)0x6E, (byte)0x75, (byte)0x69, (byte)0x6E, (byte)0x65, (byte)0x20,
    		(byte)0x41, (byte)0x64, (byte)0x6F, (byte)0x62, (byte)0x65, (byte)0x20, (byte)0x46, (byte)0x6C,
    		(byte)0x61, (byte)0x73, (byte)0x68, (byte)0x20, (byte)0x50, (byte)0x6C, (byte)0x61, (byte)0x79,
    		(byte)0x65, (byte)0x72, (byte)0x20, (byte)0x30,	(byte)0x30, (byte)0x31
    };
    /*
    		(byte)0xF0, (byte)0xEE,
    		(byte)0xC2, (byte)0x4A, (byte)0x80, (byte)0x68, (byte)0xBE, (byte)0xE8, (byte)0x2E, (byte)0x00,
    		(byte)0xD0, (byte)0xD1, (byte)0x02, (byte)0x9E, (byte)0x7E, (byte)0x57, (byte)0x6E, (byte)0xEC,
    		(byte)0x5D, (byte)0x2D, (byte)0x29, (byte)0x80, (byte)0x6F, (byte)0xAB, (byte)0x93, (byte)0xB8,
    		(byte)0xE6, (byte)0x36, (byte)0xCF, (byte)0xEB, (byte)0x31, (byte)0xAE
    };*/ 
	 
	
	
	
	public RTMP(String locator)
	{
		// extends DataSource
		super(locator);
		_app = (MyApp) UiApplication.getUiApplication();
		authkey = _app._auth.getAuthToken();
		_factory = _app.getConnectionFactory();
	}
	
	
	public void updateStatus(String val)
	{
		//MyApp _app = (MyApp) UiApplication.getUiApplication();
		//MyScreen _screen = (MyScreen) _app.getActiveScreen();
		//_screen.updateStatus("[RTMP] " + val);
		//_app.updateStatus(val);
	}
	
	/*
	public myDataSource getPlayerSource()
	{
		//nocopyba = new byte[512];
		//stream = new ByteArrayInputStream(nocopyba);
		_source = new myDataSource(stream, "audio/aac");
		aacAudioDataLength = 0;
		return _source;		
	}
	*/
	/*
	public String challengeAuth1() throws Exception
	{
		HttpsConnection c = null;
		InputStream is = null;
	    String url = "https://radiko.jp/v2/api/auth1_fms";
	    String url2 = "https://radiko.jp/v2/api/auth2_fms";
	    String authtoken, partialkey;
	    int rc, keylength, keyoffset;
	    
	    try {
	    	updateStatus("Connecting..");
	    	
	    	// Create ConnectionFactory
	    	//ConnectionFactory factory = new ConnectionFactory();
	
	    	// use the factory to get a connection
	    	ConnectionDescriptor conDescriptor = factory.getConnection( url );
	
	    	if (conDescriptor == null)
	    		throw new Exception("conDescriptor Error");
	    	
			// connection succeeded
			//int transportUsed = conDescriptor.getTransportDescriptor().getTransportType();
			
			// using the connection
			//HttpConnection  httpCon = (HttpConnection) conDescriptor.getConnection();
			c = (HttpsConnection) conDescriptor.getConnection();
		    	
	        // Set the request method and headers
	        c.setRequestMethod(HttpsConnection.POST);
	        c.setRequestProperty("pragma", "no-cache");
	        c.setRequestProperty("X-Radiko-App", "pc_1");
	        c.setRequestProperty("X-Radiko-App-Version", "2.0.1");
	        c.setRequestProperty("X-Radiko-User", "test-stream");
	        c.setRequestProperty("X-Radiko-Device", "pc");
	        
	        // Getting the response code will open the connection,
	        // send the request, and read the HTTP response headers.
	        // The headers are stored until requested.
	        rc = c.getResponseCode();
	        if (rc != HttpConnection.HTTP_OK)
	            throw new Exception("HTTP response code: " + rc);
	        
	        authtoken = c.getHeaderField("X-RADIKO-AUTHTOKEN");
	        keylength = Integer.parseInt(c.getHeaderField("X-Radiko-KeyLength"));
	        keyoffset = Integer.parseInt(c.getHeaderField("X-Radiko-KeyOffset"));
	        
	        updateStatus("auth1..\n AuthToken:" + authtoken + "\n KeyLength:" + Integer.toString(keylength) + "\n KeyOffset:" + keyoffset);
	
		} finally {
			updateStatus("try close");
			if (c != null)
				c.close();
	    } //try
		
		
	    FileConnection fconn = (FileConnection)Connector.open("file:///SDCard/authkey.png");
	    //updateStatus("OK I'm " + fconn.getName() + " " + fconn.fileSize() );
	    if (!fconn.exists())
	    	throw new Exception("Not Found authkey.png");        
	    
	    InputStream fconnIS =  fconn.openInputStream();
	    fconn.close();        
	    
	    byte[] b1 = new byte[ keylength ];
	    
	    fconnIS.skip(keyoffset);
	    fconnIS.read(b1, 0, keylength);
	    fconnIS.close();
	         
	    //byte[] encoded2 = Base64OutputStream.encode(b1, 0, b1.length, false, false);
	    partialkey = new String( Base64OutputStream.encode(b1, 0, b1.length, false, false) );
	    //updateStatus("RED2 " + partialkey);
	        
		
		
		// TRY Auth2
		try {
	    	updateStatus("Connecting..(auth2)");
	    	
	    	// Create ConnectionFactory
	    	//factory = new ConnectionFactory();
	
	    	// use the factory to get a connection
	    	ConnectionDescriptor conDescriptor = factory.getConnection( url2 );
	
	    	if (conDescriptor == null)
	    		throw new Exception("conDescriptor ERROR");    	
	
	    	// connection succeeded
	    	//int transportUsed = conDescriptor.getTransportDescriptor().getTransportType();
	
	    	// using the connection
	    	c = (HttpsConnection) conDescriptor.getConnection();   	
	    	
	        // Set the request method and headers
	        c.setRequestMethod(HttpsConnection.POST);
	        c.setRequestProperty("pragma", "no-cache");
	        c.setRequestProperty("X-Radiko-App", "pc_1");
	        c.setRequestProperty("X-Radiko-App-Version", "2.0.1");
	        c.setRequestProperty("X-Radiko-Authtoken", authtoken);
	        c.setRequestProperty("X-Radiko-Partialkey", partialkey);
	        
	        // Getting the response code will open the connection,
	        // send the request, and read the HTTP response headers.
	        // The headers are stored until requested.
	        rc = c.getResponseCode();
	        if (rc != HttpConnection.HTTP_OK)        
	            throw new IOException("HTTP response code: " + rc);                
	        
	        // Get the length and process the data
	        is = c.openDataInputStream();         
	        int waiting = is.available();
	        updateStatus("waiting:" + Integer.toString(waiting));
	        
	        int ch;
	        byte[] b = new byte[ waiting ];
	        ch = is.read(b);
	        updateStatus("BYTE[" + Integer.toString(ch) + "] " + new String(b));
	        
	        return authtoken;
	
		} finally {
			updateStatus("try close");
			if (c != null)
				c.close();
	    } //try
	} //challengeAuth1
	 */

	public DHKeyPair crypt(byte[] chunk_C1) throws Exception
	{
		
		String P1024 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" + 
				"29024E088A67CC74020BBEA63B139B22514A08798E3404DD" + 
				"EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
				"E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" + 
				"EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381" +
				"FFFFFFFFFFFFFFFF";
		
		String Q1024 = "7FFFFFFFFFFFFFFFE487ED5110B4611A62633145C06E0E68" +
				"948127044533E63A0105DF531D89CD9128A5043CC71A026E" +
				"F7CA8CD9E69D218D98158536F92F8A1BA7F09AB6B6A8E122" +
				"F242DABB312F3F637A262174D31BF6B585FFAE5B7A035BF6" +
				"F71C35FDAD44CFD2D74F9208BE258FF324943328F67329C0" +
				"FFFFFFFFFFFFFFFF";
		
		byte[] p = new byte[256];
		p = ByteArrayUtilities.hexToByteArray(P1024);
		byte[] q = new byte[256];
		q = ByteArrayUtilities.hexToByteArray(Q1024);
		byte[] g = new byte[1];
		g[0] = 0x02;
		
		DHKeyPair dh_keypair = null;
		
		try {
			DHCryptoSystem dh = new DHCryptoSystem(p,q,g);
			dh.verify();
	
			//updateStatus("P:" + ByteArrayUtilities.byteArrayToHex(dh.getP()));
			//updateStatus("Q:" + ByteArrayUtilities.byteArrayToHex(dh.getQ()));
			//updateStatus("G:" + ByteArrayUtilities.byteArrayToHex(dh.getG()));
			
			dh_keypair = dh.createDHKeyPair();		
			dh_keypair.verify();
			
			return dh_keypair;
			/*
			byte[] pub_key = new byte[dh_keypair.getDHPublicKey().getPublicKeyData().length];
			pub_key = dh_keypair.getDHPublicKey().getPublicKeyData();
			
			//updateStatus("PublicKey:" + ByteArrayUtilities.byteArrayToHex(pub_key));
			//updateStatus("PublicKey:" + ByteArrayUtilities.byteArrayToHex(dh_keypair.getDHPublicKey().getPublicKeyData()));
			//updateStatus("PublicKey_LENGTH:" + dh_keypair.getDHPublicKey().getPublicKeyData().length);
			
			//updateStatus("PrivateKey:" + ByteArrayUtilities.byteArrayToHex(dh_keypair.getDHPrivateKey().getPrivateKeyData()));
			//updateStatus("PrivateKey_LENGTH:" + dh_keypair.getDHPrivateKey().getPrivateKeyData().length);
			
			
			// dhposClient=472
			int pos = GetDHOffset2(chunk_C1);  //472
			System.arraycopy(pub_key, 0, chunk_C1, pos, pub_key.length);
			*/
		} catch (InvalidCryptoSystemException e1) {
			updateStatus( e1.toString() );
		} catch (UnsupportedCryptoSystemException e1) {
			updateStatus( e1.toString() );
		} catch (CryptoTokenException e1) {
			updateStatus( e1.toString() );
		} catch (CryptoUnsupportedOperationException e1) {
			updateStatus( e1.toString() );
		}
		/*
		// Genuine Adobe Flash Player 001
	    byte[] GenuineFPKey = new byte[] {
	    		(byte)0x47, (byte)0x65, (byte)0x6E, (byte)0x75, (byte)0x69, (byte)0x6E, (byte)0x65, (byte)0x20,
	    		(byte)0x41, (byte)0x64, (byte)0x6F, (byte)0x62, (byte)0x65, (byte)0x20, (byte)0x46, (byte)0x6C,
	    		(byte)0x61, (byte)0x73, (byte)0x68, (byte)0x20, (byte)0x50, (byte)0x6C, (byte)0x61, (byte)0x79,
	    		(byte)0x65, (byte)0x72, (byte)0x20, (byte)0x30,	(byte)0x30, (byte)0x31
	    };
	    *//*
	    		(byte)0xF0, (byte)0xEE,
	    		(byte)0xC2, (byte)0x4A, (byte)0x80, (byte)0x68, (byte)0xBE, (byte)0xE8, (byte)0x2E, (byte)0x00,
	    		(byte)0xD0, (byte)0xD1, (byte)0x02, (byte)0x9E, (byte)0x7E, (byte)0x57, (byte)0x6E, (byte)0xEC,
	    		(byte)0x5D, (byte)0x2D, (byte)0x29, (byte)0x80, (byte)0x6F, (byte)0xAB, (byte)0x93, (byte)0xB8,
	    		(byte)0xE6, (byte)0x36, (byte)0xCF, (byte)0xEB, (byte)0x31, (byte)0xAE
	    };*/ 
	    
		/*
	    int digestPosClient = GetDigestOffset2(chunk_C1);
	    CalculateDigest(digestPosClient, chunk_C1, GenuineFPKey, chunk_C1, digestPosClient);
	    */
	    return dh_keypair;
	    
		/*
		try {
			
			// See...
			// Lesson: Digests and MACs
			// http://www.blackberry.com/developers/docs/6.0.0api/net/rim/device/api/crypto/doc-files/digest.html
		    
		    final int SHA256_DIGEST_LENGTH = 32;
		    final int RTMP_SIG_SIZE        = 1536;
		    
		    int messageLen = RTMP_SIG_SIZE - SHA256_DIGEST_LENGTH;
		    int digestPos = GetDigestOffset2(chunk_C1);  //1288;
		    byte[] message = new byte[ messageLen ];
		    
		    System.arraycopy(chunk_C1, 0, message, 0, digestPos);
		    System.arraycopy(chunk_C1, digestPos + SHA256_DIGEST_LENGTH, message, digestPos, messageLen - digestPos);
		    
		    //updateStatus("message:" + ByteArrayUtilities.byteArrayToHex(message));		    
		    
		    
		    // Create the key
		    HMACKey key = new HMACKey( GenuineFPKey );
		    
		    // Create the SHA digest
		    SHA256Digest digest = new SHA256Digest();
		    
		    // Now an HMAC can be created, passing in the key and the
		    // SHA digest. Any instance of a digest can be used here.
		    HMAC hMac = new HMAC( key, digest );
		    updateStatus("HMAC Length:" + hMac.getLength());
		    	
		    // The HMAC can be updated much like a digest
		    hMac.update( message );
		    //hMac.update( chunk_C1, 0, 1536 );
		    //hMac.update( data, 0, 4 );
	
		    // Now get the MAC value.
		    byte[] macValue = hMac.getMAC();
		    
		    updateStatus("HMAC_SHA256:" + ByteArrayUtilities.byteArrayToHex(macValue));
		    updateStatus("Length:" + macValue.length);
		    
		    System.arraycopy(macValue, 0, chunk_C1, digestPos, macValue.length);
		    
		    
		} catch (CryptoUnsupportedOperationException e1) {
			updateStatus( e1.toString() );
		} catch (CryptoTokenException e1) {
			updateStatus( e1.toString() );
		}
		*/
		
	}

	public void CalculateDigest(int digestPos, byte[] chunk_C1, byte[] key, byte[] digest, int digestOffset) throws Exception
	{
		try {
			// See...
			// Lesson: Digests and MACs
			// http://www.blackberry.com/developers/docs/6.0.0api/net/rim/device/api/crypto/doc-files/digest.html
		    
		    int messageLen = RTMP_SIG_SIZE - SHA256_DIGEST_LENGTH;
		    //int digestPos = GetDigestOffset2(chunk_C1);
		    byte[] message = new byte[ messageLen ];
		    
		    System.arraycopy(chunk_C1, 0, message, 0, digestPos);
		    System.arraycopy(chunk_C1, digestPos + SHA256_DIGEST_LENGTH, message, digestPos, messageLen - digestPos);
		    
		    //updateStatus("message:" + ByteArrayUtilities.byteArrayToHex(message));		    
		    
		    // Create the key
		    HMACKey hmackey = new HMACKey( key );
		    
		    // Create the SHA digest
		    SHA256Digest sha256digest = new SHA256Digest();
		    
		    // Now an HMAC can be created, passing in the key and the
		    // SHA digest. Any instance of a digest can be used here.
		    HMAC hMac = new HMAC( hmackey, sha256digest );
		    //updateStatus("HMAC Length:" + hMac.getLength());
		    	
		    // The HMAC can be updated much like a digest
		    hMac.update( message );
		    //hMac.update( chunk_C1, 0, 1536 );
		    //hMac.update( data, 0, 4 );
	
		    // Now get the MAC value.
		    byte[] macValue = hMac.getMAC();
		    
		    //updateStatus("HMAC_SHA256:" + ByteArrayUtilities.byteArrayToHex(macValue));
		    //updateStatus("Length:" + macValue.length);
		    
		    System.arraycopy(macValue, 0, digest, digestOffset, macValue.length);
		    
		    
		} catch (CryptoUnsupportedOperationException e1) {
			updateStatus( e1.toString() );
		} catch (CryptoTokenException e1) {
			updateStatus( e1.toString() );
		}
	}

	public void HMACsha256(byte[] msg, int msgOffset, int msgLen, String key, int keyLen, byte[] digest, int digestOffset)
	{
		// See...
		// Lesson: Digests and MACs
		// http://www.blackberry.com/developers/docs/6.0.0api/net/rim/device/api/crypto/doc-files/digest.html
	
		try {
			
			byte[] modifiedKey = Arrays.copy(ByteArrayUtilities.hexToByteArray(key), 0, keyLen);
			
		    // Create the key
		    HMACKey hmackey = new HMACKey(modifiedKey);
		    
		    // Create the SHA digest
		    SHA256Digest sha256digest = new SHA256Digest();
		    
		    // Now an HMAC can be created, passing in the key and the
		    // SHA digest. Any instance of a digest can be used here.
		    HMAC hMac = new HMAC(hmackey, sha256digest);
	
		    //updateStatus("HMAC Length:" + hMac.getLength());
		    	
		    // The HMAC can be updated much like a digest
		    hMac.update(msg,  msgOffset, msgLen);
		    //hMac.update( chunk_C1, 0, 1536 );
		    //hMac.update( data, 0, 4 );
	
		    // Now get the MAC value.
		    byte[] macValue = hMac.getMAC();
	
		    //updateStatus("HMAC_SHA256:" + ByteArrayUtilities.byteArrayToHex(macValue));
		    //updateStatus("Length:" + macValue.length);
		    
		    System.arraycopy(macValue, 0, digest, digestOffset, macValue.length);
		    
		} catch (CryptoTokenException e) {
			updateStatus(e.toString());
		} catch (CryptoUnsupportedOperationException e) {
			updateStatus(e.toString());
		} 
	}


	public boolean VerifyDigest(int digestPos, byte[] handshakeMessage, byte[] key) throws Exception
	{	
		final int SHA256_DIGEST_LENGTH = 32;
		byte[] calcDigest = new byte[SHA256_DIGEST_LENGTH];
		CalculateDigest(digestPos, handshakeMessage, key, calcDigest, 0);
		
		return Arrays.equals(handshakeMessage, digestPos, calcDigest, 0, SHA256_DIGEST_LENGTH);
	}


	public int GetDHOffset1(byte[] handshake) throws Exception
	{
		int offset = 0;
		int ptr = 1532;
		int res;
		
		offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	
	    res = (offset % 632) + 772;
	    //updateStatus( "GetDHOffset1 res:" + Integer.toString(res) );
	
	    if (res + 128 > 1531)
	    	throw new Exception("GetDHOffset1 ERROR!!\n");
	    
	    return res;
	}


	public int GetDHOffset2(byte[] handshake) throws Exception
	{
		int offset = 0;
		int ptr = 768;
		int res;
		
		offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	
	    res = (offset % 632) + 8;
	    //updateStatus( "GetDHOffset2 res:" + Integer.toString(res) );
	
	    if (res + 128 > 767)
	    	throw new Exception("GetDHOffset2 ERROR!!\n");
	    
	    return res;
	}


	public int GetDigestOffset1(byte[] handshake)
	{
		int offset = 0;
	    int ptr = 8;
	    int res;
		
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	
	    res = (offset % 728) + 12;
	    updateStatus( "GetDigestOffset1 res:" + Integer.toString(res) );
	    
	    if (res + 32 > 771)
	    {
	    	updateStatus( "GetDigestOffset1 ERROR!!\n" );
	        return 0;
	    }
	
	    return res;
	}


	public int GetDigestOffset2(byte[] handshake) throws Exception
	{
		int offset = 0;
	    int ptr = 772;
	    int res;
		
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	    ptr++;
	    offset += (int)(handshake[ptr] & 0xFF); //updateStatus( "offset:" + Integer.toString(offset) );
	
	    res = (offset % 728) + 776;
	    //updateStatus( "GetDigestOffset2 res:" + Integer.toString(res) );
	    
	    if (res + 32 > 1535)
	    	throw new Exception("GetDigestOffset2 ERROR!!\n");
	
	    return res;
	}


	public ARC4PseudoRandomSource GetARC4Keystream(byte[] secretKey, byte[] msg, int msgOffset) throws Exception
	{
		// See... (ARC4)
		// ストリーム暗号化 - Security - 開発ガイド - BlackBerry Java SDK - 6.0
		// http://docs.blackberry.com/fr-fr/developers/deliverables/32802/Stream_algorithms_1304683_11.jsp
		//
		// Crypto API Sample Code - Copyright Research In Motion Limited, 1998-2001
		// http://docs.blackberry.com/en/developers/deliverables/6022/net/rim/device/api/crypto/doc-files/CryptoTest.html#sampleARC4Encryption
		//
		// Lesson: Stream Ciphers
		// http://www.blackberry.com/developers/docs/7.1.0api/net/rim/device/api/crypto/doc-files/stream.html
		
		if(secretKey.length != 128)
			throw new Exception("GetARC4Keystream() secretKey Length Error");
		//updateStatus("SECRETKEY LENGTH:" + secretKey.length);
	
		// Create the key
	    HMACKey hmackey = new HMACKey(secretKey);
	    
	    // Create the SHA digest
	    SHA256Digest sha256digest = new SHA256Digest();
	    
	    // Now an HMAC can be created, passing in the key and the
	    // SHA digest. Any instance of a digest can be used here.
	    HMAC hMac = new HMAC(hmackey, sha256digest);	
	    		   
	    // The HMAC can be updated much like a digest
	    hMac.update(msg, msgOffset, 128);  // 128bytes length
	
	    // Now get the MAC value.
	    byte[] macValue = hMac.getMAC();
	
	    
	    //updateStatus("RC4 In Key:" + ByteArrayUtilities.byteArrayToHex(macValue));
	    //updateStatus("RC4 In Key LENGTH:" + macValue.length);		    
	    //System.arraycopy(macValue, 0, digest, digestOffset, macValue.length);	   	   
	    /*
	    String dummyST = "7ae5aba04dc882f0fd1b8364bd1f4b9c";
	    byte[] dummy = ByteArrayUtilities.hexToByteArray(dummyST);
	    updateStatus("RC4 Out Key:" + ByteArrayUtilities.byteArrayToHex(dummy));
	    updateStatus("RC4 Out Key LENGTH:" + dummy.length);		   
	    */
	    // 16bytes length
	    ARC4Key arc4key = new ARC4Key( macValue, 0, 16 );
	    //ARC4Key arc4key = new ARC4Key( dummy, 0, 16 );
	    
	    ARC4PseudoRandomSource source = new ARC4PseudoRandomSource(arc4key);
	    
		// Set 1536bytes void data
		source.xorBytes( new byte[1536] );
		
		return source;
	}


	public Chunk ReadARC4EncryptionPacket(PRNGDecryptor decryptStream) throws Exception
	{
		
		//PRNGDecryptor decryptStream = new PRNGDecryptor( arc4keystream, dis );
		Chunk chunk = new Chunk();
		
		// ---- BasicHeader  --------------------------------------------- //
		//BasicHeader basicHeader = new BasicHeader();
		chunk.basicHeader = new BasicHeader();
		
		byte[] buffer_header = new byte[1];
		if(decryptStream.read( buffer_header, 0, 1) != 1)
			throw new Exception("Failed to read BasicHeader");
		totalReadSize += 1;
	//updateStatus("[1]:" + Integer.toHexString((buffer_header[0] & 0xFF)) );	 
		// FMT
		chunk.basicHeader.setFMT( (buffer_header[0] & 0xC0) >> 6 );
	//updateStatus("[1] FMT:" + Integer.toHexString(chunk.basicHeader.getFMT()) );
		
		// chunkStreamID
		byte[] chunkStreamID = new byte[3];
		chunkStreamID[0] = (byte) (buffer_header[0] & 0x3F);
	//updateStatus("[1] chunkStreamID[0]:" + Integer.toHexString(chunkStreamID[0]) );
	
		if(chunkStreamID[0] == 0)
		{
	    	// Chunk stream IDs 64-319 can be encoded in the 2-byte form of the header.
	    	// ID is computed as (the second byte + 64).
	    	//  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 
	    	// +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ 
	    	// |fmt|     0     |  cs id - 64   | 
	    	// +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			byte[] chunkStreamID_2ndbyte = new byte[1];
			if(decryptStream.read( chunkStreamID_2ndbyte, 0, 1) != 1)
				throw new Exception("Failed to read chunkStreamID");
			totalReadSize += 1;
			chunkStreamID[1] = (byte) ((int)(chunkStreamID_2ndbyte[1] & 0xFF) - 64);
			
			chunk.basicHeader.setChunkStreamID( Arrays.copy(chunkStreamID, 0, 2) );
			
		}
		else if(chunkStreamID[0] == 1)
		{    	
	    	// Chunk stream IDs 64-65599 can be encoded in the 3-byte version of this field.
	    	// ID is computed as ((the third byte)*256 + (the second byte) + 64).
	    	//   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 
	    	//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	    	//  |fmt|     1     |          cs id - 64           |
	    	//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			byte[] chunkStreamID_2ndbyte = new byte[2];
			int totalReadNum = 0;
			
			while(totalReadNum < chunkStreamID_2ndbyte.length)
			{
				int readNum = 0;
		    	if((readNum = decryptStream.read( chunkStreamID_2ndbyte, totalReadNum, chunkStreamID_2ndbyte.length - totalReadNum)) == -1)
		    		throw new Exception("Failed to read chunkStreamID");     	
		    	totalReadNum += readNum; 
			}   
			totalReadSize += totalReadNum;
			/*
			if(decryptStream.read( chunkStreamID_2ndbyte, 0, 2) != 1)
				throw new Exception("Failed to read chunkStreamID");    		
			totalReadSize += 2;
			*/
			//NOT IMPLEMENT
		}
		else
		{
	    	// Chunk stream IDs 2-63 can be encoded in the 1-byte version of this field.
	    	//   0 1 2 3 4 5 6 7
	    	//  +-+-+-+-+-+-+-+-+
	    	//  |fmt|   cs id   |
	    	//  +-+-+-+-+-+-+-+-+
			chunk.basicHeader.setChunkStreamID(chunkStreamID[0]);
		}
		//updateStatus("[1] ChunkStreamID:" + ByteArrayUtilities.byteArrayToHex(chunk.basicHeader.getChunkStreamID()) );  	
		//updateStatus("Read BasicHeader:" + ByteArrayUtilities.byteArrayToHex(chunk.basicHeader.getBasicHeader()));    	
	
		
		// ---- MessageHeader --------------------------------------------- //
		chunk.messageHeader = new MessageHeader(chunk.basicHeader.getFMT());    	
		//byte[] buffer = null;
		
		//  Type 0 chunk headers are 11 bytes long.
		if(chunk.basicHeader.getFMT() == 0)
		{
			byte[] buffer = new byte[11];
			int totalReadNum = 0;
	    	
	    	while(totalReadNum < buffer.length)
	    	{
	    		int readNum = 0;
	    		if((readNum = decryptStream.read(buffer, totalReadNum, buffer.length - totalReadNum)) == -1)
	        		throw new Exception("Failed to read MessageHeader");
	    		totalReadNum += readNum;
	    	}
			  	
	    	totalReadSize += totalReadNum;
	    	chunk.messageHeader.setMessageHeader(buffer);
		}
		//  Type 1 chunk headers are 7 bytes long.
		else if(chunk.basicHeader.getFMT() == 1)
		{
			byte[] buffer = new byte[7];
			int totalReadNum = 0;
			
			while(totalReadNum < buffer.length)
			{
				int readNum = 0;
	        	if((readNum = decryptStream.read(buffer, totalReadNum, buffer.length - totalReadNum)) ==  -1)
	        		throw new Exception("Failed to read MessageHeader");
	        	totalReadNum += readNum;
			}
	    	totalReadSize += totalReadNum;
	    	chunk.messageHeader.setMessageHeader(buffer);
		}
		//  Type 2 chunk headers are 3 bytes long.
		else if(chunk.basicHeader.getFMT() == 2)
		{
			byte[] buffer = new byte[3];
			int totalReadNum = 0;
			
			while(totalReadNum < buffer.length)
			{
				int readNum = 0;
	        	if((readNum = decryptStream.read(buffer, totalReadNum, buffer.length - totalReadNum)) == -1)
	        		throw new Exception("Failed to read MessageHeader");
	        	totalReadNum += readNum;	        	
			}
	    	totalReadSize += totalReadNum;
	    	chunk.messageHeader.setMessageHeader(buffer);
		}
		//  Type 3 chunks have no message header.
	   	else if(chunk.basicHeader.getFMT() == 3)
		{
	   		//  DO NOTHING
		}
	//updateStatus("[1] MessageLength: " + ByteArrayUtilities.byteArrayToHex(chunk.messageHeader.getMessageLengthField()));
	//updateStatus("[1] MessageTime: " + ByteArrayUtilities.byteArrayToHex(chunk.messageHeader.getTimeStampField()));
		//updateStatus("- MessageLength: " + ByteArrayUtilities.byteArrayToHex(messageHeader.getMessageLengthField()));
		//updateStatus("- MessageTypeID: " + ByteArrayUtilities.byteArrayToHex(messageHeader.getMessageTypeIDField()));
		
		
		// ---- Chunk Data --------------------------------------------- //
		int messageLength = AMF.DecodeInt24( chunk.messageHeader.getMessageLengthField() );
		byte[] chunkDataBuff = new byte[messageLength];
		int totalReadNum = 0;
		
		while(totalReadNum < messageLength)
		{
			int readNum = 0;
	    	if((readNum = decryptStream.read(chunkDataBuff, totalReadNum, messageLength-totalReadNum)) == -1)
	    		throw new Exception("Failed to read Chunk Data: " + readNum +  
	    				" MSGLength: " + messageLength + 
	    				" FMT: " + chunk.basicHeader.getFMT() +
	    				" chunkStreamID[0]: " + chunkStreamID[0]
	    				);	    	
	    	totalReadNum += readNum; 
		}   
		totalReadSize += totalReadNum;
		
		chunk.chunkData = new ChunkData(chunkDataBuff);
		
		
		
		if(totalReadSize > (reportedReadSize + ClientBW / 2))
		{
			//updateStatus("TotalReadSize: " + totalReadSize);
			SendAcknowledgementPacket(cryptos);
			reportedReadSize += totalReadSize;
		}
		
		//updateStatus("Read ChunkData: " + ByteArrayUtilities.byteArrayToHex(chunkData.getByteArray()));
	
		
		return chunk;
	}


	public void SendARC4EncryptionPacket(PRNGEncryptor os, byte[] msg) throws IOException
	{
		//NoCopyByteArrayOutputStream output = new NoCopyByteArrayOutputStream();
		//ARC4PseudoRandomSource sorce = new ARC4PseudoRandomSource(arc4key);
		//PRNGEncryptor cryptoStream = new PRNGEncryptor( arc4keystream, output );
		
		/*
		int numOfBytesToSend = msg.length;
		int counter = 0;
		updateStatus("SendARC4EncryptionPacket");
		while(numOfBytesToSend != 0)
		{
			if(numOfBytesToSend < RTMP_DEFAULT_CHUNKSIZE)
			{	
				updateStatus("numOfBytesToSend: " + numOfBytesToSend);
				os.write( msg, RTMP_DEFAULT_CHUNKSIZE * counter, numOfBytesToSend );
	    		os.flush();
	    		numOfBytesToSend -= numOfBytesToSend;
	    		counter++;
			}
			else
			{
				updateStatus("numOfBytesToSend2: " + numOfBytesToSend);
				os.write( msg, RTMP_DEFAULT_CHUNKSIZE * counter, RTMP_DEFAULT_CHUNKSIZE );
				os.flush();
				numOfBytesToSend -= RTMP_DEFAULT_CHUNKSIZE;
				counter++;
			}    	
		}
		*/
		os.write( msg, 0, msg.length );
		os.flush();   
	
		/*
		NoCopyByteArrayOutputStream output2 = new NoCopyByteArrayOutputStream();
		PRNGEncryptor cryptoStream2 = new PRNGEncryptor( arc4keystream, output2 );
		cryptoStream2.write( msg, 0, msg.length );
		cryptoStream2.close();
		*/
		//os.write(output.getByteArray(), 0, output.size());
		//os.flush();
		
	}

	public void SendConnectPacket(PRNGEncryptor os, String stationID) throws Exception
	{
		if(stationID == null)
			throw new Exception("放送局が選択されていません");
		
		//String authkey = null;
		ByteArrayOutputStream output = new ByteArrayOutputStream(); 
			
		// Get AuthKey
		//authkey = challengeAuth1();	
		
		// connect body
		Commands.NetConnectionCommands.Connect connect = new Commands.NetConnectionCommands.Connect();
		// Command Object
		connect.startCommandObject();
		connect.setAppProperty(stationID + "/_definst_");
		connect.setFpadProperty(false);
		connect.setCapabilitiesProperty(15.0);
		connect.setAudioCodecsProperty(3191.0);
		connect.setVideoCodecsProperty(252.0);
		connect.setVideoFunctionProperty(1.0);
		connect.endCommandObject();		   
	
		//updateStatus("Connect byteArray Length:" + connect.toByteArray().length);
		
		// Optional User Arguments
		connect.setAuthKey("");
		connect.setAuthKey("");
		connect.setAuthKey("");
		//connect.setAuthKey("eR7l_Oue7JeefqQcYldRyA");
		connect.setAuthKey(authkey);
		
		byte[] connect_byteArray = connect.toByteArray();
		
		//updateStatus("Connect byteArray:" + ByteArrayUtilities.byteArrayToHex(connect.toByteArray()));
		//updateStatus("Connect byteArray Length:" + connect.toByteArray().length);
		   
		
		// ---- BasicHeader  --------------------------------------------- //
		BasicHeader bh = new BasicHeader();
		bh.setFMT(0x00);
		bh.setChunkStreamID((byte) 0x03);
		
		//updateStatus("BasicHeader:" + ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ));
	
		// ---- MessageHeader  --------------------------------------------- //
		MessageHeader msgheader = new MessageHeader(MessageHeader.TYPE0);
		msgheader.setMessageLengthField(connect_byteArray.length);
		//updateStatus("MessageLength:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageLengthField()));
	
		msgheader.setMessageTypeIDField(0x14);  //Invoke
		//updateStatus("MessageTypeID:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageTypeIDField()));
		//updateStatus("MessageHeader:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageHeader()));
		
		// COMBINE
		output.write( bh.getBasicHeader() );
		output.write( msgheader.getMessageHeader() );
		
		
		// ---- One More Thing --------------------------------------------- //
		//  どうしてもうまくいかない実装。 (rtmp, flash video, windows runtime, etc…) - モノトーンの伝説日記
		//  http://mntone.hateblo.jp/entry/2013/08/22/225129
		//
		//  (3) chunkSize に注意。
		//  デフォルト値は 128 bytes。ヘッダーを除くデータ部分から 128 bytes ごとに 
		//  (2n+1) 回目は 0xc3、2n 回目は 0xc4 を挿入 (たぶん)。これがないと正常に認識されないです。
		//
		//  -> 0xC3は、(0xC0 | chunkStreamID) の結果		
		if(connect_byteArray.length > RTMP_DEFAULT_CHUNKSIZE)
		{
			int numOfBytesToWrite = connect_byteArray.length;
			int counter = 0;
			byte[] header = new byte[1];
			header[0] = (byte) (0xC0 | 0x03);  // 0x03 = chunkStreamID
			
			while(numOfBytesToWrite > 0)
			{
				if(numOfBytesToWrite < RTMP_DEFAULT_CHUNKSIZE)
				{
					output.write(connect_byteArray, RTMP_DEFAULT_CHUNKSIZE * counter, numOfBytesToWrite);
					numOfBytesToWrite -= numOfBytesToWrite;
					counter++;
				}
				else
				{
					output.write(connect_byteArray, RTMP_DEFAULT_CHUNKSIZE * counter, RTMP_DEFAULT_CHUNKSIZE);
					output.write(header);
					numOfBytesToWrite -= RTMP_DEFAULT_CHUNKSIZE;
					counter++;
				}
			}			
		}		
		
		//updateStatus("ConnectionPacket: " + ByteArrayUtilities.byteArrayToHex(output.toByteArray()));
		//updateStatus("ConnectionPacket Length: " + output.toByteArray().length);
		
		// ARC4Encrypte
		SendARC4EncryptionPacket(os, output.toByteArray());
		
		/*
		try {
			os.write(output.getByteArray(), 0, output.size());
			os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		if(connect != null)
			connect = null;
		if(bh != null)
			bh = null;
		if(msgheader != null)
			msgheader = null;
		if(output != null)
			output = null;
		
		updateStatus("SEND CONNECTPACKET");
		
	}


	public void SendAcknowledgementPacket(PRNGEncryptor os) throws IOException
	{
		// ---- Payload  --------------------------------------------- //
		// Acknowledgement (3)
		// The client or the server MUST send an acknowledgment to the peer
		// after receiving bytes equal to the window size.  The window size is
		// the maximum number of bytes that the sender sends without receiving
		// acknowledgment from the receiver.  This message specifies the
		// sequence number, which is the number of the bytes received so far.
		//
		//  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
		// +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		// |                   sequence number (4 bytes)                   |
		// +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		byte[] payload = new byte[4];
		payload = AMF.EncodeInt32(totalReadSize);    	
	
		// ---- BasicHeader  --------------------------------------------- //
		BasicHeader bh = new BasicHeader();
		bh.setFMT(0x00);
		bh.setChunkStreamID((byte) 0x02);
		
		//updateStatus("BasicHeader:" + ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ));
	
		// ---- MessageHeader  --------------------------------------------- //
		MessageHeader msgheader = new MessageHeader(bh.getFMT());
		msgheader.setMessageLengthField(payload.length);
		//updateStatus("MessageLength:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageLengthField()));
	
		msgheader.setMessageTypeIDField(0x03);  //Invoke
		//updateStatus("MessageTypeID:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageTypeIDField()));
		//updateStatus("MessageHeader:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageHeader()));
		
		updateStatus("SendWindowAcknowledgementSizePacket: " + 
				ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ) +
				ByteArrayUtilities.byteArrayToHex( msgheader.getMessageHeader() ) +
				ByteArrayUtilities.byteArrayToHex( payload ) );
		
		// Send
		os.write( bh.getBasicHeader() );
		os.write( msgheader.getMessageHeader() );
		os.write( payload );
		os.flush();
		
		
	}

	public void SendWindowAcknowledgementSizePacket(PRNGEncryptor os) throws IOException
	{
		
		// ---- Payload  --------------------------------------------- //
		
		//  Window Acknowledgement Size (5)
		//  The client or the server sends this message to inform the peer of the
		//  window size to use between sending acknowledgments.  The sender
		//  expects acknowledgment from its peer after the sender sends window
		//  size bytes.  The receiving peer MUST send an Acknowledgement
		//  after receiving the indicated number of bytes since
		//  the last Acknowledgement was sent, or from the beginning of the
		//  session if no Acknowledgement has yet been sent.
		//
		//   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
		//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		//  |             Acknowledgement Window size (4 bytes)             |
		//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		
		byte[] payload = new byte[4];
		payload = AMF.EncodeInt32(serverWindowAcknowledgementSize);
		    	
		
		// ---- BasicHeader  --------------------------------------------- //
		BasicHeader bh = new BasicHeader();
		bh.setFMT(0x00);
		bh.setChunkStreamID((byte) 0x02); // invoke (CTRL channel)
		
		//updateStatus("BasicHeader:" + ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ));
	
		// ---- MessageHeader  --------------------------------------------- //
		MessageHeader msgheader = new MessageHeader(bh.getFMT());
		msgheader.setMessageLengthField(payload.length);
		//updateStatus("MessageLength:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageLengthField()));
	
		msgheader.setMessageTypeIDField(0x05);
		//updateStatus("MessageTypeID:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageTypeIDField()));
		//updateStatus("MessageHeader:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageHeader()));
		
		updateStatus("SendWindowAcknowledgementSizePacket: " + 
				ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ) +
				ByteArrayUtilities.byteArrayToHex( msgheader.getMessageHeader() ) +
				ByteArrayUtilities.byteArrayToHex( payload ) );
		
		// Send
		os.write( bh.getBasicHeader() );
		os.write( msgheader.getMessageHeader() );
		os.write( payload );
		os.flush();
	}

	public void SendPingResponcePacket(PRNGEncryptor os, byte[] val) throws IOException
	{    	    	    	
		// ---- BasicHeader  --------------------------------------------- //
		BasicHeader bh = new BasicHeader();
		bh.setFMT(0x00);
		bh.setChunkStreamID((byte) 0x02);  // Invoke (CTRL channel)
		
		//updateStatus("BasicHeader:" + ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ));
	
		// ---- MessageHeader  --------------------------------------------- //
		MessageHeader msgheader = new MessageHeader(bh.getFMT());
		msgheader.setMessageLengthField(val.length);
		//updateStatus("MessageLength:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageLengthField()));
	
		msgheader.setMessageTypeIDField(0x04);  // Invoke (CTRL)
		//updateStatus("MessageTypeID:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageTypeIDField()));
		//updateStatus("MessageHeader:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageHeader()));
		/*
		updateStatus("SendPingResponcePacket: " + 
				ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ) +
				ByteArrayUtilities.byteArrayToHex( msgheader.getMessageHeader() ) +
				ByteArrayUtilities.byteArrayToHex( AMF.EncodeInt16(0x07) ) +
				ByteArrayUtilities.byteArrayToHex( val )
				);
		*/
		// Send
		os.write( bh.getBasicHeader() );
		os.write( msgheader.getMessageHeader() );
		os.write( AMF.EncodeInt16(0x07) );  // Invoke (Ping)
		os.write( val );
		os.flush();
	}

	public void ClientPacket(Chunk chunk) throws IOException
	{
		int messageTypeID = chunk.messageHeader.getMessageTypeIDField();
		switch (messageTypeID)
		{
			case 0x01:
			{
				//  Set Chunk Size (1)
				//  Protocol control message 1, Set Chunk Size, is used to notify the
				//  peer of a new maximum chunk size.
				//
				//   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
				//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
				//  |0|                    chunk size (31 bits)                     |
				//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
				
				int tmp = AMF.DecodeInt32(chunk.chunkData.getByteArray());
				updateStatus("received: chunk size change to " + tmp);
				break;
			}					
			case 0x04:
			{
				//  User Control Messages (4)
				//  RTMP uses message type ID 4 for User Control messages.
				//  These messages contain information used by the RTMP streaming layer.				
				
				HandleCtrlMessage(chunk.chunkData);
				break;
			}	
			case 0x05:
			{
				//  Window Acknowledgement Size (5)
				//  The client or the server sends this message to inform the peer of the
				//  window size to use between sending acknowledgments.  The sender
				//  expects acknowledgment from its peer after the sender sends window
				//  size bytes.  The receiving peer MUST send an Acknowledgement
				//  after receiving the indicated number of bytes since
				//  the last Acknowledgement was sent, or from the beginning of the
				//  session if no Acknowledgement has yet been sent.
				//
				//   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
				//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
				//  |             Acknowledgement Window size (4 bytes)             |
				//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
				
				serverWindowAcknowledgementSize = AMF.DecodeInt32(chunk.chunkData.getByteArray());
				updateStatus("Server Acknowledgement Size: " + serverWindowAcknowledgementSize);
				break;
			}	
			case 0x06:
			{
				//  Set Peer Bandwidth (6)
				//  The client or the server sends this message to limit the output
				//  bandwidth of its peer.  The peer receiving this message limits its
				//  output bandwidth by limiting the amount of sent but unacknowledged
				//  data to the window size indicated in this message.  The peer
				//  receiving this message SHOULD respond with a Window Acknowledgement
				//  Size message if the window size is different from the last one sent
				//  to the sender of this message.
				//
				//   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
				//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
				//  |                  Acknowledgement Window size                  |
				//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
				//  |  Limit Type   |
			    //  +-+-+-+-+-+-+-+-+
				
				ClientBW = AMF.DecodeInt32( Arrays.copy(chunk.chunkData.getByteArray(), 0, 4));
				int ClientBW2 = 0;
				if(chunk.chunkData.getByteArray().length > 4)				
					ClientBW2 = chunk.chunkData.getByteArray()[4];				
				else				
					ClientBW2 = -1;
				
				updateStatus("client BW = " + ClientBW + " " + ClientBW2);
				break;
			}
			case 0x08:
			{
				ByteArrayInputStream input = null;
				
				input = new ByteArrayInputStream(chunk.chunkData.getByteArray());
				int chunkDataLength = chunk.chunkData.getByteArray().length;
				
				// ---- AudioTagHeader ------------------------------------------ //				
				// About AudioTagHeader, See...
				// Adobe Flash Video File Format Specification (PDF)
				// http://www.adobe.com/devnet/f4v.html
				// E.4.2.1 AUDIODATA				
				//
				//  0 1 2 3 4 5 6 7 
				// +-+-+-+-+-+-+-+-+
				// |1|2| 3 |   4   | 
				// +-+-+-+-+-+-+-+-+	
				// 1 SoundType(1bit)    0 = Mono sound, 1 = Stereo sound
				// 2 SoundSize(1bit)    0 = 8-bit samples, 1 = 16-bit samples
				// 3 SoundRate(2bits)   0 = 5.5 kHz, 1 = 11 kHz, 2 = 22 kHz, 3 = 44 kHz
				// 4 SoundFormat(4bits) 0 = Linear PCM, platform endian, 1 = ADPCM, 2 = MP3
				//                      3 = Linear PCM, little endian, 4 = Nellymoser 16 kHz mono,
				//                      5 = Nellymoser 8 kHz mono, 6 = Nellymoser, 7 = G.711 A-law logarithmic PCM,
				//                      8 = G.711 mu-law logarithmic PCM, 9 = reserved, 10 = AAC, 11 = Speex,
				//                      14 = MP3 8 kHz, 15 = Device-specific sound
				byte[] audioTagHeader = new byte[1];
				if(-1 == input.read(audioTagHeader))
				{
					updateStatus("DON'T read AudioTagHeader. SKIP.");
					break;
				}
				
				byte SoundType = (byte) (audioTagHeader[0] & 0x01);
				byte SoundSize = (byte) ((audioTagHeader[0] & 0x02) >> 1);
				byte SoundRate = (byte) ((audioTagHeader[0] & 0x0C) >> 2);
				byte SoundFormat = (byte) ((audioTagHeader[0] & 0xF0) >> 4);
				
				/*
				updateStatus("SoundType: " + Integer.toHexString(SoundType) +
						" SoundSize: " + Integer.toHexString(SoundSize) + 
						" SoundRate: " + Integer.toHexString(SoundRate) + 
						" SoundFormat: " + Integer.toHexString(SoundFormat)
						);
				*/
				
				// ---- AACPacketType ------------------------------------------ //
				// AACPacketType(8bits) 0 = AAC sequence header, 1 = AAC raw
				byte[] AACPacketType = new byte[1];
				if(-1 == input.read(AACPacketType))
				{
					updateStatus("DON'T read AACPacketType");
					break;	
				}
				//updateStatus("AACPacketType: " + Integer.toHexString(AACPacketType[0]));
				if(AACPacketType[0] == 0x00)
				{
					updateStatus("NOT AAC RAW DATA. SKIP.");
					break;
				}
					
								
				// ---- ADTS Header ------------------------------------------ //
				byte[] adtsHeader = new byte[7];
				
				int  sync_word          = 0x0FFF; // (12bits) invoke 
				byte ID                 = 0x00;  // (1bit)  0:MPEG4, 1:MPEG2
				byte layer              = 0x00;  // (2bits) invoke
				byte protection_absent  = 0x01;  // (1bit)  0:CRC保護あり, 1:CRC保護なし
				byte profile            = 0x01;  // (2bits) 00:MAIN, 01:LC, 10:SSR, 11:reserved(LTP))
				byte sampling_freq      = 0x06;  // (4bits) 04:44100, 06:24000
				byte private_bit        = 0x00;  // (1bit)  0:なし, 1:あり
				byte channel_conf       = 0x02;  // (3bits) 
				byte original_copy      = 0x00;  // (1bit)  0:はい, 1:いいえ
				byte home               = 0x00;  // (1bit)  invoke
				byte copyright_id_bit   = 0x00;  // (1bit)
				byte copyright_id_start = 0x00;  // (1bit)
				int  aac_frame_length   = 7 + (chunkDataLength - 2);  // (13bits) フレームサイズ (header7bytes含む)
				int  adts_buffer_fullness = 0x07FF;  // (11bits) バッファサイズ(VBR=0x7FF)
				byte no_raw_data_block_in_frame = 0x00;  // (2bytes) データブロックまでのオフセット
				
				//updateStatus("aac_frame_length: " + aac_frame_length);
				
				adtsHeader[0] = (byte) 0xFF;
				adtsHeader[1] = (byte) (((sync_word << 4) & 0xF0) | ((ID << 3) & 0x08) | ((layer << 1) & 0x06) | (protection_absent & 0x01)); 
				adtsHeader[2] = (byte) (((profile << 6) & 0xC0) | ((sampling_freq << 2) & 0x3C) | ((private_bit << 1) & 0x02) | ((channel_conf >> 2) & 0x01));
				adtsHeader[3] = (byte) ((((channel_conf & 0x03) << 6) & 0xC0) | ((original_copy << 5) & 0x20) | ((home << 4) & 0x10) | ((copyright_id_bit << 3) & 0x08) | ((copyright_id_start << 2) & 0x04) | ((aac_frame_length & 0x1800) >> 11));
				adtsHeader[4] = (byte) ((aac_frame_length & 0x07F8) >> 3);
				adtsHeader[5] = (byte) (((aac_frame_length & 0x07) << 5) | ((adts_buffer_fullness & 0x07C0) >> 6));
				adtsHeader[6] = (byte) (((adts_buffer_fullness & 0x3F) << 2) | (no_raw_data_block_in_frame & 0x03));
				//updateStatus("ADTS Header: " + ByteArrayUtilities.byteArrayToHex(adtsHeader));
				
				//Arrays.fill(nocopyba, (byte) 0x00);
				aacAudioDataLength = 0;
				System.arraycopy(adtsHeader, 0, nocopyba, aacAudioDataLength, adtsHeader.length);
				aacAudioDataLength += adtsHeader.length;
				System.arraycopy(chunk.chunkData.getByteArray(), 2, nocopyba, aacAudioDataLength, (chunkDataLength - 2));
				aacAudioDataLength += (chunkDataLength - 2);
				
				break;				
			}
			case 0x12:
			{
				ByteArrayInputStream input = null;
				try {
					input = new ByteArrayInputStream(chunk.chunkData.getByteArray());
			    	//int numOfByteToDecode = input.available();
			    	//updateStatus("HandleInvoke LENGTH: " + numOfByteToDecode);
			    	//updateStatus("HandleInvoke: " + ByteArrayUtilities.byteArrayToHex(hoge));
			    	
					// ---- Command Name ------------------------------------------ //
			    	input.mark(16);
			    	if(input.read() != AMF.STRING_MARKER)
			    		break;
			    	byte[] length = new byte[2];
			    	input.read(length);
			    	byte[] string = new byte[AMF.DecodeInt16(length)];
			    	input.read(string);			    	
			    	//input.reset();
			    	updateStatus("META: " + new String(string));
			    	 		
			    	//HandleInvoke(input);
			    	
				} finally {
					input = null;
				}
				break;
			}
			case 0x14:
			{
				ByteArrayInputStream input = null;
				try {
					byte[] chunkData = chunk.chunkData.getByteArray();
					input = new ByteArrayInputStream(chunkData);
			    	//int numOfByteToDecode = input.available();
			    	//updateStatus("HandleInvoke LENGTH: " + numOfByteToDecode);
			    	//updateStatus("HandleInvoke: " + ByteArrayUtilities.byteArrayToHex(hoge));
			    	
					// ---- Command Name ------------------------------------------ //
			    	input.mark(16);
			    	if(input.read() != AMF.STRING_MARKER)
			    		break;
			    	byte[] length = new byte[2];
			    	input.read(length);
			    	byte[] string = new byte[AMF.DecodeInt16(length)];
			    	input.read(string);			    	
			    	//input.reset();
			    	updateStatus("Server return: " + new String(string));
			    	
			    	// ---- Transaction ID ---------------------------------------- //
			    	if(input.read() != AMF.NUMBER_MARKER)
			    		break;
			    	byte[] number = new byte[8];
			    	input.read(number);
			    	double transactionID = AMF.DecodeNumber(number);
			    	
			    	//input.reset();
			    	//updateStatus("Transaction ID: " + ByteArrayUtilities.byteArrayToHex(number));			    	
			    	updateStatus("Transaction ID: " + String.valueOf(transactionID) );			    	
			    	
			    	// ---- Send Commands ---------------------------------------- //		    	
			    	if(new String(string).equals("_result") && transactionID == TRANSACTION_ID_CONNECT)
			    	{
			    		updateStatus("equals _result & Transacition ID 1.0");
			    		SendWindowAcknowledgementSizePacket(cryptos);
			    		Commands.NetConnectionCommands.SendCreateStreamCommand(cryptos);
			    	}
			    	
			    	if(new String(string).equals("_result") && transactionID == TRANSACTION_ID_CREATESTREAM)
			    	{
			    		updateStatus("equals _result & Transacition ID 2.0");
			    		
			    		// 残りのすべてのバイトを読み出し
			    		byte[] tmp = new byte[input.available()];
			    		input.read(tmp);
			    		
			    		// Stream IDを取得 (Stream IDはメッセージの最後に送られてくるので、逆算して取得)
			    		streamID = AMF.DecodeNumber(Arrays.copy(tmp, tmp.length - 8, 8));
			    		updateStatus("Stream ID: " + String.valueOf( streamID ) );	
			    		
			    		// Play!!
			    		Commands.NetStreamCommands.SendPlayCommand(cryptos, 1);
			    	}
			    	
			    	if(new String(string).equals("onStatus") && transactionID == 0.0)
			    	{
			    		updateStatus("equals onStatus & Transacition ID 0.0");
			    		//byte[] hoge = Commands.NetStreamCommands.SendPlayCommand(cryptos, 1);			    		
			    		//updateStatus("SendPlayCommand: " + ByteArrayUtilities.byteArrayToHex(hoge));
			    		/*
			    		while(input.available() != 0)
				    	{    		
				    		HandleInvoke(input);
				    	}
				    	*/
			    	}
			    	/*
			    	while(input.available() != 0)
			    	{    		
			    		HandleInvoke(input);
			    	}
			    	*/
				} finally {
					input = null;
				}
				break;
			}
			default:
				updateStatus("Unknown messageTypeID: " + messageTypeID);
				break;
		   }
	}

	public void HandleCtrlMessage(ChunkData chunkData) throws IOException
	{
		//updateStatus("HandleCtrlMessage: " + ByteArrayUtilities.byteArrayToHex(chunkData.getByteArray()));
		
		//  The first 2 bytes of the message data are used to identify the Event type.
		//  +------------------------------+-------------------------
	    //  |     Event Type (16 bits)     | Event Data
	    //  +------------------------------+-------------------------
		
		int eventType = AMF.DecodeInt16( Arrays.copy(chunkData.getByteArray(), 0, 2) );
		int tmp = 0;
		
		switch (eventType)
		{
			case 0x00:
			{
				// Stream Begin (=0)
				// The 4 bytes of event data represent the ID of the stream on which playback has ended.
				tmp = AMF.DecodeInt32( Arrays.copy(chunkData.getByteArray(), 2, 4) );
				updateStatus("[CTRL] Stream Begin " + tmp);
				break;
			}
			case 0x01:
			{
				// Stream EOF (=1)
				// The 4 bytes of event data represent the ID of the stream on which playback has ended.
				tmp = AMF.DecodeInt32( Arrays.copy(chunkData.getByteArray(), 2, 4) );
				updateStatus("[CTRL] Stream EOF " + tmp);
				break;
			}
			case 0x06:
			{
				// PingRequest(=6)
				// The server sends this event to test whether the client is reachable.
				// Event data is a 4-byte timestamp, representing the local server time
				// when the server dispatched the command. The client responds with 
				// PingResponse on receiving MSGPingRequest.
				//tmp = AMF.DecodeInt32( Arrays.copy(chunkData.getByteArray(), 2, 4) );
				updateStatus("[CTRL] PingRequest");
				SendPingResponcePacket(cryptos, Arrays.copy(chunkData.getByteArray(), 2, 4));				
				break;
	
			}
			default:
				updateStatus("Unknown EventType: " + eventType);
				break;
		}
				
	}

	public void HandleInvoke(ByteArrayInputStream input) throws IOException
	{
		updateStatus("HandleInvoke");
		//ByteArrayInputStream input = new ByteArrayInputStream(chunkData.getByteArray());
		//int numOfByteToDecode = input.available();
		/*
		byte[] hoge = new byte[numOfByteToDecode];
		input.read(hoge);
		*/
		//updateStatus("HandleInvoke LENGTH: " + numOfByteToDecode);
		//updateStatus("HandleInvoke: " + ByteArrayUtilities.byteArrayToHex(hoge));
		
		/*
		if(input.read() != AMF.STRING_MARKER)
			return;
		input.reset();
		*/
	
		
		// Read 1byte
		switch(input.read())
		{
			case -1:
			{
				updateStatus("ERROR!!!");
				break;
			}	
			case AMF.NUMBER_MARKER:
			{
				byte[] output = new byte[8];
				input.read(output);
				updateStatus("NUMBER: " + Double.toString(AMF.DecodeNumber(output)) );
				updateStatus("NUMBER: 00" + ByteArrayUtilities.byteArrayToHex(output));
				updateStatus("available: " + input.available());
				break;
			}	
			case AMF.BOOLEAN_MARKER:
			{    			
				byte[] output = new byte[1];
				input.read(output);
				
				if(output[0] == 0x01)
				{
					updateStatus("BOOLEAN: TRUE");
				}
				else if(output[0] == 0x00)
				{
					updateStatus("BOOLEAN: FALSE");
				}    			
				break;
			}
			case AMF.STRING_MARKER:
			{
				byte[] length = new byte[2];
				input.read(length);
				int len = AMF.DecodeInt16(length);
				byte[] val = new byte[len];
				input.read(val);
				updateStatus("STRING: " + new String(val));
				updateStatus("STRING: 02" + ByteArrayUtilities.byteArrayToHex(length) + ByteArrayUtilities.byteArrayToHex(val));
				updateStatus("available: " + input.available());
				break;
			}
			case AMF.OBJECT_MARKER:
			{
				updateStatus("OBJECT");
				
				// 終端バイト列
				byte[] OBJEndMarker = new byte[]{0x00, 0x00, 0x09};
				
				while( input.available() > 0 )
				{
	    			// Name-value pairsなので、まずName部を取得。
	    			byte[] length = new byte[2];
	    			input.read(length);
	    			int len = AMF.DecodeInt16(length);
	    			byte[] val = new byte[len];
	    			input.read(val);
	    			updateStatus("Name: " + new String(val));
	    			updateStatus("Name: " + ByteArrayUtilities.byteArrayToHex(length) + ByteArrayUtilities.byteArrayToHex(val));
	    			
	    			// Value部を処理
	    			HandleInvoke(input);
	    			
	    			// 次の3バイトが、終端バイト列(0x00, 0x00, 0x09)に合致する場合はブレイク
	    			input.mark(8);	    			
	    			byte[] OBJEnd = new byte[3];
	    			input.read(OBJEnd);
				
	    			if(Arrays.equals(OBJEnd, OBJEndMarker))
	    			{
	    				updateStatus("Reached END");
	    				break;
	    			}
	    			else
	    			{
	    				input.reset();
	    			}
	    			
				}
				break;
			}	
			case AMF.NULL_MARKER:
				updateStatus("NULL "+ "available: " + input.available());
				break;
				
			case AMF.ECMA_ARRAY_MARKER:
			{
				updateStatus("ECMA_ARRAY");
				// 32bit
				byte[] associative_count = new byte[4];     				
				input.read(associative_count);
				updateStatus("associative_count: " + Integer.toHexString( AMF.DecodeInt32(associative_count) ));
				
				// Name-value pairsなので、まずName部を取得。
				byte[] length = new byte[2];
				input.read(length);
				int len = AMF.DecodeInt16(length);
				byte[] val = new byte[len];
				input.read(val);
				updateStatus("Name: " + new String(val));
				updateStatus("Name: " + ByteArrayUtilities.byteArrayToHex(length) + ByteArrayUtilities.byteArrayToHex(val));
				
				// Value部を処理
				HandleInvoke(input);
				
				// 次の3バイトが、終端バイト列(0x00, 0x00, 0x09)に合致する場合はブレイク
				input.mark(8);	    			
				byte[] OBJEnd = new byte[3];
				input.read(OBJEnd);
			
				// 終端バイト列
				byte[] OBJEndMarker = new byte[]{0x00, 0x00, 0x09};
				
				if(Arrays.equals(OBJEnd, OBJEndMarker))
				{
					updateStatus("Reached END");
					break;
				}
				else
				{
					input.reset();
				}
			}    		
			default:
				updateStatus("Unknown Marker");
				break;
		}
		
	}

	public void Handshake(String url) throws Exception
	{
//		new Thread()
//		{
//			public void run()
//			{
				//try {
	int digoff = 2;
	int dhoff  = 2;
	
	//String url = "socket://netradio-fm-flash.nhk.jp:1935";
	//String url = "socket://w-radiko.smartstream.ne.jp:1935";
	//url = "socket://" + url + ":1935;deviceside=false;connectionUID=GPMDSAP01";
	url = "socket://" + url + ":1935";
	//String url = "socket://192.168.10.3:1935";
	//String url = "socket://www.google.co.jp:80";
	
	// Create ConnectionFactory
	//factory = new ConnectionFactory();

	// use the factory to get a connection
	ConnectionDescriptor conDescriptor = _factory.getConnection( url );
		

	if ( conDescriptor != null ) {

	   // connection succeeded
	   //int transportUsed = conDescriptor.getTransportDescriptor().getTransportType();

	   // using the connection
	   //HttpConnection  httpCon = (HttpConnection) conDescriptor.getConnection();
	   sc = (SocketConnection) conDescriptor.getConnection();

	   //updateStatus("GetAdress:" + sc.getAddress());	   
	   
	   //try {
		   dis = sc.openInputStream();
		   os = sc.openOutputStream();		  
		   

		   // write chunk C0
		   byte[] chunk_C0 = new byte[1];
		   byte[] chunk_C1 = new byte[1536];
		   byte[] chunk_C2 = new byte[1536];
   		   
		   chunk_C0[0] = (byte) 0x06;  // RTMPE=0x06 RTMP=0x03
	    			   
		   // Write time data (4bytes)
		   int datel = (int) (System.currentTimeMillis() / 1000); // msec/1000 = sec
		   
		   //updateStatus("NOW:" + Integer.toString(datel));
		   //updateStatus("HEX_STRING:" + Integer.toHexString(datel));
		     		   
		   chunk_C1[3] = (byte) (0x000000FF & (datel));
		   chunk_C1[2] = (byte) (0x000000FF & (datel >>> 8));
		   chunk_C1[1] = (byte) (0x000000FF & (datel >>> 16));
		   chunk_C1[0] = (byte) (0x000000FF & (datel >>> 24));		  
		   
		   // Write RTMPE special data
		   chunk_C1[4] = (byte) 0x80;  // 128
		   chunk_C1[6] = (byte) 0x03;  // 3
		   chunk_C1[7] = (byte) 0x02;  // 2

		   
		   // Write random data (1528bytes)
		   Random rand = new Random();
		   for (int i = 8; i < 1536; i+=4) {
			   int randInt = rand.nextInt();			   
			   chunk_C1[i+3] = (byte) (0x000000FF & (randInt));
    		   chunk_C1[i+2] = (byte) (0x000000FF & (randInt >>> 8));
    		   chunk_C1[i+1] = (byte) (0x000000FF & (randInt >>> 16));
    		   chunk_C1[i]   = (byte) (0x000000FF & (randInt >>> 24));			   
		   }    		   
		   
		   // DH & MHAC SHA256
		   DHKeyPair dhkeypair = crypt(chunk_C1);
		   
		   //byte[] pub_key = new byte[dhkeypair.getDHPublicKey().getPublicKeyData().length];
		   //pub_key = dhkeypair.getDHPublicKey().getPublicKeyData();
			
		   DHPublicKey mydhpubkey = dhkeypair.getDHPublicKey();
		   mydhpubkey.verify();    		   
		   
		   byte[] pub_key = mydhpubkey.getPublicKeyData();
		   if(pub_key.length != 128)
			   return;
		   
		   
			//updateStatus("PublicKey:" + ByteArrayUtilities.byteArrayToHex(pub_key));
			//updateStatus("PublicKey:" + ByteArrayUtilities.byteArrayToHex(dh_keypair.getDHPublicKey().getPublicKeyData()));
			//updateStatus("PublicKey_LENGTH:" + dh_keypair.getDHPublicKey().getPublicKeyData().length);
			
			//updateStatus("PrivateKey:" + ByteArrayUtilities.byteArrayToHex(dh_keypair.getDHPrivateKey().getPrivateKeyData()));
			//updateStatus("PrivateKey_LENGTH:" + dh_keypair.getDHPrivateKey().getPrivateKeyData().length);
			   			    		  
		   int dhposClient = GetDHOffset2(chunk_C1);  //472
		   System.arraycopy(pub_key, 0, chunk_C1, dhposClient, pub_key.length);
		       		   
		   
		   int digestPosClient = GetDigestOffset2(chunk_C1);
		   CalculateDigest(digestPosClient, chunk_C1, GenuineFPKey, chunk_C1, digestPosClient);    		   

		  
		   os.write(chunk_C0, 0, 1);
		   os.flush();
		   updateStatus("Flush C0");
		   os.write(chunk_C1, 0, chunk_C1.length);
		   os.flush();
		   updateStatus("Flush C1");
		       		     		  
		   
		   byte[] chunk_S0 = new byte[1];
		   //dis.readFully(chunk_S0);
		   int totalReadNumS0 = 0;			
		   while(totalReadNumS0 < chunk_S0.length)
		   {
			   int readNum = 0;
			   if((readNum = dis.read( chunk_S0, totalReadNumS0, chunk_S0.length - totalReadNumS0)) == -1)
				   throw new Exception("Failed to read chunk_S0");     	
			   totalReadNumS0 += readNum; 
		   }   
		   totalReadSize += totalReadNumS0;			
		   //updateStatus("S0:" + ByteArrayUtilities.byteArrayToHex(chunk_S0));
		   
		   byte[] chunk_S1 = new byte[1536];
		   //dis.readFully(chunk_S1);		   
			int totalReadNumS1 = 0;			
			while(totalReadNumS1 < chunk_S1.length)
			{
				int readNum = 0;
				if((readNum = dis.read( chunk_S1, totalReadNumS1, chunk_S1.length - totalReadNumS1)) == -1)
					throw new Exception("Failed to read chunk_S1");     	
				totalReadNumS1 += readNum; 
			}   
			totalReadSize += totalReadNumS1;		   
		   //updateStatus("S1:" + ByteArrayUtilities.byteArrayToHex(chunk_S1));
		   
		   updateStatus("FMS Version:" + Integer.toString(chunk_S1[4]) +"."+ Integer.toString(chunk_S1[5]) +"."+ Integer.toString(chunk_S1[6]) +"."+ Integer.toString(chunk_S1[7]));
		   
		   
		   byte[] GenuineFMSKey = new byte[] {
				   (byte)0x47, (byte)0x65, (byte)0x6E, (byte)0x75, (byte)0x69, (byte)0x6E, (byte)0x65, (byte)0x20,
				   (byte)0x41, (byte)0x64, (byte)0x6F, (byte)0x62, (byte)0x65, (byte)0x20, (byte)0x46, (byte)0x6C,
				   (byte)0x61, (byte)0x73, (byte)0x68, (byte)0x20, (byte)0x4D, (byte)0x65, (byte)0x64, (byte)0x69,
				   (byte)0x61, (byte)0x20, (byte)0x53, (byte)0x65, (byte)0x72, (byte)0x76, (byte)0x65, (byte)0x72,
				   (byte)0x20, (byte)0x30, (byte)0x30, (byte)0x31
		   };  // 36 Genuine Adobe Flash Media Server 001    		  
		   /*
				   (byte)0xF0, (byte)0xEE, (byte)0xC2, (byte)0x4A, (byte)0x80, (byte)0x68, (byte)0xBE, (byte)0xE8,
				   (byte)0x2E, (byte)0x00, (byte)0xD0, (byte)0xD1, (byte)0x02, (byte)0x9E, (byte)0x7E, (byte)0x57,
				   (byte)0x6E, (byte)0xEC, (byte)0x5D, (byte)0x2D, (byte)0x29, (byte)0x80, (byte)0x6F, (byte)0xAB,
				   (byte)0x93, (byte)0xB8, (byte)0xE6, (byte)0x36, (byte)0xCF, (byte)0xEB, (byte)0x31, (byte)0xAE
		   };  // 68
		   */
		   
		   int digestPosServer = 0;
		   if(digoff == 1)
			   digestPosServer = GetDigestOffset1(chunk_S1);
		   else if(digoff == 2)
			   digestPosServer = GetDigestOffset2(chunk_S1);
		   
		   updateStatus("digestPosServer:" + Integer.toString(digestPosServer) + "GenuineFMSKey:" + GenuineFMSKey.length);
		   
		   if(!VerifyDigest(digestPosServer, chunk_S1, GenuineFMSKey))
		   {
			   digestPosServer = GetDigestOffset1(chunk_S1);
			   digoff = 1;
			   dhoff  = 1;
			   
			   if(!VerifyDigest(digestPosServer, chunk_S1, GenuineFMSKey))
			   {
				   updateStatus("VerifyDigest ERROR\n");
			   }
		   }
		   
		   /* do Diffie-Hellmann Key exchange for encrypted RTMP */
		   byte[] secretKey = new byte[128];
		   int dhposServer = 0;
		   
		   if(dhoff == 1)
			   dhposServer = GetDHOffset1(chunk_S1);    		
		   else if(dhoff ==2)
			   dhposServer = GetDHOffset2(chunk_S1);

		   updateStatus("dhposServer:" + Integer.toString(dhposServer));
		   
		   byte[] pubkeyBn = Arrays.copy(chunk_S1, dhposServer, 128);
		   //updateStatus("pubkeyBn:" + ByteArrayUtilities.byteArrayToHex(pubkeyBn));
		   
		   DHPublicKey dhpubkey = null;
		   //try {
			   dhpubkey = new DHPublicKey(dhkeypair.getDHCryptoSystem(), pubkeyBn);
			   dhpubkey.verify();
			   //updateStatus("dhpubkey:" + ByteArrayUtilities.byteArrayToHex(dhpubkey.getPublicKeyData()));
			   secretKey = DHKeyAgreement.generateSharedSecret(dhkeypair.getDHPrivateKey(), dhpubkey, false);
			   /*
		   } catch (InvalidKeyException e) {
			   updateStatus(e.toString());
			   return;
		   } catch (CryptoTokenException e) {
			   updateStatus(e.toString());
			   return;
		   } catch (CryptoUnsupportedOperationException e) {
			   updateStatus(e.toString());
			   return;
		   } catch (InvalidCryptoSystemException e) {
			   updateStatus(e.toString());
			   return;
		   }
		   */
		   //updateStatus("sharedSecret:" + ByteArrayUtilities.byteArrayToHex(secretKey));
		   //updateStatus("sharedSecret LENGTH:" + Integer.toString(secretKey.length));
		   
		   //ARC4Key arc4key_out = GetARC4Key(secretKey2, secretKey2, 0);
		   ARC4PseudoRandomSource arc4keystream_out = GetARC4Keystream(secretKey, chunk_S1, dhposServer);
		   ARC4PseudoRandomSource arc4keystream_in  = GetARC4Keystream(secretKey, chunk_C1, dhposClient);
		   decryptis = new PRNGDecryptor( arc4keystream_in, dis );
		   cryptos = new PRNGEncryptor( arc4keystream_out, os );	 
		   
		   /* calculate response now */		   
		   for (int i = 0; i < 1536; i+=4)
		   {
			   int randInt = rand.nextInt();			   
			   chunk_C2[i+3] = (byte) (0x000000FF & (randInt));
    		   chunk_C2[i+2] = (byte) (0x000000FF & (randInt >>> 8));
    		   chunk_C2[i+1] = (byte) (0x000000FF & (randInt >>> 16));
    		   chunk_C2[i]   = (byte) (0x000000FF & (randInt >>> 24));    		   
		   }    
		   
		   //int signatureResp = RTMP_SIG_SIZE - SHA256_DIGEST_LENGTH;
		   //updateStatus("signatureResp:" + Integer.toString(signatureResp) );
		   
		   byte[] digestResp = new byte[SHA256_DIGEST_LENGTH];
		   HMACsha256(chunk_S1, digestPosServer, SHA256_DIGEST_LENGTH, GenuineFPKey2, 62, digestResp, 0);    		   
		   HMACsha256(chunk_C2, 0, RTMP_SIG_SIZE - SHA256_DIGEST_LENGTH, ByteArrayUtilities.byteArrayToHex(digestResp), SHA256_DIGEST_LENGTH, chunk_C2, RTMP_SIG_SIZE-SHA256_DIGEST_LENGTH);    		       		
		   
		   os.write(chunk_C2, 0, chunk_C2.length);
		   os.flush();
		   updateStatus("Flush C2");    		   
		   
		   
		   /* 2nd part of handshake */		   
		   byte[] chunk_S2 = new byte[1536];		   
		   int totalReadNumS2 = 0;			
		   
		   while(totalReadNumS2 < chunk_S2.length)
		   {
			   int readNum = 0;
			   if((readNum = dis.read( chunk_S2, totalReadNumS2, chunk_S2.length - totalReadNumS2)) == -1)
				   throw new Exception("Failed to read chunk_S2");     	
			   totalReadNumS2 += readNum; 
		   }   
		   totalReadSize += totalReadNumS2;
		   //updateStatus("S2:" + ByteArrayUtilities.byteArrayToHex(chunk_S2));
			
		   /* verify server response */
		   byte[] digest = new byte[SHA256_DIGEST_LENGTH];
		   HMACsha256(chunk_C1, digestPosClient, SHA256_DIGEST_LENGTH, GenuineFMSKey2, 68, digest, 0);    		   
		   //(&clientsig[digestPosClient], SHA256_DIGEST_LENGTH, GenuineFMSKey, sizeof(GenuineFMSKey), digest);
		   
		   byte[] signature = new byte[SHA256_DIGEST_LENGTH];
		   HMACsha256(chunk_S2, 0, RTMP_SIG_SIZE - SHA256_DIGEST_LENGTH, ByteArrayUtilities.byteArrayToHex(digest), SHA256_DIGEST_LENGTH, signature, 0);
		   //(serversig, RTMP_SIG_SIZE - SHA256_DIGEST_LENGTH, digest, SHA256_DIGEST_LENGTH, signature);
		       		
		   if(!Arrays.equals(signature, 0, chunk_S2, RTMP_SIG_SIZE - SHA256_DIGEST_LENGTH, SHA256_DIGEST_LENGTH))
		   {
			   updateStatus("Server not genuine Adobe!");
			   throw new Exception("Server not genuine Adobe!");
		   }
		   else
		   {
			   updateStatus("Genuine Adobe Flash Media Server");
		   }
		}
	
		/* Connect Phase *//*
		SendConnectPacket(cryptos);
	   
	   
	   
	   // Read
	   for(int i =0; i<12; i++){
		   try {
			   Chunk chunk = ReadARC4EncryptionPacket(decryptis);    					       			   
			   //updateStatus("Read MessageTypeID: " + Integer.toHexString(chunk.messageHeader.getMessageTypeIDField()));
			   //updateStatus("Read ChunkData: " + ByteArrayUtilities.byteArrayToHex(chunk.chunkData.getByteArray()));
			   
			   ClientPacket(chunk);
			   
			   chunk = null;
		   } catch (Exception e) {
			   updateStatus(e.toString());   			   
		   }
	   }*//*

				} catch(IOException e){
					updateStatus(e.toString());
				} catch (Exception e) {
					updateStatus(e.toString());
				} *///try
//			}//run()
//		}.start(); //Thread()
	} //Handshake()
		   
	
	public void hogega() throws Exception
	{
		SendConnectPacket(cryptos, getLocator());
		
	   // Read
	   for(int i =0; i<12; i++){
		   try {
			   Chunk chunk = ReadARC4EncryptionPacket(decryptis);    					       			   
			   //updateStatus("Read MessageTypeID: " + Integer.toHexString(chunk.messageHeader.getMessageTypeIDField()));
			   //updateStatus("Read ChunkData: " + ByteArrayUtilities.byteArrayToHex(chunk.chunkData.getByteArray()));
			   
			   ClientPacket(chunk);
			   
			   chunk = null;
		   } catch (Exception e) {
			   updateStatus(e.toString());   			   
		   }
	   }
	   
	   updateStatus("END OF hogega()");
	}
	
	
	public void DeleteStream() throws IOException
	{
		Commands.NetStreamCommands.SendDeleteStreamCommand(cryptos, streamID);
	}
	
	
	public void hogera()
	{
		new Thread()
		{
			public void run()
			{
		
		   try {

				   
		   /*
		   FileConnection _file = (FileConnection) Connector.open("file:///SDCard/output.aac", Connector.READ);
		   InputStream is = _file.openInputStream();
		   byte[] tmp = new byte[BUF_SIZE];
		   stream = new ByteArrayInputStream(tmp);
		   if(is.read(tmp) != BUF_SIZE)
         		updateStatus("Read ERROR");
		   */
		   

		   
		   
		   
		   //nocopybaos = new NoCopyByteArrayOutputStream();
		   //aacAudioDataLength = 0;
		   //BufferedInputStream buffer = new BufferedInputStream(decryptis);
		   /*
		   while(aacAudioDataLength < nocopyba.length - 300)//for(int i = 0; i<100; i++)      		     		  
		   {
			   Chunk chunk = null;
			   try {
				   chunk = ReadARC4EncryptionPacket(decryptis);    					       			   
				   //updateStatus("Read MessageTypeID: " + Integer.toHexString(chunk.messageHeader.getMessageTypeIDField()));
				   //updateStatus("Read ChunkData: " + ByteArrayUtilities.byteArrayToHex(chunk.chunkData.getByteArray()));
				   
				   if(chunk.chunkData != null)
					   ClientPacket(chunk);
				   
    			   chunk = null;
				   
			   } catch (Exception e) {
				   updateStatus("while() " + e.toString());  
				   //continue;
				   break;
			   }  
		   }
		   */
		   //updateStatus("aacAudioDataLength: " + aacAudioDataLength);
		   
		   
		   //MyApp app = (MyApp) getApplication();    		   
	       //app.addMediaActionHandler(new MyMediaAction());
	       //_app.addMediaActionHandler((MediaActionHandler) _app);
		   
		   updateStatus("Player Start");
//		   nocopyba = new byte[512];
		   //nocopyba = new byte[65535 * 3];
//		   stream = new ByteArrayInputStream(nocopyba);
//		   _source = new myDataSource(stream, "audio/aac");
		   //ByteArrayInputStreamDataSource source = new ByteArrayInputStreamDataSource(stream, "audio/aac");
//           Player p = Manager.createPlayer(source);
//           p.realize();
//           p.prefetch();
//           p.start();
//           volumeCtrl = (VolumeControl) p.getControl("VolumeControl");               
//           _app._mediaActions.setVolumeCtrl(volumeCtrl);
//           volumeCtrl.setLevel(10);
           
		   /*
		   PRNGDecryptor decryptStream = null;
		   for(int i=0; i<3; i++ ) {
		   byte[] cipherText = new byte[130];
		   //ByteArrayInputStream in = new ByteArrayInputStream( chunk_S3 );
		   decryptStream = new PRNGDecryptor( arc4keystream_in, dis );
		   decryptStream.read( cipherText, 0, cipherText.length);
		   //decryptStream.close();
		   updateStatus("SS3:" + ByteArrayUtilities.byteArrayToHex(cipherText));
		   }
		   decryptStream.close();
		   */
		   
		   
		   /*
		   try {
			   // see...
			   // Crypto API Sample Code - Copyright Research In Motion Limited, 1998-2001
			   // http://docs.blackberry.com/en/developers/deliverables/6022/net/rim/device/api/crypto/doc-files/CryptoTest.html#sampleARC4Encryption
			   //
			   // Lesson: Stream Ciphers
			   // http://www.blackberry.com/developers/docs/7.1.0api/net/rim/device/api/crypto/doc-files/stream.html
			   
			   byte[] data = new byte[] {0x74,0x65,0x73,0x74};
    		   ARC4Key arc4key = new ARC4Key(data);
    		   
    		   // Create a new byte array output stream for use in encryption
    		   NoCopyByteArrayOutputStream out = new NoCopyByteArrayOutputStream();
    		   
    		   // Now create a new instance of the PRNGEncryptor (pseudorandom number generator
    		   // encryptor) and pass in an ARC4 pseudorandom source along with the output stream
    		   PRNGEncryptor cryptoStream = new PRNGEncryptor(new ARC4PseudoRandomSource( arc4key ), out );
    		   
    		   // Write dataLength bytes from plainText to the ARC4 encryptor stream  
    		   byte[] plainText = new byte[] {0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41};
    		   cryptoStream.write( plainText, 0, plainText.length );
    		   cryptoStream.close();
    	        
    		   // Now copy the encrypted bytes from out into cipherText and return the length
    		   int finalLength = out.size();
    		   byte[] cipherText = new byte[finalLength];
    		   System.arraycopy( out.getByteArray(), 0, cipherText, 0, finalLength );
    		   //return finalLength;
    		   
    		   updateStatus("ARC4Key:" + ByteArrayUtilities.byteArrayToHex(cipherText));
			
		   } catch (CryptoTokenException e) {
			   updateStatus(e.toString());
		   }
		   */
		   
		   
		   /*
		   try {
// HMACsha256(byte[] msg, int msgOffset, int msgLen, String key, int keyLen, byte[] digest, int digestOffset)
			    
				//byte[] modifiedKey = Arrays.copy(ByteArrayUtilities.hexToByteArray(key), 0, keyLen);
			   
			    // Create the key
			    HMACKey hmackey = new HMACKey(secretKey);
			    
			    // Create the SHA digest
			    SHA256Digest sha256digest = new SHA256Digest();
			    
			    // Now an HMAC can be created, passing in the key and the
			    // SHA digest. Any instance of a digest can be used here.
			    HMAC hMac = new HMAC(hmackey, sha256digest);
		
			    //updateStatus("HMAC Length:" + hMac.getLength());
			    
			    int msgOffset = GetDHOffset2(chunk_C1);
			    int msgLen = 128;
			    // The HMAC can be updated much like a digest
			    hMac.update(chunk_C1,  msgOffset, msgLen);
		
			    // Now get the MAC value.
			    byte[] macValue = hMac.getMAC();

			    //updateStatus("HMAC_SHA256:" + ByteArrayUtilities.byteArrayToHex(macValue));
			    //updateStatus("Length:" + macValue.length);
			    
			    //System.arraycopy(macValue, 0, digest, digestOffset, macValue.length);
			   
			   
			   
			   ARC4Key arc4key = new ARC4Key(macValue);
			   NoCopyByteArrayOutputStream out = new NoCopyByteArrayOutputStream();
    		   PRNGEncryptor cryptoStream = new PRNGEncryptor(new ARC4PseudoRandomSource( arc4key ), out );
    		   cryptoStream.write( chunk_S3, 0, chunk_S3.length );
    		   cryptoStream.close();
    		   

    		   int finalLength = out.size();
    		   byte[] cipherText = new byte[finalLength];
    		   System.arraycopy( out.getByteArray(), 0, cipherText, 0, finalLength );
    		   //return finalLength;
    		   
    		   updateStatus("SS3:" + ByteArrayUtilities.byteArrayToHex(cipherText));
    		   
		   } catch (CryptoTokenException e) {
			   // TODO Auto-generated catch block
			   e.printStackTrace();
		   } catch (CryptoUnsupportedOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   */
		   
		  
		   //updateStatus("S3:" + ByteArrayUtilities.byteArrayToHex(chunk_S3));
		   /*
		   byte[] chunk_S4 = new byte[1536];
		   dis.readFully(chunk_S4);
		   updateStatus("S2:" + ByteArrayUtilities.byteArrayToHex(chunk_S4));
		   */
		   //double dbl = 0.5;
		   //Double size = new Double(32.76);
		   //size.doubleToLongBits(size);
		   //long lng = Double.doubleToLongBits(dbl);
		   //updateStatus("LONG:" + Long.toString(lng));
		   //updateStatus("LONG2:" + Double.toString(dbl));
		   
		   //long num = 2.0;
		   //String hexString = "0x" + Long.toHexString(hashValLong);
		   
		   //byte[] hex16 = AMF.longToHex(lng);
		   //String hexstr = new String(hex16);
		   //byte[] hex8 = ByteArrayUtilities.hexToByteArray(hexstr);
		   
		   
		   //updateStatus("LONG:" +  new String(AMF.longToHex(lng)) + " LENGTH:" + hex16.length); 
		   //updateStatus("LONG2:" + ByteArrayUtilities.byteArrayToHex(hex8) + " LENGTH:" + hex8.length);
		   //updateStatus("LONG3:" + ByteArrayUtilities.byteArrayToHex(AMF.EncodeNumber(dbl)));
		   
		   //updateStatus("NamedNumber:" + ByteArrayUtilities.byteArrayToHex(AMF.EncodeNamedNumber("capabilities", 15.0) ));
		   
		   //updateStatus("LONG:" + strBuf.toString());
		   
		   updateStatus("END OF RTMOSock");
		   
	   //} catch (IOException ie) {
		 //  	updateStatus("GetAdress:" + ie.toString());
		   	//throw new IOException(ie.toString());
	   } catch (Exception e) {
		// TODO Auto-generated catch block
		   updateStatus(e.toString());
	   } finally {
		   /*
		   try {
			   sc.close();
			   dis.close();
			   decryptis.close();
			   os.close();
		   } catch (IOException e) {
			   updateStatus(e.toString());
		   }
		   */
		   updateStatus("THREAD END");
	   }
			} //run()
		}.start(); //Thread()
	} //hogera()


	/*
    class myDataSource extends ByteArrayInputStreamDataSource
	{
    	// Limit bytes read to the readLimit
        private int readLength = 0;
        private int offset = 0;
    	private int readOffset = 0;
    	
    	
		public myDataSource(ByteArrayInputStream stream,String type)
		{
			super(stream, type);
		}		

		public int read(byte[] b, int off, int len) throws IOException
		{
			//updateStatus("read()");
			//updateStatus("Read Request Off: " + off + " Len: " + len);

			if(readOffset == 0)
			{
				Chunk chunk;
				
				try {
					
					while(true)
					{
						chunk = ReadARC4EncryptionPacket(decryptis);
		  				   
						if(chunk.chunkData != null)
							ClientPacket(chunk);
						else
							return 0;
						   								   
						readLength = aacAudioDataLength;
						offset = 0;
		  				   
						if(chunk.messageHeader.getMessageTypeIDField() == 0x08)
							break;

					}
				} catch (Exception e) {
  				   	//updateStatus("read() " + e.toString());
					throw new IOException("read() " + e.toString());
				} finally {
					chunk = null;
				}
			}
			else
			{
				readLength = aacAudioDataLength - readOffset;
				offset = readOffset;
			}
			
			
			//stream.reset();
			//readLength = aacAudioDataLength;
			if(readLength > len)
			{
				//return 0;
				readLength = len;
				readOffset += len;
			}
			else
			{
				readOffset = 0;
			}
			
			//updateStatus("POS: " + pos + " available: " + stream.available());
			//updateStatus("aacLen: " + aacAudioDataLength + " readLen: " + readLength + " offset: " + offset + " leave: " + readOffset + " off: " + off);
			
			System.arraycopy(nocopyba, offset, b, off, readLength);
			return readLength;
			
			
            //return stream.read(b, off, readLength);
            //return stream.read(b, off, len);
		} //read()
	} //myDataSource
    */
    
    
    
    
    //public class rtmpDataSource extends DataSource
    //{

    	private SourceStream _sourceStream;
    	private FileConnection _file;
    	
    	/*
		public rtmpDataSource(String locator)
		{
			super(locator);
		}
    	*/

		public Control getControl(String controlType)
		{
			// TODO Auto-generated method stub
			updateStatus("getControl()");
			return null;
		}

		public Control[] getControls()
		{
			// TODO Auto-generated method stub
			updateStatus("getControls()");
			return null;
		}

		public void connect() throws IOException
		{
			updateStatus("connect()");			
			
			// 2014/09/12まで
			//String domain = "w-radiko.smartstream.ne.jp";
			
			// 2014/9/13から
			String domain = "f-radiko.smartstream.ne.jp";
			
			try {
				Handshake(domain);
			} catch (Exception e) {
				throw new IOException("Handshake fail");
			}
			
			nocopyba = new byte[512];
			stream = new ByteArrayInputStream(nocopyba);
			
			//_file = (FileConnection) Connector.open("file:///SDCard/output.aac", Connector.READ);
			//_file = (FileConnection) Connector.open(getLocator(), Connector.READ);

			_sourceStream = new rtmpSourceStream(stream, "audio/aac");
			
		}
		
		// Overrides: disconnect() in DataSource
		public void disconnect()
		{
			updateStatus("disconnect()");
			
			if(_sourceStream != null)
				_sourceStream = null;
			if(stream != null)
				stream = null;
			if(nocopyba != null)
				nocopyba = null;
			
			try {
				if(decryptis != null)
					decryptis.close();
				if(dis != null)
					dis.close();
				if(os != null)
					os.close();
				if(sc != null)
					sc.close();
			} catch (IOException e) {		
			}
		}

		public String getContentType()
		{
			updateStatus("getContentType()");
						
			return _sourceStream.getContentDescriptor().getContentType();
		}

		public SourceStream[] getStreams()
		{
			updateStatus("getStreams()");
			
			return new SourceStream[] { _sourceStream };
		}

		// Overrides: start() in DataSource
		public void start() throws IOException
		{
			updateStatus("start()");
			
			try {
				hogega();
			} catch (Exception e) {
				throw new IOException("start() " + e.toString());
			}
		}

		// Overrides: start() in DataSource
		public void stop() throws IOException
		{
			updateStatus("stop()");
			DeleteStream();
		}
	   	
		class rtmpSourceStream implements SourceStream
		{
			private SharedInputStream _inputStream;
			private ContentDescriptor _contentDescriptor;
			
			private int readLength = 0;
	        private int offset = 0;
	    	private int readOffset = 0;
	    	
			public rtmpSourceStream(InputStream inputStream, String contentType)
			{
				_inputStream = SharedInputStream.getSharedInputStream(inputStream);
				_contentDescriptor = new ContentDescriptor(contentType);
			}

			public Control getControl(String controlType)
			{
				// TODO Auto-generated method stub
				updateStatus("getControl()");
				return null;
			}

			public Control[] getControls()
			{
				// TODO Auto-generated method stub
				updateStatus("getControls()");
				return null;
			}

			public ContentDescriptor getContentDescriptor()
			{
				updateStatus("getContentDescriptor()");
				return _contentDescriptor;
			}

			public long getContentLength()
			{
				// TODO Auto-generated method stub
				updateStatus("getContentLength()");
				
				// the length is not known
				return -1;
			}

			public int getSeekType()
			{
				updateStatus("getSeekType()");
				return NOT_SEEKABLE;
				//return SEEKABLE_TO_START; 
			}

			public int getTransferSize()
			{
				// TODO Auto-generated method stub
				updateStatus("getTransferSize()");
				return 512;
				//return 0;
			}

			public int read(byte[] b, int off, int len) throws IOException
			{
				//_app.updateStatus("read()");
				
				if(readOffset == 0)
				{
					Chunk chunk;
					
					try {
						
						while(true)
						{
							chunk = ReadARC4EncryptionPacket(decryptis);
			  				   
							if(chunk.chunkData != null)
								ClientPacket(chunk);
							else
								return 0;
							   								   
							readLength = aacAudioDataLength;
							offset = 0;
			  				   
							if(chunk.messageHeader.getMessageTypeIDField() == 0x08)
								break;

						}
					} catch (Exception e) {
	  				   	//updateStatus("read() " + e.toString());
						throw new IOException("read() " + e.toString());
					} finally {
						chunk = null;
					}
				}
				else
				{
					readLength = aacAudioDataLength - readOffset;
					offset = readOffset;
				}
				
				
				//stream.reset();
				//readLength = aacAudioDataLength;
				if(readLength > len)
				{
					//return 0;
					readLength = len;
					readOffset += len;
				}
				else
				{
					readOffset = 0;
				}
				
				//updateStatus("POS: " + pos + " available: " + stream.available());
				//updateStatus("aacLen: " + aacAudioDataLength + " readLen: " + readLength + " offset: " + offset + " leave: " + readOffset + " off: " + off);
				
				System.arraycopy(nocopyba, offset, b, off, readLength);
				return readLength;
				
				
				
				//return _inputStream.read(b, off, len);
			}

			public long seek(long where) throws IOException
			{
				updateStatus("seek()");
				return 0;
			}

			public long tell() 
			{
				updateStatus("tell()");				
				return 0;
			}
			
		} //rtmpSourceStream
    	
	//} //rtmpDataSource
    
}//RTMP




class Commands
{
	
	static class NetConnectionCommands
	{	
		private static final double TRANSACTION_ID_CONNECT = 1.0;
		private static final double TRANSACTION_ID_CREATESTREAM = 2.0;
		
		static class Connect
		{
			private ByteArrayOutputStream output = new ByteArrayOutputStream(); 
			
			Connect()
			{
				try {
					// Command Name | String | Name of the command. Set to "connect".	
					output.write( AMF.EncodeString("connect") );
					
					// Transaction ID | Number | Always set to 1.
					output.write( AMF.EncodeNumber(TRANSACTION_ID_CONNECT) );				
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			public void startCommandObject()
			{
				// Set OBJECT MARKER
				output.write( AMF.OBJECT_MARKER );
			}
			
			public void endCommandObject()
			{
				output.write( 0x00 );
				output.write( 0x00 );
				// Set OBJECT END MARKER
				output.write( AMF.OBJECT_END_MARKER );
			}
			
			public byte[] toByteArray()
			{
				return output.toByteArray();
			}
			
			public void setAppProperty(String val) throws IOException
			{
				// app | String | The Server application name the client is connected to.
				output.write( AMF.EncodeNamedString("app", val) );
				
			}
			
			public void setFpadProperty(boolean val) throws IOException
			{
				// fpad | Boolean| True if proxy is being used.
				output.write( AMF.EncodeNamedBoolean("fpad", val) );
			}
			
			public void setAudioCodecsProperty(double val) throws IOException
			{
				// audioCodecs | Number | Indicates what audio codecs the client supports.
				output.write( AMF.EncodeNamedNumber("audioCodecs", val) );
			}
			
			public void setVideoCodecsProperty(double val) throws IOException
			{
				// videoCodecs | Number | Indicates what video codecs are supported.
				output.write( AMF.EncodeNamedNumber("videoCodecs", val) );
			}
			
			public void setVideoFunctionProperty(double val) throws IOException
			{
				// videoFunction | Number | Indicates what special video functions are supported.
				output.write( AMF.EncodeNamedNumber("videoFunction", val) );
			}
			
			public void setCapabilitiesProperty(double val) throws IOException
			{
				// ???
				output.write( AMF.EncodeNamedNumber("capabilities", val) );
			}
			
			public void setAuthKey(String val) throws IOException
			{
				// ???
				output.write( AMF.EncodeString(val) );
			}
		} //connect
		
		
		public static void SendCreateStreamCommand(PRNGEncryptor os) throws IOException
		{		
			ByteArrayOutputStream output = new ByteArrayOutputStream();
	    	
			// ---- Payload  --------------------------------------------- //    	 
			// Command Name | String | Name of the command. Set to "createStream".
			output.write( AMF.EncodeString("createStream") );
			
			// Transaction ID | Number | Transaction ID of the command.
			output.write( AMF.EncodeNumber(TRANSACTION_ID_CREATESTREAM) );
			
			// Command Object | Object | If there exists any command info this is set, else this is set to null type.
			output.write(AMF.NULL_MARKER);		
			
			byte[] payload = output.toByteArray();
			output = null;
			
			// ---- BasicHeader --------------------------------------------- //
			BasicHeader bh = new BasicHeader();
			bh.setFMT(0x01);
			bh.setChunkStreamID((byte) 0x03);
			
			//updateStatus("BasicHeader:" + ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ));
	
			// ---- MessageHeader --------------------------------------------- //
			MessageHeader msgheader = new MessageHeader(bh.getFMT());
			msgheader.setMessageLengthField(payload.length);
	
			msgheader.setMessageTypeIDField(0x14);  //Invoke
			
			//updateStatus("MessageHeader:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageHeader()));
			
			// ---- Send --------------------------------------------- //		
			os.write( bh.getBasicHeader() );
			os.write( msgheader.getMessageHeader() );
			os.write( payload );
			os.flush();		
	
		} //SendCreateStreamCommand
	} //NetConnectionCommands

	static class NetStreamCommands
	{
		private static final double TRANSACTION_ID_PLAY = 0.0;
		private static final double TRANSACTION_ID_DELETESTREAM = 0.0;
		
		public static void SendPlayCommand(PRNGEncryptor os, int streamID) throws IOException
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
	    	
			// ---- Payload  --------------------------------------------- //    	 
			// Command Name | String | Name of the command. Set to "play".
			output.write( AMF.EncodeString("play") );
			
			// Transaction ID | Number | Transaction ID set to 0.
			output.write( AMF.EncodeNumber(TRANSACTION_ID_PLAY) );
			
			// Command Object | Null | Command information does not exist. Set to null type.
			output.write( AMF.NULL_MARKER );		
			
			// Stream Name | String | Name of the stream to play.
			output.write( AMF.EncodeString("simul-stream.stream") );
			//output.write( AMF.EncodeString("mp3:simul-stream.stream") );
			
			// Start | Number | An optional parameter that specifies the start time in seconds.
			// The default value is -2, which means the subscriber first tries to play 
			// the live stream specified in the Stream Name field. If a live stream of that name is not found,
			// it plays the recorded stream of the same name. If there is no recorded stream
			// with that name, the subscriber waits for a new live stream with that name and    
			// plays it when available. If you pass -1 in the Start field, only the live stream
			// specified in the Stream Name field is played. If you pass 0 or a positive     
			// number in the Start field, a recorded stream specified in the Stream Name     
			// field is played beginning from the time specified in the Start field. If no     
			// recorded stream is found, the next item in the playlist is played.
			//output.write( AMF.EncodeNumber(-2.0) );
			
			// Duration | Number | An optional parameter that specifies the duration of playback in seconds.
			//output.write( AMF.EncodeNumber(-2.0) );
						
			byte[] payload = output.toByteArray();
			output = null;
			
			// ---- BasicHeader --------------------------------------------- //
			BasicHeader bh = new BasicHeader();
			bh.setFMT(0x00);
			bh.setChunkStreamID((byte) 0x08);  // we make 8 our stream channel
			
			//updateStatus("BasicHeader:" + ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ));

			// ---- MessageHeader --------------------------------------------- //
			MessageHeader msgheader = new MessageHeader(bh.getFMT());
			msgheader.setMessageLengthField(payload.length);

			msgheader.setMessageTypeIDField(0x14);  //Invoke
			
			msgheader.setMessageStreamIdField(streamID);
			
			//updateStatus("MessageHeader:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageHeader()));
			
			// ---- Send --------------------------------------------- //		
			os.write( bh.getBasicHeader() );
			os.write( msgheader.getMessageHeader() );
			os.write( payload );
			os.flush();
		} //SendPlayCommand
		
		
		public static void SendDeleteStreamCommand(PRNGEncryptor os, double streamID) throws IOException
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
	    	
			// ---- Payload  --------------------------------------------- //    	 
			// Command Name |  String  | Name of the command, set to "deleteStream". 
			output.write( AMF.EncodeString("deleteStream") );
			
			// Transaction ID | Number | Transaction ID set to 0.
			output.write( AMF.EncodeNumber(TRANSACTION_ID_DELETESTREAM) );
			
			// Command Object | Null | Command information does not exist. Set to null type.
			output.write( AMF.NULL_MARKER );
			
			// Stream ID | Number | The ID of the stream that is destroyed on the server.
			output.write( AMF.EncodeNumber(streamID) );
			
			byte[] payload = output.toByteArray();
			output = null;
			
			// ---- BasicHeader --------------------------------------------- //
			BasicHeader bh = new BasicHeader();
			bh.setFMT(0x01);
			bh.setChunkStreamID((byte) 0x03);
			
			//updateStatus("BasicHeader:" + ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ));

			// ---- MessageHeader --------------------------------------------- //
			MessageHeader msgheader = new MessageHeader(bh.getFMT());
			msgheader.setMessageLengthField(payload.length);

			msgheader.setMessageTypeIDField(0x14);  //Invoke
			
			//updateStatus("MessageHeader:" + ByteArrayUtilities.byteArrayToHex(msgheader.getMessageHeader()));
			
			// ---- Send --------------------------------------------- //		
			os.write( bh.getBasicHeader() );
			os.write( msgheader.getMessageHeader() );
			os.write( payload );
			os.flush();
		}
		
	} //NetStreamCommands
	
} //Commands


class Chunk
{
	public BasicHeader basicHeader = null;
	public MessageHeader messageHeader = null;
	public ChunkData chunkData = null;
	
	Chunk()
	{
		// DO NOTHING		
	}
} //Chunk

class BasicHeader
{	
	private int fmt = 0;
	private byte chunkStreamIDLength = 0;
	private byte[] chunkStreamID = new byte[3];
	
	BasicHeader()
	{
		// DO NOTHING
	}
	
	public void setFMT(int f)
	{
		fmt = f;
	}
	
	public int getFMT()
	{
		return fmt;
	}	
	
	// For Chunk basic header 1
	public void setChunkStreamID(byte s)
	{
		chunkStreamIDLength = 1;
		chunkStreamID[0] = s;
	}
	
	// For Chunk basic header 2 and 3
	public void setChunkStreamID(byte[] val)
	{
		if(val.length == 2)
		{
			chunkStreamIDLength = 2;
			chunkStreamID[0] = val[0];
			chunkStreamID[1] = val[1];
		}
		else if(val.length == 3)
		{
			chunkStreamIDLength = 3;
			chunkStreamID[0] = val[0];
			chunkStreamID[1] = val[1];
			chunkStreamID[2] = val[2];			
		}
	}
	
	public byte[] getChunkStreamID()
	{
		if(chunkStreamIDLength == 0)
			return null;			
		else if(chunkStreamIDLength == 1)
			return Arrays.copy(chunkStreamID, 0, 1);
		else if(chunkStreamIDLength == 2)
			return Arrays.copy(chunkStreamID, 0, 2);
		else if(chunkStreamIDLength == 3)
			return Arrays.copy(chunkStreamID, 0, 3);
		
		return null;
	}
	/*
	public void setBasicHeader(int val) 
	{
		//packet->m_headerType = (hbuf[0] & 0xc0) >> 6;
		//packet->m_nChannel = (hbuf[0] & 0x3f);  //=2
		
		// FMT
		fmt = (val & 0xC0) >> 6;
		
		// chunkStreamID
		int tmp = val & 0x3F;
		*//*
		if(tmp == 0){
			chunkStreamID[0] = 0;
			chunkStreamID[1] = 0;
		}*//*
		//chunkStreamID = val & 0x3F;
		
	}
	*/
	public byte[] getBasicHeader()
	{
		byte[] output;
		
		if (chunkStreamIDLength == 1)
		{
			output = new byte[1];
			
			output[0] |= (byte) ((fmt & 0x03) << 6);
			output[0] |= (byte) (chunkStreamID[0] & 0x3F);
			
			return output;
		}
		else if(chunkStreamIDLength == 2)
		{
			//tmp = new byte[2];
		}
		else if(chunkStreamIDLength == 3)
		{
			//tmp = new byte[3];
		}
		
		return null;
	}
} //BasicHeader

class MessageHeader
{
	static final int TYPE0 = 0;
	
	int type = 0;
	
	byte[] timeStamp       = new byte[3];
	byte[] messageLength   = new byte[3];
	byte[] messageTypeId   = new byte[1];
	byte[] messageStreamId = new byte[4];
		
	MessageHeader(int t){
		type = t;
	}
	
	public void setMessageHeader(byte[] val)
	{
		if(val.length == 11)
		{
			//  Type 0 chunk headers are 11 bytes long. 
			//
			//   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
			//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			//  |                   timestamp                   |message length |
			//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			//  |     message length (cont)     |message type id| msg stream id |
			//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			//  |           message stream id (cont)            |
			//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			//                    Chunk Message Header - Type 0
			System.arraycopy(val, 0, timeStamp, 0, 3);
			System.arraycopy(val, 3, messageLength, 0, 3);
			System.arraycopy(val, 6, messageTypeId, 0, 1);
			System.arraycopy(val, 7, messageStreamId, 0, 4);
			
			/*
			timeStamp       = Arrays.copy(val, 0, 3);
			messageLength   = Arrays.copy(val, 3, 3);
			messageTypeId   = Arrays.copy(val, 6, 1);
			messageStreamId = Arrays.copy(val, 7, 4);
			*/		
		}
		else if(val.length == 7)
		{
			//  Type 1 chunk headers are 7 bytes long.
			//   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
			//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			//  |                timestamp delta                |message length |
			//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			//  |     message length (cont)     |message type id|
			//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			//                    Chunk Message Header - Type 1
			System.arraycopy(val, 0, timeStamp, 0, 3);
			System.arraycopy(val, 3, messageLength, 0, 3);
			System.arraycopy(val, 6, messageTypeId, 0, 1);
			/*
			timeStamp       = Arrays.copy(val, 0, 3);
			messageLength   = Arrays.copy(val, 3, 3);
			messageTypeId   = Arrays.copy(val, 6, 1);
			*/
		}
		else if(val.length == 3)
		{
			//  Type 2 chunk headers are 3 bytes long.
			//   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3
			//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			//  |                timestamp delta                |
			//  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			//            Chunk Message Header - Type 2
			System.arraycopy(val, 0, timeStamp, 0, 3);
			//timeStamp       = Arrays.copy(val, 0, 3);
		}
	}
	
	public byte[] getMessageHeader()
	{
		byte[] output = null;
		
		if(type == 0)  // 11bytes
		{
			output = new byte[11];
			int pos = 0;
						
			// TimeStamp (3bytes)
			System.arraycopy(timeStamp, 0, output, pos, timeStamp.length);
			pos += timeStamp.length;
			
			// MessageLength (3bytes)
			System.arraycopy(messageLength, 0, output, pos, messageLength.length);
			pos += messageLength.length;
			
			// MessageTypeID (1byte)
			System.arraycopy(messageTypeId, 0, output, pos, messageTypeId.length);
			pos += messageTypeId.length;
			
			// MessageStreamID(4bytes)
			System.arraycopy(messageStreamId, 0, output, pos, messageStreamId.length);
			
			return output;
				
		}
		else if(type == 1)  // 7bytes
		{
			output = new byte[7];
			int pos = 0;
			
			// TimeStamp (3bytes)
			System.arraycopy(timeStamp, 0, output, pos, timeStamp.length);
			pos += timeStamp.length;
			
			// MessageLength (3bytes)
			System.arraycopy(messageLength, 0, output, pos, messageLength.length);
			pos += messageLength.length;
			
			// MessageTypeID (1byte)
			System.arraycopy(messageTypeId, 0, output, pos, messageTypeId.length);
			
			return output;			
		}
		else if(type == 2)  // 3bytes
		{
			output = new byte[3];
		}
		else if(type == 3)  // 0byte
		{
			output = null;
		}
		
		return output;
	}
	
	public void setTimeStampField()
	{
		
	}
	
	public byte[] getTimeStampField()
	{
		return timeStamp;
	}
	
	public void setMessageLengthField(int m)
	{
		messageLength = AMF.EncodeInt24(m);		
	}
	
	public byte[] getMessageLengthField()
	{
		return messageLength;
	}
	
	public void setMessageTypeIDField(int m)
	{
		messageTypeId[0] = (byte) (m & 0xFF);
	}
	
	public int getMessageTypeIDField()
	{		
		return (int)(messageTypeId[0] & 0xFF);
	}
	
	public void setMessageStreamIdField(int nVal)
	{
		// Message stream ID is stored in little-endian format.		
		messageStreamId[0] = (byte) (nVal & 0x000000FF);
		messageStreamId[1] = (byte) ((nVal >>> 8) & 0x000000FF);
		messageStreamId[2] = (byte) ((nVal >>> 16) & 0x000000FF);
		messageStreamId[3] = (byte) ((nVal >>> 24) & 0x000000FF);
	}
} //MessageHeader


class ChunkData
{
	private byte[] output = null;
	
	ChunkData(byte[] val)
	{
		//output = Arrays.copy(val);
		output = val;
	}
	
	public byte[] getByteArray()
	{
		return output;
	}
} //ChunkData


class AMF
{
	/* AMF 0 Data Types */	
	static final int NUMBER_MARKER       = 0x00;
	static final int BOOLEAN_MARKER      = 0x01;
	static final int STRING_MARKER       = 0x02;
	static final int OBJECT_MARKER       = 0x03;
	static final int MOVIECLIP_MARKER    = 0x04;  // reserved, not supported
	static final int NULL_MARKER         = 0x05;
	static final int UNDEFINED_MARKER    = 0x06;
	static final int REFERENCE_MARKER    = 0x07;
	static final int ECMA_ARRAY_MARKER   = 0x08;
	static final int OBJECT_END_MARKER   = 0x09;
	static final int STRICT_ARRAY_MARKER = 0x0A;
	static final int DATE_MARKER         = 0x0B;
	static final int LONG_STRING_MARKER  = 0x0C;
	static final int UNSUPPORTED_MARKER  = 0x0D;
	static final int RECORDSET_MARKER    = 0x0E;  // reserved, not supported
	static final int XML_DOCUMENT_MARKER = 0x0F;
	static final int TYPED_OBJECT_MARKER = 0x10;	
	
	public static byte[] EncodeBoolean(boolean bVal)
	{
		byte[] output = new byte[2];
		
		output[0] = AMF.BOOLEAN_MARKER;
		
		if(bVal){
			output[1] = 0x01;
		} else {
			output[1] = 0x00;
		}
		
		return output;		
	}
	
	public static byte[] EncodeInt16(int nVal)
	{
		byte[] output = new byte[2];
		output[1] = (byte) (nVal & 0xFF);
		output[0] = (byte) ((nVal >> 8) & 0xFF);
		return output;
	}
	
	public static byte[] EncodeInt24(int nVal)
	{
		byte[] output = new byte[3];		
		output[2] = (byte) (nVal & 0x0000FF);
		output[1] = (byte) ((nVal >>> 8) & 0x0000FF);
		output[0] = (byte) ((nVal >>> 16) & 0x0000FF);		
		return output;
	}
	
	public static byte[] EncodeInt32(int nVal)
	{
		byte[] output = new byte[4];		
		output[3] = (byte) (nVal & 0x000000FF);
		output[2] = (byte) ((nVal >>> 8) & 0x000000FF);
		output[1] = (byte) ((nVal >>> 16) & 0x000000FF);
		output[0] = (byte) ((nVal >>> 24) & 0x000000FF);
		return output;
	}
	
	public static byte[] EncodeNamedBoolean(String strName, boolean bVal) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
				
		output.write(EncodeInt16( strName.length() ));
		output.write(strName.getBytes());		
		output.write( EncodeBoolean(bVal) );
		
		return output.toByteArray();
	}
	
	public static byte[] EncodeNamedNumber(String strName, double dVal) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		output.write(EncodeInt16( strName.length() ));
		output.write(strName.getBytes());		
		output.write(EncodeNumber(dVal));
		
		return output.toByteArray();
	}
	
	public static byte[] EncodeNamedString(String strName, String strValue) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		output.write(EncodeInt16( strName.length() ));
		output.write(strName.getBytes());
		output.write(EncodeString(strValue));
		return output.toByteArray();
	}
	
	public static byte[] EncodeNumber(double dVal)
	{
		byte[] output = new byte[9];
		
		output[0] = NUMBER_MARKER;
		
		byte[] hex16 = longToHex( Double.doubleToLongBits(dVal) );		
		byte[] hex8 = ByteArrayUtilities.hexToByteArray( new String(hex16) );		
		
		System.arraycopy(hex8, 0, output, 1, hex8.length);
		
		return output;		
	}
	
	public static byte[] EncodeString(String str) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		try {
			if(str.length() < 65536)
			{
				output.write(STRING_MARKER);
				output.write(EncodeInt16( str.length() ));
				output.write(str.getBytes());
				return output.toByteArray();
			}
			else
			{		
				output.write(STRING_MARKER);
				output.write(EncodeInt16( str.length() ));
				output.write(str.getBytes());
				return output.toByteArray();			
			}
		} finally {
			output = null;
		}
	}
	
	public static int DecodeInt16(byte[] val)
	{
		return ((int)(val[0] & 0xFF) << 8 | (int)(val[1] & 0xFF));
	}
	
	public static int DecodeInt24(byte[] val)
	{
		return ((int)(val[0] & 0xFF) << 16 | (int)(val[1] & 0xFF) << 8 | (int)(val[2] & 0xFF));
	}
	
	public static int DecodeInt32(byte[] val)
	{
		return ((int)(val[0] & 0xFF) << 24 | (int)(val[1] & 0xFF) << 16 | (int)(val[2] & 0xFF) << 8 | (int)(val[3] & 0xFF));
	}

	public static double DecodeNumber(byte[] val) throws IllegalArgumentException
	{
		// HEX文字列に変換。
		// ex.3f f0 00 00 00 00 00 00 -> 3ff0000000000000 
		String st8 = ByteArrayUtilities.byteArrayToHex(val);

		// HEX文字列の1文字ずつをbyte[]に変換する。
		// ex.3ff0000000000000 -> 3 f f 0 0 0 0 0 0 0 0 0 0 0 0 0
		byte[] ba16 = new byte[16];		
		for(int i=0; i<16; i++)
		{
			ba16[i] = (byte) st8.charAt(i);
		}
		
		return Double.longBitsToDouble( hexToLong(ba16) );				
	}	
	
	public static String DecodeString(byte[] val)
	{
		//ByteArrayOutputStream output = new ByteArrayOutputStream();
		/*
		try {
			if(str.length() < 65536)
			{
				output.write(STRING_MARKER);
				output.write(EncodeInt16( str.length() ));
				output.write(str.getBytes());
				return output.toByteArray();
			}
			else
			{		
				output.write(STRING_MARKER);
				output.write(EncodeInt16( str.length() ));
				output.write(str.getBytes());
				return output.toByteArray();			
			}
		} finally {
			output = null;
		}*/
		
		return new String(val);
	}
	
	private static long hexToLong(byte[] bytes)
	{
		// Thanks, DZone.
		// Convert A Long To HEX Value And The Other Way Around | DZone
		// http://dzone.com/snippets/convert-long-hex-value-and
		//
		// 浮動小数点数の内部表現(IEEE)
		// http://www.k-cube.co.jp/wakaba/server/floating_point.html
		
		if (bytes.length > 16) {
			throw new IllegalArgumentException("Byte array too long (max 16 elements)");
		}
		long v = 0;
		for (int i = 0; i < bytes.length; i += 2) {
			byte b1 = (byte) (bytes[i] & 0xFF);

			b1 -= 48;
			if (b1 > 9) b1 -= 39;

			if (b1 < 0 || b1 > 15) {
				throw new IllegalArgumentException("Illegal hex value: " + bytes[i]);
			}

			b1 <<=4;

			byte b2 = (byte) (bytes[i + 1] & 0xFF);
			b2 -= 48;
			if (b2 > 9) b2 -= 39;

			if (b2 < 0 || b2 > 15) {
				throw new IllegalArgumentException("Illegal hex value: " + bytes[i + 1]);
			}

			v |= (((b1 & 0xF0) | (b2 & 0x0F))) & 0x00000000000000FFL ;

			if (i + 2 < bytes.length) v <<= 8;
		}

		return v;
	}
	
	private static byte[] longToHex(final long l)
	{
		// Thanks, DZone.
		// Convert A Long To HEX Value And The Other Way Around | DZone
		// http://dzone.com/snippets/convert-long-hex-value-and
		//
		// 浮動小数点数の内部表現(IEEE)
		// http://www.k-cube.co.jp/wakaba/server/floating_point.html
				
		long v = l & 0xFFFFFFFFFFFFFFFFL;

		byte[] result = new byte[16];
		Arrays.fill(result, (byte)0, result.length, (byte)0);

		for (int i = 0; i < result.length; i += 2) {
			byte b = (byte) ((v & 0xFF00000000000000L) >> 56);

			byte b2 = (byte) (b & 0x0F);
			byte b1 = (byte) ((b >> 4) & 0x0F);

			if (b1 > 9) b1 += 39;
			b1 += 48;

			if (b2 > 9) b2 += 39;
			b2 += 48;

			result[i] = (byte) (b1 & 0xFF);
			result[i + 1] = (byte) (b2 & 0xFF);

			v <<= 8;
		}

		return result;
	}
} //AMF

