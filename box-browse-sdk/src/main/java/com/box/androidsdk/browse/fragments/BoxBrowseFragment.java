package com.box.androidsdk.browse.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxListItemAdapter;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BoxBrowseFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BoxBrowseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BoxBrowseFragment extends Fragment {

    private static final String ARG_FOLDER_ID = "argFolderId";
    private static final String ARG_USER_ID = "argUserId";
    public static final String ARG_FOLDER_NAME = "argFolderName";

    protected static final String TAG = BoxBrowseFragment.class.getName();

    protected String mFolderId;
    protected String mUserId;
    protected String mFolderName;
    protected BoxSession mSession;
    protected OnFragmentInteractionListener mListener;

    protected BoxItemAdapter mAdapter;
    protected RecyclerView mItemsView;
    protected ThumbnailManager mThumbnailManager;
    protected Controller mController = new Controller();

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Controller.ACTION_FETCHED_FOLDER)) {
                onFolderFetched(intent);
            } else if (intent.getAction().equals(Controller.ACTION_FETCHED_FOLDER_ITEMS)) {
                onFolderItemsFetched(intent);
            }
        }
    };

    private static ThreadPoolExecutor mApiExecutor;

    private ThreadPoolExecutor getApiExecutor() {
        if (mApiExecutor == null || mApiExecutor.isShutdown()) {
            mApiExecutor = new ThreadPoolExecutor(1, 1, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return mApiExecutor;
    }

    private IntentFilter initializeIntentFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Controller.ACTION_FETCHED_FOLDER);
        filter.addAction(Controller.ACTION_FETCHED_FOLDER_ITEMS);
        filter.addAction(Controller.ACTION_DOWNLOADED_FILE_THUMBNAIL);
        return filter;
    }

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param folder    the folder to browse
     * @param session   the session that the contents will be loaded for
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxBrowseFragment newInstance(BoxFolder folder, BoxSession session) {
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
    public static BoxBrowseFragment newInstance(String folderId, String userId) {
        return newInstance(folderId, userId, null);
    }

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param folderId      the id of the folder to browse
     * @param userId        the id of the user that the contents will be loaded for
     * @param folderName    the name of the folder that will be shown in the action bar
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxBrowseFragment newInstance(String folderId, String userId, String folderName) {
        BoxBrowseFragment fragment = new BoxBrowseFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FOLDER_ID, folderId);
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_FOLDER_NAME, folderName);
        fragment.setArguments(args);
        return fragment;
    }

    public BoxBrowseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFolderId = getArguments().getString(ARG_FOLDER_ID);
            mUserId = getArguments().getString(ARG_USER_ID);
            mFolderName = getArguments().getString(ARG_FOLDER_NAME);
            if (SdkUtils.isBlank(mFolderName) && mFolderId.equals(BoxConstants.ROOT_FOLDER_ID)) {
               mFolderName =  getString(R.string.box_browsesdk_all_files);
            }

            if (SdkUtils.isBlank(mFolderId) || SdkUtils.isBlank(mUserId)) {
                Toast.makeText(getActivity(), R.string.box_browsesdk_cannot_view_folder, Toast.LENGTH_LONG).show();
                // TODO: Call error handler
            }

            mThumbnailManager = initializeThumbnailManager();

            // Initialize broadcast managers
            mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
            IntentFilter filters = initializeIntentFilters();
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, filters);

            mSession = new BoxSession(getActivity(), mUserId);

        }
    }

    private ThumbnailManager initializeThumbnailManager() {
        try {
            return new ThumbnailManager(getActivity().getCacheDir());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // TODO: should finish fragment
            return null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Toolbar actionBar = (Toolbar) getActivity().findViewById(R.id.box_action_bar);
        actionBar.setTitle(mFolderName);

        View rootView = inflater.inflate(R.layout.box_browsesdk_fragment_browse, container, false);
        mItemsView = (RecyclerView) rootView.findViewById(R.id.items_recycler_view);
        mItemsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new BoxItemAdapter();
        mItemsView.setAdapter(mAdapter);
        mAdapter.add(new BoxListItem(mController.fetchFolder(), Controller.ACTION_FETCHED_FOLDER));
        mAdapter.notifyDataSetChanged();
        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void onFolderFetched(Intent intent) {
        if (!intent.getBooleanExtra(Controller.ARG_SUCCESS, false)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.boxsdk_Problem_fetching_folder), Toast.LENGTH_LONG).show();
            return;
        }

        mAdapter.remove(intent.getAction());
        if (mFolderId.equals(intent.getStringExtra(Controller.ARG_FOLDER_ID))) {
            BoxFolder folder = (BoxFolder) intent.getSerializableExtra(Controller.ARG_BOX_FOLDER);
            if (folder != null && folder.getItemCollection() != null) {
                mAdapter.addAll(folder.getItemCollection());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    private void onFolderItemsFetched(Intent intent) {
        if (!intent.getBooleanExtra(Controller.ARG_SUCCESS, false)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.boxsdk_Problem_fetching_folder), Toast.LENGTH_LONG).show();
            return;
        }

        if (mFolderId.equals(intent.getStringExtra(Controller.ARG_FOLDER_ID))) {
            BoxListItems items = (BoxListItems) intent.getSerializableExtra(Controller.ARG_BOX_COLLECTION);
            mAdapter.addAll(items);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class BoxItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        BoxItem mItem;
        ImageView mThumbView;
        TextView mNameView;
        TextView mSizeView;
        TextView mUpdatedView;
        TextView mMetaDescription;
        ProgressBar mProgressBar;

        public BoxItemHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mThumbView = (ImageView) itemView.findViewById(R.id.box_browsesdk_thumb_image);
            mNameView = (TextView) itemView.findViewById(R.id.box_browsesdk_name_text);
            mSizeView = (TextView) itemView.findViewById(R.id.box_browsesdk_size_text);
            mUpdatedView = (TextView) itemView.findViewById(R.id.box_browsesdk_updated_text);
            mMetaDescription = (TextView) itemView.findViewById(R.id.metaline_description);
            mProgressBar = (ProgressBar) itemView.findViewById((R.id.spinner));
        }

        public void bindItem(BoxItem item) {
            mItem = item;
            mNameView.setText(item.getName());
//            long size = item.getSize() == null ? 0 : item.getSize().longValue();
//            mSizeView.setText(Long.toString(size));
            mMetaDescription.setText(localFileSizeToDisplay(item.getSize()));
            mProgressBar.setVisibility(View.GONE);
            mThumbView.setVisibility(View.VISIBLE);
            mThumbnailManager.setThumbnailIntoView(mThumbView, item);
        }

        public void setLoading() {
            mThumbView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mNameView.setText(getActivity().getResources().getString(R.string.boxsdk_Please_wait));
        }


        /**
         * Java version of routine to turn a long into a short user readable string.
         * <p/>
         * This routine is used if the JNI native C version is not available.
         *
         * @param numSize the number of bytes in the file.
         * @return String Short human readable String e.g. 2.5 MB
         */
        private String localFileSizeToDisplay(final double numSize) {
            final int constKB = 1024;
            final int constMB = constKB * constKB;
            final int constGB = constMB * constKB;
            final double floatKB = 1024.0f;
            final double floatMB = floatKB * floatKB;
            final double floatGB = floatMB * floatKB;
            final String BYTES = "B";
            String textSize = "0 bytes";
            String strSize = Double.toString(numSize);
            double size;

            if (numSize < constKB) {
                textSize = strSize + " " + BYTES;
            } else if ((numSize >= constKB) && (numSize < constMB)) {
                size = numSize / floatKB;
                textSize = String.format(Locale.ENGLISH, "%4.1f KB", size);
            } else if ((numSize >= constMB) && (numSize < constGB)) {
                size = numSize / floatMB;
                textSize = String.format(Locale.ENGLISH, "%4.1f MB", size);
            } else if (numSize >= constGB) {
                size = numSize / floatGB;
                textSize = String.format(Locale.ENGLISH, "%4.1f GB", size);
            }
            return textSize;
        }

        @Override
        public void onClick(View v) {
            if (mItem instanceof BoxFolder) {
                BoxFolder folder = (BoxFolder) mItem;
                FragmentTransaction trans = getActivity().getSupportFragmentManager().beginTransaction();
                BoxBrowseFragment browseFragment = newInstance(folder, mSession);
                trans.replace(R.id.box_browsesdk_fragment_container, browseFragment)
                        .addToBackStack(TAG)
                        .commit();
            } else {

            }
        }
    }

    private class BoxItemAdapter extends RecyclerView.Adapter<BoxItemHolder> {
        protected ArrayList<BoxListItem> mListItems = new ArrayList<BoxListItem>();
        protected HashMap<String, BoxListItem> mItemsMap = new HashMap<String, BoxListItem>();

        @Override
        public BoxItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            BoxListItem item = mListItems.get(i);
            View view = item != null && item.getBoxItem() != null && item.getBoxItem().getType().equals(BoxFolder.TYPE) ?
                    LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.boxsdk_box_folder_list_item, viewGroup, false) :
                    LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.boxsdk_box_list_item, viewGroup, false);
            return new BoxItemHolder(view);
        }

        @Override
        public void onBindViewHolder(BoxItemHolder boxItemHolder, int i) {
            BoxListItem item = mListItems.get(i);
            if (item.getType() == BoxListItem.TYPE_FUTURE_TASK) {
                getApiExecutor().execute(item.getTask());
                boxItemHolder.setLoading();
            } else {
                boxItemHolder.bindItem(item.getBoxItem());
            }
        }

        @Override
        public int getItemCount() {
            return mListItems.size();
        }

        public void remove(BoxListItem listItem) {
            remove(listItem.getIdentifier());
        }

        public synchronized void remove(String key) {
            BoxListItem item = mItemsMap.remove(key);
            if (item != null) {
                mListItems.remove(item);
            }
        }

        public void addAll(BoxListItems items) {
            for (BoxItem item : items) {
                add(new BoxListItem(item, Controller.ACTION_FETCHED_FOLDER_ITEMS));
            }
        }

        public synchronized void add(BoxListItem listItem) {
            // TODO: Add item filter here and set actual identifier
            mListItems.add(listItem);
            mItemsMap.put(listItem.getIdentifier(), listItem);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }


    public class Controller {

        public static final String ACTION_FETCHED_FOLDER_ITEMS = "PickerActivity_FetchedFolderItems";
        public static final String ACTION_FETCHED_FOLDER = "PickerActivity_FetchedFolder";
        public static final String ACTION_DOWNLOADED_FILE_THUMBNAIL = "PickerActivity_DownloadedFileThumbnail";
        public static final String ARG_SUCCESS = "PickerActivity_ArgSuccess";
        public static final String ARG_FOLDER_ID = "PickerActivity_FolderId";
        public static final String ARG_FILE_ID = "PickerActivity_FileId";
        public static final String ARG_OFFSET = "PickerActivity_ArgOffset";
        public static final String ARG_LIMIT = "PickerActivity_Limit";
        public static final String ARG_BOX_FOLDER = "PickerActivity_Folde";
        public static final String ARG_BOX_COLLECTION = "PickerActivity_Collection";

        /**
         * Fetch a Box folder.
         *
         * @return A FutureTask that is tasked with fetching information on the given folder.
         */
        public FutureTask<Intent> fetchFolder() {
            return new FutureTask<Intent>(new Callable<Intent>() {

                @Override
                public Intent call() throws Exception {
                    Intent intent = new Intent();
                    intent.setAction(ACTION_FETCHED_FOLDER);
                    intent.putExtra(ARG_FOLDER_ID, mFolderId);
                    try {
                        BoxRequestsFolder.GetFolderInfo req = new BoxApiFolder(mSession).getInfoRequest(mFolderId)
                                // TODO: Should clean-up to only include required fields
                                .setFields(BoxFolder.ALL_FIELDS);
                        BoxFolder bf = req.send();
                        if (bf != null) {
                            intent.putExtra(ARG_SUCCESS, true);
                            intent.putExtra(Controller.ARG_BOX_FOLDER, bf);
                        }

                    } catch (BoxException e) {
                        e.printStackTrace();
                        intent.putExtra(ARG_SUCCESS, false);
                    } finally {
                        mLocalBroadcastManager.sendBroadcast(intent);
                    }

                    return intent;
                }
            });

        }

        /**
         * Fetch items from folder using given offset and limit.
         *
         * @param folderId Folder id to be fetched.
         * @return A FutureTask that is tasked with fetching information on the given folder.
         */
        public FutureTask<Intent> fetchFolderItems(final String folderId, final int offset, final int limit) {
            return new FutureTask<Intent>(new Callable<Intent>() {

                @Override
                public Intent call() throws Exception {
                    Intent intent = new Intent();
                    intent.setAction(ACTION_FETCHED_FOLDER_ITEMS);
                    intent.putExtra(ARG_OFFSET, offset);
                    intent.putExtra(ARG_LIMIT, limit);
                    intent.putExtra(ARG_FOLDER_ID, folderId);
                    try {

                        // this call the collection is just BoxObjectItems and each does not appear to be an instance of BoxItem.
                        ArrayList<String> itemFields = new ArrayList<String>();
                        String[] fields = new String[]{BoxFile.FIELD_NAME, BoxFile.FIELD_SIZE, BoxFile.FIELD_OWNED_BY, BoxFolder.FIELD_HAS_COLLABORATIONS, BoxFolder.FIELD_IS_EXTERNALLY_OWNED};
                        BoxApiFolder api = new BoxApiFolder(mSession);
                        BoxListItems items = api.getItemsRequest(folderId).setLimit(limit).setOffset(offset).setFields(fields).send();
                        intent.putExtra(ARG_SUCCESS, true);
                        intent.putExtra(Controller.ARG_BOX_COLLECTION, items);
                    } catch (BoxException e) {
                        e.printStackTrace();
                        intent.putExtra(ARG_SUCCESS, false);
                    } finally {
                        mLocalBroadcastManager.sendBroadcast(intent);
                    }

                    return intent;
                }
            });

        }

        /**
         * Download the thumbnail for a given file.
         *
         * @param fileId file id to download thumbnail for.
         * @return A FutureTask that is tasked with fetching information on the given folder.
         */
        public FutureTask<Intent> downloadThumbnail(final String fileId, final File downloadLocation, final BoxListItemAdapter.ViewHolder holder) {
            return new FutureTask<Intent>(new Callable<Intent>() {

                @Override
                public Intent call() throws Exception {
                    Intent intent = new Intent();
                    intent.setAction(ACTION_DOWNLOADED_FILE_THUMBNAIL);
                    intent.putExtra(ARG_FILE_ID, fileId);
                    intent.putExtra(ARG_SUCCESS, false);
                    try {
                        // no need to continue downloading thumbnail if we already have a thumbnail
                        if (downloadLocation.exists() && downloadLocation.length() > 0) {
                            intent.putExtra(ARG_SUCCESS, false);
                            return intent;
                        }
                        // no need to continue downloading thumbnail if we are not viewing this thumbnail.
                        if (holder.getBoxListItem() == null || !(holder.getBoxListItem().getBoxItem() instanceof BoxFile)
                                || !holder.getBoxListItem().getBoxItem().getId().equals(fileId)) {
                            intent.putExtra(ARG_SUCCESS, false);
                            return intent;
                        }

                        BoxApiFile api = new BoxApiFile(mSession);
                        api.getDownloadThumbnailRequest(downloadLocation, fileId).send();
                        if (downloadLocation.exists()) {
                            intent.putExtra(ARG_SUCCESS, true);
                        }
                    } catch (BoxException e) {
                        intent.putExtra(ARG_SUCCESS, false);
                    } finally {
                        mLocalBroadcastManager.sendBroadcast(intent);
                    }

                    return intent;
                }
            });

        }

    }

}
