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

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper{

	private static final String TAG = "DigiFrameDatabaseHelper";
	
	private static final String DATABASE_NAME = "digiframe.db";
	
	// Note: if you update the version number, you must also update the code
    // in upgradeDatabase() to modify the database (gracefully, if possible).
    static final int DATABASE_VERSION = 4;
	
	public static final int ITEM_TYPE_FOLDER = 1;
	public static final int ITEM_TYPE_AUDIO = 2;
	public static final int ITEM_TYPE_IMAGE = 3;
	
	private static DatabaseHelper sSingleton;
	
	private DatabaseUtils.InsertHelper mSlideshowInserter;
	private DatabaseUtils.InsertHelper mDeviceInserter;
	private DatabaseUtils.InsertHelper mSlideshowItemInserter;
	private DatabaseUtils.InsertHelper mPlayItemInserter;
	
	public static final int LOCATION_FILE_SYSTEM = 1;
	public static final int LOCATION_MEDIA_SERVER = 2;
	
	public interface Views {
	      public static final String ITEMS = "Items";
	    }
	
	public interface Tables {
	      public static final String SLIDESHOW = "Slideshow";
	      public static final String DEVICE = "Device";
	      public static final String SLIDESHOW_ITEM = "Slideshow_Item";
	      public static final String PLAY_ITEM = "Play_Item";
	    }
	
	public interface Columns {
	      public static final String ID = "_id";
	      public static final String NAME = "name";
	      public static final String SLIDESHOW_ID = "slideshow_id";
	      public static final String SLIDESHOW_ITEM_ID = "slideshow_item_id";
	      public static final String UDN = "udn";
	      public static final String FRIENDLY_NAME = "friendly_name";
	      public static final String DEVICE_ID = "device_id";
	      public static final String OID = "oid";
	      public static final String URL = "url";
	      public static final String ITEM_TYPE = "item_type";
	      public static final String PLAYLIST_CREATED = "playlist_created";
	      public static final String PLAYLIST_CREATED_DATE = "playlist_created_date";
	      public static final String SYNC_ID = "sync_id";
	      public static final String LOCATION = "location";
	      public static final String LANDSCAPE = "landscape";
	      public static final String WIDTH = "width";
	      public static final String HEIGHT = "height";
	      public static final String EXCLUSION_PARENT_ID = "exclusion_parent_id";
	      public static final String IS_EXCLUSION = "is_exclusion";
	}
	
	public DatabaseHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}
	
	/* package */ DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	public static synchronized DatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new DatabaseHelper(context);
        }
        return sSingleton;
    }
	
	public long slideshowInsert(ContentValues values) {
        return mSlideshowInserter.insert(values);
    }
	
	public long slideshowItemInsert(ContentValues values) {
        return mSlideshowItemInserter.insert(values);
    }
	
	public long playItemInsert(ContentValues values) {
        return mPlayItemInserter.insert(values);
    }
	
	public long deviceInsert(ContentValues values) {
        return mDeviceInserter.insert(values);
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		bootstrapDB(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion == 1 && newVersion == 2){
			db.execSQL("ALTER TABLE Play_Item ADD COLUMN width INTEGER;");
			db.execSQL("ALTER TABLE Play_Item ADD COLUMN height INTEGER;");
		}
		if(oldVersion == 2 && newVersion == 3){
			//Oops - forgot to test new install of v2 (missing commsa in create stmt)
			try{
				db.execSQL("ALTER TABLE Play_Item ADD COLUMN width INTEGER;");
				db.execSQL("ALTER TABLE Play_Item ADD COLUMN height INTEGER;");
			}catch (Exception e){
				//Ignore
			}
		}
		if(oldVersion < 4 && newVersion == 4){
			db.execSQL("DROP TRIGGER item_cleanup");
			db.execSQL("CREATE TRIGGER item_cleanup DELETE ON Slideshow " +
	                "BEGIN " +
	                "DELETE FROM Slideshow_Item WHERE slideshow_id = old._id;" +
	                "DELETE FROM Slideshow_Item WHERE parent_id = old._id;" +
	                "DELETE FROM Play_Item WHERE slideshow_id = old._id;" +
	                "END");
			db.execSQL("ALTER TABLE Slideshow_Item ADD COLUMN exclusion_parent_id INTEGER;");
			db.execSQL("ALTER TABLE Slideshow_Item ADD COLUMN is_exclusion BOOLEAN;");
		}
	}
	
    @Override
    public void onOpen(SQLiteDatabase db) {

        mSlideshowInserter = new DatabaseUtils.InsertHelper(db, Tables.SLIDESHOW);
        mDeviceInserter = new DatabaseUtils.InsertHelper(db, Tables.DEVICE);
        mSlideshowItemInserter = new DatabaseUtils.InsertHelper(db, Tables.SLIDESHOW_ITEM);
        mPlayItemInserter = new DatabaseUtils.InsertHelper(db, Tables.PLAY_ITEM);

    }
	
	private void bootstrapDB(SQLiteDatabase db) {
    	android.util.Log.i(TAG, "Bootstrapping database");
    	
        db.execSQL("CREATE TABLE Slideshow (" +
                "_id INTEGER PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "playlist_created INTEGER, " +
                "playlist_created_date DATETIME, " +
                "requires_wifi INTEGER, " +
                "requires_sdcard INTEGER" +
                ");");
        
        db.execSQL("CREATE TABLE Device (" +
                "_id INTEGER PRIMARY KEY," +
                "udn TEXT NOT NULL," +
                "friendly_name TEXT" +
                ");");
        
        db.execSQL("CREATE TABLE Slideshow_Item (" +
                "_id INTEGER PRIMARY KEY," +
                "slideshow_id INTEGER NOT NULL," +
                "location INTEGER NOT NULL, " + //FILE_SYSTEM or MEDIA_SERVER
                "device_id INTEGER," +
                "oid TEXT," + //Can be null for file item (use url instead)
                "url TEXT," + 
                "media_uid TEXT, " + //The unique identifier (if available) of media card
                "name TEXT NOT NULL," + 
                "item_type INTEGER NOT NULL, " +
                "sync_id TEXT, " +
                "exclusion_parent_id INTEGER, " +
                "is_exclusion BOOLEAN " +
                ");");
        
        String itemsSelect = "SELECT "
        	+ Tables.SLIDESHOW_ITEM + "." + Columns.ID + " AS " + Columns.SLIDESHOW_ITEM_ID + ","
        	+ Columns.DEVICE_ID + ","
        	+ Columns.SLIDESHOW_ID + ","
        	+ Columns.UDN + ","
        	+ Columns.FRIENDLY_NAME + ","
        	+ Columns.OID + ","
        	+ Columns.URL + ","
        	+ Columns.NAME + ","
        	+ Columns.ITEM_TYPE + ","
        	+ Columns.LOCATION
        	+ " FROM " + Tables.SLIDESHOW_ITEM + " LEFT OUTER JOIN " + Tables.DEVICE
            + " ON (" + Tables.DEVICE + "." + Columns.ID
            + "=" + Tables.SLIDESHOW_ITEM + "." + Columns.DEVICE_ID
            + ")";

        db.execSQL("CREATE VIEW " + Views.ITEMS + " AS " + itemsSelect);
        
        db.execSQL("CREATE TABLE Play_Item (" +
                "_id INTEGER PRIMARY KEY," +
                "slideshow_id INTEGER NOT NULL," +
                "location INTEGER NOT NULL, " + //FILE_SYSTEM or NETWORK
                "oid TEXT," +
                "url TEXT NOT NULL," + 
                "item_type INTEGER NOT NULL," +
                "landscape BOOLEAN," +
                "width INTEGER," +
                "height INTEGER" +
                ");");
        
        db.execSQL("CREATE TRIGGER item_cleanup DELETE ON Slideshow " +
                "BEGIN " +
                "DELETE FROM Slideshow_Item WHERE slideshow_id = old._id;" +
                "DELETE FROM Slideshow_Item WHERE parent_id = old._id;" +
                "DELETE FROM Play_Item WHERE slideshow_id = old._id;" +
                "END");
                
	}

}