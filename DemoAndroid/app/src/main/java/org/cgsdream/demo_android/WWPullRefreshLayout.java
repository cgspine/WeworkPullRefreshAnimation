package org.cgsdream.demo_android;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import org.cgsdream.demo_android.qmui.QMUIPullRefreshLayout;

/**
 * @author cginechen
 * @date 2016-12-11
 */

public class WWPullRefreshLayout extends QMUIPullRefreshLayout {

    public WWPullRefreshLayout(Context context) {
        super(context);
    }

    public WWPullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View createRefreshView() {
        return new WWLoadingView(getContext(), getResources().getDimensionPixelSize(R.dimen.loading_size));
    }
}
