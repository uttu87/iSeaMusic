package com.iseasoft.iSeaMusic.youtubeapi;

import android.support.annotation.StringDef;

@StringDef({OrderBy.DATE, OrderBy.RATING, OrderBy.RELEVANCE, OrderBy.TITLE, OrderBy.VIEW_COUNT, OrderBy.VIDEO_COUNT})
public @interface OrderBy {
    String DATE = "date";
    String RATING = "rating";
    String RELEVANCE = "relevance";
    String TITLE = "title";
    String VIEW_COUNT = "viewCount";
    String VIDEO_COUNT = "videoCount";

}