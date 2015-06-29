/*
	MyApp.java
	
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
import java.util.Hashtable;

import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.media.MediaActionHandler;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.TransitionContext;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.UiEngineInstance;
import net.rim.device.api.ui.component.Dialog;

/**
 * This class extends the UiApplication class, providing a
 * graphical user interface.
 */
public class MyApp extends UiApplication
{
	//private static final String domain = "w-radiko.smartstream.ne.jp"; //2014/09/12�܂�
	private static final String domain = "f-radiko.smartstream.ne.jp"; //2014/9/13����
	
	private MyScreen _screen;
	private MediaActions _mediaActions;
	private Runnable _onCloseRunnable;
	private ConnectionFactory _connectionFactory;
	private String authToken = "";
	private String areaID = "";
	private Station[] stations = null;
	private int currentStation = 0;
	
	
	
	/**
	 * Entry point for application
	 * @param args Command line arguments (not used)
	 */ 
	public static void main(String[] args)
	{
		// Create a new instance of the application and make the currently
		// running thread the application's event dispatch thread.
		MyApp theApp = new MyApp();
		theApp.enterEventDispatcher();
	}
	
	
	/**
	 * Creates a new MyApp object
	 */
	public MyApp()
	{
		_mediaActions = new MediaActions(this);
		
		// Push a screen onto the UI stack for rendering.
		_screen = new MyScreen();
		_screen.setOnCloseRunnable(new Runnable()
		{
			public void run(){ close(); }
		});
		pushScreen(_screen);
		
	} //MyApp()
	
	
	public void runApp()
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				for(;;)
				{
					try {
						// �ʐM�o�H��I������
						_screen.updateStatusField("�g�����X�|�[�g��I��...");
						_connectionFactory = Network.selectTransport();
						
						// authToken�AareaID���擾
						Hashtable authToken_and_areaID = getAuthTokenAndAreaID(_connectionFactory, _screen);
						authToken = (String)authToken_and_areaID.get("authToken");
						areaID = (String)authToken_and_areaID.get("areaID");
						
						// �����ǂ̃��X�g���擾
						stations = getAvailableStationList(_connectionFactory, _screen, areaID);
						
						// ��ʂɕ����ǂ̃��X�g��\������
						_screen.addContents(stations);
						
						// �ԑg�����X�V����
						updateProgramInfo();
						
						break;
						
					} catch (Exception e) {
						
						updateStatus("MyApp::runApp() \n" + e.toString());
						
						// �_�C�A���O�ɕ\������G���[���b�Z�[�W���쐬
						final String message;
						if(e.toString().endsWith("OutOfCoverage")) {
							message = "�L���ȒʐM�o�H������܂���B�l�b�g���[�N�ɐڑ����Ă��������B";
						} else {
							message = "�G���A����A�F�؂Ɏ��s���܂����B";
						}
						
						// �m�F�_�C�A���O��\��
						UiApplication.getUiApplication().invokeAndWait(new Runnable()
						{
							public void run()
							{
								int select = Dialog.ask(message, new Object[]{"�Ď��s", "�I��"}, 0);
								// �I���A�L�����Z���̏ꍇ�̓A�v�������I��
								if(select == 1 || select == Dialog.CANCEL)
								{
									_screen.setForcedTermination(true);
									close();
								}
							}
						});
						
						// �Ď��s�̍ۂ̓N���[���A�b�v����
						if(_connectionFactory != null) { _connectionFactory = null; }
						authToken = "";
						areaID = "";
						if(stations != null) { stations = null; }
					} //try
				} //for
			} //run()
		}).start();
	} //runApp()
	
	
	public void setOnCloseRunnable( Runnable runnable )
	{
		_onCloseRunnable = runnable;
	}
	
	public void close()
	{
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
		
		if(_mediaActions != null){ _mediaActions = null; }
		if(_connectionFactory != null){ _connectionFactory = null; }
		if(stations != null){ stations = null; }
		if(_screen != null){ _screen = null; }
	} //close()
	
	
	public void doPlayStop(final int index)
	{
		// �����`�F�b�N
		if(index >= getStationList().length) { throw new IndexOutOfBoundsException("index"); }
		
		// �I�y���[�V�������̏ꍇ�̓��^�[��
		if(_mediaActions.isOperating())
		{
			Dialog.alert("�o�b�t�@��...");
			return;
		}
		
		// ���ݑI������Ă�������ǂ��擾
		final int current_station = getCurrentStation();
		
		// �I�����ꂽ�����ǂ��A���݂̕����ǂɓo�^
		currentStation = index;
		
		// �Đ��A��~�����s
		if(_mediaActions.isPlaying()) {
			// �I�����������ǂƁA�Đ����̕����ǂ��r���Ď��s������e�����߂�
			if(index != current_station) {
				_mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_STOP, MediaActionHandler.SOURCE_FOREGROUND_KEY, null);
				_mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_PLAY, MediaActionHandler.SOURCE_FOREGROUND_KEY, null);
			} else {
				_mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_STOP, MediaActionHandler.SOURCE_FOREGROUND_KEY, null);
			}
		} else {
			_mediaActions.mediaAction(MediaActionHandler.MEDIA_ACTION_PLAY, MediaActionHandler.SOURCE_FOREGROUND_KEY, null);
		}
	} //doPlayStop()
	
	
	public String getAuthToken() { return authToken; }
	public String getAreaID() { return areaID; }
	public ConnectionFactory getConnectionFactory() { return _connectionFactory; }
	public int getCurrentStation() { return currentStation; }
	public String getCurrentStationID() { return stations[currentStation].getID(); }
	public String getCurrentStationName() { return stations[currentStation].getName(); }
	public String getDomain() { return domain; }
	public MyScreen getMainScreen() { return _screen; }
	
	
	public Station getStationByIndex(final int index)
	{
		if(index >= stations.length) { throw new IndexOutOfBoundsException("index"); }
		return stations[index];
	}
	
	
	public Station[] getStationList()
	{
		return this.stations;
	}
	
	
	public boolean isPlaying()
	{
		if(_mediaActions == null) { throw new NullPointerException("MediaActions"); }
		return _mediaActions.isPlaying();
	}
	
	
	public void setAudioPath()
	{
		if(_mediaActions == null) { throw new NullPointerException("MediaActions"); }
		_mediaActions.setAudioPath();
	}
	
	
	public void showEPGScreen(final int index)
	{
		final ConnectionFactory _connfactory = getConnectionFactory();
		final MyScreen _screen = getMainScreen();
		final Station[] stations = getStationList();
		final String areaID = getAreaID();
		final String stationID = stations[index].getID();
		
		// �����`�F�b�N
		if(index >= stations.length) { throw new IndexOutOfBoundsException("indexOfStation"); }
				
		new Thread(new Runnable()
		{
			public void run()
			{
				//---- For EPGScreen ----//
			 	EPGScreen _epgScreen = new EPGScreen();
				
				// FADE IN
				TransitionContext transIN = new TransitionContext(TransitionContext.TRANSITION_FADE);
				transIN.setIntAttribute(TransitionContext.ATTR_DURATION, 100);
				
				UiEngineInstance engine = Ui.getUiEngineInstance();
				engine.setTransition(null, _epgScreen, UiEngineInstance.TRIGGER_PUSH, transIN);
				
				// FADE OUT
				TransitionContext transOUT = new TransitionContext(TransitionContext.TRANSITION_FADE);
				transOUT.setIntAttribute(TransitionContext.ATTR_DURATION, 100);
				transOUT.setIntAttribute(TransitionContext.ATTR_KIND, TransitionContext.KIND_OUT);
				
				engine.setTransition(_epgScreen, null, UiEngineInstance.TRIGGER_POP, transOUT);
				
				synchronized (UiApplication.getEventLock())
				{
					pushScreen(_epgScreen);
				}
				
				try {
					_epgScreen.showActivityIndicator();
					
					Program[] timetable = EPG.getTimetable(_connfactory, areaID, stationID);
					
					_epgScreen.addContents(timetable);
					
					timetable = null;
					
				} catch (Exception e) {
					popScreen(_epgScreen);
					_screen.updateStatusField("�ԑg���̍X�V�Ɏ��s���܂����B");
					updateStatus(e.toString());
				} finally {
					_epgScreen.hideActivityIndicator();
					_epgScreen = null;
				}
			} //run()
		}).start();
	} //showEPGScreen()
	
	
	public void showProgScreen(Program prog)
	{
		//---- For ProgInfoScreen ----//
		ProgInfoScreen _progInfoScreen = new ProgInfoScreen();
		
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
		
		pushScreen(_progInfoScreen);
		
		try {
			_progInfoScreen.addContents(prog);
		} catch (UnsupportedEncodingException e) {
			popScreen(_progInfoScreen);
			_screen.updateStatusField("�ԑg���̕\���Ɏ��s���܂����B");
			updateStatus(e.toString());
		} finally {
			_progInfoScreen = null;
		}
	} //showProgScreen()
	
	
	public void updateProgramInfo()
	{
		final ConnectionFactory _connfactory = getConnectionFactory();
		final MyScreen _screen = getMainScreen();
		final Station[] stations = getStationList();
		final String areaID = getAreaID();
		
		// �����`�F�b�N
		if(_connfactory == null) { throw new NullPointerException("ConnectionFactory"); }
		if(_screen == null) { throw new NullPointerException("MyScreen"); }
		if(stations == null) { throw new NullPointerException("Stations"); }
		if(areaID == null) { throw new NullPointerException("areaID"); }
		if(areaID.length() == 0) { throw new IllegalArgumentException("areaID"); }
		
		
		new Thread(new Runnable()
		{
			public void run()
			{
				try {
					//updateStatus("PGInfoThread Start");
					
					// �A�N�e�B�u�C���W�P�[�^�[��\��
					_screen.showActivityIndicator();
					
					// �v���O���������擾
					Hashtable tmp = EPG.getCurrentPrograms(_connfactory, areaID);
					
					// ���ꂼ��̃X�e�[�V�����N���X�ɁA�擾�����v���O����������������
					for(int j=0; j<stations.length; j++)
					{
						Station tmp_station = stations[j];
						
						Program[] progs = (Program[])tmp.get(tmp_station.getID());
						
						if(progs == null) { continue; }
						
						tmp_station.setPrograms(progs);
					}
					
					// �\�����X�V
					_screen.updateProgramInfo(stations);
				}
				catch (Exception e)
				{
					_screen.updateStatusField("�ԑg���̍X�V�Ɏ��s���܂����B");
					updateStatus(e.toString());
				} finally {
					// CLEANUP Status
					_screen.hideActivityIndicator();
				}
			} //run()
		}).start();
	} //updateProgramInfo()
	
	
	private Hashtable getAuthTokenAndAreaID(ConnectionFactory _connfactory, final MyScreen _screen) throws Exception
	{
		// �����`�F�b�N
		if(_connfactory == null) { throw new NullPointerException("ConnectionFactory"); }
		if(_screen == null) { throw new NullPointerException("MyScreen"); }
		
		_screen.updateStatusField("�G���A���� & �F�ؒ�...");
		
		// Get AuthToken, Keylength, Keyoffset
		Hashtable tmp = Auth.getAuthtokenAndKeylengthAndKeyoffset(_connfactory, _screen);
		String authToken = ((String)tmp.get("authToken"));
		int keylength = Integer.parseInt((String)tmp.get("keylength"));
		int keyoffset = Integer.parseInt((String)tmp.get("keyoffset"));
		tmp.clear();
		tmp = null;
		
		// Get AreaID
		String areaID = Auth.getAreaID(_connfactory, _screen, authToken, keylength, keyoffset);
		
		Hashtable out = new Hashtable();
		out.put("authToken", authToken);
		out.put("areaID", areaID);
		return out;
	} //getAuthTokenAndAreaID()
	
	
	private Station[] getAvailableStationList(ConnectionFactory _connfactory, final MyScreen _screen, final String areaID) throws Exception
	{
		// �����`�F�b�N
		if(_connfactory == null) { throw new NullPointerException("ConnectionFactory"); }
		if(_screen == null) { throw new NullPointerException("MyScreen"); }
		if(areaID == null) { throw new NullPointerException("areaID"); }
		if(areaID.length() == 0) { throw new IllegalArgumentException("areaID"); }
		
		
		_screen.updateStatusField("�����ǂ��擾��...");
		
		Hashtable tmp = EPG.getStationListAndAreaName(_connfactory, areaID);
		
		Station[] stations = (Station[])tmp.get("stationList");
		//String areaname = (String) tmp.get("areaName");
		
		for(int i=0; i<stations.length; i++)
		{
			Station _station = stations[i];
			Bitmap bitmap = Network.getWebBitmap(_connfactory, _station.getLogoURL());
			_station.setLogo(bitmap);
		}
		
		return stations;
		
	} //getAvailableStationList()
	
	
	private void updateStatus(String val)
	{
		_screen.updateStatus(val);
	}
} //MyApp
