package com.box.androidsdk.browse.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.BoxDateFormat;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.ArrayList;
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

    protected static final String TAG = BoxBrowseFragment.class.getName();

    protected String mFolderId;
    protected String mUserId;
    protected BoxSession mSession;
    protected OnFragmentInteractionListener mListener;

    protected RecyclerView.Adapter mAdapter;
    protected RecyclerView mItemsView;
    protected ArrayList<BoxItem> mItems = new ArrayList<BoxItem>();
    protected ProgressDialog mDialog;

    private static ThreadPoolExecutor mApiExecutor;

    private ThreadPoolExecutor getApiExecutor() {
        if (mApiExecutor == null) {
            mApiExecutor = new ThreadPoolExecutor(1, 1, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()) {

                @Override
                protected void afterExecute(Runnable r, Throwable t) {
                    super.afterExecute(r, t);
                    try {
                        BoxResponse resp = (BoxResponse) ((BoxFutureTask) r).get();
                        handleResponse(resp);
                    } catch (Exception e) {
                        // This should not happen
                    }
                }
            };
        }
        return mApiExecutor;
    }

    /**
     * Use this factory method to create a new instance of
     * of the Browse fragment
     *
     * @param folderId the id of the folder to browse
     * @param userId   the id of the user that the contents will be loaded for
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxBrowseFragment newInstance(String folderId, String userId) {
        BoxBrowseFragment fragment = new BoxBrowseFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FOLDER_ID, folderId);
        args.putString(ARG_USER_ID, userId);
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

            if (SdkUtils.isBlank(mFolderId) || SdkUtils.isBlank(mUserId)) {
                Toast.makeText(getActivity(), R.string.box_browsesdk_cannot_view_folder, Toast.LENGTH_LONG).show();
                // TODO: Call error handler
            }

            mSession = new BoxSession(getActivity(), mUserId);
            fetchFolderItems();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.box_browsesdk_fragment_browse, container, false);
        mItemsView = (RecyclerView) rootView.findViewById(R.id.items_recycler_view);
        mItemsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new BoxItemAdapter();
        mItemsView.setAdapter(mAdapter);
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

    private void fetchFolderItems() {
        BoxRequestsFolder.GetFolderInfo req = new BoxApiFolder(mSession).getInfoRequest(mFolderId)
                // TODO: Should clean-up to only include required fields
                .setFields(BoxFolder.ALL_FIELDS);
        getApiExecutor().execute(req.toTask());
    }

    private void handleResponse(BoxResponse resp) {
        if (resp.isSuccess()) {
            if (resp.getRequest() instanceof BoxRequestsFolder.GetFolderInfo) {
                mItems.clear();
                BoxFolder folder = (BoxFolder) resp.getResult();
                mItems.addAll(folder.getItemCollection());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        } else {
            Toast.makeText(getActivity(), R.string.box_browsesdk_network_error, Toast.LENGTH_LONG).show();
        }
    }

    protected void showSpinner() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mDialog != null && mDialog.isShowing()) {
                        return;
                    }
                    mDialog = ProgressDialog.show(getActivity(), getString(R.string.boxsdk_Please_wait), getString(R.string.boxsdk_Please_wait));
                    mDialog.show();
                } catch (Exception e) {
                    // WindowManager$BadTokenException will be caught and the app will not display
                    // the 'Force Close' message
                    mDialog = null;
                    return;
                }
            }
        });
    }

    private class BoxItemHolder extends RecyclerView.ViewHolder {

        ImageView mThumbView;
        TextView mNameView;
        TextView mSizeView;
        TextView mUpdatedView;

        public BoxItemHolder(View itemView) {
            super(itemView);

            mThumbView = (ImageView) itemView.findViewById(R.id.box_browsesdk_thumb_image);
            mNameView = (TextView) itemView.findViewById(R.id.box_browsesdk_name_text);
            mSizeView = (TextView) itemView.findViewById(R.id.box_browsesdk_size_text);
            mUpdatedView = (TextView) itemView.findViewById(R.id.box_browsesdk_updated_text);
        }

        public void bindItem(BoxItem item) {
            mNameView.setText(item.getName());
            long size = item.getSize() == null ? 0 : item.getSize().longValue();
            mSizeView.setText(Long.toString(size));
            mUpdatedView.setText(BoxDateFormat.format(item.getModifiedAt()));
        }
    }

    private class BoxItemAdapter extends RecyclerView.Adapter<BoxItemHolder> {

        @Override
        public BoxItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.box_browsesdk_box_item, viewGroup, false);
            return new BoxItemHolder(view);
        }

        @Override
        public void onBindViewHolder(BoxItemHolder boxItemHolder, int i) {
            BoxItem item = mItems.get(i);
            boxItemHolder.bindItem(item);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
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

}
