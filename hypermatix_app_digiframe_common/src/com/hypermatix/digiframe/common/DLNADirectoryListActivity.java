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

import java.util.ArrayList;
import java.util.LinkedList;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.TextView;

import com.hypermatix.digiframe.common.R;

import android.os.Bundle;
import android.util.DisplayMetrics;


public class DLNADirectoryListActivity extends ListActivity 
									implements AdapterView.OnItemClickListener, 
												View.OnClickListener {
	
	private DLNADirectoryListAdapter adapter;
	private LinkedList<DLNAItem> parentStack= new LinkedList<DLNAItem>();
	private DLNAHelper dlnaHelper;
	private int displayWidth;
	private int displayHeight;
	private TextView emptyView;
	private static LinkedList<SlideshowListItem> parentInclusionStack= new LinkedList<SlideshowListItem>();
	private static LinkedList<SlideshowListItem> parentExclusionStack= new LinkedList<SlideshowListItem>();
	
	public static DLNAItem di;
	private ArrayList<DLNAItem> dirList;
	
	private class RefreshThread extends Thread{
		@Override
		public void run(){
			try{
			String oid = null;
	    	if(parentStack.size() > 0){
	    		DLNAItem item = (DLNAItem)parentStack.getLast();
	    		oid = item.ID;
	    	}
	    	String pid = null;
	    	if(parentStack.size() > 1){
	    		DLNAItem pitem = (DLNAItem)parentStack.get(parentStack.size() - 2); 
	    		pid = pitem.ID;
	    	}else if(parentStack.size() > 0){
	    		pid = "0";
	    	}
	    	
	    	dirList = dlnaHelper.getDirectoryList(oid, pid,0,0, displayWidth, displayHeight); 
			processDirList();
			}finally{
				DLNADirectoryListActivity.this.runOnUiThread(RefreshComplete);
			}
		}
	}
	
	private Runnable RefreshComplete = new Runnable(){
		public void run(){
			emptyView.setText(R.string.no_items);
			setProgressBarIndeterminateVisibility(false);
	    	adapter.setList(dirList);
	    	setListAdapter(null);
	    	setListAdapter(adapter);
		}
	};
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    	requestWindowFeature(Window.FEATURE_LEFT_ICON);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.directory_list);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);
        
        emptyView = (TextView) this.getListView().getEmptyView();
        emptyView.setText("");
        
        Intent intent = this.getIntent();
        int devpos = intent.getIntExtra("DevicePos",0);
        dlnaHelper = new DLNAHelper();
        dlnaHelper.setDevice(devpos);
        adapter = new DLNADirectoryListAdapter(this, null, this);
        setListAdapter(adapter);
        
		DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		displayWidth = metrics.widthPixels;
		displayHeight = metrics.heightPixels;
        
        refreshDirectoryList();
        
        getListView().setOnItemClickListener(this);
    }
    
    @Override
    public void onDestroy(){
    	dlnaHelper.close();
    	parentInclusionStack.clear();
    	parentExclusionStack.clear();
    	super.onDestroy();
    }
        
    private void refreshDirectoryList(){
    	setProgressBarIndeterminateVisibility(true);
    	new RefreshThread().start();
    }

	@Override
	public void onItemClick(AdapterView<?> av, View view, int position, long id) {
		
		
	}

	@Override
	public void onClick(View v) {
		if(v.getClass().equals(CheckBox.class)){
			CheckBox cb = (CheckBox)v;
			di = (DLNAItem)cb.getTag();
			//Add to list
			//setResult(SlideshowActivity.RES_CODE_ADD_ITEM);
			//this.finish();
			if(!cb.isChecked()){
				if(inInclusionParent()){
					SlideshowActivity.itemList.addExclusionDLNAItem(di,
							parentInclusionStack.peek().ItemID);
				}else{
					SlideshowActivity.itemList.removeInclusionItem(di.ID);
				}
			}else{
				SlideshowListItem si = parentInclusionStack.peek();
				if(inExclusionParent() && (si == null || (di.ID.equals(si.ItemID)))){
					//Remove Exclusion from list and from stack
					SlideshowActivity.itemList.removeExclusionItem(si.ItemID);
					parentExclusionStack.remove(si);
				}

				SlideshowActivity.itemList.addInclusionDLNAItem(di);
			}

		}else{
			int position = Integer.parseInt(v.getTag().toString());
			DLNAItem item = (DLNAItem)adapter.getList().get(position);
			if(item.URI == null){ //A container, not a file				
				if(item.isParentNavigator)
					parentStack.removeLast();
				else{
					parentStack.addLast(item);
					checkDirectoryInclusionDown(item);
				}
				
				refreshDirectoryList();
			}
		}
	}
	
	public void processDirList(){
		SlideshowListItem si;
		if(SlideshowActivity.itemList != null){
			//Remove inclusion/exlcusion stack items as travelling down/back up tree
			for(DLNAItem item : dirList)
			{
				//Do do this check for parent navigator as it would remove the inclusion everytime
				if(!item.isParentNavigator){
					si = SlideshowActivity.itemList.getItem(item.ID);
					SlideshowListItem p = parentInclusionStack.peek();
					if(p != null && p.equals(si)) 
						parentInclusionStack.remove(p);
					//If moving back folders, remove the inclusion
					p = parentExclusionStack.peek();
					if(p != null && p.equals(si)) 
						parentExclusionStack.remove(p);
					}
				}
			}
			
			for(DLNAItem item : dirList)
			{
				si = SlideshowActivity.itemList.getItem(item.ID);
				if(si != null){
					item.isSelected = !si.IsExclusion;
				}else if(DLNADirectoryListActivity.inInclusionParent()){
					item.isSelected = true;
				}
		}

	}
	
	private void checkDirectoryInclusionDown(DLNAItem item){
		SlideshowListItem si;
		if(SlideshowActivity.itemList != null){
			si = SlideshowActivity.itemList.getItem(item.ID);
			if(si != null && !si.IsExclusion){
				parentInclusionStack.add(si);
			}
			if(si != null && si.IsExclusion){
				parentExclusionStack.add(si);
			}
		}
	}
	
	public static boolean inInclusionParent(){
		return parentInclusionStack.size() > 0 &&
				(parentInclusionStack.size() > parentExclusionStack.size());
	}
	
	public static boolean inExclusionParent(){
		return parentExclusionStack.size() > 0 &&
				(parentExclusionStack.size() == parentInclusionStack.size());
	}

}