package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.pagetransformer.CubeOutPageTransformer;
import com.example.xyzreader.pagetransformer.DepthPageTransformer;
import com.example.xyzreader.pagetransformer.PopPageTransformer;
import com.example.xyzreader.pagetransformer.ZoomOutPageTransformer;

import java.util.List;
import java.util.Map;

import static com.example.xyzreader.Constants.ANIM_DURATION;
import static com.example.xyzreader.Constants.CUBE;
import static com.example.xyzreader.Constants.DEPTH;
import static com.example.xyzreader.Constants.EXTRA_PAGE_TRANSFORMATION;
import static com.example.xyzreader.Constants.EXTRA_STARTING_POSITION;
import static com.example.xyzreader.Constants.POP;
import static com.example.xyzreader.Constants.STATE_CURRENT_PAGE_POSITION;
import static com.example.xyzreader.Constants.ZOOM;

public class ArticleDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private View mUpButtonContainer;
    private View mUpButton;

    private int currentPosition;
    private int startingPosition;
    private boolean isReturning;
    private ArticleDetailFragment currentDetailFragment;
    private ViewPager.PageTransformer pageTransformer;
    private String pageTransformerStr;

    private final SharedElementCallback sharedElementCallback = new SharedElementCallback() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (isReturning) {
                ImageView sharedElement = currentDetailFragment.getPhotoView();
                if (sharedElement == null) {
                    names.clear();
                    sharedElements.clear();
                } else if (startingPosition != currentPosition) {
                    names.clear();
                    names.add(sharedElement.getTransitionName());
                    sharedElements.clear();
                    sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            ActivityCompat.postponeEnterTransition(this);
            setEnterSharedElementCallback(sharedElementCallback);
        }
        setContentView(R.layout.activity_article_detail);

        startingPosition = getIntent().getIntExtra(EXTRA_STARTING_POSITION, 0);
        if (savedInstanceState == null) {
            currentPosition = startingPosition;
        } else {
            currentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE_POSITION);
        }

        pageTransformerStr = getIntent().getStringExtra(EXTRA_PAGE_TRANSFORMATION);

//        getSupportLoaderManager().initLoader(XYZ_LOADER_ID, null, this);

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));


        mPager.setCurrentItem(currentPosition);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(ANIM_DURATION);
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                updateUpButtonPosition();

                currentPosition = position;
            }
        });

        mUpButtonContainer = findViewById(R.id.up_container);

        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reverses the Activity Scene entry Transition and triggers the calling Activity
                // to reverse its exit Transition.
                // References: @see "https://stackoverflow.com/questions/37713793/shared-element-transition-when-using-actionbar-back-button"
                // @see "https://discussions.udacity.com/t/transition-work-when-exiting-but-not-entering/207227/4"
                supportFinishAfterTransition();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    view.onApplyWindowInsets(windowInsets);
                    mTopInset = windowInsets.getSystemWindowInsetTop();
                    mUpButtonContainer.setTranslationY(mTopInset);
                    updateUpButtonPosition();
                    return windowInsets;
                }
            });
        }

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        mPager.setCurrentItem(currentPosition, false);
        mCursor.moveToPosition(currentPosition);

        mPager.setPageTransformer(true, getPageTransformer());
    }

    public ViewPager.PageTransformer getPageTransformer() {
        if (!TextUtils.isEmpty(pageTransformerStr)) {
            switch (pageTransformerStr) {
                case POP:
                    pageTransformer = new PopPageTransformer();
                    break;
                case ZOOM:
                    pageTransformer = new ZoomOutPageTransformer();
                    break;
                case DEPTH:
                    pageTransformer = new DepthPageTransformer();
                    break;
                case CUBE:
                    pageTransformer = new CubeOutPageTransformer();
                    break;
                default:
                    pageTransformer = new PopPageTransformer();
            }
        }
        return pageTransformer;
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
        }
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;
            if (fragment != null) {
                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
                updateUpButtonPosition();
            }
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
