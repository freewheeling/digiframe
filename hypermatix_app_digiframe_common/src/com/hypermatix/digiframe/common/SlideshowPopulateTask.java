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
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;

public class SlideshowPopulateTask extends AsyncTask<Void, Integer, Void>{

	private long slideshow_id;
	private Context cxt;
	private SQLiteDatabase db;
	private DatabaseHelper dbHelper;
	private DLNAHelper dlnaHelper;
	private static Hashtable<Long,SlideshowPopulateTask> tasks = new Hashtable<Long,SlideshowPopulateTask>();
	private static ArrayList<SlideshowPopulateListener> listeners = new ArrayList<SlideshowPopulateListener>();
	private int itemCount;
	private int displayWidth;
	private int displayHeight;
	private long memoryLimit;
	private SlideshowItemList items = new SlideshowItemList(cxt,slideshow_id);
	
	public interface SlideshowPopulateListener{
		void progressUpdate(long SlideshowID, int ItemCount);
		void finished(long SlideshowID);
		void startPopulating(long SlideshowID);
	}
	
	public SlideshowPopulateTask(Context cxt, long slideshow_id) throws Exception{
		if(SlideshowPopulateTask.hasTaskForSlideshow(slideshow_id)){
			throw new Exception("Task already exists for that Slideshow");
		}
		this.cxt = cxt;
		this.slideshow_id = slideshow_id;
		DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		displayWidth = metrics.widthPixels;
		displayHeight = metrics.heightPixels;
		//MemoryLimit = android max mb for app, -1Mb (for safety)
		ActivityManager am = (ActivityManager) cxt.getSystemService(Service.ACTIVITY_SERVICE);
		memoryLimit = (am.getMemoryClass() - 1) * 1048576;
		SlideshowPopulateTask.addTaskForSlideshow(slideshow_id, this);
	}
	
	public static void registerListener(SlideshowPopulateListener listener){
		synchronized(listeners){
			listeners.add(listener);
		}
	}

	public static void deregisterListener(SlideshowPopulateListener listener){
		synchronized(listeners){
			listeners.remove(listener);
		}
	}
	
	public static boolean hasTaskForSlideshow(long id){
		synchronized(tasks){
			return tasks.get(id) != null;
		}
	}
	
	private static boolean addTaskForSlideshow(long id, SlideshowPopulateTask task){
		synchronized(tasks){
			tasks.put(id, task);
			return true;
		}
	}
	
	private static boolean removeTaskForSlideshow(long id){
		synchronized(tasks){
			tasks.remove(id);
			return true;
		}
	}
	
	@Override
	protected Void doInBackground(Void... voids) {
		
		//Get the list of items for the slideshow and add them. If they are directories, recurse down.
		dbHelper = DatabaseHelper.getInstance(this.cxt);
        db = dbHelper.getReadableDatabase();
		
        itemCount = 0;
        
        //Remove all the current items in the database, reset last refreshed field in database
        db.delete(DatabaseHelper.Tables.PLAY_ITEM, "slideshow_id = ?", new String[] {Long.toString(slideshow_id)});
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.Columns.PLAYLIST_CREATED, 0);
        values.put(DatabaseHelper.Columns.PLAYLIST_CREATED_DATE, 0);
        db.update(DatabaseHelper.Tables.SLIDESHOW, values, "_id = ?", new String[] {Long.toString(slideshow_id)});
        values.clear();
        values.put(DatabaseHelper.Columns.SYNC_ID, "");
        db.update(DatabaseHelper.Tables.SLIDESHOW_ITEM, values, "slideshow_id = ?", new String[] {Long.toString(slideshow_id)});
        
        dlnaHelper = new DLNAHelper();
        
        items = new SlideshowItemList(cxt,slideshow_id);
        Iterator<SlideshowListItem> inclusions = items.getInclusionIterator();
        
        try{
        
        while(inclusions.hasNext()){
        	SlideshowListItem item = inclusions.next();
        	
        	int location = item.Location;
        	String device_udn = item.DeviceUDN;
        	String item_oid = item.ItemID;
        	String item_url = item.ItemUrl;
        	int item_type = item.ItemType;
        	
        	if(location == DatabaseHelper.LOCATION_FILE_SYSTEM){
        		recurseSDDirectoryItem(item_url, false);
        	}else if(location == DatabaseHelper.LOCATION_MEDIA_SERVER){
        	
	        	dlnaHelper.setDevice(device_udn);
	        	
	        	int count = 0;
	        	while((!dlnaHelper.isDeviceAvailable() && count < 15) && !isCancelled()){
	        		try{
		        		Thread.sleep(1000);
		        	} catch (InterruptedException e) {
		    		}
	        		count++;
	        	}
	        	
	        	if(dlnaHelper.isDeviceAvailable() && !isCancelled()) 
	        	{
	        		recurseDirectoryItem(item_type, item_oid, item_url, false, 0, 0);
	        	}
        	}
        }
        
        //Update the last refreshed field in the database
        values.clear();
        values.put(DatabaseHelper.Columns.PLAYLIST_CREATED, 1);
        values.put(DatabaseHelper.Columns.PLAYLIST_CREATED_DATE, (new Date()).getTime());
        db.update(DatabaseHelper.Tables.SLIDESHOW, values, "_id = ?", new String[] {Long.toString(slideshow_id)});
        
        }finally{
        	dlnaHelper.close();
        }
        
        dbHelper.close();
		
		return null;
	}
	
	@Override
	protected void onPreExecute (){
		synchronized(listeners){
			Iterator<SlideshowPopulateListener> it = listeners.iterator();
			while(it.hasNext()){
				SlideshowPopulateListener listener = it.next();
				listener.startPopulating(slideshow_id);
			}
		}
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		synchronized(listeners){
			Iterator<SlideshowPopulateListener> it = listeners.iterator();
			while(it.hasNext()){
				SlideshowPopulateListener listener = it.next();
				listener.progressUpdate(slideshow_id, progress[0]);
			}
		}
    }
	
	private void recurseSDDirectoryItem(String uri, Boolean landscape){
		File file = new File(URI.create(uri));
		if(file.isFile()){
			SDItem item = new SDItem(null,file);
			boolean add = true;
        	//If there's no image size data from which to calculate load scaling factor,
        	//we need to determine if the whole image is likely to fit comfortably  in
        	//memory (or else OutOfMemoryError could be thrown)
        	if(item.width == 0 || item.height == 0){
        		if(item.size > memoryLimit) add = false;
        	}
        	if(add){
				ContentValues values = new ContentValues();
				values.put(DatabaseHelper.Columns.SLIDESHOW_ID, slideshow_id);
				values.put(DatabaseHelper.Columns.LOCATION, DatabaseHelper.LOCATION_FILE_SYSTEM);
				values.put(DatabaseHelper.Columns.URL, uri);
				values.put(DatabaseHelper.Columns.ITEM_TYPE, item.isMusic ? DatabaseHelper.ITEM_TYPE_AUDIO : DatabaseHelper.ITEM_TYPE_IMAGE);
				values.put(DatabaseHelper.Columns.LANDSCAPE, landscape);
				values.put(DatabaseHelper.Columns.WIDTH, item.width);
				values.put(DatabaseHelper.Columns.HEIGHT, item.height);
				dbHelper.playItemInsert(values);
				itemCount++;
				publishProgress(itemCount);
        	}
		}else{
			//Directory
			SDItem[] sditems = SDDirectoryListActivity.getDirectoryList(file,false);
			for(int i = 0; i < sditems.length; i++){
    			SDItem item = sditems[i];
    			if (items.getExclusion(item.getUrl()) == null)
    				recurseSDDirectoryItem(Uri.fromFile(item.file).toString(),item.isLandscape); 
    		}
		}
	}
	
	private void recurseDirectoryItem(int itemClass, String id, String uri, Boolean landscape, int width, int height){
		if(itemClass != DatabaseHelper.ITEM_TYPE_FOLDER){
    		//Add the item directly to play list
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.Columns.SLIDESHOW_ID, slideshow_id);
			values.put(DatabaseHelper.Columns.LOCATION, DatabaseHelper.LOCATION_MEDIA_SERVER);
			values.put(DatabaseHelper.Columns.OID, id);
			values.put(DatabaseHelper.Columns.URL, uri);
			values.put(DatabaseHelper.Columns.ITEM_TYPE, itemClass);
			values.put(DatabaseHelper.Columns.LANDSCAPE, landscape);
			values.put(DatabaseHelper.Columns.WIDTH, width);
			values.put(DatabaseHelper.Columns.HEIGHT, height);
			dbHelper.playItemInsert(values);
			itemCount++;
			publishProgress(itemCount);
    	}else{
    		//Item is a directory - recurse and add contents to play list
    		ArrayList<DLNAItem> list = dlnaHelper.getDirectoryList(id,null,0,0, displayWidth, displayHeight);
    		
    		for(int i = 0; i < list.size(); i++){
    			DLNAItem item = list.get(i);
    			if (items.getExclusion(item.ID) == null)
    				recurseDirectoryItem(DLNAHelper.textTypeToDBType(item.ItemClass), item.ID, item.URI, item.Landscape, item.width, item.height);
    		}
    	}
	}
	
	@Override
	protected void onPostExecute (Void result) 
	{
		SlideshowPopulateTask.removeTaskForSlideshow(this.slideshow_id);
		synchronized(listeners){
			Iterator<SlideshowPopulateListener> it = listeners.iterator();
			while(it.hasNext()){
				SlideshowPopulateListener listener = it.next();
				listener.finished(slideshow_id);
			}
		}
	}
	
	protected void onCancelled (Void result){
		SlideshowPopulateTask.removeTaskForSlideshow(this.slideshow_id);
		synchronized(listeners){
			Iterator<SlideshowPopulateListener> it = listeners.iterator();
			while(it.hasNext()){
				SlideshowPopulateListener listener = it.next();
				listener.finished(slideshow_id);
			}
		}
	}
}