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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;

public class DrawerHandleView extends ImageView {

	public DrawerHandleView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public DrawerHandleView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DrawerHandleView(Context context) {
		super(context);
	}
	
	@Override
	protected void onDraw (Canvas canvas){
		super.onDraw(canvas);
	}
	
	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
		DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
		this.setMeasuredDimension(metrics.widthPixels, this.getBackground().getMinimumHeight());
	}
	
	@Override
	public void setVisibility(int visibility){
		super.setVisibility(visibility);
	}
}
