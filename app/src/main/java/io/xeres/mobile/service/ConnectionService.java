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

package io.xeres.mobile.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

import io.xeres.mobile.service.auth.BasicAuthInterceptor;
import io.xeres.mobile.service.json.ChatBacklog;
import io.xeres.mobile.service.json.ChatMessage;
import io.xeres.mobile.service.json.ChatRoomBacklog;
import io.xeres.mobile.service.json.ChatRoomContext;
import io.xeres.mobile.service.json.Contact;
import io.xeres.mobile.service.json.Location;
import io.xeres.mobile.service.json.Profile;
import io.xeres.mobile.service.rest.XeresApi;
import io.xeres.mobile.util.JsonUtils;
import io.xeres.mobile.view.AsyncImageView;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompHeader;
import ua.naiksoftware.stomp.client.StompClient;
import ua.naiksoftware.stomp.client.StompCommand;
import ua.naiksoftware.stomp.client.StompMessage;

public class ConnectionService extends Service implements AsyncImageView.ImageInput
{
	private static final String TAG = "ConnectionService";

	private XeresApi xeresApiClient;

	private OkHttpClient okHttpClient;

	private StompClient stompClient;

	private final IBinder binder;

	private Profile ownProfile;

	public ConnectionService()
	{
		Log.d(TAG, "Creating service...");

		binder = new LocalBinder(this);
	}

	private void initializeClientsIfNeeded(SharedPreferences prefs)
	{
		var liberalSslContext = new LiberalSslContext(getApplicationContext());

		var sslSocketFactory = liberalSslContext.getSocketFactory();

		okHttpClient = new OkHttpClient.Builder()
				.sslSocketFactory(sslSocketFactory, liberalSslContext.getLiberalCert())
				.hostnameVerifier((hostname, session) -> true)
				.addInterceptor(new BasicAuthInterceptor("user", prefs.getString("password", "")))
				.build();

		var retrofit = new Retrofit.Builder()
				.baseUrl("https://" + prefs.getString("hostname", "localhost") + ":" + prefs.getString("port", "1024") + "/api/v1/")
				.client(okHttpClient)
				.addConverterFactory(GsonConverterFactory.create(JsonUtils.GSON))
				.build();

		xeresApiClient = retrofit.create(XeresApi.class);

		fetchOwnProfile(profile -> ownProfile = profile);
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent)
	{
		var prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		initializeClientsIfNeeded(prefs);

		stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "wss://" + prefs.getString("hostname", "localhost") + ":" + prefs.getString("port", "1024") + "/ws", null, okHttpClient);
		stompClient.connect(List.of(new StompHeader("accept-version", "1.1,1.0"))); // The header seems to be missing somehow

		Log.d(TAG, "Stomp client connected");
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		if (stompClient != null && stompClient.isConnected())
		{
			stompClient.disconnect();
		}
		return false;
	}

	public void getChatRoomContext(Consumer<ChatRoomContext> consumer)
	{
		xeresApiClient.getChatRoomContext().enqueue(new Callback<>()
		{
			@Override
			public void onResponse(Call<ChatRoomContext> call, Response<ChatRoomContext> response)
			{
				consumer.accept(response.body());
			}

			@Override
			public void onFailure(Call<ChatRoomContext> call, Throwable throwable)
			{
				showError(throwable);
			}
		});
	}

	public void getChatRoomBacklog(long roomId, Consumer<List<ChatRoomBacklog>> consumer)
	{
		xeresApiClient.getChatRoomMessages(roomId).enqueue(new Callback<>()
		{
			@Override
			public void onResponse(Call<List<ChatRoomBacklog>> call, Response<List<ChatRoomBacklog>> response)
			{
				if (response.isSuccessful())
				{
					Log.d(TAG, "Got chat room backlogs!");
					consumer.accept(response.body());
				}
				else
				{
					Log.e(TAG, "Error HTTP: " + response.code());
				}
			}

			@Override
			public void onFailure(Call<List<ChatRoomBacklog>> call, Throwable throwable)
			{
				showError(throwable);
			}
		});
	}

	public Profile getOwnProfile()
	{
		return ownProfile;
	}

	private void fetchOwnProfile(Consumer<Profile> consumer)
	{
		xeresApiClient.getOwnProfile().enqueue(new Callback<>()
		{
			@Override
			public void onResponse(Call<Profile> call, Response<Profile> response)
			{
				if (response.isSuccessful())
				{
					Log.d(TAG, "Own profile: " + response.body());
					consumer.accept(response.body());
				}
				else
				{
					Log.e(TAG, "Error HTTP: " + response.code());
				}
			}

			@Override
			public void onFailure(Call<Profile> call, Throwable throwable)
			{
				showError(throwable);
			}
		});
	}

	public void findProfileById(long id, Consumer<Profile> consumer)
	{
		xeresApiClient.findProfileById(id).enqueue(new Callback<>()
		{
			@Override
			public void onResponse(Call<Profile> call, Response<Profile> response)
			{
				if (response.isSuccessful())
				{
					consumer.accept(response.body());
				}
				else
				{
					Log.e(TAG, "Error HTTP: " + response.code());
				}
			}

			@Override
			public void onFailure(Call<Profile> call, Throwable throwable)
			{
				showError(throwable);
			}
		});
	}

	public void findLocationById(long id, Consumer<Location> consumer)
	{
		xeresApiClient.findLocationById(id).enqueue(new Callback<>()
		{
			@Override
			public void onResponse(Call<Location> call, Response<Location> response)
			{
				if (response.isSuccessful())
				{
					consumer.accept(response.body());
				}
				else
				{
					Log.e(TAG, "Error HTTP: " + response.code());
				}
			}

			@Override
			public void onFailure(Call<Location> call, Throwable throwable)
			{
				showError(throwable);
			}
		});
	}

	public void getChatBacklog(long locationId, Consumer<List<ChatBacklog>> consumer)
	{
		xeresApiClient.getChatMessages(locationId).enqueue(new Callback<>()
		{
			@Override
			public void onResponse(Call<List<ChatBacklog>> call, Response<List<ChatBacklog>> response)
			{
				if (response.isSuccessful())
				{
					Log.d(TAG, "Got chat backlogs!");
					consumer.accept(response.body());
				}
				else
				{
					Log.e(TAG, "Error HTTP: " + response.code());
				}
			}

			@Override
			public void onFailure(Call<List<ChatBacklog>> call, Throwable throwable)
			{
				showError(throwable);
			}
		});
	}

	public void getContacts(Consumer<List<Contact>> consumer)
	{
		Log.d(TAG, "Calling getContacts...");
		xeresApiClient.getContacts().enqueue(new Callback<>()
		{
			@Override
			public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response)
			{
				if (response.isSuccessful())
				{
					consumer.accept(response.body());
				}
				else
				{
					Log.e(TAG, "Error HTTP: " + response.code());
				}
			}

			@Override
			public void onFailure(Call<List<Contact>> call, Throwable throwable)
			{
				showError(throwable);
			}
		});
	}

	public Observable<StompMessage> getMessages(String topic)
	{
		return stompClient.topic(topic);
	}

	public void sendChatMessage(String locationIdentifier, String message)
	{
		var headers = List.of(new StompHeader(StompHeader.DESTINATION, "/app/chat/private"),
				new StompHeader("messageType", message == null ? "CHAT_TYPING_NOTIFICATION" : "CHAT_PRIVATE_MESSAGE"),
				new StompHeader("destinationId", locationIdentifier));
		sendMessage(message, headers);
	}

	public void sendChatRoomMessage(long roomId, String message)
	{

		var headers = List.of(new StompHeader(StompHeader.DESTINATION, "/app/chat/room"),
				new StompHeader("messageType", message == null ? "CHAT_ROOM_TYPING_NOTIFICATION" : "CHAT_ROOM_MESSAGE"),
				new StompHeader("destinationId", String.valueOf(roomId)));
		sendMessage(message, headers);
	}

	private void sendMessage(String message, List<StompHeader> headers)
	{
		var chatMessage = new ChatMessage(message);
		var stompMessage = new StompMessage(StompCommand.SEND, headers, JsonUtils.GSON.toJson(chatMessage));
		Log.d(TAG, "Called stomp client");
		stompClient.send(stompMessage).subscribe(() -> Log.d(TAG, "message sent!"),
				throwable -> Log.e(TAG, "Error while sending data", throwable));
	}

	@Override
	public AsyncImageView.ImageConnection getImageConnection()
	{
		return new ImageFetcher(xeresApiClient);
	}

	public static class ImageFetcher implements AsyncImageView.ImageConnection
	{
		private final XeresApi xeresApiClient;
		private String url;
		private InputStream input;

		public ImageFetcher(XeresApi xeresApiClient)
		{
			this.xeresApiClient = xeresApiClient;
		}

		@Override
		public void connect(String url) throws IOException
		{
			this.url = url;
		}

		@Override
		public void disconnect()
		{
			if (input != null)
			{
				try
				{
					input.close();
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		public InputStream getInputStream() throws IOException
		{
			if (url.startsWith("data:"))
			{
				var decoded = Base64.decode(url.substring(url.indexOf(',') + 1), Base64.NO_WRAP);
				return new ByteArrayInputStream(decoded);
			}
			else
			{
				Response<ResponseBody> response = xeresApiClient.getImage(Long.parseLong(url)).execute();
				if (response.isSuccessful())
				{
					try (var body = response.body())
					{
						if (body != null)
						{
							input = body.byteStream();
						}
					}
				}
			}
			return input;
		}
	}

	private void showError(Throwable throwable)
	{
		Toast.makeText(this, "Network error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
		Log.e(TAG, "Error: " + throwable.getMessage());
	}
}
