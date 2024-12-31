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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import io.xeres.mobile.MainActivity;
import io.xeres.mobile.databinding.FragmentContactsBinding;
import io.xeres.mobile.service.ConnectionService;
import io.xeres.mobile.service.LocalBinder;
import io.xeres.mobile.service.json.Contact;
import io.xeres.mobile.service.json.Location;
import io.xeres.mobile.service.json.Profile;
import io.xeres.mobile.ui.chat.ChatActivity;
import io.xeres.mobile.view.AsyncImageView;
import io.xeres.mobile.view.TreeItem;

public class ContactsFragment extends Fragment
{
	private static final String TAG = "ContactsFragment";

	private FragmentContactsBinding binding;
	private ConnectionService connectionService;
	private boolean bound;

	public View onCreateView(@NonNull LayoutInflater inflater,
	                         ViewGroup container, Bundle savedInstanceState)
	{
		binding = FragmentContactsBinding.inflate(inflater, container, false);
		var listView = binding.contactsListview;
		return binding.getRoot();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
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

	void setContacts(List<Contact> contacts, AsyncImageView.ImageInput imageInput)
	{
		var filteredContacts = sortContacts(getContacts(filterContacts(contacts)));

		binding.contactsListview.setAdapter(new ContactsAdapter(filteredContacts, imageInput, contact -> connectionService.findProfileById(contact.getProfileId(), profile -> {
			var intent = new Intent(getContext(), ChatActivity.class);
			var location = getFirstConnectedLocation(profile);
			if (location != null)
			{
				intent.putExtra("id", location.getId());
				intent.putExtra("locationIdentifier", location.getLocationIdentifier());
				intent.putExtra("nick", contact.getName());
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
			}
		})));
	}

	private Location getFirstConnectedLocation(Profile profile)
	{
		if (profile == null || profile.getLocations().isEmpty())
		{
			return null;
		}

		return profile.getLocations().stream()
				.filter(Location::isConnected)
				.findFirst()
				.orElse(null);
	}

	private List<Contact> filterContacts(List<Contact> contacts)
	{
		return contacts.stream()
				.filter(Contact::isAccepted)
				.collect(Collectors.toList());
	}

	private List<TreeItem<Contact>> sortContacts(List<TreeItem<Contact>> contacts)
	{
		return contacts.stream()
				.sorted(Comparator.comparing(contactTreeItem -> contactTreeItem.getValue().getName().toLowerCase(Locale.ROOT)))
				.collect(Collectors.toList());
	}

	private List<TreeItem<Contact>> getContacts(List<Contact> incomingContacts)
	{
		Map<Long, TreeItem<Contact>> contacts = new HashMap<>();
		List<TreeItem<Contact>> identities = new ArrayList<>();

		incomingContacts.forEach(contact -> {
			if (contact.getProfileId() != 0L)
			{
				if (contact.getIdentityId() != 0L)
				{
					if (contact.getIdentityId() == 1L || contact.getProfileId() == 1L)
					{
						return;
					}
					if (contacts.containsKey(contact.getProfileId()))
					{
						var profile = contacts.get(contact.getProfileId());
						updateProfileWithIdentity(profile, new TreeItem<>(contact));
					}
					else
					{
						contacts.put(contact.getProfileId(), new TreeItem<>(contact));
					}
				}
				else
				{
					if (contact.getProfileId() == 1L)
					{
						return;
					}
					if (contacts.put(contact.getProfileId(), new TreeItem<>(contact)) != null)
					{
						throw new IllegalStateException("Profile overwritten");
					}
				}
			}
			else
			{
				identities.add(new TreeItem<>(contact));
			}
		});

		List<TreeItem<Contact>> resultList = new ArrayList<>();
		resultList.addAll(contacts.values());
		//resultList.addAll(identities); XXX: add later, perhaps
		return resultList;
	}

	private void updateProfileWithIdentity(TreeItem<Contact> profile, TreeItem<Contact> identity)
	{
		if (profile.getValue().getIdentityId() != 0L)
		{
			// Profile with an identity already
			if (profile.getValue().getIdentityId() == identity.getValue().getIdentityId())
			{
				// Same identity, we replace it
				profile.setValue(identity.getValue());
				refreshContactIfNeeded(profile);
			}
			else
			{
				if (profile.getChildren().isEmpty())
				{
					// Not the same, we replace if we have a matching name
					if (!replaceIfSameName(profile, identity))
					{
						profile.getChildren().add(identity);
					}
				}
				else
				{
					if (!replaceIfSameName(profile, identity))
					{
						replaceOrAddChildren(profile, identity);
					}
				}
			}
		}
		else
		{
			// Lone profile that gets an identity added
			if (!replaceIfSameName(profile, identity))
			{
				profile.getChildren().add(identity);
			}
		}
	}

	private boolean replaceIfSameName(TreeItem<Contact> profile, TreeItem<Contact> identity)
	{
		if (profile.getValue().getName().equalsIgnoreCase(identity.getValue().getName()))
		{
			profile.setValue(identity.getValue());
			refreshContactIfNeeded(profile);
			return true;
		}
		return false;
	}

	private void replaceOrAddChildren(TreeItem<Contact> parent, TreeItem<Contact> identity)
	{
		for (TreeItem<Contact> child : parent.getChildren())
		{
			if (child.getValue().getIdentityId() == identity.getValue().getIdentityId())
			{
				child.setValue(identity.getValue());
				refreshContactIfNeeded(child);
				return;
			}
		}
		parent.getChildren().add(identity);
	}

	private void refreshContactIfNeeded(TreeItem<Contact> contact)
	{
		// XXX: nothing for now
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

			connectionService.getContacts(contacts -> setContacts(contacts, connectionService));
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			bound = false;
		}
	};
}