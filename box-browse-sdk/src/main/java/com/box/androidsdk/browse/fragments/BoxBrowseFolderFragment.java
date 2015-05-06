package com.box.androidsdk.browse.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Use the {@link BoxBrowseFolderFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BoxBrowseFolderFragment extends BoxBrowseFragment {

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param folder  the folder to browse
     * @param session the session that the contents will be loaded for
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxBrowseFolderFragment newInstance(BoxFolder folder, BoxSession session) {
        return newInstance(folder.getId(), session.getUserId(), folder.getName());
    }

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param folderId the id of the folder to browse
     * @param userId   the id of the user that the contents will be loaded for
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxBrowseFolderFragment newInstance(String folderId, String userId) {
        return newInstance(folderId, userId, null);
    }

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param folderId   the id of the folder to browse
     * @param userId     the id of the user that the contents will be loaded for
     * @param folderName the name of the folder that will be shown in the action bar
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxBrowseFolderFragment newInstance(String folderId, String userId, String folderName) {
        BoxBrowseFolderFragment fragment = new BoxBrowseFolderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, folderId);
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_NAME, folderName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public FutureTask<Intent> fetchInfo() {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCHED_INFO);
                intent.putExtra(EXTRA_ID, mFolderId);
                try {
                    BoxRequestsFolder.GetFolderInfo req = new BoxApiFolder(mSession).getInfoRequest(mFolderId)
                            // TODO: Should clean-up to only include required fields
                            .setFields(BoxFolder.ALL_FIELDS);
                    BoxFolder bf = req.send();
                    if (bf != null) {
                        intent.putExtra(EXTRA_SUCCESS, true);
                        intent.putExtra(EXTRA_FOLDER, bf);
                    }

                } catch (BoxException e) {
                    e.printStackTrace();
                    intent.putExtra(EXTRA_SUCCESS, false);
                } finally {
                    mLocalBroadcastManager.sendBroadcast(intent);
                }

                return intent;
            }
        });
    }

    @Override
    public FutureTask<Intent> fetchItems(final String folderId, final int offset, final int limit) {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCHED_ITEMS);
                intent.putExtra(EXTRA_OFFSET, offset);
                intent.putExtra(EXTRA_LIMIT, limit);
                intent.putExtra(EXTRA_ID, folderId);
                try {

                    // this call the collection is just BoxObjectItems and each does not appear to be an instance of BoxItem.
                    ArrayList<String> itemFields = new ArrayList<String>();
                    String[] fields = new String[]{BoxFile.FIELD_NAME, BoxFile.FIELD_SIZE, BoxFile.FIELD_OWNED_BY, BoxFolder.FIELD_HAS_COLLABORATIONS, BoxFolder.FIELD_IS_EXTERNALLY_OWNED};
                    BoxApiFolder api = new BoxApiFolder(mSession);
                    BoxListItems items = api.getItemsRequest(folderId).setLimit(limit).setOffset(offset).setFields(fields).send();
                    intent.putExtra(EXTRA_SUCCESS, true);
                    intent.putExtra(EXTRA_COLLECTION, items);
                } catch (BoxException e) {
                    e.printStackTrace();
                    intent.putExtra(EXTRA_SUCCESS, false);
                } finally {
                    mLocalBroadcastManager.sendBroadcast(intent);
                }

                return intent;
            }
        });
    }

    @Override
    protected void onItemTapped(BoxItem item) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (item instanceof BoxFolder) {
            BoxFolder folder = (BoxFolder) item;
            FragmentTransaction trans = activity.getSupportFragmentManager().beginTransaction();
            BoxBrowseFragment browseFragment = newInstance(folder, mSession);
            trans.replace(R.id.box_browsesdk_fragment_container, browseFragment)
                    .addToBackStack(TAG)
                    .commit();
        } else {

        }
    }
}
