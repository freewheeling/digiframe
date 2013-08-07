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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.Icon;
import org.cybergarage.upnp.IconList;

import com.hypermatix.digiframe.common.R;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class DeviceListAdapter implements ListAdapter {

	private DeviceList list;
	private LayoutInflater mInflater;
	
	public DeviceListAdapter(Context context){
		mInflater = LayoutInflater.from(context);
	}
	
	public void setList(DeviceList list){
		this.list = list;
	}
	
	@Override
	public int getCount() {
		return list == null ? 0 : list.size();
	}

	@Override
	public Object getItem(int idx) {
		return list == null ? null : list.getDevice(idx);
	}

	@Override
	public long getItemId(int idx) {
		return idx;
	}

	@Override
	public int getItemViewType(int idx) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.device_list_item, null);
        }
        
        TextView text = (TextView) convertView.findViewById(R.id.text);
        ImageView ivIcon = (ImageView) convertView.findViewById(R.id.icon);
        Device device = (Device)getItem(position);
        text.setText(device.getFriendlyName());
        
        /*
         * Get the DNLA server icon - use the largest one available (and scale it down)
         * If any problems occur, just leave it blank for now
         */
        
        try {
	        IconList ilist = device.getIconList();
	        Icon icon = null;
	        if(ilist != null){
	        	for(int i = 0; i < ilist.size();i++){
	        		Icon temp_icon = device.getIcon(i);
	        		if(icon == null) icon = temp_icon;
	        		else{	
	        			if(temp_icon.getHeight() > icon.getHeight() &&
	        					temp_icon.getHeight() <= android.R.attr.listPreferredItemHeight)
	        				icon = temp_icon;
	        		}
	        	}
	        }
	        if(icon != null && icon.getURL() != null){
		        URL myFileUrl =null;          
		        myFileUrl= new URL(device.getAbsoluteURL(icon.getURL())); //TODO - icon url could be absolute (i.e. not relative like here)
		        HttpURLConnection conn= (HttpURLConnection)myFileUrl.openConnection();
		        conn.setDoInput(true);
		        conn.connect();
		        InputStream is = conn.getInputStream();
		        Bitmap bmIcon = BitmapFactory.decodeStream(is);
		        ivIcon.setImageBitmap(bmIcon);
	        }
        } catch (Exception e) {
        	ivIcon.setImageBitmap(null);
       }

        return convertView;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return list == null ? true : list.isEmpty();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver arg0) {
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver arg0) {
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int arg0) {
		return true;
	}
	
}