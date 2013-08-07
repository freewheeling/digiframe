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


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.app.ListActivity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;

import com.hypermatix.digiframe.common.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;


public class SDDirectoryListActivity extends ListActivity 
									implements AdapterView.OnItemClickListener, 
												View.OnClickListener {
	
	private SDDirectoryListAdapter adapter;
	public static File f;
	public static SDItem sdi;
	private static LinkedList<SlideshowListItem> parentInclusionStack= new LinkedList<SlideshowListItem>();
	private static LinkedList<SlideshowListItem> parentExclusionStack= new LinkedList<SlideshowListItem>();
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    	requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.directory_list);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);
        
        adapter = new SDDirectoryListAdapter(this, this);
        setListAdapter(adapter);
        
        refreshDirectoryList(null);
        
        getListView().setOnItemClickListener(this);
        getListView().setClickable(true);
    }
    
    @Override
    public void onDestroy(){
    	parentInclusionStack.clear();
    	parentExclusionStack.clear();
    	super.onDestroy();
    }

    public static SDItem[] getDirectoryList(File directory){
    	return getDirectoryList(directory, true);
    }
    
	public static SDItem[] getDirectoryList(File directory, boolean addParent){
    	
    	SDItem parent = null;
    	String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    	
    	if(directory == null){
    		directory = new File(baseDir + File.separator);
    	}else{
    		if(addParent){
    			if(!directory.getAbsolutePath().equals(baseDir))
    			{
    				parent = new SDItem("[..]", directory.getParentFile());
    				parent.isSelectable = false;
    			}
    		}
    	}
    	
    	File[] results = directory.listFiles(new MediaFileFilter());
    	//Arrays.sort(results);	
    	
        List<SDItem>dir = new ArrayList<SDItem>();
        List<SDItem>fls = new ArrayList<SDItem>();
        List<SDItem>fnl = new ArrayList<SDItem>();
        
        try{
	        for(File ff: results)
	        {	
	        	if(!ff.isHidden()){
	        		String url = Uri.fromFile(ff).toString();
	        		
	        		SlideshowListItem si;
	        		if(SlideshowActivity.itemList != null){
	        			si = SlideshowActivity.itemList.getItem(url);
	        			//If moving back folders, remove the inclusion
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
	        
	        for(File ff: results)
	        {	
	        	if(!ff.isHidden()){
	        		String url = Uri.fromFile(ff).toString();
	        		SDItem item = new SDItem(ff.getName(),ff);
	        		SlideshowListItem si;
	        		if(SlideshowActivity.itemList != null){
	        			si = SlideshowActivity.itemList.getItem(url);
	        			if(si != null && si.IsExclusion) 
	        				item.isSelected = false;
	        			
	        			if(si != null){
	        				item.isSelected = !si.IsExclusion;
	        			}else if(SDDirectoryListActivity.inInclusionParent()){
	        				item.isSelected = true;
	        			}
	        			
	        		}
		        	if(ff.isDirectory())
		        		dir.add(item);
		        	else
		        	{
		        		fls.add(item);
		        	}
	        	}
	        }
        }catch(Exception e)
        {
        	e.printStackTrace();
        }
        Collections.sort(dir);
        Collections.sort(fls);
        
        if(addParent && parent != null){
        	fnl.add(parent);
        }
        
        fnl.addAll(dir);
        fnl.addAll(fls);
        
        SDItem[] arr = new SDItem[0];
        return fnl.toArray(arr);
    	
    }
    
    private void refreshDirectoryList(File directory){
    	adapter.setList(getDirectoryList(directory));
    	setListAdapter(null);
    	setListAdapter(adapter);
    }

	@Override
	public void onItemClick(AdapterView<?> av, View view, int position, long id) {
		
		
	}

	@Override
	public void onClick(View v) {
		if(v.getClass().equals(CheckBox.class)){
			//Click is on the check of the item
			CheckBox cb = (CheckBox)v;
			SDItem item = (SDItem)cb.getTag();
			if(!cb.isChecked()){
				if(inInclusionParent()){
					SlideshowActivity.itemList.addExclusionSDItem(item,
							parentInclusionStack.peek().ItemUrl);
				}else{
					SlideshowActivity.itemList.removeInclusionItem(item.getUrl());
				}
			}else{
				SlideshowListItem si = parentInclusionStack.peek();
				if(inExclusionParent() && (si == null || (item.getUrl().equals(si.ItemUrl)))){
					//Remove Exclusion from list and from stack
					SlideshowActivity.itemList.removeExclusionItem(si.ItemUrl);
					parentExclusionStack.remove(si);
				}

				SlideshowActivity.itemList.addInclusionSDItem(item);
			}
		}else{
			//The click is on the item itself
			int position = Integer.parseInt(v.getTag().toString());
			SDItem item = (SDItem)adapter.getList()[position]; 
			File file = item.file;
			if(file.isDirectory()){
				//if the item is checked, set the flag
				if(!item.name.equals("[..]")){
					checkDirectoryInclusionDown(file);
				}
				refreshDirectoryList(file);
			}
		}
	}
	
	private void checkDirectoryInclusionDown(File dir){
		String url = Uri.fromFile(dir).toString();
		SlideshowListItem si;
		if(SlideshowActivity.itemList != null){
			si = SlideshowActivity.itemList.getItem(url);
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
	
