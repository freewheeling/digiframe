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

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceList;
import org.cybergarage.upnp.UPnPStatus;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class DLNAHelper implements SearchResponseListener {
	
	public static final String ITEM_TYPE_CONTAINER = "object.container";
	private static final String ITEM_TYPE_STORAGE_FOLDER = "object.container.storageFolder";
	private static final String ITEM_TYPE_MUSIC = "object.item.audioItem.musicTrack"; 
	private static final String ITEM_TYPE_PHOTO = "object.item.imageItem.photo";
	
	private static ControlPoint ctrlPoint = new ControlPoint();
	private static int count = 0;
	private String device_udn;
	private Device device;
	private Hashtable<DeviceAvailableListener,String> htListeners = new Hashtable<DeviceAvailableListener,String>();
	
	public interface DeviceAvailableListener{
		public void OnDeviceAvailable(String udn);
	}
    
	private class SearchThread extends Thread{
		@Override
		public void run(){
			ctrlPoint.start();
			ctrlPoint.search("upnp:rootdevice");
		};
	}
	
	public DLNAHelper(){
		ctrlPoint.addSearchResponseListener(this);
		synchronized(DLNAHelper.class){
			if(count == 0){
				SearchThread t = new SearchThread();
				t.start();
			}
			count++;
		}
	}
	
	public void setDevice(String udn){
		device_udn = udn;
		fetchDevice();
	}
	
	public void setDevice(int pos){
		device = DeviceListActivity.dl.getDevice(pos);
	}
	
	public boolean isDeviceAvailable(){
		return (device != null);
	}
	
	public void addDeviceAvailableListener(DLNAHelper.DeviceAvailableListener listener){
		synchronized(htListeners){
			htListeners.put(listener,"");
		}
	}
	
	public void addDeviceAvailableListener(String udn, DLNAHelper.DeviceAvailableListener listener){
		synchronized(htListeners){
			htListeners.put(listener,udn);
		}
	}
	
	public void removeDeviceAvailableListener(DLNAHelper.DeviceAvailableListener listener){
		htListeners.remove(listener);
	}
	
	public void close(){
		synchronized(DLNAHelper.class){
			count--;
		}
		if(count == 0){
			try{
				ctrlPoint.stop();
			}catch(Exception e){
				
			}
		}
	}

	@Override
	public void deviceSearchResponseReceived(SSDPPacket packet) {
		String usn = packet.getUSN(); //TODO - USN may not be UDN
		notifyDeviceAvailable(usn);
		fetchDevice();
	}
	
	private void fetchDevice(){
		if(device_udn != null && device == null)
		{
			//Try to get the device
			device = ctrlPoint.getDevice(device_udn);
		}
	}
	
	private void notifyDeviceAvailable(String usn){
		Enumeration<DeviceAvailableListener> en = htListeners.keys();
		while(en.hasMoreElements()){
			DeviceAvailableListener key = (DeviceAvailableListener)en.nextElement();
			String val = (String)htListeners.get(key);
			if(val == "")
				key.OnDeviceAvailable(usn);
			else{
				if(val.equals(usn)) key.OnDeviceAvailable(usn);
			}
		}
	}
	
	public DeviceList getDeviceList(){
		return ctrlPoint.getDeviceList();
	}
	
	public ArrayList<DLNAItem> getDirectoryList(String oid, String pid, int start, int count, int minX, int minY){
    	ArrayList<DLNAItem> list = new ArrayList<DLNAItem>();
    	
    	//parent
			if(pid != null){
				DLNAItem parent = new DLNAItem();
		    	
		    	parent.Title = "[..]";
		    	parent.ID = pid;
		    	parent.isParentNavigator = true;
		    	parent.ItemClass = ITEM_TYPE_CONTAINER;
		    	parent.URI = null;
		    	
		    	list.add(parent);
			}
    	
    	if(device != null){
    		ServiceList sl = device.getServiceList();
        	for(int i =0; i < sl.size(); i++){
        		Service service = (Service)sl.get(i);
        		if(service.getServiceType().equals("urn:schemas-upnp-org:service:ContentDirectory:1")){
        			
        			Action actBrowse = service.getAction("Browse");
        			
        			//Arguments:
        			//ObjectID - IN
        			//BrowseFlag - IN
        			//Filter - IN
        			//StartingIndex - IN
        			//RequestedCount - IN
        			//SortCriteria - IN
        			//Result - OUT
        			//NumberReturned - OUT
        			//TotalMatches - OUT
        			//UpdateID - OUT
        			
        			//oid may be null if we are requesting root dir
        			actBrowse.setArgumentValue("ObjectID", oid == null ? "0" : oid);
        			actBrowse.setArgumentValue("BrowseFlag", "BrowseDirectChildren");
        			actBrowse.setArgumentValue("Filter", "*");
        			actBrowse.setArgumentValue("StartingIndex", start);
        			actBrowse.setArgumentValue("RequestedCount", (start == 0 && count ==0 ) ? 0 : count);
        			actBrowse.setArgumentValue("SortCriteria", "");
        			
        			if(actBrowse.postControlAction()){
        				
        				//The search results is an XML string following the DIDL-Lite schema.
        				String xml = actBrowse.getArgument("Result").getValue();

        				XPath xpath = XPathFactory.newInstance().newXPath();
        				xpath.setNamespaceContext(new DIDLNamespaceContext());
        				
        				Reader reader = new StringReader(xml);
        				InputSource is = new InputSource(reader);
        				String expression = "/didl:DIDL-Lite/didl:container"; //item
        				
        				//String expression = "/dc/title";
        				try {
    						NodeList nodes = (NodeList) xpath.evaluate(expression, is, XPathConstants.NODESET);
    						for(int j = 0; j < nodes.getLength(); j++){
    							Node node = nodes.item(j);
    							DLNAItem item = new DLNAItem();
    							NamedNodeMap attrs = node.getAttributes();
    							Node attr;
    							attr = attrs.getNamedItem("id");
    							if(attr != null) item.ID = attr.getTextContent();
    							
    							attr = attrs.getNamedItem("restricted");
    							if(attr != null) item.Restricted = attr.getTextContent();
    							attr = attrs.getNamedItem("childCount");
    							if(attr != null) item.ChildCount = Integer.parseInt(attr.getTextContent());
    							NodeList subnodes = node.getChildNodes();
    							for(int k = 0; k < subnodes.getLength(); k++){
    								Node subnode = subnodes.item(k);
    								if(subnode.getNodeName().equals("dc:title")){
    									item.Title = subnode.getTextContent();
    								}
    								if(subnode.getNodeName().equals("upnp:class")){
    									item.ItemClass = subnode.getTextContent();
    								}
    							}
    							
    							list.add(item);
    						}
    					} catch (XPathExpressionException e) {
    						e.printStackTrace();
    					}
    					
    					reader = new StringReader(xml);
        				is = new InputSource(reader);
        				expression = "/didl:DIDL-Lite/didl:item"; //item
        				
        				try {
    						NodeList nodes = (NodeList) xpath.evaluate(expression, is, XPathConstants.NODESET);
    						for(int j = 0; j < nodes.getLength(); j++){
    							Node node = nodes.item(j);
    							DLNAItem item = new DLNAItem();
    							NamedNodeMap attrs = node.getAttributes();
    							Node attr;
    							attr = attrs.getNamedItem("id");
    							if(attr != null) item.ID = attr.getTextContent();
    							attr = attrs.getNamedItem("parentID");
    							if(attr != null) item.ParentID = attr.getTextContent();
    							attr = attrs.getNamedItem("restricted");
    							if(attr != null) item.Restricted = attr.getTextContent(); 							
    							NodeList subnodes = node.getChildNodes();
    							int lastAcceptedRes = 0;
    							for(int k = 0; k < subnodes.getLength(); k++){
    								Node subnode = subnodes.item(k);
    								if(subnode.getNodeName().equals("dc:title")){
    									item.Title = subnode.getTextContent();
    								}
    								if(subnode.getNodeName().equals("upnp:class")){
    									item.ItemClass = subnode.getTextContent();
    								}
    								
    								//Handle <res> node, with URI information
    								if(subnode.getNodeName().equals("res")){
    									if(subnode.hasAttributes()){
    										Node resNode = subnode.getAttributes().getNamedItem("resolution");
    										if(resNode != null){
    											//Check image resolution
    											String res = resNode.getTextContent();
    											String[] ress = res.split("x");
    											int resx = Integer.parseInt(ress[0]);
    											int resy = Integer.parseInt(ress[1]);
    											int minres = resx <= resy ? resx : resy;
    											
    											//Flag the orientation of image
    											if(resx >= resy) item.Landscape = true;
    											
    											//Use this image if it won't require any upscaling
    											//If there is a smaller version, which still won't 
    											//require upscaling, use that instead (to keep downloads small)
    											int minsize = minX <= minY ? minX : minY;
    											if(minres >= minsize && (lastAcceptedRes == 0 || minres < lastAcceptedRes)){
    												lastAcceptedRes = minres;
    												item.URI = subnode.getTextContent();
    												item.width = resx;
    												item.height = resy;
    											}
    											
    										}else
    											item.URI = subnode.getTextContent();
    									}
    									
    								}
    							}
    							
    							if(item.URI != null)
    								list.add(item);
    						}
    					} catch (XPathExpressionException e) {
    						e.printStackTrace();
    					}
        				
        			}else{
        				UPnPStatus err = actBrowse.getControlStatus();
        				System.out.println("Action Error: " + err.getDescription());
        			}
        			
        		}
        	}
    	}
    	return list;
    }

	public static boolean isDirectory(String itemType){
		if(itemType.startsWith(ITEM_TYPE_CONTAINER))
    		return true;
		else
			return false;
	}
	
	public static boolean isMusic(String ext){
		if(ext.equals("mp3") ||
				ext.equals(ITEM_TYPE_MUSIC))
    		return true;
		else
			return false;
	}
	
	public static boolean isPicture(String ext){
		if(ext.equals("jpg") || 
    			ext.equals("png") ||
    			ext.equals("jpeg") ||
    			ext.equals(ITEM_TYPE_PHOTO))
    		return true;
		else
			return false;
	}
	
	public static int textTypeToDBType(String itemClass){
		String cl = itemClass.toLowerCase();
		if(cl.equals(ITEM_TYPE_CONTAINER.toLowerCase()))
			return DatabaseHelper.ITEM_TYPE_FOLDER;
		else if(cl.equals(ITEM_TYPE_STORAGE_FOLDER.toLowerCase()))
			return DatabaseHelper.ITEM_TYPE_FOLDER;
		else if(cl.equals(ITEM_TYPE_MUSIC.toLowerCase()))
			return DatabaseHelper.ITEM_TYPE_AUDIO;
		else if(cl.equals(ITEM_TYPE_PHOTO.toLowerCase()))
			return DatabaseHelper.ITEM_TYPE_IMAGE;
		else
			return -1;
	}
	
}