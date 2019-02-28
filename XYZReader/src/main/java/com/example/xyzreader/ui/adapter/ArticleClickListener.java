package com.example.xyzreader.ui.adapter;

import com.example.xyzreader.ui.views.DynamicHeightNetworkImageView;

public interface ArticleClickListener {

    void onClick(int position, String transitionName, DynamicHeightNetworkImageView imageView, long itemId);

}
