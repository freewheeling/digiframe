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

import java.io.BufferedInputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;

import android.net.Uri;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.jpeg.JpegDirectory;

	public class SDItem implements Comparable<SDItem>{
		public String name;
		public File file;
		public boolean isSelectable;
		public boolean isSelected;
		public boolean isMusic;
		public boolean isPicture;
		public boolean isLandscape = false;
		public int width = 0;
		public int height = 0;
		public long size = 0;
		
		public SDItem(String name, File file){
			this.name = name;
			this.file = file;
			size = file.length();
			
			if(file.isDirectory()){
				isSelectable = true;
			}else{
				try{
					String ext = Utils.getFileExtension(file.getName());
					isMusic = DLNAHelper.isMusic(ext);
					if(isMusic) isSelectable = true;
					isPicture = DLNAHelper.isPicture(ext);
					if(isPicture){
						isSelectable = true;
			        	//Get relevant EXIF data
			        	
			        		URL fileUrl = file.toURI().toURL();
			        		URLConnection conn = fileUrl.openConnection();
			        		BufferedInputStream stream = new BufferedInputStream(conn.getInputStream());
			        		//TODO - check if this is very slow if reading header thumbnails, etc.?
			        		Metadata metadata = ImageMetadataReader.readMetadata(stream,false); 
			        		stream.close();
			        		JpegDirectory jdir = metadata.getDirectory(JpegDirectory.class);
			        		if(jdir != null){
			        			width = jdir.getImageWidth();
			        			height = jdir.getImageHeight();
			        			if(width > 0 && height >0 && width > height) isLandscape = true;
			        		}     	
					}
				}catch(Exception e){
	        		//
	        	}
			}
		}
		
		public SDItem(String name, File file, boolean selected){
			this(name,file);
			isSelected = selected;
		}
		
		public String getUrl(){
			return Uri.fromFile(file).toString();
		}

		public int compareTo(SDItem obj) {
			return this.file.compareTo(obj.file);
		}
	}