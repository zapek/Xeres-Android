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

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;

public final class BitmapUtils
{
	private BitmapUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Bitmap getScaledBitmap(Bitmap b, int reqWidth, int reqHeight)
	{
		int bWidth = b.getWidth();
		int bHeight = b.getHeight();

		int nWidth = bWidth;
		int nHeight = bHeight;

		if(nWidth > reqWidth)
		{
			int ratio = bWidth / reqWidth;
			if(ratio > 0)
			{
				nWidth = reqWidth;
				nHeight = bHeight / ratio;
			}
		}

		if(nHeight > reqHeight)
		{
			int ratio = bHeight / reqHeight;
			if(ratio > 0)
			{
				nHeight = reqHeight;
				nWidth = bWidth / ratio;
			}
		}
		return Bitmap.createScaledBitmap(b, nWidth, nHeight, true);
	}

	public static Bitmap getScaledBitmap(Bitmap bitmap, int maximumSize)
	{
		double nWidth = bitmap.getWidth();
		double nHeight = bitmap.getHeight();

		var actualSize = nWidth * nHeight;

		if (actualSize > maximumSize)
		{
			var ratio = Math.sqrt(maximumSize / actualSize);
			nWidth *= ratio;
			nHeight *= ratio;
		}
		return Bitmap.createScaledBitmap(bitmap, (int) nWidth, (int) nHeight, true);
	}

	public static Bitmap rotateBitmapIfNeeded(Bitmap in, int degreesToRotate)
	{
		if (degreesToRotate == 0)
		{
			return in;
		}
		var rotateMatrix = new Matrix();
		rotateMatrix.postRotate(degreesToRotate);
		return Bitmap.createBitmap(in, 0, 0, in.getWidth(), in.getHeight(), rotateMatrix, false);
	}

	public static int getImageOrientation(ContentResolver contentResolver, Uri imageUri)
	{
		int orientation = 0;

		String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION};

		try (var cursor = contentResolver.query(imageUri, columns, null, null, null))
		{
			if(cursor !=null)
			{
				cursor.moveToFirst();
				int orientationColumnIndex = cursor.getColumnIndex(columns[1]);
				orientation = cursor.getInt(orientationColumnIndex);
			}
		}
		return orientation;
	}
}
