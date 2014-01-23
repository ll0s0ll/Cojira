/*
	MyApp.java
	
	Copyright (C) 2014  Shun ITO
	
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
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.TransitionContext;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.UiEngineInstance;

/**
 * This class extends the UiApplication class, providing a
 * graphical user interface.
 */
public class MyApp extends UiApplication
{

	private MyScreen _screen;
	private EPGScreen _epgScreen; 
	public MediaActions _mediaActions;
	public Auth _auth;
	public EPG _epg;
	private Runnable _onCloseRunnable;
	private ConnectionFactory _connectionFactory;
	
	//private InitThread _initThread;
	
	//public StationsScreen _secondaryScreen;
	public StationsScreen2 _secondaryScreen2;
	
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
    	
    	_screen.setOnCloseRunnable( new Runnable()
        {
            public void run(){ close(); }
        } );
    	
        pushScreen(_screen);
        
        //_secondaryScreen = new StationsScreen("Title", Color.LIGHTBLUE);
        _secondaryScreen2 = new StationsScreen2();
        _secondaryScreen2.setOnCloseRunnable( new Runnable()
        {
            public void run()
            {
            	//popScreen(_screen);
            	_screen.close();
            }
        } );
        pushScreen(_secondaryScreen2);
        
        // FADE IN
        TransitionContext transition = new TransitionContext(TransitionContext.TRANSITION_FADE);
        transition.setIntAttribute(TransitionContext.ATTR_DURATION, 100);

        // WIPE IN
        //TransitionContext transition = new TransitionContext(TransitionContext.TRANSITION_WIPE);
        //transition.setIntAttribute(TransitionContext.ATTR_DURATION, 200);
        //transition.setIntAttribute(TransitionContext.ATTR_DIRECTION, TransitionContext.DIRECTION_UP);
        
        // SLIDE IN
        //TransitionContext transition = new TransitionContext(TransitionContext.TRANSITION_SLIDE);
        //transition.setIntAttribute(TransitionContext.ATTR_DURATION, 200);
        //transition.setIntAttribute(TransitionContext.ATTR_DIRECTION, TransitionContext.DIRECTION_RIGHT);              
         
        
        UiEngineInstance engine = Ui.getUiEngineInstance();
        engine.setTransition(null, _secondaryScreen2, UiEngineInstance.TRIGGER_PUSH, transition);

        // FADE OUT
        transition = new TransitionContext(TransitionContext.TRANSITION_FADE);
        transition.setIntAttribute(TransitionContext.ATTR_DURATION, 100);
        transition.setIntAttribute(TransitionContext.ATTR_KIND, TransitionContext.KIND_OUT);
        
        // WIPE OUT
        //transition = new TransitionContext(TransitionContext.TRANSITION_WIPE);
        //transition.setIntAttribute(TransitionContext.ATTR_DURATION, 200);
        //transition.setIntAttribute(TransitionContext.ATTR_DIRECTION, TransitionContext.DIRECTION_DOWN);  
        //transition.setIntAttribute(TransitionContext.ATTR_KIND, TransitionContext.KIND_OUT);          
        
        // SLIDE OUT
        //transition = new TransitionContext(TransitionContext.TRANSITION_SLIDE);
        //transition.setIntAttribute(TransitionContext.ATTR_DURATION, 200);
        //transition.setIntAttribute(TransitionContext.ATTR_DIRECTION, TransitionContext.DIRECTION_LEFT);                                                            
        //transition.setIntAttribute(TransitionContext.ATTR_KIND, TransitionContext.KIND_OUT);     
        
        engine.setTransition(_secondaryScreen2, null, UiEngineInstance.TRIGGER_POP, transition);
        
        //_initThread = new InitThread();
        //_initThread.start();
    	
    	
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
    
    
    public void updateStatus(String val)
    {    	
    	//_screen.updateStatus(val);
    	_secondaryScreen2.updateStatus(val);
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
    }
    
    public ConnectionFactory GetConnectionFactory()
    {
    	return _connectionFactory;
    }
    
    public StationsScreen2 getMainScreen()
    {
    	return _secondaryScreen2;
    }
    
    public void pushMyScreen()
    {
    	synchronized (UiApplication.getEventLock()) 
		{
			//_screen.AddStationList();
			//pushScreen(_secondaryScreen);
			pushScreen(_secondaryScreen2);
			//pushScreen(_testScreen);
		}
    }
    
    public void popMyScreen()
    {
    	synchronized (UiApplication.getEventLock()) 
		{
    		Screen activeScreen = getActiveScreen();                
            popScreen(activeScreen);   
		}
    }
    
    public void pushEPGScreen()
    {
    	synchronized (UiApplication.getEventLock()) 
		{
			//_screen.AddStationList();
			pushScreen(_epgScreen);
			//pushScreen(_testScreen);
		}
    }
    
    /*
    final class InitThread extends Thread
    { 
    	InitThread()
    	{
    		// DO NOTHING
    	}
    	
    	
    	public void run()
		{
			try {
				updateStatus("Init Start");

				//_mediaActions = new MediaActions();
				_mediaActions.Init();
				
				updateStatusCommnet("トランスポートを選択中...");
				
				//selectTransport();
				
				updateStatusCommnet("エリア判定中...");
    							
    			//_auth = new Auth(_connectionFactory);
    			_auth.doAuth();
    			
    			updateStatusCommnet("放送局を取得中...");
    			
				//_epg = new EPG(_connectionFactory);
				_epg.getStationList();
				//_epg.GetStationsLogo();
				
				//_secondaryScreen.acc();
				//_secondaryScreen.getProgramInfo();				
				
				
				_secondaryScreen2 = new StationsScreen2();
				
  				updateStatusCommnet("radikoに接続しました。現在のエリアは「" + _epg.getAreaName() + "」です。");
  				
  				
    			synchronized (UiApplication.getEventLock()) 
    			{
    				_screen.deleteField();  
    			}
    			
    
				//_epg.GetProgramInfo();

				
				updateStatus("Init Done");
				//pushMyScreen();
				
			} catch (Exception e) {
				updateStatus("MyApp() " + e.toString());
				// And.. Retry...
			}
			
		} //run()
    	
    	
    	private void updateStatusCommnet(String val)
    	{
    		synchronized (UiApplication.getEventLock()) 
			{
				_screen.SetStatusCommand(val);
			}
    	} //UpdateStatusCommnet
    	
    } //InitThread
    */
} //MyApp
