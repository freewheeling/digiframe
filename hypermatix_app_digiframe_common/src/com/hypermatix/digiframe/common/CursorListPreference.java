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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class CursorListPreference extends ListPreference {

	public CursorListPreference(Context context) {
		super(context);
	}

	public CursorListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setData(String TableName, String DisplayColumnName) {
		DatabaseHelper dbHelper = DatabaseHelper.getInstance(this.getContext());
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();       
	     
	    CharSequence[] lEntries;
	    CharSequence[] lEntryValues;
	     
	    Cursor lCursor = db.query(
	        TableName, //Table to select from
	        new String[]{"_id", DisplayColumnName}, //Column to retreive
	        null, null, null, null, 
	        DisplayColumnName); //Sorting
	    if ((lCursor.getCount() == 0) || !lCursor.moveToFirst()) {
	        lEntries = new CharSequence[]{};
	        lEntryValues = new CharSequence[]{};
	    } else {
	        lCursor.moveToFirst();
	        lEntries = new CharSequence[lCursor.getCount()];
	        lEntryValues = new CharSequence[lCursor.getCount()];
	        int i = 0;
	        do {
	            lEntries[i] = lCursor.getString(lCursor.getColumnIndex(DisplayColumnName));
	            lEntryValues[i] = lCursor.getString(lCursor.getColumnIndex("_id"));
	            ++i;
	        } while (lCursor.moveToNext());
	    }
	    
	    lCursor.close();
	    
	    this.setEntries(lEntries);
	    this.setEntryValues(lEntryValues);
	     
	    dbHelper.close();
	}
}
