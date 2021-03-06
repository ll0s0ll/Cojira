/*
	StationsScreen2.java
	
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

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import net.rim.device.api.command.Command;
import net.rim.device.api.command.CommandHandler;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Characters;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.XYEdges;
import net.rim.device.api.ui.XYRect;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.GaugeField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
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
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.decor.BackgroundFactory;
import net.rim.device.api.ui.decor.Border;
import net.rim.device.api.ui.decor.BorderFactory;
import net.rim.device.api.ui.menu.CommandItem;
import net.rim.device.api.ui.menu.CommandItemProvider;
import net.rim.device.api.ui.menu.DefaultContextMenuProvider;
import net.rim.device.api.util.StringProvider;


public class MyScreen extends MainScreen
{
	private MyApp _app;
	private Runnable _onCloseRunnable;
	
	private StandardTitleBar _titleBar;
	
	private TableModel _tableModel;
	private TableView _tableView;
	private DataTemplate dataTemplate;
	
	private VerticalFieldManager _tab1Manager;
	private BasicEditField _statusField;
	
	private PopupScreen _popup;
	private GaugeField _gaugeField;
	private Timer _timer;
	
	private ActivityIndicatorView _aiView;
	
	private boolean isForcedTermination = false;
	
	
	public MyScreen()
	{
		super(Manager.NO_VERTICAL_SCROLL);
		
		_app = (MyApp) UiApplication.getUiApplication();
		
		//---- タイトルバーを作成
		_titleBar = new StandardTitleBar() ;
		_titleBar.addSignalIndicator();
		_titleBar.addNotifications();
		_titleBar.addClock();
		_titleBar.setPropertyValue(StandardTitleBar.PROPERTY_BATTERY_VISIBILITY, StandardTitleBar.BATTERY_VISIBLE_ALWAYS);
		setTitleBar(_titleBar);
		
		//---- テーブルを作成 --------------------------------------------- //
		// Table
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
		
		TableController _controller = new TableController(_tableModel, _tableView, TableController.ROW_FOCUS);
		//_controller.setFocusPolicy(TableController.ROW_FOCUS);
		_tableView.setController(_controller);
		_controller = null;
		
		
		dataTemplate = new DataTemplate(_tableView, 1, 1)
		{
			public Field[] getDataFields(int modelRowIndex)
			{
				Object[] data = (Object[]) _tableModel.getRow(modelRowIndex);
				Field[] fields = new Field[data.length];
				// LOGO
				//fields[0] = new BitmapField((Bitmap) data[0]);
				fields[0] = (HorizontalFieldManager)data[0];
				
				return fields;
			}
		};
		
		// LOGO
		RegionStyles logo = new RegionStyles(
				BorderFactory.createSimpleBorder(new XYEdges(1, 0, 0, 0), new XYEdges(Color.GRAY, Color.WHITE, Color.WHITE, Color.WHITE), Border.STYLE_SOLID), 
				null,
				null,
				new XYEdges(10, 10, 0, 10),
				RegionStyles.ALIGN_CENTER,
				RegionStyles.ALIGN_TOP);
		dataTemplate.createRegion(new XYRect(0, 0, 1, 1), logo);
		dataTemplate.setRowProperties(0, new TemplateRowProperties(Font.getDefault().getHeight() * 4 + 20));
		
		// Set Left column width
		dataTemplate.setColumnProperties(0, new TemplateColumnProperties(Display.getWidth()));        
		
		//Apply the template to the view
		_tableView.setDataTemplate(dataTemplate);
		dataTemplate.useFixedHeight(true);
		
		add(_tableView);
		
		
		//---- ActivityIndicator --------------------------------------------- //
		_aiView = new ActivityIndicatorView(Field.USE_ALL_WIDTH);
		ActivityIndicatorModel aiModel = new ActivityIndicatorModel();
		ActivityIndicatorController aiController = new ActivityIndicatorController();
		
		_aiView.setController(aiController);
		_aiView.setModel(aiModel);
		_aiView.setMargin(5, 0, 5, 0);
		
		aiController.setModel(aiModel);
		aiController.setView(_aiView);
		aiModel.setController(aiController);
		Bitmap aiBitmap = Bitmap.getBitmapResource("spinner2.png");
		
		_aiView.createActivityImageField(aiBitmap, 6, Field.FIELD_HCENTER);
		
		
		//---- ログ表示用フィールド --------------------------------------------- //
		_statusField = new BasicEditField(Field.NON_FOCUSABLE);
		_tab1Manager = new VerticalFieldManager(Manager.VERTICAL_SCROLL);
		_tab1Manager.add(_statusField);
		
		//---- 音量表示用フィールド --------------------------------------------- //
		_gaugeField = new GaugeField(null, 0, 100, 0, GaugeField.NO_TEXT)
		{
			protected void layout(int width, int height)
			{
				width = Display.getWidth() / 3;
				super.layout(width, height);
			}
		};
		VerticalFieldManager manager = new VerticalFieldManager();
		manager.add(_gaugeField);
		_popup = new PopupScreen(manager);
		
	} //MyScreen()
	
	
	protected boolean keyChar(char c, int status, int time)
	{
		switch(c)
		{
			case 'i':
			{
				// 現在の番組情報を表示
				Station tmp_station = _app.getStationByIndex(_tableView.getRowNumberWithFocus());
				Program[] tmp_progs = tmp_station.getPrograms();
				_app.showProgScreen(tmp_progs[0]);
				break;
			}
			case 'n':
			{
				// 次の番組情報を表示
				Station tmp_station = _app.getStationByIndex(_tableView.getRowNumberWithFocus());
				Program[] tmp_progs = tmp_station.getPrograms();
				_app.showProgScreen(tmp_progs[1]);
				break;
			}
			case 'r':
			{
				// 更新
				_app.updateProgramInfo();
				break;
			}
			case 't':
			{
				// 番組表を表示
				_app.showEPGScreen(_tableView.getRowNumberWithFocus());
				break;
			}
			case Characters.SPACE:
			{
				// 再生、停止
				_app.doPlayStop(_tableView.getRowNumberWithFocus());
				break;
			}
		}
		return super.keyChar(c, status, time);
	}
	
	
	protected void makeMenu(Menu menu, int instance)
	{
		super.makeMenu(menu, instance);
		
		MenuItem _prog = new MenuItem(new StringProvider("Earpiece ON/OFF"), 0x00020000, 0);
		_prog.setCommand(new Command(
				new CommandHandler()
				{
					public void execute(ReadOnlyCommandMetadata metadata, Object context)
					{
						_app.setAudioPath();
					}
				}
				));
		menu.add(_prog);
		
		
		MenuItem _tester = new MenuItem(new StringProvider("更新"), 0x00020000, 1);
		_tester.setCommand(new Command(
				new CommandHandler()
				{
					public void execute(ReadOnlyCommandMetadata metadata, Object context)
					{
						_app.updateProgramInfo();
					}
				}
				));
		menu.add(_tester);
		
	} //makeMenu()
	
	
	/* To override the default functionality that prompts the user to save changes before the application closes, 
	 * override the MainScreen.onSavePrompt() method. In the following code sample, the return value is true which 
	 * indicates that the application does not prompt the user before closing.
	 */ 
	/*protected boolean onSavePrompt()
	{
		return true;
	}*/
	
	
	protected void onUiEngineAttached(boolean attached)
	{
		super.onUiEngineAttached(attached);
		if(attached) {
			_app.runApp();
		} else {
			//updateStatus("Deattached!!");
		}
	} //onUiEngineAttached()
	
	
	protected boolean openProductionBackdoor(int backdoorCode)
	{
		// Use a Backdoor Sequence
		// http://www.blackberryforums.com.au/forums/blackberry-java-development-environment/211-use-backdoor-sequence.html
			
		switch( backdoorCode )
		{
			// BACKDOOR – converts four chars to an int via bit shifting and a bitwise OR
			/*
			case ( 'A' << 24 ) | ( 'B' << 16 ) | ( 'C' << 8 ) | 'D': 
			UiApplication.getUiApplication().invokeLater (new Runnable() {
				public void run() 
				{
					Dialog.inform("Backdoor sequence received");
				}
			}); 
			return true; // handled
			 */
			// ログを表示
			case ( 'L' << 16 ) | ( 'O' << 8 ) | 'G':
				UiApplication.getUiApplication().invokeLater (new Runnable() {
					public void run() 
					{
						if(getField(0).equals(_tableView)) {
							deleteAll();
							add(_tab1Manager);
						} else {
							deleteAll();
							add(_tableView);
						}
					}
				});
			return true;
		} //switch
		return super.openProductionBackdoor(backdoorCode);
	} //openProductionBackdoor()
	
	
	public void addContents(Station[] stations)
	{
		//　引数チェック
		if(stations == null) { throw new NullPointerException("stations"); }
		
		for(int i=0; i<stations.length; i++)
		{
			Station station = stations[i];
			
			// LeftColumn
			VerticalFieldManager vManager_left = new VerticalFieldManager(VerticalFieldManager.FIELD_VCENTER);
			
			BitmapField bitmapField = new BitmapField(station.getLogo(), Field.NON_FOCUSABLE);
			vManager_left.add(bitmapField);
			
			// RightColumn
			VerticalFieldManager vManager_right = new VerticalFieldManager(VerticalFieldManager.FIELD_VCENTER);
			Font sfont = Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt);
			
			// StaionNAME
			LabelField _stationName = new LabelField(station.getName(), Field.NON_FOCUSABLE | LabelField.ELLIPSIS);
			_stationName.setFont(Font.getDefault());
			_stationName.setPadding(10, 10, 0, 10);
			vManager_right.add(_stationName);
			
			// ProgramTIME
			LabelField _progTime = new LabelField("", Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
			{
				protected void paint(Graphics g)
				{
					g.setColor(Color.GRAY);
					super.paint(g);
				}
			};
			_progTime.setFont(sfont);
			_progTime.setPadding(0, 10, 0, 10);
			vManager_right.add(_progTime);
			
			// ProgramNAME
			LabelField _progNAME = new LabelField("", Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
			{
				protected void paint(Graphics g)
				{
					g.setColor(Color.GRAY);
					super.paint(g);
				}
			};        	
			_progNAME.setFont(sfont);
			_progNAME.setPadding(0, 10, 0, 10);
			vManager_right.add(_progNAME);
			
			// ProgramPFM
			LabelField _progPFM = new LabelField("", Field.NON_FOCUSABLE | LabelField.ELLIPSIS)
			{
				protected void paint(Graphics g)
				{
					g.setColor(Color.GRAY);
					super.paint(g);
				}
			};
			_progPFM.setFont(sfont);
			_progPFM.setPadding(0, 10, 10, 10);
			vManager_right.add(_progPFM);
			
			HorizontalFieldManager hManager = new HorizontalFieldManager(Manager.USE_ALL_WIDTH);
			hManager.add(vManager_left);
			hManager.add(vManager_right);
			_tableModel.addRow(new Object[]{hManager});
		}
	}
	
	
	// Overrides: close() in Screen
	public void close()
	{
		if(!isForcedTermination)
		{
			int select = Dialog.ask(Dialog.D_OK_CANCEL, "終了してもよろしいですか?", Dialog.NO);
			
			if(select == Dialog.NO) { return; }
		}
		
		Runnable runnable = _onCloseRunnable;
		if ( runnable != null )
		{
			_onCloseRunnable = null;
			try {
				runnable.run();
			} catch ( Throwable e ) {
			}
		}
		super.close();
	} //close()
	
	
	public void hideActivityIndicator()
	{
		synchronized (UiApplication.getEventLock()) 
		{
			this.setStatus(null);
		}
	} //hideActivityIndicator()
	
	
	public void popupVolumeVal(int val)
	{
		if(_popup.equals(UiApplication.getUiApplication().getActiveScreen())) {
			_timer.cancel();
			_gaugeField.setValue(val);
			_timer  = new Timer();
			_timer.schedule(new CountDown(), 700);
		} else {
			_gaugeField.setValue(val);
			
			UiApplication.getUiApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					UiApplication.getUiApplication().pushScreen(_popup);
				}
			});
			
			_timer  = new Timer();
			_timer.schedule(new CountDown(), 700);
		}
	} //popupVolumeVal()
	
	
	public void setOnCloseRunnable( Runnable runnable )
	{
		_onCloseRunnable = runnable;
	}
	
	
	public void setForcedTermination(boolean val)
	{
		this.isForcedTermination = val;
	}
	
	
	public void showActivityIndicator()
	{
		synchronized (UiApplication.getEventLock()) 
		{
			this.setStatus(_aiView);
		}
	} //showActivityIndicator()
	
	
	public void updateProgramInfo(Station[] stations)
	{
		// 引数チェック
		if(stations == null) { throw new NullPointerException("stations"); }
		
		
		for(int i=0; i<stations.length; i++)
		{
			Station tmp_station = stations[i];
			
			// GET OuterHFM
			Field[] fields = dataTemplate.getDataFields(i);
			
			//
			HorizontalFieldManager outerHFM = (HorizontalFieldManager) fields[0];
			
			// GET RightVFM
			VerticalFieldManager rightVFM = (VerticalFieldManager) outerHFM.getField(1);
			
			// ステーションのプログラム情報を取得
			Program[] tmp_progs = tmp_station.getPrograms();
			
			// 現在のプログラムの情報は、配列の一番始めに保存されている。
			Program tmp_prog = tmp_progs[0];
			
			// SET NewData
			synchronized (UiApplication.getEventLock()) 
			{
				((LabelField)rightVFM.getField(1)).setText(tmp_prog.getTime());
				((LabelField)rightVFM.getField(2)).setText(tmp_prog.getTitle());
				((LabelField)rightVFM.getField(3)).setText(tmp_prog.getPfm());
			}
		}
	} //updateProgramInfo()
	
	
	public void updateStatus(final String message)
	{
		UiApplication.getUiApplication().invokeLater(new Runnable()
		{
			public void run()
			{
				_statusField.setText(_statusField.getText() + "\n" + message);
			}
		});
	} //updateStatus()
	
	
	public void updateStatusField(String val)
	{
		LabelField _tmpField = new LabelField(val, Field.FIELD_HCENTER);
		_tmpField.setFont(Font.getDefault().derive(Font.PLAIN, (Font.getDefault().getHeight(Ui.UNITS_pt) - 1), Ui.UNITS_pt));
		_tmpField.setPadding(10, 0, 0, 0);
		synchronized (UiApplication.getEventLock())
		{
			this.setStatus(_tmpField);
		}
	} //updateStatusField()
	
	
	public void updateTitleBarTitle(String val)
	{
		if(val != null) {
			_titleBar.addTitle(val);
		} else {
			synchronized (UiApplication.getEventLock()) 
			{
				if(_app.isPlaying())
					_titleBar.addTitle(_app.getCurrentStationName());
				else
					_titleBar.addTitle("");
			}
		}
	} //updateTitleBarTitle()
	
	
	private class CountDown extends TimerTask
	{
		public void run()
		{
			UiApplication.getUiApplication().invokeLater(new Runnable() {
				
				public void run() {
					//UiApplication.getUiApplication().popScreen(UiApplication.getUiApplication().getActiveScreen());
					UiApplication.getUiApplication().popScreen(_popup);
					_timer = null;
				}
			});
		}
	} //CountDown
	
	private class ItemProvider implements CommandItemProvider 
	{
		public Object getContext(Field field)
		{
			return field;
		}
		public Vector getItems(Field field)
		{
			//isContextMenuOpened = true;
			
			Vector items = new Vector();
			
			// 再生/停止ボタン
			items.addElement(new CommandItem(new StringProvider("再生/停止"), null, new Command(new CommandHandler() 
			{
				public void execute(ReadOnlyCommandMetadata metadata, Object context) 
				{
					_app.doPlayStop(_tableView.getRowNumberWithFocus());
				}
			})));
			
			// 番組表ボタン
			items.addElement(new CommandItem(new StringProvider("番組表"), null, new Command(new CommandHandler() 
			{
				public void execute(ReadOnlyCommandMetadata metadata, Object context) 
				{
					_app.showEPGScreen(_tableView.getRowNumberWithFocus());
				}
			})));
			
			items.addElement(new CommandItem(new StringProvider("現在の番組"), null, new Command(new CommandHandler() 
			{
				public void execute(ReadOnlyCommandMetadata metadata, Object context) 
				{
					Station tmp_station = _app.getStationByIndex(_tableView.getRowNumberWithFocus());
					Program[] tmp_progs = tmp_station.getPrograms();
					_app.showProgScreen(tmp_progs[0]);
				}
			})));
			
			items.addElement(new CommandItem(new StringProvider("次の番組"), null, new Command(new CommandHandler() 
			{
				public void execute(ReadOnlyCommandMetadata metadata, Object context) 
				{
					Station tmp_station = _app.getStationByIndex(_tableView.getRowNumberWithFocus());
					Program[] tmp_progs = tmp_station.getPrograms();
					_app.showProgScreen(tmp_progs[1]);
				}
			})));
			
			items.addElement(new CommandItem(new StringProvider("更新"), null, new Command(new CommandHandler() 
			{
				public void execute(ReadOnlyCommandMetadata metadata, Object context) 
				{
					_app.updateProgramInfo();
				}
			})));
			
			return items;
		}
	} //ItemProvider
}