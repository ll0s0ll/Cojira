/*
	EPGScreen.java
	
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

import java.io.IOException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import javax.microedition.io.HttpConnection;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.rim.device.api.command.Command;
import net.rim.device.api.command.CommandHandler;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.TransitionContext;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.UiEngineInstance;
import net.rim.device.api.ui.XYEdges;
import net.rim.device.api.ui.XYRect;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.StandardTitleBar;
import net.rim.device.api.ui.component.progressindicator.ActivityIndicatorController;
import net.rim.device.api.ui.component.progressindicator.ActivityIndicatorModel;
import net.rim.device.api.ui.component.progressindicator.ActivityIndicatorView;
import net.rim.device.api.ui.component.table.DataTemplate;
import net.rim.device.api.ui.component.table.RegionStyles;
import net.rim.device.api.ui.component.table.TableController;
import net.rim.device.api.ui.component.table.TableModel;
import net.rim.device.api.ui.component.table.TableView;
import net.rim.device.api.ui.component.table.TemplateColumnProperties;
import net.rim.device.api.ui.component.table.TemplateRowProperties;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.decor.BackgroundFactory;
import net.rim.device.api.ui.decor.Border;
import net.rim.device.api.ui.decor.BorderFactory;
import net.rim.device.api.xml.jaxp.SAXParserImpl;

public class EPGScreen extends MainScreen
{
	private MyApp _app;
	private Hashtable stationInfo;
	private Vector progs = new Vector();
	
	private TableModel _tableModel;
	private TableView _tableView;
	private TableController _controller; 
	
	
	public EPGScreen(int rowNumberWithFocus)
	{
		super(Manager.NO_VERTICAL_SCROLL);
		
		_app = (MyApp) UiApplication.getUiApplication();
		stationInfo = (Hashtable) _app._epg.GetStationsInfoV().elementAt(rowNumberWithFocus);
		
		StandardTitleBar _titleBar = new StandardTitleBar() ;
    	_titleBar.addSignalIndicator();
    	_titleBar.addNotifications();
    	_titleBar.addClock();
    	_titleBar.setPropertyValue(StandardTitleBar.PROPERTY_BATTERY_VISIBILITY, StandardTitleBar.BATTERY_VISIBLE_ALWAYS);
    	setTitleBar(_titleBar);
		
		// TABLE
		_tableModel = new TableModel();    	
        _tableView = new TableView(_tableModel);
        
        // See.. http://www.w3schools.com/html/html_colornames.asp
        //_tableView.setDataTemplateFocus(BackgroundFactory.createLinearGradientBackground(0xEFF7FA, 0xEFF7FA, Color.LIGHTBLUE, Color.LIGHTBLUE));
        //_tableView.setDataTemplateFocus(BackgroundFactory.createSolidTransparentBackground(Color.LIGHTBLUE, 125));
        _tableView.setDataTemplateFocus(BackgroundFactory.createSolidBackground(Color.LIGHTBLUE));
        
        _controller = new TableController(_tableModel, _tableView, TableController.ROW_FOCUS);
        
        _tableView.setController(_controller);
        
        
        _controller.setCommand(new Command(new CommandHandler() 
        {
            
             public void execute(ReadOnlyCommandMetadata metadata, Object context) 
             { 
                 //---- For ProgInfoScreen ----//
             	 //ProgInfoScreen _progInfoScreen = new ProgInfoScreen(ProgInfoScreen.NEXT, _tableView.getRowNumberWithFocus());
             	 ProgInfoScreen _progInfoScreen = new ProgInfoScreen((Hashtable)progs.elementAt(_tableView.getRowNumberWithFocus()));
             	
                 // FADE IN
                 TransitionContext transIN = new TransitionContext(TransitionContext.TRANSITION_FADE);
                 transIN.setIntAttribute(TransitionContext.ATTR_DURATION, 100);
                 
                 UiEngineInstance engine = Ui.getUiEngineInstance();
                 engine.setTransition(null, _progInfoScreen, UiEngineInstance.TRIGGER_PUSH, transIN);
                 
                 // FADE OUT
                 TransitionContext transOUT = new TransitionContext(TransitionContext.TRANSITION_FADE);
                 transOUT.setIntAttribute(TransitionContext.ATTR_DURATION, 100);
                 transOUT.setIntAttribute(TransitionContext.ATTR_KIND, TransitionContext.KIND_OUT);
                 
                 engine.setTransition(_progInfoScreen, null, UiEngineInstance.TRIGGER_POP, transOUT);
                 
                 _app.pushScreen(_progInfoScreen);
             }
         }));
        
        
        DataTemplate dataTemplate = new DataTemplate(_tableView, 4, 1)
        {
            public Field[] getDataFields(int modelRowIndex)
            {
                Object[] data = (Object[]) _tableModel.getRow(modelRowIndex);
                Field[] fields = new Field[data.length];                
                
                // NAME
                LabelField _name = new LabelField(data[0], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
                {
            		protected void paint(Graphics g)
            		{
            		      g.setColor(Color.GRAY); //here you can add any colors. either color codes (like - 0x0511a0a) or like this- Color.RED
            		      super.paint(g);
            		}
            	};
                fields[0] = _name;
                
                // TIME
                LabelField _time = new LabelField(data[1], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
                {
            		protected void paint(Graphics g)
            		{
            		      g.setColor(Color.BLACK); //here you can add any colors. either color codes (like - 0x0511a0a) or like this- Color.RED
            		      super.paint(g);
            		}
            	};
            	//_time.setFont(sfont);
                fields[1] = _time;
                               
                // PFM
                LabelField _pfm = new LabelField(data[2], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
            	{
            		protected void paint(Graphics g)
            		{
            		      g.setColor(Color.BLACK); //here you can add any colors. either color codes (like - 0x0511a0a) or like this- Color.RED
            		      super.paint(g);
            		}
            	};
            	//_pfm.setFont(sfont);
                fields[2] = _pfm;
                
                
            	// PROGRAM NAME
                //ActiveRichTextField descinfo = new ActiveRichTextField((String) data[3], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
                LabelField descinfo = new LabelField((String) data[3], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
            	{
            		protected void paint(Graphics g)
            		{
            		      g.setColor(Color.GRAY);
            		      super.paint(g);
            		}
            	};
            	fields[3] = descinfo;
            	
                return fields;
            }
        };  
        
        XYEdges marginXY = new XYEdges(0, 15, 0, 15);
        
        // TIME
        RegionStyles name = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(0, 0, 1, 0), new XYEdges(Color.WHITE, Color.WHITE, Color.GRAY, Color.WHITE), Border.STYLE_SOLID), 
        		Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt),
        		marginXY, 
                new XYEdges(15, 15, 0, 15), 
                RegionStyles.ALIGN_LEFT, 
                RegionStyles.ALIGN_MIDDLE);
        dataTemplate.createRegion(new XYRect(0, 0, 1, 1), name);      
        dataTemplate.setRowProperties(0, new TemplateRowProperties(Font.getDefault().getHeight() + name.getPadding().top));
        
        // NAME 
        RegionStyles time = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(0, 0, 0, 0), new XYEdges(Color.GRAY, Color.WHITE, Color.WHITE, Color.WHITE), Border.STYLE_SOLID),
        		Font.getDefault(),
        		marginXY,
                new XYEdges(0, 15, 0, 15), 
                RegionStyles.ALIGN_LEFT, 
                RegionStyles.ALIGN_MIDDLE);
        dataTemplate.createRegion(new XYRect(0, 1, 1, 1), time);
        dataTemplate.setRowProperties(1, new TemplateRowProperties(Font.getDefault().getHeight()));
        
        // PFM
        RegionStyles pfm = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(0, 0, 0, 0), Border.STYLE_SOLID),
        		Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt),
        		marginXY,
                new XYEdges(0, 15, 0, 15), 
                RegionStyles.ALIGN_LEFT, 
                RegionStyles.ALIGN_MIDDLE);
        dataTemplate.createRegion(new XYRect(0, 2, 1, 1), pfm);
        dataTemplate.setRowProperties(2, new TemplateRowProperties(Font.getDefault().getHeight() + pfm.getPadding().bottom));  
        
        // Program Name
        RegionStyles prog_name = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(0, 0, 0, 0), Border.STYLE_SOLID),
        		Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt),
        		marginXY,
                new XYEdges(0, 15, 15, 15), 
                RegionStyles.ALIGN_LEFT, 
                RegionStyles.ALIGN_MIDDLE);
        dataTemplate.createRegion(new XYRect(0, 3, 1, 1), prog_name);
        dataTemplate.setRowProperties(3, new TemplateRowProperties(Font.getDefault().getHeight() + prog_name.getPadding().bottom));
        
        // Set Left column width
        dataTemplate.setColumnProperties(0, new TemplateColumnProperties(Display.getWidth() - _tableView.getMarginLeft() - _tableView.getMarginRight()));
        
        // Set Right column width
        //dataTemplate.setColumnProperties(1, new TemplateColumnProperties(Display.getWidth() - dataTemplate.getColumnProperties(0).getWidth()));
        
        //Apply the template to the view
        _tableView.setDataTemplate(dataTemplate);
        dataTemplate.useFixedHeight(true);    
        
        add(_tableView);
	} //EPGScreen()
	
	protected void onUiEngineAttached(boolean attached)
    {
		getProgramInfo();
    }
	
	private void getAndParseXML() throws Exception
	{
		SAXParserImpl saxparser = new SAXParserImpl();
		HttpConnection httpconn = null;
		String url = "http://radiko.jp/v2/api/program/today?area_id=" + _app._epg.getAreaID();

		try {
			updateStatus("Connecting..(EPG Screen SAX)");	    	
	
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
			
			ProgramParserHandler handler = new ProgramParserHandler();
			saxparser.parse(httpconn.openDataInputStream(), handler);
			//saxparser.parse(url, handler, false);
			 
		} finally {
			if(httpconn != null){ httpconn.close(); }
		}
	}
	
	private void getProgramInfo()
    {
    	// ActivityIndicator ST
		ActivityIndicatorView _aiView = new ActivityIndicatorView(Field.USE_ALL_WIDTH);
        ActivityIndicatorModel aiModel = new ActivityIndicatorModel();
        ActivityIndicatorController aiController = new ActivityIndicatorController();
        
        _aiView.setController(aiController);
        _aiView.setModel(aiModel);
        _aiView.setMargin(5, 0, 5, 0);
        
        aiController.setModel(aiModel);
        aiController.setView(_aiView);
        aiModel.setController(aiController);
        // Define the indicator image and create a field from it
        Bitmap aiBitmap = Bitmap.getBitmapResource("spinner2.png");
        
        _aiView.createActivityImageField(aiBitmap, 6, Field.FIELD_HCENTER);
        
        this.setStatus(_aiView);
        
    	PGInfoThread _th = new PGInfoThread();
        _th.start();
    }
	
	private void updateStatus(final String message)
    {
    	synchronized (UiApplication.getEventLock()) 
		{			
			_app.updateStatus("[EPGS] " + message);
		}
    }
	
	private void removeField()
    {
    	this.setStatus(null);
    }
    
    private class PGInfoThread extends Thread
    { 
    	PGInfoThread()
    	{
    		// DO NOTHING
    	}
    	
    	
    	public void run()
		{
			try {
				updateStatus("PGInfoThread Start");
				
				getAndParseXML();
				
	            synchronized (UiApplication.getEventLock()) 
				{		
	            	removeField();
				}
	            
			} catch (Exception e) {
				//updateStatus("PGInfoThread " + e.toString());
				LabelField _tmpField = new LabelField("番組情報の更新に失敗しました。", Field.FIELD_HCENTER);
				_tmpField.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt));
				_tmpField.setPadding(10, 0, 0, 0);
				synchronized (UiApplication.getEventLock()) 
				{		
					setStatus(_tmpField);
				}   
			} //try()
		} //run()
    } //PGInfoThread

    
    private class ProgramParserHandler extends DefaultHandler 
	{		
		private Hashtable ht = new Hashtable();
		private Stack stack = new Stack();
		private boolean isSelectedStation;
		
		/*
		public ProgramParserHandler()
		{
			// DO NOTHING
		} 
	 	*/
		
		public void startElement(String uri, String localName, String qname, Attributes attributes) throws SAXException 
		{
			stack.push(qname);
		 
			
			if(qname.equals("station"))
			{
				if(attributes.getValue("id").equals(stationInfo.get("id")))
					isSelectedStation = true;
				else
					isSelectedStation = false;
				
				//updateStatus("startElement() " + qname);
					
			}
		 
			if(qname.equals("prog"))
			{
				if(isSelectedStation)
				{
					ht = null;
					ht = new Hashtable();
		                  
					StringBuffer ftl = new StringBuffer(attributes.getValue("ftl"));
					ftl.insert(2, ":");
					StringBuffer tol = new StringBuffer(attributes.getValue("tol"));
					tol.insert(2, ":");
					//int dur = Integer.parseInt(attributes.getValue("dur"));
					
					ht.put("time", ftl.toString() + " - " + tol.toString());
					//ht.put("time", ftl.toString() + " - " + tol.toString() + " (" + dur + "min.)");
				}
			}
			
		} //startElement()
	
		
		public void endElement(String uri, String localName, String qName) throws SAXException
		{			
			if(qName.equals("prog"))
			{
				//updateStatus("endElement() " + qName);
				if(isSelectedStation)
				{
					progs.addElement(ht);
					
					String desc = "";
					if(ht.get("desc") != null)
						desc = (String)ht.get("desc");
					
					String info = "";
					if(ht.get("info") != null)
						info = (String)ht.get("info");
					
					// Add data to the TableModel            
					_tableModel.addRow(new Object[]{(String)ht.get("time"), (String)ht.get("title"), (String)ht.get("pfm"), desc + info});
					//_tableModel.addRow(new Object[]{(String)ht.get("title"), (String)ht.get("time"), (String)ht.get("pfm"), ""});
				}
			}
			
			if(qName.equals("station"))
			{
				isSelectedStation = false;				
				//updateStatus("startElement() " + qname);
			}
			
			stack.pop();
		} //endElement()
	 
		public void characters(char[] ch, int start, int length) throws SAXException 
		{
			if(isSelectedStation)
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
			}
		} //characters()
	 } //ProgramParserHandler
}