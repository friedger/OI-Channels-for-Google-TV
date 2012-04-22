package org.openintents.gtv.channels;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class OIChannelsActivity extends Activity {
    private GridView gridView;
	private ProgressBar progressBar;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channel_app_grid);
        
        gridView = (GridView) findViewById(R.id.gridview);
        final ChannelAdapter channelAdapter = new ChannelAdapter(this);
        gridView.setAdapter(channelAdapter);
        progressBar = (ProgressBar) findViewById(R.id.a_progressbar);
        gridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                // Create an intent to show the channel
            	ChannelItem item = (ChannelItem) parent.getAdapter().getItem(position);
                final Intent i = new Intent();
                i.setClassName(item.getPackageName(), item.getActivityName());
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        });
        gridView.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
            public void onChildViewAdded(View parent, View child) {
                progressBar.setVisibility(View.INVISIBLE);
                ((ViewGroup) parent).getChildAt(0).setSelected(true);
            }

            public void onChildViewRemoved(View parent, View child) {
            }
        });
        View emptyView = getLayoutInflater().inflate(R.layout.channels_empty, null, false);
        gridView.setEmptyView(emptyView);
        addContentView(emptyView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        //ChannelsLeftNavService.getLeftNavBar(this);
        gridView.requestFocus();
        
        ChannelAppManager.getInstance(this).load();
    }
}