/*
	Network.java
	
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

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.HttpConnection;
import javax.microedition.io.HttpsConnection;

import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.io.transport.TransportInfo;
import net.rim.device.api.system.Bitmap;



public class Network
{
	private static final int NUM_OF_TRIALS = 3;
	
	
	public static HttpConnection doGet(ConnectionFactory _connfactory, final String url) throws Exception
	{
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException(); }
		if(url == null) { throw new NullPointerException(); }
		if(url.length() == 0) { throw new IllegalArgumentException(); }
		
		
		// 
		ConnectionDescriptor conDescriptor = _connfactory.getConnection(url);
		
		if(conDescriptor == null)
		{ 
			throw new Exception("Failed to open connection to '" + url + "'");
		}
		
		HttpConnection out = (HttpConnection) conDescriptor.getConnection();
		out.setRequestMethod(HttpConnection.GET);
	
		int rc = out.getResponseCode();
		if (rc != HttpConnection.HTTP_OK)
		{
			throw new Exception("HTTP response code: " + rc);
		}
		
		return out;
				
	} //doGet()
	
	
	public static HttpsConnection doPost(ConnectionFactory _connfactory, final String url, final Hashtable property) throws Exception
	{
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException(); }
		if(url == null) { throw new NullPointerException(); }
		if(url.length() == 0) { throw new IllegalArgumentException(); }
		if(property == null) { throw new NullPointerException(); }
			
		
		ConnectionDescriptor conDescriptor = _connfactory.getConnection( url );
		
		if(conDescriptor == null)
		{
			throw new Exception("Failed to open connection to '" + url + "'");
		}
		
		HttpsConnection out = (HttpsConnection) conDescriptor.getConnection();
		
		// Set the request method and headers
		out.setRequestMethod(HttpsConnection.POST);
		
		//
		for(Enumeration e = property.keys(); e.hasMoreElements();)
		{
			String key = (String) e.nextElement();
			String value = (String) property.get(key);
			out.setRequestProperty(key, value);
		}
		
		int rc = out.getResponseCode();
		if (rc != HttpsConnection.HTTP_OK)
		{
			throw new Exception("HTTP response code: " + rc);
		}
		
		return out;
		
	} //doPost()
	
	
	public static Bitmap getWebBitmap(ConnectionFactory _connfactory, final String url) throws Exception
	{
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException("ConnectionFactory"); }
		if(url == null) { throw new NullPointerException("url"); }
		if(url.length() == 0) { throw new IllegalArgumentException("url"); }
		
		// 有効な通信経路があるか確認
		if(!Network.isCoverageSufficient()) { throw new Exception("OutOfCoverage"); }
		
		String errorlog = "Network::getWebBitmap()\n";
		
		for(int j=0; j<NUM_OF_TRIALS; j++)
		{
			InputStream is = null;
			HttpConnection httpconn = null;
			
			try {
				httpconn = Network.doGet(_connfactory, url);
				is = httpconn.openInputStream();
				if(is == null) { throw new Exception("[bitmap] imageData Error"); }
				
				byte[] imageData = IOUtilities.streamToBytes(is);
					
				Bitmap out = Bitmap.createBitmapFromBytes(imageData, 0, -1, Bitmap.SCALE_TO_FIT);
				
				return out;
				
			} catch (Exception e) {
				errorlog += e.toString() + "\n";
			} finally {
				if(is != null){ is.close(); is = null; }
				if(httpconn != null){ httpconn.close(); httpconn = null; }
			}
			
			Thread.sleep(1000);
		}
		throw new Exception(errorlog);
	} //getWebBitmap()
	
	
	public static boolean isCoverageSufficient()
	{
		int [] transportTypes = TransportInfo.getCoverageStatus();
		
		if (transportTypes.length != 0) {
			return true;
		} else {
			return false;
		}
	} //isCoverageSufficient()
	
	
	public static ConnectionFactory selectTransport()
	{
		/*
		int[] availableTransport = TransportInfo.getAvailableTransportTypes();
	
		for(int i=0; i<availableTransport.length; i++)
		{
			updateStatus("availableTransport: " + Integer.toString(availableTransport[i]));
		}
		*/
		
		ConnectionFactory _connectionFactory = new ConnectionFactory();
		
		_connectionFactory.setTimeLimit(15000L);
		
		int[] preferredTransports = new int[]{
				TransportInfo.TRANSPORT_TCP_WIFI,
				TransportInfo.TRANSPORT_TCP_CELLULAR
		};
		_connectionFactory.setPreferredTransportTypes(preferredTransports);
		
		// DirectTCP APN
		//TcpCellularOptions tcpOptions = new TcpCellularOptions();
		//tcpOptions.setApn("mpr.ex-pkt.net");
		//tcpOptions.setTunnelAuthUsername("");
		//tcpOptions.setTunnelAuthPassword("");
		//_connectionFactory.setTransportTypeOptions(TransportInfo.TRANSPORT_TCP_CELLULAR, tcpOptions);
		
		
		//
		int[] disallowedTransportTypes = new int[] {
				TransportInfo.TRANSPORT_BIS_B,
				TransportInfo.TRANSPORT_WAP,
				TransportInfo.TRANSPORT_WAP2,
				TransportInfo.TRANSPORT_MDS
		};
		_connectionFactory.setDisallowedTransportTypes(disallowedTransportTypes);
		
		return _connectionFactory;
		
	} //selectTransport()
}