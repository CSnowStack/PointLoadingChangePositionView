package csnowstack.pointloadingchangepositionview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 *
 */

public class PointLoadingChangePositionView extends View {
    /**
     * 向上或向下滑动
     */
    private static final int STATE_DRAG = 1;
    /**
     * 加载中
     */
    private static final int STATE_LOADING = 1 << 1;

    /**
     * 当前状态
     */
    private int mStateCurrent = STATE_LOADING;

    private Paint mPaint;

    /**
     * 球的个数
     */
    private int mCircleCount = 6;
    /**
     * 半径
     */
    private int mCircleRadius = 15;
    /**
     * 居中需要的位移
     */
    private int[] mCenterTranslation;
    /**
     * 宽,高,中间的y点
     */
    private int mWidth, mHeight, mCenterY;

    /**
     * 球的颜色
     */
    private int mPointColor = Color.BLACK;
    /**
     * 运动的比例,value,线的长度
     */
    private float mFraction = 0, mAnimationValue, mPathLength;

    /**
     * 每个小圆需要消耗的比例
     */
    private float mOneFullFraction;


    private ValueAnimator mAnimator;
    /**
     * 是否是向上弹出小球
     */
    private boolean mThroughAbove = true;
    /**
     * 小球运动的pathMeasure
     */
    private PathMeasure mPathMeasure;
    private Path mPath;//bezier
    /**
     * 当前position
     */
    private float[] mPos = new float[2];// set position
    /**
     * 运动时间
     */
    private static final long sDuration = 700;

    /**
     * 缓冲的bitmap ,中间的 mCircleCount-1 个小球
     */
    private Bitmap mBufferBitmap;
    private Canvas mBufferCanvas;


    public PointLoadingChangePositionView(Context context) {
        this(context,null);
    }

    public PointLoadingChangePositionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mPointColor);
        mPaint.setAntiAlias(true);
        //bezier
        mPath = new Path();
        mPathMeasure = new PathMeasure();
        mCenterTranslation = new int[2];


        mAnimator = ValueAnimator.ofFloat(1, 0);
        mAnimator.setDuration(sDuration);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimationValue = (float) animation.getAnimatedValue();
                mFraction = animation.getAnimatedFraction();
                invalidate();
            }
        });

        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                super.onAnimationRepeat(animation);
                mThroughAbove = !mThroughAbove;
            }
        });
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);

    }


    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = (3 * mCircleCount + 1) * mCircleRadius;//gap == radius
        mHeight = mCircleRadius * 2 * 10;//5 circle height

        mCenterTranslation[0] = w / 2 - mWidth / 2;
        mCenterTranslation[1] = h / 2 - mHeight / 2;

        mCenterY = mHeight / 2;
        //小球运动的路径
        mPath.moveTo(mCircleRadius * 2, mCenterY);
        mPath.cubicTo(mCircleRadius * 2, mCenterY, mWidth / 2, 0, mWidth - mCircleRadius * 2, mCenterY);

        mPathMeasure.setPath(mPath, false);
        mPathLength = mPathMeasure.getLength();

        mOneFullFraction = 1f / mCircleCount;

        mBufferBitmap = Bitmap.createBitmap(mWidth - 4 * mCircleRadius, h, Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(mBufferBitmap);
        //先画出来缓存的小圆
        drawCenterCircle();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (mStateCurrent) {
            case STATE_DRAG:
                drawDrag(canvas);
                break;
            case STATE_LOADING:
                drawLoading(canvas);
                break;
        }


    }

    /**
     * 按比例绘制向下的动画
     */
    private void drawDrag(Canvas canvas) {
        //每个运动到中间的小球所需要的比例
        canvas.translate(mCenterTranslation[0],mCenterTranslation[1]);
        float fraction = 0;
        for (int i = 0, n = mCircleCount; i < n; i++) {

            fraction = (mFraction - i * mOneFullFraction) / mOneFullFraction;
            if(fraction>1){
                fraction=1;
            }
            canvas.drawCircle((3 * i + 2) * mCircleRadius, mCenterY * fraction, mCircleRadius, mPaint);
        }

    }


    /**
     * 绘制loading
     */
    private void drawLoading(Canvas canvas) {
        canvas.translate(mCenterTranslation[0], mCenterTranslation[1]);

        //5个小圆应该在的位置
        canvas.drawBitmap(mBufferBitmap, 3 * mCircleRadius * mAnimationValue, 0, mPaint);//在双缓冲中绘图，将自定义缓冲绘制到屏幕上

        mPathMeasure.getPosTan(mPathLength * mFraction, mPos, null);
        canvas.drawCircle(mPos[0], mThroughAbove ? mPos[1] : mHeight - mPos[1], mCircleRadius, mPaint);//go left
    }

    /**
     * 绘制中间的mCircleCount-1个小球
     */
    private void drawCenterCircle() {
        for (int i = 0; i < mCircleCount - 1; i++) {
            mBufferCanvas.drawCircle((3 * i + 2) * mCircleRadius, mCenterY, mCircleRadius, mPaint);
        }
    }

    public void startLoading() {
        if (!mAnimator.isRunning())
            mAnimator.start();
    }

    public void stopLoading() {
        if (mAnimator.isRunning())
            mAnimator.cancel();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startLoading();
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopLoading();
    }

    /**
     * 设置进度
     */
    public void setFraction(float fraction) {
        mFraction = fraction;
    }
}
