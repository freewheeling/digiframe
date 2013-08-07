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

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import com.hypermatix.digiframe.common.ConfigActivity;

public abstract class Utils{
	
	
	public static String getFileExtension(String filename){
		int loc = filename.lastIndexOf(".");
	    if(loc > 0){
	    	String ext = filename.substring(loc+1);
	    	return ext;
	    }
	    return "";
	}
	
	 public static String getSharedPreference(Context context, String key, String defaultValue) {
	        SharedPreferences prefs = ConfigActivity.getSharedPreferences(context);
	        return prefs.getString(key, defaultValue);
	    }

	 public static boolean getSharedPreference(Context context, String key, boolean defaultValue) {
	        SharedPreferences prefs = ConfigActivity.getSharedPreferences(context);
	        return prefs.getBoolean(key, defaultValue);
	    }
	  
	 public static void setSharedPreference(Context context, String key, String value) {
	        SharedPreferences prefs = ConfigActivity.getSharedPreferences(context);
	        SharedPreferences.Editor editor = prefs.edit();
	        editor.putString(key, value);
	        editor.commit();
	    }

	    public static void startActivity(Context context, String className) {
	        Intent intent = new Intent(Intent.ACTION_VIEW);
	        intent.setClassName(context, className);
	        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);

	        context.startActivity(intent);
	    }

	    public static int getPackageVersionCode(Activity cxt){
	    	try {
				return cxt.getApplication().getPackageManager().getPackageInfo(cxt.getApplication().getPackageName(), 0).versionCode;
			} catch (Exception e) {
				return -1;
			}
	    }

	    public static String getPackageVersionName(Activity cxt){
	    	try {
				return cxt.getApplication().getPackageManager().getPackageInfo(cxt.getApplication().getPackageName(), 0).versionName;
			} catch (Exception e) {
				return null;
			}
	    }

	    public static String getPackageName(Activity cxt){
	    	try {
				return cxt.getApplication().getPackageManager().getPackageInfo(cxt.getApplication().getPackageName(), 0).packageName;
			} catch (Exception e) {
				return null;
			}
	    }
	    
	    public static String readAssetFile(Activity cxt,String name){
	    	InputStream is = null;
			try {
				is = cxt.getAssets().open(name, AssetManager.ACCESS_BUFFER);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			
			StringBuilder sb = new StringBuilder();           
			try {              
				byte[] bytes = new byte[4096];
				int num = 0;
				do{
					num = is.read(bytes);
					sb.append(new String(bytes,0,num,"UTF8"));
				}
				while(num == 4096);
			}catch(Exception e){       
			} finally {             
				try {
					is.close();
				} catch (Exception e) {
				}           
			}           
			return sb.toString();
	    }
}