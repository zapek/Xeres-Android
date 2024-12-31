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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NotificationsViewModel extends ViewModel
{

	private final MutableLiveData<String> mText;

	public NotificationsViewModel()
	{
		mText = new MutableLiveData<>();
		mText.setValue("This is notifications fragment");
	}

	public LiveData<String> getText()
	{
		return mText;
	}
}