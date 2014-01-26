/*
	Auth.java
	
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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.HttpConnection;
import javax.microedition.io.HttpsConnection;

import net.rim.device.api.compress.ZLibInputStream;
import net.rim.device.api.io.Base64OutputStream;
import net.rim.device.api.io.LineReader;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.io.transport.TransportInfo;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.util.Arrays;


public class Auth
{
	private MyApp _app;
	
	private byte[] authkey;
	private String authToken;
	private String areaID;
	
	
	public Auth(UiApplication app)
	{
		_app = (MyApp) app;
		authkey = null;
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
		
		//---- Get AuthToken,keylength,keyoffset --------------------- //
		try {
			_app.getMainScreen().updateStatusField("接続中...");
			ConnectionDescriptor conDescriptor = _app.getConnectionFactory().getConnection( url );
			
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
			
			auth1Con = (HttpsConnection) conDescriptor.getConnection();
			
			// Set the request method and headers
			auth1Con.setRequestMethod(HttpsConnection.POST);
			auth1Con.setRequestProperty("pragma", "no-cache");
			auth1Con.setRequestProperty("X-Radiko-App", "pc_1");
			auth1Con.setRequestProperty("X-Radiko-App-Version", "2.0.1");
			auth1Con.setRequestProperty("X-Radiko-User", "test-stream");
			auth1Con.setRequestProperty("X-Radiko-Device", "pc");
			
			int rc = auth1Con.getResponseCode();
			if (rc != HttpsConnection.HTTP_OK)
				throw new Exception("HTTP response code: " + rc);
			
			authToken = auth1Con.getHeaderField("X-RADIKO-AUTHTOKEN");
			keylength = Integer.parseInt(auth1Con.getHeaderField("X-Radiko-KeyLength"));
			keyoffset = Integer.parseInt(auth1Con.getHeaderField("X-Radiko-KeyOffset"));
			
			//updateStatus("AuthToken: " + authToken + "\n KeyLength: " + Integer.toString(keylength) + "\n KeyOffset: " + keyoffset);
		} finally {
			if(auth1Con != null){ auth1Con.close(); }
		}
		
		/*
		FileConnection fconn = (FileConnection)Connector.open("file:///SDCard/authkey.png");
		//updateStatus("OK I'm " + fconn.getName() + " " + fconn.fileSize() );
		if (!fconn.exists())
			throw new Exception("Not Found authkey.png");
		InputStream fconnIS =  fconn.openInputStream();
		fconn.close();
		*/

		//---- Get Authkey --------------------------------------------- //
		getAuthKey();
		
		//---- Get PartialKey --------------------------------------------- //
		InputStream fconnIS = new ByteArrayInputStream(authkey);
		
		byte[] b1 = new byte[ keylength ];
		
		fconnIS.skip(keyoffset);
		fconnIS.read(b1, 0, keylength);
		fconnIS.close();
		authkey = null;
		
		partialkey = new String( Base64OutputStream.encode(b1, 0, b1.length, false, false) );
		
		
		//---- Get AreaID --------------------------------------------- //
		try {
			_app.getMainScreen().updateStatusField("エリア判定中...");
			ConnectionDescriptor conDescriptor = _app.getConnectionFactory().getConnection( url2 );
			
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
			
			int rc = auth2Con.getResponseCode();
			if (rc != HttpsConnection.HTTP_OK)
				throw new IOException("HTTP response code: " + rc);
		
			// レスポンスを解析してエリアIDを取得
			LineReader lineReader = new LineReader(auth2Con.openDataInputStream());
			for(;;)
			{
				try {
					String line = new String(lineReader.readLine());
					if(line.length() != 0)
					{
						// エリア内の場合は'JP'から、エリア外の場合は'OUT'から始まる
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
	
	public String getCurrentAreaID()
	{
		return areaID;
	}
	
	private static int decodeInt16(byte[] val)
	{
		// リトルエンディアン
		return ((int)(val[0] & 0xFF) | (int)(val[1] & 0xFF) << 8);
	}
	
	private static int decodeInt32(byte[] val)
	{
		// リトルエンディアン
		return ((int)(val[0] & 0xFF) | (int)(val[1] & 0xFF) << 8 | (int)(val[2] & 0xFF) << 16 | (int)(val[3] & 0xFF) << 24);
	}
	
	
	private void getAuthKey() throws Exception
	{
		String url = "http://radiko.jp/player/swf/player_3.0.0.01.swf";
		
		ConnectionFactory _factory = _app.getConnectionFactory();
		if(_factory == null)
			throw new IOException("getAuthKey() _factory Error");
		
		// Get swf file
		ConnectionDescriptor conDescriptor = _factory.getConnection(url);
		if(conDescriptor == null)
			throw new IOException("getAuthKey() conDescriptor Error");
		
		HttpConnection httpconn = (HttpConnection) conDescriptor.getConnection();
		
		try {
			httpconn.setRequestMethod(HttpConnection.GET);
		
			int rc = httpconn.getResponseCode();
			if (rc != HttpConnection.HTTP_OK)
				throw new IOException("getAuthKey() HTTP response code: " + rc);
		
			parseSWF(httpconn.openInputStream());
		
		} finally {
			if(httpconn != null){ httpconn.close(); }
		}
	} //getAuthKey()
	
	private void parseSWF(InputStream is) throws Exception
	{
		// SWF and AMF Technology Center | Adobe Developer Connection
		// http://www.adobe.com/devnet/swf.html
		
		// SWF File Format Specification (version 19)
		// http://wwwimages.adobe.com/www.adobe.com/content/dam/Adobe/en/devnet/swf/pdf/swf-file-format-spec.pdf
		
		// The SWF header
		// Signature  | UI8  | Signature byte:
		//                     “F” indicates uncompressed
		//                     “C” indicates a zlib compressed SWF (SWF 6 and later only)
		//                     “Z” indicates a LZMA compressed SWF (SWF 13 and later only)
		// Signature  | ￼UI8  | ￼Signature byte always “W”
		// ￼Signature  | UI8  | Signature byte always “S”
		// ￼Version    | UI8  | Single byte file version (for example, 0x06 for SWF 6)
		// FileLength | UI32 | ￼￼Length of entire file in bytes
		// ￼FrameSize  | ￼RECT | Frame size in twips
		// FrameRate  | UI16 | Frame delay in 8.8 fixed number of frames per second
		// FrameCount | ￼UI16 | ￼Total number of frames in file
		
		//---- Signature
		byte[] header_Signature = readbytesFromStream(is, 3);
		if(!Arrays.equals(header_Signature, new byte[]{'C', 'W', 'S'}))
			throw new Exception("Failed to parse header_Signature"); 		
		//updateStatus("Signature: " + new String(header_Signature));
		
		//---- Version
		// 不要なのでスキップ
		is.skip(1);
		//byte[] header_Version = readbytesFromStream(is, 1);
		//updateStatus("Version: " + header_Version[0]);
		
		
		//---- FileLength
		// 不要なのでスキップ
		is.skip(4);
		//byte[] header_FileLength = readbytesFromStream(is, 4);
		//updateStatus("FileLength: " + decodeInt32(header_FileLength));
				
		//---- ここからzlib解凍 ----//
		
		//---- zlib
		ZLibInputStream _zlibIS = new ZLibInputStream(is, false);
		
		//---- FrameSize
		// 長さは計算しません。べたでいきます。すみません。
		// 不要なのでスキップ
		_zlibIS.skip(1+7);
		//byte[] header_FrameSize = readbytesFromStream(_zlibIS, 1+7);
		//updateStatus("FrameSize: " + ByteArrayUtilities.byteArrayToHex(header_FrameSize));
		
		//---- FrameRate
		//　不要なのでスキップ
		_zlibIS.skip(2);
		//byte[] header_FrameRate = readbytesFromStream(_zlibIS, 2);
		//updateStatus("FrameRate: " + header_FrameRate[1] + "." + header_FrameRate[0]);
		
		//---- FrameCount
		// 不要なのでスキップ
		_zlibIS.skip(2);
		//byte[] header_FrameCount = readbytesFromStream(_zlibIS, 2);		
		//updateStatus("FrameCount: " + decodeInt16(header_FrameCount));
		
		//---- ヘッダここまで ----//
		//---- タグここから ----//
		
		while(true)
		{
			// Tag format
			// RECORDHEADER (short)
			// TagCodeAndLength | UI16 | Upper 10 bits: tag type Lower 6 bits: tag length
			//
			// RECORDHEADER (long)
			// TagCodeAndLength | UI16 | Tag type and length of 0x3F Packed together as in short header
			// Length           | UI32 | Length of tag
			
			int tagCode = 0;
			int tagLength = 0;
			
			byte[] tagCodeAndLength = readbytesFromStream(_zlibIS, 2);
			tagCode = (tagCodeAndLength[1] << 2) | ((tagCodeAndLength[0] & 0xC0) >> 6);
			
			if((tagCodeAndLength[0] & 0x3F) != 0x3F)
				tagLength = tagCodeAndLength[0] & 0x3F;
			else
				tagLength = decodeInt32(readbytesFromStream(_zlibIS, 4));
			
			// CodeがDefineBinaryData(Tag type 0x57)の場合のみ処理
			if(tagCode == 0x57)
			{
				// DefineBinaryData
				// Header   | RECORDHEADER | ￼Tag type = 87(0x57)
				// Tag      | ￼UI16         | ￼16-bit character ID
				// Reserved | ￼U32          | ￼￼Reserved space; must be 0
				// ￼Data     | ￼BINARY       | A blob of binary data, up to the end of the tag
				
				byte[] tag = readbytesFromStream(_zlibIS, 2);
				
				// 目的のデータは14に保存されている。14の場合のみ処理
				if(decodeInt16(tag) == 14)
				{
					byte[] reserved = readbytesFromStream(_zlibIS, 4);
					if(decodeInt32(reserved) != 0)
						throw new Exception("Tag parser Error");
					authkey = readbytesFromStream(_zlibIS, tagLength - (2+4));
				}
				else
				{
					_zlibIS.skip(tagLength -2);
				}
			}
			else
			{
				_zlibIS.skip(tagLength);
			}
			
			if(tagCode == 0)
				break;
		}		
	}  //parseSWF()
	
	private byte[] readbytesFromStream(InputStream is, int length) throws Exception 
	{
		byte[] b = new byte[length];
		int totalReadNum = 0;
		while(totalReadNum < b.length)
		{
			int readNum = 0;
			if((readNum = is.read( b, totalReadNum, b.length - totalReadNum)) == -1)
				throw new Exception("Failed to read");     	
			totalReadNum += readNum;
		}
		
		return b;
	} //readbytesFromStream()
	
	private void updateStatus(String val)
	{
		_app.updateStatus("[Auth] " + val);
	}
}
