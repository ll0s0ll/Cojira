/*
	MyApp.java
	
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

import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.io.transport.TransportInfo;
import net.rim.device.api.io.transport.options.TcpCellularOptions;
import net.rim.device.api.ui.UiApplication;

/**
 * This class extends the UiApplication class, providing a
 * graphical user interface.
 */
public class MyApp extends UiApplication
{

	private MyScreen _screen;
	public MediaActions _mediaActions;
	public Auth _auth;
	public EPG _epg;
	private Runnable _onCloseRunnable;
	private ConnectionFactory _connectionFactory;
	
	/**
	 * Entry point for application
	 * @param args Command line arguments (not used)
	 */ 
	public static void main(String[] args)
	{
		// Create a new instance of the application and make the currently
		// running thread the application's event dispatch thread.
		MyApp theApp = new MyApp();
		theApp.enterEventDispatcher();
	}
	
	
	/**
	 * Creates a new MyApp object
	 */
	public MyApp()
	{
		_mediaActions = new MediaActions(this);
		_auth = new Auth(this);
		_epg = new EPG(this);
		
		// Push a screen onto the UI stack for rendering.
		_screen = new MyScreen();
		_screen.setOnCloseRunnable(new Runnable()
		{
			public void run(){ close(); }
		});
		pushScreen(_screen);
	} //MyApp()
	
	
	public void setOnCloseRunnable( Runnable runnable )
	{
		_onCloseRunnable = runnable;
	}
	
	private void close()
	{
		Runnable runnable = _onCloseRunnable;
		if ( runnable != null )
		{
			_onCloseRunnable = null;
			try
			{
				runnable.run();
			}
			catch ( Throwable e )
			{
			}
		}
		
		if(_mediaActions != null){ _mediaActions = null; }
		if(_auth != null){ _auth = null; }
	}
	
	public ConnectionFactory getConnectionFactory()
	{
		return _connectionFactory;
	}
	
	public MyScreen getMainScreen()
	{
		return _screen;
	}
	
	public void selectTransport()
	{
		/*
		int[] availableTransport = TransportInfo.getAvailableTransportTypes();
	
		for(int i=0; i<availableTransport.length; i++)
		{
			updateStatus("availableTransport: " + Integer.toString(availableTransport[i]));
		}
		*/
		
		_connectionFactory = new ConnectionFactory();
		
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
		
		//return _connectionFactory;
	} //selectTransport()
	
	public void updateStatus(String val)
	{
		_screen.updateStatus(val);
	}
} //MyApp
