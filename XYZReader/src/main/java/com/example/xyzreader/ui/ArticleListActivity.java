package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static com.example.xyzreader.Constants.CUBE;
import static com.example.xyzreader.Constants.DEPTH;
import static com.example.xyzreader.Constants.EXTRA_CURRENT_POSITION;
import static com.example.xyzreader.Constants.EXTRA_LARGE;
import static com.example.xyzreader.Constants.EXTRA_STARTING_POSITION;
import static com.example.xyzreader.Constants.LARGE;
import static com.example.xyzreader.Constants.MEDIUM;
import static com.example.xyzreader.Constants.POP;
import static com.example.xyzreader.Constants.SMALL;
import static com.example.xyzreader.Constants.XYZ_LOADER_ID;
import static com.example.xyzreader.Constants.ZOOM;

public class ArticleListActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = ArticleListActivity.class.toString();

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private CoordinatorLayout coordinatorLayout;

    private String pageTransformerStr;
    private String textSizeStr;

    private Bundle reenterState;

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (reenterState != null) {
                // Get the starting position and the current position
                int startingPosition = reenterState.getInt(EXTRA_STARTING_POSITION);
                int currentPosition = reenterState.getInt(EXTRA_CURRENT_POSITION);
                if (startingPosition != currentPosition) {
                    String newTransitionName = getString(R.string.transition_photo) + currentPosition;
                    View newSharedElement = recyclerView.findViewWithTag(newTransitionName);
                    if (newSharedElement != null) {
                        names.clear();
                        names.add(newTransitionName);
                        sharedElements.clear();
                        sharedElements.put(newTransitionName, newSharedElement);
                    }
                }

                reenterState = null;
            } else {
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                }
                if (statusBar != null) {
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setExitSharedElementCallback(mCallback);
        }

//        coordinatorLayout = findViewById(R.id.layout_coordinator);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setSwipeRefreshLayout();

        recyclerView = findViewById(R.id.recycler_view);
        LoaderManager.getInstance(this).initLoader(XYZ_LOADER_ID, null, this);

        if (savedInstanceState == null) {
            refresh();
        }

        pageTransformerStr = getPreferredPageTransformationStr();
        textSizeStr = getPreferredTextSizeStr();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private String getPreferredPageTransformationStr() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String keyForPageAnimation = getString(R.string.pref_page_animation_key);
        String defaultPageAnimation = getString(R.string.pref_page_animation_default);
        return prefs.getString(keyForPageAnimation, defaultPageAnimation);
    }

    private String getPreferredTextSizeStr() {
        // Get all of the values from shared preferences to set it up
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // String for the key
        String keyForTextSize = getString(R.string.pref_text_size_key);
        // String for the default value
        String defaultTextSize = getString(R.string.pref_text_size_default);
        return prefs.getString(keyForTextSize, defaultTextSize);
    }

    private void setSwipeRefreshLayout() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.color_swipe_deep_purple),
                getResources().getColor(R.color.color_swipe_red));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
                runLayoutAnimation(recyclerView);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void runLayoutAnimation(RecyclerView recyclerView) {
        Context context = recyclerView.getContext();
        LayoutAnimationController controller =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_from_bottom);
        recyclerView.setLayoutAnimation(controller);
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        swipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        recyclerView.setAdapter(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_page_animation_key))) {
            String pageAnimation = sharedPreferences
                    .getString(key, getString(R.string.pref_page_animation_default));
            switch (pageAnimation) {
                case POP:
                    pageTransformerStr = getString(R.string.pref_page_animation_pop);
                    break;
                case ZOOM:
                    pageTransformerStr = getString(R.string.pref_page_animation_zoom);
                    break;
                case DEPTH:
                    pageTransformerStr = getString(R.string.pref_page_animation_depth);
                    break;
                case CUBE:
                    pageTransformerStr = getString(R.string.pref_page_animation_cube);
                    break;
                default:
                    pageTransformerStr = getString(R.string.pref_page_animation_pop);
            }

        } else if (key.equals(getString(R.string.pref_text_size_key))) {
            String textSize = sharedPreferences
                    .getString(key, getString(R.string.pref_text_size_default));
            switch (textSize) {
                case SMALL:
                    textSizeStr = getString(R.string.pref_text_size_small);
                    break;
                case MEDIUM:
                    textSizeStr = getString(R.string.pref_text_size_medium);
                    break;
                case LARGE:
                    textSizeStr = getString(R.string.pref_text_size_large);
                    break;
                case EXTRA_LARGE:
                    textSizeStr = getString(R.string.pref_text_size_extra_large);
                    break;
                default:
                    textSizeStr = getString(R.string.pref_text_size_medium);
            }
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                }
            });
            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }
}
