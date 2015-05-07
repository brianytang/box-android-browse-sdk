package com.box.androidsdk.browse.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.fragments.BoxBrowseFolderFragment;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.utils.SdkUtils;

public class BoxBrowseFileActivity extends BoxBrowseActivity {
    /**
     * Extra serializable intent parameter that adds a {@link com.box.androidsdk.content.models.BoxFile} to the intent
     */
    public static final String EXTRA_BOX_FILE = "extraBoxFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.box_browsesdk_activity_file);
        if (mBrowseFragment == null) {
            mBrowseFragment = BoxBrowseFolderFragment.newInstance(mItem.getId(), mSession.getUserId());
        }
        getSupportFragmentManager().beginTransaction()
                .add(R.id.box_browsesdk_fragment_container, mBrowseFragment)
                .commit();
        initToolbar();
    }

    @Override
    public boolean handleOnItemClick(BoxItem item) {
        if (item instanceof BoxFile) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_BOX_FILE, item);
            setResult(Activity.RESULT_OK, intent);
            finish();
            return false;
        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.box_browsesdk_menu_file, menu);
        return true;
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

        Intent intent = new Intent(context, BoxBrowseFileActivity.class);
        intent.putExtra(EXTRA_ITEM, folder);
        intent.putExtra(EXTRA_USER_ID, session.getUser().getId());
        return intent;
    }
}
