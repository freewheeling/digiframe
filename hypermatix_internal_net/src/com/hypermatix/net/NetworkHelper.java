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
 
package com.hypermatix.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkHelper {

	public static boolean isWiFiConnected(Context context){

		boolean result = false;
		
		ConnectivityManager cman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo ninf = cman.getActiveNetworkInfo();
		
		if(ninf != null && ninf.isConnected()){
			int netType = ninf.getType();
			
			if (netType == ConnectivityManager.TYPE_WIFI) {
				result = true;
			}
		}
		
		return result;
	}
	
	 public static boolean isNetworkAvailable(Context context){
			ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo inf = cm.getActiveNetworkInfo();
			if(inf != null){
				if (inf.getState() ==  NetworkInfo.State.CONNECTED ||
						inf.getState() == NetworkInfo.State.CONNECTING){
					return true;
				}
			}
			return false;
		}

}