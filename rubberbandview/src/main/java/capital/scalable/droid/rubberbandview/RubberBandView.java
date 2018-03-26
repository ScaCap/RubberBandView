package capital.scalable.droid.rubberbandview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

public class RubberBandView extends View {

    private static final String CURRENT_INDEX = "CurrentIndex";
    private static final String MAX_VALUE = "MaxValue";
    private static final String SUPER_STATE = "SuperState";

    private @Nullable RubberBandListener listener;

    private static final long VIBRATION_START_DELAY = 5000;
    private static final int NUMBER_REVERBS_IN_VIB = 3;
    private static final long VIBRATION_DURATION = 200;
    private static final Interpolator VIBRATION_INTERPOLATOR = new DecelerateInterpolator();
    private static final Interpolator POSITION_INTERPOLATOR = new OvershootInterpolator(1f);

    /**
     * Default value, in dp, of the width of rubber,
     * when it is stretched to its max
     */
    private static final int DEFAULT_MIN_RUBBER_WIDTH = 4;

    /**
     * Default value, in dp, of the width of rubber,
     * when it is at rest
     */
    private static final int DEFAULT_MAX_RUBBER_WIDTH = 5;

    /**
     * Default flatness ratio
     */
    private static final float DEFAULT_LOOSENESS_RATIO = 0.2f;

    /**
     * Default value, in dp, of the peak amplitude of each vibration
     */
    private static final int DEFAULT_VIBRATION_PEAK_AMPLITUDE = 3;

    /**
     * Duration of animation that sets the position of the rubberview
     */
    private static final long POSITION_DURATION = 500;

    /**
     * Delay before animating to current position
     */
    private static final long POSITION_START_DELAY = 150;

    /**
     * Max value to be selected.
     */
    private int maxValue = 0;

    /**
     * Width of rubber when it is stretched to its max
     */
    private int minRubberWidth;

    /**
     * Width of rubber when it is at rest
     */
    private int maxRubberWidth;

    /**
     * Ratio that determines how loose the rubber band will be.
     * Must be between [0, 0.5]
     */
    private float loosenessRatio;

    /**
     * Peak amplitude of each vibration
     */
    private int vibrationPeakAmplitude;

    /**
     * Current position of rubber band
     */
    private float currentPosition;

    /**
     * Path used to draw rubber band
     */
    private Path path;

    /**
     * Paint used to draw rubber band
     */
    private Paint paint;

    /**
     * Animator to set up rubber band initial position
     */
    private @Nullable ValueAnimator positionAnimator;

    /**
     * Animator to reverberate the rubber band
     */
    private @Nullable ValueAnimator vibrationAnimator;

    public RubberBandView(Context context) {
        super(context);
        initView(context, null);
    }

    public RubberBandView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    private void initView(Context context, @Nullable AttributeSet attrs) {
        int rubberColor = getThemeAccentColor(context);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        minRubberWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_MIN_RUBBER_WIDTH, metrics);
        maxRubberWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_MAX_RUBBER_WIDTH, metrics);
        vibrationPeakAmplitude = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_VIBRATION_PEAK_AMPLITUDE, metrics);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RubberBandView);
            rubberColor = typedArray.getColor(R.styleable.RubberBandView_rubberColor, rubberColor);
            minRubberWidth = typedArray.getDimensionPixelSize(R.styleable.RubberBandView_minRubberWidth, minRubberWidth);
            maxRubberWidth = typedArray.getDimensionPixelSize(R.styleable.RubberBandView_maxRubberWidth, maxRubberWidth);
            loosenessRatio = typedArray.getFloat(R.styleable.RubberBandView_loosenessRatio, DEFAULT_LOOSENESS_RATIO);
            vibrationPeakAmplitude = typedArray.getDimensionPixelSize(R.styleable.RubberBandView_vibrationPeakAmplitude, vibrationPeakAmplitude);
            typedArray.recycle();
        }

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(rubberColor);

        path = new Path();

        vibrationAnimator = ValueAnimator.ofFloat(-1f, 1f, -0.5f, 0.5f, 0);
        vibrationAnimator.setInterpolator(VIBRATION_INTERPOLATOR);
        vibrationAnimator.setRepeatCount(NUMBER_REVERBS_IN_VIB);
        vibrationAnimator.setDuration(VIBRATION_DURATION / NUMBER_REVERBS_IN_VIB);
        vibrationAnimator.setStartDelay(VIBRATION_START_DELAY);
        vibrationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (positionAnimator != null && positionAnimator.isRunning()) {
                    stopVibrationAnimation();
                } else {
                    invalidate();
                }
            }
        });
        vibrationAnimator.addListener(new AnimatorListenerAdapter() {

            private boolean isCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.isCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isCancelled) {
                    animation.setStartDelay(VIBRATION_START_DELAY);
                    animation.start();
                }
                isCancelled = false;
                invalidate();
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            setMaxSelection(bundle.getInt(MAX_VALUE));
            setSelection(bundle.getInt(CURRENT_INDEX));
            state = bundle.getParcelable(SUPER_STATE);
        }
        super.onRestoreInstanceState(state);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putInt(CURRENT_INDEX, fromPositionToIndex());
        bundle.putInt(MAX_VALUE, maxValue);
        bundle.putParcelable(SUPER_STATE, super.onSaveInstanceState());
        return bundle;
    }

    /**
     * Set the maximum value that could be selected
     * @param maxValue the maximum value
     */
    public void setMaxSelection(int maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Get the maximum value that could be selected
     * @return the maximum value
     */
    public int getMaxSelection() {
        return maxValue;
    }

    /**
     * Set the selected value
     * @param index Cannot be larger than the value returned by {@link #getMaxSelection()}
     */
    public void setSelection(int index) {
        if (maxValue > 0) {
            stopVibrationAnimation();
            if (positionAnimator != null && positionAnimator.isRunning()) {
                positionAnimator.cancel();
            }
            final int selectedIndex = Math.min(index, maxValue);
            if (getHeight() > 0) {
                animateSelection(selectedIndex);
            } else {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        animateSelection(selectedIndex);
                    }
                }, POSITION_START_DELAY);
            }
        }
    }

    private void animateSelection(int index) {
        positionAnimator = ValueAnimator.ofFloat(currentPosition, fromIndexToPosition(index));
        positionAnimator.setDuration(POSITION_DURATION);
        positionAnimator.setInterpolator(POSITION_INTERPOLATOR);
        positionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                invalidate();
            }
        });
        positionAnimator.start();
    }

    /**
     * Get the last selected value
     * @return selected value
     */
    public int getSelection() {
        return fromPositionToIndex();
    }

    /**
     * Set a {@link RubberBandListener} to receive updates to selection changes
     * @param listener listener that will receive updates
     */
    public void setListener(@Nullable RubberBandListener listener) {
        this.listener = listener;
    }

    /**
     * Set a {@link ValueAnimator} to animate the vibration at the selected value
     * @param vibrationAnimator set to null to turn off animations
     */
    public void setVibrationAnimator(@Nullable ValueAnimator vibrationAnimator) {
        this.vibrationAnimator = vibrationAnimator;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return super.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            stopVibrationAnimation();
            if (positionAnimator != null && positionAnimator.isStarted()) {
                positionAnimator.cancel();
            }
        }
        setCurrentPosition(event.getY());
        invalidate();
        updateListener(event.getAction());
        if (event.getAction() == MotionEvent.ACTION_UP) {
            startVibrationAnimation();
        }
        return true;
    }

    /**
     * Start the vibration animation
     */
    public void startVibrationAnimation() {
        if (vibrationAnimator != null) {
            vibrationAnimator.start();
        }
    }

    /**
     * Stop the vibration animation
     */
    public void stopVibrationAnimation() {
        if (vibrationAnimator != null) {
            vibrationAnimator.cancel();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (positionAnimator != null) {
            positionAnimator.removeAllUpdateListeners();
        }
        if (vibrationAnimator != null) {
            vibrationAnimator.removeAllUpdateListeners();
        }
        if (vibrationAnimator != null) {
            vibrationAnimator.removeAllListeners();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int strokeWidth = (int) ((1 - positionHeightRatio()) * (maxRubberWidth - minRubberWidth) + minRubberWidth);
        paint.setStrokeWidth(strokeWidth);
        if (currentPosition == 0) {
            currentPosition = strokeWidth / 2;
        }

        float drawPosition;
        if (vibrationAnimator != null && vibrationAnimator.isRunning()) {
            drawPosition = currentPosition + ((float) vibrationAnimator.getAnimatedValue() * vibrationPeakAmplitude);
        } else if (positionAnimator != null && positionAnimator.isRunning()) {
            currentPosition = (float) positionAnimator.getAnimatedValue();
            drawPosition = currentPosition;
        } else {
            drawPosition = currentPosition;
        }

        float centerX = getWidth() / 2;
        float cubicPadding = getWidth() * loosenessRatio;
        float top = strokeWidth / 2;
        path.reset();
        path.moveTo(0, top);
        path.cubicTo(cubicPadding, top, centerX - cubicPadding, drawPosition, centerX, drawPosition);
        path.cubicTo(centerX + cubicPadding, drawPosition, getWidth() - cubicPadding, top, getWidth(), top);
        canvas.drawPath(path, paint);
    }

    private void setCurrentPosition(float currentPosition) {
        float paintStroke = paint.getStrokeWidth();
        this.currentPosition = Math.max(paintStroke / 2, Math.min(getHeight() - paintStroke / 2, currentPosition));
    }

    private float positionHeightRatio() {
        return currentPosition / getHeight();
    }

    private void updateListener(int action) {
        if (listener != null) {
            int index = fromPositionToIndex();
            switch (action) {
                case MotionEvent.ACTION_UP:
                    listener.onSelectionFinished(index);
                    break;
                default:
                    listener.onSelectionChanged(index);
                    break;
            }
        }
    }

    private int fromPositionToIndex() {
        if (maxValue > 0) {
            return Math.round(positionHeightRatio() * maxValue);
        }
        return 0;
    }

    private float fromIndexToPosition(int index) {
        if (maxValue > 0) {
            return index * getHeight() / maxValue;
        }
        return 0f;
    }

    @ColorInt private int getThemeAccentColor(@NonNull Context context) {
        int colorAttr;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorAttr = android.R.attr.colorAccent;
        } else {
            colorAttr = context.getResources().getIdentifier("colorAccent", "attr", context.getPackageName());
        }
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue.data;
    }
}
