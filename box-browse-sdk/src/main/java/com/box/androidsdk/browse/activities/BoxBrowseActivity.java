package com.box.androidsdk.browse.activities;

import android.app.Application;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.fragments.BoxBrowseFolderFragment;
import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.requests.BoxResponse;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class BoxBrowseActivity extends BoxThreadPoolExecutorActivity implements BoxBrowseFolderFragment.OnFragmentInteractionListener {

    /**
     * Extra intent parameter that adds a user id to the intent
     */
    public static final String EXTRA_USER_ID = "extraUserId";

    protected static final String TAG = BoxBrowseActivity.class.getName();
    private static final String OUT_BROWSE_FRAGMENT = "outBrowseFragment";

    protected BoxFolder mCurrentFolder = null;
    protected BoxBrowseFragment mBrowseFragment = null;

    private static final ConcurrentLinkedQueue<BoxResponse> RESPONSE_QUEUE = new ConcurrentLinkedQueue<BoxResponse>();
    private static ThreadPoolExecutor mApiExecutor;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentFolder = (BoxFolder) mItem;
        if (savedInstanceState != null) {
            mBrowseFragment = (BoxBrowseFragment) getSupportFragmentManager().getFragment(savedInstanceState, OUT_BROWSE_FRAGMENT);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, OUT_BROWSE_FRAGMENT, mBrowseFragment);
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
    public boolean handleOnItemClick(BoxItem item) {
        if (item instanceof BoxFolder) {
            mCurrentFolder = (BoxFolder) item;
        }
        return true;
    }

    @Override
    public void onFolderLoaded(BoxFolder folder) {
        mCurrentFolder = folder;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.box_browsesdk_action_search) {
            // Launch search experience
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected BoxBrowseFragment getTopBrowseFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment frag = fragmentManager.findFragmentById(R.id.box_browsesdk_fragment_container);
        return frag instanceof BoxBrowseFragment ? (BoxBrowseFragment) frag : null;
    }
}
