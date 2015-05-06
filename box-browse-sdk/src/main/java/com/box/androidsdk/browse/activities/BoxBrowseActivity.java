package com.box.androidsdk.browse.activities;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.fragments.BoxBrowseFolderFragment;
import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class BoxBrowseActivity extends BoxThreadPoolExecutorActivity implements BoxBrowseFragment.OnFragmentInteractionListener {

    /**
     * Extra intent parameter that adds a user id to the intent
     */
    public static final String EXTRA_USER_ID = "extraUserId";

    public static final String TAG = BoxBrowseActivity.class.getName();

    private static final ConcurrentLinkedQueue<BoxResponse> RESPONSE_QUEUE = new ConcurrentLinkedQueue<BoxResponse>();
    private static ThreadPoolExecutor mApiExecutor;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.box_browsesdk_activity_browse);
        initToolbar();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.box_browsesdk_fragment_container, BoxBrowseFolderFragment.newInstance(mItem.getId(), mSession.getUserId()))
                    .commit();
        }
    }

    @Override
    public ThreadPoolExecutor getApiExecutor(Application application) {
        if (mApiExecutor == null) {
            mApiExecutor = BoxThreadPoolExecutorActivity.createTaskMessagingExecutor(application, getResponseQueue());
        }
        return mApiExecutor;
    }

    @Override
    public Queue<BoxResponse> getResponseQueue() {
        return RESPONSE_QUEUE;
    }

    @Override
    protected void handleBoxResponse(BoxResponse response) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.box_browsesdk_menu_browse, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Create an intent to launch an instance of this activity to browse folders.
     *
     * @param context current context.
     * @param folder  folder to browse
     * @param session a session, should be already authenticated.
     * @return an intent to launch an instance of this activity.
     */
    public static Intent getLaunchIntent(Context context, final BoxFolder folder, final BoxSession session) {
        if (folder == null || SdkUtils.isBlank(folder.getId()))
            throw new IllegalArgumentException("A valid folder must be provided to browse");
        if (session == null || session.getUser() == null || SdkUtils.isBlank(session.getUser().getId()))
            throw new IllegalArgumentException("A valid user must be provided to browse");

        Intent intent = new Intent(context, BoxBrowseActivity.class);
        intent.putExtra(EXTRA_ITEM, folder);
        intent.putExtra(EXTRA_USER_ID, session.getUser().getId());
        return intent;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.e(TAG, "onFragmentInteraction " + uri.toString());
    }
}
