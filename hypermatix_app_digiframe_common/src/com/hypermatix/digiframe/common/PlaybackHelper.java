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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Random;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.HttpFile;
import org.jaudiotagger.audio.mp3.MP3File;

import com.hypermatix.digiframe.common.DatabaseHelper.Tables;
import com.hypermatix.net.NetworkHelper;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ViewAnimator;
import android.widget.ImageView.ScaleType;


public abstract class PlaybackHelper implements MediaPlayer.OnPreparedListener,
												MediaPlayer.OnCompletionListener,
												MediaPlayer.OnInfoListener,
												AnimationListener,
												OnBufferingUpdateListener, 
												OnErrorListener,
												OnSeekCompleteListener
												{
	
	private static final int PHOTO_DELAY_SECONDS = 25;
	
	private static final int MILLIS_IN_HOUR = 1000 * 60 * 60;
	private static final int MILLIS_IN_MIN = 1000 * 60;
	private static final int MILLIS_IN_SEC = 1000;
	
	private static final String ZERO_STRING = "0:00";
	
	protected boolean firstPicture = true;
	protected TrackThread trackThread;
	protected AnimThread photoAnimThread;
	protected ViewAnimator va;
	protected ImageView iv1, iv2;
	protected PositionThread positionThread;
	protected Bitmap bmPhoto1, bmPhoto2;
	protected ImageView vs;
	protected LinearLayout progress;
	protected LinearLayout no_wifi;
	protected LinearLayout no_card;
	protected LinearLayout error;
	protected LinearLayout splash;
	protected PowerManager.WakeLock wakelock;
	protected WifiManager.WifiLock wifi_wakelock;
	protected AlphaAnimation outAnim;
	protected AlphaAnimation inAnim;
	protected boolean isMusicPaused = false;
	protected boolean isPicturePaused = false;
	protected long slideshow_id;
	protected boolean pictures_only = false;
	
	protected int numCardItems = 0;
	protected int numWifiItems = 0;
	protected int numPictures = 0;
	protected int numTracks = 0;
	
	protected Animation picInAnim;
	protected Animation picOutAnim;
	
	protected int displayWidth;
	protected int displayHeight;
	protected boolean landscape;
	
	protected static MediaPlayer mp;
	protected DatabaseHelper dbHelper;
	protected SQLiteDatabase db;
	protected Random randGen;
	
	protected String currentTrackUrl;
	protected MP3Info mp3info;
	
	private Context cxt;
	private PlaybackHelperCallback callback;
	
	private int currImg = 1;
	
	public PlaybackHelper(PlaybackHelperCallback callback){
		this.callback = callback;
	}
	
	private class Item {
		String url;
		int width;
		int height;
		
		public Item(String url, int width, int height)
		{ this.url =url;
		  this.width = width;
		  this.height = height;
		}
	}
	
	private class AnimThread extends Thread{
		
		public volatile boolean newPicNow = false;
		public volatile boolean loadingPic = false;
		
		@Override
		public void run(){
			while(true){
				try{
					if(!isPicturePaused || newPicNow){
						fetchPhoto(getRandomItem(DatabaseHelper.ITEM_TYPE_IMAGE),newPicNow);
					}
				}catch(Exception e1){
					e1.printStackTrace();
				}catch(OutOfMemoryError oe){
					Log.w("DigiFrame","OutOfMemory Loading Bitmap");
					newPicNow = false;
					System.gc();
					fetchPhoto(getRandomItem(DatabaseHelper.ITEM_TYPE_IMAGE),newPicNow);
				}finally{
					newPicNow = false;
				}
				try {
					sleep(PHOTO_DELAY_SECONDS * MILLIS_IN_SEC);
				} catch (InterruptedException e) {
					if(!newPicNow){
						// Quitting
						break;
					}
				}
			}
			
		}
	}
	
	private class TrackThread extends Thread{
		
		public volatile boolean playNewTrack = true;
		
		@Override
		public void run(){
			while(true){
				if(playNewTrack){
					playNewTrack = false;
					if(isTrackPlaying()) stopTrack();
					playTrack(getRandomItem(DatabaseHelper.ITEM_TYPE_AUDIO));
				}
				try {
					sleep(500);
				} catch (InterruptedException e) {
					if(isTrackPlaying()) stopTrack();
					break; //Quit
				}
			}
		}
	}
	
	private class PositionThread extends Thread{
		
		public volatile boolean doUpdate = true;
		
		@Override
		public void run(){
			while(true){
				try{
					if(doUpdate){
						if(mp == null){
							callback.OnTrackTimeUpdate(0,0,ZERO_STRING,ZERO_STRING);
						}
						else{
							if(mp.isPlaying()){
								int pos = mp.getCurrentPosition();
								callback.OnTrackTimeUpdate(mp.getCurrentPosition(),
										mp.getDuration(),
										getDurationString(mp.getDuration() - pos),
										getDurationString(pos));
							}
						}
					}
				}catch(Exception e1){
					e1.printStackTrace();
				}
				try {
					sleep(500);
				} catch (InterruptedException e) {
					// Quitting
					break;
				}
			}
			
		}
	}
	
	private BroadcastReceiver mPhoneReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			if(state != null && state.equals(TelephonyManager.EXTRA_STATE_RINGING)){
				pauseTrack();
			}
		}
	};
	
	private class ShowPhoto implements Runnable {
        public ShowPhoto() {
        }

        public void run() {        	
        	error.setVisibility(View.INVISIBLE);
        	if(currImg == 1)
        	{
        		iv2.setVisibility(View.VISIBLE);
        		iv2.setImageBitmap(bmPhoto1);
        		iv2.getDrawable().setDither(true);
        		va.showNext();
        		currImg++;
        	}else{
        		iv1.setVisibility(View.VISIBLE);
        		iv1.setImageBitmap(bmPhoto2);
        		iv1.getDrawable().setDither(true);
        		va.showPrevious();
        		currImg--;
        	}
        }
    }
	
	private class ShowError implements Runnable {
        public ShowError() {
        }

        public void run() {
        	error.setVisibility(View.VISIBLE);
        	progress.setVisibility(View.INVISIBLE);
        	photoAnimThread.loadingPic = false;
        	callback.OnEnablePictureNavigation();
        }
    }
	
	public Runnable newShowPhotoRunnable(){
		return new ShowPhoto();
	}
	
	public Runnable newShowErrorRunnable(){
		return new ShowError();
	}
	
	protected void InitializeHelper(Context context, boolean pictures_only){
		cxt = context;
		this.pictures_only = pictures_only;
		vs.setScaleType(ImageView.ScaleType.CENTER);
        vs.setVisibility(View.INVISIBLE);
        outAnim = new AlphaAnimation((float)1.0, (float)0);
        outAnim.setDuration(2000);
        outAnim.setInterpolator(new AccelerateInterpolator(1.5f));
        outAnim.setAnimationListener(this);
        outAnim.setFillEnabled(true);
        outAnim.setFillAfter(true);
        outAnim.setZAdjustment(Animation.ZORDER_TOP);
        
        inAnim = new AlphaAnimation((float)0, (float)1.0f);
        inAnim.setDuration(500);
        inAnim.setAnimationListener(this);
        inAnim.setFillEnabled(true);
        inAnim.setFillAfter(true);
        inAnim.setZAdjustment(Animation.ZORDER_TOP);
        
        iv1 = new ImageView(cxt);
        iv2 = new ImageView(cxt);
                
        if(Utils.getSharedPreference(cxt, ConfigActivity.KEY_PICTURE_FIT, ConfigActivity.PICTURE_CROP).
        			equals(ConfigActivity.PICTURE_FIT)){
        	iv1.setScaleType(ScaleType.FIT_CENTER);
            iv2.setScaleType(ScaleType.FIT_CENTER); 
        }else{
        	iv1.setScaleType(ScaleType.CENTER_CROP);
            iv2.setScaleType(ScaleType.CENTER_CROP); 
        }
        
        
        picInAnim = AnimationUtils.loadAnimation(cxt,R.anim.fade_in);
        picInAnim.setAnimationListener(this);
        va.setInAnimation(picInAnim); 

        picOutAnim = AnimationUtils.loadAnimation(cxt,R.anim.fade_out);
        picOutAnim.setAnimationListener(this);
        va.setOutAnimation(picOutAnim);
        va.setAnimateFirstView(true);
        
        va.addView(iv1);
        va.addView(iv2);
        
        IntentFilter iF = new IntentFilter();
        iF.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        cxt.registerReceiver(mPhoneReceiver, iF);
	}
	
	public void onDestroy(){
		stopSlideshow();
		cxt.unregisterReceiver(mPhoneReceiver);
		if(mp!=null){
			mp.reset();
			mp.release();
			mp = null;
		}
	}
	
	public void startSlideshow(){
		
		if(firstPicture)
			progress.setVisibility(View.VISIBLE);
		
		dbHelper = DatabaseHelper.getInstance(cxt);
		db = dbHelper.getReadableDatabase();
		
		randGen = new Random((new Date()).getTime());
		
		//Does slideshow require wifi or sdcard, and have music and/or pictures;
		
		Cursor cursor = db.rawQuery("SELECT COUNT(slideshow_id) as items FROM Slideshow_Item WHERE location = 1 AND slideshow_id = ?", 
								new String[]{Long.toString(slideshow_id)}); 
		
		cursor.moveToNext();
		numCardItems = cursor.getInt(0);
		cursor.close();
		cursor = db.rawQuery("SELECT COUNT(slideshow_id) as items FROM Slideshow_Item WHERE location = 2 AND slideshow_id = ?", 
				new String[]{Long.toString(slideshow_id)}); 

		cursor.moveToNext();
		numWifiItems = cursor.getInt(0);
		cursor.close();
		
		cursor = db.rawQuery("SELECT COUNT(slideshow_id) as items FROM Play_Item WHERE item_type LIKE '" +
				DatabaseHelper.ITEM_TYPE_IMAGE + "' AND slideshow_id = ?", 
				new String[]{Long.toString(slideshow_id)}); 

		cursor.moveToNext();
		numPictures = cursor.getInt(0);
		cursor.close();
		
		cursor = db.rawQuery("SELECT COUNT(slideshow_id) as items FROM Play_Item WHERE item_type LIKE '" +
				DatabaseHelper.ITEM_TYPE_AUDIO + "' AND slideshow_id = ?", 
		new String[]{Long.toString(slideshow_id)}); 
		
		cursor.moveToNext();
		numTracks = cursor.getInt(0);
		cursor.close();
		
		if(numCardItems > 0){
			//Check that external storage card is available, show error if not
			if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) &&
					!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)){
				no_card.setVisibility(View.VISIBLE);
				progress.setVisibility(View.INVISIBLE);
				return;
			}
		}else if(numWifiItems > 0){
			//Check that wi-fi is enabled, show error if not
	        if(!NetworkHelper.isWiFiConnected(cxt)){
	        	no_wifi.setVisibility(View.VISIBLE);
	        	progress.setVisibility(View.INVISIBLE);
	        	return;
	        }else{
	        	no_wifi.setVisibility(View.INVISIBLE);
	        	if(wifi_wakelock == null){
	        		WifiManager wfm = (WifiManager) cxt.getSystemService(Context.WIFI_SERVICE);
	        		wifi_wakelock = wfm.createWifiLock(WifiManager.WIFI_MODE_FULL, "DigiFrame");
	        		wifi_wakelock.setReferenceCounted(false);
	        	}
	        	wifi_wakelock.acquire();
	        }	
		}
		
		if(numPictures > 0){
			if(wakelock == null){
				PowerManager pm = (PowerManager) cxt.getSystemService(Context.POWER_SERVICE);
				wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "DigiFrame");
				wakelock.setReferenceCounted(false);
			}
			wakelock.acquire();
			
			photoAnimThread = new AnimThread();
		
			photoAnimThread.setPriority(Thread.MIN_PRIORITY);
			photoAnimThread.start();
		}else{
			splash.setVisibility(View.VISIBLE);
			progress.setVisibility(View.INVISIBLE);
			callback.OnNoPicturesAvailable();
		}
		
		if(numPictures <= 0){
			callback.OnNoPicturesAvailable();
		}

		/*
		 * If the slideshow has no photos, fetch a music track now
		 * (If it has photos, track fetch will be triggered after first picture loaded)
		 */
		if(numPictures <= 0 && !pictures_only){
			trackThread = new TrackThread();
			trackThread.start();
		}
	}
	
	public void stopSlideshow(){
		try{
			if(positionThread != null){
				positionThread.interrupt();
			}
			stopTrack();
			if(photoAnimThread != null){
				photoAnimThread.interrupt();
			}
			if(wakelock != null)
			{
				wakelock.release();
			}
			if(wifi_wakelock != null)
			{
				wifi_wakelock.release();
			}
			}catch(Exception e){
				//ignore
			}
	}
	
	public boolean hasMusic(){
		return numTracks > 0;
	}
	
	private Item getRandomItem(int itemType){
		
		Cursor c = null;
		landscape = (cxt.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
		if(itemType == DatabaseHelper.ITEM_TYPE_IMAGE && Utils.getSharedPreference(cxt, ConfigActivity.KEY_MATCH_DEVICE_ORIENTATION, true)){
			c = db.query(Tables.PLAY_ITEM, new String[]{DatabaseHelper.Columns.URL, DatabaseHelper.Columns.WIDTH, DatabaseHelper.Columns.HEIGHT}, 
					"slideshow_id = ? and item_type like ? and landscape = ?", 
					new String[]{Long.toString(slideshow_id),Integer.toString(itemType),landscape ? "1" : "0"}, null, null, null);			
		}else{
			c = db.query(Tables.PLAY_ITEM, new String[]{DatabaseHelper.Columns.URL, DatabaseHelper.Columns.WIDTH, DatabaseHelper.Columns.HEIGHT}, "slideshow_id = ? and item_type like ?", 
					new String[]{Long.toString(slideshow_id),Integer.toString(itemType)}, null, null, null);	
		}
		
		if(c.getCount() <= 0) return null;
		int rand = randGen.nextInt(c.getCount());
		c.moveToPosition(rand);
		Item item = new Item(c.getString(0),c.getInt(1),c.getInt(2));
		
		c.close();
		return item;
	}
	
	public void playTrack(){
		playTrack(getRandomItem(DatabaseHelper.ITEM_TYPE_AUDIO));
	}
	
	private void playTrack(Item item){
		if(item!=null && item.url != null){
			isMusicPaused = false;
			currentTrackUrl = item.url;
			boolean newMP = false;
			try {
				callback.OnStartingDownload();
				if(mp == null){
					newMP = true;
					mp = MediaPlayer.create(cxt, Uri.parse(currentTrackUrl));
					mp.setOnPreparedListener(this);
					mp.setOnCompletionListener(this);
					mp.setOnInfoListener(this);
					mp.setOnBufferingUpdateListener(this);
					mp.setOnErrorListener(this);
					mp.setOnSeekCompleteListener(this);
				}
				
				//Get Track details
				if(currentTrackUrl.startsWith("file")){
					File f = new File(URI.create(currentTrackUrl));
					MP3File af = (MP3File)AudioFileIO.read(f);
					callback.OnNewTrackInfo(af);
				}else{
					HttpFile f = new HttpFile(currentTrackUrl);
					MP3File af = (MP3File)AudioFileIO.read(f);
					callback.OnNewTrackInfo(af);
				}
				
				mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
				if(!newMP){
					mp.reset();
					mp.setDataSource(cxt, Uri.parse(currentTrackUrl));
					mp.prepareAsync();
				}
				
				
			} catch (Exception e) {
				e.printStackTrace();
				try{
					callback.OnFinishedDownload();
				}catch(Exception ex){
				
				}
			}
		}
	}
	
	public void stopTrack(){
		if(mp != null){
			if(mp.isPlaying()){
				mp.stop();
				mp.reset();
				callback.OnTrackStopped();
				System.gc();
			}

			isMusicPaused = false;
			if(positionThread != null)
				positionThread.doUpdate = false;
			if(trackThread != null)
				trackThread.playNewTrack = true;
		}
	}
	
	public boolean isTrackPlaying(){
		if(mp == null) return false;
		else return mp.isPlaying();
	}
	
	public void pauseTrack(){
		if(mp != null){
			if(mp.isPlaying()) {
				mp.pause();
				isMusicPaused = true;
				callback.OnTrackStopped();
			}
		}
	}
	
	public void pausePicture(){
		isPicturePaused = true;
	}
	
	public boolean isPicturePlaying(){
		return !isPicturePaused;
	}
	
	public void resumePicture(){
		isPicturePaused = false;
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		try{
		mp.start();
		callback.OnTrackStarted();
		if(positionThread == null)
		{
			positionThread = new PositionThread();
			positionThread.setPriority(Thread.MIN_PRIORITY);
			positionThread.start();
		}else{
			positionThread.doUpdate = true;
		}
		}finally{
			callback.OnFinishedDownload();
		}
	}
	
	@SuppressLint("DefaultLocale")
	String getDurationString(int millis){
		String res;

		int hour = millis / MILLIS_IN_HOUR;
		int min = (millis - (hour * MILLIS_IN_HOUR)) / MILLIS_IN_MIN;
		int sec = (millis - (hour * MILLIS_IN_HOUR) - (min * MILLIS_IN_MIN)) / MILLIS_IN_SEC;
		
		if(hour > 0)
			res = String.format("%d:%02d:%02d",hour, min, sec);
		else if(min > 0)
			res = String.format("%d:%02d", min,sec);
		else
			res = String.format("0:%02d", sec);
		
		return res;
	}
	
	//v1.0.1 Set options for bitmap loading (mostly scaled sampling) to avoid memory error on large bitmaps
	private BitmapFactory.Options getBitmapOptions(Item item){
		DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		displayWidth = metrics.widthPixels;
		displayHeight = metrics.heightPixels;
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inDither = true;
		if(item.width > 0 && item.height >0 ) { // we have image size data
			if(item.width <= displayWidth && item.height <= displayHeight){
				opts.inSampleSize = 1;
			}else{
				//Discover be power of 2 divisor that won't reduce image quality on this display
				float xfactor = item.width / displayWidth;
				int c = 0;
				double divisor = Math.pow(2,c);
				while((int)(xfactor / divisor) > 0){
					c++;
					divisor = Math.pow(2,c);
				}
				opts.inSampleSize = (c == 0 ? 1 : (int)Math.pow(2,c-1));
			}
			
		}else{
			//TODO - how to handle if no image size data
		}
		return opts;
	}
	
	void fetchPhoto(Item item, boolean showStatus){
		if(item != null && item.url != null){
	        URL myFileUrl =null;          
	        try {
	             myFileUrl= new URL(item.url);
	        } catch (MalformedURLException e) {
	             e.printStackTrace();
	        }
	        //V1.0.1 - Add Opts to scale bitmap on load in case it is too large for memory
	        BitmapFactory.Options opts = getBitmapOptions(item);
	        
	        if(showStatus) callback.OnStartingDownload();
	        if(myFileUrl.getProtocol().equals("file")){
	        	try{
	        		photoAnimThread.loadingPic = true;

		        	if(currImg == 1)
		        		bmPhoto1 = BitmapFactory.decodeFile(item.url.substring(6),opts);
		        	else
		        		bmPhoto2 = BitmapFactory.decodeFile(item.url.substring(6),opts);
		        	callback.OnPhotoDownloaded();
	        	} catch (Exception e) {
	        		callback.OnError();
		            e.printStackTrace();
		        } catch (OutOfMemoryError e) {
		        	System.gc();
		        	callback.OnError();
		            e.printStackTrace();
		        }
	        }else{
	        	//Assume http
	        	HttpURLConnection conn = null;
	        	InputStream is = null;
		        try {
		        	 photoAnimThread.loadingPic = true;
		        	 if(showStatus) callback.OnStartingDownload();
		             conn= (HttpURLConnection)myFileUrl.openConnection();
		             conn.setDoInput(true);
		             conn.connect();
		             is = conn.getInputStream();
		             if(currImg == 1)
		            	 bmPhoto1 = BitmapFactory.decodeStream(is,null,opts);
		             else
		            	 bmPhoto2 = BitmapFactory.decodeStream(is,null,opts);
		             is.close();
		             callback.OnPhotoDownloaded();
		             
		        } catch (Exception e) {
		        	callback.OnError();
		            e.printStackTrace();
		        }finally{
		        	if(is != null)
						try {
							is.close();
						} catch (IOException e) {}
		        	if(conn != null) conn.disconnect();
		        }
	        }
	        
		}
   }

	@Override
	public void onCompletion(MediaPlayer arg0) {
		stopTrack();
		callback.OnTrackCompleted();
		playTrack(getRandomItem(DatabaseHelper.ITEM_TYPE_AUDIO));
	}
	
	@Override
	public void onAnimationEnd(Animation anim) {
		if(anim == picInAnim){
			progress.setVisibility(View.INVISIBLE);
        	if(firstPicture){
        		firstPicture = false;
        		if(!pictures_only){
        			trackThread = new TrackThread();
        			trackThread.start();
        		}
        	}
		}		
		if(anim == picOutAnim){
            if(currImg ==1){
            	iv2.setVisibility(View.INVISIBLE);
            	iv2.setImageBitmap(null);
            	if(iv2.getBackground() != null)
            		iv2.getBackground().setCallback(null);
            	if(iv2.getDrawable() != null)
            		iv2.getDrawable().setCallback(null);
            	if(bmPhoto1 != null) bmPhoto1.recycle();
            	bmPhoto1 = null;
            }else{
            	iv1.setVisibility(View.INVISIBLE);
            	iv1.setImageBitmap(null);
            	if(iv1.getBackground() != null)
            		iv1.getBackground().setCallback(null);
            	if(iv1.getDrawable() != null)
            		iv1.getDrawable().setCallback(null);
            	bmPhoto2 = null;
            }
            callback.OnEnablePictureNavigation();
            photoAnimThread.loadingPic = false;
		}
	}

	@Override
	public void onAnimationRepeat(Animation arg0) {
	}

	@Override
	public void onAnimationStart(Animation anim) {
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		stopTrack();
		return true;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
	}
	
	@Override
	public boolean onInfo(MediaPlayer mp, int arg1, int arg2) {
		return false;
	}

	public void setOrientation(boolean landscape){
		this.landscape = landscape;
	}
	
	public void seekToTrackPosition(int position){
		mp.seekTo(position);
	}
	
	public void resumeTrack(){
		mp.start();
		isMusicPaused = false;
	}
	
	public boolean isMusicPaused(){
		return isMusicPaused;
	}
	
	public void nextPicture(){
		if(!photoAnimThread.loadingPic){
			photoAnimThread.newPicNow = true;
			photoAnimThread.interrupt();
		}
	}
	
	public void onControlDrawerClosed(){
		
	}
}
