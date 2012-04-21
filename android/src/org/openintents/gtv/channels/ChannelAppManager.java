/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.gtv.channels;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.os.Handler;
import android.util.Log;

/**
 * This class is responsible for analyzing installed apps. All of the work is
 * done on a separate thread, and progress is reported back through the
 * DataSetObserver set in {@link #addObserver(DataSetObserver). State is held in
 * memory by in memory maintained by a single instance of the ChannelAppManager
 * class. *
 */
public class ChannelAppManager {
	private static final String TAG = "OIChannels";

	/**
	 * Used to post results back to the UI thread
	 */
	private static Handler mHandler = new Handler();

	/**
	 * Holds the single instance of a ImageManager that is shared by the
	 * process.
	 */
	private static ChannelAppManager sInstance;

	/**
	 * Holds the images and related data that have been downloaded
	 */
	private final ArrayList<ChannelItem> mChannelItems = new ArrayList<ChannelItem>();

	private int mCurrentPosition;

	/**
	 * Observers interested in changes to the current search results
	 */
	private final ArrayList<WeakReference<DataSetObserver>> mObservers = new ArrayList<WeakReference<DataSetObserver>>();

	/**
	 * True if we are in the process of loading
	 */
	private static boolean mLoading;

	private static Context mContext;

	public static ChannelAppManager getInstance(Context c) {
		if (sInstance == null) {
			sInstance = new ChannelAppManager(c);
		}
		return sInstance;
	}

	private ChannelAppManager(Context c) {
		mContext = c;
	}

	/**
	 * @return True if we are still loading content
	 */
	public boolean isLoading() {
		return mLoading;
	}

	/**
	 * Clear all downloaded content
	 */
	public void clear() {
		File cacheDir = mContext.getCacheDir();
		for (File file : cacheDir.listFiles()) {
			file.delete();
		}
		cacheDir.delete();
		for (ChannelItem item : mChannelItems) {
			item.clear();
			item = null;
		}
		mChannelItems.clear();
		notifyInvalidateObservers();
	}

	public ChannelItem getNext() {
		if (mCurrentPosition + 1 <= mChannelItems.size() - 1) {
			mCurrentPosition = mCurrentPosition + 1;
			return mChannelItems.get(mCurrentPosition);
		}
		return null;
	}

	public ChannelItem getPrevious() {
		if (mCurrentPosition - 1 >= 0) {
			mCurrentPosition = mCurrentPosition - 1;
			return mChannelItems.get(mCurrentPosition);
		}
		return null;
	}

	/**
	 * Add an item to and notify observers of the change.
	 * 
	 * @param item
	 *            The item to add
	 */
	private void add(ChannelItem item) {
		mChannelItems.add(item);
		notifyObservers();
	}

	/**
	 * @return The number of items displayed so far
	 */
	public int size() {
		return mChannelItems.size();
	}

	/**
	 * Gets the item at the specified position
	 */
	public ChannelItem get(int position) {
		mCurrentPosition = position;
		if (mChannelItems.size() > position) {
			return mChannelItems.get(position);
		}
		return null;
	}

	/**
	 * Adds an observer to be notified when the set of items held by this
	 * ImageManager changes.
	 */
	public void addObserver(DataSetObserver observer) {
		final WeakReference<DataSetObserver> obs = new WeakReference<DataSetObserver>(
				observer);
		mObservers.add(obs);
	}

	Thread mPrevThread = null;

	/**
	 * Load a new set of search results for the specified area.
	 * 
	 * @throws JSONException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public void load() {

		clear();
		mLoading = true;

		mPrevThread = new ChannelAppsLoader();
		mPrevThread.start();

	}

	/**
	 * Called when something changes in our data set. Cleans up any weak
	 * references that are no longer valid along the way.
	 */
	private void notifyObservers() {
		final ArrayList<WeakReference<DataSetObserver>> observers = mObservers;
		final int count = observers.size();
		for (int i = count - 1; i >= 0; i--) {
			final WeakReference<DataSetObserver> weak = observers.get(i);
			final DataSetObserver obs = weak.get();
			if (obs != null) {
				obs.onChanged();
			} else {
				observers.remove(i);
			}
		}
	}

	/**
	 * Called when something changes in our data set. Cleans up any weak
	 * references that are no longer valid along the way.
	 */
	private void notifyInvalidateObservers() {
		final ArrayList<WeakReference<DataSetObserver>> observers = mObservers;
		final int count = observers.size();
		for (int i = count - 1; i >= 0; i--) {
			final WeakReference<DataSetObserver> weak = observers.get(i);
			final DataSetObserver obs = weak.get();
			if (obs != null) {
				obs.onInvalidated();
			} else {
				observers.remove(i);
			}
		}
	}

	/**
	 * This thread does the actual work of fetching and parsing Panoramio JSON
	 * response data.
	 */
	private static class ChannelAppsLoader extends Thread {

		public ChannelAppsLoader() {

		}

		@Override
		public void run() {
			PackageManager pm = ChannelAppManager.mContext
			.getPackageManager();
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory("org.openintents.category.LAUNCHER_CHANNEL");
			List<ResolveInfo> packageList = pm.queryIntentActivities(intent,
							PackageManager.GET_META_DATA);
			if (packageList != null) {
				for (int i = 0; i < packageList.size(); i++) {
					final boolean done = (i == packageList.size() - 1);
					ResolveInfo ri = packageList.get(i);
					final String title = ri.activityInfo.loadLabel(pm).toString();
					final String packageName = ri.activityInfo.applicationInfo.packageName;
					final String activityName = ri.activityInfo.name;
					final int icon = ri.getIconResource(); 
					Log.v(TAG, "" + packageName + " " + title);
					final ChannelItem item = new ChannelItem(mContext, i, title, packageName, activityName, icon, mHandler);
					mHandler.post(new Runnable() {
						public void run() {
							sInstance.mLoading = !done;
							sInstance.add(item);
						}
					});
				}
			}
		}
	}
}
