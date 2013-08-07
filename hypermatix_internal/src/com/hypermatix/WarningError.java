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

public final class WarningError extends Error {

	private static final long serialVersionUID = 1L;

	public WarningError() {
	}

	public WarningError(String detailMessage) {
		super(detailMessage);
	}

	public WarningError(Throwable throwable) {
		super(throwable);
	}

	public WarningError(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
