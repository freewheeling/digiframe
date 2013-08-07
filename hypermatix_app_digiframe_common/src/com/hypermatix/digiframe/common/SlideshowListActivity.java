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

import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.hypermatix.digiframe.common.R;

import android.os.Bundle;

public class SlideshowListActivity extends ListActivity implements View.OnClickListener {
	
	private SlideshowListAdapter adapter;
	private Cursor cursor;
	private DatabaseHelper dbHelper;
	private Button btnNew;
	
	private static final HashMap<String, String> sSlideshowProjectionMap;
	
	public static final int REQ_CODE_SETTINGS = 1;
	
	public static final int DLG_HELP = 1; 
	public static final int DLG_PURCHASE_UNLOCK = 2;
	public static final int DLG_ABOUT = 3; 
	
	static {
		sSlideshowProjectionMap = new HashMap<String, String>();
		sSlideshowProjectionMap.put(DatabaseHelper.Columns.ID,"_id");
		sSlideshowProjectionMap.put(DatabaseHelper.Columns.NAME, "name");
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.slideshows);
        
        btnNew = (Button)findViewById(R.id.create_slideshow);
        btnNew.setOnClickListener(this);
        
        dbHelper = DatabaseHelper.getInstance(this);
       
        refreshList();
        
        this.setTitle(getString(R.string.window_title_slideshows));
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);
        
    }

	@Override
	public void onClick(View v) {
		if(v == btnNew){
			createSlideshow();
		}
	}
	
	private void createSlideshow(){
		Intent intent = new Intent();
		intent.setClass(this, SlideshowActivity.class);
		startActivityForResult(intent, REQ_CODE_SETTINGS);
	}
	
	@Override
	protected void onActivityResult(int reqcode, int res, Intent data){
		if(reqcode == REQ_CODE_SETTINGS){
			refreshList();
		}
	}
	
	@Override
    protected void onResume(){
		super.onResume();
		if(adapter != null)
			adapter.isVisible = true;
	}

	@Override
    protected void onPause(){
		super.onPause();
		if(adapter != null)
			adapter.isVisible = false;
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		this.stopManagingCursor(cursor);
		cursor.close();
		//if (dbHelper != null) dbHelper.close();
	}
	
	private void refreshList(){
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		if(cursor != null){
			stopManagingCursor(cursor);
			cursor.close();
		}
		cursor = db.rawQuery("SELECT Slideshow._id as _id, Slideshow.name as name, COUNT(Play_Item.slideshow_id) as items FROM Slideshow LEFT OUTER JOIN Play_Item ON Slideshow._id = Play_Item.slideshow_id GROUP BY Slideshow._id", null); 
       
		startManagingCursor(cursor);
		adapter = new SlideshowListAdapter(this,cursor);
		setListAdapter(null);
        setListAdapter(adapter);
	}
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu){
		menu.setGroupVisible(1, true);
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu (Menu menu){

		MenuItem item = menu.add(1, 1, 1, R.string.create);
		item.setIcon(android.R.drawable.ic_menu_add);
		
		item = menu.add(1,2,2, R.string.preferences);
		item.setIcon(android.R.drawable.ic_menu_preferences);
    	
    	item = menu.add(1,3,3, R.string.about);
    	item.setIcon(android.R.drawable.ic_menu_info_details);

    	
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		if(item.getItemId() == 1){
			createSlideshow();
		}
		if(item.getItemId() == 2)
    	{
			Utils.startActivity(this, ConfigActivity.class.getName());
    	}
		if(item.getItemId() == 3)
    	{
			showDialog(DLG_ABOUT); 
    	}
		return true;
	}
	
	@Override
	public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
		
	}
	
	@Override
    protected Dialog onCreateDialog(int id) {
    	switch(id){
    		case DLG_ABOUT: {   			
    			
    			String html = Utils.readAssetFile(this,"about.html");
    			html = String.format(html, getString(R.string.app_name), Utils.getPackageVersionName(this));
    			Spanned spannedText = Html.fromHtml(html);
    			
    			//Put in text view, and activate web links
    			TextView view = new TextView(this);
    			view.setText(spannedText, BufferType.SPANNABLE);
    			view.setGravity(Gravity.CENTER);
    			view.setPadding(10, 10, 10, 10);
    			Linkify.addLinks( view, Linkify.WEB_URLS );
    			
    			//Create and return dialog
                return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.about)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	//
                    }
                })
                .setView(view)
                .create();
    		}
    		default: throw new AssertionError("Unknown dialog");
    	}

	}
}