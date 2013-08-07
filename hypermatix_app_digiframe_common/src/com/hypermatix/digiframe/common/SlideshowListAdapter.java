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
import java.util.Iterator;

import com.hypermatix.digiframe.common.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SlideshowListAdapter extends CursorAdapter
implements SlideshowPopulateTask.SlideshowPopulateListener{

	private LayoutInflater mInflater;
	private Activity mContext;
	private ArrayList<View> views = new ArrayList<View>();
	public boolean isVisible;
	
	private class SettingsClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			long id = getItemId((Integer)v.getTag());
			Intent intent = new Intent(mContext, SlideshowActivity.class);
			intent.putExtra(SlideshowActivity.EXT_ID, id);
			mContext.startActivityForResult(intent,SlideshowListActivity.REQ_CODE_SETTINGS);
		}
		
	}
	
	private class PlayClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			long id = getItemId((Integer)v.getTag());
			Intent intent = new Intent(mContext, PlaybackActivity.class);
			intent.putExtra("SlideshowID", id);
			mContext.startActivity(intent);
		}
		
	}
	
	public SlideshowListAdapter(Activity context, Cursor cursor){
		super(context, cursor);
		mContext = context;
		mInflater = LayoutInflater.from(context);
		SlideshowPopulateTask.registerListener(this);
	}
	
	@Override
	public int getCount() {
		return getCursor() == null ? 0 : getCursor().getCount();
	}

	@Override
	public long getItemId(int idx) {
		if(getCursor() == null || getCursor().getCount() <= 0){
			return -1;
		}else{
			getCursor().moveToPosition(idx);
			return getCursor().getInt(getCursor().getColumnIndexOrThrow(DatabaseHelper.Columns.ID));
		}
	}

	@Override
	public int getItemViewType(int idx) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.slideshow_list_item, null);
        }
        
        TextView text = (TextView) convertView.findViewById(R.id.text);
        TextView items = (TextView) convertView.findViewById(R.id.items);
        ImageButton btnSettings = (ImageButton) convertView.findViewById(R.id.btn_settings_slideshow);
        ImageButton btnPlay = (ImageButton) convertView.findViewById(R.id.btn_play_slideshow);
        
        btnSettings.setTag(position); btnSettings.setOnClickListener(new SettingsClickListener());
        btnPlay.setTag(position); btnPlay.setOnClickListener(new PlayClickListener());
        
        if(getCursor().isClosed()) return null;
        
        getCursor().moveToPosition(position);
        
        long slideshow_id = getCursor().getLong(getCursor().getColumnIndexOrThrow(DatabaseHelper.Columns.ID));

        
        text.setText(getCursor().getString(getCursor().getColumnIndexOrThrow(DatabaseHelper.Columns.NAME)));
        items.setText(String.format(mContext.getString(R.string.num_items), 
        			getCursor().getString(getCursor().getColumnIndexOrThrow("items"))));

        ProgressBar pb = (ProgressBar)convertView.findViewById(R.id.progressbar);
        if(SlideshowPopulateTask.hasTaskForSlideshow(slideshow_id)){
        	pb.setVisibility(View.VISIBLE);
        }else{
        	pb.setVisibility(View.GONE);
        }

        convertView.setTag(slideshow_id);
        
        if(!views.contains(convertView)) views.add(convertView);
        
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
		return getCursor() == null ? true : getCursor().getCount() <= 0;
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

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return null;
	}

	public View getViewForSlideshow(long SlideshowID){
		Iterator<View> it = views.iterator();
		while(it.hasNext()){
			View view = it.next();
			if(view.getTag().equals(SlideshowID))
				return view;
		}
		return null;
	}
	
	protected void finalize() throws Throwable{
		SlideshowPopulateTask.deregisterListener(this);
		super.finalize();
	}

	@Override
	public void progressUpdate(long SlideshowID, int ItemCount) {
		if(isVisible){
			View view = this.getViewForSlideshow(SlideshowID);
			if(view != null){
				TextView items = (TextView) view.findViewById(R.id.items);
				items.setText(String.format(mContext.getString(R.string.num_items),ItemCount));
			}
		}
	}

	@Override
	public void finished(long SlideshowID) {
		if(isVisible){
			View view = this.getViewForSlideshow(SlideshowID);
			if(view != null){
				ProgressBar pb = (ProgressBar)view.findViewById(R.id.progressbar);
	        	pb.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void startPopulating(long SlideshowID) {
		if(isVisible){
			View view = this.getViewForSlideshow(SlideshowID);
			if(view != null){
				ProgressBar pb = (ProgressBar)view.findViewById(R.id.progressbar);
	        	pb.setVisibility(View.VISIBLE);
			}
		}
	}
	
}