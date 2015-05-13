package com.box.androidsdk.browse.uidata;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.adapters.BoxSearchListAdapter;
import com.box.androidsdk.browse.uidata.NavigationItem;
import com.box.androidsdk.browse.uidata.ThumbnailManager;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxJsonObject;
import com.box.androidsdk.content.models.BoxList;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.BoxDateFormat;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is a view used to show search results.
 */
public class BoxSearchView extends SearchView{

    private BoxSession mSession;
    private BoxApiSearch mSearchApi;
    private OnQueryTextListener mOnQueryTextListener;
    /**
     * Sets the maximum number of results to display in the search widget.
     */
    private int mMaxDisplayedResults = 10;


   public BoxSearchView(final Context context){
       super(context);
   }

    public BoxSearchView(final Context context, final AttributeSet attrs){
        super(context, attrs);
        setSuggestionsAdapter(new BoxSearchListAdapter(context, R.layout.abc_list_menu_item_layout, 0, mSession));
        if (mSession == null){
            // this widget cannot be used until a session has been set into it.
            this.setEnabled(false);
        }
        this.setOnSuggestionListener(new OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                System.out.println("onSuccestionSelect " + position);
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                System.out.println("onSuggestionClick " + position);

                return false;
            }
        });
        this.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


    }



    public void setSession(final BoxSession session){
        mSession = session;
        mSearchApi = new BoxApiSearch(mSession);
        if (mSession != null){
            this.setEnabled(true);
            getSuggestionsAdapter().setFilterQueryProvider(new BoxSearchListAdapter.SearchFilterQueryProvider(mSession));
        }
    }


    private static final String EXTRA_ORIGINAL_PARCELABLE = "extraOriginalParcelable";
    private static final String EXTRA_USER_ID = "extraUserId";

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_ORIGINAL_PARCELABLE,parcelable);
        bundle.putString(EXTRA_USER_ID, mSession.getUserId());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle){
            setSession(new BoxSession(getContext(), ((Bundle)state).getString(EXTRA_USER_ID) ));
            super.onRestoreInstanceState(((Bundle) state).getParcelable(EXTRA_ORIGINAL_PARCELABLE));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    public class SearchItemCursor extends MatrixCursor {
        SearchItemCursor(String[] columnNames){
            super(columnNames);
        }
    }

    public static interface BoxSearchListener extends BoxSearchListAdapter.BoxSearchListener {

        /**
         * This is called if a user clicks on the search icon, hits enter/search button on keyboard, or clicks on the more results item at the bottom of the suggestions.
         * @param searchRequest The request that this search was desired for.
         * @param searchTask The task that was generated from the request that may be in progress or completed.
         */
        public void onMoreResultsRequested(BoxRequestsSearch.Search searchRequest, final BoxFutureTask<BoxListItems> searchTask);

    }

}
