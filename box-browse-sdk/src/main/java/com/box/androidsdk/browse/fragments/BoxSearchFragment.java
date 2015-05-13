package com.box.androidsdk.browse.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxSearchListAdapter;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Use the {@link com.box.androidsdk.browse.fragments.BoxSearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BoxSearchFragment extends BoxBrowseFragment {

    protected BoxFolder mFolder = null;
    protected final static String EXTRA_SEARCH_REQUEST = "BoxSearchFragment_SEARCH_REQUEST";
    private static final String OUT_ITEM = "outItem";
    BoxSearchHolder mSearchRequestHolder;
    protected BoxApiSearch mApiSearch;
    public static final int DEFAULT_SEARCH_LIMIT = 30;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getSerializable(OUT_ITEM) instanceof BoxSearchHolder) {
            mSearchRequestHolder =(BoxSearchHolder) getArguments().getSerializable(OUT_ITEM);
        }
        mApiSearch = new BoxApiSearch(mSession);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState == null || savedInstanceState.getSerializable(OUT_ITEM) == null) {
            mAdapter.add(new BoxListItem(fetchInfo(), ACTION_FETCHED_INFO));
        }
        return view;
    }

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param session The BoxSession that should be used to perform network calls.
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxSearchFragment newInstance(BoxSession session, BoxRequestsSearch.Search searchRequest) {
        BoxSearchFragment fragment = new BoxSearchFragment();
        BoxSearchHolder holder = new BoxSearchHolder(searchRequest);
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, session.getUserId());
        args.putSerializable(OUT_ITEM, holder);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Helper method creating the simplest search of the entire account for the given query.
     *
     * @param session
     * @param query
     * @return
     */
    public static BoxSearchFragment newInstance(BoxSession session, String query) {
        return BoxSearchFragment.newInstance(session, (new BoxApiSearch(session)).getSearchRequest(query));
    }


    @Override
    public FutureTask<Intent> fetchInfo() {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCHED_INFO);
                try {
                    BoxListItems items = mSearchRequestHolder.createSearchRequest(mApiSearch).setLimit(DEFAULT_SEARCH_LIMIT).send();
                    if (items != null) {
                        intent.putExtra(EXTRA_SUCCESS, true);
                        intent.putExtra(EXTRA_COLLECTION, items);
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

    public FutureTask<Intent> fetchItems(final int offset, final int limit) {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCHED_ITEMS);
                intent.putExtra(EXTRA_OFFSET, offset);
                intent.putExtra(EXTRA_LIMIT, limit);
                try {

                    BoxListItems items = mSearchRequestHolder.createSearchRequest(mApiSearch).setOffset(offset).setLimit(limit).send();
                    System.out.println("offset " + offset + " limit " + limit + " items " + items.limit());
                    intent.putExtra(EXTRA_SUCCESS, true);
                    intent.putExtra(EXTRA_COLLECTION, items);
                } catch (BoxException e) {
                    e.printStackTrace();
                    e.getCause().printStackTrace();
                    intent.putExtra(EXTRA_SUCCESS, false);
                } finally {
                    mLocalBroadcastManager.sendBroadcast(intent);
                }

                return intent;
            }
        });
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mFolder = (BoxFolder) savedInstanceState.getSerializable(OUT_ITEM);
            if (mFolder != null && mFolder.getItemCollection() != null) {
                mAdapter.addAll(mFolder.getItemCollection());
                mAdapter.notifyDataSetChanged();
                if (mToolbar != null) {
                    mToolbar.setTitle(mFolder.getName());
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(OUT_ITEM, mFolder);
        super.onSaveInstanceState(outState);
    }


    protected void onItemsFetched(Intent intent) {
        logIntent(intent);
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (!intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_performing_search), Toast.LENGTH_LONG).show();
            return;
        }
        super.onItemsFetched(intent);
  }

    protected void onInfoFetched(Intent intent) {
        FragmentActivity activity = getActivity();
        if (activity == null || mAdapter == null) {
            return;
        }

        if (!intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.box_browsesdk_problem_performing_search), Toast.LENGTH_LONG).show();
            return;
        }
        if (intent.getSerializableExtra(EXTRA_COLLECTION) != null) {
            mAdapter.removeAll();
            displayBoxList((BoxListItems) intent.getSerializableExtra(EXTRA_COLLECTION));
        }

        mSwipeRefresh.setRefreshing(false);
    }

    protected BoxItemHolder createBoxViewHolder(final ViewGroup viewGroup, int i){
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.box_browsesdk_list_item, viewGroup, false);
        return new BoxItemHolder(view){
            @Override
            public void bindItem(BoxItem item) {
                mItem = item;
                mNameView.setText(item.getName());
                mMetaDescription.setText(BoxSearchListAdapter.createPath(item, File.separator));
                mThumbnailManager.setThumbnailIntoView(mThumbView, item);
                mProgressBar.setVisibility(View.GONE);
                mMetaDescription.setVisibility(View.VISIBLE);
                mThumbView.setVisibility(View.VISIBLE);
            }
        };
    }


    private void logIntent(final Intent intent) {
        Iterator<String> iterator = intent.getExtras().keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            System.out.println("extra: " + key + " => " + String.valueOf(intent.getExtras().get(key)));
        }

    }


    /**
     * Helper class to hold onto the parameters held by given search request. This class is not meant
     * to be used beyond storing and retrieving these fields.
     */
    protected static class BoxSearchHolder extends BoxRequestsSearch.Search implements Serializable {
        /**
         * Construct a search holder.
         * @param searchRequest the search request used to populate this holder.
         */
        public BoxSearchHolder(BoxRequestsSearch.Search searchRequest) {
            super("", "", null);
            importRequestContentMapsFrom(searchRequest);
        }

        /**
         * Create a new search request from the given search holder object.
         * @param searchApi the search api that should be used for this request.
         * @return a new search based off the parameters of this holder.
         */
        public BoxRequestsSearch.Search createSearchRequest(final BoxApiSearch searchApi){
            BoxRequestsSearch.Search search = searchApi.getSearchRequest(this.getQuery());
            for (Map.Entry<String,String> entry :mQueryMap.entrySet()){
                search.limitValueForKey(entry.getKey(), entry.getValue());
            }
            return search;
        }

    }
}
