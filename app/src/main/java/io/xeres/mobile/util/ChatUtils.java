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

package io.xeres.mobile.util;

import android.content.Context;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.jsoup.Jsoup;

import io.xeres.mobile.view.AsyncImageView;

public final class ChatUtils
{
	private ChatUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void processChatLine(Context context, String colorInput, String nickname, String message, boolean isOwn, TextView textView, AsyncImageView imageView)
	{
		String data = null;

		var formattedNickname = "<" + nickname + ">";

		var img = Jsoup.parse(message).selectFirst("img");
		if (img != null)
		{
			var imgUrl = img.absUrl("src");
			if (!TextUtils.isEmpty(imgUrl) && imgUrl.startsWith("data:"))
			{
				data = imgUrl;
			}
		}
		else if (message.startsWith("![](data:"))
		{
			data = message.substring(4);
		}

		if (data != null)
		{
			imageView.setImageUrl(data);
			imageView.setVisibility(View.VISIBLE);
			message = "";
		}
		else
		{
			imageView.setImageUrl(null);
			imageView.setVisibility(View.GONE);
		}

		if (isOwn)
		{
			textView.setText(formattedNickname + " " + message);
		}
		else
		{
			var spannableString = new SpannableString(formattedNickname + " " + message);
			var color = ContextCompat.getColor(context, ColorGenerator.generateColor(colorInput));

			spannableString.setSpan(new ForegroundColorSpan(color), 0, formattedNickname.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
			textView.setText(spannableString);
		}
	}
}
