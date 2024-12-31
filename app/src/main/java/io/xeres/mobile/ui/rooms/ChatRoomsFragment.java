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

package io.xeres.mobile.ui.rooms;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.xeres.mobile.MainActivity;
import io.xeres.mobile.databinding.FragmentRoomsBinding;
import io.xeres.mobile.service.ConnectionService;
import io.xeres.mobile.service.LocalBinder;
import io.xeres.mobile.service.json.ChatRoom;
import io.xeres.mobile.service.json.ChatRoomContext;

public class ChatRoomsFragment extends Fragment
{
	private static final String TAG = "RoomsFragment";

	private FragmentRoomsBinding binding;
	private ConnectionService connectionService;
	private boolean bound;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		binding = FragmentRoomsBinding.inflate(inflater, container, false);
		var recyclerView = binding.roomsRecycler;
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		recyclerView.setHasFixedSize(true); // XXX: be careful with that...
		return binding.getRoot();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.d(TAG, "Binding to service...");
		var intent = new Intent(getContext(), ConnectionService.class);
		requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop()
	{
		super.onStop();
		if (bound)
		{
			requireActivity().unbindService(connection);
			bound = false;
		}
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		binding = null;
	}

	private void setChatRoomContext(ChatRoomContext chatRoomContext)
	{
		var sortedChatRooms = sortChatRooms(chatRoomContext.getChatRooms().getSubscribed());
		binding.roomsRecycler.setAdapter(new ChatRoomsAdapter(sortedChatRooms, position -> {
			var chatRoom = sortedChatRooms.get(position);
			var intent = new Intent(getContext(), ChatRoomActivity.class);
			intent.putExtra("id", chatRoom.getId());
			intent.putExtra("nick", chatRoomContext.getIdentity().getNickname());
			intent.putExtra("roomName", chatRoom.getName());
			var mainActivity = (MainActivity) getActivity();
			assert mainActivity != null;
			var textToShare = mainActivity.getTextToShare();
			if (textToShare != null)
			{
				intent.putExtra(Intent.EXTRA_TEXT, textToShare);
				mainActivity.clearSharingUris();
			}
			else
			{
				var imageToShare = mainActivity.getImageToShare();
				if (imageToShare != null)
				{
					intent.setData(imageToShare);
					mainActivity.clearSharingUris();
				}
			}
			startActivity(intent);
		}));
	}

	private List<ChatRoom> sortChatRooms(List<ChatRoom> chatRooms)
	{
		return chatRooms.stream()
				.sorted(Comparator.comparing(ChatRoom::getName))
				.collect(Collectors.toList());
	}

	private final ServiceConnection connection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			var binder = (LocalBinder) service;
			connectionService = binder.getService();
			bound = true;
			Log.d(TAG, "Bound to service");

			connectionService.getChatRoomContext(chatRoomContext -> setChatRoomContext(chatRoomContext));
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			bound = false;
		}
	};
}