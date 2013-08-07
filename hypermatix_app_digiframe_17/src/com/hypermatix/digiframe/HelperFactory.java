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
import android.app.Service;
import android.service.dreams.DreamService;

import com.hypermatix.digiframe.common.IHelperFactory;
import com.hypermatix.digiframe.common.PlaybackHelper;
import com.hypermatix.digiframe.common.PlaybackHelperCallback;

public class HelperFactory implements IHelperFactory {

	@Override
	public PlaybackHelper createForActivity(PlaybackHelperCallback callback,
			Activity activity, long slideshow) {
		return new PlaybackHelperActivity(callback, activity, slideshow);
	}

	@Override
	public PlaybackHelper createForService(PlaybackHelperCallback callback,
			Service service) {
		return new PlaybackHelperService(callback, (DreamService)service);
	}

}
