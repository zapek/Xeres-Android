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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.xeres.mobile.util.ImageCache;
import io.xeres.mobile.view.AsyncImageView;

public class Application extends android.app.Application implements AsyncImageView.ImageExecutor, AsyncImageView.ImageCache
{
	private static final String TAG = "Application";

	private ExecutorService imageLoaderExecutor;
	private ImageCache imageCache;

	@Override
	public void onCreate()
	{
		super.onCreate();

		imageCache = new ImageCache(ImageCache.calculateSize(this));
		imageLoaderExecutor = Executors.newFixedThreadPool(4);
	}

	@Override
	public void onTrimMemory(int level)
	{
		super.onTrimMemory(level);

		if (level >= TRIM_MEMORY_COMPLETE)
		{
			//HTTPCache.flush();
		}
		else if (level >= TRIM_MEMORY_MODERATE)
		{
			/*
			 * Nearing middle list of cached background apps.
			 * Remove all the cache size.
			 */
			if (imageCache != null)
			{
				Log.d(TAG, "evicting ImageCache (all)");
				imageCache.evictAll();
			}
		}
		else if (level >= TRIM_MEMORY_BACKGROUND)
		{
			/*
			 * Entering list of cached background apps. Trim
			 * half the cache size.
			 */
			if (imageCache != null)
			{
				Log.d(TAG, "evicting ImageCache (half)");
				imageCache.evictPartial();
			}
		}
	}

	@Override
	public ExecutorService getImageExecutor()
	{
		return imageLoaderExecutor;
	}

	@Override
	public Bitmap getBitmap(String url)
	{
		return imageCache.getBitmap(url);
	}

	@Override
	public void addBitmap(String url, Bitmap bitmap)
	{
		imageCache.addBitmap(url, bitmap);
	}

	@Override
	public Bitmap getReusableBitmap(BitmapFactory.Options options)
	{
		return imageCache.getReusableBitmap(options);
	}

	@Override
	public void evictAll()
	{
		imageCache.evictAll();
	}
	// XXX: HTTP not sure... probably not...
}
