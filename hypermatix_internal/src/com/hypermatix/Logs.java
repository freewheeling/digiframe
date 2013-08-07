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

package com.hypermatix;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for LogProvider
 */
public final class Logs {
    public static final String AUTHORITY = "com.hypermatix.digiframe";

    // This class cannot be instantiated
    private Logs() {}
    
    /**
     * Logs table
     */
    public static final class LogItem implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI
                = Uri.parse("content://" + Logs.AUTHORITY + "/logs");

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "logDate DESC";

        /**
         * The detail of the log item 
         * <P>Type: TEXT</P>
         */
        public static final String DETAIL = "detail";
        
        /**
         * The time of the log item 
         * <P>Type: TEXT</P>
         */
        public static final String DATE = "logDate";
        
    	public static final String TAG = "tag";
    	public static final String LEVEL = "level";
    	public static final String ENTITY = "entityId";
    	public static final String EXTRA = "extra";

    }
}
