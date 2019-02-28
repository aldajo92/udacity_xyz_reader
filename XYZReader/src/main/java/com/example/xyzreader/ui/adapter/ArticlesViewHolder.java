package com.example.xyzreader.ui.adapter;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.models.ArticleModel;
import com.example.xyzreader.ui.views.DynamicHeightNetworkImageView;
import com.squareup.picasso.Picasso;

public class ArticlesViewHolder extends RecyclerView.ViewHolder {

    private View view;
    private DynamicHeightNetworkImageView thumbnailView;
    private TextView titleView;
    private TextView subtitleView;
    private Context context;

    private ArticleModel articleModel;
    private ArticleClickListener articleClickListener;

    public ArticlesViewHolder(View view) {
        super(view);
        this.view = view;
        this.thumbnailView = view.findViewById(R.id.thumbnail);
        this.titleView = view.findViewById(R.id.article_title);
        this.subtitleView = view.findViewById(R.id.article_subtitle);
        this.context = view.getContext();
    }

    public void bind(ArticleModel articleModel){
        this.articleModel = articleModel;

        titleView.setText(articleModel.getTitle());
        subtitleView.setText(articleModel.getSubtitle());

        Picasso.get()
                .load(articleModel.getImageUrl())
                .error(R.drawable.photo_error)
                .into(thumbnailView);
        thumbnailView.setAspectRatio(articleModel.getAspectRatio());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            thumbnailView.setTransitionName(context.getString(R.string.transition_photo) + getAdapterPosition());
        }
        thumbnailView.setTag(context.getString(R.string.transition_photo) + getAdapterPosition());
    }

    public void setArticleClickListener(ArticleClickListener listener) {
        this.articleClickListener = listener;
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                articleClickListener.onClick(getAdapterPosition(), getTransitionName(), thumbnailView, articleModel.getItemId());
            }
        });
    }

    private String getTransitionName(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return thumbnailView.getTransitionName();
        } else {
            return null;
        }
    }


}