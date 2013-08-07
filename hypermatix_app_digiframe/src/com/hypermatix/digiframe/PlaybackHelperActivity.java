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

import android.app.Activity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ViewAnimator;

import com.hypermatix.digiframe.common.PlaybackHelper;
import com.hypermatix.digiframe.common.PlaybackHelperCallback;

public class PlaybackHelperActivity extends PlaybackHelper {
	
	public PlaybackHelperActivity(PlaybackHelperCallback callback, Activity activity, long slideshow){
		super(callback);
		va = (ViewAnimator)activity.findViewById(R.id.viewanimator);
		vs = (ImageView)activity.findViewById(R.id.viewstatus);
        progress = (LinearLayout)activity.findViewById(R.id.progress);
        no_wifi = (LinearLayout)activity.findViewById(R.id.no_wifi);
        no_card = (LinearLayout)activity.findViewById(R.id.no_card);
        splash = (LinearLayout)activity.findViewById(R.id.splash);
        error = (LinearLayout)activity.findViewById(R.id.error);
        slideshow_id = slideshow;
        InitializeHelper(activity, false);
	}
	
}
