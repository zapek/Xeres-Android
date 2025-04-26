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

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import io.xeres.mobile.R;
import io.xeres.mobile.service.json.ChatRoomBacklog;
import io.xeres.mobile.service.json.ChatRoomMessage;
import io.xeres.mobile.util.ChatUtils;
import io.xeres.mobile.view.AsyncImageView;

class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder>
{
	private static final String TAG = "ChatRoomAdapter";

	private final String ownName;
	private final long roomId;
	private final List<ChatRoomBacklog> backlogs;
	private final AsyncImageView.ImageInput imageInput;

	private static final DateTimeFormatter TIME_DISPLAY = DateTimeFormatter.ofPattern("HH:mm")
			.withLocale(Locale.ROOT)
			.withZone(ZoneId.systemDefault());

	public ChatRoomAdapter(String ownName, long roomId, List<ChatRoomBacklog> backlogs, AsyncImageView.ImageInput imageInput)
	{
		this.ownName = ownName;
		this.roomId = roomId;
		this.backlogs = backlogs;
		this.imageInput = imageInput;
	}

	@NonNull
	@Override
	public ChatRoomAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_row_item, parent, false);
		return new ChatRoomAdapter.ViewHolder(view);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull ChatRoomAdapter.ViewHolder holder, int position)
	{
		var line = backlogs.get(position);
		holder.getAsyncImageView().setImageInput(imageInput);
		processChatLine(holder, line);
		holder.getTimeView().setText(TIME_DISPLAY.format(line.getCreated()));
	}

	@Override
	public int getItemCount()
	{
		return backlogs.size();
	}

	public void addIncomingChatMessage(ChatRoomMessage chatRoomMessage)
	{
		backlogs.add(new ChatRoomBacklog(chatRoomMessage.getSenderNickname(), chatRoomMessage.getGxsId(), chatRoomMessage.getContent()));
		notifyItemInserted(backlogs.size() - 1);
	}

	private void processChatLine(ChatRoomAdapter.ViewHolder holder, ChatRoomBacklog line)
	{
		var nickname = line.getGxsId() == null ? ownName : line.getNickname();
		var isOwn = line.getGxsId() == null;
		ChatUtils.processChatLine(holder.getTextView().getContext(), isOwn ? nickname : line.getGxsId().toString(), nickname, line.getMessage(), isOwn, holder.getTextView(), holder.getAsyncImageView());
	}

	public static class ViewHolder extends RecyclerView.ViewHolder
	{
		private final TextView textView;
		private final AsyncImageView asyncImageView;
		private final TextView timeView;

		public ViewHolder(View view)
		{
			super(view);
			textView = view.findViewById(R.id.textView);
			asyncImageView = view.findViewById(R.id.imageView);
			timeView = view.findViewById(R.id.textTime);
		}

		public TextView getTextView()
		{
			return textView;
		}

		public AsyncImageView getAsyncImageView()
		{
			return asyncImageView;
		}

		public TextView getTimeView()
		{
			return timeView;
		}
	}
}
