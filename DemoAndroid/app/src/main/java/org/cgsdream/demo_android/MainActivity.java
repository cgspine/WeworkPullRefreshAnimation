package org.cgsdream.demo_android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private WWPullRefreshLayout mPullRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPullRefreshLayout = (WWPullRefreshLayout) findViewById(R.id.pull_to_refresh);
        mPullRefreshLayout.setOnPullListener(new WWPullRefreshLayout.OnPullListener() {
            @Override
            public void onMoveTarget(int offset) {

            }

            @Override
            public void onMoveRefreshView(int offset) {

            }

            @Override
            public void onRefresh() {
                Log.w(TAG, "onPullDownToRefresh start");
                final WeakReference<WWPullRefreshLayout> pullToRefreshLayout = new WeakReference<>(mPullRefreshLayout);
                if (pullToRefreshLayout.get() != null) {
                    pullToRefreshLayout.get().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            pullToRefreshLayout.get().finishRefresh();
                        }
                    }, 3000);
                }
            }
        });
    }
}
