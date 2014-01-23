/*
	MyScreen.java
	
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
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.HttpConnection;

import net.rim.device.api.browser.field2.BrowserField;
import net.rim.device.api.browser.field2.BrowserFieldConfig;
import net.rim.device.api.command.Command;
import net.rim.device.api.command.CommandHandler;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.media.MediaActionHandler;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.ActiveRichTextField;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.TextField;
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
import net.rim.device.api.ui.container.FullScreen;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.decor.BackgroundFactory;
import net.rim.device.api.ui.decor.Border;
import net.rim.device.api.ui.decor.BorderFactory;
import net.rim.device.api.ui.menu.SubMenu;
import net.rim.device.api.util.StringProvider;



/**
 * A class extending the MainScreen class, which provides default standard
 * behavior for BlackBerry GUI applications.
 */


public final class MyScreen extends MainScreen implements FocusChangeListener, FieldChangeListener
{
	private MyApp _app;
	private Runnable _onCloseRunnable;
	private MenuItem _myItem;
	
	private LabelField tab1;
    private LabelField tab2;
    private LabelField tab3;
    private LabelField spacer1;
    private LabelField spacer2;
    private VerticalFieldManager tabArea;
    
    private VerticalFieldManager tab1Manager;
    private VerticalFieldManager tab2Manager;
    private VerticalFieldManager tab3Manager;
    
    private LabelField tab1Heading;
    private LabelField tab2Heading;
    private LabelField tab3Heading;
	
    private ActivityIndicatorView aiView;
    private LabelField _statusComment;
    //private TableModel _tableModel;
    //private TableView _tableView;
    //private TableController _controller;
   
    private HorizontalFieldManager hManager;
    
	/**
     * Creates a new MyScreen object
     */
	
	private BasicEditField _statusField;
	
	private Screen _secondaryScreen; // Transition
    private Runnable _popRunnable; // Transition
    
    public MyScreen()
    {        
    	//super(Manager.NO_VERTICAL_SCROLL);
    	
    	_app = (MyApp) UiApplication.getUiApplication();
    	
    	_statusField = new BasicEditField(Field.NON_FOCUSABLE);
        add(_statusField);
        
    	/*
    	// Transition ST
    	_secondaryScreen = new StationsScreen("title", Color.LIGHTBLUE);
        //_secondaryScreen.setBackground( BackgroundFactory.createSolidBackground(Color.LIGHTBLUE) );

        LabelField labelField = new LabelField("The screen closes automatically in two seconds by using a fade transition");
        _secondaryScreen.add(labelField);
    	 *//*
        ButtonField buttonField = new ButtonField("View Transition", ButtonField.CONSUME_CLICK) ;
        buttonField.setChangeListener(this);
        add(buttonField);
        
        
        _popRunnable = new Runnable() {
            public void run() {
            	 UiApplication.getUiApplication().popScreen(_secondaryScreen);
            }
        };
        // Transition END 
    	
        
        // ActivityIndicator ST
        aiView = new ActivityIndicatorView(Field.USE_ALL_WIDTH);
        ActivityIndicatorModel aiModel = new ActivityIndicatorModel();
        ActivityIndicatorController aiController = new ActivityIndicatorController();
        
        aiView.setController(aiController);
        aiView.setModel(aiModel);
        aiController.setModel(aiModel);
        aiController.setView(aiView);
        aiModel.setController(aiController);
        // Define the indicator image and create a field from it
        Bitmap aiBitmap = Bitmap.getBitmapResource("spinner2.png");
        
        aiView.createActivityImageField(aiBitmap, 6, Field.FIELD_HCENTER);
        // add the view to the screen
        add(aiView);
        
        
        *//*
        MenuItem _stopIndicator = new MenuItem("Stop spinner", 66000, 0)
        {
            public void run()
            {
            	aiView.getModel().cancel();
            }
        };
        MenuItem _resumeIndicator = new MenuItem("Resume spinner", 66010, 0)
        {
            public void run()
            {
            	aiView.getModel().resume();
            }
        };
        addMenuItem(_stopIndicator);
        addMenuItem(_resumeIndicator);
    	*//*
        
    	// ActivityIndicator END
        
           
        
        HorizontalFieldManager hManager = new HorizontalFieldManager();
        tab1 = new LabelField("Page 1", LabelField.FOCUSABLE);
        tab2 = new LabelField("Page 2", LabelField.FOCUSABLE);
        tab3 = new LabelField("Page 3", LabelField.FOCUSABLE);
        spacer1 = new LabelField(" | ", LabelField.NON_FOCUSABLE);
        spacer2 = new LabelField(" | ", LabelField.NON_FOCUSABLE);

        tab1.setFocusListener(this);
        tab2.setFocusListener(this);
        tab3.setFocusListener(this);
        hManager.add(tab1);
        hManager.add(spacer1);
        hManager.add(tab2);
        hManager.add(spacer2);
        hManager.add(tab3);
        
        add(hManager);        
        add(new SeparatorField());
        
        tab1Manager = new VerticalFieldManager(Manager.VERTICAL_SCROLL);
        tab2Manager = new VerticalFieldManager(Manager.VERTICAL_SCROLL);
        tab3Manager = new VerticalFieldManager(Manager.VERTICAL_SCROLL);
        
		if (tab1Heading == null) {
		    tab1Heading = new LabelField("Registration");
		    tab1Manager.add(tab1Heading);
		    
		    
		    
		}
        
        if (tab2Heading == null) {
            //tab2Heading = new LabelField("Password Recovery");
            //tab2Manager.add(tab2Heading);
            
            _statusField = new BasicEditField(Field.NON_FOCUSABLE);
            tab2Manager.add(_statusField);
        }
        
        
        if (tab3Heading == null) {
            tab3Heading = new LabelField("Interests");
            tab3Manager.add(tab3Heading);
                        
        }
        
        tabArea = tab1Manager;
        add(tabArea);

        VerticalFieldManager vm_status = new VerticalFieldManager(Manager.NO_VERTICAL_SCROLL);
        vm_status.add(new SeparatorField());
        _statusComment = new LabelField("", LabelField.NON_FOCUSABLE);
        _statusComment.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt));
        vm_status.add(_statusComment);
        setStatus(vm_status);
        
        updateStatus("hoge");
        */
    }
    
    
    
    
    public void focusChanged(Field field, int eventType)
    {
        if (tabArea != null) {
                if (eventType == FOCUS_GAINED) {
                        if (field == tab1) {
                                delete(tabArea);
                                
                                tabArea = tab1Manager;
                                add(tabArea);
                        } else if (field == tab2) {
                                delete(tabArea);

                                tabArea = tab2Manager;
                                add(tabArea);
                        } else if (field == tab3) {
                                delete(tabArea);

                                tabArea = tab3Manager;
                                add(tabArea);
                        }
                }
        }

    }

    /*
    public void AddStationList()
    {
    	MyApp _app = (MyApp) UiApplication.getUiApplication();
        final Vector stationsInfoV = _app._epg.GetStationsInfoV();
        
        
        // マネージャーバージョン 
        for(Enumeration e = stationsInfoV.elements(); e.hasMoreElements(); )
        {
        	final Hashtable station = (Hashtable) e.nextElement();
        	updateStatus("Vec: " + station.get("id").toString());
        	
        	
        	
        	
        	VerticalFieldManager vManager1 = new VerticalFieldManager(VerticalFieldManager.FIELD_VCENTER | Field.FOCUSABLE);
        	
        	Bitmap bitmap = null;
			try {
				bitmap = GetWebBitmap((String) station.get("logo_small"));
			} catch (Exception e1) {
				updateStatus("画像取得失敗 " + e1);
			}
        	BitmapField bitmapField = new BitmapField(bitmap, Field.NON_FOCUSABLE);
        	vManager1.add(bitmapField);
        	
        	
        	VerticalFieldManager vManager = new VerticalFieldManager(VerticalFieldManager.FIELD_VCENTER);
        	
        	
        	LabelField tmp = new LabelField(station.get("name"), Field.NON_FOCUSABLE)
        	{
	            protected boolean navigationClick(int status,int time)
	            {
	            	Dialog dialog = new Dialog(Dialog.D_OK, (String) station.get("name"), 0, null, 0);
	                dialog.doModal();   
	                return true;
	            }
        	};
        	
        	tmp.setPadding(10, 10, 0, 10);
        	vManager.add(tmp);
        	
        	
        	LabelField prog = new LabelField(station.get("PGInfo_title"), Field.NON_FOCUSABLE)
        	{
        		protected void paint(Graphics g)
        		{
        		      g.setColor(Color.GRAY); //here you can add any colors. either color codes (like - 0x0511a0a) or like this- Color.RED
        		      super.paint(g);
        		}
        	};
        	Font font = Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt);
        	//int def_size = font.getHeight(Ui.UNITS_pt);
        	//updateStatus("FONTSIZE: " + def_size);
        	//font.derive(Font.PLAIN, 5, Ui.UNITS_pt);
        	
        	prog.setFont(font);
        	
        	prog.setPadding(0, 10, 0, 10);
        	vManager.add(prog);
        	
        	
        	LabelField pfm = new LabelField(station.get("PGInfo_pfm"), Field.NON_FOCUSABLE)
        	{
        		protected void paint(Graphics g)
        		{
        		      g.setColor(Color.GRAY); //here you can add any colors. either color codes (like - 0x0511a0a) or like this- Color.RED
        		      super.paint(g);
        		}
        	};
        	
        	pfm.setFont(font);
        	
        	pfm.setPadding(0, 10, 10, 10);
        	vManager.add(pfm);        	
        	
        	
        	hManager = new HorizontalFieldManager(Manager.USE_ALL_WIDTH | Field.FOCUSABLE)
        	{
        		
        		int background_color = 0;
        		
        		
        		protected void onFocus(int direction)
        		{
        	        super.onFocus(direction);
        	        background_color=Color.RED;
        	        invalidate();
        	    }

        	    protected void onUnfocus()
        	    {
        	        invalidate();
        	        background_color=Color.GREEN;
        	    }
				
				protected void paint(Graphics g)
				{
					g.setBackgroundColor(background_color);
			        g.clear();
			        invalidate();
			        super.paint(g);
				}
				
			    protected boolean navigationClick(int status, int time)
			    {
			        if(Touchscreen.isSupported()){
			            return false;
			        }else{
			            fieldChangeNotify(1);
			            return true;
			        }

			    }
			    protected boolean touchEvent(TouchEvent message)
			    {
			        if (TouchEvent.CLICK == message.getEvent())
			        {
			            FieldChangeListener listener = getChangeListener();
			            if (null != listener)
			                this.setFocus();
			            listener.fieldChanged(this, 1);
			        }
			        return super.touchEvent(message);
			    }
			    
        	};
        	
        	//hManager.add(new NullField(Field.FOCUSABLE));
        	hManager.add(vManager1);
        	hManager.add(vManager);
        	
        	
        	tab3Manager.add(hManager);
        }
        	
        	*//*
        	// LOGO
        	Bitmap bitmap = null;
			try {
				bitmap = GetWebBitmap((String) station.get("logo_large"));
			} catch (Exception e1) {
				updateStatus("画像取得失敗 " + e1);
			}
            BitmapField bitmapField = new BitmapField(bitmap){
                protected boolean navigationClick(int status,int time)
                {
                	Dialog dialog = new Dialog(Dialog.D_OK, (String) station.get("name"), 0, null, 0);
                    dialog.doModal();   
                    return true;
                }
            };
            tab3Manager.add(bitmapField); 
            *//*    
        }
        */
        /*
        //完成形
    	try {
        	
        	_tableModel = new TableModel();
            // Set up view and controller
            _tableView = new TableView(_tableModel);
            //_tableView.setDataTemplateFocus(BackgroundFactory.createLinearGradientBackground(Color.WHITE, Color.WHITE, Color.BLUEVIOLET, Color.BLUEVIOLET));
            _controller = new TableController(_tableModel, _tableView);            
            _tableView.setController(_controller);
            
            _controller.setFocusPolicy(TableController.ROW_FOCUS);
                        
            _controller.setCommand(new Command(new CommandHandler() 
            {
                *//**
                 * @see net.rim.device.api.command.CommandHandler#execute(ReadOnlyCommandMetadata, Object)
                 *//*
                 public void execute(ReadOnlyCommandMetadata metadata, Object context) 
                 {
                     // Display selected device in a pop up dialog                
                     //Object[] objArray = (Object[])_tableModel.getRow(_tableView.getRowNumberWithFocus()); 
                     
                     MyApp _app = (MyApp) UiApplication.getUiApplication();
                     _app._epg.SetCurrentStation((String)((Hashtable)stationsInfoV.elementAt(_tableView.getRowNumberWithFocus())).get("id"));
                     
                     // 再生、停止を実行
                     if(_app._mediaActions.isPlaying())
                     {             
                    	 _app._mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_STOP, MediaActionHandler.SOURCE_BACKGROUND_KEY, null);
                    	 _app._mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_PLAY, MediaActionHandler.SOURCE_BACKGROUND_KEY, null);
                     }
                     else
                     {
                    	 _app._mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_PLAY, MediaActionHandler.SOURCE_BACKGROUND_KEY, null);                     
                     }
                     
                     //Dialog dialog = new Dialog(Dialog.D_OK, (String)objArray[1], 0, null, 0); 
                     Dialog dialog = new Dialog(Dialog.D_OK, "CurrentStation:" + _app._epg.GetCurrentStation(), 0, null, 0);
                     dialog.doModal();
                     
                     UiApplication.getUiApplication().invokeLater( _popRunnable, 500, false);
                 }
             }));
            
            setStyle();
            tab1Manager.add(_tableView);
            //_secondaryScreen.add(_tableView);
            
            for(Enumeration e = stationsInfoV.elements(); e.hasMoreElements(); )
            {
            	Hashtable station = (Hashtable) e.nextElement();
            	updateStatus("Vec2: " + station.get("id").toString());
            	
            	// LOGO
            	Bitmap bitmap = GetWebBitmap((String) station.get("logo_medium"));
                //BitmapField bitmapField = new BitmapField(bitmap);
                
                // Name
                String name = (String) station.get("name");
                
                // etc
                String pgTitle = (String) station.get("PGInfo_title");
                String pgPfm  = (String) station.get("PGInfo_pfm");            	

                // Separator
                //SeparatorField sf = new SeparatorField();
                
            	// Add data to the TableModel            
                _tableModel.addRow(new Object[]{bitmap, name, pgTitle, pgPfm});
            }

            
    	} catch (Exception e) {
			updateStatus("[table] " + e.toString());
		}
        *//*
        
    }
    */

	public void setOnCloseRunnable( Runnable runnable )
    {
        _onCloseRunnable = runnable;
    }
    
    public void close()
    {
    	//Dialog.alert("Goodbye!");
    	
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
        super.close();
    }
      

    /*
    public void setStyle() throws Exception
    {
    	final int NUM_ROWS = 3;
    	final int NUM_COLUMNS = 2;
    	final int IMAGE_WIDTH = 123;
    	    
        DataTemplate dataTemplate = new DataTemplate(_tableView, NUM_ROWS, NUM_COLUMNS)
        {
            public Field[] getDataFields(int modelRowIndex)
            {
                Object[] data = (Object[]) _tableModel.getRow(modelRowIndex);
                Field[] fields = new Field[data.length];
                
                
                // LOGO
                fields[0] = new BitmapField((Bitmap) data[0], DrawStyle.VCENTER);
                
                // STATION NAME
                fields[1] = new LabelField(data[1], Field.NON_FOCUSABLE | LabelField.ELLIPSIS);                
                                
            	// PROGRAM NAME
                LabelField prog = new LabelField(data[2], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
            	{
            		protected void paint(Graphics g)
            		{
            		      g.setColor(Color.GRAY); //here you can add any colors. either color codes (like - 0x0511a0a) or like this- Color.RED
            		      super.paint(g);
            		}
            	};
            	Font font = Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt);            	
            	prog.setFont(font);
            	fields[2] = prog;
            	
            	
            	// PROGRAM PFM
            	LabelField pfm = new LabelField(data[3], Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
            	{
            		protected void paint(Graphics g)
            		{
            		      g.setColor(Color.GRAY); //here you can add any colors. either color codes (like - 0x0511a0a) or like this- Color.RED
            		      super.paint(g);
            		}            		
            	};
            	pfm.setFont(font);
            	fields[3] = pfm;
            	
                return fields;
            }
        };  
        
        // LOGO
        RegionStyles logo = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(1, 0, 0, 0), new XYEdges(Color.GRAY, Color.WHITE, Color.WHITE, Color.WHITE), Border.STYLE_SOLID), 
        		null,
                null, 
                new XYEdges(0, 0, 0, 10), 
                RegionStyles.ALIGN_CENTER, 
                RegionStyles.ALIGN_TOP);
        dataTemplate.createRegion(new XYRect(0, 0, 1, 3), logo);

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
        
        // Program Name
        RegionStyles prog_name = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(0, 0, 0, 0), Border.STYLE_SOLID),
        		Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt),
                null,
                new XYEdges(0, 10, 0, 10), 
                RegionStyles.ALIGN_LEFT, 
                RegionStyles.ALIGN_MIDDLE);
        dataTemplate.createRegion(new XYRect(1, 1, 1, 1), prog_name);
        dataTemplate.setRowProperties(1, new TemplateRowProperties(Font.getDefault().getHeight()));        
        
        // Program PFM
        RegionStyles prog_pfm = new RegionStyles(
        		BorderFactory.createSimpleBorder(new XYEdges(0, 0, 0, 0), Border.STYLE_SOLID), 
        		Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt),
                null,
                new XYEdges(0, 10, 5, 10),
                RegionStyles.ALIGN_LEFT, 
                RegionStyles.ALIGN_MIDDLE);
        dataTemplate.createRegion(new XYRect(1, 2, 1, 1), prog_pfm);
        dataTemplate.setRowProperties(2, new TemplateRowProperties(Font.getDefault().getHeight() + prog_pfm.getPadding().bottom));
                
        // Set Left column width
        dataTemplate.setColumnProperties(0, new TemplateColumnProperties(IMAGE_WIDTH + logo.getPadding().left + logo.getPadding().right));
        
        // Set Right column width
        dataTemplate.setColumnProperties(1, new TemplateColumnProperties(Display.getWidth() - dataTemplate.getColumnProperties(0).getWidth()));
        
        //Apply the template to the view
        _tableView.setDataTemplate(dataTemplate);
        dataTemplate.useFixedHeight(true);
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
        UiApplication.getUiApplication().invokeLater(new Runnable()
        {
            public void run()
            {
                _statusField.setText(_statusField.getText() + "\n" + message);
            }
        });
    }
    
    protected void makeMenu( Menu menu, int instance )
    {
    	 // MenuItem
        _myItem = new MenuItem(new StringProvider("Set AudioPath"), 0x230000, 0);        
        _myItem.setCommandContext(new Object()
        {
            public String toString()
            {
                return "My MenuItem Object"; 
            }          
        });       
        // Set command to be invoked by the MenuItem
        _myItem.setCommand(new Command(new DialogCommandHandler()));
        //addMenuItem(_myItem);
        
    	MenuItem[] toAddMenu = new MenuItem[1];
    	toAddMenu[0] = _myItem;
        SubMenu statusSubMenu = new SubMenu(toAddMenu,"My Status",300,3);
        //statusSubMenu.add(_myItem);
        //statusSubMenu.add(_status2);
        menu.add(statusSubMenu);
        
        
        MenuItem _prog = new MenuItem(new StringProvider("選局"), 0x00020000, 0);
        _prog.setCommand(new Command(        
	        new CommandHandler()
	        {
	        	public void execute(ReadOnlyCommandMetadata metadata, Object context)
	            {
	            	MyApp _app = (MyApp) UiApplication.getUiApplication();
	            	_app.pushMyScreen();
	            }        	
	        }
        ));
        
        menu.add(_prog);        
        
        super.makeMenu(menu, instance);
    };
    
    
    public void SetStatusCommand(final String val)
    {
    	UiApplication.getUiApplication().invokeLater(new Runnable()
        {
            public void run()
            {
            	_statusComment.setText(val);
            }
        });    	
    }
    
    
    public void deleteField()
    {
    	delete(aiView);
    }
    
    class DialogCommandHandler extends CommandHandler
    {
        public void execute(ReadOnlyCommandMetadata metadata, Object context)
        {
        	Dialog.inform(context.toString());
        	//Dialog.inform("CapitolChange");
        	MyApp _app = (MyApp) UiApplication.getUiApplication();
        	_app._mediaActions.SetAudioPath();
        }
    }

    

	public void fieldChanged(Field field, int context)
	{
		//UiApplication.getUiApplication().pushScreen(_secondaryScreen);
		((MyApp) UiApplication.getUiApplication()).pushMyScreen();
		//UiApplication.getUiApplication().invokeLater( _popRunnable, 3000, false);
	}

} //MyScrren


