/*
	StationsScreen.java
	
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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.HttpConnection;


import mypackage.MyScreen.DialogCommandHandler;
import net.rim.device.api.command.Command;
import net.rim.device.api.command.CommandHandler;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.media.MediaActionHandler;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Display;
import net.rim.device.api.system.EventInjector;
import net.rim.device.api.system.EventInjector.KeyCodeEvent;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.NullField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.StandardTitleBar;
import net.rim.device.api.ui.component.progressindicator.ActivityIndicatorController;
import net.rim.device.api.ui.component.progressindicator.ActivityIndicatorModel;
import net.rim.device.api.ui.component.progressindicator.ActivityIndicatorView;
import net.rim.device.api.ui.component.progressindicator.ProgressIndicatorController;
import net.rim.device.api.ui.component.progressindicator.ProgressIndicatorModel;
import net.rim.device.api.ui.component.progressindicator.ProgressIndicatorView;
import net.rim.device.api.ui.component.table.AbstractTableModel;
import net.rim.device.api.ui.component.table.DataModel;
import net.rim.device.api.ui.component.table.DataTemplate;
import net.rim.device.api.ui.component.table.RegionStyles;
import net.rim.device.api.ui.component.table.TableController;
import net.rim.device.api.ui.component.table.TableModel;
import net.rim.device.api.ui.component.table.TableModelAdapter;
import net.rim.device.api.ui.component.table.TableModelChangeEvent;
import net.rim.device.api.ui.component.table.TableView;
import net.rim.device.api.ui.component.table.TemplateColumnProperties;
import net.rim.device.api.ui.component.table.TemplateRowProperties;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.ui.image.Image;
import net.rim.device.api.ui.image.ImageFactory;
import net.rim.device.api.ui.menu.CommandItem;
import net.rim.device.api.ui.menu.CommandItemProvider;
import net.rim.device.api.ui.menu.DefaultContextMenuProvider;
import net.rim.device.api.ui.menu.SubMenu;
import net.rim.device.api.util.StringProvider;


/**
 * A screen to display a Bitmap in a BitmapField, used to demonstrate screen transitions
 */
public class StationsScreen extends MainScreen
{   
	private MyApp _app;
	private LabelField _test;
	private LabelField _test2;
	private BasicEditField _statusField;
	private LabelField _labelField;
	private TableModel _tableModel;
    private TableView _tableView;
    private TableController _controller;
    private  Vector stationsInfoV;
    
    private ProgressIndicatorModel _model;
    private ActivityIndicatorView _aiView;
    
    private DataTemplate dataTemplate;
    private StandardTitleBar _titleBar;
    
    /**
     * Creates a new StationsScreen object
     */
    public StationsScreen(String title, int color)
    {     
    	super(Manager.NO_VERTICAL_SCROLL);
    	
    	 _app = (MyApp) UiApplication.getUiApplication(); 
    	
        //setTitle("放送局を選択");
        
    	_titleBar = new StandardTitleBar() ;
    	_titleBar.addSignalIndicator();
    	_titleBar.addNotifications();
    	_titleBar.addClock();
    	_titleBar.setPropertyValue(StandardTitleBar.PROPERTY_BATTERY_VISIBILITY, StandardTitleBar.BATTERY_VISIBLE_ALWAYS);
    	setTitleBar(_titleBar);
    	
        //VerticalFieldManager _Vmanager = (VerticalFieldManager)getMainManager();
    	//VerticalFieldManager _Vmanager = new VerticalFieldManager(Field.FIELD_HCENTER | Field.USE_ALL_WIDTH);
    	
    	final LabelField _separator = new LabelField("|", Field.FIELD_HCENTER)
    	{
    		protected void paint(Graphics g)
    		{
    		      g.setColor(Color.BLACK);
    		      super.paint(g);
    		}
    	};
    	_separator.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt));
    	//_separator.setPadding(0, 10, 0, 10);
    	//_separator.setPosition(Display.getWidth() / 2);
    	

        _test = new LabelField("TOKYO Japan " + Display.getWidth() + " " + Display.getHeight(), LabelField.ELLIPSIS | Field.FIELD_RIGHT)
    	{        	
    		protected void paint(Graphics g)
    		{
    		      g.setColor(Color.WHITE);
    		      super.paint(g);
    		}
    		
    		protected void onExposed()
    		{
    			MyApp _app = (MyApp) UiApplication.getUiApplication(); 
    			if(_app._epg.getAreaName() != null)
    				this.setText(_app._epg.getAreaName());
    			else    				
    				this.setText("error!!");
    			super.onObscured();
    		}
    	};
    	_test.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt));
    	//_test.setPadding(20, 10, 20, 10);
    	//_test.setBackground(BackgroundFactory.createLinearGradientBackground(Color.WHITE, Color.WHITE, Color.LIGHTGRAY, Color.LIGHTGRAY));
    	//_test.setBorder(BorderFactory.createSimpleBorder(new XYEdges(0, 0, 1, 0), new XYEdges(Color.GRAY, Color.GRAY, Color.GRAY, Color.GRAY), Border.STYLE_SOLID));

        //add(_test);
    	
        
    	_test2 = new LabelField("ラジオNIKKEI第1" , LabelField.ELLIPSIS | Field.FIELD_LEFT)
    	{    		
    		protected void paint(Graphics g)
    		{
    		      g.setColor(Color.WHITE);
    		      super.paint(g);
    		}
    		
    		protected void onExposed()
    		{
    			MyApp _app = (MyApp) UiApplication.getUiApplication(); 
    			if(_app._epg.getCurrentStationName() != null)
    				this.setText(_app._epg.getCurrentStationName());
    			else    				
    				this.setText("error!!");
    		}
    	};
    	_test2.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt));
        //_test2.setPosition(Display.getWidth() -_test2.getPreferredWidth());
        
        //_Vmanager.add(_Hmanager);
        //_Vmanager.add(new SeparatorField());
        
        HorizontalFieldManager _Hmanager = new HorizontalFieldManager(Field.USE_ALL_WIDTH)
    	{
        	
    		protected void sublayout(int maxWidth, int maxHeight)
    		{
    			layoutChild(_test, _test.getPreferredWidth(), _test.getPreferredHeight());
    			setPositionChild(_test, Display.getWidth()/2 - _test.getPreferredWidth() - 10, 0);
    			
    			layoutChild(_separator, _separator.getPreferredWidth(), _separator.getPreferredHeight());
    			setPositionChild(_separator, Display.getWidth()/2, 0);
    			
    			layoutChild(_test2, _test2.getPreferredWidth(), _test2.getPreferredHeight());
    			setPositionChild(_test2, Display.getWidth()/2 + _separator.getPreferredWidth() + 10, 0);
    			
    			setExtent(Display.getWidth(), getPreferredHeight());
    		}
    		
    	}; 
        _Hmanager.setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
    	//_Hmanager.setBackground(BackgroundFactory.createLinearGradientBackground(Color.WHITE, Color.WHITE, Color.LIGHTGRAY, Color.LIGHTGRAY));
    	_Hmanager.setPadding(0, 0, 5, 0);
    	//_Hmanager.setBorder(BorderFactory.createSimpleBorder(new XYEdges(0, 0, 1, 0), new XYEdges(Color.GRAY, Color.GRAY, Color.GRAY, Color.GRAY), Border.STYLE_SOLID));
        
    	_Hmanager.add(_test);
    	_Hmanager.add(_separator);
    	_Hmanager.add(_test2);
        
        //add(_Hmanager);        
        
        
        //_labelField = new LabelField("The screen closes automatically in two seconds by using a fade transition", Field.NON_FOCUSABLE);
        //add(_labelField);
        
        //_statusField = new BasicEditField(Field.NON_FOCUSABLE);
        //add(_statusField);
        
    	/*
        try {
        	
        	updateStatus("koko1");
        	MyApp _app = (MyApp) UiApplication.getUiApplication();
        	
        	updateStatus("NULLPO");
        		
            stationsInfoV = _app._epg.GetStationsInfoV();
            
            if(stationsInfoV == null)
            {
            	updateStatus("NULLPOPO");
            	return;
            }
            */
    	try {
        	_tableModel = new TableModel();
    	
        	
            // Set up view and controller
            _tableView = new TableView(_tableModel);

        	setContextMenuProvider(new DefaultContextMenuProvider());
            ItemProvider itemProvider = new ItemProvider();
            _tableView.setCommandItemProvider(itemProvider);
            // See.. http://www.w3schools.com/html/html_colornames.asp
            //_tableView.setDataTemplateFocus(BackgroundFactory.createLinearGradientBackground(0xEFF7FA, 0xEFF7FA, Color.LIGHTBLUE, Color.LIGHTBLUE));
            //_tableView.setDataTemplateFocus(BackgroundFactory.createSolidTransparentBackground(Color.LIGHTBLUE, 125));
            _tableView.setDataTemplateFocus(BackgroundFactory.createSolidBackground(Color.LIGHTBLUE));
            
            _controller = new TableController(_tableModel, _tableView, TableController.ROW_FOCUS); 
            //_controller.setFocusPolicy(TableController.ROW_FOCUS);
            
            _tableView.setController(_controller);
            
            /*
            _controller.setCommand(new Command(new CommandHandler() 
            {
                
                 public void execute(ReadOnlyCommandMetadata metadata, Object context) 
                 {
                     // Display selected device in a pop up dialog                
                     //Object[] objArray = (Object[])_tableModel.getRow(_tableView.getRowNumberWithFocus()); 
                     
                     MyApp _app = (MyApp) UiApplication.getUiApplication();
                     //_app._epg.SetCurrentStation((String)((Hashtable)stationsInfoV.elementAt(_tableView.getRowNumberWithFocus())).get("id"));
                     _app._epg.SetCurrentStation(_tableView.getRowNumberWithFocus());
                     
                     // 再生、停止を実行
                     if(_app._mediaActions.isPlaying())
                     {             
                    	 _app._mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_STOP, MediaActionHandler.SOURCE_BACKGROUND_KEY, null);
                    	 _app._mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_PLAY, MediaActionHandler.SOURCE_BACKGROUND_KEY, null);
                    	 //_app._mediaActions.doStop();
                    	 //_app._mediaActions.doPlay();
                    	 
                     }
                     else
                     {
                    	 _app._mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_PLAY, MediaActionHandler.SOURCE_BACKGROUND_KEY, null);
                    	 //_app._mediaActions.doPlay();
                     }
                     
                     //Dialog dialog = new Dialog(Dialog.D_OK, (String)objArray[1], 0, null, 0); 
                     //Dialog dialog = new Dialog(Dialog.D_OK, "CurrentStation:" + _app._epg.GetCurrentStation(), 0, null, 0);
                	 //Dialog dialog = new Dialog(Dialog.D_OK, (String)((Hashtable)stationsInfoV.elementAt(_tableView.getRowNumberWithFocus())).get("id"), 0, null, 0); 
                     //dialog.doModal();
                     
                     _app.popMyScreen();
                     
                     //UiApplication.getUiApplication().invokeLater( _popRunnable, 500, false);
                 }
             }));
            */
            setStyle();
            add(_tableView);
            //_secondaryScreen.add(_tableView);
            
            
            /*
            for(Enumeration e = stationsInfoV.elements(); e.hasMoreElements(); )
            //for(int e = 0; e<5; e++)
            {
            	Hashtable station = (Hashtable) e.nextElement();
            	updateStatus("Vec2: " + station.get("id").toString());
            	
            	// LOGO
            	//Bitmap bitmap = GetWebBitmap((String) station.get("logo_medium"));
            	Bitmap bitmap = (Bitmap) station.get("station_logo");
            	//String bitmap = "bitmap";
                
                // Name
                String name = (String) station.get("name");
                //String name = "name";
                
                // etc
                String pgTitle = (String) station.get("PGInfo_title");
                String pgPfm  = (String) station.get("PGInfo_pfm");
                //String pgTitle = "PGInfo_title";
                //String pgPfm  = "PGInfo_pfm";
                
            	// Add data to the TableModel            
                _tableModel.addRow(new Object[]{bitmap, name, pgTitle, pgPfm});
            }
            
          */
    	} catch (Exception e) {
			updateStatus("[table] " + e.toString());
		}
    	
            
    } 
    
    protected void onUiEngineAttached(boolean attached)
    {
        super.onUiEngineAttached(attached);
        if(attached)
        {
        	updateStatus("Attached!!");
        	/*
        	getMainManager().deleteAll();
			//delete(_tableView);
        	
			*/
	        // add the view to the screen
        	//if(_aiView == null || _aiView.getManager() == null )
        	//{
        		//_aiView = createAIView();
        		//getMainManager().add(_aiView);
        	//}
        		
        	//getMainManager().add(new LabelField("hoge", Field.NON_FOCUSABLE | LabelField.ELLIPSIS));
	        
        	getProgramInfo();
        	
        	
        	
        }        
        else
        {
        	updateStatus("Deattached!!");
        	/*
        	if(_aiView != null || _aiView.getManager() != null )
        	{	
				getMainManager().delete(_aiView);
        	}
        	*/
        }
    }
	
	protected void onExposed()
	{
		_titleBar.addTitle(_app._epg.getCurrentStationName());
		
	}
    
    protected void makeMenu( Menu menu, int instance )
    {        
        MenuItem _prog = new MenuItem(new StringProvider("選局"), 0x00020000, 0);
        _prog.setCommand(new Command(        
	        new CommandHandler()
	        {
	        	public void execute(ReadOnlyCommandMetadata metadata, Object context)
	            {
	        		;       		
	            }        	
	        }
        ));
        
        menu.add(_prog);
        
        
        super.makeMenu(menu, instance);
    };
    
    
    
    private ActivityIndicatorView createAIView()
    {
    	// ActivityIndicator ST
    	ActivityIndicatorView _aiView = new ActivityIndicatorView(Field.USE_ALL_WIDTH);
	    ActivityIndicatorModel aiModel = new ActivityIndicatorModel();
	    ActivityIndicatorController aiController = new ActivityIndicatorController();
	    
	    _aiView.setController(aiController);
	    _aiView.setModel(aiModel);
	    aiController.setModel(aiModel);
	    aiController.setView(_aiView);
	    aiModel.setController(aiController);
	    // Define the indicator image and create a field from it
	    Bitmap aiBitmap = Bitmap.getBitmapResource("spinner2.png");
	    
	    _aiView.createActivityImageField(aiBitmap, 6, Field.FIELD_HCENTER | Field.FIELD_VCENTER);
	    
	    return _aiView;
    }
    
    public void addRow(Bitmap bitmap, String name, String pgtime, String pgTitle, String pgPfm)
    {
    	// STATION LOGO
    	Field field0 =  new BitmapField(bitmap);
    	
    	// STATION NAME
    	Field field1 = new LabelField(name, Field.NON_FOCUSABLE | LabelField.ELLIPSIS);
                       
        // PROGRAM TIME
    	Field field2 = new LabelField(pgtime, Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
    	{
    		protected void paint(Graphics g)
    		{
    		      g.setColor(Color.GRAY); //here you can add any colors. either color codes (like - 0x0511a0a) or like this- Color.RED
    		      super.paint(g);
    		}
    	};
        
    	// PROGRAM NAME
    	Field field3 = new LabelField(pgTitle, Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
    	{
    		protected void paint(Graphics g)
    		{
    		      g.setColor(Color.GRAY);            		      
    		      super.paint(g);
    		}
    	};
    	
    	
    	// PROGRAM PFM
    	Field field4 = new LabelField(pgPfm, Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
    	{
    		protected void layout(int width, int height)
    		{
    			/*
    			MyApp _app = (MyApp) UiApplication.getUiApplication(); 
    			if(_app._epg.getAreaName() != null)
    				this.setText(_app._epg.getCurrentStationName());
    			else    				
    				this.setText("error!!");
    			*/
    			super.layout(width, height);
    		}
    		
    		protected void paint(Graphics g)
    		{
    			MyApp _app = (MyApp) UiApplication.getUiApplication(); 
    			if(_app._epg.getAreaName() != null)
    				this.setText(_app._epg.getCurrentStationName());
    			else    				
    				this.setText("error!!");
    			
    		    g.setColor(Color.GRAY);
    		    super.paint(g);
    		}
    		/*
    		protected void onExposed()
    		{
    			MyApp _app = (MyApp) UiApplication.getUiApplication(); 
    			if(_app._epg.getAreaName() != null)
    				this.setText(_app._epg.getCurrentStationName());
    			else    				
    				this.setText("error!!");
    		}
    		*/
    	};
    	
    	_tableModel.addRow(new Object[]{field0, field1, field2, field3, field4});
    }
    
    public void acc()
    {
    	try {
        	MyApp _app = (MyApp) UiApplication.getUiApplication();        	
            stationsInfoV = _app._epg.GetStationsInfoV();            
            
            for(Enumeration e = stationsInfoV.elements(); e.hasMoreElements(); )
            {
            	Hashtable station = (Hashtable) e.nextElement();
            	updateStatus("Vec2: " + station.get("id").toString());
            	
            	// LOGO
            	//Bitmap bitmap = GetWebBitmap((String) station.get("logo_medium"));
            	Bitmap bitmap = (Bitmap) station.get("station_logo");
                //BitmapField bitmapField = new BitmapField(bitmap);                
            	
                // StationName
                String name = (String) station.get("name");
                
                // ProgramInfo
                String pgtime = "...";
                String pgTitle = "...";
                String pgPfm  = "...";
                
            	// Add data to the TableModel            
                _tableModel.addRow(new Object[]{bitmap, name, pgtime, pgTitle, pgPfm});
            }

            
    	} catch (Exception e) {
			updateStatus("[table] " + e.toString());
		}
    } //aac()
    
        
   /**
    * @see Screen#invokeAction(int)
    */
    /*
    protected boolean invokeAction(int action)
    {
        if(action == ACTION_INVOKE)
        {          
        	//MyApp app = (MyApp)UiApplication.getUiApplication();
            //app.startOrStopThread();
            return true;            
        }
        
        return super.invokeAction(action);
    }
    */
    
   /**
    * @see Screen#touchEvent(TouchEvent)
    */
    /*
    public boolean touchEvent(TouchEvent event)
    {
        if(event.getEvent() == TouchEvent.UNCLICK)
        {            
            invokeAction(ACTION_INVOKE);
            return true;
        }
        
        return super.touchEvent(event);
    } 
    */
    /*
    private Bitmap GetWebBitmap(String url) throws Exception
    {
    	InputStream is;
    	MyApp _app;
    	byte[] imageData;
    	
    	_app = (MyApp) UiApplication.getUiApplication();
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
			
			if(_app != null){_app = null;};
		}
    }
    */
    public void updateStatus(final String message)
    {   
    	/*
    	UiApplication.getUiApplication().invokeLater(new Runnable()
    	{
    		public void run()
    		{
    			//_statusField.setText(_statusField.getText() + "\n" + message);
    		}
        });
    	*/
    	synchronized (UiApplication.getEventLock()) 
		{
			MyApp _app = (MyApp) UiApplication.getUiApplication();
			_app.updateStatus("[SS] " + message);
		}
    }
    
    private void setStyle()
    {
    	final int NUM_ROWS = 4;
    	final int NUM_COLUMNS = 2;
    	final int IMAGE_WIDTH = 123;
    	final int IMAGE_HEIGHT = 50;
    	    
        dataTemplate = new DataTemplate(_tableView, NUM_ROWS, NUM_COLUMNS)
        {
            public Field[] getDataFields(int modelRowIndex)
            {
                Object[] data = (Object[]) _tableModel.getRow(modelRowIndex);
                Field[] fields = new Field[data.length];
                
                // LOGO
                //fields[0] = new BitmapField((Bitmap) data[0]);
                fields[0] = (Field) data[0];
                
                // STATION NAME
                //fields[1] = new LabelField(data[1], Field.NON_FOCUSABLE | LabelField.ELLIPSIS);
                fields[1] = (Field) data[1];
                               
                // PROGRAM TIME
                /*
                LabelField time = new LabelField(data[2], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
            	{
            		protected void paint(Graphics g)
            		{
            		      g.setColor(Color.GRAY); //here you can add any colors. either color codes (like - 0x0511a0a) or like this- Color.RED
            		      super.paint(g);
            		}
            	};
            	Font font = Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt);            	
                time.setFont(font);
                fields[2] = time;
                */
                fields[2] = (Field) data[2];
                
            	// PROGRAM NAME
                /*
                LabelField prog = new LabelField(data[3], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
            	{
            		protected void paint(Graphics g)
            		{
            		      g.setColor(Color.GRAY);            		      
            		      super.paint(g);
            		}
            	};
            	prog.setFont(font);
            	fields[3] = prog;
            	*/
                fields[3] = (Field) data[3];
            	
                /*
            	// PROGRAM PFM
            	LabelField pfm = new LabelField(data[4], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
            	{
            		protected void paint(Graphics g)
            		{
            		      g.setColor(Color.GRAY);
            		      super.paint(g);
            		      
            		      MyApp _app = (MyApp) UiApplication.getUiApplication(); 
              			if(_app._epg.getAreaName() != null)
              				this.setText(_app._epg.getCurrentStationName());
              			else    				
              				this.setText("error!!");
              			
            		}
            		*//*
            		protected void onExposed()
            		{
            			MyApp _app = (MyApp) UiApplication.getUiApplication(); 
            			if(_app._epg.getAreaName() != null)
            				this.setText(_app._epg.getCurrentStationName());
            			else    				
            				this.setText("error!!");
            		}
            		*//*
            	};
            	pfm.setFont(font);
            	fields[4] = pfm;
            	*/
            	fields[4] = (Field) data[4];
            	
                return fields;
            }
        };  
        
        // LOGO
        RegionStyles logo = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(1, 0, 0, 0), new XYEdges(Color.GRAY, Color.WHITE, Color.WHITE, Color.WHITE), Border.STYLE_SOLID), 
        		null,
                null, 
                new XYEdges(((Font.getDefault().getHeight() * 4) - IMAGE_HEIGHT ) / 2 + 10, 10, 0, 10), 
                RegionStyles.ALIGN_CENTER, 
                RegionStyles.ALIGN_TOP);
        dataTemplate.createRegion(new XYRect(0, 0, 1, 4), logo);      

        // Station Name 
        RegionStyles station_name = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(1, 0, 0, 0), new XYEdges(Color.GRAY, Color.WHITE, Color.WHITE, Color.WHITE), Border.STYLE_SOLID),
        		Font.getDefault(),
                null,
                new XYEdges(10, 10, 0, 10), 
                RegionStyles.ALIGN_LEFT, 
                RegionStyles.ALIGN_MIDDLE);
        dataTemplate.createRegion(new XYRect(1, 0, 1, 1), station_name);
        dataTemplate.setRowProperties(0, new TemplateRowProperties(Font.getDefault().getHeight() + station_name.getPadding().top));
        
        // Program Time
        RegionStyles prog_time = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(0, 0, 0, 0), Border.STYLE_SOLID),
        		Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt),
                null,
                new XYEdges(0, 10, 0, 10), 
                RegionStyles.ALIGN_LEFT, 
                RegionStyles.ALIGN_MIDDLE);
        dataTemplate.createRegion(new XYRect(1, 1, 1, 1), prog_time);
        dataTemplate.setRowProperties(1, new TemplateRowProperties(Font.getDefault().getHeight()));  
        
        // Program Name
        RegionStyles prog_name = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(0, 0, 0, 0), Border.STYLE_SOLID),
        		Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt),
                null,
                new XYEdges(0, 10, 0, 10), 
                RegionStyles.ALIGN_LEFT, 
                RegionStyles.ALIGN_MIDDLE);
        dataTemplate.createRegion(new XYRect(1, 2, 1, 1), prog_name);
        dataTemplate.setRowProperties(2, new TemplateRowProperties(Font.getDefault().getHeight()));        
        
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
                
        // Set Left column width
        dataTemplate.setColumnProperties(0, new TemplateColumnProperties(IMAGE_WIDTH + logo.getPadding().left + logo.getPadding().right));
        
        // Set Right column width
        dataTemplate.setColumnProperties(1, new TemplateColumnProperties(Display.getWidth() - dataTemplate.getColumnProperties(0).getWidth()));
        
        //Apply the template to the view
        _tableView.setDataTemplate(dataTemplate);
        dataTemplate.useFixedHeight(true);        
        
    } //setStyle()
    
    public void setProgressBarVal(int val)
    {
    	_model.setValue(val);
    }
    
    private void removeField()
    {
    	//this.delete(_labelField);
    	this.setStatus(null);
    }
    
    public void getProgramInfo()
    {
    	/*
    	_labelField = new LabelField("Updateing...", Field.NON_FOCUSABLE | Field.FIELD_HCENTER);        
    	//this.add(_labelField);		
    	//this.insert(_labelField, 1);
    	this.setStatus(_labelField);
    	*/
    	/*
    	// Initialize progress indicator
        ProgressIndicatorView view = new ProgressIndicatorView(0);
        _model = new ProgressIndicatorModel(5, 100, 0);
        ProgressIndicatorController controller = new ProgressIndicatorController();        
        _model.setController(controller);       
        view.setModel(_model);
        view.setController(controller);        
        controller.setModel(_model);
        controller.setView(view);
        view.createProgressBar(Field.FIELD_HCENTER);
        this.setStatus(view);
    	*/
    	
    	// ActivityIndicator ST
        _aiView = new ActivityIndicatorView(Field.USE_ALL_WIDTH);
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
        //_th.start();
    }
    
    
    
    final class PGInfoThread extends Thread
    { 
    	PGInfoThread()
    	{
    		// DO NOTHING
    	}
    	
    	public void run()
		{
			try {
				updateStatus("PGInfoThread Start");
				/*
				_labelField = new LabelField("The screen closes automatically in two seconds by using a fade transition", Field.NON_FOCUSABLE);
		        
				synchronized (UiApplication.getEventLock()) 
				{		
					getMainManager().add(_labelField);					
				}
				*/
				
				MyApp _app = (MyApp) UiApplication.getUiApplication();
				_app._epg.getProgramInfo();
				/*
				if(_aiView != null || _aiView.getManager() != null )
	        	{
	        		synchronized (UiApplication.getEventLock()) 
					{		
						getMainManager().delete(_aiView);
					}
	        	}
	        	*/
				/*
				// 消して足す
				_tableModel.removeRowRangeAt(0, _tableModel.getNumberOfRows());
				acc();
				*/
				/*
				//NullField field = new NullField();
	            synchronized (UiApplication.getEventLock()) 
				{		
					//getMainManager().add(field);
					_nullField.setFocus();
				}
	            */
				
				//_tableModel.addRow(new Object[]{"bitmap", "name", "pgTitle", "pgPfm"}, true);
				//_tableModel.insertRowAt(_tableModel.getNumberOfRows(), data, doNotifyListeners)
				
				stationsInfoV = _app._epg.GetStationsInfoV();
				//int num = stationsInfoV.size() - 1;
				int num = 0;
				
				//_controller.setFocusPolicy(0);
				
	            for(Enumeration e = stationsInfoV.elements(); e.hasMoreElements(); )
	            //for(int num = stationsInfoV.size() - 1; num<0; num--)
	            //while(num > -1)
	            {
	            	//_tableModel.removeRowAt(num, true);
	            	
	            	
	            	//Hashtable station = (Hashtable) stationsInfoV.elementAt(num);
	            	Hashtable station = (Hashtable) e.nextElement();
	            	//updateStatus("Vec2.1: " + station.get("id").toString());
	            	/*
	            	if(!e.hasMoreElements())
	            	{
	            	
	            	// LOGO
	            	//Bitmap bitmap = GetWebBitmap((String) station.get("logo_medium"));
	            	Bitmap bitmap = (Bitmap) station.get("station_logo");
	                //BitmapField bitmapField = new BitmapField(bitmap);
	                
	                // Name
	                String name = (String) station.get("name");
	                
	                _tableModel.addRow(new Object[]{bitmap, name, "pgTitle", "pgPfm"});
	                
	            	}
	            	*/
	                // etc
	                //String pgTitle = (String) station.get("PGInfo_title");
	                //String pgPfm  = (String) station.get("PGInfo_pfm");
	                
	            	// Add data to the TableModel            
	                //_tableModel.addRow(new Object[]{bitmap, name, pgTitle, pgPfm});
	                //_tableModel.insertRowAt(num, new Object[]{bitmap, name, pgTitle, pgPfm}, true);
	                /*
	                if(num != 0)
	                {
	                	//Object[] hoge = (Object[]) _tableModel.getRow(num - 1);
	                	//updateStatus("HOGE: " + ((String)hoge[1]));
	                	//hoge[1] = (String)hoge[1] + " KAMOSIRENAI";
	                
	                	//Field[] hogera = dataTemplate.getDataFields(num - 1);	                	
	                	//((LabelField)hogera[1]).setFocus();
	                }
	                */
	            	/*
	            	synchronized (UiApplication.getEventLock()) 
					{
	            		UiApplication.getUiApplication().invokeLater(new Runnable()
				        {
				            public void run()
				            {
				            	_controller.setFocusPolicy(0);
				            	
				            }
				        });
					}
	            	*/
	            	_controller.setFocusPolicy(0);
	            	Hashtable htb = (Hashtable) station.get("prog_now");
	            	_tableModel.setElement(num, 2, (String) htb.get("time"), false);
	            	_tableModel.setElement(num, 3, (String) htb.get("title"), true);
	                //_tableModel.setElement(num, 4, (String) htb.get("pfm"), true);
	                _controller.setFocusPolicy(TableController.ROW_FOCUS);
	                
	                /*
	                synchronized (UiApplication.getEventLock()) 
					{
	            		UiApplication.getUiApplication().invokeLater(new Runnable()
				        {
				            public void run()
				            {
				            	_controller.setFocusPolicy(TableController.ROW_FOCUS);
				            }
				        });
					}
					*/
	                num++;
	            }
	            
	            //_controller.setFocusPolicy(0);
	            //_tableModel.modelReset();
	            //_controller.setFocusPolicy(TableController.ROW_FOCUS);
	            synchronized (UiApplication.getEventLock()) 
				{		
	            	removeField();
				}
	            
	            /*
	            _tableModel.removeRowAt(_tableModel.getNumberOfRows()-1, true);
	            
	            if(_controller.moveFocus(Field.AXIS_VERTICAL | -1))
	            	updateStatus("MOVE SCCESSED");
	            else
	            	updateStatus("MOVE FAILED");
	            */
	            //EventInjector.KeyCodeEvent keyCodeEvent = new EventInjector.KeyCodeEvent(KeyCodeEvent.KEY_DOWN, (char) Keypad.KEY_NEXT, 0, 100);  
	            
	            //EventInjector.KeyCodeEvent keyCodeEvent = new EventInjector.KeyCodeEvent(KeyCodeEvent.KEY_DOWN, (char) Keypad.KEY_NEXT, 0);
	            //keyCodeEvent.post();
	            
	            //_tableView.setFocus(0, 0, KeypadListener.STATUS_FOUR_WAY);
	            //_controller.moveFocus(0, 4, KeypadListener.STATUS_FOUR_WAY, keyCodeEvent.getTime());
	            //_tableView.invalidate();
	            //return keyCodeEvent.getTime();
				
				/*
				synchronized (UiApplication.getEventLock()) 
				{		
					getMainManager().deleteAll();
				}
				synchronized (UiApplication.getEventLock()) 
				{
					add(_tableView);			
				}
				*/
				/*
				synchronized (UiApplication.getEventLock()) 
				{
					delete(_tableView);						
				}
				_tableModel.modelReset();
				acc();
				*/
				/*
				Object[] hoge = (Object[]) _tableModel.getRow(1);
				updateStatus("HOGE: " + ((String)hoge[1]));
				hoge[1] = (String)hoge[1] + " KAMOSIRENAI";
				
				
				_tableModel.setElement(0, 1, "hoge", true);
				*/
				//_tableModel.removeRowAt(1, false);
                //_tableModel.insertRowAt(1, new Object[]{hoge[0], hoge[1], hoge[2], hoge[3]}, false);
				
                //_tableModel.modelReset();
				/*
				final Field[] fields = dataTemplate.getDataFields(0);
				
				updateStatus("NUM: " + fields.length);
				
				updateStatus("TEXT: " + ((LabelField) fields[1]).getText());
							
				synchronized (UiApplication.getEventLock()) 
				{
					UiApplication.getUiApplication().invokeLater(new Runnable()
			        {
			            public void run()
			            {
			            	((LabelField) fields[1]).setText("hoge");
			            	
			            }
			        });					
				}
				*/
				
				/*
				invalidate();
				*/
			} catch (Exception e) {
				updateStatus("PGInfoThread " + e.toString());
			} 
		}
    } //PGInfoThread
    
    class ItemProvider implements CommandItemProvider 
    {
        public Object getContext(Field field)
        {            
            return field;
        }
        public Vector getItems(Field field)
        {            
            Vector items = new Vector();
            
            CommandItem defaultCmd;
            /*
            Image myIcon = ImageFactory.createImage(Bitmap.getBitmapResource("icon.png"));
            
            if(field.equals(emailAddress)){          
                 defaultCmd = new CommandItem(new StringProvider("Email Address"), null, new Command(new DialogCommandHandler()));
            }
            else{
                defaultCmd = new CommandItem(new StringProvider("選局/停止"), null, new Command(new DialogCommandHandler()));
            }
            */
            defaultCmd = new CommandItem(new StringProvider("再生/停止"), null, new Command(new CommandHandler() 
            {
                
                public void execute(ReadOnlyCommandMetadata metadata, Object context) 
                {
                    // Display selected device in a pop up dialog                
                    //Object[] objArray = (Object[])_tableModel.getRow(_tableView.getRowNumberWithFocus()); 
                    
                    MyApp _app = (MyApp) UiApplication.getUiApplication();
                    //_app._epg.SetCurrentStation((String)((Hashtable)stationsInfoV.elementAt(_tableView.getRowNumberWithFocus())).get("id"));
                    _app._epg.SetCurrentStation(_tableView.getRowNumberWithFocus());
                    /*
                    // 再生、停止を実行
                    if(_app._mediaActions.isPlaying())
                    {             
                   	 _app._mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_STOP, MediaActionHandler.SOURCE_BACKGROUND_KEY, null);
                   	 _app._mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_PLAY, MediaActionHandler.SOURCE_BACKGROUND_KEY, null);
                   	 //_app._mediaActions.doStop();
                   	 //_app._mediaActions.doPlay();
                   	 
                    }
                    else
                    {
                   	 _app._mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_PLAY, MediaActionHandler.SOURCE_BACKGROUND_KEY, null);
                   	 //_app._mediaActions.doPlay();
                    }
                    */
                    //Dialog dialog = new Dialog(Dialog.D_OK, (String)objArray[1], 0, null, 0); 
                    //Dialog dialog = new Dialog(Dialog.D_OK, "CurrentStation:" + _app._epg.GetCurrentStation(), 0, null, 0);
               	 //Dialog dialog = new Dialog(Dialog.D_OK, (String)((Hashtable)stationsInfoV.elementAt(_tableView.getRowNumberWithFocus())).get("id"), 0, null, 0); 
                    //dialog.doModal();
                    
                    //_app.popMyScreen();
                    
                    //UiApplication.getUiApplication().invokeLater( _popRunnable, 500, false);
                    invalidate();
                    
                    //Field[] fields = dataTemplate.getDataFields(0);
                    //((LabelField)fields[2]).setText(_app._epg.getCurrentStationName());
                    
                }
            }));
            items.addElement(defaultCmd);
            items.addElement(new CommandItem(new StringProvider("番組表"), null, new Command(new CommandHandler() 
            {
                
                public void execute(ReadOnlyCommandMetadata metadata, Object context) 
                {
                	MyApp _app = (MyApp) UiApplication.getUiApplication();
                	//_app.pushEPGScreen();
                	
                	//---- For EPGScreen ----//
                    //_testScreen = new TestScreen();
                	EPGScreen _epgScreen = new EPGScreen(_tableView.getRowNumberWithFocus());
                	
                    // FADE IN
                    TransitionContext transIN = new TransitionContext(TransitionContext.TRANSITION_FADE);
                    transIN.setIntAttribute(TransitionContext.ATTR_DURATION, 100);
                    
                    //TransitionContext transIN = new TransitionContext(TransitionContext.TRANSITION_SLIDE);
                    //transIN.setIntAttribute(TransitionContext.ATTR_DURATION, 250);
                    //transIN.setIntAttribute(TransitionContext.ATTR_DIRECTION, TransitionContext.DIRECTION_RIGHT); 
                    
                    UiEngineInstance engine = Ui.getUiEngineInstance();
                    engine.setTransition(null, _epgScreen, UiEngineInstance.TRIGGER_PUSH, transIN);
                    
                    // FADE OUT
                    TransitionContext transOUT = new TransitionContext(TransitionContext.TRANSITION_FADE);
                    transOUT.setIntAttribute(TransitionContext.ATTR_DURATION, 100);
                    transOUT.setIntAttribute(TransitionContext.ATTR_KIND, TransitionContext.KIND_OUT);
                    
                    //TransitionContext transOUT = new TransitionContext(TransitionContext.TRANSITION_SLIDE);
                    //transOUT.setIntAttribute(TransitionContext.ATTR_DURATION, 250);
                    //transOUT.setIntAttribute(TransitionContext.ATTR_DIRECTION, TransitionContext.DIRECTION_LEFT);                                                            
                    //transOUT.setIntAttribute(TransitionContext.ATTR_KIND, TransitionContext.KIND_OUT);  
                    
                    engine.setTransition(_epgScreen, null, UiEngineInstance.TRIGGER_POP, transOUT);
                    
                    _app.pushScreen(_epgScreen);
                    //UiApplication.getUiApplication().pushScreen(new CustomPopup("Hello good morning "));
                }
            })));
            //items.addElement(new CommandItem(new StringProvider("現在の番組"), null, new Command(new DialogCommandHandler())));
            //items.addElement(new CommandItem(new StringProvider("番組表"), null, new Command(new DialogCommandHandler())));
            //items.addElement(new CommandItem(new StringProvider("停止"), null, new Command(new DialogCommandHandler())));
            
            return items;
        }
    }
    class DialogCommandHandler extends CommandHandler
    {
        public void execute(ReadOnlyCommandMetadata metadata, Object context)
        {
            Dialog.alert("Executing command for " + context.toString());
        }           
    }
    
    class CustomPopup extends PopupScreen
	{
		//Bitmap img;
		public CustomPopup(String helpTxt)
		{
			super(new VerticalFieldManager(VerticalFieldManager.NO_VERTICAL_SCROLL));
			 //img = Bitmap.getBitmapResource("vista-theme-red.png");
			VerticalFieldManager vfm = new VerticalFieldManager(Manager.VERTICAL_SCROLL|Manager.VERTICAL_SCROLLBAR);
			LabelField rtf = new LabelField(helpTxt);
			vfm.add(rtf);
			add(vfm);
		}
	}
}
    	
