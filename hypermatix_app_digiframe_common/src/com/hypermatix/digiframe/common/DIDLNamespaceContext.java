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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class DIDLNamespaceContext implements NamespaceContext {
	
	private Map<String,String> urisByPrefix = new HashMap<String,String>();

    private Map<String,Set<String>> prefixesByURI = new HashMap<String,Set<String>>();

    public DIDLNamespaceContext() {
        // prepopulate with xml and xmlns prefixes per JavaDoc of NamespaceContext interface
        addNamespace(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        addNamespace(XMLConstants.XMLNS_ATTRIBUTE,XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
        addNamespace(XMLConstants.DEFAULT_NS_PREFIX, "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/");
        addNamespace("didl", "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/");
        addNamespace("dc", "http://purl.org/dc/elements/1.1/");
        addNamespace("dlna", "urn:schemas-dlna-org:metadata-1-0/");
        addNamespace("upnp", "urn:schemas-upnp-org:metadata-1-0/upnp/");
        addNamespace("arib", "urn:schemas-arib-or-jp:elements-1-0/");
        addNamespace("dtcp", "urn:schemas-dtcp-com:metadata-1-0/");
    }

    public synchronized void addNamespace(String prefix, String namespaceURI) {
        urisByPrefix.put(prefix, namespaceURI);
        if (prefixesByURI.containsKey(namespaceURI)) {
            ((Set<String>) prefixesByURI.get(namespaceURI)).add(prefix);
        } else {
            Set<String> set = new HashSet<String>();
            set.add(prefix);
            prefixesByURI.put(namespaceURI, set);
        }
    }

    public String getNamespaceURI(String prefix) {
        if (prefix == null)
            throw new IllegalArgumentException("prefix cannot be null");
        if (urisByPrefix.containsKey(prefix))
            return (String) urisByPrefix.get(prefix);
        else
            return XMLConstants.NULL_NS_URI;
    }

    public String getPrefix(String namespaceURI) {
        return (String) getPrefixes(namespaceURI).next();
    }

    @SuppressWarnings("unchecked")
	public Iterator<String> getPrefixes(String namespaceURI) {
        if (namespaceURI == null)
            throw new IllegalArgumentException("namespaceURI cannot be null");
        if (prefixesByURI.containsKey(namespaceURI)) {
            return ((Set<String>) prefixesByURI.get(namespaceURI)).iterator();
        } else {
            return Collections.EMPTY_SET.iterator();
        }
    }
	}
