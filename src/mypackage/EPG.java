/*
	EPG.java
	
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

import java.io.DataInputStream;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import javax.microedition.io.HttpConnection;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.xml.jaxp.SAXParserImpl;


public final class EPG
{
	//private MyApp _app;
	private static final int NUM_OF_TRIALS = 3;
	
	
	public EPG(UiApplication app)
	{
		// DO NOTHING
		//_app = (MyApp) app;
	}
	
	
	public static Hashtable getCurrentPrograms(ConnectionFactory _connfactory, final String areaID) throws Exception
	{
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException("ConnectionFactory"); }
		if(areaID == null) { throw new NullPointerException("areaID"); }
		if(areaID.length() == 0) { throw new IllegalArgumentException("areaID"); }
		
		final String url = "http://radiko.jp//v2/api/program/now?area_id=" + areaID;
		
		return getProgramInfo(_connfactory, url);
	}
	
	
	private static Hashtable getProgramInfo(ConnectionFactory _connfactory, final String url) throws Exception
	{
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException("ConnectionFactory"); }
		if(url == null) { throw new NullPointerException("url"); }
		if(url.length() == 0) { throw new IllegalArgumentException("url"); }
		
		// 有効な通信経路があるか確認
		if(!Network.isCoverageSufficient()) { throw new Exception("OutOfCoverage"); }
		
		String errorlog = "EPG::getProgramInfo()\n";
		
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			ProgramParserHandler _handler = new ProgramParserHandler();
			SAXParserImpl saxparser = new SAXParserImpl();
			
			HttpConnection httpconn = null;
			DataInputStream dis = null;
			try {
				httpconn = Network.doGet(_connfactory, url);
				dis = httpconn.openDataInputStream();
				saxparser.parse(dis, _handler);
				
				return _handler.getResult();
			
			} catch (Exception e) {
				errorlog += e.toString() + "\n";
			} finally {
				if(dis != null){ dis.close(); dis = null; }
				if(httpconn != null){ httpconn.close(); httpconn = null; }
			}
		}
		throw new Exception(errorlog);
	} //getProgramInfo()
	
	
	public static Hashtable getStationListAndAreaName(ConnectionFactory _connfactory, final String areaID) throws Exception
	{
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException("ConnectionFactory"); }
		if(areaID == null) { throw new NullPointerException("areaID"); }
		if(areaID.length() == 0) { throw new IllegalArgumentException("areaID"); }
		
		// 有効な通信経路があるか確認
		if(!Network.isCoverageSufficient()) { throw new Exception("OutOfCoverage"); }
				
		String errorlog = "EPG::getStationListAndAreaName()\n";
		
		for(int i=0; i<NUM_OF_TRIALS; i++)
		{
			HttpConnection httpconn = null;
			DataInputStream dis = null;
			
			try {
				final String url = "http://radiko.jp/v2/station/list/" + areaID + ".xml";
				
				StationParserHandler _station = new StationParserHandler();
				SAXParserImpl saxparser = new SAXParserImpl();
				
				//
				httpconn = Network.doGet(_connfactory, url);
				
				dis = httpconn.openDataInputStream();
				saxparser.parse(dis, _station);
				
				Hashtable out= new Hashtable();
				out.put("stationList", _station.getStationList());
				out.put("areaName", _station.getAreaName());
				
				return out;
				
			} catch (Exception e) {
				errorlog += e.toString() + "\n";
			} finally {
				if(dis != null){ dis.close(); dis = null;}
				if(httpconn != null){ httpconn.close(); httpconn = null;}
			}
		}
		throw new Exception(errorlog);
	} //getStationListAndAreaName()
	
	
	public static Program[] getTimetable(ConnectionFactory _connfactory, final String areaID, final String stationID) throws Exception
	{
		// 引数チェック
		if(_connfactory == null) { throw new NullPointerException("ConnectionFactory"); }
		if(areaID == null) { throw new NullPointerException("areaID"); }
		if(areaID.length() == 0) { throw new IllegalArgumentException("areaID"); }
		if(stationID == null) { throw new NullPointerException("stationID"); }
		if(stationID.length() == 0) { throw new IllegalArgumentException("stationID"); }
		
		final String url = "http://radiko.jp/v2/api/program/today?area_id=" + areaID;
		
		// 番組情報を取得
		Hashtable tmp = getProgramInfo(_connfactory, url);
		
		// stationIDで指定された放送局の番組情報を取得
		Program[] out = (Program[])tmp.get(stationID);
		
		if(out == null) { return null; }
		
		return out;
	} //getTimetable()
	
	
	/*private void updateStatus(String val)
	{
		synchronized (UiApplication.getEventLock())
		{
			_app.updateStatus("[EPG] " + val);
		}
	}*/
} //EPG	
	
class Station
{
	private Program[] programs = null;
	private Program[] timetable = null;
	private String ascii_name = "";
	private String id = "";
	private String logo_url = "";
	private String name = "";
	private Bitmap logo = null;
	
	
	public Program[] getPrograms() { return this.programs; }
	public Program[] getTimetable() { return this.timetable; }
	public String getASCIIName() { return this.ascii_name; }
	public String getID() { return this.id; }
	public Bitmap getLogo() { return this.logo; }
	public String getLogoURL() { return this.logo_url; }
	public String getName() { return this.name; }
	
	
	public void setPrograms(Program[] programs)
	{
		// 引数チェック
		if(programs == null) { throw new NullPointerException("programs"); }
		if(programs.length == 0) { throw new IllegalArgumentException("programs"); }
		
		this.programs = programs;
	} //setPrograms()
	
	
	public void setTimetable(Program[] timetable)
	{
		// 引数チェック
		if(timetable == null) { throw new NullPointerException("timetable"); }
		if(timetable.length == 0) { throw new IllegalArgumentException("timetable"); }
		
		this.timetable = timetable;
	} //setPrograms()
	
	
	public void setASCIIName(String ascii_name)
	{
		// 引数チェック
		if(ascii_name == null) { throw new NullPointerException("ascii_name"); }
		if(ascii_name.length() == 0) { throw new IllegalArgumentException("ascii_name"); }
		
		this.ascii_name = ascii_name;
	} //setASCIIName()
	
	
	public void setID(String id)
	{
		// 引数チェック
		if(id == null) { throw new NullPointerException("id"); }
		if(id.length() == 0) { throw new IllegalArgumentException("id"); }
		
		this.id = id;
	} //setID()
	
	
	public void setLogo(Bitmap logo)
	{
		// 引数チェック
		if(logo == null) { throw new NullPointerException("logo"); }
		
		this.logo = logo;
	} //setLogoURL()
	
	
	public void setLogoURL(String logo_url)
	{
		// 引数チェック
		if(logo_url == null) { throw new NullPointerException("logo_url"); }
		if(logo_url.length() == 0) { throw new IllegalArgumentException("logo_url"); }
		
		this.logo_url = logo_url;
	} //setLogoURL()
	
	
	public void setName(String name)
	{
		// 引数チェック
		if(name == null) { throw new NullPointerException("name"); }
		if(name.length() == 0) { throw new IllegalArgumentException("name"); }
		
		this.name = name;
	} //setName()
}


class Program
{
	private String description = "";
	private String info = "";
	private String pfm = "";
	private String time = "";
	private String title = "";
	private String url = "";
	
	
	public String getDescription() { return this.description; }
	public String getInfo() { return this.info; }
	public String getPfm() { return this.pfm; }
	public String getTime() { return this.time; }
	public String getTitle() { return this.title; }
	public String getUrl() { return this.url; }
	
	
	public void setDescription(String description)
	{
		// 引数チェック
		if(description == null) { throw new NullPointerException("description"); }
		
		this.description = description;
	} //setDescription()
	
	
	public void setInfo(String info)
	{
		// 引数チェック
		if(info == null) { throw new NullPointerException("info"); }
		
		this.info = info;
	} //setInfo()
	
	
	public void setPfm(String pfm)
	{
		// 引数チェック
		if(pfm == null) { throw new NullPointerException("pfm"); }
		
		this.pfm = pfm;
	} //setPfm()
	
	
	public void setTime(String time)
	{
		// 引数チェック
		if(time == null) { throw new NullPointerException("time"); }
		
		this.time = time;
	} //setTime()
	
	
	public void setTitle(String title)
	{
		// 引数チェック
		if(title == null) { throw new NullPointerException("title"); }
		
		this.title = title;
	} //setTitle()
	
	
	public void setUrl(String url)
	{
		// 引数チェック
		if(url == null) { throw new NullPointerException("url"); }
		
		this.url = url;
	} //setUrl()
	
}


class StationParserHandler extends DefaultHandler
{
	/*
	 * <stations area_id="JP13" area_name="TOKYO JAPAN">
	 *  <station>
	 *   <id>TBS</id>
	 *   <name>TBSラジオ</name>
	 *   <ascii_name>TBS RADIO</ascii_name>
	 *   <href>http://www.tbs.co.jp/radio/</href>
	 *   <logo_xsmall>http://radiko.jp/station/logo/TBS/logo_xsmall.png</logo_xsmall>
	 *   <logo_small>http://radiko.jp/station/logo/TBS/logo_small.png</logo_small>
	 *   <logo_medium>http://radiko.jp/station/logo/TBS/logo_medium.png</logo_medium>
	 *   <logo_large>http://radiko.jp/station/logo/TBS/logo_large.png</logo_large>
	 *   <logo width="124" height="40">http://radiko.jp/v2/static/station/logo/TBS/124x40.png</logo>
	 *   <logo width="344" height="80">http://radiko.jp/v2/static/station/logo/TBS/344x80.png</logo>
	 *   <logo width="688" height="160">http://radiko.jp/v2/static/station/logo/TBS/688x160.png</logo>
	 *   <logo width="172" height="40">http://radiko.jp/v2/static/station/logo/TBS/172x40.png</logo>
	 *   <logo width="224" height="100">http://radiko.jp/v2/static/station/logo/TBS/224x100.png</logo>
	 *   <logo width="448" height="200">http://radiko.jp/v2/static/station/logo/TBS/448x200.png</logo>
	 *   <logo width="112" height="50">http://radiko.jp/v2/static/station/logo/TBS/112x50.png</logo>
	 *   <logo width="168" height="75">http://radiko.jp/v2/static/station/logo/TBS/168x75.png</logo>
	 *   <logo width="258" height="60">http://radiko.jp/v2/static/station/logo/TBS/258x60.png</logo>
	 *   <feed>http://radiko.jp/station/feed/TBS.xml</feed>
	 *   <banner>http://radiko.jp/res/banner/TBS/20130329155819.jpg</banner>
	 *  </station>
	 *  ...
	 * </stations>
	 * 
	 */
	
	
	private Station _station = null;
	private Vector _stations = new Vector();
	private String areaName = "";
	private Stack tags = new Stack();
	
	
	public StationParserHandler()
	{
		// DO NOTHING
	}
	
	
	public void startElement(String uri, String localName, String qname, Attributes attributes) throws SAXException
	{
		// タグ名を記憶
		tags.push(qname);
		
		// 現在のエリアネームを取得
		if(qname.equals("stations"))
		{
			this.areaName = attributes.getValue("area_name");
		}
		
		// 放送局情報を取得開始
		if(qname.equals("station"))
		{
			_station = new Station();
		}
		
	} //startElement()
	
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if(qName.equals("station"))
		{
			_stations.addElement(_station);
			_station = null;
		}
		
		// タグ名を忘れる。
		tags.pop();
	} //endElement()
	
	public void characters(char[] ch, int start, int length) throws SAXException 
	{
		String tag = (String)tags.peek();
		
		if(tag.equals("id"))
		{
			String id = new String(ch, start, length);
			_station.setID(id);
		}
		
		
		if(tag.equals("name"))
		{
			String name = new String(ch, start, length);
			_station.setName(name);
		}
		
		
		if(tag.equals("ascii_name"))
		{
			String ascii_name = new String(ch, start, length);
			_station.setASCIIName(ascii_name);
		}
		
		
		if(tag.equals("logo_medium"))
		{
			String logo_medium = new String(ch, start, length);
			_station.setLogoURL(logo_medium);
		}
	} //characters()
	
	
	public String getAreaName()
	{
		return this.areaName;
	}
	
	
	public Station[] getStationList()
	{
		Station[] out = new Station[_stations.size()];
		
		for(int i=0; i<_stations.size(); i++)
		{
			out[i] = (Station)_stations.elementAt(i);
		}
		
		return out;
	} //getStationList()
	
} //StationParserHandler


class ProgramParserHandler extends DefaultHandler
{
	private Vector _programs = null;
	private Program _program = null;
	private String processing_station_ID = "";
	private Hashtable out = new Hashtable();
	private Stack tags = new Stack();
	
	
	public ProgramParserHandler()
	{
		// DO NOTHING 
	}
	
	
	public void startElement(String uri, String localName, String qname, Attributes attributes) throws SAXException
	{
		tags.push(qname);
	
		if(qname.equals("station"))
		{
			// ステーションのプログラム情報を保存するベクターを作成
			_programs = new Vector();
			
			// 処理するステーションのIDを保存
			processing_station_ID = attributes.getValue("id");
		}
	
		if(qname.equals("prog"))
		{
			StringBuffer ftl = new StringBuffer(attributes.getValue("ftl"));
			ftl.insert(2, ":");
			StringBuffer tol = new StringBuffer(attributes.getValue("tol"));
			tol.insert(2, ":");
			
			
			// プログラム情報を保存するクラスを作成
			_program = new Program();
			
			// 放送時間を保存
			_program.setTime(ftl.toString() + " - " + tol.toString());
		}
	} //startElement()

	
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if(qName.equals("prog"))
		{
			//updateStatus("endElement() " + qName);
			
			// 取得したプログラム情報を一時保存
			_programs.addElement(_program);
			_program = null;
		}
	
		if(qName.equals("station"))
		{
			//updateStatus("endElement() " + qName);
			
			// ベクターから配列に変換
			Program[] out_val = new Program[_programs.size()];
			for(int i=0; i<_programs.size(); i++)
			{
				out_val[i] = (Program)_programs.elementAt(i);
			}
			_programs = null;
			
			//
			out.put(processing_station_ID, out_val);
		}
	
		tags.pop();
	} //endElement()

	public void characters(char[] ch, int start, int length) throws SAXException 
	{
		String tag = (String)tags.peek();
		
		if(tag.equals("title"))
		{
			String element = new String(ch, start, length);
			_program.setTitle(element);
		}
	
		if(tag.equals("pfm"))
		{
			String element = new String(ch, start, length);
			_program.setPfm(element);
		}
		
		if(tag.equals("desc"))
		{
			String element = new String(ch, start, length);
			_program.setDescription(element);
			
		}
		
		if(tag.equals("info"))
		{
			String element = new String(ch, start, length);
			_program.setInfo(element);
		}
		
		if(tag.equals("url"))
		{
			String element = new String(ch, start, length);
			_program.setUrl(element);
		}
		
	} //characters()
	
	
	public Hashtable getResult()
	{
		return out;
	}
 } //ProgramParserHandler
