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

public final class FormatException extends Exception {

	private static final long serialVersionUID = 1L;

	public FormatException() {
	}

	public FormatException(String detailMessage) {
		super(detailMessage);
	}

	public FormatException(Throwable throwable) {
		super(throwable);
	}

	public FormatException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
