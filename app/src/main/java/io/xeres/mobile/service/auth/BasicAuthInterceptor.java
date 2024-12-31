/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
 *
 * This file is part of Xeres-Android.
 *
 * Xeres-Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeres-Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres-Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.mobile.service.auth;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Response;

public class BasicAuthInterceptor implements Interceptor
{
	private final String credentials;

	public BasicAuthInterceptor(String username, String password)
	{
		this.credentials = Credentials.basic(username, password);
	}

	@NonNull
	@Override
	public Response intercept(@NonNull Chain chain) throws IOException
	{
		var request = chain.request();
		var authenticatedRequest = request.newBuilder()
				.header("Authorization", credentials).build();
		return chain.proceed(authenticatedRequest);
	}

}
