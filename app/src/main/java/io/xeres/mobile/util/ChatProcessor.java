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
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.commonmark.node.Heading;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.ThematicBreak;
import org.jsoup.Jsoup;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import io.xeres.mobile.view.AsyncImageView;

public class ChatProcessor
{
	private final Context context;
	private final Markwon markwon;

	public ChatProcessor(Context context)
	{
		this.context = context;
		markwon = Markwon.builder(context)
				.usePlugin(StrikethroughPlugin.create())
				.usePlugin(LinkifyPlugin.create(true))
				.usePlugin(new AbstractMarkwonPlugin()
		{
			@Override
			public void configureVisitor(@NonNull MarkwonVisitor.Builder builder)
			{
				builder.on(Heading.class, (visitor, heading) -> {
					visitor.builder().append("#".repeat(heading.getLevel())).append(" ");
					visitor.visitChildren(heading);
				});
				builder.on(ThematicBreak.class, (visitor, thematicBreak) -> visitor.builder().append("---"));
				builder.on(HtmlInline.class, (visitor, htmlInline) -> {
					// Display inline tags we don't know about
					visitor.builder().append(htmlInline.getLiteral());
				});
			}
		}).build();
	}

	public void processLine(String colorInput, String nickname, String message, boolean isOwn, TextView textView, AsyncImageView imageView)
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

		var spanMessage = markwon.render(markwon.parse(message));
		var spanNickname = new SpannableString(formattedNickname + " ");
		if (!isOwn)
		{
			var color = ContextCompat.getColor(context, ColorGenerator.generateColor(colorInput));
			spanNickname.setSpan(new ForegroundColorSpan(color), 0, formattedNickname.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE); // XXX: can be simplified, perhaps
		}

		var combined = new SpannableStringBuilder();
		combined.append(spanNickname);
		combined.append(spanMessage);

		textView.setText(combined);
	}
}
