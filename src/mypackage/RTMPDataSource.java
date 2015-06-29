/*
	RTMPDataSource.java
	
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

import javax.microedition.media.Control;
import javax.microedition.media.protocol.ContentDescriptor;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.protocol.SourceStream;

import net.rim.device.api.io.transport.ConnectionFactory;


public class RTMPDataSource extends DataSource
{
	private MyApp _app = null;
	private RTMP _rtmp = null;
	private SourceStream _sourceStream = null;
	
	private ConnectionFactory _connfactory = null;
	private final String authToken;
	private final String domain;
	
	
	public RTMPDataSource(MyApp _app, ConnectionFactory _connfactory, String domain, String locator, String authToken)
	{
		super(locator);
		this._app = _app;
		this._connfactory = _connfactory;
		this.domain = domain;
		this.authToken = authToken;
	}
	
	
	public Control getControl(String controlType)
	{
		return null;
	}

	public Control[] getControls()
	{
		return null;
	}

	// Overrides: connect() in DataSource
	public void connect() throws IOException
	{
		_rtmp = new RTMP(_app);
		_sourceStream = new RTMPSourceStream(_rtmp, "audio/aac");
		_rtmp.doConnect(_connfactory, "socket://" + domain + ":1935");
	} //connect()
	
	// Overrides: disconnect() in DataSource
	public void disconnect()
	{
		_rtmp.doDisconnect();
		
		if(_sourceStream != null) { _sourceStream = null; }
		if(_rtmp != null) { _rtmp = null; }
	}

	public String getContentType()
	{
		return _sourceStream.getContentDescriptor().getContentType();
	}

	public SourceStream[] getStreams()
	{
		return new SourceStream[] { _sourceStream };
	}

	// Overrides: start() in DataSource
	public void start() throws IOException
	{
		_rtmp.doStart(getLocator(), authToken);
	}

	// Overrides: start() in DataSource
	public void stop() throws IOException
	{
		_rtmp.doStop();
	}
	
	
	private class RTMPSourceStream implements SourceStream
	{
		private RTMP _rtmp = null;
		private ContentDescriptor _contentDescriptor = null;
		private byte[] audioPacket = null;
		private int remain = 0;
		
		public RTMPSourceStream(RTMP _rtmp, String contentType)
		{
			this._rtmp = _rtmp;
			this._contentDescriptor = new ContentDescriptor(contentType);
			this.remain = 0;
		}

		public Control getControl(String controlType)
		{
			return null;
		}

		public Control[] getControls()
		{
			return null;
		}

		public ContentDescriptor getContentDescriptor()
		{
			return _contentDescriptor;
		}

		public long getContentLength()
		{
			// the length is not known
			return -1;
		}

		public int getSeekType()
		{
			return NOT_SEEKABLE;
		}

		public int getTransferSize()
		{
			return 512;
		}
		
		public int read(byte[] b, int off, int len) throws IOException
		{
			if(remain == 0) {
				//== 読み込み残しがない場合 ==//
				
				// Audioデータを読み込み
				audioPacket = null;
				audioPacket = _rtmp.doReadAudioPackets();
				
				// Audioデータのオフセットを設定。読み残しはないので'0'に設定
				int offset_audioPacket = 0;
				
				// Audioデータが、Playerから指定された長さより大きい場合は、残ったバイト数を記憶する
				int num_AudioPacket = audioPacket.length;
				if(num_AudioPacket > len) {
					remain = num_AudioPacket - len;
				} else {
					remain = 0;
					len = num_AudioPacket;
				}
				
				// Audioデータをコピーする
				System.arraycopy(audioPacket, offset_audioPacket, b, off, len);
				
				return len;
				
			} else {
				//== 読み込み残しがある場合 ==//
				
				// Audioデータのオフセットを設定
				int offset_audioPacket = audioPacket.length - remain;
				
				if(remain >= len) {
					remain -= len;
				} else { 
					len = remain;
					remain = 0;
				}
				
				// Audioデータをコピーする
				System.arraycopy(audioPacket, offset_audioPacket, b, off, len);
				
				return len;
			}
		} //read()
		
		public long seek(long where) throws IOException
		{
			return 0;
		}

		public long tell()
		{
			return 0;
		}
		
	} //rtmpSourceStream
} //rtmpDataSource