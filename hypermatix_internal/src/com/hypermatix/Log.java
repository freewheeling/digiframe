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

package com.hypermatix;

import com.hypermatix.Logs;

import android.content.ContentValues;
import android.content.Context;


public final class Log {

	private static boolean logCat = true;
	private static boolean logDb = true;
	private static Context mContext;
	private static int mLevel;
	
	public static final int	ASSERT	= android.util.Log.ASSERT;
	public static final int	ERROR	= android.util.Log.ERROR;
	public static final int	WARN	= android.util.Log.WARN;
	
	public static final int	INFO	= android.util.Log.INFO;
	public static final int	DEBUG	= android.util.Log.DEBUG;
	
	public static final int	VERBOSE	= android.util.Log.VERBOSE;
	
	public static synchronized void initialize(Context context, int level){
		mContext = context;
		mLevel = level;
	}
	
	public static synchronized void setLoggableLevel(int level){
		mLevel = level;
	}
	
	protected static int logToDatabase(String tag, String msg, int level, Throwable tr){
		return logToDatabase(tag, msg, level, 0, tr, null);
	}
	
	protected static int logToDatabase(String tag, String msg, int level, Throwable tr, String extra){
		return logToDatabase(tag, msg, level, 0, tr, extra);
	}
	
	protected static int logToDatabase(String tag, String msg, int level){
		return logToDatabase(tag, msg, level, 0, null,null);
	}
	
	protected static int logToDatabase(String tag, String msg, int level, int entityId, Throwable tr, String extra){
		
		if(logDb && level >= mLevel){
			
			if(mContext == null){
				android.util.Log.w(tag,"LogToDatabase attempt without a valid context");
			}
			else{
			
				ContentValues values = new ContentValues();
		        values.put(Logs.LogItem.TAG, tag);
		        values.put(Logs.LogItem.LEVEL, level);
		        if(entityId != 0)
		        	values.put(Logs.LogItem.ENTITY, entityId);
		        values.put(Logs.LogItem.DETAIL, msg);
		        String text = "";
		        if(tr != null){
		        	text += android.util.Log.getStackTraceString(tr);
		        }
		        if(extra != null){
		        	if(text != "") text += "\n\n";
		        	text += extra;
		        }
		        
		        values.put(Logs.LogItem.EXTRA, text);
		        
		        mContext.getContentResolver().insert(Logs.LogItem.CONTENT_URI, values);
		
		        return 1;
	        }
			
		}
		
		return 0;
	}

	public static int d (String tag, String msg){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.DEBUG) ){
			res = android.util.Log.d(tag, msg);
		}
		
		logToDatabase(tag, msg, DEBUG);
		
		return res;
	}
	
	public static int d (String tag, String msg, Throwable tr){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.DEBUG) ){
			res = android.util.Log.d(tag, msg, tr);
		}
		
		logToDatabase(tag, msg, DEBUG);
		
		return res;
	}
	
	public static int e (String tag, String msg){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.ERROR) ){
			res = android.util.Log.e(tag, msg == null ? "" : msg);
		}
		
		logToDatabase(tag, msg, ERROR);
		
		return res;
	}
	
	public static int e (String tag, String msg, Throwable tr){
		return e(tag,msg,tr,null);
	}
	
	public static int e (String tag, String msg, Throwable tr, String extra){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.ERROR) ){
			res = android.util.Log.e(tag, msg, tr);
		}
		logToDatabase(tag, msg, ERROR, tr, extra);
		return res;
	}
	
	public static int i (String tag, String msg){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.INFO) ){
			res = android.util.Log.i(tag, msg);
		}
		logToDatabase(tag, msg, INFO);
		return res;
	}

	public static int i (String tag, String msg, Throwable tr){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.INFO) ){
			res = android.util.Log.i(tag, msg, tr);
		}
		logToDatabase(tag, msg, INFO);
		return res;
	}
	
	public static boolean isLoggable (String tag, int level){
		return android.util.Log.isLoggable(tag, level);
	}
	
	public static int println (int priority, String tag, String msg){
		int res = 0;
		if(logCat){
			res = android.util.Log.println(priority, tag, msg);
		}
		logToDatabase(tag, msg, priority);
		return res;
	}
	
	public static int v (String tag, String msg){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.VERBOSE) ){
			res = android.util.Log.v(tag, msg);
		}
		logToDatabase(tag, msg, VERBOSE);
		return res;
	}
	
	public static int v (String tag, String msg, Throwable tr){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.VERBOSE) ){
			res = android.util.Log.v(tag, msg, tr);
		}
		logToDatabase(tag, msg, VERBOSE);
		return res;
	}
	
	public static int w (String tag, Throwable tr){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.WARN) ){
			res = android.util.Log.w(tag, tr);
		}
		logToDatabase(tag, android.util.Log.getStackTraceString(tr), WARN);
		return res;
	}
	
	public static int w (String tag, String msg, Throwable tr){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.WARN) ){
			res = android.util.Log.w(tag, msg, tr);
		}
		logToDatabase(tag, msg, WARN);
		return res;
	}
	
	public static int w (String tag, String msg){
		int res = 0;
		if(logCat && android.util.Log.isLoggable(tag, android.util.Log.WARN) ){
			res = android.util.Log.w(tag, msg);
		}
		logToDatabase(tag, msg, WARN);
		return res;
	}
	
	public static int wtf (String tag, Throwable tr){
		logToDatabase(tag, android.util.Log.getStackTraceString(tr), ERROR);
		return android.util.Log.wtf(tag, tr);
	}
	
	public static int wtf (String tag, String msg){
		logToDatabase(tag, msg, ERROR);
		return android.util.Log.wtf(tag, msg);
	}
	
	public static int wtf (String tag, String msg, Throwable tr){
		logToDatabase(tag, msg, ERROR);
		return android.util.Log.wtf(tag, msg, tr);
	}
	
}
