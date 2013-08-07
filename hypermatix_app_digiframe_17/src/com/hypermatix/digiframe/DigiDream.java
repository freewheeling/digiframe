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

import org.jaudiotagger.audio.mp3.MP3File;

import android.content.res.Configuration;
import android.service.dreams.DreamService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.hypermatix.digiframe.common.PlaybackHelperCallback;

public class DigiDream extends DreamService implements PlaybackHelperCallback{
	
		 private PlaybackHelperService helper; 
		 private View view;
	
	     @Override
	     public void onAttachedToWindow() {
	         super.onAttachedToWindow();

	         getWindow().setFlags(
	        		    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
	        		    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
	         
	         // Exit dream upon user touch
	         setInteractive(false);
	         // Hide system UI
	         setFullscreen(true);
	         // Set the dream layout
	         view = LayoutInflater.from(this).inflate(R.layout.playback_dream, null);
	         setContentView(view);
	         helper = new PlaybackHelperService(this,this);
	         helper.startSlideshow();
	         
	     }
	     
	     @Override
	     public void onDreamingStarted (){
	    	 super.onDreamingStarted();
	     }
	     
	     @Override
	     public void onDreamingStopped (){
	    	 super.onDreamingStopped();
	     }
	     
	     @Override
	     public void onDetachedFromWindow (){
	    	 helper.onDestroy();
	    	 super.onDetachedFromWindow();
	     }
	     
	     @Override
	 	public void onConfigurationChanged(Configuration newConfig) {
	 		helper.setOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
	 	    super.onConfigurationChanged(newConfig);
	 	}

		@Override
		public void OnTrackTimeUpdate(int pos, int duration, String sPos,
				String sRemaining) {

		}

		@Override
		public void OnEnablePictureNavigation() {

		}

		@Override
		public void OnNoPicturesAvailable() {

		}

		@Override
		public void OnStartingDownload() {

		}

		@Override
		public void OnFinishedDownload() {

		}

		@Override
		public void OnNewTrackInfo(MP3File mp3) {

		}

		@Override
		public void OnTrackStopped() {

		}

		@Override
		public void OnTrackStarted() {

		}

		@Override
		public void OnTrackCompleted() {

		}

		@Override
		public void OnPhotoDownloaded() {
			if(view != null){
				view.post(helper.newShowPhotoRunnable());
			}
		}

		@Override
		public void OnError() {
			if(view != null){
				view.post(helper.newShowErrorRunnable());
			}
		}

		@Override
		public void onControlDrawerClosed() {
			
		}

}
