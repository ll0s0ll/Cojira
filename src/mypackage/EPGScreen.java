/*
	EPGScreen.java
	
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

import net.rim.device.api.command.Command;
import net.rim.device.api.command.CommandHandler;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
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

public class EPGScreen extends MainScreen
{
	private MyApp _app = null;
	private Program[] programs = null;
	
	private TableModel _tableModel;
	private TableView _tableView;
	private ActivityIndicatorView _aiView = null;
	
	
	public EPGScreen()
	{
		super(Manager.NO_VERTICAL_SCROLL);
		
		_app = (MyApp) UiApplication.getUiApplication();
		
		StandardTitleBar _titleBar = new StandardTitleBar();
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

		TableController _controller = new TableController(_tableModel, _tableView, TableController.ROW_FOCUS);
		_controller.setCommand(new Command(new CommandHandler() 
		{
			
			public void execute(ReadOnlyCommandMetadata metadata, Object context)
			{
				Program prog = programs[_tableView.getRowNumberWithFocus()];
				_app.showProgScreen(prog);
			}
		}));
		_tableView.setController(_controller);
		_controller = null;
		
		
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
		dataTemplate.useFixedHeight(true);
		_tableView.setDataTemplate(dataTemplate);
		dataTemplate = null;
		
		add(_tableView);
		
		
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
		Bitmap aiBitmap = Bitmap.getBitmapResource("spinner2.png");
		
		_aiView.createActivityImageField(aiBitmap, 6, Field.FIELD_HCENTER);
		
		aiModel = null;
		aiController = null;
		aiBitmap = null;
		
	} //EPGScreen()
	
	
	public void addContents(Program[] programs)
	{
		this.programs = programs;
		
		synchronized (UiApplication.getEventLock()) 
		{
			for(int i=0; i<programs.length; i++)
			{
				Program prog = programs[i];
				
				String time = prog.getTime();
				String title = prog.getTitle();
				String pfm = prog.getPfm();
				String desc = prog.getDescription();
				String info = prog.getInfo();
				
				_tableModel.addRow(new Object[]{time, title, pfm, desc + info});
			}
		}
	} //addContents()
	
	
	public void hideActivityIndicator()
	{
		synchronized (UiApplication.getEventLock()) 
		{
			this.setStatus(null);
		}
	} //hideActivityIndicator()
	
	
	public void showActivityIndicator()
	{
		synchronized (UiApplication.getEventLock()) 
		{
			this.setStatus(_aiView);
		}
	} //showActivityIndicator()
	
	
	/*private void updateStatus(final String message)
	{
		synchronized (UiApplication.getEventLock())
		{
			_app.updateStatus("[EPGS] " + message);
		}
	}*/
}