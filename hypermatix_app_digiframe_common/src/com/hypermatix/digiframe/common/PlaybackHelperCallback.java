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

public interface PlaybackHelperCallback {
	public void OnTrackTimeUpdate(int pos, int duration, String sPos, String sRemaining);
	public void OnEnablePictureNavigation();
	public void OnNoPicturesAvailable();
	public void OnStartingDownload();
	public void OnFinishedDownload();
	public void OnNewTrackInfo(MP3File mp3);
	public void OnTrackStopped();
	public void OnTrackStarted();
	public void OnTrackCompleted();
	public void OnPhotoDownloaded();
	public void OnError();
	public void onControlDrawerClosed();
}
