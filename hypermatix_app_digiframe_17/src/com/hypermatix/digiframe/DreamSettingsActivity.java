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

package com.hypermatix.digiframe;

import com.hypermatix.digiframe.common.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import com.hypermatix.digiframe.common.*;

public class DreamSettingsActivity extends PreferenceActivity
							implements OnSharedPreferenceChangeListener,
									   OnPreferenceClickListener{
	
    // The name of the shared preferences file. This name must be maintained for historical
    // reasons, as it's what PreferenceManager assigned the first time the file was created.
    private static final String SHARED_PREFS_NAME = "com.hypermatix.digiframe.preferences";
    
    // Preference keys
    static final String KEY_DREAM_SLIDESHOW = "preferences_dream_slideshow";
    static final String KEY_DREAM_PICTURES_ONLY = "preferences_dream_pictures_only";

    private CursorListPreference slideshowPref;
    
    /** Set the default shared preferences in the proper context */
    public static void setDefaultValues(Context context) {
        PreferenceManager.setDefaultValues(context, SHARED_PREFS_NAME, Context.MODE_PRIVATE,
                R.xml.preferences, false);
    }

    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Make sure to always use the same preferences file regardless of the package name
        // we're running under
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(SHARED_PREFS_NAME);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.dream_preferences);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        
        slideshowPref = (CursorListPreference) preferenceScreen.findPreference("preferences_dream_slideshow");
        slideshowPref.setData("slideshow", "name");
        
        long id = DreamSettingsActivity.getPreferenceSlideshowId(this);
        String name = DreamSettingsActivity.getPreferenceSlideshowName(this);
        
        slideshowPref.setValue(Long.toString(id));
        slideshowPref.setSummary(name);
        if(id <= 0){
        	slideshowPref.setSummary(getString(R.string.no_slideshows));
        }
    }

	@Override
	public boolean onPreferenceClick(Preference arg0) {
		return false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals(KEY_DREAM_SLIDESHOW)){
			slideshowPref.setSummary(DreamSettingsActivity.getPreferenceSlideshowName(this));
		}
		
	}

	public static class SlideshowInfo{
		public SlideshowInfo() {};
		long id;
		String name;
	}
	
	public static long getPreferenceSlideshowId(Context cxt){
		SlideshowInfo s = DreamSettingsActivity.getPreferenceSlideshow(cxt);
		return s.id;
	}

	public static String getPreferenceSlideshowName(Context cxt){
		SlideshowInfo s = DreamSettingsActivity.getPreferenceSlideshow(cxt);
		return s.name;
	}
	
	private static SlideshowInfo getPreferenceSlideshow(Context cxt){
		SlideshowInfo s = new SlideshowInfo();
		SharedPreferences prefs = ConfigActivity.getSharedPreferences(cxt);
        try{
		s.id = Long.parseLong(prefs.getString(KEY_DREAM_SLIDESHOW, null));
        }catch(NumberFormatException e){
        	s.id = -1;
        }
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(cxt);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
        
		Cursor lCursor;
		
        if(s.id <= 0){
        	//Not set, so pick first
        	lCursor = db.query(
        	        "slideshow", 
        	        new String[]{"_id", "name"}, 
        	        null, null, null, null, 
        	        null);
        	if(lCursor.getCount() > 0){ 
        		lCursor.moveToFirst();
        		s.id = lCursor.getLong(lCursor.getColumnIndex("_id"));
        		s.name = lCursor.getString(lCursor.getColumnIndex("name"));
        	}
        	
        	lCursor.close();
        }else{
        	//Have an id, get slideshow name
        	lCursor = db.query(
        	        "slideshow", 
        	        new String[]{"_id", "name"}, 
        	        "_id = ?", new String[]{Long.toString(s.id)}, null, null, 
        	        null);
        	if(lCursor.getCount() > 0){ 
        		lCursor.moveToFirst();
        		s.name = lCursor.getString(lCursor.getColumnIndex("name"));
        	}else{
        		//Slideshow has been deleted
        		lCursor.close();
        		lCursor = db.query(
            	        "slideshow", 
            	        new String[]{"_id", "name"}, 
            	        null, null, null, null, 
            	        null);
            	if(lCursor.getCount() > 0){ 
            		lCursor.moveToFirst();
            		s.id = lCursor.getLong(lCursor.getColumnIndex("_id"));
            		s.name = lCursor.getString(lCursor.getColumnIndex("name"));
            	}
	
        	}
        	
        	lCursor.close();
        }
        		       	    
	     
	    dbHelper.close();
	    return s;
	}
}
