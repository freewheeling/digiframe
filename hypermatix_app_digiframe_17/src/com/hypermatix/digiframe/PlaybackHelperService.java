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

import android.service.dreams.DreamService;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ViewAnimator;
import com.hypermatix.digiframe.common.PlaybackHelper;
import com.hypermatix.digiframe.common.PlaybackHelperCallback;
import com.hypermatix.digiframe.common.Utils;

public class PlaybackHelperService extends PlaybackHelper {

	public PlaybackHelperService(PlaybackHelperCallback callback, DreamService service){
		super(callback);
		va = (ViewAnimator)service.findViewById(R.id.viewanimator);
		vs = (ImageView)service.findViewById(R.id.viewstatus);
        progress = (LinearLayout)service.findViewById(R.id.progress);
        no_wifi = (LinearLayout)service.findViewById(R.id.no_wifi);
        no_card = (LinearLayout)service.findViewById(R.id.no_card);
        splash = (LinearLayout)service.findViewById(R.id.splash);
        error = (LinearLayout)service.findViewById(R.id.error);
        //TODO - get SLideshowId from settings (or else pick first)
        slideshow_id = DreamSettingsActivity.getPreferenceSlideshowId(service);
        boolean pictures_only = Utils.getSharedPreference(service, DreamSettingsActivity.KEY_DREAM_PICTURES_ONLY , true);
        InitializeHelper(service, pictures_only);
        service.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION); //TODO - aPi 11 only
	}
}
