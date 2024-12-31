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

package io.xeres.mobile.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import kotlin.text.Charsets;

class LiberalSslContext
{
	private static final String TAG = "LiberalSslContext";

	private final SSLContext sslContext;

	private final Context context;

	@SuppressLint("CustomX509TrustManager")
	private final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{
			new X509TrustManager()
			{
				@SuppressLint("TrustAllX509TrustManager")
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType)
				{
					// Not used
				}

				@SuppressLint("TrustAllX509TrustManager")
				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
				{
					var prefs = PreferenceManager.getDefaultSharedPreferences(context);

					if (chain == null || chain.length != 1)
					{
						Log.d(TAG, "Wrong or missing certificate");
						throw new CertificateException("Wrong or missing certificate");
					}
					var certificate = chain[0];

					if (!TextUtils.equals(certificate.getIssuerDN().getName(), "CN=Xeres") && !TextUtils.equals(certificate.getSubjectDN().getName(), "CN=Xeres"))
					{
						throw new CertificateException("Wrong DN");
					}

					if (prefs.getBoolean("pinned", false))
					{
						try
						{
							var publicKey = KeyFactory.getInstance(prefs.getString("public_key_format", ""))
									.generatePublic(new X509EncodedKeySpec(prefs.getString("public_key_array", "").getBytes(Charsets.ISO_8859_1)));
							certificate.verify(publicKey);
						}
						catch (SignatureException e)
						{
							throw new CertificateException("SSL certificate doesn't match.");
						}
						catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e)
						{
							throw new CertificateException(e);
						}
					}
					else
					{
						var publicKey = certificate.getPublicKey();
						var editor = prefs.edit();
						editor.putString("public_key_format", publicKey.getAlgorithm());
						editor.putString("public_key_array", new String(publicKey.getEncoded(), Charsets.ISO_8859_1));
						editor.putBoolean("pinned", true);
						editor.apply();
					}
				}

				@Override
				public X509Certificate[] getAcceptedIssuers()
				{
					return new X509Certificate[0];
				}
			}
	};

	public LiberalSslContext(Context context)
	{
		this.context = context;
		try
		{
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, TRUST_ALL_CERTS, new SecureRandom());
		}
		catch (KeyManagementException | NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
	}

	public SSLSocketFactory getSocketFactory()
	{
		return sslContext.getSocketFactory();
	}

	public X509TrustManager getLiberalCert()
	{
		return (X509TrustManager) TRUST_ALL_CERTS[0];
	}
}
