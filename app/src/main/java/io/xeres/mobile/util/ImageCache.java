/*
 * Copyright (c) 2013-2025 by David Gerber - https://zapek.com
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

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Class used to cache frequently used bitmaps
 *
 * @author David Gerber
 */
public class ImageCache extends LruCache<String, Bitmap> {

	private static int maxSize;
	private final Set<SoftReference<Bitmap>> reusableBitmaps = Collections.synchronizedSet(new HashSet<>());

	public static int calculateSize(Context context) {
		int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		int cacheSize = 1024 * 1024;

		if (memClass <= 16) /* G1 (192MB) */
		{
			cacheSize *= memClass / 8;
		}
		else if (memClass <= 24) /* Droid (288MB?) */
		{
			cacheSize *= memClass / 6;
		}
		else if (memClass <= 32) /* Nexus One (512MB) */
		{
			cacheSize *= memClass / 4;
		}
		else if (memClass <= 48) /* Xoom (1GB) */
		{
			cacheSize *= memClass / 2;
		}
		else
		{
			cacheSize *= memClass / 2; /* 64MB+, Galaxy Nexus (96 in 4.2, 64 before, 64 in Nexus 7) */
		}
		maxSize = cacheSize;
		Log.d("ImageCache", "memoryClass: " + memClass + ", using cache of " + (cacheSize / 1024 / 1024) + " MB");
		return cacheSize;
	}

	public ImageCache(int maxSize) {
		super(maxSize);
	}

	@Override
	protected int sizeOf(String url, Bitmap bitmap) {
		/* measure cache in bytes */
		return bitmap.getByteCount();
	}

	public void evictPartial() {
		trimToSize(maxSize / 2);
	}

	public void addBitmap(String url, Bitmap bitmap) {
		if (url.startsWith("data:"))
		{
			return;
		}
		put(url, bitmap);
	}

	public Bitmap getBitmap(String url) {
		return get(url);
	}

	@Override
	protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
		reusableBitmaps.add(new SoftReference<>(oldValue));
	}

	public Bitmap getReusableBitmap(BitmapFactory.Options options) {
		Bitmap bitmap = null;

		if (!reusableBitmaps.isEmpty())
		{
			synchronized (reusableBitmaps)
			{
				Iterator<SoftReference<Bitmap>> iterator = reusableBitmaps.iterator();
				Bitmap item;

				while (iterator.hasNext())
				{
					item = iterator.next().get();

					if (item != null && item.isMutable())
					{
						if (canUseForInBitmap(item, options))
						{
							bitmap = item;

							iterator.remove(); /* remove from reusable set */
							break;
						}
					}
					else
					{
						iterator.remove(); /* remove from the set if the reference has been cleared */
					}
				}
			}
		}
		return bitmap;
	}

	private boolean canUseForInBitmap(Bitmap candidate, BitmapFactory.Options targetOptions) {
        int width = targetOptions.outWidth / targetOptions.inSampleSize;
        int height = targetOptions.outHeight / targetOptions.inSampleSize;
        int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
        return byteCount <= candidate.getAllocationByteCount();
    }

	private static int getBytesPerPixel(Bitmap.Config config) {
		if (config == Bitmap.Config.ARGB_8888)
		{
			return 4;
		}
		else if (config == Bitmap.Config.RGB_565)
		{
			return 2;
		}
		else if (config == Bitmap.Config.ARGB_4444)
		{
			return 2;
		}
		else if (config == Bitmap.Config.ALPHA_8)
		{
			return 1;
		}
		return 1;
	}
}
