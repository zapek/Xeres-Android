/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.mobile.ui.qr;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Arrays;
import java.util.stream.Collectors;

import io.xeres.mobile.databinding.ActivityScanBinding;
import io.xeres.mobile.service.ConnectionService;
import io.xeres.mobile.service.LocalBinder;
import io.xeres.mobile.service.json.Profile;
import io.xeres.mobile.service.json.RsIdRequest;
import io.xeres.mobile.service.json.Trust;
import io.xeres.mobile.util.Id;

public class ScanActivity extends AppCompatActivity
{
	private ActivityScanBinding binding;
	private ConnectionService connectionService;
	private boolean bound;
	private RsIdRequest currentRsIdRequest;
	private String pendingRsId;

	private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
			result -> {
				if (result.getContents() == null)
				{
					Toast.makeText(ScanActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
					finish();
				}
				else
				{
					checkRsId(result.getContents());
				}
			});

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		binding = ActivityScanBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		setSupportActionBar(binding.toolbar);

		ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			binding.appbar.setPadding(0, systemBars.top, 0, 0);
			return insets;
		});

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle("Scan QR Code");
		}

		setupTrustSpinner();

		binding.buttonAdd.setOnClickListener(v -> createProfile());

		var options = new ScanOptions()
				.setOrientationLocked(false)
				.setBeepEnabled(false)
				.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
				.setCaptureActivity(PortraitCaptureActivity.class);
		barcodeLauncher.launch(options);
	}

	private void setupTrustSpinner()
	{
		var values = Arrays.stream(Trust.values())
				.filter(t -> t != Trust.ULTIMATE)
				.collect(Collectors.toList());
		var adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.spinnerTrust.setAdapter(adapter);
		binding.spinnerTrust.setSelection(0);
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
		if (bound)
		{
			unbindService(connection);
			bound = false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private final ServiceConnection connection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			var binder = (LocalBinder) service;
			connectionService = binder.getService();
			bound = true;
			if (pendingRsId != null)
			{
				checkRsId(pendingRsId);
				pendingRsId = null;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			bound = false;
		}
	};

	private void checkRsId(String rsId)
	{
		if (connectionService != null)
		{
			currentRsIdRequest = new RsIdRequest();
			currentRsIdRequest.setRsId(rsId);

			binding.progressBar.setVisibility(View.VISIBLE);
			connectionService.checkRsId(currentRsIdRequest, this::displayProfile);
		}
		else
		{
			pendingRsId = rsId;
		}
	}

	private void displayProfile(Profile profile)
	{
		binding.progressBar.setVisibility(View.GONE);
		binding.textName.setText(profile.getName());

		var pgpIdentifier = profile.getPgpIdentifier();
		if (pgpIdentifier != null)
		{
			var keyId = Long.parseLong(pgpIdentifier);
			binding.textPgpIdentifier.setText(Long.toHexString(keyId).toUpperCase());
		}

		binding.textFingerprint.setText(formatFingerprint(profile.getPgpFingerprint()));

		var locationText = "none";
		var locations = profile.getLocations();
		if (locations != null && !locations.isEmpty())
		{
			var identifier = locations.get(0).getLocationIdentifier();
			if (identifier != null)
			{
				locationText = Id.toString(identifier).toLowerCase();
			}
		}
		binding.textLocation.setText(locationText);

		binding.labelTrust.setVisibility(View.VISIBLE);
		binding.spinnerTrust.setVisibility(View.VISIBLE);
		binding.buttonAdd.setVisibility(View.VISIBLE);
	}

	private String formatFingerprint(byte[] fingerprint)
	{
		if (fingerprint == null) return "";
		var hex = Id.toString(fingerprint).toUpperCase();
		var sb = new StringBuilder();
		for (int i = 0; i < hex.length(); i++)
		{
			if (i > 0 && i % 4 == 0)
			{
				if (i == 20)
				{
					sb.append("  ");
				}
				else
				{
					sb.append(" ");
				}
			}
			sb.append(hex.charAt(i));
		}
		return sb.toString();
	}

	private void createProfile()
	{
		var trust = (Trust) binding.spinnerTrust.getSelectedItem();
		binding.progressBar.setVisibility(View.VISIBLE);
		connectionService.createProfile(currentRsIdRequest, trust, response -> {
			binding.progressBar.setVisibility(View.GONE);
			finish();
		});
	}
}
