package de.droidcon.android.channel;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class DroidconChannel extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://youtube.com/droidcon"));		
		startActivity(i);
		finish();
	}
}
