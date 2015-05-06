package com.box.androidsdk.browse.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.fragments.BoxBrowseFolderFragment;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.utils.SdkUtils;

public class BoxBrowseFolderActivity extends BoxBrowseActivity implements View.OnClickListener {

    /**
     * Extra serializable intent parameter that adds a {@link com.box.androidsdk.content.models.BoxFolder} to the intent
     */
    public static final String EXTRA_BOX_FOLDER = "extraBoxFolder";

    protected Button mSelectFolderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initViews() {
        setContentView(R.layout.box_browsesdk_activity_folder);
        initToolbar();
        mSelectFolderButton = (Button) findViewById(R.id.box_browsesdk_select_folder_button);
        mSelectFolderButton.setOnClickListener(this);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.box_browsesdk_fragment_container, BoxBrowseFolderFragment.newInstance(mItem.getId(), mSession.getUserId()))
                .commit();
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
}
