package mypackage;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.HttpsConnection;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.compress.ZLibInputStream;
import net.rim.device.api.io.Base64OutputStream;
import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.io.LineReader;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.io.transport.TransportInfo;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.ByteArrayUtilities;


public class Auth
{
	private MyApp _app;
	
	private String authToken;
	private String areaID;
	
	private SWF _swf;
	
	
	public Auth(UiApplication app)
	{
		_app = (MyApp) app;
	}
	
	
	public void doAuth() throws Exception
	{
		HttpsConnection auth1Con = null;
		HttpsConnection auth2Con = null;
	    //String url = "https://radiko.jp/v2/api/auth1_fms;deviceside=false;connectionUID=GPMDSAP01";
	    //String url2 = "https://radiko.jp/v2/api/auth2_fms;deviceside=false;connectionUID=GPMDSAP01";
		String url = "https://radiko.jp/v2/api/auth1_fms";
	    String url2 = "https://radiko.jp/v2/api/auth2_fms";
	    String partialkey;
	    int keylength, keyoffset;
	    
	    try {
	    	updateStatus("Connecting..");	    	
	
	    	// use the factory to get a connection
	    	
	    	ConnectionDescriptor conDescriptor = _app.GetConnectionFactory().getConnection( url );
	
	    	if (conDescriptor == null)
	    		throw new Exception("conDescriptor Error");
	    	
			// connection succeeded
			int transportUsed = conDescriptor.getTransportDescriptor().getTransportType();
			switch(transportUsed)
			{
				case TransportInfo.TRANSPORT_TCP_CELLULAR:
					updateStatus("Connecting via Direct TCP");
					break;
				case TransportInfo.TRANSPORT_TCP_WIFI:
					updateStatus("Connecting via WIFI");
					break;				
			}
			
			// using the connection
			auth1Con = (HttpsConnection) conDescriptor.getConnection();
		    	
	        // Set the request method and headers
			auth1Con.setRequestMethod(HttpsConnection.POST);
			auth1Con.setRequestProperty("pragma", "no-cache");
			auth1Con.setRequestProperty("X-Radiko-App", "pc_1");
			auth1Con.setRequestProperty("X-Radiko-App-Version", "2.0.1");
			auth1Con.setRequestProperty("X-Radiko-User", "test-stream");
			auth1Con.setRequestProperty("X-Radiko-Device", "pc");
	        
	        // Getting the response code will open the connection,
	        // send the request, and read the HTTP response headers.
	        // The headers are stored until requested.
	        int rc = auth1Con.getResponseCode();
	        if (rc != HttpsConnection.HTTP_OK)
	            throw new Exception("HTTP response code: " + rc);
	        
	        authToken = auth1Con.getHeaderField("X-RADIKO-AUTHTOKEN");
	        keylength = Integer.parseInt(auth1Con.getHeaderField("X-Radiko-KeyLength"));
	        keyoffset = Integer.parseInt(auth1Con.getHeaderField("X-Radiko-KeyOffset"));
	        
	        updateStatus("AuthToken: " + authToken + "\n KeyLength: " + Integer.toString(keylength) + "\n KeyOffset: " + keyoffset);
	
		} finally {
			if(auth1Con != null){ auth1Con.close(); }
	    }
		
		
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

	    	ConnectionDescriptor conDescriptor = _app.GetConnectionFactory().getConnection( url2 );
	
	    	if (conDescriptor == null)
	    		throw new Exception("conDescriptor ERROR");    		
	
	    	// using the connection
	    	auth2Con = (HttpsConnection) conDescriptor.getConnection();   	
	    	
	        // Set the request method and headers
	    	auth2Con.setRequestMethod(HttpsConnection.POST);
	    	auth2Con.setRequestProperty("pragma", "no-cache");
	    	auth2Con.setRequestProperty("X-Radiko-App", "pc_1");
	    	auth2Con.setRequestProperty("X-Radiko-App-Version", "2.0.1");
	    	auth2Con.setRequestProperty("X-Radiko-Authtoken", authToken);
	    	auth2Con.setRequestProperty("X-Radiko-Partialkey", partialkey);
	        
	        // Getting the response code will open the connection,
	        // send the request, and read the HTTP response headers.
	        // The headers are stored until requested.
	        int rc = auth2Con.getResponseCode();
	        if (rc != HttpsConnection.HTTP_OK)        
	            throw new IOException("HTTP response code: " + rc);                
	        
	        // Get the length and process the data	        
	        LineReader lineReader = new LineReader(auth2Con.openDataInputStream());
	        for(;;)
	        {
	        	try
	            {
	        		String line = new String(lineReader.readLine());
	        		if(line.length() != 0)
	                {
	        			if(line.startsWith("OUT"))
	        			{
	        				throw new Exception("Out of Area");
	        			}
	        			else if(line.startsWith("JP"))
	        			{
	        				int comma;
	        				if((comma = line.indexOf(",")) == -1)
	        					throw new Exception("Failed to get the AreaID (Not found 'comma')");
	        				
	        				areaID = line.substring(0, comma);
	        				
	        				updateStatus("Area_ID: " + areaID);
	        			}
	        			else
	        			{
	        				throw new Exception("Failed to get the AreaID (Not found 'JP')");
	        			}
	                }     
	            }
	            catch(EOFException eof)
	            {
	                // We've reached the end of the file.
	                break;
	            }
	        } //for
	
		} finally {
			if(auth2Con != null){ auth2Con.close(); }
	    }
	} //doAuth

	
	public String getAuthToken()
	{
		return authToken;
	}
	
	public String getAreaID()
	{
		return areaID;
	}
	
	
	public void getAuthKey() throws Exception
	{
		InputStream is;
    	byte[] swf_rawData;
    	String url = "http://radiko.jp/player/swf/player_3.0.0.01.swf";
    	
    	ConnectionFactory _factory = _app.GetConnectionFactory();
    	if(_factory == null)
    		throw new IOException("getAuthKey() _factory Error");
    	
    	
    	ConnectionDescriptor conDescriptor = _factory.getConnection(url);
    	if(conDescriptor == null)
    		throw new IOException("getAuthKey() conDescriptor Error");
    	
    	HttpConnection httpconn = (HttpConnection) conDescriptor.getConnection();
    	
    	try {
	    	httpconn.setRequestMethod(HttpConnection.GET);
	        
	        int rc = httpconn.getResponseCode();
	        if (rc != HttpConnection.HTTP_OK)        
	            throw new IOException("getAuthKey() HTTP response code: " + rc);      
	        
	        is = httpconn.openInputStream();
	        /*
	        if((swf_rawData = IOUtilities.streamToBytes(is)) == null)
	        	throw new IOException("getAuthKey() imageData Error");
	        
	       updateStatus("length: " + swf_rawData.length);
	        
	       byte[] check = new byte[10];
	       System.arraycopy(swf_rawData, 0, check, 0, check.length);
	       updateStatus("byte: " + ByteArrayUtilities.byteArrayToHex(check));
	       */
	       
	       _swf = new SWF();
	       Authkey_header(is, _swf);

	        //return Bitmap.createBitmapFromBytes(imageData, 0, -1, Bitmap.SCALE_TO_FIT);
	        
		} finally {
			if(httpconn != null){ 
				try {
					httpconn.close();
				} catch (IOException e) {} 
			}
		}
	}
	
	private void Authkey_header(InputStream is, SWF _myswf) throws Exception
	{
		int totalReadNum = 0;
		// SWFヘッダ
		// 
		
		
		//---- Signature
		byte[] header_Signature = new byte[3];				
		while(totalReadNum < header_Signature.length)
		{
			int readNum = 0;
			if((readNum = is.read( header_Signature, totalReadNum, header_Signature.length - totalReadNum)) == -1)
				throw new Exception("Failed to read header_Signature");     	
			totalReadNum += readNum; 
		}
   
		if(Arrays.equals(header_Signature, new byte[]{'F', 'W', 'S'}))
			_myswf.compressed = false;
		else if(Arrays.equals(header_Signature, new byte[]{'C', 'W', 'S'}))
			_myswf.compressed = true;
		else
			throw new Exception("Failed to analyze header_Signature"); 
		
		updateStatus("Signature: " + new String(header_Signature));
		
		//---- Version
		byte[] header_Version = new byte[1];
		totalReadNum = 0;
		while(totalReadNum < header_Version.length)
		{
			int readNum = 0;
			if((readNum = is.read( header_Version, totalReadNum, header_Version.length - totalReadNum)) == -1)
				throw new Exception("Failed to read header_Version");     	
			totalReadNum += readNum; 
		}
		
		updateStatus("Version: " + header_Version[0]);
		
		
		//---- FileLength
		// リトルエンディアン
		// !! FileLength = UI32. NOW use Sighed INT !!
		byte[] header_FileLength = new byte[4];
		totalReadNum = 0;
		while(totalReadNum < header_FileLength.length)
		{
			int readNum = 0;
			if((readNum = is.read( header_FileLength, totalReadNum, header_FileLength.length - totalReadNum)) == -1)
				throw new Exception("Failed to read header_FileLength");     	
			totalReadNum += readNum; 
		}
		
		_myswf.fileLength = decodeInt32(header_FileLength);
		updateStatus("FileLength: " + decodeInt32(header_FileLength));
				
		//---- ここからzlib解凍 ----//
		
		//---- zlib
		ZLibInputStream zlibInputStream = new ZLibInputStream(is, false);
		/*
	    byte[] data = new byte[_myswf.fileLength - 8];
	    totalReadNum = 0;
		while(totalReadNum < data.length)
		{
			int readNum = 0;
			if((readNum = zlibInputStream.read( data, totalReadNum, data.length - totalReadNum)) == -1)
				throw new Exception("Failed to read zlibInputStream");     	
			totalReadNum += readNum; 
		}
	    
	    
		byte[] check = new byte[10];
		System.arraycopy(data, 0, check, 0, check.length);
		updateStatus("zlib: " + data.length);
		updateStatus("zlib: " + ByteArrayUtilities.byteArrayToHex(check));
	    */
		
		//---- FrameSize
		// 計算しません。べたでいきます。すみません。
		// 読み出しのみ。
		byte[] header_FrameSize = new byte[1+7];
	    totalReadNum = 0;
		while(totalReadNum < header_FrameSize.length)
		{
			int readNum = 0;
			if((readNum = zlibInputStream.read( header_FrameSize, totalReadNum, header_FrameSize.length - totalReadNum)) == -1)
				throw new Exception("Failed to read header_FrameSize");     	
			totalReadNum += readNum; 
		}
		
		updateStatus("FrameSize: " + ByteArrayUtilities.byteArrayToHex(header_FrameSize));
		
		//---- FrameRate
		byte[] header_FrameRate = new byte[2];
	    totalReadNum = 0;
		while(totalReadNum < header_FrameRate.length)
		{
			int readNum = 0;
			if((readNum = zlibInputStream.read(header_FrameRate, totalReadNum, header_FrameRate.length - totalReadNum)) == -1)
				throw new Exception("Failed to read header_FrameRate");
			totalReadNum += readNum; 
		}
		
		updateStatus("FrameRate: " + header_FrameRate[1] + "." + header_FrameRate[0]);
		
		//---- FrameCount
		// リトルエンディアン
		byte[] header_FrameCount = new byte[2];
	    totalReadNum = 0;
		while(totalReadNum < header_FrameCount.length)
		{
			int readNum = 0;
			if((readNum = zlibInputStream.read(header_FrameCount, totalReadNum, header_FrameCount.length - totalReadNum)) == -1)
				throw new Exception("Failed to read header_FrameCount");
			totalReadNum += readNum; 
		}
		
		updateStatus("FrameCount: " + decodeInt16(header_FrameCount));
		
		//---- ヘッダここまで ----//
		
		byte[] data = new byte[10];
	    totalReadNum = 0;
		while(totalReadNum < data.length)
		{
			int readNum = 0;
			if((readNum = zlibInputStream.read( data, totalReadNum, data.length - totalReadNum)) == -1)
				throw new Exception("Failed to read data");     	
			totalReadNum += readNum; 
		}
	    
	    
		byte[] check = new byte[10];
		System.arraycopy(data, 0, check, 0, check.length);
		updateStatus("data: " + data.length);
		updateStatus("data: " + ByteArrayUtilities.byteArrayToHex(check));
	    
		
	}  //Authkey_header()
	
	public static int decodeInt16(byte[] val)
	{
		// リトルエンディアン
		return ((int)(val[0] & 0xFF) | (int)(val[1] & 0xFF) << 8);
	}
	
	public static int decodeInt32(byte[] val)
	{
		// リトルエンディアン
		return ((int)(val[0] & 0xFF) | (int)(val[1] & 0xFF) << 8 | (int)(val[2] & 0xFF) << 16 | (int)(val[3] & 0xFF) << 24);
	}
	
	private void updateStatus(String val)
	{
		MyApp _app = (MyApp) UiApplication.getUiApplication();
		_app.updateStatus("[Auth] " + val);
	}
	
	private class SWF
	{
		private boolean compressed;
		private int fileLength;
		
		public SWF()
		{
			compressed = false;
			fileLength = 0;
		}
	}
}
