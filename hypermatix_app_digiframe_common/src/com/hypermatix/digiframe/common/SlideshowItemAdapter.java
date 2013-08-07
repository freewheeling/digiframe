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

import com.hypermatix.digiframe.common.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class SlideshowItemAdapter extends ArrayAdapter<SlideshowListItem>
								  {

	private LayoutInflater mInflater;
	private SlideshowActivity mActivity;
	private Cursor cursor;
	private ArrayList<SlideshowListItem> itemList = new ArrayList<SlideshowListItem>();
	private static final int textViewResId = 0;
	private int pos;
	
	private class DeleteClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			pos = (Integer)v.getTag();
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity).
				setCancelable(true).
				setTitle(mActivity.getString(R.string.delete)).
				setMessage(R.string.confirm).
				setInverseBackgroundForced(true).
				setPositiveButton(mActivity.getString(android.R.string.yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						mActivity.deleteSlideshowItem(pos);
					}
					}).
				setNegativeButton(mActivity.getString(android.R.string.no), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
			AlertDialog alert = builder.create();
			alert.show();

		}
		
	}
	
	public SlideshowItemAdapter(SlideshowActivity activity, ArrayList<SlideshowListItem> itemList){
		super(activity, textViewResId);
		mActivity = activity;
		mInflater = LayoutInflater.from(mActivity);
		this.itemList = itemList;
	}
	
	@Override
	public int getCount() {
		if(cursor != null) return cursor.getCount();
		else return itemList == null ? 0 : itemList.size();
	}

	@Override
	public SlideshowListItem getItem(int position){
		if((itemList == null || itemList.size() <= 0)){ 
			return null;
		}else{
			return itemList.get(position);
		}
	}

	@Override
	public long getItemId(int idx) {
		if((itemList == null || itemList.size() <= 0) &&
				(cursor == null || cursor.getCount() <= 0)){
			return -1;
		}else if(cursor != null){
			cursor.moveToPosition(idx);
			return cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.Columns.ID));
		}else{
			if(itemList.get(idx) == null) return -1;
			else{
				return ((SlideshowListItem)itemList.get(idx)).Id;
			}
		}
	}
	
	public String getItemUrl(int idx) {
		if((itemList == null || itemList.size() <= 0) &&
				(cursor == null || cursor.getCount() <= 0)){
			return null;
		}else if(cursor != null){
			cursor.moveToPosition(idx);
			return cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Columns.URL));
		}else{
			if(itemList.get(idx) == null) return null;
			else{
				return ((SlideshowListItem)itemList.get(idx)).ItemUrl;
			}
		}
	}

	@Override
	public int getItemViewType(int idx) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.slideshow_item, null);
        }
        
        TextView text = (TextView) convertView.findViewById(R.id.text);
        
        ImageButton btnDelete = (ImageButton) convertView.findViewById(R.id.btn_slideshow_item_delete);
        
        btnDelete.setTag(position); btnDelete.setOnClickListener(new DeleteClickListener());
        
        SlideshowListItem si = null;
        
        si = (SlideshowListItem)itemList.get(position);
                
        if(si.DeviceFriendlyName == null)
        	text.setText(si.ItemName);
        else
        	text.setText(si.ItemName + " [" + si.DeviceFriendlyName + "]");
        
        return convertView;
	}

	@Override
	public void add(SlideshowListItem item){
		itemList.add(item);
	}
	
	@Override
	public void remove(SlideshowListItem item){
		itemList.remove(item);
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