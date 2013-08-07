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
import java.util.Hashtable;
import java.util.Iterator;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SlideshowItemList {
	
	public static final String KEY_SLIDESHOW_ITEM_ADAPTER = "SlideshowItemsAdapter";
	
	private Hashtable<String, SlideshowListItem> Items = new Hashtable<String, SlideshowListItem>(); 			//All items - inclusions and exclusions, keyed by url
	private Hashtable<String, SlideshowListItem> ExclusionItems = new Hashtable<String, SlideshowListItem>();
	private Hashtable<String, SlideshowListItem> InclusionItems = new Hashtable<String, SlideshowListItem>();	//Inclusion items only
	private Hashtable<Long, SlideshowListItem> ItemsById = new Hashtable<Long, SlideshowListItem>(); 			//All items - inclusions and exclusions, keyed by url
	private Hashtable<Long, ArrayList<SlideshowListItem>> Children = new Hashtable<Long, ArrayList<SlideshowListItem>>();		//Child to Parent relationship links
	private ArrayList<String> Deletions = new ArrayList<String>();
	
	public SlideshowItemList(){
		
	}
	
	public SlideshowItemList(Context cxt, long SlideshowId){
		//Load it in from the database
		DatabaseHelper dbHelper = DatabaseHelper.getInstance(cxt);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
       
        Cursor cursor = db.rawQuery("SELECT si._id as _id, si.location as location, si.name as name, device_id, udn, friendly_name, " +
        			    "oid, url, item_type, slideshow_id, is_exclusion, exclusion_parent_id " +
        			    "FROM Slideshow_item si LEFT OUTER JOIN Device d ON si.device_id = d._id " +
        			    "WHERE slideshow_id = ? ORDER BY is_exclusion asc", new String[]{Long.toString(SlideshowId)});
        cursor.moveToFirst();

		while(!cursor.isAfterLast()){
			SlideshowListItem item = new SlideshowListItem();
			item.Id = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.Columns.ID));
			item.Location = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.Columns.LOCATION));
			item.DeviceId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.Columns.DEVICE_ID));
			item.DeviceUDN = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Columns.UDN));
			item.DeviceFriendlyName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Columns.FRIENDLY_NAME));
			item.SlideshowId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.Columns.SLIDESHOW_ID));
			item.ItemID = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Columns.OID));
			item.ItemName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Columns.NAME));
			item.ItemUrl = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Columns.URL));
			item.ItemType = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.Columns.ITEM_TYPE));
			item.IsExclusion = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.Columns.IS_EXCLUSION)) > 0 ? true : false;
			item.ExclusionParentId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.Columns.EXCLUSION_PARENT_ID));
			if(item.IsExclusion)
			{
				item.ExclusionParentUrl = getItemById(item.ExclusionParentId).ItemUrl;
				addChild(item.ExclusionParentId,item);
				addExclusionItem(item.DeviceUDN != null ? item.ItemID : item.ItemUrl, item);
			}
			else
				addInclusionItem(item.DeviceUDN != null ? item.ItemID : item.ItemUrl,item);
			cursor.moveToNext(); 
        }
		cursor.close();
	}
	
	public SlideshowListItem getItem(String key){
		return Items.get(key);
	}

	public SlideshowListItem getInclusion(String key){
		return InclusionItems.get(key);
	}

	public SlideshowListItem getExclusion(String key){
		return ExclusionItems.get(key);
	}
	
	public void addInclusionItem(String key, SlideshowListItem item){
		ItemsById.put(item.Id, item);
		Items.put(key, item);
		InclusionItems.put(key, item);
	}
	
	public void addExclusionItem(String key, SlideshowListItem item){
		ItemsById.put(item.Id, item);
		Items.put(key, item);
		ExclusionItems.put(key, item);
	}
	
	public void addInclusionSDItem(SDItem item){
		SlideshowListItem si = new SlideshowListItem(item);
		item.isSelected = true;
		//Won't have an Id yet
		Items.put(si.ItemUrl,si);
		InclusionItems.put(si.ItemUrl, si);
	}
	
	public void addExclusionSDItem(SDItem item, String parent){
		SlideshowListItem si = new SlideshowListItem(item);
		si.IsExclusion = true;
		si.ExclusionParentUrl = parent;
		Items.put(si.ItemUrl,si);
		ExclusionItems.put(si.ItemUrl, si);
	}

	public void addInclusionDLNAItem(DLNAItem item){
		SlideshowListItem si = new SlideshowListItem(item);
		item.isSelected = true;
		Items.put(si.ItemID, si);
		InclusionItems.put(si.ItemID, si);
	}
	
	public void addExclusionDLNAItem(DLNAItem item, String parent){
		SlideshowListItem si = new SlideshowListItem(item);
		si.IsExclusion = true;
		si.ExclusionParentUrl = parent;
		Items.put(si.ItemID, si);
		ExclusionItems.put(si.ItemID, si);
	}

	
	public void removeExclusionItem(String key){
		SlideshowListItem si = Items.get(key);
		if(si != null){
			if(si.Id > 0) ItemsById.remove(si.Id);
			Items.remove(key);
			ExclusionItems.remove(key);
		}
	}
	
	public void removeInclusionItem(String key){
		SlideshowListItem si = Items.get(key);
		if(si != null){
			if(si.Id > 0) ItemsById.remove(si.Id);
			Items.remove(key);
			InclusionItems.remove(key);
			Deletions.add(key);
		}
	}
	
	public ArrayList<SlideshowListItem> getInclusionArray(){
		return new ArrayList<SlideshowListItem>(InclusionItems.values());
	}
	
	public int size(){
		return Items.size();
	}
	
	public Iterator<SlideshowListItem> getInclusionIterator(){
		return InclusionItems.values().iterator();
	}
	
	public Iterator<SlideshowListItem> getExclusionIterator(){
		return ExclusionItems.values().iterator();
	}
	
	public void setItemId(long id, SlideshowListItem si){
		si.Id = id;
		ItemsById.put(si.Id, si);
	}
	
	public SlideshowListItem getItemById(long id){
		return ItemsById.get(id);
	}
	
	public void addChild(long id, SlideshowListItem item){
		ArrayList<SlideshowListItem> items = Children.get(id);
		if(items == null){
			items = new ArrayList<SlideshowListItem>();
			Children.put(id, items);
		}
		items.add(item);
	}
	
	public Iterator<String> getDeletionsIterator(){
		return Deletions.iterator();
	}
}
