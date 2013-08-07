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

import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;

import com.hypermatix.digiframe.common.R;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.view.View;

public class PlaybackActivity extends Activity implements 
														  View.OnClickListener,
														  View.OnTouchListener,
														  SlidingDrawer.OnDrawerCloseListener,
														  SlidingDrawer.OnDrawerOpenListener,
														  SlidingDrawer.OnDrawerScrollListener,
														  SeekBar.OnSeekBarChangeListener,
														  PlaybackHelperCallback
														  {

	private SeekBar seekBar;
	private TextView trackTitle, trackArtist;
	private ImageView handle;
	private SlidingDrawer slidingDrawer;
	private LinearLayout drawerContent;
	private TextView timeRemaining, timeGone;
	private ImageButton btnPlayPauseTrack, btnPlayPausePic,
						btnSkipTrack, btnSkipPic;
	private ProgressBar processing;
	private Drawable playDrawable;
	private Drawable pauseDrawable;
	
	private PlaybackHelper helper;
	
	
	int pos; 
	int duration; 
	String sPos; 
	String sRemaining;
	
	public static IHelperFactory helperFactory;
	
	//Runnables, which are called directly or indirectly from threads,
	//but which access UI and so must be run on UI Thread
	private Runnable setTimesRunnable = new Runnable() {
		public void run(){
			seekBar.setMax(duration);
			seekBar.setProgress(pos);
			timeRemaining.setText(sPos);
			timeGone.setText(sRemaining);
		}
	};
	
	private Runnable ShowProcessing = new Runnable() {
		public void run(){
			processing.setVisibility(View.VISIBLE);
		}
	};
	
	private Runnable ClearProcessing = new Runnable() {
		public void run(){
			processing.setVisibility(View.INVISIBLE);
			if(helper.hasMusic())
				btnSkipTrack.setEnabled(true);
		}
	};
	
	private Runnable EnableSeekBar = new Runnable() {
		public void run(){
			seekBar.setEnabled(true);
		}
	};
	
	private Runnable EnableTrackCtrl = new Runnable() {
		public void run(){
	        btnPlayPauseTrack.setEnabled(true);
	        btnSkipTrack.setEnabled(true);
		}
	};
	
	private Runnable EnablePicCtrl = new Runnable() {
		public void run(){
	        btnPlayPausePic.setEnabled(true);
	        btnSkipPic.setEnabled(true);
		}
	};
	
	private Runnable DisableSeekBar = new Runnable() {
		public void run(){
			seekBar.setEnabled(false);
		}
	};
	
	private Runnable DisableTrackCtrl = new Runnable() {
		public void run(){
			btnPlayPauseTrack.setEnabled(false);
	        btnSkipTrack.setEnabled(false);
		}
	};
		
	
	
	private class ShowTrackInfoRunnable implements Runnable {
		private MP3Info mp3info;
		private MP3File mp3file;
		
		public ShowTrackInfoRunnable(MP3File file){
			mp3file = file;
		}
		
		public void run() {
			if(mp3info != null){
				trackTitle.setText(mp3info.getTitle());
				trackArtist.setText(mp3info.getArtist());
			}else{
				if(mp3file.hasID3v1Tag()){
					ID3v1Tag v1Tag  = (ID3v1Tag)mp3file.getID3v1Tag();
					trackTitle.setText(v1Tag.getFirstTitle());
					trackArtist.setText(v1Tag.getFirstArtist());
				}else if(mp3file.hasID3v2Tag()){
					ID3v24Tag v2Tag = mp3file.getID3v2TagAsv24();
					trackTitle.setText(v2Tag.getFirst(ID3v24Frames.FRAME_ID_TITLE));
					trackArtist.setText(v2Tag.getFirst(ID3v24Frames.FRAME_ID_ARTIST));
				}
			}
			btnPlayPauseTrack.setImageResource(R.drawable.pause);
		}
	}
	
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        
        setContentView(R.layout.playback);
        
        drawerContent = (LinearLayout)findViewById(R.id.contentLayout);
        drawerContent.setOnTouchListener(this);
        processing = (ProgressBar)findViewById(R.id.processing);
        handle = (ImageView)findViewById(R.id.drawerHandle);
        handle.setAlpha(0);
        handle.getBackground().setAlpha(0);
        slidingDrawer = (SlidingDrawer)findViewById(R.id.slidingDrawer);
        slidingDrawer.setOnDrawerCloseListener(this);
        slidingDrawer.setOnDrawerScrollListener(this);
        slidingDrawer.setOnDrawerOpenListener(this);
        slidingDrawer.setOnTouchListener(this);

        Intent intent = this.getIntent();
        long slideshow_id = intent.getLongExtra("SlideshowID", -1);
        helper = helperFactory.createForActivity(this, this, slideshow_id);

		btnPlayPauseTrack = (ImageButton)findViewById(R.id.btnPlayPauseTrack);
        btnPlayPauseTrack.setOnClickListener(this);
        btnPlayPausePic = (ImageButton)findViewById(R.id.btnPlayPausePic);
        btnPlayPausePic.setOnClickListener(this);
        btnSkipTrack = (ImageButton)findViewById(R.id.btnSkipTrack);
        btnSkipTrack.setOnClickListener(this);
        btnSkipPic = (ImageButton)findViewById(R.id.btnSkipPic);
        btnSkipPic.setOnClickListener(this);
        btnPlayPauseTrack.setEnabled(false);
        btnPlayPausePic.setEnabled(false);
        btnSkipTrack.setEnabled(false);
        btnSkipPic.setEnabled(false);
        playDrawable = this.getResources().getDrawable(R.drawable.play);
    	pauseDrawable = this.getResources().getDrawable(R.drawable.pause);
        
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setEnabled(false);
        trackTitle = (TextView)findViewById(R.id.trackTitle);
        trackArtist = (TextView)findViewById(R.id.trackArtist);
        timeRemaining = (TextView)findViewById(R.id.timeRemaining);
        timeGone = (TextView)findViewById(R.id.timeGone);
        
        seekBar.setOnSeekBarChangeListener(this);
        
        helper.startSlideshow();
    }
	
	@Override
	public void onPause(){
		helper.pausePicture();
		super.onPause();
	}
	
	@Override
	public void onResume(){
		if(!helper.isPicturePlaying() && btnPlayPausePic.getDrawable().equals(this.getResources().getDrawable(R.drawable.play))){
			helper.resumePicture();
		}
		super.onResume();
	}
	
	@Override
	public void onDestroy(){
		helper.onDestroy();
		super.onDestroy();
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent ev){
		
		if(ev.getAction() == MotionEvent.ACTION_DOWN){
			slidingDrawer.animateToggle();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event){
		if(keyCode == KeyEvent.KEYCODE_MENU){
			slidingDrawer.animateToggle();
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_BACK){
			finish();
			return true;
		}
		
		return false;
	}

	@Override
	public void onClick(View view) {
		if(view == btnPlayPauseTrack){
			if(helper.isMusicPaused()){
				helper.resumeTrack();
				seekBar.setEnabled(true);
				btnPlayPauseTrack.setImageResource(R.drawable.pause);
			}
			else if(helper.isTrackPlaying()){
				helper.pauseTrack();
				btnPlayPauseTrack.setImageResource(R.drawable.play);
			}else{
				helper.playTrack();
			}
		}
		if(view == btnPlayPausePic){
			if(helper.isPicturePlaying()){
				helper.pausePicture();
				btnPlayPausePic.setImageDrawable(playDrawable);
			}
			else{
				helper.resumePicture();
				btnPlayPausePic.setImageDrawable(pauseDrawable);
			}
		}
		if(view == btnSkipTrack){
			btnSkipTrack.setEnabled(false);
			btnPlayPauseTrack.setEnabled(false);
			helper.stopTrack();
		}
		if(view == btnSkipPic){
			btnSkipPic.setEnabled(false);
			helper.nextPicture();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		//Prevent the PlaybackActivity handler from being used when drawer is touched
		if(v == drawerContent) return true;
		else return false;
	}
	
	@Override
	public void onDrawerClosed() {
		onControlDrawerClosed();
	}
	
	@Override 
	public void onControlDrawerClosed(){
		handle.setAlpha(0);
		handle.getBackground().setAlpha(0);
	}

	@Override
	public void onScrollEnded() {	
	}

	@Override
	public void onScrollStarted() {
		handle.setAlpha(255);
		handle.getBackground().setAlpha(255);
	}

	@Override
	public void onDrawerOpened() {
		handle.setAlpha(255);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		helper.seekToTrackPosition(seekBar.getProgress());
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		helper.setOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
	    super.onConfigurationChanged(newConfig);
	}

	@Override
	public void OnTrackTimeUpdate(int pos, int duration, String sPos, String sRemaining){
		this.pos = pos;
		this.duration = duration;
		this.sPos = sPos;
		this.sRemaining = sRemaining;
		PlaybackActivity.this.runOnUiThread(setTimesRunnable);
	}
	
	@Override
	public void OnEnablePictureNavigation(){
		btnSkipPic.setEnabled(true);
	}

	@Override
	public void OnNoPicturesAvailable() {
		btnPlayPausePic.setEnabled(false);
		btnSkipPic.setEnabled(false);
	}

	@Override
	public void OnStartingDownload(){
		runOnUiThread(ShowProcessing);
	}
	
	@Override
	public void OnNewTrackInfo(MP3File mp3){
		runOnUiThread(new ShowTrackInfoRunnable(mp3));
	}
	
	@Override
	public void OnFinishedDownload(){
		runOnUiThread(ClearProcessing);
	}
	
	@Override
	public void OnTrackStopped(){
		runOnUiThread(DisableSeekBar);
	}
	
	@Override
	public void OnTrackStarted(){
		runOnUiThread(EnableSeekBar);
		runOnUiThread(EnableTrackCtrl);
	}
	
	@Override
	public void OnTrackCompleted(){
		runOnUiThread(DisableTrackCtrl);
	}
	
	@Override
	public void OnPhotoDownloaded(){
		runOnUiThread(helper.newShowPhotoRunnable());
		runOnUiThread(EnablePicCtrl);
		runOnUiThread(ClearProcessing);
	}
	
	@Override
	public void OnError(){
		runOnUiThread(helper.newShowErrorRunnable());
		runOnUiThread(EnablePicCtrl);
	}
}