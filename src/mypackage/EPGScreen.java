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
import net.rim.device.api.ui.component.Dialog;
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
				
		
		//setTitle("今日の番組表 (" + stationInfo.get("name") + ")");
		StandardTitleBar _titleBar = new StandardTitleBar() ;
    	_titleBar.addSignalIndicator();
    	_titleBar.addNotifications();
    	_titleBar.addClock();
    	_titleBar.setPropertyValue(StandardTitleBar.PROPERTY_BATTERY_VISIBILITY, StandardTitleBar.BATTERY_VISIBLE_ALWAYS);
    	setTitleBar(_titleBar);
		
		// TABLE
		_tableModel = new TableModel();    	
        _tableView = new TableView(_tableModel);

    	//setContextMenuProvider(new DefaultContextMenuProvider());
        //ItemProvider itemProvider = new ItemProvider();
        //_tableView.setCommandItemProvider(itemProvider);
        
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
                 // Display selected device in a pop up dialog                
                 //Object[] objArray = (Object[])_tableModel.getRow(_tableView.getRowNumberWithFocus()); 
                 
                 //MyApp _app = (MyApp) UiApplication.getUiApplication();
                 //_app._epg.SetCurrentStation((String)((Hashtable)stationsInfoV.elementAt(_tableView.getRowNumberWithFocus())).get("id"));
                 
            	 
            	 
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
                 
                 
                 
                 //Dialog dialog = new Dialog(Dialog.D_OK, (String)objArray[0] + _tableView.getRowNumberWithFocus(), 0, null, 0); 
                 //Dialog dialog = new Dialog(Dialog.D_OK, "CurrentStation:" + _app._epg.GetCurrentStation(), 0, null, 0);
            	 //Dialog dialog = new Dialog(Dialog.D_OK, "Push " + _tableView.getRowNumberWithFocus(), 0, null, 0); 
                 //dialog.doModal();
                 
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
            	/*
            	// PROGRAM PFM
            	LabelField pfm = new LabelField(data[4], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
            	{
            		protected void paint(Graphics g)
            		{
            		      g.setColor(Color.GRAY); //here you can add any colors. either color codes (like - 0x0511a0a) or like this- Color.RED
            		      super.paint(g);
            		}            		
            	};
            	pfm.setFont(font);
            	fields[4] = pfm;
            	*/
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
        /*
        // Program PFM
        RegionStyles prog_pfm = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(0, 0, 0, 0), Border.STYLE_SOLID), 
        		Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt),
                null,
                new XYEdges(0, 10, 5, 10),
                RegionStyles.ALIGN_LEFT, 
                RegionStyles.ALIGN_MIDDLE);
        dataTemplate.createRegion(new XYRect(1, 3, 1, 1), prog_pfm);
        dataTemplate.setRowProperties(3, new TemplateRowProperties(Font.getDefault().getHeight() + prog_pfm.getPadding().bottom));
        */
        // Set Left column width
        dataTemplate.setColumnProperties(0, new TemplateColumnProperties(Display.getWidth() - _tableView.getMarginLeft() - _tableView.getMarginRight()));
        
        // Set Right column width
        //dataTemplate.setColumnProperties(1, new TemplateColumnProperties(Display.getWidth() - dataTemplate.getColumnProperties(0).getWidth()));
        
        //Apply the template to the view
        _tableView.setDataTemplate(dataTemplate);
        dataTemplate.useFixedHeight(true);    
        
        add(_tableView);
        
        /*
        for(int i=0; i<1; i++)
        {
        	//Hashtable station = (Hashtable) e.nextElement();
        	//updateStatus("Vec2: " + station.get("id").toString());
        	
            // StationName
            String pgname = "name";
            
            // ProgramInfo
            String pgtime = "time";
            String pgPfm  = "pfm";
            
        	// Add data to the TableModel            
            _tableModel.addRow(new Object[]{pgname, pgtime, pgPfm});
        }
        */
        
	} //EPGScreen()
	
	protected void onUiEngineAttached(boolean attached)
    {
		getProgramInfo();
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
    	//this.delete(_labelField);
    	this.setStatus(null);
    }
	
	private void getAndParseXML() throws Exception
	{
		SAXParserImpl saxparser = new SAXParserImpl();
		HttpConnection httpconn = null;
		String url = "http://radiko.jp/v2/api/program/today?area_id=" + _app._epg.getAreaID();

		try {
			updateStatus("Connecting..(EPG Screen SAX)");	    	
	
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
			
			ProgramParserHandler handler = new ProgramParserHandler();
			saxparser.parse(httpconn.openDataInputStream(), handler);
			//saxparser.parse(url, handler, false);
			 
		} finally {
			if(httpconn != null){ httpconn.close(); }
		}
	}
	
	public void getProgramInfo()
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
				//updateStatus("startElement() " + qname);				
				//updateStatus("ftl: " + attributes.getValue("ftl"));
				//updateStatus("tol: " + attributes.getValue("tol"));
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