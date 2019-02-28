package com.example.xyzreader.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.models.ArticleModel;
import com.example.xyzreader.ui.activities.ArticleListActivity;
import com.example.xyzreader.ui.views.DynamicHeightNetworkImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class ArticlesAdapter extends RecyclerView.Adapter<ArticlesViewHolder> {

    private static final String TAG = ArticleListActivity.class.toString();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
    private SimpleDateFormat outputFormat = new SimpleDateFormat();

    private Cursor mCursor;

    private ArticleClickListener listener;

    public ArticlesAdapter(Cursor cursor, ArticleClickListener listener) {
        this.mCursor = cursor;
        this.listener = listener;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @NonNull
    @Override
    public ArticlesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_article, parent, false);
        ArticlesViewHolder articlesViewHolder = new ArticlesViewHolder(view);
        articlesViewHolder.setArticleClickListener(listener);
        return articlesViewHolder;
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
    public void onBindViewHolder(@NonNull ArticlesViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        String title = mCursor.getString(ArticleLoader.Query.TITLE);
        Date publishedDate = parsePublishedDate();
        Spanned subtitle = (!publishedDate.before(START_OF_EPOCH.getTime())) ?
            Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR))
        :
            Html.fromHtml(
                    outputFormat.format(publishedDate)
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));

        String imageUrl = mCursor.getString(ArticleLoader.Query.THUMB_URL);
        float aspectRatio = mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO);

        holder.bind(new ArticleModel(title, subtitle, imageUrl, aspectRatio, getItemId(position)));
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }
}