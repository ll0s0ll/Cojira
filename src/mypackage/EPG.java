/*
	EPG.java
	
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import javax.microedition.io.HttpConnection;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.xml.jaxp.SAXParserImpl;


public final class EPG
{
	private MyApp _app;
	
	private String areaID;
	private String areaName;
	private Vector stationsInfoV;
	private int currentStation;
	
	
	public EPG(UiApplication app)
	{
		_app = (MyApp) app;
	}
	
	public String getAreaID()
	{
		return areaID;
	}
	
	public String getAreaName()
	{
		return areaName;
	}
	
	public int GetCurrentStation()
	{
		//return (String) ((Hashtable)stationsInfoV.elementAt(currentStation)).get("id");
		return currentStation;
	}
	
	public String GetCurrentStationID()
	{
		return (String) ((Hashtable)stationsInfoV.elementAt(currentStation)).get("id");
		//return currentStation;
	}
	
	public String getCurrentStationName()
	{
		return (String) ((Hashtable)stationsInfoV.elementAt(currentStation)).get("name");
	}
	
	public void getProgramInfo() throws Exception
	{
		if((areaID = _app._auth.getCurrentAreaID()) == null)
			throw new Exception("AreaID Error");
		
		String url = "http://radiko.jp//v2/api/program/now?area_id=" + areaID;
		ProgramParserHandler _handler = new ProgramParserHandler();
		
		getAndParseXML(url, _handler);
	} //getProgramInfo()
	
	public Vector GetStationsInfoV()
	{
		return stationsInfoV;
	}
	
	public void getStationList() throws Exception
	{
		HttpConnection httpconn = null;
		
		try {
			updateStatus("Connecting..(EPG)");

			if((areaID = _app._auth.getCurrentAreaID()) == null)
				throw new Exception("AreaID Error");
						
			String url = "http://radiko.jp/v2/station/list/" + areaID + ".xml";
			
			stationsInfoV = new Vector();
			StationParserHandler _handle = new StationParserHandler();
			getAndParseXML(url, _handle);
			
			//放送局のロゴ画像を取得
			getStationsLogo();
			
		} finally {
			if(httpconn != null){ httpconn.close(); }
		}
	} //getStationList()
	
	public void SetCurrentStation(int val)
	{
		currentStation = val;
	}
	
	private void getAndParseXML(String url, DefaultHandler handler) throws Exception
	{
		SAXParserImpl saxparser = new SAXParserImpl();
		//ListParser receivedListHandler = new ListParser();
		HttpConnection httpconn = null;
		 
		try {
			//updateStatus("Connecting..(EPG INFO SAX)");
	
			ConnectionDescriptor conDescriptor = _app.getConnectionFactory().getConnection( url );
			
			if (conDescriptor == null)
				throw new Exception("conDescriptor ERROR");
			
			// using the connection
			httpconn = (HttpConnection) conDescriptor.getConnection();
				
			// Set the request method and headers
			httpconn.setRequestMethod(HttpConnection.GET);

			int rc = httpconn.getResponseCode();
			if (rc != HttpConnection.HTTP_OK)
				throw new IOException("SAX HTTP response code: " + rc);
			
			saxparser.parse(httpconn.openDataInputStream(), handler);
			//saxparser.parse(url, handler, false);
			
		} finally {
			if(httpconn != null){ httpconn.close(); }
		}
	} //getAndParseXML()
	
	private void getStationsLogo() throws Exception
	{
		if(stationsInfoV == null)
			throw new Exception("stationsInfoV is NULL");
		
		for(Enumeration e = stationsInfoV.elements(); e.hasMoreElements(); )
		{
			Hashtable station = (Hashtable) e.nextElement();
	
			// LOGO
			Bitmap bitmap = GetWebBitmap((String) station.get("logo_medium"));

			station.put("station_logo",	bitmap);
		}
	}
	
	private Bitmap GetWebBitmap(String url) throws Exception
	{
		InputStream is;
		byte[] imageData;
	
		ConnectionFactory _factory = _app.getConnectionFactory();
		if(_factory == null)
			throw new IOException("[bitmap] _factory Error");

	
		ConnectionDescriptor conDescriptor = _factory.getConnection(url);
		if(conDescriptor == null)
			throw new IOException("[bitmap] conDescriptor Error");

		HttpConnection httpconn = (HttpConnection) conDescriptor.getConnection();

		try {
			httpconn.setRequestMethod(HttpConnection.GET);

			int rc = httpconn.getResponseCode();
			if (rc != HttpConnection.HTTP_OK)
				throw new IOException("HTTP response code: " + rc);

			is = httpconn.openInputStream();
			if((imageData = IOUtilities.streamToBytes(is)) == null)
				throw new IOException("[bitmap] imageData Error");
	
			return Bitmap.createBitmapFromBytes(imageData, 0, -1, Bitmap.SCALE_TO_FIT);

		} finally {
			if(httpconn != null){ 
				try {
					httpconn.close();
				} catch (IOException e) {} 
			}
		}
	} //GetWebBitmap
	
	
	private void updateStatus(String val)
	{
		synchronized (UiApplication.getEventLock())
		{
			_app.updateStatus("[EPG] " + val);
		}
	}
	
	private class StationParserHandler extends DefaultHandler
	{
		private Stack stack = new Stack();
		private Hashtable ht;
		
		public void startElement(String uri, String localName, String qname, Attributes attributes) throws SAXException
		{
			stack.push(qname);
			
			// 現在のエリアネームを取得
			if(qname.equals("stations"))
			{
				areaName = attributes.getValue("area_name");
			}
			
			if(qname.equals("station"))
			{
				ht = null;
				ht = new Hashtable();
			}
			
		} //startElement()
		
		public void endElement(String uri, String localName, String qName) throws SAXException
		{
			if(qName.equals("station"))
			{
				stationsInfoV.addElement(ht);
			}
			
			stack.pop();
		} //endElement()
		
		public void characters(char[] ch, int start, int length) throws SAXException 
		{
			if(!stack.peek().equals("logo"))
			{
				String element = new String(ch, start, length);
				//updateStatus("[STP] " + element);
				ht.put(stack.peek(), element);
			}
		} //characters()
	} //StationParserHandler
	
	
	private class ProgramParserHandler extends DefaultHandler
	{
		private  Hashtable ht = new Hashtable();
		private Stack stack = new Stack();
		private int num = 0;
		private boolean isFirstProgtag;
		

		public ProgramParserHandler()
		{
			// DO NOTHING
		}
		
		public void startElement(String uri, String localName, String qname, Attributes attributes) throws SAXException
		{
			stack.push(qname);
		
			if(qname.equals("station"))
			{
				//updateStatus("startElement() " + qname);
				isFirstProgtag = true;
			}
		
			if(qname.equals("prog"))
			{
				ht = null;
				ht = new Hashtable();
				
				StringBuffer ftl = new StringBuffer(attributes.getValue("ftl"));
				ftl.insert(2, ":");
				StringBuffer tol = new StringBuffer(attributes.getValue("tol"));
				tol.insert(2, ":");
	
				ht.put("time", ftl.toString() + " - " + tol.toString());
			}
		} //startElement()
	
		
		public void endElement(String uri, String localName, String qName) throws SAXException
		{
			if(qName.equals("prog"))
			{
				//updateStatus("endElement() " + qName);
			
				Hashtable station = (Hashtable)stationsInfoV.elementAt(num);
				if(isFirstProgtag)
					station.put("prog_now", ht);
				else
					station.put("prog_next", ht);
			
				isFirstProgtag = false;
			}
		
			if(qName.equals("station"))
			{
				//updateStatus("endElement() " + qName);
			
				num++;
			}
		
			stack.pop();
		} //endElement()
	
		public void characters(char[] ch, int start, int length) throws SAXException 
		{
			if(stack.peek().equals("title"))
			{
				String element = new String(ch, start, length);
				//updateStatus("characters() " + element);
				ht.put("title", element);
			}
		
			if(stack.peek().equals("pfm"))
			{
				String element = new String(ch, start, length);
				//updateStatus("characters() " + element);
				ht.put("pfm", element);
			}
			
			if(stack.peek().equals("desc"))
			{
				String element = new String(ch, start, length);
				//updateStatus("characters() " + element);
				ht.put("desc", element);
			}
			
			if(stack.peek().equals("info"))
			{
				String element = new String(ch, start, length);
				//updateStatus("characters() " + element);
				ht.put("info", element);
			}
			
			if(stack.peek().equals("url"))
			{
				String element = new String(ch, start, length);
				//updateStatus("characters() " + element);
				ht.put("url", element);
			}
			
		} //characters()
	 } //ProgramParserHandler
} //EPG