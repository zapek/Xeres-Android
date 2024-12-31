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

package io.xeres.mobile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import io.xeres.mobile.databinding.ActivityMainBinding;
import io.xeres.mobile.service.ConnectionService;
import io.xeres.mobile.service.LocalBinder;
import io.xeres.mobile.ui.AboutActivity;
import io.xeres.mobile.ui.SettingsActivity;

public class MainActivity extends AppCompatActivity
{
	private static final String TAG = "MainActivity";

	private ActivityMainBinding binding;
	private ConnectionService connectionService;
	private boolean bound;

	private Uri imageToShare;
	private String textToShare;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		BottomNavigationView navView = findViewById(R.id.nav_view);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
				R.id.navigation_home, R.id.navigation_contacts, R.id.navigation_rooms/*, R.id.navigation_notifications*/)
				.build();
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
		NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
		NavigationUI.setupWithNavController(binding.navView, navController);

		var intent = getIntent();
		handleIntent(intent);
	}

	@Override
	protected void onNewIntent(@NonNull Intent intent)
	{
		super.onNewIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent)
	{
		if (intent != null)
		{
			var action = intent.getAction();
			var type = intent.getType();

			if (Intent.ACTION_SEND.equals(action) && type != null)
			{
				if (type.equals("text/plain"))
				{
					handleReceivedLink(intent);
				}
				else if (type.startsWith("image/"))
				{
					handleReceivedImage(intent);
				}
				// XXX: multiple image needs (ACTION_SEND_MULTIPLE)
			}
		}
	}

	private void handleReceivedImage(Intent intent)
	{
		var imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		if (imageUri != null)
		{
			Log.d(TAG, "Received image url: " + imageUri);
			imageToShare = imageUri;
			showShareInformation(true);
			var navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
			navController.navigate(R.id.navigation_contacts);
		}
	}

	private void handleReceivedLink(Intent intent)
	{
		var text = intent.getStringExtra(Intent.EXTRA_TEXT);
		if (text != null)
		{
			Log.d(TAG, "Received text: " + text);
			textToShare = text;
			showShareInformation(true);
			var navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
			navController.navigate(R.id.navigation_contacts);
		}
	}

	private void showShareInformation(boolean visible)
	{
		binding.shareIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.top_nav_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		if (item.getItemId() == R.id.settings)
		{
			var intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		else if (item.getItemId() == R.id.about)
		{
			var intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		var intent = new Intent(this, ConnectionService.class);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		unbindService(connection);
		bound = false;
	}

	public Uri getImageToShare()
	{
		return imageToShare;
	}

	public String getTextToShare()
	{
		return textToShare;
	}

	public void clearSharingUris()
	{
		imageToShare = null;
		textToShare = null;
		showShareInformation(false);
	}

	private final ServiceConnection connection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			var binder = (LocalBinder) service;
			connectionService = binder.getService();
			bound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			bound = false;
		}
	};
}