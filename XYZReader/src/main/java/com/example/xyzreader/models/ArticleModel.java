package com.example.xyzreader.models;

import android.text.Spanned;
import android.text.SpannedString;

public class ArticleModel {

    private String title;

    private Spanned subtitle;

    private String imageUrl;

    private float aspectRatio;

    private long itemId;

    public ArticleModel() {
        this.title = "";
        this.subtitle = new SpannedString("");
        this.imageUrl = "";
        this.aspectRatio = 0f;
    }

    public ArticleModel(String title, Spanned subtitle, String imageUrl, float aspectRatio, long itemId) {
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.aspectRatio = aspectRatio;
        this.itemId = itemId;
    }

    public String getTitle() {
        return title;
    }

    public Spanned getSubtitle() {
        return subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public long getItemId() {
        return itemId;
    }
}
