package org.jaudiotagger.audio;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class HttpFile {

	private URL url;
	private HttpURLConnection conn;
	private int length;
	private int pos = 0;
	
	public HttpFile(String u) throws IOException{
		url = new URL(u);
		URLConnection c = (URLConnection)url.openConnection();
		if(!(c instanceof HttpURLConnection)) throw new IOException("Http only URLs allowed");
		else conn = (HttpURLConnection)c;
		//Access file and get it's total length
    	conn.setRequestMethod("HEAD");
    	conn.connect();
    	length = conn.getContentLength();
    	conn.disconnect(); conn=null;
	}
	
	public void close() throws IOException {
		if(conn != null) { conn.disconnect(); conn = null; }
	}

	public boolean isOpen() {
		return (conn != null);
	}

	public long position(long newPos) throws IOException{
		pos = (int)newPos;
		return (long)pos;
	}
	
	public long position(){
		return (long)pos;
	}
	
	public long length(){
		return length;
	}

	public int read(ByteBuffer bb) throws IOException {
		
		int numRead = read(bb,pos);
		if(numRead > 0) pos += numRead;
		return numRead;
	}
	
	public int read(ByteBuffer bb, long position) throws IOException {
		
		int lpos = (int)position;
		//Position already at or past EOF
		if(lpos > length-1) return -1;
		
		int to = lpos + bb.remaining() - 1;
		if(to >= length) to = length - 1;
		int numToRead = to-lpos+1;
		
		conn = (HttpURLConnection)url.openConnection();
		conn.setRequestProperty("Range", String.format("bytes=%d-%d",lpos,to));
		conn.setDoInput(true);
		conn.connect();
        InputStream ds = conn.getInputStream();
        byte[] bytes = new byte[numToRead];
        ds.read(bytes);
        bb.put(bytes, 0, numToRead);
        ds.close();
        conn.disconnect();
		
		return numToRead;
	}
	
	public String getName(){
		return url.toString();
	}

}
