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

package io.xeres.mobile.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import io.xeres.mobile.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment
{

	private FragmentNotificationsBinding binding;

	public View onCreateView(@NonNull LayoutInflater inflater,
	                         ViewGroup container, Bundle savedInstanceState)
	{
		NotificationsViewModel notificationsViewModel =
				new ViewModelProvider(this).get(NotificationsViewModel.class);

		binding = FragmentNotificationsBinding.inflate(inflater, container, false);
		View root = binding.getRoot();

		final TextView textView = binding.textNotifications;
		notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
		return root;
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		binding = null;
	}
}