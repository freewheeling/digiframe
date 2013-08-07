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

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import android.app.ListActivity;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import com.hypermatix.digiframe.common.R;
import com.hypermatix.net.NetworkHelper;

import android.os.Bundle;

public class DeviceListActivity extends ListActivity implements DLNAHelper.DeviceAvailableListener, AdapterView.OnItemClickListener {
	
	private DeviceListAdapter adapter;
	public static DeviceList dl = null;
	public static Device dev = null;
	private DLNAHelper dlnaHelper;
	private LinearLayout progress,listLayout;
	
	private class CheckThread extends Thread{
		@Override
		public void run(){
			try {
				sleep(30000);
			} catch (InterruptedException e) {
				
			}
			if(dl == null || dl.isEmpty()){
    			if(dlnaHelper != null){
    	    		dlnaHelper.removeDeviceAvailableListener(DeviceListActivity.this);
    	    		dlnaHelper.close();
    	    		dlnaHelper = null;
    	    	}
    			DeviceListActivity.this.runOnUiThread(new Runnable(){
    				public void run(){
    					progress.setVisibility(View.INVISIBLE);
    	            	listLayout.setVisibility(View.VISIBLE);
    				}
    			});
    			
    		}
		}
	}
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.device_list);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);
        
        if(!NetworkHelper.isWiFiConnected(this)){
        	this.findViewById(R.id.no_wifi).setVisibility(View.VISIBLE);
        }else{
        
	        progress = (LinearLayout)this.findViewById(R.id.progress);
	        progress.setVisibility(View.VISIBLE);
	        listLayout = (LinearLayout)this.findViewById(R.id.the_list);
	        
	        adapter = new DeviceListAdapter(this);
	        setListAdapter(adapter);
	        
	        getListView().setOnItemClickListener(this);
	        
	        new CheckThread().start();
	        
	        dlnaHelper = new DLNAHelper();
	        dlnaHelper.addDeviceAvailableListener(this);
        }
    }
    
    @Override
    public void onDestroy(){
    	if(dlnaHelper != null){
    		dlnaHelper.removeDeviceAvailableListener(this);
    		dlnaHelper.close();
    	}
    	super.onDestroy();
    }
    
	private class RefreshDevices implements Runnable {

        public RefreshDevices() {
        }

        public void run() {
        	
        	dl = dlnaHelper.getDeviceList();
        	adapter.setList(dl);
        	setListAdapter(null);
        	setListAdapter(adapter);
        	progress.setVisibility(View.INVISIBLE);
        	listLayout.setVisibility(View.VISIBLE);
        }
    }

	@Override
	public void onItemClick(AdapterView<?> av, View view, int position, long id) {
		//Device selected - open it's directory view
		dev = DeviceListActivity.dl.getDevice(position);
		Intent intent = new Intent(this, DLNADirectoryListActivity.class);
		intent.putExtra("DevicePos", position);
		startActivityForResult(intent, SlideshowActivity.REQ_CODE_ADD_ITEM);
	}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if(requestCode == SlideshowActivity.REQ_CODE_ADD_ITEM &&
				resultCode == SlideshowActivity.RES_CODE_ADD_ITEM){
			setResult(SlideshowActivity.RES_CODE_ADD_ITEM);
			this.finish();
		}
	}

	@Override
	public void OnDeviceAvailable(String udn) {
		runOnUiThread(new RefreshDevices()); 
	}
}