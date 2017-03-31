package org.cgsdream.demo_android.qmui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.Scroller;

import org.cgsdream.demo_android.R;

/**
 * @author cginechen
 * @date 2016-12-11
 */

public class QMUIPullRefreshLayout extends ViewGroup implements NestedScrollingParent {
    private static final String TAG = "QMUIPullRefreshLayout";
    private static final int INVALID_POINTER = -1;

    private View mTargetView;
    private IRefreshView mIRefreshView;
    private View mRefreshView;
    private int mRefreshZIndex = -1;
    private int mTouchSlop;
    private boolean mIsRefreshing = false;
    private OnPullListener mListener;
    private OnChildScrollUpCallback mChildScrollUpCallback;

    private int mRefreshInitOffset;
    private int mRefreshCurrentOffset;
    private int mRefreshEndOffset;

    private int mTargetInitOffset;
    private int mTargetCurrentOffset;
    private int mTargetRefreshOffset;
    private boolean mAutoTargetRefreshOffset = true;
    private boolean mEnableOverPull = true;

    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private boolean mNestedScrollInProgress;
    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsDragging;
    private float mInitialDownY;
    private float mInitialMotionY;
    private float mLastMotionY;

    private VelocityTracker mVelocityTracker;
    private float mMaxVelocity;

    private Scroller mScroller;
    private boolean mNeedScrollToInitPos = false;
    private boolean mNeedScrollToRefreshPos = false;
    private boolean mNeedDecideDoRefreshOrNot = false;
    private boolean mNeedScrollToInitOrRefreshPosDependOnCurrentOffset = false;

    private float mDragRate = 0.65f;


    public QMUIPullRefreshLayout(Context context) {
        this(context, null);
    }

    public QMUIPullRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.QMUIPullRefreshLayoutStyle);
    }

    public QMUIPullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);

        final ViewConfiguration vc = ViewConfiguration.get(getContext());
        mMaxVelocity = vc.getScaledMaximumFlingVelocity();
        mTouchSlop = QMUIDisplayHelper.px2dp(context, vc.getScaledTouchSlop()); //系统的值是8dp,如何配置？

        mScroller = new Scroller(getContext());
        mScroller.setFriction(0.98f);

        addRefreshView();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

        TypedArray array = context.obtainStyledAttributes(attrs,
                R.styleable.QMUIPullRefreshLayout, defStyleAttr, 0);

        try {
            mRefreshInitOffset = array.getDimensionPixelSize(
                    R.styleable.QMUIPullRefreshLayout_qmui_refresh_init_offset, Integer.MIN_VALUE);
            mRefreshEndOffset = array.getDimensionPixelSize(
                    R.styleable.QMUIPullRefreshLayout_qmui_refresh_end_offset, QMUIDisplayHelper.dp2px(getContext(), 20)
            );
            mTargetInitOffset = array.getDimensionPixelSize(
                    R.styleable.QMUIPullRefreshLayout_qmui_target_init_offset, 0
            );
            mTargetRefreshOffset = array.getDimensionPixelSize(
                    R.styleable.QMUIPullRefreshLayout_qmui_target_refresh_offset, Integer.MIN_VALUE
            );
            mAutoTargetRefreshOffset = array.getBoolean(
                    R.styleable.QMUIPullRefreshLayout_qmui_auto_target_refresh_offset, true
            );
            if (!mAutoTargetRefreshOffset && mTargetRefreshOffset == Integer.MIN_VALUE) {
                mAutoTargetRefreshOffset = true;
            }

        } finally {
            array.recycle();
        }
        mRefreshCurrentOffset = mRefreshInitOffset;
        mTargetCurrentOffset = mTargetInitOffset;
    }

    public void setOnPullListener(OnPullListener listener) {
        mListener = listener;
    }

    public void setChildScrollUpCallback(OnChildScrollUpCallback childScrollUpCallback) {
        mChildScrollUpCallback = childScrollUpCallback;
    }

    protected View createRefreshView() {
        return new RefreshView(getContext());
    }

    private void addRefreshView() {
        if (mRefreshView == null) {
            mRefreshView = createRefreshView();
        }
        if (!(mRefreshView instanceof IRefreshView)) {
            throw new RuntimeException("refreshView must be a view");
        }
        mIRefreshView = (IRefreshView) mRefreshView;
        if (mRefreshView.getLayoutParams() == null) {
            mRefreshView.setLayoutParams(new LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }
        addView(mRefreshView);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mRefreshZIndex < 0) {
            return i;
        }
        // 最后才绘制mRefreshView
        if (i == mRefreshZIndex) {
            return childCount - 1;
        }
        if (i > mRefreshZIndex) {
            return i - 1;
        }
        return i;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTargetView instanceof AbsListView)
                || (mTargetView != null && !ViewCompat.isNestedScrollingEnabled(mTargetView))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ensureTargetView();
        if (mTargetView == null) {
            Log.d(TAG, "onMeasure: mTargetView == null");
            return;
        }
        int targetMeasureWidthSpec = MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY);
        int targetMeasureHeightSpec = MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY);
        mTargetView.measure(targetMeasureWidthSpec, targetMeasureHeightSpec);
        measureChild(mRefreshView, widthMeasureSpec, heightMeasureSpec);
        mRefreshZIndex = -1;
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) == mRefreshView) {
                mRefreshZIndex = i;
                break;
            }
        }
        if (mRefreshInitOffset == Integer.MIN_VALUE) {
            mRefreshInitOffset = -mRefreshView.getMeasuredHeight();
            mRefreshCurrentOffset = 0;
        }
        if (mAutoTargetRefreshOffset) {
            mTargetRefreshOffset = mRefreshEndOffset * 2 + mRefreshView.getMeasuredHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        ensureTargetView();
        if (mTargetView == null) {
            Log.d(TAG, "onLayout: mTargetView == null");
            return;
        }

        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        mTargetView.layout(childLeft, childTop + mTargetCurrentOffset,
                childLeft + childWidth, childTop + childHeight + mTargetCurrentOffset);
        int refreshViewWidth = mRefreshView.getMeasuredWidth();
        int refreshViewHeight = mRefreshView.getMeasuredHeight();
        mRefreshView.layout((width / 2 - refreshViewWidth / 2), mRefreshCurrentOffset,
                (width / 2 + refreshViewWidth / 2), mRefreshCurrentOffset + refreshViewHeight);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTargetView();

        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex;

        if (!isEnabled() || canChildScrollUp() || mNestedScrollInProgress) {
            Log.d(TAG, "fast end onIntercept: isEnabled = " + isEnabled() + "; canChildScrollUp = "
                    + canChildScrollUp() + " ; mNestedScrollInProgress = " + mNestedScrollInProgress);
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsDragging = false;
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mInitialDownY = ev.getY(pointerIndex);
                break;

            case MotionEvent.ACTION_MOVE:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = ev.getY(pointerIndex);
                startDragging(y);
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDragging = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex;

        if (!isEnabled() || canChildScrollUp() || mNestedScrollInProgress) {
            Log.d(TAG, "fast end onTouchEvent: isEnabled = " + isEnabled() + "; canChildScrollUp = "
                    + canChildScrollUp() + " ; mNestedScrollInProgress = " + mNestedScrollInProgress);
            return false;
        }

        acquireVelocityTracker(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsDragging = false;
                resetTag();
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;

            case MotionEvent.ACTION_MOVE: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }
                final float y = ev.getY(pointerIndex);
                startDragging(y);

                if (mIsDragging) {
                    float dy = y - mLastMotionY;
                    if (dy >= 0) {
                        moveTargetView(dy, true);
                    } else {
                        if (mTargetCurrentOffset + dy <= mTargetInitOffset) {
                            moveTargetView(dy, true);
                            // 重新dispatch一次down事件，使得列表可以继续滚动
                            int oldAction = ev.getAction();
                            ev.setAction(MotionEvent.ACTION_DOWN);
                            dispatchTouchEvent(ev);
                            ev.setAction(oldAction);
                        } else {
                            moveTargetView(dy, true);
                        }
                    }
                    mLastMotionY = y;
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    Log.e(TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = ev.getPointerId(pointerIndex);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }

                if (mIsDragging) {
                    mIsDragging = false;
                    mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
                    final float vy = mVelocityTracker.getYVelocity(mActivePointerId);
                    finishPull((int) vy);
                }
                mActivePointerId = INVALID_POINTER;
                releaseVelocityTracker();
                return false;
            }
            case MotionEvent.ACTION_CANCEL:
                releaseVelocityTracker();
                return false;
        }

        return true;
    }


    private void ensureTargetView() {
        if (mTargetView == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);
                if (!view.equals(mRefreshView)) {
                    mTargetView = view;
                }
            }
        }
    }

    private void finishPull(int vy) {
        if (mTargetCurrentOffset >= mTargetRefreshOffset) {
            onRefresh();
            if (vy > 0) {
                mNeedScrollToRefreshPos = true;
                mScroller.fling(0, mTargetCurrentOffset, 0, vy,
                        0, 0, mTargetInitOffset, Integer.MAX_VALUE);
                invalidate();
            } else if (vy < 0) {
                mNeedScrollToInitOrRefreshPosDependOnCurrentOffset = true;
                mScroller.fling(0, mTargetCurrentOffset, 0, vy,
                        0, 0, mTargetInitOffset, Integer.MAX_VALUE);
                invalidate();
            } else {
                mNeedScrollToRefreshPos = true;
                invalidate();
            }
        } else {
            if (vy > 0) {
                mNeedDecideDoRefreshOrNot = true;
                mScroller.fling(0, mTargetCurrentOffset, 0, vy,
                        0, 0, mTargetInitOffset, Integer.MAX_VALUE);
                invalidate();
            } else if (vy < 0) {
                mNeedScrollToInitPos = true;
                mScroller.fling(0, mTargetCurrentOffset, 0, vy,
                        0, 0, mTargetInitOffset, Integer.MAX_VALUE);
                invalidate();
            } else {
                mNeedScrollToInitPos = true;
                invalidate();
            }
        }
    }


    protected void onRefresh() {
        if (mIsRefreshing) {
            return;
        }
        mIRefreshView.doRefresh();
        if (mListener != null) {
            mListener.onRefresh();
        }
    }

    public void doRefresh() {
        if (mRefreshCurrentOffset == mRefreshEndOffset) {
            onRefresh();
            return;
        }
        ensureTargetView();
        onRefresh();
        mNeedScrollToRefreshPos = true;
        invalidate();
    }

    public void finishRefresh() {
        mIsRefreshing = false;
        mIRefreshView.stop();
        mNeedScrollToRefreshPos = false;
        mNeedDecideDoRefreshOrNot = false;
        mNeedScrollToInitPos = true;
        invalidate();
    }


    public void setEnableOverPull(boolean enableOverPull) {
        mEnableOverPull = enableOverPull;
    }


    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private void reset() {
        moveTargetViewTo(mTargetInitOffset, false);
        mIRefreshView.stop();
        resetTag();
    }

    private void resetTag() {
        mNeedScrollToRefreshPos = false;
        mNeedDecideDoRefreshOrNot = false;
        mNeedScrollToInitPos = false;
        mNeedScrollToInitOrRefreshPosDependOnCurrentOffset = false;
    }

    private void startDragging(float y) {
        final float yDiff = y - mInitialDownY;
        if ((yDiff > mTouchSlop || (yDiff < -mTouchSlop && mTargetCurrentOffset > mTargetInitOffset)) && !mIsDragging) {
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mLastMotionY = mInitialMotionY;
            mIsDragging = true;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            reset();
            invalidate();
        }
    }

    public boolean canChildScrollUp() {
        if (mChildScrollUpCallback != null) {
            return mChildScrollUpCallback.canChildScrollUp(this, mTargetView);
        }
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTargetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTargetView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTargetView, -1) || mTargetView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTargetView, -1);
        }
    }


    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        Log.i(TAG, "onStartNestedScroll: nestedScrollAxes = " + nestedScrollAxes);
        return isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        Log.i(TAG, "onNestedScrollAccepted: axes = " + axes);
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        Log.i(TAG, "onNestedPreScroll: dx = " + dx + " ; dy = " + dy);
        int parentCanConsume = mTargetCurrentOffset - mTargetInitOffset;
        if (dy > 0 && parentCanConsume > 0) {
            if (dy >= parentCanConsume) {
                consumed[1] = parentCanConsume;
                moveTargetViewTo(mTargetInitOffset, true);
            } else {
                consumed[1] = dy;
                moveTargetView(-dy, true);
            }
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        Log.i(TAG, "onNestedScroll: dxConsumed = " + dxConsumed + " ; dyConsumed = " + dyConsumed +
                " ; dxUnconsumed = " + dxUnconsumed + " ; dyUnconsumed = " + dyUnconsumed);
        if (dyUnconsumed < 0 && !canChildScrollUp()) {
            moveTargetView(-dyUnconsumed, true);
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View child) {
        Log.i(TAG, "onStopNestedScroll");
        mNestedScrollingParentHelper.onStopNestedScroll(child);
        mNestedScrollInProgress = false;
        finishPull(0);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        Log.i(TAG, "onNestedPreFling: mTargetCurrentOffset = " + mTargetCurrentOffset +
                " ; velocityX = " + velocityX + " ; velocityY = " + velocityY);
        if (mTargetCurrentOffset > mTargetInitOffset) {
            return true;
        }
        return false;
    }

    private void moveTargetView(float dy, boolean isDragging) {
        int target = (int) (mTargetCurrentOffset + dy * mDragRate);
        moveTargetViewTo(target, isDragging);
    }

    private void moveTargetViewTo(int target, boolean isDragging) {
        target = Math.max(target, mTargetInitOffset);
        if (!mEnableOverPull) {
            target = Math.min(target, mTargetRefreshOffset);
        }
        if (target != mTargetCurrentOffset) {
            ViewCompat.offsetTopAndBottom(mTargetView, target - mTargetCurrentOffset);
            mTargetCurrentOffset = target;
            int total = mTargetRefreshOffset - mTargetInitOffset;
            if (isDragging) {
                mIRefreshView.onPull(Math.min(mTargetCurrentOffset - mTargetInitOffset, total), total,
                        mTargetCurrentOffset - mTargetRefreshOffset);
            }
            if (mListener != null) {
                mListener.onMoveTarget(mTargetCurrentOffset);
            }

            int refreshOffset;
            if (mTargetCurrentOffset >= mTargetRefreshOffset) {
                refreshOffset = mRefreshEndOffset;
            } else if (mTargetCurrentOffset <= mTargetInitOffset) {
                refreshOffset = mRefreshInitOffset;
            } else {
                float percent = (mTargetCurrentOffset - mTargetInitOffset) * 1.0f / mTargetRefreshOffset - mTargetInitOffset;
                refreshOffset = (int) (mRefreshInitOffset + percent * (mRefreshEndOffset - mRefreshInitOffset));
            }

            if (refreshOffset != mRefreshCurrentOffset) {
                ViewCompat.offsetTopAndBottom(mRefreshView, refreshOffset - mRefreshCurrentOffset);
                mRefreshCurrentOffset = refreshOffset;
                if (mListener != null) {
                    mListener.onMoveRefreshView(mTargetCurrentOffset);
                }
            }
        }

    }

    private void acquireVelocityTracker(final MotionEvent event) {
        if (null == mVelocityTracker) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    private void releaseVelocityTracker() {
        if (null != mVelocityTracker) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int offsetY = mScroller.getCurrY();
            moveTargetViewTo(offsetY, false);
            invalidate();
        } else if (mNeedScrollToInitPos) {
            mNeedScrollToInitPos = false;
            if (mTargetCurrentOffset == mTargetInitOffset) {
                if (mScroller.getCurrVelocity() > 0) {
                    // 如果还有速度，则传递给子view
                    if (mTargetView instanceof RecyclerView) {
                        ((RecyclerView) mTargetView).fling(0, (int) mScroller.getCurrVelocity());
                    } else if (mTargetView instanceof AbsListView && android.os.Build.VERSION.SDK_INT >= 21) {
                        ((AbsListView) mTargetView).fling((int) mScroller.getCurrVelocity());
                    }
                }
                return;
            }
            mScroller.startScroll(0, mTargetCurrentOffset, 0, mTargetInitOffset - mTargetCurrentOffset);
            invalidate();
        } else if (mNeedScrollToRefreshPos) {
            mNeedScrollToRefreshPos = false;
            mScroller.startScroll(0, mTargetCurrentOffset, 0, mTargetRefreshOffset - mTargetCurrentOffset);
            invalidate();
        } else if (mNeedDecideDoRefreshOrNot) {
            mNeedDecideDoRefreshOrNot = false;
            if (mTargetCurrentOffset >= mTargetRefreshOffset) {
                doRefresh();
                mNeedScrollToRefreshPos = true;
            } else {
                mNeedScrollToInitPos = true;
            }
            invalidate();
        } else if (mNeedScrollToInitOrRefreshPosDependOnCurrentOffset) {
            mNeedScrollToInitOrRefreshPosDependOnCurrentOffset = false;
            if (mTargetCurrentOffset >= mTargetRefreshOffset) {
                mNeedScrollToRefreshPos = true;
            } else {
                mNeedScrollToInitPos = true;
            }
            invalidate();
        }
    }


    public interface OnPullListener {
        void onMoveTarget(int offset);

        void onMoveRefreshView(int offset);

        void onRefresh();
    }


    public interface OnChildScrollUpCallback {
        boolean canChildScrollUp(QMUIPullRefreshLayout parent, @Nullable View child);
    }

    public static class RefreshView extends ImageView implements IRefreshView {
        private static final int MAX_ALPHA = 255;
        private static final float TRIM_RATE = 0.85f;
        private static final float TRIM_OFFSET = 0.4f;

        private QMUIMaterialProgressDrawable mProgress;

        public RefreshView(Context context) {
            super(context);
            mProgress = new QMUIMaterialProgressDrawable(getContext(), this);
            mProgress.setColorSchemeColors(0x00ff00);
            mProgress.updateSizes(QMUIMaterialProgressDrawable.LARGE);
            mProgress.setAlpha(MAX_ALPHA);
            mProgress.setArrowScale(1.1f);
            setImageDrawable(mProgress);
        }

        @Override
        public void onPull(int offset, int total, int overPull) {
            float end = TRIM_RATE * offset / total;
            float rotate = TRIM_OFFSET * offset / total;
            if (overPull > 0) {
                rotate += TRIM_OFFSET * overPull / total;
            }
            mProgress.showArrow(true);
            mProgress.setStartEndTrim(0, end);
            mProgress.setProgressRotation(rotate);
        }

        public void setSize(int size) {
            if (size != QMUIMaterialProgressDrawable.LARGE && size != QMUIMaterialProgressDrawable.DEFAULT) {
                return;
            }
            setImageDrawable(null);
            mProgress.updateSizes(size);
            setImageDrawable(mProgress);
        }

        public void stop() {
            mProgress.stop();
        }

        public void doRefresh() {
            if (!mProgress.isRunning()) {
                mProgress.start();
            }
        }

        public void setColorSchemeResources(@ColorRes int... colorResIds) {
            final Context context = getContext();
            int[] colorRes = new int[colorResIds.length];
            for (int i = 0; i < colorResIds.length; i++) {
                colorRes[i] = ContextCompat.getColor(context, colorResIds[i]);
            }
            setColorSchemeColors(colorRes);
        }

        public void setColorSchemeColors(@ColorInt int... colors) {
            mProgress.setColorSchemeColors(colors);
        }
    }

    public interface IRefreshView {
        void stop();

        void doRefresh();

        void setColorSchemeResources(@ColorRes int... colorResIds);

        void setColorSchemeColors(@ColorInt int... colors);

        void onPull(int offset, int total, int overPull);
    }
}