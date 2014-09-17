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
import java.util.Hashtable;

import javax.microedition.io.HttpConnection;
import javax.microedition.io.HttpsConnection;

import net.rim.device.api.compress.ZLibInputStream;
import net.rim.device.api.io.Base64OutputStream;
import net.rim.device.api.io.LineReader;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.util.Arrays;


public class Auth
{
	private MyApp _app;
	
	
	public Auth(UiApplication app)
	{
		_app = (MyApp) app;
	}
	
	
	public void doAuth() throws Exception
	{
		ConnectionFactory _connfactory = _app.getConnectionFactory();
		
		// Get AuthToken, Keylength, Keyoffset
		Hashtable tmp = getAuthtokenAndKeylengthAndKeyoffset(_connfactory);
		String authToken = ((String)tmp.get("authToken"));
		int keylength = Integer.parseInt((String)tmp.get("keylength"));
		int keyoffset = Integer.parseInt((String)tmp.get("keyoffset"));
		tmp.clear();
		tmp = null;
		
		// Get Authkey
		byte[] authkey = getAuthKey(_connfactory);
		
		// Get PartialKey
		String partialkey = getPartialkey(authkey, keylength, keyoffset);
		
		// Get AreaID
		String areaID = getAreaID(_connfactory, authToken, partialkey);
		
		// Store
		_app.setAuthToken(authToken);
		_app.setAreaID(areaID);
		
		updateStatus("Area_ID: " + areaID);
		
	} //doAuth()
	
	
	private String getAreaID(ConnectionFactory _connfactory, String authToken, String partialkey) throws IOException, Exception
	{
		_app.getMainScreen().updateStatusField("接続中... (4/4)");
		
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException(); }
		if(authToken == null) { throw new NullPointerException(); }
		if(authToken.length() == 0) { throw new IllegalArgumentException(); }
		if(partialkey == null) { throw new NullPointerException(); }
		if(partialkey.length() == 0) { throw new IllegalArgumentException(); }
		
		
		// 有効な通信経路があるか確認
		if(!MyApp.isCoverageSufficient()) { throw new IOException("OutCoverage"); }
		
		final String url = "https://radiko.jp/v2/api/auth2_fms";
		//final String url = "https://radiko.jp/v2/api/auth2_fms;deviceside=false;connectionUID=GPMDSAP01";
		
		//
		Hashtable property = new Hashtable();
		property.put("pragma", "no-cache");
		property.put("X-Radiko-App", "pc_1");
		property.put("X-Radiko-App-Version", "2.0.1");
		property.put("X-Radiko-Authtoken", authToken);
		property.put("X-Radiko-Partialkey", partialkey);
		
		HttpsConnection auth2Con = null;
		
		try {
			auth2Con = MyApp.doPost(_connfactory, url, property);
			
			// レスポンスを解析してエリアIDを取得
			String out = "";
			LineReader lineReader = new LineReader(auth2Con.openDataInputStream());
			for(;;)
			{
				try {
					String line = new String(lineReader.readLine());
					if(line.length() != 0)
					{
						// エリア内の場合は'JP'から、エリア外の場合は'OUT'から始まる
						if(line.startsWith("OUT")) {
							throw new Exception("Out of Area");
						} else if(line.startsWith("JP")) {
							int comma;
							if((comma = line.indexOf(",")) == -1)
							{
								throw new Exception("Failed to get the AreaID (Not found 'comma')");
							}
							
							out = line.substring(0, comma);
							
						} else {
							throw new Exception("Failed to get the AreaID (Not found 'JP')");
						}
					}
				} catch(EOFException eof) {
					break;
				}
			} //for
			
			return out;
			
		} finally {
			if(auth2Con != null){ auth2Con.close(); auth2Con = null; }
		}
	} //getAreaID()
	
	
	private byte[] getAuthKey(ConnectionFactory _connfactory) throws IOException, Exception
	{
		_app.getMainScreen().updateStatusField("接続中... (2/4)");
		
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException(); }
		
		// 有効な通信経路があるか確認
		if(!MyApp.isCoverageSufficient()) { throw new IOException("OutCoverage"); }
		
		HttpConnection httpconn = null;
		
		try {
			final String url = "http://radiko.jp/player/swf/player_3.0.0.01.swf";
			
			httpconn = MyApp.doGet(_connfactory, url);
			
			byte[] out = parseSWF(httpconn);
			
			return out;
		} finally {
			if(httpconn != null){ httpconn.close(); httpconn = null;}
		}
	} //getAuthKey()
	
	
	private Hashtable getAuthtokenAndKeylengthAndKeyoffset(ConnectionFactory _connfactory) throws IOException, Exception
	{
		_app.getMainScreen().updateStatusField("接続中... (1/4)");
		
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException(); }
		
		// 有効な通信経路があるか確認
		if(!MyApp.isCoverageSufficient()) { throw new IOException("OutCoverage"); }
		
		final String url = "https://radiko.jp/v2/api/auth1_fms";
		//final String url = "https://radiko.jp/v2/api/auth1_fms;deviceside=false;connectionUID=GPMDSAP01";
		
		Hashtable property = new Hashtable();
		property.put("pragma", "no-cache");
		property.put("X-Radiko-App", "pc_1");
		property.put("X-Radiko-App-Version", "2.0.1");
		property.put("X-Radiko-User", "test-stream");
		property.put("X-Radiko-Device", "pc");
		
		HttpsConnection auth1Con = null;
		
		try {
			auth1Con = MyApp.doPost(_connfactory, url, property);
			
			Hashtable out = new Hashtable();
			out.put("authToken", auth1Con.getHeaderField("X-RADIKO-AUTHTOKEN"));
			out.put("keylength", auth1Con.getHeaderField("X-Radiko-KeyLength"));
			out.put("keyoffset", auth1Con.getHeaderField("X-Radiko-KeyOffset"));
			
			return out;
			
		} finally {
			if(auth1Con != null){ auth1Con.close(); auth1Con = null; }
		}
	} //getAuthtokenAndKeylengthAndKeyoffset()
	
	
	private String getPartialkey(byte[] authkey, int keylength, int keyoffset) throws IOException
	{
		_app.getMainScreen().updateStatusField("接続中... (3/4)");
		
		// 引数チェック
		if(authkey == null) { throw new NullPointerException(); }
		if(authkey.length == 0) { throw new IllegalArgumentException(); }
		if(keylength == 0) { throw new IllegalArgumentException(); }
		
		InputStream fconnIS  = null;
		try {
			fconnIS = new ByteArrayInputStream(authkey);
			
			byte[] b1 = new byte[ keylength ];
			
			fconnIS.skip(keyoffset);
			fconnIS.read(b1, 0, keylength);
			
			String out = new String( Base64OutputStream.encode(b1, 0, b1.length, false, false) );
			
			return out;
		
		} finally {
			if(fconnIS != null) { fconnIS.close(); fconnIS = null; }
		}
	} //getPartialkey()
	
	
	private byte[] parseSWF(HttpConnection httpconn) throws Exception
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
		// Signature  | UI8  | Signature byte always “W”
		// Signature  | UI8  | Signature byte always “S”
		// Version    | UI8  | Single byte file version (for example, 0x06 for SWF 6)
		// FileLength | UI32 | Length of entire file in bytes
		// FrameSize  | RECT | Frame size in twips
		// FrameRate  | UI16 | Frame delay in 8.8 fixed number of frames per second
		// FrameCount | UI16 | Total number of frames in file
		
		InputStream is = null;
		ZLibInputStream _zlibIS = null;
		
		try {
			byte[] out = null;
			
			is = httpconn.openInputStream();
			
			//---- Signature
			byte[] header_Signature = readbytesFromStream(is, 3);
			if(!Arrays.equals(header_Signature, new byte[]{'C', 'W', 'S'}))
			{
				throw new Exception("Failed to parse header_Signature");
			}
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
			_zlibIS = new ZLibInputStream(is, false);
			
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
				
				if((tagCodeAndLength[0] & 0x3F) != 0x3F) {
					tagLength = tagCodeAndLength[0] & 0x3F;
				} else {
					tagLength = decodeInt32(readbytesFromStream(_zlibIS, 4));
				}
				
				// CodeがDefineBinaryData(Tag type 0x57)の場合のみ処理
				if(tagCode == 0x57) {
					// DefineBinaryData
					// Header   | RECORDHEADER | Tag type = 87(0x57)
					// Tag      | UI16         | 16-bit character ID
					// Reserved | U32          | Reserved space; must be 0
					// Data     | BINARY       | A blob of binary data, up to the end of the tag
					
					byte[] tag = readbytesFromStream(_zlibIS, 2);
					
					// 目的のデータは14に保存されている。14の場合のみ処理
					if(decodeInt16(tag) == 14) {
						byte[] reserved = readbytesFromStream(_zlibIS, 4);
						
						if(decodeInt32(reserved) != 0) 
						{
							throw new Exception("Tag parser Error");
						}
						
						out = readbytesFromStream(_zlibIS, tagLength - (2+4));
					} else {
						_zlibIS.skip(tagLength -2);
					}
				} else {
					_zlibIS.skip(tagLength);
				}
				
				if(tagCode == 0) { break; }
			}
			
			return out;
			
		} finally {
			if(_zlibIS != null) { _zlibIS.close(); _zlibIS = null; }
			if(is != null) { is.close(); is = null; }
		}
	} //parseSWF()
	
	
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
}
