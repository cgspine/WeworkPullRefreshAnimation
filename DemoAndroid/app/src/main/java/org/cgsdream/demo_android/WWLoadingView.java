package org.cgsdream.demo_android;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.cgsdream.demo_android.qmui.QMUIDisplayHelper;
import org.cgsdream.demo_android.qmui.QMUIPullRefreshLayout;

/**
 * Created by cgine on 17/1/18.
 */

public class WWLoadingView extends View implements QMUIPullRefreshLayout.IRefreshView {
    private static final int DURATION = 600;
    private Ball[] mBalls = new Ball[4];
    private int mSize;
    private ValueAnimator mAnimator;
    private float mOriginInset;
    private float mCurrentPercent = 0;

    public WWLoadingView(Context context, int size) {
        super(context);
        mSize = size;
        init(context);
    }

    public WWLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.WWLoadingView);
        mSize = array.getDimensionPixelSize(R.styleable.WWLoadingView_loading_size, 0);
        array.recycle();

        init(context);
    }

    private void init(Context context) {
        mOriginInset = QMUIDisplayHelper.getDensity(context) * 3.5f;
        float ballRadius = mOriginInset;
        float ballSmallRadius = QMUIDisplayHelper.getDensity(context) * 1.5f;

        OriginPoint op1 = new OriginPoint(mSize / 2, mOriginInset);
        OriginPoint op2 = new OriginPoint(mOriginInset, mSize / 2);
        OriginPoint op3 = new OriginPoint(mSize / 2, mSize - mOriginInset);
        OriginPoint op4 = new OriginPoint(mSize - mOriginInset, mSize / 2);
        op1.setNext(op2);
        op2.setNext(op3);
        op3.setNext(op4);
        op4.setNext(op1);

        mBalls[0] = new Ball(ballRadius, ballSmallRadius, 0xFF0082EF, op1);
        mBalls[1] = new Ball(ballRadius, ballSmallRadius, 0xFF2DBC00, op2);
        mBalls[2] = new Ball(ballRadius, ballSmallRadius, 0xFFFFCC00, op3);
        mBalls[3] = new Ball(ballRadius, ballSmallRadius, 0xFFFB6500, op4);
    }

    private void startAnim() {
        stopAnim();
        mAnimator = ValueAnimator.ofFloat(0, 1);
        mAnimator.setDuration(DURATION);
        mAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimator.setCurrentPlayTime((long) (DURATION * mCurrentPercent));
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentPercent = (Float) animation.getAnimatedValue();
                for (int i = 0; i < mBalls.length; i++) {
                    Ball ball = mBalls[i];
                    ball.calculate(mCurrentPercent);
                }
                invalidate();
            }
        });
        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                setToNextPosition();
            }
        });
        mAnimator.start();
    }

    private void stopAnim() {
        if (mAnimator != null) {
            mAnimator.removeAllUpdateListeners();
            if (Build.VERSION.SDK_INT >= 19) {
                mAnimator.pause();
            }
            mAnimator.end();
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    private void setToNextPosition() {
        for (int i = 0; i < mBalls.length; i++) {
            Ball ball = mBalls[i];
            ball.next();
        }
    }

    @Override
    public void doRefresh() {
        startAnim();
    }

    @Override
    public void setColorSchemeResources(@ColorRes int... colorResIds) {

    }

    @Override
    public void setColorSchemeColors(@ColorInt int... colors) {

    }

    @Override
    public void stop() {
        stopAnim();
    }

    @Override
    public void onPull(int offset, int total, int overPull) {
        if (mAnimator != null && mAnimator.isRunning()) {
            return;
        }
        int dis = offset;
        if (overPull > 0) {
            dis = total + overPull;
        }
        float useDis = dis * 0.3f;
        mCurrentPercent = useDis / total;
        for (int i = 0; i < mBalls.length; i++) {
            Ball ball = mBalls[i];
            ball.calculate(mCurrentPercent);
        }
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnim();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < mBalls.length; i++) {
            mBalls[i].draw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mSize, mSize);
    }

    private static class OriginPoint {
        private float mX;
        private float mY;
        private OriginPoint mNext;

        public OriginPoint(float x, float y) {
            mX = x;
            mY = y;
        }

        public void setNext(OriginPoint next) {
            mNext = next;
        }

        public OriginPoint getNext() {
            return mNext;
        }

        public float getX() {
            return mX;
        }

        public float getY() {
            return mY;
        }
    }

    private static class Ball {
        private float mRadius;
        private float mX;
        private float mY;
        private float mSmallRadius;
        private float mSmallX;
        private float mSmallY;
        private Path mPath;
        private Paint mPaint;
        private OriginPoint mOriginPoint;

        public Ball(float radius, float smallRadius, @ColorInt int color, OriginPoint op) {
            mRadius = radius;
            mSmallRadius = smallRadius;
            mX = mSmallX = op.getX();
            mY = mSmallY = op.getY();
            mPaint = new Paint();
            mPaint.setColor(color);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAntiAlias(true);
            mPath = new Path();
            mOriginPoint = op;
        }


        public void calculate(float percent) {
            if (percent > 1f) {
                percent = 1f;
            }
            float v = 1.3f;
            float smallChangePoint = 0.5f, smallV1 = 0.3f;
            float smallV2 = (1 - smallChangePoint * smallV1) / (1 - smallChangePoint);
            float ev = Math.min(1f, v * percent);
            float smallEv;
            if (percent > smallChangePoint) {
                smallEv = smallV2 * (percent - smallChangePoint) + smallChangePoint * smallV1;
            } else {
                smallEv = smallV1 * percent;
            }


            float startX = mOriginPoint.getX();
            float startY = mOriginPoint.getY();
            OriginPoint next = mOriginPoint.getNext();
            float endX = next.getX();
            float endY = next.getY();
            float f = (endY - startY) * 1f / (endX - startX);

            mX = (int) (startX + (endX - startX) * ev);
            mY = (int) (f * (mX - startX) + startY);
            mSmallX = (int) (startX + (endX - startX) * smallEv);
            mSmallY = (int) (f * (mSmallX - startX) + startY);
        }

        public void next() {
            mOriginPoint = mOriginPoint.getNext();
        }

        public void draw(Canvas canvas) {
            canvas.drawCircle(mX, mY, mRadius, mPaint);
            canvas.drawCircle(mSmallX, mSmallY, mSmallRadius, mPaint);
            if (mSmallX == mX && mSmallY == mY) {
                return;
            }

            /* 三角函数求四个点 */
            float angle;
            float x1, y1, smallX1, smallY1, x2, y2, smallX2, smallY2;
            if (mSmallX == mX) {
                double v = (mRadius - mSmallRadius) / (mY - mSmallY);
                if (v > 1 || v < -1) {
                    return;
                }
                angle = (float) Math.asin(v);
                float sin = (float) Math.sin(angle);
                float cos = (float) Math.cos(angle);
                x1 = mX - mRadius * cos;
                y1 = mY - mRadius * sin;
                x2 = mX + mRadius * cos;
                y2 = y1;
                smallX1 = mSmallX - mSmallRadius * cos;
                smallY1 = mSmallY - mSmallRadius * sin;
                smallX2 = mSmallX + mSmallRadius * cos;
                smallY2 = smallY1;
            } else if (mSmallY == mY) {
                double v = (mRadius - mSmallRadius) / (mX - mSmallX);
                if (v > 1 || v < -1) {
                    return;
                }
                angle = (float) Math.asin(v);
                float sin = (float) Math.sin(angle);
                float cos = (float) Math.cos(angle);
                x1 = mX - mRadius * sin;
                y1 = mY + mRadius * cos;
                x2 = x1;
                y2 = mY - mRadius * cos;
                smallX1 = mSmallX - mSmallRadius * sin;
                smallY1 = mSmallY + mSmallRadius * cos;
                smallX2 = smallX1;
                smallY2 = mSmallY - mSmallRadius * cos;
            } else {
                double ab = Math.sqrt(Math.pow(mY - mSmallY, 2) + Math.pow(mX - mSmallX, 2));
                double v = (mRadius - mSmallRadius) / ab;
                if (v > 1 || v < -1) {
                    return;
                }
                double alpha = Math.asin(v);
                double b = Math.atan((mSmallY - mY) / (mSmallX - mX));
                angle = (float) (Math.PI / 2 - alpha - b);
                float sin = (float) Math.sin(angle);
                float cos = (float) Math.cos(angle);
                smallX1 = mSmallX - mSmallRadius * cos;
                smallY1 = mSmallY + mSmallRadius * sin;
                x1 = mX - mRadius * cos;
                y1 = mY + mRadius * sin;

                angle = (float) (b - alpha);
                sin = (float) Math.sin(angle);
                cos = (float) Math.cos(angle);
                smallX2 = mSmallX + mSmallRadius * sin;
                smallY2 = mSmallY - mSmallRadius * cos;
                x2 = mX + mRadius * sin;
                y2 = mY - mRadius * cos;

            }

            /* 控制点 */
            float centerX = (mX + mSmallX) / 2, centerY = (mY + mSmallY) / 2;
            float center1X = (x1 + smallX1) / 2, center1y = (y1 + smallY1) / 2;
            float center2X = (x2 + smallX2) / 2, center2y = (y2 + smallY2) / 2;
            float k1 = (center1y - centerY) / (center1X - centerX);
            float k2 = (center2y - centerY) / (center2X - centerX);
            float ctrlV = 0.08f;
            float anchor1X = center1X + (centerX - center1X) * ctrlV, anchor1Y = k1 * (anchor1X - center1X) + center1y;
            float anchor2X = center2X + (centerX - center2X) * ctrlV, anchor2Y = k2 * (anchor2X - center2X) + center2y;

            /* 画贝塞尔曲线 */
            mPath.reset();
            mPath.moveTo(x1, y1);
            mPath.quadTo(anchor1X, anchor1Y, smallX1, smallY1);
            mPath.lineTo(smallX2, smallY2);
            mPath.quadTo(anchor2X, anchor2Y, x2, y2);
            mPath.lineTo(x1, y1);
            canvas.drawPath(mPath, mPaint);
        }
    }
}
