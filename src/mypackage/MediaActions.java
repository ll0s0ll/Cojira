/*
	MediaActions.java
	
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

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

import net.rim.device.api.media.MediaActionHandler;
import net.rim.device.api.media.control.AudioPathControl;
import net.rim.device.api.ui.UiApplication;



public class MediaActions implements MediaActionHandler, PlayerListener
{
	private MyApp _app;
	private RTMP _rtmp;
	private Player _player;	
	private int currentVolume;
	private boolean isOperating;
	
	
	public MediaActions(UiApplication app)
	{
		// 
		_app = (MyApp) app;
		
		// 
        _app.setOnCloseRunnable(new Runnable()
        {
            public void run() { close(); }
        } );
	}
	

	public void Init()
	{	
        // Add Handler		
        _app.addMediaActionHandler(this);
        
        // Set initial volume
        currentVolume = 20;
        
        //
        isOperating = false;
	}

    
	private void close()
    {
    	doStop();
    	
		
    	// Reset AudioPath
		AudioPathControl _audioPathCtrl = (AudioPathControl) _player.getControl("net.rim.device.api.media.control.AudioPathControl");
		try {
			_audioPathCtrl.resetAudioPath();
			//_audioPathCtrl.setAudioPath(net.rim.device.api.media.control.AudioPathControl.AUDIO_PATH_HANDSET);
		} catch (IllegalArgumentException e1) {
		}
		
    	
    	
        UiApplication app = _app;
        _app = null;
        if ( app != null )
        {
            app.removeMediaActionHandler(this);
            app = null;
        }       
    }	

	
	// Invoked MediaActionHandler
 	public boolean mediaAction(int action, int source, Object context)
 	{
 		switch (action) 
         {
             case MEDIA_ACTION_VOLUME_UP:	                	
             {
                 return doVolumeUp();
             }
             case MEDIA_ACTION_VOLUME_DOWN:
             {
                 return doVolumeDown();            	
             }           
             case MEDIA_ACTION_MUTE:
                 //return mediaActions.doMute(true);
             	updateStatus("MEDIA_ACTION_MUTE");
             	break;
             case MEDIA_ACTION_UNMUTE:
                 //return mediaActions.doMute(false);
             	updateStatus("MEDIA_ACTION_UNMUTE");
             	break;
             case MEDIA_ACTION_MUTE_TOGGLE:
                 //return mediaActions.doMute(!isMuted(_volumeController));
             	updateStatus("MEDIA_ACTION_MUTE_TOGGLE");
             	break;
             case MEDIA_ACTION_PLAYPAUSE_TOGGLE:
             {
             	updateStatus("MEDIA_ACTION_PLAYPAUSE_TOGGLE");
             	
                if (isPlaying()) 
                {
                    //actionRunnable = new MediaAction(MEDIA_ACTION_PAUSE, source, context, MediaPlayerDemo.this);               
                	return doStop();
                } 
                else 
                {
                    //actionRunnable = new MediaAction(MEDIA_ACTION_PLAY, source, context, MediaPlayerDemo.this);
                	updateTitleBarTitle("バッファ中...");
                	return doPlay();
                }
             }  
             //case MEDIA_ACTION_CHANGE_TRACK:
             case MEDIA_ACTION_NEXT_TRACK:
            	 break;
             case MEDIA_ACTION_PAUSE:
            	 break;
             case MEDIA_ACTION_PLAY:
             {
            	 updateStatus("MEDIA_ACTION_PLAY");
            	 updateTitleBarTitle("バッファ中...");
            	 return doPlay();
             }
             case MEDIA_ACTION_PREV_TRACK:
            	 break;
             case MEDIA_ACTION_STOP:
             {
            	 updateStatus("MEDIA_ACTION_STOP");	
            	 return doStop();
             }
             default:
                 //actionRunnable = null;
             	updateStatus("default");	                	
                 break;
         }
 			
 		return false;
 	} // mediaAction()
	 	

    // Invoked PlayerListener
	public void playerUpdate(Player player, String event, Object eventData)
	{
		if(player != _player)
			return;

		if( event.equals("error") )
		{
			updateStatus("[PL] " + event);
			doPlay();
		}
			
		if(event.equals("com.rim.externalStop"))
		{
			updateStatus("[PL] " + event);
		}
		
		/*
		if(!event.equals("com.rim.timeUpdate"))
		{
			updateStatus("[PL] " + event);
		}
		*/
	}	
	
	private boolean doPlay() //throws Exception
	{
		updateStatus("doPlay()");		
		
		isOperating = true;
		
		final String station = _app._epg.GetCurrentStationID();
		if(station == null)
			return false;
		
		new Thread()
		{
			public void run()
			{
				try {
					_rtmp = new RTMP(station);
					_player = Manager.createPlayer(_rtmp);
										
					//updateStatus("[Realize]");		
					_player.realize();
					//updateStatus("[Realized]");
					
					AddPlayerListener(_player);
					
					final VolumeControl volumeCtrl = (VolumeControl) _player.getControl("VolumeControl");
					volumeCtrl.setLevel(currentVolume);
					
					//updateStatus("[Start]");
					_player.start();
					//updateStatus("[Started]");					

					// スクリーンのタイトルを更新
					updateTitleBarTitle(null);
										
				} catch (MediaException e) {
					//updateStatus("doPlay() " + e.toString());
					updateTitleBarTitle("再生失敗(M)");
				} catch (IOException e) {
					//updateStatus("doPlay() " + e.toString());
					updateTitleBarTitle("再生失敗(I)");
				} finally {
					isOperating = false;
				}
			}
		}.start();
		
		return true;		
	}
	
	
	private boolean doStop()
	{
		updateStatus("doStop()");
		
		try {
			_rtmp.stop();
			_rtmp.disconnect();
			
			_player.stop();
			_player.deallocate();
			_player.close();
			
			if(_player != null)
				_player = null;
			if(_rtmp != null)
				_rtmp = null;
			

			// スクリーンのタイトルを更新
			updateTitleBarTitle(null);
			
		} catch (MediaException e) {
			updateStatus("doStop() " + e.toString());
			return false;
		} catch (IOException e) {
			updateStatus("doStop() " + e.toString());
			return false;
		}
		
		return true;
	}
	
	private boolean doVolumeUp()
	{
		final VolumeControl volumeCtrl = (VolumeControl) _player.getControl("VolumeControl");
		final int newVolume = Math.min(volumeCtrl.getLevel() + 5, 100);
		
		//updateStatus("doVolumeUp()" + Integer.toString(newVolume));
		
        if (volumeCtrl.getLevel() == newVolume) 
        {
        	currentVolume = newVolume;
            return false;
        }
        volumeCtrl.setLevel(newVolume);
        currentVolume = newVolume;
        
        _app._secondaryScreen2.popupVolumeVal(currentVolume);
        
        return true;
	}
	
	private boolean doVolumeDown()
	{
		final VolumeControl volumeCtrl = (VolumeControl) _player.getControl("VolumeControl");
		final int newVolume = Math.max(volumeCtrl.getLevel() - 5, 00);
		
		//updateStatus("doVolumeUp()" + Integer.toString(newVolume));
						        
        if (volumeCtrl.getLevel() == newVolume) 
        {
        	currentVolume = newVolume;
            return false;
        }
        
        volumeCtrl.setLevel(newVolume);
        currentVolume = newVolume;
        
        
        _app._secondaryScreen2.popupVolumeVal(currentVolume);
        
        return true;
	}
	
	private void AddPlayerListener(Player _player)
	{
		_player.addPlayerListener(this);
	}
	
	private void updateTitleBarTitle(String val)
	{
		_app._secondaryScreen2.updateTitleBarTitle(val);
	}
	
	public boolean isOperating() 
    {
    	return isOperating;
    }
	
    public boolean isPlaying() 
    {
    	return _player != null && _player.getState() == Player.STARTED;
    }
	
    
    public void SetAudioPath()
    {
    	AudioPathControl _audioPathCtrl = (AudioPathControl) _player.getControl("net.rim.device.api.media.control.AudioPathControl");
    	
		if(_audioPathCtrl == null)
			return;
    	
    	try	{
    		
	    	if(_audioPathCtrl.getAudioPath() != net.rim.device.api.media.control.AudioPathControl.AUDIO_PATH_HANDSET)
	    	{    		    		
				_audioPathCtrl.setAudioPath(net.rim.device.api.media.control.AudioPathControl.AUDIO_PATH_HANDSET);			
	    	}
	    	else
	    	{    	
	    		_audioPathCtrl.setAudioPath(net.rim.device.api.media.control.AudioPathControl.AUDIO_PATH_HANDSFREE);
	    	}
    	} catch (IllegalArgumentException e) {
		} catch (MediaException e) {
		}
		
    }
    
	public void updateStatus(String val)
	{
		synchronized (UiApplication.getEventLock()) 
		{
			_app.updateStatus("[MA] " + val);
		}
	}
}