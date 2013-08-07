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

import java.util.List;

import com.hypermatix.digiframe.common.R;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.TextView;

public class DirectoryListAdapter implements ListAdapter {

	private LayoutInflater mInflater;
	private List<DirectoryItem> list;
	private DirectoryListActivity activity;
	
	public DirectoryListAdapter(Context context, String directoryId, DirectoryListActivity activity){
		mInflater = LayoutInflater.from(context);
		this.activity = activity;
	}
	
	public void setList(List<DirectoryItem> list){
		this.list = list;
	}
	
	public List<DirectoryItem> getList(){
		return list;
	}
	
	@Override
	public int getCount() {
		return list == null ? 0 : list.size();
	}

	@Override
	public Object getItem(int idx) {
		return list == null ? null : list.get(idx);
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
            convertView = mInflater.inflate(R.layout.directory_list_item, null);
        }
        
        convertView.setOnClickListener(activity);
        convertView.setTag(position);
        
        TextView text = (TextView) convertView.findViewById(R.id.text);
        TextView itemClass = (TextView) convertView.findViewById(R.id.item_class);
        TextView itemURI = (TextView) convertView.findViewById(R.id.item_uri);
        
        CheckBox check = (CheckBox) convertView.findViewById(R.id.item_check);
        
        check.setOnClickListener(activity);

        DirectoryItem item = (DirectoryItem)getItem(position);
        
        check.setTag(item);
        text.setText(item.Title);
        itemClass.setText(item.ItemClass);
        itemURI.setText(item.URI);

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