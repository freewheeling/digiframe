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

import org.cybergarage.upnp.Device;

public class SlideshowListItem{
	
	public long Id;
	public long DeviceId;
	public long SlideshowId;
	public int Location;
	public String DeviceFriendlyName;
	public String DeviceUDN;
	public String ItemName;
	public String ItemParentID;
	public String ItemID;
	public String ItemUrl;
	public int ItemType;
	public boolean IsExclusion;
	public long ExclusionParentId;
	public String ExclusionParentUrl;
	
	public SlideshowListItem(){
		
	}
	
	public SlideshowListItem(SDItem item){
		Location = DatabaseHelper.LOCATION_FILE_SYSTEM;
		ItemName = item.name;
		ItemUrl = item.getUrl();
	}
	
	public SlideshowListItem(DLNAItem item){
		Location = DatabaseHelper.LOCATION_MEDIA_SERVER;
		ItemName = item.Title;
		ItemUrl = item.URI;
		Device dev = DeviceListActivity.dev;
		ItemID = item.ID;
		ItemParentID = item.ParentID;
		ItemType = DLNAHelper.textTypeToDBType(item.ItemClass); 
		DeviceUDN = dev.getUDN();
		DeviceFriendlyName = dev.getFriendlyName();
	}
}
