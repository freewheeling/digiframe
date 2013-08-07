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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceList;
import org.cybergarage.upnp.UPnPStatus;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.app.ListActivity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;

import com.hypermatix.digiframe.common.R;

import android.os.Bundle;



public class DirectoryListActivity extends ListActivity 
									implements AdapterView.OnItemClickListener, 
												View.OnClickListener {
	
	private DirectoryListAdapter adapter;
	private Device device;
	private String objectId="0"; 
	
	public static DirectoryItem di;
	
	public static final String ITEM_TYPE_CONTAINER = "object.container";
	public static final String ITEM_TYPE_FOLDER = "object.container.storageFolder"; 
	public static final String ITEM_TYPE_MUSIC = "object.item.audioItem.musicTrack"; 
	public static final String ITEM_TYPE_PHOTO = "object.item.imageItem.photo";
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.directory_list);
        
        Intent intent = this.getIntent();
        int devpos = intent.getIntExtra("DevicePos",0);
        device = DeviceListActivity.dl.getDevice(devpos);
        adapter = new DirectoryListAdapter(this, null, this);
        setListAdapter(adapter);
        
        refreshDirectoryList();
        
        getListView().setOnItemClickListener(this);
        
    }

    public static ArrayList<DirectoryItem> getDirectoryList(Device dev, String oid){
    	ArrayList<DirectoryItem> list = new ArrayList<DirectoryItem>();
    	if(dev != null){
    		ServiceList sl = dev.getServiceList();
        	for(int i =0; i < sl.size(); i++){
        		Service service = (Service)sl.get(i);
        		if(service.getServiceType().equals("urn:schemas-upnp-org:service:ContentDirectory:1")){			
        			Action browse = service.getAction("Browse");
        			ArgumentList argList= browse.getArgumentList();
        			for (int k=0; k<argList.size(); k++) {
        				Argument arg = argList.getArgument(k);
        				System.out.println("Argument: " + arg.getName());
        				//arg.getName();
        			}
        			
        			//Arguments:
        			//ObjectID
        			//BrowseFlag
        			//Filter
        			//StartingIndex
        			//RequestedCount
        			//SortCriteria
        			//Result
        			//NumberReturned
        			//TotalMatches
        			//UpdateID
        			
        			//browse.setArgumentValue("ObjectID", "A_F");
        			browse.setArgumentValue("ObjectID", oid);
        			browse.setArgumentValue("BrowseFlag", "BrowseDirectChildren");
        			browse.setArgumentValue("Filter", "*");
        			browse.setArgumentValue("StartingIndex", 0);
        			browse.setArgumentValue("RequestedCount", 0); //Get all
        			browse.setArgumentValue("SortCriteria", "");
        			
        			if(browse.postControlAction()){       				
        				//The search results is an XML string following the DIDL-Lite schema.
        				String xml = browse.getArgument("Result").getValue();
        			
        				XPath xpath = XPathFactory.newInstance().newXPath();
        				xpath.setNamespaceContext(new DIDLNamespaceContext());
        				
        				Reader reader = new StringReader(xml);
        				InputSource is = new InputSource(reader);
        				String expression = "/didl:DIDL-Lite/didl:container"; //item
        				
        				try {
    						NodeList nodes = (NodeList) xpath.evaluate(expression, is, XPathConstants.NODESET);
    						for(int j = 0; j < nodes.getLength(); j++){
    							Node node = nodes.item(j);
    							DirectoryItem item = new DirectoryItem();
    							NamedNodeMap attrs = node.getAttributes();
    							Node attr;
    							attr = attrs.getNamedItem("id");
    							if(attr != null) item.ID = attr.getTextContent();
    							attr = attrs.getNamedItem("parentID");
    							if(attr != null) item.ParentID = attr.getTextContent();
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
    							DirectoryItem item = new DirectoryItem();
    							NamedNodeMap attrs = node.getAttributes();
    							Node attr;
    							attr = attrs.getNamedItem("id");
    							if(attr != null) item.ID = attr.getTextContent();
    							attr = attrs.getNamedItem("parentID");
    							if(attr != null) item.ParentID = attr.getTextContent();
    							attr = attrs.getNamedItem("restricted");
    							if(attr != null) item.Restricted = attr.getTextContent(); 							
    							NodeList subnodes = node.getChildNodes();
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
    									item.URI = subnode.getTextContent();
    								}
    							}
    							
    							list.add(item);
    						}
    					} catch (XPathExpressionException e) {
    						e.printStackTrace();
    					}
        				
        			}else{
        				UPnPStatus err = browse.getControlStatus();
        				System.out.println("Action Error: " + err.getDescription());
        			}
        			
        		}
        	}
    	}
    	return list;
    }
    
    private void refreshDirectoryList(){
    	ArrayList<DirectoryItem> list = getDirectoryList(device,objectId);
    	adapter.setList(list);
    	setListAdapter(null);
    	setListAdapter(adapter);
    }

	@Override
	public void onItemClick(AdapterView<?> av, View view, int position, long id) {
	}

	@Override
	public void onClick(View v) {
		if(v.getClass().equals(CheckBox.class)){
			CheckBox cb = (CheckBox)v;
			di = (DirectoryItem)cb.getTag();
			//Add to list
			setResult(SlideshowActivity.RES_CODE_ADD_ITEM);
			this.finish();
		}else{
			int position = Integer.parseInt(v.getTag().toString());
			DirectoryItem item = (DirectoryItem)adapter.getList().get(position);
			if(item.URI == null){ //Sub-Directory
				objectId = item.ID;
				refreshDirectoryList();
			}
		}
	}
}