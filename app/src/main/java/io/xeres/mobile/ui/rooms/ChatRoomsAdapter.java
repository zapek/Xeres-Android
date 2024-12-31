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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.xeres.mobile.OnRecyclerViewItemClickListener;
import io.xeres.mobile.R;
import io.xeres.mobile.service.json.ChatRoom;

class ChatRoomsAdapter extends RecyclerView.Adapter<ChatRoomsAdapter.ViewHolder>
{
	private static final String TAG = "RoomsAdapter";

	private final List<ChatRoom> chatRooms;
	private final OnRecyclerViewItemClickListener listener;

	public ChatRoomsAdapter(List<ChatRoom> chatRooms, OnRecyclerViewItemClickListener listener)
	{
		this.chatRooms = chatRooms;
		this.listener = listener;
	}

	@NonNull
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.room_row_item, parent, false);
		return new ViewHolder(view, listener);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position)
	{
		var room = chatRooms.get(position);
		holder.getTextView().setText(room.getName());
	}

	@Override
	public int getItemCount()
	{
		return chatRooms.size();
	}

	public static class ViewHolder extends RecyclerView.ViewHolder
	{
		private final TextView textView;

		public ViewHolder(View view, OnRecyclerViewItemClickListener listener)
		{
			super(view);
			view.setOnClickListener(v -> listener.onRecyclerViewItemClicked(getAdapterPosition()));
			textView = view.findViewById(R.id.textView);
		}

		public TextView getTextView()
		{
			return textView;
		}
	}
}
