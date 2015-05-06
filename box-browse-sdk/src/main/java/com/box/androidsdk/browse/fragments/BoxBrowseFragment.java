package com.box.androidsdk.browse.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
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
 */
public abstract class BoxBrowseFragment extends Fragment {

    public static final String ARG_ID = "argId";
    public static final String ARG_USER_ID = "argUserId";
    public static final String ARG_NAME = "argName";

    protected static final String TAG = BoxBrowseFragment.class.getName();

    protected static final String ACTION_FETCHED_ITEMS = "BoxBrowseFragment_FetchedItems";
    protected static final String ACTION_FETCHED_INFO = "BoxBrowseFragment_FetchedInfo";
    protected static final String ACTION_DOWNLOADED_FILE_THUMBNAIL = "BoxBrowseFragment_DownloadedFileThumbnail";
    protected static final String EXTRA_SUCCESS = "BoxBrowseFragment_ArgSuccess";
    protected static final String EXTRA_ID = "BoxBrowseFragment_FolderId";
    protected static final String EXTRA_FILE_ID = "BoxBrowseFragment_FileId";
    protected static final String EXTRA_OFFSET = "BoxBrowseFragment_ArgOffset";
    protected static final String EXTRA_LIMIT = "BoxBrowseFragment_Limit";
    protected static final String EXTRA_FOLDER = "BoxBrowseFragment_Folder";
    protected static final String EXTRA_COLLECTION = "BoxBrowseFragment_Collection";

    protected String mFolderId;
    protected String mUserId;
    protected String mFolderName;
    protected BoxSession mSession;
    protected OnFragmentInteractionListener mListener;

    protected BoxItemAdapter mAdapter;
    protected RecyclerView mItemsView;
    protected ThumbnailManager mThumbnailManager;

    protected LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_FETCHED_INFO)) {
                onFolderInfoFetched(intent);
            } else if (intent.getAction().equals(ACTION_FETCHED_ITEMS)) {
                onItemsFetched(intent);
            } else if (intent.getAction().equals(ACTION_DOWNLOADED_FILE_THUMBNAIL)) {
                onDownloadedThumbnail(intent);
            }
        }
    };

    private static ThreadPoolExecutor mApiExecutor;
    private static ThreadPoolExecutor mThumbnailExecutor;

    protected ThreadPoolExecutor getApiExecutor() {
        if (mApiExecutor == null || mApiExecutor.isShutdown()) {
            mApiExecutor = new ThreadPoolExecutor(1, 1, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return mApiExecutor;
    }

    /**
     * Executor that we will submit thumbnail tasks to.
     *
     * @return executor
     */
    protected ThreadPoolExecutor getThumbnailExecutor() {
        if (mThumbnailExecutor == null || mThumbnailExecutor.isShutdown()) {
            mThumbnailExecutor = new ThreadPoolExecutor(1, 10, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return mThumbnailExecutor;
    }

    private IntentFilter initializeIntentFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FETCHED_INFO);
        filter.addAction(ACTION_FETCHED_ITEMS);
        filter.addAction(ACTION_DOWNLOADED_FILE_THUMBNAIL);
        return filter;
    }

    public BoxBrowseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFolderId = getArguments().getString(ARG_ID);
            mUserId = getArguments().getString(ARG_USER_ID);
            mFolderName = getArguments().getString(ARG_NAME);
            if (SdkUtils.isBlank(mFolderName) && mFolderId.equals(BoxConstants.ROOT_FOLDER_ID)) {
                mFolderName = getString(R.string.box_browsesdk_all_files);
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
        if (actionBar != null) {
            actionBar.setTitle(mFolderName);
        }

        View rootView = inflater.inflate(R.layout.box_browsesdk_fragment_browse, container, false);
        mItemsView = (RecyclerView) rootView.findViewById(R.id.items_recycler_view);
        mItemsView.addItemDecoration(new BoxItemDividerDecoration(getResources()));
        mItemsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new BoxItemAdapter();
        mItemsView.setAdapter(mAdapter);
        mAdapter.add(new BoxListItem(fetchInfo(), ACTION_FETCHED_INFO));
        mAdapter.notifyDataSetChanged();
        return rootView;
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

    private void onFolderInfoFetched(Intent intent) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (!intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.boxsdk_Problem_fetching_folder), Toast.LENGTH_LONG).show();
            return;
        }

        BoxFolder folder = null;
        mAdapter.remove(intent.getAction());
        if (mFolderId.equals(intent.getStringExtra(EXTRA_ID))) {
            folder = (BoxFolder) intent.getSerializableExtra(EXTRA_FOLDER);
            if (folder != null && folder.getItemCollection() != null) {
                mAdapter.addAll(folder.getItemCollection());
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });


                int offset = folder.getItemCollection().offset().intValue() - 1;
                int limit = folder.getItemCollection().limit().intValue() - 1;
                if (offset + limit < folder.getItemCollection().fullSize()) {
                    // if not all entries were fetched add a task to fetch more items if user scrolls to last entry.
                    mAdapter.add(new BoxListItem(fetchItems(mFolderId, offset + limit, 1000),
                            ACTION_FETCHED_ITEMS));
                }
            }
        }

        if (mListener != null) {
            mListener.onFolderLoaded(folder);
        }
    }

    protected void onItemsFetched(Intent intent) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (!intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.boxsdk_Problem_fetching_folder), Toast.LENGTH_LONG).show();
            return;
        }

        mAdapter.remove(intent.getAction());
        if (mFolderId.equals(intent.getStringExtra(EXTRA_ID))) {
            BoxListItems items = (BoxListItems) intent.getSerializableExtra(EXTRA_COLLECTION);
            mAdapter.addAll(items);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });


            int offset = intent.getIntExtra(EXTRA_OFFSET, -1);
            int limit = intent.getIntExtra(EXTRA_LIMIT, -1);
            if (offset + limit < items.fullSize()) {
                // if not all entries were fetched add a task to fetch more items if user scrolls to last entry.
                mAdapter.add(new BoxListItem(fetchItems(mFolderId, offset + limit, 1000),
                        ACTION_FETCHED_ITEMS));
            }
        }
    }
    /**
     * Handles showing new thumbnails after they have been downloaded.
     *
     * @param intent
     */
    protected void onDownloadedThumbnail(final Intent intent) {
        if (intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
            mAdapter.update(intent.getStringExtra(EXTRA_FILE_ID));
        }
    }

    private class BoxItemDividerDecoration extends RecyclerView.ItemDecoration {
        Drawable mDivider;

        public BoxItemDividerDecoration(Resources resources) {
            mDivider = resources.getDrawable(R.drawable.box_browsesdk_item_divider);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }

    private class BoxItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        BoxItem mItem;
        ImageView mThumbView;
        TextView mNameView;
        TextView mMetaDescription;
        ProgressBar mProgressBar;

        public BoxItemHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mThumbView = (ImageView) itemView.findViewById(R.id.box_browsesdk_thumb_image);
            mNameView = (TextView) itemView.findViewById(R.id.box_browsesdk_name_text);
            mMetaDescription = (TextView) itemView.findViewById(R.id.metaline_description);
            mProgressBar = (ProgressBar) itemView.findViewById((R.id.spinner));
        }

        public void bindItem(BoxItem item) {
            mItem = item;
            mNameView.setText(item.getName());
            String description = item.getModifiedAt() != null ?
                    String.format(Locale.ENGLISH, "%s  • %s",
                            //TODO: Need to localize date format
                            new SimpleDateFormat("MMM d yyyy").format(item.getModifiedAt()).toUpperCase(),
                            localFileSizeToDisplay(item.getSize())) :
                    localFileSizeToDisplay(item.getSize());
            mMetaDescription.setText(description);
            mThumbnailManager.setThumbnailIntoView(mThumbView, item);
            mProgressBar.setVisibility(View.GONE);
            mMetaDescription.setVisibility(View.VISIBLE);
            mThumbView.setVisibility(View.VISIBLE);
        }

        public void setLoading() {
            FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            mThumbView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mMetaDescription.setVisibility(View.GONE);
            mNameView.setText(activity.getResources().getString(R.string.boxsdk_Please_wait));
        }

        public BoxItem getItem() {
            return mItem;
        }

        @Override
        public void onClick(View v) {

            if (mListener != null) {
                if (mListener.handleOnItemClick(mItem)) {
                    FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    if (mItem instanceof BoxFolder) {
                        BoxFolder folder = (BoxFolder) mItem;
                        FragmentTransaction trans = activity.getSupportFragmentManager().beginTransaction();

                        // All fragments will always navigate into folders
                        BoxBrowseFolderFragment browseFolderFragment = BoxBrowseFolderFragment.newInstance(folder, mSession);
                        trans.replace(R.id.box_browsesdk_fragment_container, browseFolderFragment)
                                .addToBackStack(TAG)
                                .commit();
                    }
                }
            }
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
    }

    protected class BoxItemAdapter extends RecyclerView.Adapter<BoxItemHolder> {
        protected ArrayList<BoxListItem> mListItems = new ArrayList<BoxListItem>();
        protected HashMap<String, BoxListItem> mItemsMap = new HashMap<String, BoxListItem>();

        @Override
        public BoxItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.box_browsesdk_list_item, viewGroup, false);
            return new BoxItemHolder(view);
        }

        @Override
        public void onBindViewHolder(BoxItemHolder boxItemHolder, int i) {
            BoxListItem item = mListItems.get(i);
            if (item.getType() == BoxListItem.TYPE_FUTURE_TASK) {
                getApiExecutor().execute(item.getTask());
                boxItemHolder.setLoading();
                return;
            } else {
                boxItemHolder.bindItem(item.getBoxItem());

                // Fetch thumbnails for media file types
                if (item.getBoxItem() instanceof BoxFile && isMediaType(item.getBoxItem().getName())) {
                    if (item.getTask() == null) {
                        item.setTask(downloadThumbnail(item.getBoxItem().getId(),
                                mThumbnailManager.getThumbnailForFile(item.getBoxItem().getId()), boxItemHolder));
                    } else if (item.getTask().isDone()) {
                        try {
                            Intent intent = (Intent) item.getTask().get();
                            // if we were unable to get this thumbnail before try it again.
                            if (!intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
                                item.setTask(downloadThumbnail(item.getBoxItem().getId(),
                                        mThumbnailManager.getThumbnailForFile(item.getBoxItem().getId()), boxItemHolder));
                            }
                        } catch (Exception e) {
                            // e.printStackTrace();
                        }
                    }
                }
            }

            if (item.getTask() != null && !item.getTask().isDone()) {
                getThumbnailExecutor().execute(item.getTask());
            }
        }

        private boolean isMediaType(String name) {
            if (SdkUtils.isBlank(name)) {
                return false;
            }

            int index = name.lastIndexOf(".");
            if (index > 0) {
                String ext = name.substring(index + 1);
                return (ext.equals("gif") ||
                        ext.equals("bmp") ||
                        ext.equals("jpeg") ||
                        ext.equals("jpg") ||
                        ext.equals("png") ||
                        ext.equals("svg") ||
                        ext.equals("tiff"));
            }
            return false;
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
                if (!mItemsMap.containsKey(item.getId())) {
                    add(new BoxListItem(item, item.getId()));
                }
            }
        }

        public synchronized void add(BoxListItem listItem) {
            // TODO: Add item filter here and set actual identifier
            mListItems.add(listItem);
            mItemsMap.put(listItem.getIdentifier(), listItem);
        }

        public void update(String id) {
            BoxListItem item = mItemsMap.get(id);
            if (item != null) {
                int index = mListItems.indexOf(item);
                notifyItemChanged(index);
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an item being tapped to be communicated to the activity
     */
    public interface OnFragmentInteractionListener {

        /**
         * Called whenever the current folders information has been retrieved
         *
         * @param folder    the folder that the information has been retreived for
         */
        void onFolderLoaded(BoxFolder folder);

        /**
         * Called whenever an item in the RecyclerView is tapped
         *
         * @param item  the item that was tapped
         * @return  whether the tap event should continue to be handled by the fragment
         */
        boolean handleOnItemClick(BoxItem item);
    }


    /**
     * Fetch a Box folder.
     *
     * @return A FutureTask that is tasked with fetching information on the given folder.
     */
    public abstract FutureTask<Intent> fetchInfo();

    /**
     * Fetch items from folder using given offset and limit.
     *
     * @param folderId Folder id to be fetched.
     * @return A FutureTask that is tasked with fetching information on the given folder.
     */
    public abstract FutureTask<Intent> fetchItems(final String folderId, final int offset, final int limit);

    /**
     * Download the thumbnail for a given file.
     *
     * @param fileId file id to download thumbnail for.
     * @return A FutureTask that is tasked with fetching information on the given folder.
     */
    private FutureTask<Intent> downloadThumbnail(final String fileId, final File downloadLocation, final BoxItemHolder holder) {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_DOWNLOADED_FILE_THUMBNAIL);
                intent.putExtra(EXTRA_FILE_ID, fileId);
                intent.putExtra(EXTRA_SUCCESS, false);
                try {
                    // no need to continue downloading thumbnail if we already have a thumbnail
                    if (downloadLocation.exists() && downloadLocation.length() > 0) {
                        intent.putExtra(EXTRA_SUCCESS, false);
                        return intent;
                    }
                    // no need to continue downloading thumbnail if we are not viewing this thumbnail.
                    if (holder.getItem() == null || !(holder.getItem() instanceof BoxFile)
                            || !holder.getItem().getId().equals(fileId)) {
                        intent.putExtra(EXTRA_SUCCESS, false);
                        return intent;
                    }

                    BoxApiFile api = new BoxApiFile(mSession);
                    DisplayMetrics metrics = getResources().getDisplayMetrics();
                    int thumbSize = BoxRequestsFile.DownloadThumbnail.SIZE_128;
                    if (metrics.density <= DisplayMetrics.DENSITY_MEDIUM) {
                        thumbSize = BoxRequestsFile.DownloadThumbnail.SIZE_32;
                    } else if (metrics.density <= DisplayMetrics.DENSITY_HIGH) {
                        thumbSize = BoxRequestsFile.DownloadThumbnail.SIZE_64;
                    }
                    api.getDownloadThumbnailRequest(downloadLocation, fileId)
                            .setMinHeight(thumbSize)
                            .setMinWidth(thumbSize)
                            .send();
                    if (downloadLocation.exists()) {
                        intent.putExtra(EXTRA_SUCCESS, true);
                    }
                } catch (BoxException e) {
                    intent.putExtra(EXTRA_SUCCESS, false);
                } finally {
                    mLocalBroadcastManager.sendBroadcast(intent);
                }

                return intent;
            }
        });
    }
}
