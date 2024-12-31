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

import android.annotation.SuppressLint;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.jsoup.Jsoup;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import io.xeres.mobile.R;
import io.xeres.mobile.service.json.ChatBacklog;
import io.xeres.mobile.service.json.ChatMessage;
import io.xeres.mobile.util.ColorGenerator;
import io.xeres.mobile.view.AsyncImageView;

class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder>
{
	private static final String TAG = "ChatAdapter";

	private final String ownName;
	private final String targetName;
	private final List<ChatBacklog> backlogs;
	private final AsyncImageView.ImageInput imageInput;

	private static final DateTimeFormatter TIME_DISPLAY = DateTimeFormatter.ofPattern("HH:mm")
			.withLocale(Locale.ROOT)
			.withZone(ZoneId.systemDefault());

	public ChatAdapter(String ownName, String targetName, List<ChatBacklog> backlogs, AsyncImageView.ImageInput imageInput)
	{
		this.ownName = ownName;
		this.targetName = targetName;
		this.backlogs = backlogs;
		this.imageInput = imageInput;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_row_item, parent, false);
		return new ViewHolder(view);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position)
	{
		var line = backlogs.get(position);
		processChatLine(holder, line);
		holder.getTimeView().setText(TIME_DISPLAY.format(line.getCreated()));
	}

	@Override
	public int getItemCount()
	{
		return backlogs.size();
	}

	public void addIncomingChatMessage(ChatMessage chatMessage)
	{
		backlogs.add(new ChatBacklog(chatMessage.isOwn(), chatMessage.getContent()));
		notifyItemInserted(backlogs.size() - 1);
	}

	private void processChatLine(ViewHolder holder, ChatBacklog line)
	{
		var nickname = line.isOwn() ? ownName : targetName;
		var formattedNickname = "<" + nickname + ">";
		String message;

		var img = Jsoup.parse(line.getMessage()).selectFirst("img");
		if (img != null)
		{
			var data = img.absUrl("src");
			if (!TextUtils.isEmpty(data) && data.startsWith("data:"))
			{
				holder.getAsyncImageView().setImageInput(imageInput);
				holder.getAsyncImageView().setImageUrl(data);
			}
			holder.getAsyncImageView().setVisibility(View.VISIBLE);
			holder.getTextView().setVisibility(View.GONE);
		}
		else
		{
			message = line.getMessage();
			holder.getAsyncImageView().setVisibility(View.GONE);
			holder.getAsyncImageView().setImageUrl(null);
			if (line.isOwn())
			{
				holder.getTextView().setText(formattedNickname + " " + message);
			}
			else
			{
				var spannableString = new SpannableString(formattedNickname + " " + message);
				var color = ContextCompat.getColor(holder.getTextView().getContext(), ColorGenerator.generateColor(nickname));

				spannableString.setSpan(new ForegroundColorSpan(color), 0, formattedNickname.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
				holder.getTextView().setText(spannableString);
			}
			holder.getTextView().setVisibility(View.VISIBLE);
		}
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