package com.zhh.mymap.indoorview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

/**
 * 楼层条View
 */
public class StripListView extends ListView {
    public StripListView(Context context) {
        super(context);
        initView(context);
    }

    public StripListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public StripListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }


    public void setStripAdapter(BaseAdapter adapter) {
        super.setAdapter(adapter);
        //调整显示的楼层数，最多显示5个
        if (adapter.getCount() < 6)
        {
            View item = adapter.getView(0, null, this);
            item.measure(0, 0);
            layoutParam.height = adapter.getCount() * item.getMeasuredHeight();
        }
        else
        {
            View item = adapter.getView(0, null, this);
            item.measure(0, 0);
            layoutParam.height = (int) (5.5 * item.getMeasuredHeight());
        }
        requestLayout();
    }

    CoordinatorLayout.LayoutParams layoutParam;

    private void initView(Context context) {
        setId(0);
        setBackgroundColor(Color.parseColor("#FFFFFFFF"));
        setVisibility(View.GONE);
        setDividerHeight(0);
        setVerticalScrollBarEnabled(false);
        setScrollingCacheEnabled(false);
        setCacheColorHint(Color.TRANSPARENT);
        setCacheColorHint(0);
        layoutParam = new CoordinatorLayout.LayoutParams(120, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
        layoutParam.setMargins(StripItem.dip2px(context, 20), StripItem.dip2px(context, 180), 0, 0);
        setLayoutParams(layoutParam);
    }
}
