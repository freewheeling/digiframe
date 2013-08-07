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

import java.util.Iterator;

import com.hypermatix.digiframe.common.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

public class SlideshowActivity extends Activity implements View.OnClickListener {
	
	private Button btnSave, btnAdd, btnDel, btnPlaylist;
	private EditText editName;
	private ListView itemListView;
	private SlideshowItemAdapter adapter;
	private long id = -1;
	private SQLiteDatabase db;
	private DatabaseHelper dbHelper;
	private boolean itemsChanged = false;
	public static SlideshowItemList itemList;
	
	public static final String EXT_ID = "id";
	
	public static final int REQ_CODE_ADD_ITEM = 1;
	
	public static final int RES_CODE_ADD_ITEM = RESULT_FIRST_USER + 1;
	
	private static final String[] SLIDESHOW_PROJECTION = new String[] {
        DatabaseHelper.Columns.ID,
        DatabaseHelper.Columns.NAME
	};
	private static final int SLIDESHOW_NAME_INDEX = 1;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.slideshow);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);
        
        Intent intent = this.getIntent();
        id = intent.getLongExtra(EXT_ID,-1);
        btnAdd = (Button)findViewById(R.id.add_slideshow_item);
        btnAdd.setOnClickListener(this);
        
        editName = (EditText)findViewById(R.id.name_edit);
        
        itemListView = (ListView)findViewById(R.id.item_list);
        
        if(isNew()){
        	itemList = new SlideshowItemList();
        	adapter = new SlideshowItemAdapter(this,null);
        }else{
        	itemList = new SlideshowItemList(this,id);
        	loadSlideshow();
        }

    }
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu){
		if(isNew()){
			menu.setGroupVisible(1, false);
		}else{
			menu.setGroupVisible(1, true);
		}
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu (Menu menu){

		MenuItem item = menu.add(1, 1, 0, R.string.refresh_playlist);
		item.setIcon(android.R.drawable.ic_menu_rotate);
		item = menu.add(2, 2, 0, R.string.delete_slideshow);
        item.setIcon(android.R.drawable.ic_delete);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		if(item.getItemId() == 1){
			refreshPlaylist();
			this.finish();
		}else if(item.getItemId() == 2){
			//Delete
			AlertDialog.Builder builder = new AlertDialog.Builder(this).
			setCancelable(true).
			setTitle(getString(R.string.delete)).
			setMessage(R.string.confirm).
			setInverseBackgroundForced(true).
			setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					deleteSlideshow();
				}
				}).
			setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		AlertDialog alert = builder.create();
		alert.show();
			
		}
		return true;
	}
	
	@Override
	protected void onDestroy (){
		//saveSlideshow();
		super.onDestroy();
	}
	
	private boolean isNew(){
		return id <= 0;
	}
	
	public void loadSlideshow(){
		DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor c = db.query(DatabaseHelper.Tables.SLIDESHOW, SLIDESHOW_PROJECTION, "_id = ?", new String[]{Long.toString(id)}, null, null, null);
        
        while(c.moveToNext()){
        	editName.setText(c.getString(SLIDESHOW_NAME_INDEX));
        }
        
        c.close();
        dbHelper.close();
        
        refreshAdapter();
       
        /*
        //c = db.query(DatabaseHelper.Tables.SLIDESHOW_ITEM, SLIDESHOW_ITEM_PROJECTION, "slideshow_id = ?", new String[]{Long.toString(id)}, null, null, null);
        //c = db.query(DatabaseHelper.Tables.SLIDESHOW_ITEM, SLIDESHOW_ITEM_PROJECTION, null, null, null, null, null);
        c = db.rawQuery("SELECT si._id as _id, si.name as name, device_id, udn, friendly_name, " +
        			    "oid, url, item_type, slideshow_id " +
        			    "FROM Slideshow_item si LEFT OUTER JOIN Device d ON si.device_id = d._id " +
        			    "WHERE slideshow_id = ?", new String[]{Long.toString(id)});
        c.moveToFirst();
        
        //startManagingCursor(c);
        //adapter = new SlideshowItemAdapter(this,c);
        //itemListView.setAdapter(adapter);
        
        Cursor c2 = db.rawQuery("SELECT COUNT(*) FROM Play_Item WHERE slideshow_id = ?", new String[]{Long.toString(id)}); 
        c2.moveToNext();
        String count = c2.getString(0); 
        c2.close();
        
        countView.setText(count);
        */
	}

	
	public boolean saveSlideshow(){
		String name = editName.getText().toString().trim();
		
		if(isNew()){
			if(name.length() == 0 &&
					adapter.getCount() == 0)
				return true; //Close
		}
		
		if(name.length() == 0){
			Toast.makeText(this, getString(R.string.name_mandatory), Toast.LENGTH_LONG).show();
			return false;
		}	
		
		dbHelper = DatabaseHelper.getInstance(this);
        db = dbHelper.getReadableDatabase();
		
		if(isNew()){
			//Insert the Slideshow and an item/device details
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.Columns.NAME, name);
			id = dbHelper.slideshowInsert(values);
			Iterator<SlideshowListItem> itr = itemList.getInclusionIterator();
			while(itr.hasNext()){
				SlideshowListItem si = (SlideshowListItem)itr.next();
				long did = addOrGetDevice(si);
				addOrGetItem(id, did, si);
			}
			itr = itemList.getExclusionIterator();
			while(itr.hasNext()){
				SlideshowListItem si = (SlideshowListItem)itr.next();
				long did = addOrGetDevice(si);
				addOrGetItem(id, did, si);
			}
			refreshPlaylist();
		}else{
			//Update the Slideshow, and insert and new items/devices and delete any removed ones
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.Columns.NAME, name);
			db.update(DatabaseHelper.Tables.SLIDESHOW, values, DatabaseHelper.Columns.ID + " = ? ", new String[] {Long.toString(id)});
			Iterator<String> deletions = itemList.getDeletionsIterator();
			while(deletions.hasNext()){
				String url = deletions.next();
				db.delete(DatabaseHelper.Tables.SLIDESHOW_ITEM, DatabaseHelper.Columns.URL + " = ?", new String[] {url});
			}

			Iterator<SlideshowListItem> itr = itemList.getInclusionIterator();
			while(itr.hasNext()){
				SlideshowListItem si = (SlideshowListItem)itr.next();
				long did = addOrGetDevice(si);
				addOrGetItem(id, did, si);
			}
			itr = itemList.getExclusionIterator();
			while(itr.hasNext()){
				SlideshowListItem si = (SlideshowListItem)itr.next();
				long did = addOrGetDevice(si);
				addOrGetItem(id, did, si);
			}
			if(itemsChanged) refreshPlaylist();
		}
		
		Toast.makeText(this, getString(R.string.slideshow_saved), Toast.LENGTH_SHORT).show();
		return true;
	}
	
	public long addOrGetDevice(SlideshowListItem si){
		
		long id = -1;
		
		if(si.DeviceUDN == null) return id;
		
		Cursor c = db.rawQuery("SELECT _id FROM Device WHERE udn like ?", new String[] {si.DeviceUDN});
		
		if(c.getCount() > 0){
			c.moveToNext();
			id = c.getLong(0);
		}
		c.close();
		
		if(id >= 0) return id;
		else{
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.Columns.UDN, si.DeviceUDN);
			values.put(DatabaseHelper.Columns.FRIENDLY_NAME, si.DeviceFriendlyName);
			return dbHelper.deviceInsert(values);	
		}
	}
	
	public long addOrGetItem(long sid, long did, SlideshowListItem si){
		
		long id = -1;
		Cursor c = null;
		
		//See iff its already in db
		if(si.ItemID == null){
			c = db.rawQuery("SELECT _id FROM Slideshow_Item WHERE slideshow_id = ? " +
					"AND url like ?", new String[] {Long.toString(sid),si.ItemUrl});			
		}else{
			c = db.rawQuery("SELECT _id FROM Slideshow_Item WHERE slideshow_id = ? " +
									"AND device_id = ? AND oid = ?", new String[] {Long.toString(sid),
																					Long.toString(did),
																					si.ItemID});
		}
			
		if(c.getCount() > 0){
			c.moveToNext();
			id = c.getLong(0);
		}
		c.close();
		
		if(id >= 0) return id;
		else{
			ContentValues values = new ContentValues();
			values.clear();
			values.put(DatabaseHelper.Columns.SLIDESHOW_ID, sid);
			values.put(DatabaseHelper.Columns.DEVICE_ID, did);
			values.put(DatabaseHelper.Columns.LOCATION, si.Location);
			values.put(DatabaseHelper.Columns.OID, si.ItemID);
			values.put(DatabaseHelper.Columns.NAME, si.ItemName);
			values.put(DatabaseHelper.Columns.URL, si.ItemUrl);
			values.put(DatabaseHelper.Columns.ITEM_TYPE, si.ItemType);
			values.put(DatabaseHelper.Columns.IS_EXCLUSION, si.IsExclusion);
			if(si.IsExclusion){
				//Get Inclusion parent for exclusion
				long pid = itemList.getInclusion(si.ExclusionParentUrl).Id;
				values.put(DatabaseHelper.Columns.EXCLUSION_PARENT_ID, pid);
			}
			itemsChanged = true;
			id = dbHelper.slideshowItemInsert(values);
			itemList.setItemId(id, si);
			return id;
		}
	}
	
	public void deleteSlideshow(){
		if(!isNew()){
			dbHelper = DatabaseHelper.getInstance(this);
			db = dbHelper.getReadableDatabase();
        
			db.delete(DatabaseHelper.Tables.SLIDESHOW, "_id = ?", new String[] {Long.toString(id)});
		}
        finish();
	}
	
	public void deleteSlideshowItem(int position){
		DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        long id = adapter.getItemId(position);
        String url = adapter.getItemUrl(position);
        itemList.removeInclusionItem(url);
        if(id > 0)
        	db.delete(DatabaseHelper.Tables.SLIDESHOW_ITEM, "_id = ?", new String[] {Long.toString(id)});
        itemsChanged = true;
        adapter.remove(adapter.getItem(position));
        itemListView.setAdapter(adapter); //refresh
	}
	
	private void refreshPlaylist(){

		try{
			SlideshowPopulateTask task = new SlideshowPopulateTask(this,id);
			task.execute((Void[])null);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void launchAddActivity(){
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle("Select Location"); 
	    CharSequence[] options = new CharSequence[2];
	    options[0] = new String("Device Storage");
	    options[1] = new String("Media Server");
	    builder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	if(whichButton == 0){
	            	Intent intent = new Intent();
	         		intent.setClass(SlideshowActivity.this, SDDirectoryListActivity.class);
	         		SlideshowActivity.this.startActivityForResult(intent, REQ_CODE_ADD_ITEM);
	             } 
	        	if(whichButton == 1){
	            	Intent intent = new Intent();
	         		intent.setClass(SlideshowActivity.this, DeviceListActivity.class);
	         		SlideshowActivity.this.startActivityForResult(intent, REQ_CODE_ADD_ITEM);
	             }
	        	dialog.cancel();
	        }
	    });
	    AlertDialog dialog = builder.create();
	    dialog.show();
	}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		refreshAdapter();
	}

	@Override
	public void onClick(View v) {
		if(v == btnSave){
			saveSlideshow();
			finish();
		}
		if(v == btnAdd){
			launchAddActivity();
		}
		if(v == btnDel){
			deleteSlideshow();
		}
		if(v == btnPlaylist){
			refreshPlaylist();
		}
	}
	
	@Override
	public void onBackPressed (){
		if(saveSlideshow())
			this.finish();
	}
	
	private void refreshAdapter(){
		adapter = new SlideshowItemAdapter(this,itemList.getInclusionArray());
        itemListView.setAdapter(adapter);
	}
	
}