/*
	ProgInfoScreen.java
	
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

import java.io.UnsupportedEncodingException;

import net.rim.blackberry.api.browser.Browser;
import net.rim.blackberry.api.browser.BrowserSession;
import net.rim.device.api.browser.field2.BrowserField;
import net.rim.device.api.browser.field2.BrowserFieldConfig;
import net.rim.device.api.browser.field2.BrowserFieldNavigationRequestHandler;
import net.rim.device.api.browser.field2.BrowserFieldRequest;
import net.rim.device.api.browser.field2.ProtocolController;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.component.StandardTitleBar;
import net.rim.device.api.ui.container.MainScreen;


public class ProgInfoScreen extends MainScreen
{
	//private MyApp _app;
	private BrowserField _browserField;
		
	
	public ProgInfoScreen()
	{
		//_app = (MyApp) UiApplication.getUiApplication();
		
		//---- タイトルバーを作成
		StandardTitleBar _titleBar = new StandardTitleBar();
		_titleBar.addSignalIndicator();
		_titleBar.addNotifications();
		_titleBar.addClock();
		_titleBar.setPropertyValue(StandardTitleBar.PROPERTY_BATTERY_VISIBILITY, StandardTitleBar.BATTERY_VISIBLE_ALWAYS);
		setTitleBar(_titleBar);
		_titleBar = null;
		
		// BrowserField Sample Code - Using the BrowserFieldC... - BlackBerry Support Community Forums
		// http://supportforums.blackberry.com/t5/Java-Development/BrowserField-Sample-Code-Using-the-BrowserFieldConfig-class/ta-p/495716		
		_browserField = new BrowserField();
		
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
		
	} //ProgramInfoScreen()
	
	
	public void addContents(final Program prog) throws UnsupportedEncodingException
	{
		// 引数チェック
		if(prog == null) { throw new NullPointerException("prog"); }
		
		// 表示内容をHTMLで作成
		// STYLE
		String style = "body{font-size: " + (Font.getDefault().getHeight(Ui.UNITS_px) - 2) + "px;}";
		style = style + "dd {margin-left: 0; font-size: " + (Font.getDefault().getHeight(Ui.UNITS_px) - 5) + "px;}";
		
		// BODY
		String body = "<dl><dt>" + prog.getTitle() + "</dt>" + "<dd>" + prog.getTime() + "</dd>";
		
		if(prog.getPfm() != null)
		{
			body = body + "<dd>" + prog.getPfm() + "</dd>";
		}
		
		if(prog.getUrl() != null)
		{
			body = body + "<dd>" + "<a href=\"" + prog.getUrl() + "\">" + prog.getUrl() + "</a>" + "</dd>";
		}
		
		body = body + "</dl>";
		body = body + "<hr />";
		
		if(prog.getDescription() != null)
		{
			body = body + "<p>" + prog.getDescription() + "</p>";
		}
		if(prog.getInfo() != null)
		{
			body = body + "<p>" + prog.getInfo() + "</p>";
		}
		
		String str = "<html><head><style type=\"text/css\">" + style + "</style></head><body>" + body + "</body>" + "</html>";
		
		// 作成したHTMLを表示
		_browserField.displayContent(str.getBytes("utf-8"), "text/html; charset=utf-8", "http://localhost/");
		
	} //addContents()
	
	
	/*private void updateStatus(final String message)
	{
		synchronized (UiApplication.getEventLock()) 
		{
			_app.updateStatus("[PIS] " + message);
		}
	}*/ //updateStatus()
}