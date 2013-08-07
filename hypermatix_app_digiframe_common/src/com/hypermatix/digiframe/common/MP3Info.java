/*
 *  @author : Brendan Whelan
 *  
 *  Copyright (c) 2011-2013 Brendan Whelan <brendanwhelan.net>
 *
 *  This application is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 */

package com.hypermatix.digiframe.common;

import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import android.graphics.Bitmap;

/*
 * Class to read MPS ID Tag info from an MP3 File, or network stream
 */
public final class MP3Info{
    
    private static final String ERR_NO_STREAM       = "MP3 Stream not set";
    
    private static final int INITIAL_READ_SIZE = 512;
   
    public static final int MPEG1     = 1;
    public static final int MPEG2     = 2;
    public static final int MPEG2_5   = 3;
    
    public static final int LAYER1     = 1;
    public static final int LAYER2     = 2;
    public static final int LAYER3     = 3;
    
    protected boolean _hasID3               = false;
    protected boolean _hasID3v2             = false;
    protected String _title                 = "";               //Title from MP3 file data
    protected String _artist                = "";               //Artist from MP3 file data
    protected String _album            		= "";
    protected String _description           = "";               //Comment from MP3 file data
    protected long _duration                = -1;               //MP3 ile duration in microseconds
    protected long _size                    = 0;                //MP3 data size bytes
    protected int _mpegVersion              = 0;
    protected int _mpegLayer                = 0;
    protected short _br                     = 0;                //bit rate (kb/s)
    protected int _sf                       = 0;                //sampling freq (kHz)
    protected short _padded                 = 0;                
    protected boolean _hasPicture           = false;            //Flag to indicate whether MP3 has Album Art
    protected Bitmap _picture         = null;                   //Album Art image from the MP3 file
  
    protected URL oUrl;
    protected InputStream dataStream;
    protected URLConnection conn;
    protected long loc = 0; //Stream byte location
 
    protected MP3Info() {
    }
  
    public void setTitle(String Title){
        _title = Title;
    }
    
    public void setArtist(String Artist){
        _artist = Artist;
    }

    public void setAlbum(String Album){
        _album = Album;
    }
    
    public static MP3Info FromFile(){
    	return null;
    }
    
    public static MP3Info FromURL(String url){
    	MP3Info info = new MP3Info();
    	
    	info.LoadFromURL(url);
    	
    	return info;
    }
    
    private void LoadFromURL(String url){
    	try{
        	
        	oUrl = new URL(url);
        	
        	InitStream(INITIAL_READ_SIZE,false);
            
            LoadFromStream();
        	
            dataStream.close();
            if(conn instanceof HttpURLConnection){
            	((HttpURLConnection)conn).disconnect();
            }
            
            //conn.setRequestProperty("Range", "bytes=-128"); //Get the last 128 bytes
            
        }catch(Exception e){
        	e.printStackTrace();
        }
    }
    
    private void InitStream(long length, boolean fromEnd) throws IOException{
    	
    	if(dataStream != null) dataStream.close();
    	if(conn != null && conn instanceof HttpURLConnection){
        	((HttpURLConnection)conn).disconnect();
        }
    	
    	conn= (URLConnection)oUrl.openConnection();
    	
    	if(conn instanceof HttpURLConnection){
    		HttpURLConnection hconn = (HttpURLConnection)conn;
	    	if(fromEnd)
	    	{
	    		//This doesn't work on some servers - so need to get size with HEAD request
	    		//conn.setRequestProperty("Range", String.format("bytes=-%d",length));
	    		hconn.setRequestMethod("HEAD");
	    		hconn.connect();
	    		int itemlength = conn.getContentLength();
	    		hconn.disconnect();
	    		hconn= (HttpURLConnection)oUrl.openConnection();
	    		hconn.setRequestProperty("Range", String.format("bytes=%d-",itemlength - length));
	    	}else{
	    		//conn= (HttpURLConnection)oUrl.openConnection();
	    		hconn.setRequestProperty("Range", String.format("bytes=0-%d",length));
	    	}
	    	hconn.setDoInput(true);
	        hconn.connect();
	        dataStream = conn.getInputStream();
    	}else{ //Non-http/https (usually FileUrlConnection)
    		int itemlength = conn.getContentLength();
    		dataStream = conn.getInputStream();
    		if(fromEnd){
    			dataStream.skip(itemlength - length);
    		}
    	}
    	
    	
        
    }
    
    private void LoadFromStream()
    {
        byte[] bytes = new byte[30];
        String strVal;
        loc = 0;
        long id3v2len = 0, ltmp;
        byte btmp;
        String frame = "";
        
        try{
            
        //check for an ID3v2
        streamRead(bytes,0,10);
        strVal = new String(bytes,0,3);
        if(strVal.equals("ID3")){
            _hasID3v2 = true;
            
            //Get the length of the entire header
            id3v2len = bytes[9];
            ltmp = bytes[8];
            ltmp = ltmp << 7;
            id3v2len = id3v2len | ltmp;
            ltmp = bytes[7];
            ltmp = ltmp << 14;
            id3v2len = id3v2len | ltmp;
            ltmp = bytes[6];
            ltmp = ltmp << 21;
            id3v2len = id3v2len | ltmp;
            
            long reqdReadLength = id3v2len + 20;
            if(reqdReadLength > INITIAL_READ_SIZE){
            	InitStream(reqdReadLength,false);
            }
            
            //Look for an attached picture, while skipping to the end of the header
            long init_loc = loc;
            for(long i = loc; i < init_loc + id3v2len; i++)
            {
                btmp = streamReadByte();
                frame += (char)btmp;
                
                //Read in Title frame
                if(frame.equals("TIT2")){
                    _title = readTextFrame(); i = loc;
                }
                
                if(frame.length() > 3 && frame.substring(1,4).equals("TT2")){
                    _title = readV22TextFrame(); i = loc;
                }
                
                //Read in Artist frame
                if(frame.equals("TPE1")){
                    _artist = readTextFrame(); i = loc;
                }
                
                if(frame.length() > 3 && frame.substring(1,4).equals("TP1")){
                    _artist = readV22TextFrame(); i = loc;
                }
                
                //Read in Album frame
                if(frame.equals("TALB")){
                    _album = readTextFrame(); i = loc;
                }
                
                if(frame.length() > 3 && frame.substring(1,4).equals("TAL")){
                    _album = readV22TextFrame(); i = loc;
                }
                
                if(frame.length() == 4){ frame = frame.substring(1,4); }
            }
            
            loc = id3v2len + 10;
        }else{
            loc = 0;
        }
        
        //If text info fields not gathered from ID3v2 tag at start of file,
        //try to read them from the older ID3 Tag at the end of the file
        if(_artist.equals("") || _title.equals("")){
            
        	InitStream(128,true);
        	loc = 0;
            
            Arrays.fill(bytes,(byte)0);
            streamRead(bytes,0,3);
            strVal = (new String(bytes)).trim();
            if(strVal.equals("TAG")){
                _hasID3 = true;
                //We have an MPs info tag, so get the details
                //Title
                Arrays.fill(bytes,(byte)0); streamRead(bytes,0,30);
                if(_title.equals("") && bytes[0] != 0) _title = (new String(bytes)).trim();
                Arrays.fill(bytes,(byte)0); streamRead(bytes,0,30);
                if(_artist.equals("") && bytes[0] != 0) _artist = (new String(bytes)).trim();
                Arrays.fill(bytes,(byte)0); streamRead(bytes,0,30); //Album
                Arrays.fill(bytes,(byte)0); streamRead(bytes,0,4); //Year
                Arrays.fill(bytes,(byte)0); streamRead(bytes,0,30); //Comment
            }
        }
        
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    
    private String readTextFrame(){
            int fsize = 0;
            byte enc = 0;
            
            try{
                fsize = streamReadInt();                        //Frame Size (4 bytes)
                streamReadByte(); streamReadByte();             //Flags
                enc = streamReadByte();                         //TextEncoding Flag
                return streamReadString(fsize - 2, enc);             //Text String
             }catch(IOException ioex){
                ioex.printStackTrace();
             }
             
            return null;
    }
    
    private String readV22TextFrame(){
        int fsize = 0;
        byte enc = 0;
        
        try{                   
            //Frame Size (3 bytes)
        	byte[] bytes = new byte[3];
            int read = dataStream.read(bytes,0,3);
            if(read != -1){
            	loc += read;
            }

            fsize = bytes[2] & 0x000000FF;
            fsize += (bytes[1] << 8) & 0x0000FF00;
            fsize += (bytes[0] << 16) & 0x00FF0000;

            enc = streamReadByte();                         //TextEncoding Flag
            return streamReadString(fsize - 2, enc);             //Text String
         }catch(IOException ioex){
            ioex.printStackTrace();
        	 //Utilities.LogException(ioex);
         }
         
        return null;
}
    
    public String getTitle()
    {
        return _title;
    }
    
    public String getArtist()
    {
        return _artist;
    }
    
    public String getAlbum()
    {
        return _album;
    }
    
    public long getDuration()
    {
        return _duration;
    }
    
    public String getDescription()
    {
        return _description;
    }
    
    public String getDurationString()
    {
        return MediaToTimeString(_duration);
    }
    
    public int getMPEGVersion(){
        return _mpegVersion;
    }
    
    public static String MediaToTimeString(long time)
    {
        int tsec = (int)(time / 1000000);
        int min = tsec / 60;
        int sec = tsec - (min * 60);
        String ssec = Integer.toString(sec);
        if(ssec.length() == 1) ssec = "0" + ssec;
        return Integer.toString(min) + ":" + ssec;
    }
    
    public long getSize(){
        return _size;
    }
    
    public boolean hasPicture(){
        return _hasPicture;
    }
    
    public Bitmap getPicture(){
        return _picture;
    }
    
    //Private data stream methods that will read MP3 data from whichever type of stream has
    //the MP3 data
    
    private int streamRead(byte[] b, int off, int len) throws IOException{
        if(dataStream != null){
        	int read = dataStream.read(b,off,len);
        	if(read != -1) loc += read;
        	return read;
        }
        else throw new IOException(ERR_NO_STREAM);
    }
    
    private byte streamReadByte() throws IOException{
        if(dataStream != null){
        	int read = dataStream.read();
        	if(read != -1){
        		loc++;
        	}
        	return (byte)read;
        }
        else throw new IOException(ERR_NO_STREAM);
    }
    
    private int streamReadInt() throws IOException{
    	if(dataStream != null){
            byte[] bytes = new byte[4];
            int read = dataStream.read(bytes,0,4);
            if(read == -1){
            	return -1;
            }else{
            	loc += read;
            }
            int ret;
            ret = bytes[3] & 0x000000FF;
            ret += (bytes[2] << 8) & 0x0000FF00;
            ret += (bytes[1] << 16) & 0x00FF0000;
            ret += (bytes[0] << 24) & 0xFF000000;
            return ret;
        }
        else throw new IOException(ERR_NO_STREAM);
    }

    private short streamReadShort() throws IOException{
    	if(dataStream != null){
            byte[] bytes = new byte[2];
            int read =  dataStream.read(bytes,0,2);
            if(read == -1){
            	return -1;
            }else{
            	loc += read;
            }
            short ret = 0;
            ret += bytes[1] & 0x00FF;
            ret += (bytes[0] << 8) & 0xFF00;
            return ret;
        }
        else throw new IOException(ERR_NO_STREAM);
    }
    
    //Read fixed size String from the stream
    private String streamReadString(int size, byte encoding) throws IOException{
        int csize = size;
        byte btmp = 0;
        
        StringBuffer readString;
        if(encoding == 0) readString = new StringBuffer(size);       //UTF-8
        else readString = new StringBuffer(size >> 1);                //UTF-16 UNICODE
        
            if(encoding == 0){
                btmp = streamReadByte(); csize--;
                while(csize >= 0){
                    if(btmp != 0){
                        readString.append((char)btmp);
                        btmp = streamReadByte(); csize--;
                    }else{
                        //null encountered  -continue to end of size but don't add to string
                        streamReadByte(); csize--; 
                    }
                }
            }else{
                //Unicode
                short border = streamReadShort(); csize -= 2;
                short stmp = streamReadShort(); csize -= 2;
                short rtmp;
                while(csize >= 0){
                    if(stmp != 0){
                        if((border & 0xFFFE) == 0xFFFE){
                            //Reverse byte order before char conversion
                            rtmp = stmp;
                            stmp = 0;
                            stmp += (rtmp & 0x00FF) << 8;
                            stmp += (rtmp & 0xFF00) >> 8;
                        } 
                        readString.append((char)stmp);
                        if(csize == 0) break;
                        else
                        {
                            stmp = streamReadShort(); csize -= 2;
                        }
                    }else{
                        //null encountered  -continue to end of size but don't add to string
                        if(csize == 0) break;
                        else
                        {
                            streamReadShort(); csize -= 2; 
                        }
                    }
                }
            }

        return readString.toString();
    }
} 
