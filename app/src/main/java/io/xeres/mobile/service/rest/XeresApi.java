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

package io.xeres.mobile.service.rest;

import java.util.List;

import io.xeres.mobile.service.json.ChatBacklog;
import io.xeres.mobile.service.json.ChatRoomBacklog;
import io.xeres.mobile.service.json.ChatRoomContext;
import io.xeres.mobile.service.json.Contact;
import io.xeres.mobile.service.json.Location;
import io.xeres.mobile.service.json.Profile;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface XeresApi
{
	@GET("profiles/1")
	Call<Profile> getOwnProfile();

	@GET("profiles/{id}")
	Call<Profile> findProfileById(@Path("id") long id);

	@GET("locations/1")
	Call<Location> getOwnLocation();

	@GET("locations/{id}")
	Call<Location> findLocationById(@Path("id") long id);

	@GET("chat/chats/{id}/messages")
	Call<List<ChatBacklog>> getChatMessages(@Path("id") long id);

	@GET("chat/rooms/{id}/messages")
	Call<List<ChatRoomBacklog>> getChatRoomMessages(@Path("id") long id);

	@GET("contacts")
	Call<List<Contact>> getContacts();

	@GET("identities/{id}/image")
	Call<ResponseBody> getImage(@Path("id") long id);

	@GET("chat/rooms")
	Call<ChatRoomContext> getChatRoomContext();
}
