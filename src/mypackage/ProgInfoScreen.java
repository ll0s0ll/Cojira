package mypackage;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import org.w3c.dom.Document;

import mypackage.MyScreen.DialogCommandHandler;
import net.rim.blackberry.api.browser.Browser;
import net.rim.blackberry.api.browser.BrowserSession;
import net.rim.device.api.browser.field.ContentReadEvent;
import net.rim.device.api.browser.field2.BrowserField;
import net.rim.device.api.browser.field2.BrowserFieldConfig;
import net.rim.device.api.browser.field2.BrowserFieldNavigationRequestHandler;
import net.rim.device.api.browser.field2.BrowserFieldRequest;
import net.rim.device.api.browser.field2.ProtocolController;
import net.rim.device.api.command.Command;
import net.rim.device.api.command.CommandHandler;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.script.ScriptEngine;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ActiveRichTextField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.StandardTitleBar;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.util.StringProvider;


public class ProgInfoScreen extends MainScreen
{
	
	final static String CURRENT = "prog_now";
	final static String NEXT    = "prog_next";
	
	private MyApp _app;
	private StandardTitleBar _titleBar;
	private BrowserField _browserField;
	
	
	//public ProgInfoScreen(String val, int rowNumber_with_focus)
	public ProgInfoScreen(Hashtable prog)
	{
		_app = (MyApp) UiApplication.getUiApplication();
		
		
		//---- タイトルバーを作成
        _titleBar = new StandardTitleBar() ;
    	_titleBar.addSignalIndicator();
    	_titleBar.addNotifications();
    	_titleBar.addClock();
    	_titleBar.setPropertyValue(StandardTitleBar.PROPERTY_BATTERY_VISIBILITY, StandardTitleBar.BATTERY_VISIBLE_ALWAYS);
    	setTitleBar(_titleBar);
 
		
		//Hashtable station = (Hashtable) _app._epg.GetStationsInfoV().elementAt(rowNumber_with_focus);
		//Hashtable prog = (Hashtable) station.get(val);
		
		//Font.getDefault().getHeight(Ui.UNITS_px);
		
    	// STYLE
		String style = "body{font-size: " + (Font.getDefault().getHeight(Ui.UNITS_px) - 2) + "px;}";
		style = style + "dd {margin-left: 0; font-size: " + (Font.getDefault().getHeight(Ui.UNITS_px) - 5) + "px;}";
		
		// BODY
		String body = "<dl><dt>" + (String)prog.get("title") + "</dt>" + "<dd>" + (String)prog.get("time") + "</dd>";
		
		if((String)prog.get("pfm") != null)
		{
			body = body + "<dd>" + (String)prog.get("pfm") + "</dd>";
		}
		
		if((String)prog.get("url") != null)
		{
			body = body + "<dd>" + "<a href=\"" + (String)prog.get("url") + "\">" + (String)prog.get("url") + "</a>" + "</dd>";
		}
		
		body = body + "</dl>";
		body = body + "<hr />";
		
		if(prog.get("desc") != null)
		{
			body = body + "<p>" + (String)prog.get("desc") + "</p>";
		}
		if(prog.get("info") != null)
		{
			body = body + "<p>" + (String)prog.get("info") + "</p>";
		}
		
		String str = "<html><head><style type=\"text/css\">" + style + "</style></head><body>" + body + "</body>" + "</html>";
		
		//ActiveRichTextField _artf = new ActiveRichTextField(str);
		//add(_artf);
		
		// BrowserField Sample Code - Using the BrowserFieldC... - BlackBerry Support Community Forums
		// http://supportforums.blackberry.com/t5/Java-Development/BrowserField-Sample-Code-Using-the-BrowserFieldConfig-class/ta-p/495716
		BrowserFieldConfig _config = new BrowserFieldConfig();
		//_config.setProperty(BrowserFieldConfig.INITIAL_SCALE, new Float(2.0));
		_config.setProperty(BrowserFieldConfig.VIEWPORT_WIDTH, new Integer(getWidth() / 2 ));
		//_config.setProperty(BrowserFieldConfig.VIEWPORT_WIDTH, new Integer(480));
		//_config.setProperty(BrowserFieldConfig.NAVIGATION_MODE, BrowserFieldConfig.NAVIGATION_MODE_POINTER);
		_browserField = new BrowserField(_config);
		
		// Use the default (full-featured) browser to view external http content
		// See..
		// BrowserField - open links in actual browser? - BlackBerry Support Community Forums
		// http://supportforums.blackberry.com/t5/Java-Development/BrowserField-open-links-in-actual-browser/m-p/468721
		ProtocolController hybridController = new ProtocolController(_browserField);      
		hybridController.setNavigationRequestHandler("http", new BrowserFieldNavigationRequestHandler() {                 
			public void handleNavigation(BrowserFieldRequest request)
	        {
				BrowserSession browser = Browser.getDefaultSession();
				browser.displayPage(request.getURL());                   
	        }
		});
		hybridController.setNavigationRequestHandler("https", new BrowserFieldNavigationRequestHandler() {                 
            public void handleNavigation(BrowserFieldRequest request)
            {
               BrowserSession browser = Browser.getDefaultSession();
               browser.displayPage(request.getURL());                   
            }
		});
	    _browserField.getConfig().setProperty(BrowserFieldConfig.CONTROLLER, hybridController);
		
		add(_browserField); 

		// BrowserField Encoding problem - BlackBerry Support Community Forums
		// http://supportforums.blackberry.com/t5/Java-Development/BrowserField-Encoding-problem/td-p/1428779
		try {
			_browserField.displayContent(str.getBytes("utf-8"), "text/html; charset=utf-8", "http://localhost/");
		} catch (UnsupportedEncodingException e) {
		}
		
	} //ProgramInfoScreen()
	
	protected void makeMenu( Menu menu, int instance )
    {   
        MenuItem _prog = new MenuItem(new StringProvider("選局"), 0x00020000, 0);
        _prog.setCommand(new Command(        
	        new CommandHandler()
	        {
	        	public void execute(ReadOnlyCommandMetadata metadata, Object context)
	            {
	        		//Dialog.inform("NAV_MODE: " + _browserField.getConfig().getProperty(BrowserFieldConfig.NAVIGATION_MODE)); //.equals(BrowserFieldConfig.NAVIGATION_MODE_POINTER);
	        		
	        		_browserField.getConfig().setProperty(BrowserFieldConfig.NAVIGATION_MODE, BrowserFieldConfig.NAVIGATION_MODE_POINTER);
	        		
	        		//_browserField.getConfig().setProperty(key, value);
	            }        	
	        }
        ));
        
        menu.add(_prog);        
        
        super.makeMenu(menu, instance);
    };
	
	private void updateStatus(final String message)
    {
    	synchronized (UiApplication.getEventLock()) 
		{			
			_app.updateStatus("[PIS] " + message);
		}
    } //updateStatus()
}