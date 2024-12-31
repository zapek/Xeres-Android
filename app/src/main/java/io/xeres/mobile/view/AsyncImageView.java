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

package io.xeres.mobile.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;

import io.xeres.mobile.R;

/**
 * Special image class that loads its content asynchronously from an Uri
 *
 * @author David Gerber
 */
public class AsyncImageView extends androidx.appcompat.widget.AppCompatImageView
{
	private static final String TAG = "AsyncImageView";

	/**
	 * Optional interface to change the Url once the size of the view is known.
	 */
	public interface OnImageSizeAvailableListener
	{
		/**
		 * Allows to change the Url once the size of the view is known. This is
		 * useful when the server has argument support for image resizing.
		 *
		 * @param url    the URL of the image
		 * @param width  the width of the view in pixels
		 * @param height the height of the view in pixels
		 * @return the new URL that AsyncImageView will fetch
		 */
		String onImageSizeAvailable(String url, int width, int height);
	}

	/**
	 * Optional interface to get the result of an image loading operation.
	 */
	public interface OnImageLoadedListener
	{
		/**
		 * Returns the result of an image loading operation.
		 *
		 * @param result true if the image loaded properly
		 * @return true if you handled the event, false if AsyncImageView can perform its own handling (eg. showing error image)
		 */
		boolean onImageLoaded(boolean result);
	}

	/**
	 * Optional interface to allow to use a fast memory cache. Implement it in
	 * your Application object.
	 */
	public interface ImageCache
	{
		/**
		 * Gets a cache bitmap for a given URL
		 *
		 * @param url where the image is
		 * @return a bitmap if it's in the cache, null otherwise
		 */
		Bitmap getBitmap(String url);

		/**
		 * Puts a bitmap in the cache
		 *
		 * @param url    where the image was fetched from
		 * @param bitmap the resulting bitmap
		 */
		void addBitmap(String url, Bitmap bitmap);

		/**
		 * Gets a bitmap that we can use to write to
		 *
		 * @param options the options we will use to decode
		 * @return a mutable bitmap
		 */
		Bitmap getReusableBitmap(BitmapFactory.Options options);

		/**
		 * Called when we're running out of memory (typically when decoding a bitmap).
		 * Try to free up as much resources as possible.
		 */
		void evictAll();
	}

	/**
	 * Optional interface to allow to use a different executor than the default one. For example
	 * you can do Executors.newFixedThreadPool(4); to have a 4 fixed threads useful for decoding
	 * in a listview. Implement it in your Application object.
	 */
	public interface ImageExecutor
	{
		/**
		 * Gets the image executor.
		 *
		 * @return an ExecutorService
		 */
		ExecutorService getImageExecutor();
	}

	/**
	 * Optional interface to allow to use a different input than the built in HttpURLConnection.
	 */
	public interface ImageInput
	{
		ImageConnection getImageConnection();
	}

	public interface ImageConnection
	{
		void connect(String url) throws IOException;

		void disconnect();

		InputStream getInputStream() throws IOException;
	}

	private static final int DRAWABLE_PLACEHOLDER = 1;
	private static final int DRAWABLE_BITMAP = 2;

	private OnImageSizeAvailableListener onImageSizeAvailableListener;
	private OnImageLoadedListener onImageLoadedListener;
	private boolean canLoad;
	private boolean hasSize;
	private boolean loaded;
	private boolean done;
	private String imageUrl;
	private final int transitionTime;
	private final AsyncTransitionDrawable transitionDrawable;
	private final Drawable errorDrawable;
	private final Drawable defaultDrawable; /* used as a placeholder */
	private ImageCache imageCache;
	private ExecutorService executor;
	private ImageInput imageInput;

	public AsyncImageView(Context context)
	{
		this(context, null, 0);
	}

	public AsyncImageView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public AsyncImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		boolean useMemoryCache;
		try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AsyncImageView, defStyle, 0))
		{
			useMemoryCache = a.getBoolean(R.styleable.AsyncImageView_useMemoryCache, true);
			transitionTime = (a.getBoolean(R.styleable.AsyncImageView_fading, true)) ? 300 : 0;
			errorDrawable = a.getDrawable(R.styleable.AsyncImageView_errorDrawable);
			defaultDrawable = a.getDrawable(R.styleable.AsyncImageView_defaultDrawable);
		}

		if (getScaleType() == ScaleType.FIT_CENTER) /* this is the default */
		{
			/* XXX: we don't really support other modes */
			setAdjustViewBounds(true);
		}

		Context applicationContext = context.getApplicationContext();

		if (useMemoryCache)
		{
			if (applicationContext instanceof ImageCache)
			{
				imageCache = (ImageCache) applicationContext;
			}
		}

		if (applicationContext instanceof ImageExecutor)
		{
			executor = ((ImageExecutor) applicationContext).getImageExecutor();
		}

		if (applicationContext instanceof ImageInput)
		{
			imageInput = ((ImageInput) applicationContext);
		}

		Drawable placeholderDrawable;

		if (defaultDrawable != null)
		{
			clipDrawable(defaultDrawable);
			placeholderDrawable = defaultDrawable;
		}
		else
		{
			placeholderDrawable = new ColorDrawable(getResources().getColor(android.R.color.transparent));
		}

		if (errorDrawable != null)
		{
			clipDrawable(errorDrawable);
		}

		Drawable[] drawables = {
				placeholderDrawable,
				new ColorDrawable(getResources().getColor(android.R.color.transparent))
		};

		transitionDrawable = new AsyncTransitionDrawable(drawables);
		transitionDrawable.setId(0, DRAWABLE_PLACEHOLDER);
		transitionDrawable.setId(1, DRAWABLE_BITMAP);
		transitionDrawable.setCrossFadeEnabled(true);
		setImageDrawable(transitionDrawable);
	}

	public void setImageInput(ImageInput imageInput)
	{
		this.imageInput = imageInput;
	}

	private void clipDrawable(Drawable drawable)
	{
		if (drawable instanceof BitmapDrawable)
		{
			((BitmapDrawable) drawable).setGravity(Gravity.DISPLAY_CLIP_HORIZONTAL | Gravity.DISPLAY_CLIP_VERTICAL | Gravity.CENTER);
		}
		else
		{
			if (drawable.getIntrinsicWidth() != -1 || drawable.getIntrinsicHeight() != -1)
			{
				throw new IllegalArgumentException("specified drawables cannot have a size");
			}
		}
	}

	public void setImageUrl(String imageUrl)
	{
		if (imageUrl != null)
		{
			if (done || loaded)
			{
				if (this.imageUrl != null)
				{
					if (this.imageUrl.equals(imageUrl))
					{
						/*
						 * Don't load the same path if it was already loaded
						 * properly.
						 */
						return;
					}
				}
			}
			transitionDrawable.resetTransition();
			this.imageUrl = imageUrl;
			loaded = false;
			loadImage();
		}
		else
		{
			setDrawable(null, true);
			this.imageUrl = null;
		}
	}

	public void setOnImageSizeAvailableListener(OnImageSizeAvailableListener listener)
	{
		onImageSizeAvailableListener = listener;
	}

	public void setOnImageLoadedListener(OnImageLoadedListener listener)
	{
		onImageLoadedListener = listener;
	}

	private void loadImage()
	{
		if (imageUrl != null && canLoad && !loaded)
		{
			if (onImageSizeAvailableListener != null)
			{
				imageUrl = onImageSizeAvailableListener.onImageSizeAvailable(imageUrl, getWidth(), getHeight());
			}
			if (imageUrl != null)
			{
				loaded = true;
				LoaderTask.loadImage(this, imageUrl, imageInput, imageCache, executor);
			}
			/* XXX: show an error? or default? */
		}
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		if (hasSize) /* see onSizeChanged() */
		{
			canLoad = true;
			loadImage();
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		/*
		 * If we need the ImageView's size to do some computation,
		 * in that case to calculate the resulting image size,
		 * we need to wait until we get an onMeasure()->onSizeChanged() call.
		 * onAttachToWindow() is done before. We only need to do this once
		 * as not every parent calls onSizeChanged() every time a view is
		 * attached.
		 */
		hasSize = true;
		canLoad = true;
		loadImage();
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		canLoad = false;
	}

	private void setDrawable(Drawable drawable, boolean immediate)
	{
		transitionDrawable.setLoaderTask(null);

		if (drawable != null)
		{
			transitionDrawable.setDrawableByLayerId(DRAWABLE_BITMAP, drawable);
			if (immediate)
			{
				transitionDrawable.startTransition(0);
			}
			else
			{
				transitionDrawable.startTransition(transitionTime);
			}
		}
		else
		{
			transitionDrawable.resetTransition();
		}
		done = true;
	}

	private void setBitmap(Bitmap bitmap, boolean immediate)
	{
		BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
		/*
		 * Default to clipping so that the drawable's aspect ratio is
		 * preserved.
		 */
		if (getScaleType() == ScaleType.FIT_CENTER)
		{
			bitmapDrawable.setGravity(Gravity.DISPLAY_CLIP_HORIZONTAL | Gravity.DISPLAY_CLIP_VERTICAL | Gravity.CENTER);
		}
		setDrawable(bitmapDrawable, immediate);
	}

	private void setOnImageLoaded(boolean result)
	{
		if (onImageLoadedListener != null)
		{
			if (!onImageLoadedListener.onImageLoaded(result))
			{
				if (errorDrawable != null && !result)
				{
					setDrawable(errorDrawable, false);
				}
			}
		}
		else
		{
			if (errorDrawable != null && !result)
			{
				setDrawable(errorDrawable, false);
			}
		}
	}

	private static class LoaderTask extends AsyncTask<String, Void, Bitmap>
	{
		private final Context context;
		private final ImageInput input;
		private final ImageCache cache;
		private final WeakReference<AsyncImageView> imageViewReference;
		private String imageUrl;
		private final int imageWidth;
		private final int imageHeight;

		static public boolean loadImage(AsyncImageView imageView, String imageUrl, ImageInput input, ImageCache cache, ExecutorService executor)
		{
			if (cache != null)
			{
				Bitmap bitmap = cache.getBitmap(imageUrl);
				if (bitmap != null)
				{
					Log.d(TAG, "Got image from cache!");
					imageView.setBitmap(bitmap, true);
					imageView.setOnImageLoaded(true);
					return true;
				}
			}
			if (canDoWork(imageUrl, imageView))
			{
				final LoaderTask task = new LoaderTask(imageView, input, cache);
				final AsyncTransitionDrawable asyncDrawable = (AsyncTransitionDrawable) imageView.getDrawable();
				asyncDrawable.setLoaderTask(task);

				if (executor != null)
				{
					task.executeOnExecutor(executor, imageUrl);
				}
				else
				{
					task.execute(imageUrl);
				}
				return true;
			}
			return false;
		}

		static private boolean canDoWork(String imageUrl, AsyncImageView imageView)
		{
			final LoaderTask task = getLoaderTask(imageView);

			if (task != null)
			{
				final String taskImageUrl = task.imageUrl;
				if (imageUrl.equals(taskImageUrl))
				{
					/* same work already in progress */
					return false;
				}
				else
				{
					/*
					 * Cancel previous task
					 * Note: since Android 5.0, it
					 * seems cancelling an active thread makes
					 * OkHttp (used internally) abort read() without
					 * returning an error so skia tries to decode
					 * broken data and one gets incomplete images.
					 * Not cancelling the thread is ok too.
					 */
					task.cancel(false);
				}
			}
			/*
			 * No task associated with the ImageView, or an existing task was
			 * cancelled
			 */
			return true;
		}

		static private LoaderTask getLoaderTask(AsyncImageView imageView)
		{
			if (imageView != null)
			{
				final AsyncTransitionDrawable asyncDrawable = (AsyncTransitionDrawable) imageView.getDrawable();

				return (LoaderTask) (asyncDrawable.getLoaderTask());
			}
			return null;
		}

		private LoaderTask(AsyncImageView imageView, ImageInput input, ImageCache cache)
		{
			/*
			 * We need a long living context so that other resources can be
			 * garbage collected if they're closed and we didn't finish yet.
			 */
			context = imageView.getContext().getApplicationContext();
			this.input = input;
			this.cache = cache;
			imageViewReference = new WeakReference<>(imageView);
			imageWidth = imageView.getMeasuredWidth();
			imageHeight = imageView.getMeasuredHeight();
			if (imageWidth == 0 || imageHeight == 0)
			{
				throw new IllegalArgumentException("width: " + imageWidth + ", height: " + imageHeight);
			}
		}

		@Override
		protected Bitmap doInBackground(String... params)
		{
			imageUrl = params[0];
			Bitmap bitmap = null;

			if (!isCancelled())
			{
				try
				{
					ImageConnection connection = null;
					InputStream in = null;
					HttpURLConnection urlConnection = null;

					if (input != null)
					{
						connection = input.getImageConnection();
						connection.connect(imageUrl);
						in = connection.getInputStream();
					}
					else
					{
						urlConnection = (HttpURLConnection) new URL(imageUrl).openConnection();
						if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
						{
							in = urlConnection.getInputStream();
						}
					}

					if (in != null)
					{
						ImageView imageView = imageViewReference.get();
						int retry = 2;

						while (!isCancelled() && retry > 0)
						{
							try
							{
								if (imageView != null)
								{
									BitmapFactory.Options options = new BitmapFactory.Options();

									if (cache != null)
									{
										options.inMutable = true;
										options.inSampleSize = 1;
										options.outWidth = imageWidth;
										options.outHeight = imageHeight;

										Bitmap inBitmap = cache.getReusableBitmap(options);
										if (inBitmap != null)
										{
											options.inBitmap = inBitmap;
										}
									}
									try
									{
										bitmap = BitmapFactory.decodeStream(in, null, options);
									}
									catch (IllegalArgumentException e)
									{
										/* this happens when the input is wrong and we use an inBitmap */
									}
								}

								if (bitmap != null)
								{
									/*
									 * Scale the bitmap up or down so it fits
									 * in the view with the proper aspect
									 * ratio.
									 */
									int targetWidth = imageWidth;
									int targetHeight = imageHeight;
									int srcWidth = bitmap.getWidth();
									int srcHeight = bitmap.getHeight();

									float aspectRatio = (float) srcWidth / srcHeight;

									int width = srcWidth;
									int height = srcHeight;

									/*
									 * Get the biggest corner.
									 */
									if (width > height)
									{
										width = targetWidth;
										height = (int) ((float) targetWidth / aspectRatio);

										if (height > targetHeight)
										{
											height = targetHeight;
											width = (int) ((float) height * aspectRatio);
										}
									}
									else
									{
										height = targetHeight;
										width = (int) ((float) targetHeight * aspectRatio);

										if (width > targetWidth)
										{
											width = targetWidth;
											height = (int) ((float) width / aspectRatio);
										}
									}
									bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

									if (cache != null)
									{
										cache.addBitmap(imageUrl, bitmap);
									}
								}
								else
								{
									Log.d(TAG, "error decoding bitmap");
								}
								retry = 0;
							}
							catch (OutOfMemoryError e)
							{
								Log.d(TAG, "out of memory, trying to evict cache");
								if (cache != null)
								{
									cache.evictAll();
									retry--;
								}
								else
								{
									retry = 0;
								}
							}
						}
					}
					if (input != null)
					{
						connection.disconnect();
					}
					else
					{
						urlConnection.disconnect();
					}
				}
				catch (IOException e)
				{
					Log.d(TAG, imageUrl + " IOException: " + e);
				}
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			if (!isCancelled())
			{
				final AsyncImageView imageView = imageViewReference.get();
				final LoaderTask task = getLoaderTask(imageView);
				if (this == task)
				{
					if (bitmap != null)
					{
						imageView.setBitmap(bitmap, false);
						imageView.setOnImageLoaded(true);
					}
					else
					{
						imageView.setOnImageLoaded(false);
					}
				}
			}
		}
	}

	static class AsyncTransitionDrawable extends TransitionDrawable
	{

		private WeakReference<AsyncTask> taskReference;

		public AsyncTransitionDrawable(Drawable[] layers)
		{
			super(layers);
		}

		public void setLoaderTask(AsyncTask task)
		{
			taskReference = new WeakReference<>(task);
		}

		public AsyncTask getLoaderTask()
		{
			if (taskReference != null)
			{
				return taskReference.get();
			}
			return null;
		}

		/*
		 * That way we make sure no drawable will set a constraint.
		 * ie. placeholder setting the size before the final image
		 * is loaded.
		 */
		@Override
		public int getIntrinsicWidth()
		{
			return -1;
		}

		@Override
		public int getIntrinsicHeight()
		{
			return -1;
		}
	}
}
