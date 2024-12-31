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

package io.xeres.mobile.ui.chat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import io.xeres.mobile.R;
import io.xeres.mobile.service.ConnectionService;
import io.xeres.mobile.service.LocalBinder;
import io.xeres.mobile.service.json.ChatMessage;
import io.xeres.mobile.util.BitmapUtils;
import io.xeres.mobile.util.Id;
import io.xeres.mobile.util.JsonUtils;
import rx.Subscription;
import ua.naiksoftware.stomp.client.StompMessage;

public class ChatActivity extends AppCompatActivity
{
	private static final String TAG = "ChatActivity";

	private static final Duration TYPING_NOTIFICATION_DELAY = Duration.ofSeconds(5);

	private ConnectionService connectionService;
	private boolean bound;

	private long id;
	private String locationIdentifier;
	private String targetName;
	private Subscription subscription;

	private ChatAdapter chatAdapter;

	private String messageToSend;

	private TextView typingView;

	private Instant lastTypingNotification = Instant.EPOCH;

	private enum MessageType {
		TYPING_NOTIFICATION,
		PRIVATE_MESSAGE,
		OTHER
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_chat);
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
			return insets;
		});

		RecyclerView recyclerView = findViewById(R.id.chat_recycler);
		var layoutManager = new LinearLayoutManager(this);
		layoutManager.setStackFromEnd(true);
		recyclerView.setLayoutManager(layoutManager);

		typingView = findViewById(R.id.typing_view);

		EditText editText = findViewById(R.id.chat_send);
		editText.setOnKeyListener((v, keyCode, event) -> {
			if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))
			{
				sendMessage(editText);
				return true;
			}
			sendTypingNotificationIfNeeded();
			return false;
		});
		editText.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{

			}

			@Override
			public void afterTextChanged(Editable s)
			{
				if (s.length() > 0)
				{
					sendTypingNotificationIfNeeded();
				}
			}
		});

		ImageButton sendButton = findViewById(R.id.chat_send_button);
		sendButton.setOnClickListener(v -> sendMessage(editText));

		var intent = getIntent();

		id = intent.getLongExtra("id", 0L);
		locationIdentifier = Id.toString(Objects.requireNonNull(intent.getByteArrayExtra("locationIdentifier")));
		targetName = intent.getStringExtra("nick");
		var shareUri = intent.getData();
		if (shareUri != null)
		{
			Log.d(TAG, "shareUri: " + shareUri);
			try
			{
				var bitmap = BitmapUtils.getScaledBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), shareUri), 800 * 600);
				bitmap = BitmapUtils.rotateBitmapIfNeeded(bitmap, BitmapUtils.getImageOrientation(getContentResolver(), shareUri));
				var output = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output); // XXX: thread... and also limite the size

				Log.d(TAG, "send size: " + output.size());

				var imageString = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);

				messageToSend = "<img src=\"data:image/jpeg;base64," + imageString + "\"/>"; // XXX: we cannot use the send EditText because it has a limited size... we will have to delay the sending
			}
			catch (FileNotFoundException e)
			{
				Log.e(TAG, "File not found: " + e.getMessage());
			}
			catch (IOException e)
			{
				Log.e(TAG, "I/O error: " + e.getMessage());
			}
		}
		else
		{
			var text = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (text != null)
			{
				editText.setText(text);
			}
		}

		var actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setTitle(targetName);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void sendMessage(EditText editText)
	{
		var message = editText.getText().toString();
		if (!TextUtils.isEmpty(message))
		{
			connectionService.sendChatMessage(locationIdentifier, message);
			lastTypingNotification = Instant.EPOCH;
			editText.setText("");
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		var intent = new Intent(this, ConnectionService.class);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		if (bound)
		{
			unbindService(connection);
			bound = false;
		}
	}

	private void sendTypingNotificationIfNeeded()
	{
		var now = Instant.now();
		if (java.time.Duration.between(lastTypingNotification, now).compareTo(TYPING_NOTIFICATION_DELAY.minusSeconds(1)) > 0)
		{
			connectionService.sendChatMessage(locationIdentifier, null);
			lastTypingNotification = now;
		}
	}

	private final ServiceConnection connection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			var binder = (LocalBinder) service;
			connectionService = binder.getService();
			bound = true;

			connectionService.getChatBacklog(id, chatBacklogs -> {
				Log.d(TAG, "Getting backlogs from location " + id);
				RecyclerView recyclerView = findViewById(R.id.chat_recycler);

				var ownProfile = connectionService.getOwnProfile();
				var ownName = ownProfile != null ? ownProfile.getName() : "Own";

				chatAdapter = new ChatAdapter(ownName, targetName, chatBacklogs, connectionService);
				recyclerView.setAdapter(chatAdapter);

				subscription = connectionService.getMessages("/topic/chat/private")
						.doOnNext(stompMessage -> {
							var messageType = getMessageType(stompMessage);

							if (!isForUs(stompMessage))
							{
								return;
							}

							if (messageType == MessageType.TYPING_NOTIFICATION)
							{
								typingView.setText(targetName + " is typing...");
								typingView.postDelayed(() -> typingView.setText(""), 4000);
							}
							else if (messageType == MessageType.PRIVATE_MESSAGE)
							{
								var chatMessage = JsonUtils.GSON.fromJson(stompMessage.getPayload(), ChatMessage.class);
								//Log.d(TAG, "message: " + chatMessage.getContent());
								if (chatMessage.getContent() != null)
								{
									runOnUiThread(() -> {
										chatAdapter.addIncomingChatMessage(chatMessage);
										recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
									});
								}
							}
						})
						.doOnError(throwable -> Log.e(TAG, "Error: ", throwable))
						.subscribe();

				if (messageToSend != null)
				{
					connectionService.sendChatMessage(locationIdentifier, messageToSend);
					messageToSend = null;
				}
			});
		}

		private MessageType getMessageType(StompMessage stompMessage)
		{
			var messageType = stompMessage.findHeader("messageType");

			if (messageType == null)
			{
				return MessageType.OTHER;
			}
			if (messageType.equals("CHAT_PRIVATE_MESSAGE"))
			{
				return MessageType.PRIVATE_MESSAGE;
			}
			else if (messageType.equals("CHAT_TYPING_NOTIFICATION"))
			{
				return MessageType.TYPING_NOTIFICATION;
			}
			return MessageType.OTHER;
		}

		private boolean isForUs(StompMessage stompMessage)
		{
			var destination = stompMessage.findHeader("destinationId");

			return destination != null && destination.equals(locationIdentifier);
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			bound = false;

			if (subscription != null)
			{
				subscription.unsubscribe();
			}
		}
	};
}