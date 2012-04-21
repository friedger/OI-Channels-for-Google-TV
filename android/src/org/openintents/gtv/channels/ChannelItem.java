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

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * Holds one item returned from the PackageManager.
 */
public class ChannelItem implements Parcelable {
	private long mId;
	private String mTitle;
	private String mPackageName;
	private String mActivityName;

	private Context mContext;
	private int mIconResource;

	public ChannelItem(Context context, long id, String title,
			String packageName, String activityName, int iconResource, Handler handler) {
		mTitle = title;
		mId = id;
		mContext = context;
		mPackageName = packageName;
		mActivityName = activityName;
		mIconResource = iconResource;
	}

	public long getId() {
		return mId;
	}

	public String getTitle() {
		return mTitle;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public String getActivityName() {
		return mActivityName;
	}

	public int getIconResource() {
		return mIconResource;
	}
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeLong(mId);
		parcel.writeString(mTitle);
		parcel.writeString(mPackageName);
	}

	public void clear() {
	}

}
