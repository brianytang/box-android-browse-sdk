package com.box.androidsdk.browse.activities;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.fragments.BoxBrowseFolderFragment;
import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.browse.fragments.BoxCreateFolderFragment;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;

import org.apache.http.HttpStatus;

public class BoxBrowseFolderActivity extends BoxBrowseActivity implements View.OnClickListener, BoxCreateFolderFragment.OnCreateFolderListener {

    /**
     * Extra serializable intent parameter that adds a {@link com.box.androidsdk.content.models.BoxFolder} to the intent
     */
    public static final String EXTRA_BOX_FOLDER = "extraBoxFolder";

    protected Button mSelectFolderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.box_browsesdk_activity_folder);
        if (mBrowseFragment == null) {
            mBrowseFragment = BoxBrowseFolderFragment.newInstance(mItem.getId(), mSession.getUserId());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.box_browsesdk_fragment_container, mBrowseFragment)
                    .commit();
        }
        mSelectFolderButton = (Button) findViewById(R.id.box_browsesdk_select_folder_button);
        mSelectFolderButton.setOnClickListener(this);

        initToolbar();
    }

    @Override
    public void onFolderLoaded(BoxFolder folder) {
        super.onFolderLoaded(folder);
        if (folder != null) {
            mSelectFolderButton.setEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_BOX_FOLDER, mCurrentFolder);
        setResult(RESULT_OK, intent);
        finish();
    }


    @Override
    public void handleBoxResponse(BoxResponse response) {
        if (response.isSuccess()) {
            if (response.getRequest() instanceof BoxRequestsFolder.CreateFolder) {
                BoxBrowseFragment browseFrag = getTopBrowseFragment();
                if (browseFrag != null) {
                    browseFrag.onRefresh();
                }

            }
        } else {
            int resId = R.string.box_browsesdk_network_error;
            if (response.getException() instanceof BoxException) {
                if (((BoxException) response.getException()).getResponseCode() == HttpStatus.SC_CONFLICT) {
                    resId = R.string.box_browsesdk_create_folder_conflict;
                } else {

                }
            }
            Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.box_browsesdk_menu_folder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.box_browsesdk_action_create_folder) {
            BoxCreateFolderFragment.newInstance(mCurrentFolder, mSession)
                    .show(getFragmentManager(), TAG);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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

        Intent intent = new Intent(context, BoxBrowseFolderActivity.class);
        intent.putExtra(EXTRA_ITEM, folder);
        intent.putExtra(EXTRA_USER_ID, session.getUser().getId());
        return intent;
    }

    @Override
    public void onCreateFolder(String name) {
        BoxApiFolder folderApi = new BoxApiFolder(mSession);
        BoxRequestsFolder.CreateFolder req = folderApi.getCreateRequest(mCurrentFolder.getId(), name);
        getApiExecutor(getApplication()).execute(req.toTask());
    }
}
