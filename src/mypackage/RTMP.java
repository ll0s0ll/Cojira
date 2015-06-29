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

import net.rim.device.api.crypto.ARC4Key;
import net.rim.device.api.crypto.ARC4PseudoRandomSource;
import net.rim.device.api.crypto.DHCryptoSystem;
import net.rim.device.api.crypto.DHKeyAgreement;
import net.rim.device.api.crypto.DHKeyPair;
import net.rim.device.api.crypto.DHPublicKey;
import net.rim.device.api.crypto.HMAC;
import net.rim.device.api.crypto.HMACKey;
import net.rim.device.api.crypto.PRNGDecryptor;
import net.rim.device.api.crypto.PRNGEncryptor;
import net.rim.device.api.crypto.SHA256Digest;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.ByteArrayUtilities;

public class RTMP
{
	private static final int RTMP_SIG_SIZE          = 1536;
	private static final int SHA256_DIGEST_LENGTH   = 32;
	private static final int RTMP_DEFAULT_CHUNKSIZE = 128;  //140
	
	private static final double TRANSACTION_ID_CONNECT = 1.0;
	private static final double TRANSACTION_ID_CREATESTREAM = 2.0;
	
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
	private static final byte[] GenuineFMSKey = new byte[] {
			(byte)0x47, (byte)0x65, (byte)0x6E, (byte)0x75, (byte)0x69, (byte)0x6E, (byte)0x65, (byte)0x20,
			(byte)0x41, (byte)0x64, (byte)0x6F, (byte)0x62, (byte)0x65, (byte)0x20, (byte)0x46, (byte)0x6C,
			(byte)0x61, (byte)0x73, (byte)0x68, (byte)0x20, (byte)0x4D, (byte)0x65, (byte)0x64, (byte)0x69,
			(byte)0x61, (byte)0x20, (byte)0x53, (byte)0x65, (byte)0x72, (byte)0x76, (byte)0x65, (byte)0x72,
			(byte)0x20, (byte)0x30, (byte)0x30, (byte)0x31
	};  // 36 Genuine Adobe Flash Media Server 001    		  
	/*(byte)0xF0, (byte)0xEE, (byte)0xC2, (byte)0x4A, (byte)0x80, (byte)0x68, (byte)0xBE, (byte)0xE8,
	(byte)0x2E, (byte)0x00, (byte)0xD0, (byte)0xD1, (byte)0x02, (byte)0x9E, (byte)0x7E, (byte)0x57,
	(byte)0x6E, (byte)0xEC, (byte)0x5D, (byte)0x2D, (byte)0x29, (byte)0x80, (byte)0x6F, (byte)0xAB,
	(byte)0x93, (byte)0xB8, (byte)0xE6, (byte)0x36, (byte)0xCF, (byte)0xEB, (byte)0x31, (byte)0xAE
	};*/  // 68
	
	private static final byte[] GenuineFPKey = new byte[] {
		(byte)0x47, (byte)0x65, (byte)0x6E, (byte)0x75, (byte)0x69, (byte)0x6E, (byte)0x65, (byte)0x20,
		(byte)0x41, (byte)0x64, (byte)0x6F, (byte)0x62, (byte)0x65, (byte)0x20, (byte)0x46, (byte)0x6C,
		(byte)0x61, (byte)0x73, (byte)0x68, (byte)0x20, (byte)0x50, (byte)0x6C, (byte)0x61, (byte)0x79,
		(byte)0x65, (byte)0x72, (byte)0x20, (byte)0x30,	(byte)0x30, (byte)0x31
	};
	/*(byte)0xF0, (byte)0xEE,
	(byte)0xC2, (byte)0x4A, (byte)0x80, (byte)0x68, (byte)0xBE, (byte)0xE8, (byte)0x2E, (byte)0x00,
	(byte)0xD0, (byte)0xD1, (byte)0x02, (byte)0x9E, (byte)0x7E, (byte)0x57, (byte)0x6E, (byte)0xEC,
	(byte)0x5D, (byte)0x2D, (byte)0x29, (byte)0x80, (byte)0x6F, (byte)0xAB, (byte)0x93, (byte)0xB8,
	(byte)0xE6, (byte)0x36, (byte)0xCF, (byte)0xEB, (byte)0x31, (byte)0xAE
	};*/ 

	//private MyApp _app = null;
	
	private SocketConnection sc = null;
	private InputStream is = null;
	private OutputStream os = null;
	private PRNGDecryptor decryptis = null;
	private PRNGEncryptor cryptos = null;
	
	private int serverWindowAcknowledgementSize = 2500000;
	private int totalReadSize = 0;
	private int reportedReadSize = 0;
	private int ClientBW = 0;
	private double streamID;
	
	
	public RTMP(MyApp _app)
	{
		//this._app = _app;
		this.totalReadSize = 0;
	}
	
	
	public void doConnect(ConnectionFactory _connfactory, final String url) throws IOException
	{
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException("ConnectionFactory"); }
		if(url == null) { throw new NullPointerException("url"); }
		if(url.length() == 0) { throw new IllegalArgumentException("url"); }
	
		//String url = "socket://netradio-fm-flash.nhk.jp:1935";
		//String url = "socket://w-radiko.smartstream.ne.jp:1935";
		//url = "socket://" + url + ":1935;deviceside=false;connectionUID=GPMDSAP01";
		
		// use the factory to get a connection
		ConnectionDescriptor conDescriptor = _connfactory.getConnection(url);
		if(conDescriptor == null) { throw new IOException("Failed to open '" + url + "'"); }
		
		// using the connection
		sc = (SocketConnection) conDescriptor.getConnection();
		is = sc.openInputStream();
		os = sc.openOutputStream();
	} //doConnect()
	
	
	public void doDisconnect()
	{
		try {
			if(decryptis != null) { decryptis.close(); decryptis = null; }
			if(cryptos != null) {cryptos.close(); cryptos = null; }
			if(is != null) { is.close(); is = null; }
			if(os != null) { os.close(); os = null; }
			if(sc != null) { sc.close(); sc = null; }
		} catch (IOException e) { }
	} //doDisconnect()
	
	
	public byte[] doReadAudioPackets()
	{
		try {
			Chunk chunk = readARC4EncryptionPacket(decryptis, cryptos);
			
			if(chunk.chunkData == null) { return new byte[0]; }
			
			byte[] out = readAudioPackets(cryptos, chunk);
			
			return out;
			
		} catch (Exception e) { return new byte[0]; }
		
	} //doReadAudioPackets()
	
	
	public void doStart(final String stationId, final String authToken) throws IOException
	{
		// 引数チェック
		if(stationId == null) { throw new NullPointerException("stationId"); }
		if(stationId.length() == 0) { throw new IllegalArgumentException("stationId"); }
		if(authToken == null) { throw new NullPointerException("authToken"); }
		if(authToken.length() == 0) { throw new IllegalArgumentException("authToken"); }
				
		try {
			ARC4PseudoRandomSource[] arc4keystream = handshake(is, os);
			
			decryptis = new PRNGDecryptor( arc4keystream[0], is );
			cryptos = new PRNGEncryptor( arc4keystream[1], os );
			
			openStream(decryptis, cryptos, authToken, stationId);
			
		} catch (Exception e) {
			throw new IOException(e.toString());
		}
	} //doStart()
	
	
	public void doStop() throws IOException
	{
		deleteStream(cryptos);
	} //doStart()
	
	
	private void deleteStream(PRNGEncryptor cryptos) throws IOException
	{
		Commands.NetStreamCommands.SendDeleteStreamCommand(cryptos, streamID);
	}
	

	private void handleCtrlMessage(PRNGEncryptor cryptos, ChunkData chunkData) throws IOException
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
				sendPingResponcePacket(cryptos, Arrays.copy(chunkData.getByteArray(), 2, 4));
				break;
	
			}
			default:
				updateStatus("Unknown EventType: " + eventType);
				break;
		}
	}

	/*private static void HandleInvoke(ByteArrayInputStream input) throws IOException
	{
		//updateStatus("HandleInvoke");
		
		// Read 1byte
		switch(input.read())
		{
			case -1:
			{
				//updateStatus("ERROR!!!");
				break;
			}	
			case AMF.NUMBER_MARKER:
			{
				byte[] output = new byte[8];
				input.read(output);
				//updateStatus("NUMBER: " + Double.toString(AMF.DecodeNumber(output)) );
				//updateStatus("NUMBER: 00" + ByteArrayUtilities.byteArrayToHex(output));
				//updateStatus("available: " + input.available());
				break;
			}	
			case AMF.BOOLEAN_MARKER:
			{    			
				byte[] output = new byte[1];
				input.read(output);
				
				if(output[0] == 0x01)
				{
					//updateStatus("BOOLEAN: TRUE");
				}
				else if(output[0] == 0x00)
				{
					//updateStatus("BOOLEAN: FALSE");
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
				//updateStatus("STRING: " + new String(val));
				//updateStatus("STRING: 02" + ByteArrayUtilities.byteArrayToHex(length) + ByteArrayUtilities.byteArrayToHex(val));
				//updateStatus("available: " + input.available());
				break;
			}
			case AMF.OBJECT_MARKER:
			{
				//updateStatus("OBJECT");
				
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
					//updateStatus("Name: " + new String(val));
					//updateStatus("Name: " + ByteArrayUtilities.byteArrayToHex(length) + ByteArrayUtilities.byteArrayToHex(val));
					
					// Value部を処理
					HandleInvoke(input);
					
					// 次の3バイトが、終端バイト列(0x00, 0x00, 0x09)に合致する場合はブレイク
					input.mark(8);
					byte[] OBJEnd = new byte[3];
					input.read(OBJEnd);
				
					if(Arrays.equals(OBJEnd, OBJEndMarker))
					{
						//updateStatus("Reached END");
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
				//updateStatus("NULL "+ "available: " + input.available());
				break;
				
			case AMF.ECMA_ARRAY_MARKER:
			{
				//updateStatus("ECMA_ARRAY");
				// 32bit
				byte[] associative_count = new byte[4];
				input.read(associative_count);
				//updateStatus("associative_count: " + Integer.toHexString( AMF.DecodeInt32(associative_count) ));
				
				// Name-value pairsなので、まずName部を取得。
				byte[] length = new byte[2];
				input.read(length);
				int len = AMF.DecodeInt16(length);
				byte[] val = new byte[len];
				input.read(val);
				//updateStatus("Name: " + new String(val));
				//updateStatus("Name: " + ByteArrayUtilities.byteArrayToHex(length) + ByteArrayUtilities.byteArrayToHex(val));
				
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
					//updateStatus("Reached END");
					break;
				}
				else
				{
					input.reset();
				}
			}
			default:
				//updateStatus("Unknown Marker");
				break;
		}
	}*/

	
	private ARC4PseudoRandomSource[] handshake(InputStream is, OutputStream os) throws Exception
	{
		// 引数チェック
		if(is == null) { throw new NullPointerException("InputStream"); }
		if(os == null) { throw new NullPointerException("OutputStream"); }
		
		// =========== chunk C0 =========== //
		byte[] chunk_C0 = new byte[1];
		chunk_C0[0] = (byte) 0x06;  // RTMPE=0x06 RTMP=0x03
		os.write(chunk_C0, 0, 1);
		os.flush();
		chunk_C0 = null;
		
		
		// =========== chunk C1 =========== //
		byte[] chunk_C1 = new byte[1536];
		
		int datel = (int) (System.currentTimeMillis() / 1000); // msec/1000 = sec
		
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
		for (int i = 8; i < 1536; i+=4)
		{
			int randInt = rand.nextInt();
			chunk_C1[i+3] = (byte) (0x000000FF & (randInt));
			chunk_C1[i+2] = (byte) (0x000000FF & (randInt >>> 8));
			chunk_C1[i+1] = (byte) (0x000000FF & (randInt >>> 16));
			chunk_C1[i]   = (byte) (0x000000FF & (randInt >>> 24));
		}
		
		// DH & MHAC SHA256
		DHKeyPair dhkeypair = crypt(chunk_C1);
		DHPublicKey mydhpubkey = dhkeypair.getDHPublicKey();
		mydhpubkey.verify();
		
		byte[] pub_key = mydhpubkey.getPublicKeyData();
		if(pub_key.length != 128) { throw new Exception("Illegal pub_key length"); }
		
		//updateStatus("PublicKey:" + ByteArrayUtilities.byteArrayToHex(pub_key));
		//updateStatus("PublicKey:" + ByteArrayUtilities.byteArrayToHex(dh_keypair.getDHPublicKey().getPublicKeyData()));
		//updateStatus("PublicKey_LENGTH:" + dh_keypair.getDHPublicKey().getPublicKeyData().length);
			
		//updateStatus("PrivateKey:" + ByteArrayUtilities.byteArrayToHex(dh_keypair.getDHPrivateKey().getPrivateKeyData()));
		//updateStatus("PrivateKey_LENGTH:" + dh_keypair.getDHPrivateKey().getPrivateKeyData().length);
		
		int dhposClient = getDHOffset2(chunk_C1);  //472
		System.arraycopy(pub_key, 0, chunk_C1, dhposClient, pub_key.length);
		pub_key = null;
		
		int digestPosClient = getDigestOffset2(chunk_C1);
		calculateDigest(digestPosClient, chunk_C1, GenuineFPKey, chunk_C1, digestPosClient);
		
		os.write(chunk_C1, 0, chunk_C1.length);
		os.flush();
		
		
		// =========== chunk S0 =========== //
		byte[] chunk_S0 = new byte[1];
		int totalReadNumS0 = 0;
		
		while(totalReadNumS0 < chunk_S0.length)
		{
			int readNum = 0;
			if((readNum = is.read( chunk_S0, totalReadNumS0, chunk_S0.length - totalReadNumS0)) == -1)
			{
				throw new Exception("Failed to read chunk_S0");
			}
			totalReadNumS0 += readNum;
		}
		totalReadSize += totalReadNumS0;
		//updateStatus("S0:" + ByteArrayUtilities.byteArrayToHex(chunk_S0));
		chunk_S0 = null;
		
		
		// =========== chunk S1 =========== //
		byte[] chunk_S1 = new byte[1536];
		int totalReadNumS1 = 0;
		
		while(totalReadNumS1 < chunk_S1.length)
		{
			int readNum = 0;
			if((readNum = is.read( chunk_S1, totalReadNumS1, chunk_S1.length - totalReadNumS1)) == -1)
			{
				throw new Exception("Failed to read chunk_S1");
			}
			totalReadNumS1 += readNum; 
		}
		totalReadSize += totalReadNumS1;
		//updateStatus("S1:" + ByteArrayUtilities.byteArrayToHex(chunk_S1));
		
		updateStatus("FMS Version:" + Integer.toString(chunk_S1[4]) +"."+ Integer.toString(chunk_S1[5]) +"."+ Integer.toString(chunk_S1[6]) +"."+ Integer.toString(chunk_S1[7]));
		
		
		int digestPosServer = 0;
		int digoff = 2;
		int dhoff  = 2;
		
		if(digoff == 1) {
			digestPosServer = getDigestOffset1(chunk_S1);
		} else if(digoff == 2) {
			digestPosServer = getDigestOffset2(chunk_S1);
		}
		
		//updateStatus("digestPosServer:" + Integer.toString(digestPosServer) + "GenuineFMSKey:" + GenuineFMSKey.length);
		
		if(!verifyDigest(digestPosServer, chunk_S1, GenuineFMSKey))
		{
			digestPosServer = getDigestOffset1(chunk_S1);
			digoff = 1;
			dhoff  = 1;
			
			if(!verifyDigest(digestPosServer, chunk_S1, GenuineFMSKey))
			{
				updateStatus("VerifyDigest ERROR\n");
			}
		}
		
		
		// do Diffie-Hellmann Key exchange for encrypted RTMP
		byte[] secretKey = new byte[128];
		int dhposServer = 0;
		
		if(dhoff == 1) {
			dhposServer = getDHOffset1(chunk_S1);
		} else if(dhoff ==2) {
			dhposServer = getDHOffset2(chunk_S1);
		}
		//updateStatus("dhposServer:" + Integer.toString(dhposServer));
		
		byte[] pubkeyBn = Arrays.copy(chunk_S1, dhposServer, 128);
		DHPublicKey dhpubkey = new DHPublicKey(dhkeypair.getDHCryptoSystem(), pubkeyBn);
		pubkeyBn = null;
				
		dhpubkey.verify();
		
		secretKey = DHKeyAgreement.generateSharedSecret(dhkeypair.getDHPrivateKey(), dhpubkey, false);
		
		//updateStatus("sharedSecret:" + ByteArrayUtilities.byteArrayToHex(secretKey));
		//updateStatus("sharedSecret LENGTH:" + Integer.toString(secretKey.length));
		
		
		ARC4PseudoRandomSource arc4keystream_out = getARC4Keystream(secretKey, chunk_S1, dhposServer);
		ARC4PseudoRandomSource arc4keystream_in  = getARC4Keystream(secretKey, chunk_C1, dhposClient);
		
		// =========== chunk C2 =========== //
		/* calculate response now */	
		byte[] chunk_C2 = new byte[1536];
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
		digestResp = null;
		
		os.write(chunk_C2, 0, chunk_C2.length);
		os.flush();
		chunk_C2 = null;
		
		
		// =========== chunk C2 =========== //
		// 2nd part of handshake
		byte[] chunk_S2 = new byte[1536];
		int totalReadNumS2 = 0;
		
		while(totalReadNumS2 < chunk_S2.length)
		{
			int readNum = 0;
			if((readNum = is.read( chunk_S2, totalReadNumS2, chunk_S2.length - totalReadNumS2)) == -1)
			{
				throw new Exception("Failed to read chunk_S2");
			}
			totalReadNumS2 += readNum; 
		}
		totalReadSize += totalReadNumS2;
			
		/* verify server response */
		byte[] digest = new byte[SHA256_DIGEST_LENGTH];
		HMACsha256(chunk_C1, digestPosClient, SHA256_DIGEST_LENGTH, GenuineFMSKey2, 68, digest, 0);
		
		byte[] signature = new byte[SHA256_DIGEST_LENGTH];
		HMACsha256(chunk_S2, 0, RTMP_SIG_SIZE - SHA256_DIGEST_LENGTH, ByteArrayUtilities.byteArrayToHex(digest), SHA256_DIGEST_LENGTH, signature, 0);
		digest = null;
		
		if(!Arrays.equals(signature, 0, chunk_S2, RTMP_SIG_SIZE - SHA256_DIGEST_LENGTH, SHA256_DIGEST_LENGTH))
		{
			throw new Exception("Server not genuine Adobe!");
		}

		ARC4PseudoRandomSource[] out = new ARC4PseudoRandomSource[]{arc4keystream_in, arc4keystream_out};
		return out;
	} //Handshake()
	
	
	private void openStream(PRNGDecryptor decryptor, PRNGEncryptor encryptor, String authkey, String stationId ) throws Exception
	{
		// 引数チェック
		if(decryptor == null) { throw new NullPointerException("PRNGDecryptor"); }
		if(encryptor == null) { throw new NullPointerException("PRNGEncryptor"); }
		if(authkey == null) { throw new NullPointerException("authkey"); }
		if(authkey.length() == 0) { throw new IllegalArgumentException("authkey"); }
		if(stationId == null) { throw new NullPointerException("stationId"); }
		if(stationId.length() == 0) { throw new IllegalArgumentException("stationId"); }
		
		// 
		sendConnectPacket(encryptor, stationId, authkey);
		
		// ConnectPacketの応答を読み込む
		for(int i =0; i<12; i++)
		{
			Chunk chunk = readARC4EncryptionPacket(decryptor, encryptor);
			//updateStatus("Read MessageTypeID: " + Integer.toHexString(chunk.messageHeader.getMessageTypeIDField()));
			//updateStatus("Read ChunkData: " + ByteArrayUtilities.byteArrayToHex(chunk.chunkData.getByteArray()));
			
			readPackets(encryptor, chunk);
		}
	} //openStream()
	
	
	private Chunk readARC4EncryptionPacket(PRNGDecryptor decryptStream, PRNGEncryptor cryptos) throws Exception
	{
		//PRNGDecryptor decryptStream = decryptis;
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
			sendAcknowledgementPacket(cryptos);
			reportedReadSize += totalReadSize;
		}
		
		//updateStatus("Read ChunkData: " + ByteArrayUtilities.byteArrayToHex(chunkData.getByteArray()));
		
		return chunk;
	} //ReadARC4EncryptionPacket()
	
	
	private byte[] readAudioPackets(PRNGEncryptor cryptos, Chunk chunk) throws IOException
	{
		int messageTypeID = chunk.messageHeader.getMessageTypeIDField();
		switch (messageTypeID)
		{
			case 0x08:
			{
				ByteArrayInputStream input = new ByteArrayInputStream(chunk.chunkData.getByteArray());
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
				
				int len = adtsHeader.length + (chunkDataLength - 2);
				byte[] out = new byte[len];
				
				System.arraycopy(adtsHeader, 0, out, 0, adtsHeader.length);
				System.arraycopy(chunk.chunkData.getByteArray(), 2, out, adtsHeader.length, (chunkDataLength - 2));
				
				return out;
			}
		}
		
		// Audioデータではない場合は、再度通常のデータとして解析する
		readPackets(cryptos, chunk);
		return new byte[0];
		
	} //readAudioPackets()
	
	
	private void readPackets(PRNGEncryptor cryptos, Chunk chunk) throws IOException
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
				
				handleCtrlMessage(cryptos, chunk.chunkData);
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
			/*case 0x08:
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
				
				*//*
				updateStatus("SoundType: " + Integer.toHexString(SoundType) +
						" SoundSize: " + Integer.toHexString(SoundSize) + 
						" SoundRate: " + Integer.toHexString(SoundRate) + 
						" SoundFormat: " + Integer.toHexString(SoundFormat)
						);
				*//*
				
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
			}*/
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
						sendWindowAcknowledgementSizePacket(cryptos);
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
	} //readPackets()


	private void sendAcknowledgementPacket(PRNGEncryptor os) throws IOException
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
		
		/*updateStatus("SendWindowAcknowledgementSizePacket: " + 
				ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ) +
				ByteArrayUtilities.byteArrayToHex( msgheader.getMessageHeader() ) +
				ByteArrayUtilities.byteArrayToHex( payload ) );*/
		
		// Send
		os.write( bh.getBasicHeader() );
		os.write( msgheader.getMessageHeader() );
		os.write( payload );
		os.flush();
	}

	private void sendWindowAcknowledgementSizePacket(PRNGEncryptor os) throws IOException
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
	
	
	private void updateStatus(String val)
	{
		//MyApp _app = (MyApp) UiApplication.getUiApplication();
		//MyScreen _screen = (MyScreen) _app.getActiveScreen();
		//_screen.updateStatus("[RTMP] " + val);
		//_app.updateStatus("[RTMP] " + val);
	} //updateStatus()
	
	
	private static void calculateDigest(int digestPos, byte[] chunk_C1, byte[] key, byte[] digest, int digestOffset) throws Exception
	{
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
	
	} //calculateDigest()
	
	
	private static DHKeyPair crypt(byte[] chunk_C1) throws Exception
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
		
		DHCryptoSystem dh = new DHCryptoSystem(p,q,g);
		dh.verify();

		//updateStatus("P:" + ByteArrayUtilities.byteArrayToHex(dh.getP()));
		//updateStatus("Q:" + ByteArrayUtilities.byteArrayToHex(dh.getQ()));
		//updateStatus("G:" + ByteArrayUtilities.byteArrayToHex(dh.getG()));
		
		dh_keypair = dh.createDHKeyPair();
		dh_keypair.verify();
		
		return dh_keypair;
	} //crypt()

	
	private static void HMACsha256(byte[] msg, int msgOffset, int msgLen, String key, int keyLen, byte[] digest, int digestOffset) throws Exception
	{
		// See...
		// Lesson: Digests and MACs
		// http://www.blackberry.com/developers/docs/6.0.0api/net/rim/device/api/crypto/doc-files/digest.html
	
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
	} //HMACsha256()


	private static ARC4PseudoRandomSource getARC4Keystream(byte[] secretKey, byte[] msg, int msgOffset) throws Exception
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
		{
			throw new Exception("GetARC4Keystream() secretKey Length Error");
		}
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
		
		// 16bytes length
		ARC4Key arc4key = new ARC4Key( macValue, 0, 16 );
		
		ARC4PseudoRandomSource source = new ARC4PseudoRandomSource(arc4key);
		
		// Set 1536bytes void data
		source.xorBytes( new byte[1536] );
		
		return source;
	} //getARC4Keystream()


	private static int getDHOffset1(byte[] handshake) throws Exception
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
	
		if (res + 128 > 1531) { throw new Exception("GetDHOffset1 ERROR!!\n"); }
		
		return res;
	}


	private static int getDHOffset2(byte[] handshake) throws Exception
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
	
		if(res + 128 > 767) { throw new Exception("GetDHOffset2 ERROR!!\n"); }
		
		return res;
	}


	private static int getDigestOffset1(byte[] handshake)
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
		//updateStatus( "GetDigestOffset1 res:" + Integer.toString(res) );
		
		if (res + 32 > 771) { return 0; }
	
		return res;
	} //getDigestOffset1()
	
	
	private static int getDigestOffset2(byte[] handshake) throws Exception
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
		
		if(res + 32 > 1535) { throw new Exception("GetDigestOffset2 ERROR!!\n");}
		
		return res;
	} //getDigestOffset2()
	

	private static void sendConnectPacket(PRNGEncryptor os, String stationID, final String authkey) throws Exception
	{
		// 引数チェック
		if(os == null) {throw new NullPointerException("PRNGEncryptor"); }
		if(stationID == null) {throw new NullPointerException("stationID"); }
		if(stationID.length() == 0) {throw new IllegalArgumentException("stationID"); }
		if(authkey == null) {throw new NullPointerException("authkey"); }
		if(authkey.length() == 0) {throw new IllegalArgumentException("authkey"); }
		
		
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
		connect = null;
		
		//updateStatus("Connect byteArray:" + ByteArrayUtilities.byteArrayToHex(connect.toByteArray()));
		//updateStatus("Connect byteArray Length:" + connect.toByteArray().length);
		   
		
		// ---- BasicHeader  --------------------------------------------- //
		BasicHeader bh = new BasicHeader();
		bh.setFMT(0x00);
		bh.setChunkStreamID((byte) 0x03);
		output.write( bh.getBasicHeader() );
		bh = null;
		
		//updateStatus("BasicHeader:" + ByteArrayUtilities.byteArrayToHex( bh.getBasicHeader() ));
	
		// ---- MessageHeader  --------------------------------------------- //
		MessageHeader msgheader = new MessageHeader(MessageHeader.TYPE0);
		msgheader.setMessageLengthField(connect_byteArray.length);
		msgheader.setMessageTypeIDField(0x14);  //Invoke
		output.write( msgheader.getMessageHeader() );
		msgheader = null;
		
		
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
				if(numOfBytesToWrite < RTMP_DEFAULT_CHUNKSIZE) {
					output.write(connect_byteArray, RTMP_DEFAULT_CHUNKSIZE * counter, numOfBytesToWrite);
					numOfBytesToWrite -= numOfBytesToWrite;
					counter++;
				} else {
					output.write(connect_byteArray, RTMP_DEFAULT_CHUNKSIZE * counter, RTMP_DEFAULT_CHUNKSIZE);
					output.write(header);
					numOfBytesToWrite -= RTMP_DEFAULT_CHUNKSIZE;
					counter++;
				}
			}
		}
		connect_byteArray = null;
		
		// SendARC4EncryptionPacket
		//SendARC4EncryptionPacket(os, output.toByteArray());
		byte[] out = output.toByteArray();
		os.write( out, 0, out.length );
		os.flush();
		out = null;
		
	} //SendConnectPacket()

	private static void sendPingResponcePacket(PRNGEncryptor os, byte[] val) throws IOException
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
	} //sendPingResponcePacket()
	
	
	private static boolean verifyDigest(int digestPos, byte[] handshakeMessage, byte[] key) throws Exception
	{	
		final int SHA256_DIGEST_LENGTH = 32;
		byte[] calcDigest = new byte[SHA256_DIGEST_LENGTH];
		calculateDigest(digestPos, handshakeMessage, key, calcDigest, 0);
		
		return Arrays.equals(handshakeMessage, digestPos, calcDigest, 0, SHA256_DIGEST_LENGTH);
	} //verifyDigest()
	

	private static class Commands
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
						
					} catch (IOException e) {}
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
} //RTMP


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
	
	/*public static String DecodeString(byte[] val)
	{
		//ByteArrayOutputStream output = new ByteArrayOutputStream();
		*//*
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
		}*//*
		
		return new String(val);
	}*/
	
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

