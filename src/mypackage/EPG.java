package mypackage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import javax.microedition.io.HttpConnection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.xml.jaxp.SAXParserImpl;
import net.rim.device.api.xml.parsers.DocumentBuilderFactory;


public final class EPG
{
	private MyApp _app;
	
	private String areaID;
	private String areaName;
	private Vector stationsInfoV;
	private Hashtable programInfo;
	private int currentStation;
	
	
	public EPG(UiApplication app)
	{
		_app = (MyApp) app;
	}
	
	
	public void getStationList() throws Exception
	{
		HttpConnection httpconn = null;
		
		try {
			updateStatus("Connecting..(EPG)");	    	

			if((areaID = _app._auth.getAreaID()) == null)
				throw new Exception("AreaID Error");
						
			String url = "http://radiko.jp/v2/station/list/" + areaID + ".xml";
			
			stationsInfoV = new Vector();
			StationParserHandler _handle = new StationParserHandler();
			getAndParseXML(url, _handle);
			
			//放送局のロゴ画像を取得
			getStationsLogo();
			
			/*
	    	ConnectionDescriptor conDescriptor = _connfactory.getConnection( url );
	
	    	if (conDescriptor == null)
	    		throw new Exception("conDescriptor ERROR");    		
	
	    	// using the connection
	    	httpconn = (HttpConnection) conDescriptor.getConnection();   	
	    	
	        // Set the request method and headers
	    	httpconn.setRequestMethod(HttpConnection.GET);
	        
	        // Getting the response code will open the connection,
	        // send the request, and read the HTTP response headers.
	        // The headers are stored until requested.
	        int rc = httpconn.getResponseCode();
	        if (rc != HttpConnection.HTTP_OK)        
	            throw new IOException("HTTP response code: " + rc);                
	        
	        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(httpconn.openInputStream());            
	        
	        
	        Element rootElement = doc.getDocumentElement();
            rootElement.normalize();
            
            *//*
            NodeList stations = rootElement.getElementsByTagName("station");             
            for(int i=0; i<stations.getLength(); i++ )
	        {
	        	Node station = stations.item(i);
	        	Hashtable ht = null;
    			NamedNodeMap namdNodeMap = station.getAttributes();    			
    			for(int k=0; k<namdNodeMap.getLength(); k++)
    			{	        				
    				updateStatus(" Att: " + namdNodeMap.item(k).getNodeName() + " val: " + namdNodeMap.item(k).getNodeValue());
    				if(namdNodeMap.item(k).getNodeValue().equals("INT"))
    				{
    					// Display the root node and all its descendant nodes, which covers the entire
    		            // document.
    		            displayNode( station, 0 );
    				}
    			}
	        }
            *//*
            
            // 取得したエリアの放送局を取得
            NodeList stations = rootElement.getElementsByTagName("station");
            //stationsInfoV = new Vector();
            for(int i=0; i<stations.getLength(); i++ )
	        {
            	Node station = stations.item(i);
            	//stationsInfo[i] = new Hashtable();
	            //StoreNode( station, stationsInfo[i] );
            	Hashtable htt = new Hashtable();
            	StoreNode(station, htt);
				stationsInfoV.addElement(htt);
	        }
            */
            
            
            
            
            /*
	        NodeList nodeList = rootElement.getElementsByTagName("stations");
	        if(nodeList == null)
	        	throw new Exception("nodeList is null");
	        
	        //Node[] stations = new Node[nodeList.getLength()];	        
	        //Hashtable _stations = new Hashtable();
	        
	        for(int i=0; i<nodeList.getLength(); i++ )
	        {
	        	Node station = nodeList.item(i);
	        		        	
	        	//updateStatus("Tag: " + station.getNodeName());
	        	
	        	NodeList tmpNL = station.getChildNodes();
	        	for(int j=0; j<tmpNL.getLength(); j++)
	        	{
	        		Node tmpN = tmpNL.item(j);	        		
	        		updateStatus(" Tag: " + tmpN.getNodeName());
	        		
	        		if(tmpN.getNodeName().equals("station"))
	        		{
	        			NamedNodeMap namdNodeMap = tmpN.getAttributes();
	        			
	        			for(int k=0; k<namdNodeMap.getLength(); k++)
	        			{	        				
	        				updateStatus(" Att: " + namdNodeMap.item(k).getNodeName() + " val: " + namdNodeMap.item(k).getNodeValue());
	        			}
	        			
	        		}
	        	}
	        	
	        	
	        	*//*
	        	Hashtable _station = new Hashtable();
	        	
	        	Node station = nodeList.item(i);
	        	NodeList tmpNL = station.getChildNodes();
	        	for(int j=0; j<tmpNL.getLength(); j++)
	        	{
	        		Node tmpN = tmpNL.item(j);
	        		_station.put(tmpN.getNodeName(), tmpN.getNodeValue());
	        		
	        		updateStatus("Tag: " + tmpN.getNodeName() + " Value: " + tmpN.getNodeValue());
	        	}
	        	
	        	
	        	//stationN.getChildNodes()
	        	//updateStatus("Station: " + station.getTextContent());
	        	 
	        	 *//*
	        }
	        */	        	        
	
		} finally {
			if(httpconn != null){ httpconn.close(); }
	    }
	}
	
	/*
	 private void StoreNode( Node node, Hashtable ht ) 
	 { 
		 
		 
		 if( node.getNodeType() == Node.ELEMENT_NODE ) 
		 {
			 NodeList childNodes = node.getChildNodes();
			 int numChildren = childNodes.getLength();
			 Node firstChild = childNodes.item( 0 );
			 
			 if ( numChildren == 1 && firstChild.getNodeType() == Node.TEXT_NODE )
			 {				
				 //htt.put(node.getNodeName(), firstChild.getNodeValue());				 
				 ht.put(node.getNodeName(), firstChild.getNodeValue());
				 //updateStatus("NAME:" + node.getNodeName() + " Value:" +  firstChild.getNodeValue() );
			 } 
			 else 
			 {
				 // The node either has > 1 children, or it has at least one Element node child. 
				 // Either way, its children have to be visited.  Print the name of the element
				 // and recurse.
				 //updateStatus("NAME2:" + node.getNodeName());							
				 
				 // Recursively visit all this node's children.
				 for ( int i = 0; i < numChildren; ++i ) 
				 {
					 StoreNode( childNodes.item( i ), ht );
				 }
			 }
		 }
	 }
	
	*/
	 /*
	 public void GetProgramInfo() throws Exception
	 {
		 HttpConnection httpconn = null;
		 
		 try {
			 updateStatus("Connecting..(EPG INFO)");	    	
			
			 MyApp _app = (MyApp) UiApplication.getUiApplication();
			 if((areaID = _app._auth.getAreaID()) == null)
				 throw new Exception("AreaID Error");
							
			 //String url = "http://radiko.jp/v2/station/list/" + areaID + ".xml";
			 String url = "http://radiko.jp//v2/api/program/now?area_id=" + areaID;
			 ConnectionDescriptor conDescriptor = _connfactory.getConnection( url );
			
			 if (conDescriptor == null)
				 throw new Exception("conDescriptor ERROR");    		
			
			 // using the connection
			 httpconn = (HttpConnection) conDescriptor.getConnection();   	
				
			 // Set the request method and headers
			 httpconn.setRequestMethod(HttpConnection.GET);
			    
			 // Getting the response code will open the connection,
			 // send the request, and read the HTTP response headers.
			 // The headers are stored until requested.
			 int rc = httpconn.getResponseCode();
			 if (rc != HttpConnection.HTTP_OK)        
				 throw new IOException("HTTP response code: " + rc);                
			    
			 Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(httpconn.openInputStream());            
			        
			 Element rootElement = doc.getDocumentElement();
			 rootElement.normalize();
			 
			 // 現在放送中の番組の情報を取得
			 programInfo = new Hashtable();
			 NodeList stations = rootElement.getElementsByTagName("station");             
			 for(int i=0; i<stations.getLength(); i++ )
			 {
				 Node station = stations.item(i);
				 //updateStatus("[ITEM] " + station.getNodeName() + " / " + station.getNodeValue());
				 
				 Hashtable htt = new Hashtable();
				 StoreNode(station, htt);
				 //updateStatus("[PI] " + htt.toString());
				 //displayNode(station, 0);				 
				 
				 NamedNodeMap namdNodeMap = station.getAttributes();  
				 if(namdNodeMap != null)
				 {
					 for(int k=0; k<namdNodeMap.getLength(); k++)
					 {	        				
						 //updateStatus(" Att: " + namdNodeMap.item(k).getNodeName() + " val: " + namdNodeMap.item(k).getNodeValue());
						 
						 if(namdNodeMap.item(k).getNodeName().equals("id"))
						 {
							 programInfo.put(namdNodeMap.item(k).getNodeValue(), htt);
						 }		
					 }
				 }
				 
				 //_app._secondaryScreen.setProgressBarVal((i * 100)/ stations.getLength());
			 }
			    
			 //Vector stationsInfoV = _app._epg.GetStationsInfoV();
			 for(Enumeration e = stationsInfoV.elements(); e.hasMoreElements(); )
			 {
				 Hashtable station = (Hashtable) e.nextElement();				 
				 Hashtable tbs = (Hashtable) programInfo.get(station.get("id"));	
					
				 //updateStatus("[ENU [" + tbs.toString());
				 station.put("PGInfo_title", tbs.get("title") == null ? " " : tbs.get("title"));
				 station.put("PGInfo_pfm", tbs.get("pfm")== null ? " " : tbs.get("pfm"));
				 
				 
				 //updateStatus("[ENU [" + station.get("id") + "] " + tbs.get("name"));
				 //updateStatus("[ENU [" + station.get("id") + "] " + tbs.get("title"));
				 //updateStatus("[ENU [" + station.get("id") + "] " + tbs.get("pfm"));
				 
			 }	
			 
			 *//*
			 for(Enumeration e = programInfo.keys(); e.hasMoreElements();)
			 {
				  updateStatus("[ENU] " + e.nextElement().toString());
			 }
			 *//*
			
			 	        	        
			
			} finally {
				if(httpconn != null){ httpconn.close(); }
			}
	 } //GetProgramInfo
	 */
	 
	 public void getProgramInfo() throws Exception
	 {
		 if((areaID = _app._auth.getAreaID()) == null)
			 throw new Exception("AreaID Error");
		 
		 String url = "http://radiko.jp//v2/api/program/now?area_id=" + areaID;
		 
		 ProgramParserHandler _handler = new ProgramParserHandler();
		 
		 getAndParseXML(url, _handler);
	 }
	 
	 
	 private void getAndParseXML(String url, DefaultHandler handler) throws Exception
	 {
		 SAXParserImpl saxparser = new SAXParserImpl();
		 //ListParser receivedListHandler = new ListParser();
		 HttpConnection httpconn = null;
		 
		 try {
			 updateStatus("Connecting..(EPG INFO SAX)");	    	
	
			 ConnectionDescriptor conDescriptor = _app.GetConnectionFactory().getConnection( url );
			
			 if (conDescriptor == null)
				 throw new Exception("conDescriptor ERROR");    		
			
			 // using the connection
			 httpconn = (HttpConnection) conDescriptor.getConnection();   	
				
			 // Set the request method and headers
			 httpconn.setRequestMethod(HttpConnection.GET);
			    
			 // Getting the response code will open the connection,
			 // send the request, and read the HTTP response headers.
			 // The headers are stored until requested.
			 int rc = httpconn.getResponseCode();
			 if (rc != HttpConnection.HTTP_OK)        
				 throw new IOException("SAX HTTP response code: " + rc);
			 
			 saxparser.parse(httpconn.openDataInputStream(), handler);
			 //saxparser.parse(url, handler, false);
			 
		 } finally {
			 if(httpconn != null){ httpconn.close(); }
		 }
	 }
	 
	 
	 
	 
	
	    
	
	public Vector GetStationsInfoV()
	{		
		return stationsInfoV;
	}
	
	public void SetCurrentStation(int val)
	{
		currentStation = val;
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
	
	public String getAreaID()
	{
		return areaID;
	}
	
	public String getAreaName()
	{
		return areaName;
	}
	
	private void getStationsLogo() throws Exception
	{
		if(stationsInfoV == null)
			throw new Exception("stationsInfoV is NULL");
		
		for(Enumeration e = stationsInfoV.elements(); e.hasMoreElements(); )
        {
        	Hashtable station = (Hashtable) e.nextElement();
        	//updateStatus("Vec3: " + station.get("id").toString());
        	
        	// LOGO
        	Bitmap bitmap = GetWebBitmap((String) station.get("logo_medium"));
        	//Bitmap bitmap = GetWebBitmap((String) station.get("logo_large"));
        	
        	station.put("station_logo",	bitmap);
        }		
	}
	
	private Bitmap GetWebBitmap(String url) throws Exception
    {
    	InputStream is;
    	byte[] imageData;
    	
    	ConnectionFactory _factory = _app.GetConnectionFactory();
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
		
		/*
		public ProgramParserHandler()
		{
			// DO NOTHING
		}
		
		public void startDocument() throws SAXException 
		{
			//DO NOTHING
		}
	 
		public void endDocument() throws SAXException 
		{ 
			//DO NOTHING
		} 
	 	*/
		
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
				//updateStatus("startElement() " + qname);				
				//updateStatus("ftl: " + attributes.getValue("ftl"));
				//updateStatus("tol: " + attributes.getValue("tol"));
			 
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