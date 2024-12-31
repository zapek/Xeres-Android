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

package io.xeres.mobile.ui.contacts;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.List;

import io.xeres.mobile.OnExpandedListItemClickListener;
import io.xeres.mobile.R;
import io.xeres.mobile.service.json.Availability;
import io.xeres.mobile.service.json.Contact;
import io.xeres.mobile.view.AsyncImageView;
import io.xeres.mobile.view.TreeItem;

class ContactsAdapter extends BaseExpandableListAdapter
{
	private static final String TAG = "ContactsAdapter";

	private static final int[] EMPTY_STATE_SET = {};
	private static final int[] GROUP_EXPANDED_STATE_SET = {android.R.attr.state_expanded};
	private static final int[][] INDICATOR_STATE_SETS = {
			EMPTY_STATE_SET,
			GROUP_EXPANDED_STATE_SET
	};

	private final List<TreeItem<Contact>> contacts;
	private final AsyncImageView.ImageInput imageInput;
	private final OnExpandedListItemClickListener<Contact> listener;

	public ContactsAdapter(List<TreeItem<Contact>> contacts, AsyncImageView.ImageInput imageInput, OnExpandedListItemClickListener<Contact> listener)
	{
		this.contacts = contacts;
		this.imageInput = imageInput;
		this.listener = listener;
	}

	@Override
	public Object getGroup(int groupPosition)
	{
		return contacts.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition)
	{
		return contacts.get(groupPosition).getChildren().get(childPosition);
	}

	@Override
	public int getGroupCount()
	{
		return contacts.size();
	}

	@Override
	public int getChildrenCount(int groupPosition)
	{
		return contacts.get(groupPosition).getChildren().size();
	}

	@Override
	public long getGroupId(int groupPosition)
	{
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition)
	{
		return childPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
	{
		var isLeaf = getChildrenCount(groupPosition) == 0;
		TreeItem<Contact> contact = (TreeItem<Contact>) getGroup(groupPosition);
		var view = makeView(contact.getValue(), convertView, parent, R.layout.contact_row_group_item);

		ImageView indicatorView = view.findViewById(R.id.indicatorView);
		if (indicatorView != null)
		{
			if (isLeaf)
			{
				indicatorView.setVisibility(View.INVISIBLE);
			}
			else
			{
				indicatorView.setVisibility(View.VISIBLE);
				int stateSetIndex = isExpanded ? 1 : 0;
				var drawable = indicatorView.getDrawable();
				drawable.setState(INDICATOR_STATE_SETS[stateSetIndex]);
			}
		}

		return view;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
	{
		TreeItem<Contact> contact = (TreeItem<Contact>) getChild(groupPosition, childPosition);
		return makeView(contact.getValue(), convertView, parent, R.layout.contact_row_child_item);
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition)
	{
		return true;
	}

	private View makeView(Contact contact, View convertView, ViewGroup parent, @LayoutRes int resource)
	{
		if (convertView == null)
		{
			convertView = LayoutInflater.from(parent.getContext()).inflate(resource, parent, false);
		}

		LinearLayout groupView = convertView.findViewById(R.id.groupView);
		if (groupView != null)
		{
			groupView.setOnClickListener(v -> listener.onExpandedListViewItemClicked(contact));
		}

		AsyncImageView avatarView = convertView.findViewById(R.id.avatarView);
		TextView textView = convertView.findViewById(R.id.textView);
		ImageView statusView = convertView.findViewById(R.id.statusView);

		avatarView.setImageInput(imageInput);
		if (contact.getIdentityId() != 0L)
		{
			avatarView.setImageUrl(String.valueOf(contact.getIdentityId()));
		}
		else
		{
			avatarView.setImageUrl(null);
		}
		textView.setText(contact.getName());
		statusView.setImageDrawable(getDrawableForAvailability(statusView, contact.getAvailability()));
		return convertView;
	}

	@Override
	public boolean hasStableIds()
	{
		return false;
	}

	private Drawable getDrawableForAvailability(ImageView imageView, Availability availability)
	{
		int id = getResourceForAvailability(availability);

		if (id == 0)
		{
			return null;
		}
		return AppCompatResources.getDrawable(imageView.getContext(), id);
	}

	private int getResourceForAvailability(Availability availability)
	{
		switch (availability)
		{
			case AVAILABLE:
				return R.drawable.ic_status_connected_24;

			case AWAY:
				return R.drawable.ic_status_away_24;

			case BUSY:
				return R.drawable.ic_status_busy_24;

			default:
				return 0;
		}
	}
}
