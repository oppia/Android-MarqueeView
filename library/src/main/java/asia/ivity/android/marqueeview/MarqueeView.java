package asia.ivity.android.marqueeview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Provides a simple marquee effect for a single {@link android.widget.TextView}.
 *
 * @author Sebastian Roth <sebastian.roth@gmail.com>
 */
public class MarqueeView extends LinearLayout {
    private TextView mTextField;

    private ScrollView mScrollView;

    private Animation mMoveTextOut = null;
    private Animation mMoveTextIn = null;

    private Paint mPaint;

    private boolean mMarqueeNeeded = false;

    private static final String TAG = MarqueeView.class.getSimpleName();

    private float mTextDifference;

    private int textViewActualWidth;

    /**
     * Control the speed. The lower this value, the faster it will scroll.
     */
    private static final int DEFAULT_ANIMATION_SPEED = 60;

    /**
     * Control the pause between the animations. Also, after starting this activity.
     */
    private static final int DEFAULT_ANIMATION_DELAY = 2000;

    private int mSpeed = DEFAULT_ANIMATION_SPEED;

    private int mAnimationDelay = DEFAULT_ANIMATION_DELAY;

    private boolean mAutoStart = false;

    private Interpolator mInterpolator = new LinearInterpolator();

    private boolean mCancelled = false;
    private Runnable mAnimationStartRunnable;

    private boolean mStarted;

    /**
     * Sets the animation speed.
     * The lower the value, the faster the animation will be displayed.
     *
     * @param speed Milliseconds per PX.
     */
    public void setSpeed(int speed) {
        this.mSpeed = speed;
    }

    /**
     * Sets the pause between animations
     *
     * @param pause In milliseconds.
     */
    public void setPauseBetweenAnimations(int pause) {
        this.mAnimationDelay = pause;
    }

    /**
     * Sets a custom interpolator for the animation.
     *
     * @param interpolator Animation interpolator.
     */
    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public MarqueeView(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public MarqueeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
        extractAttributes(attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public MarqueeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
        extractAttributes(attrs);
    }

    private void extractAttributes(AttributeSet attrs) {
        if (getContext() == null) {
            return;
        }

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.asia_ivity_android_marqueeview_MarqueeView);

        if (a == null) {
            return;
        }

        mSpeed = a.getInteger(R.styleable.asia_ivity_android_marqueeview_MarqueeView_animationSpeed, DEFAULT_ANIMATION_SPEED);
        mAnimationDelay = a.getInteger(R.styleable.asia_ivity_android_marqueeview_MarqueeView_animationDelay, DEFAULT_ANIMATION_DELAY);
        mAutoStart = a.getBoolean(R.styleable.asia_ivity_android_marqueeview_MarqueeView_autoStart, false);

        a.recycle();
    }

    private void init() {
        // init helper
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(1);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mInterpolator = new LinearInterpolator();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (getChildCount() != 1) {
            throw new RuntimeException("MarqueeView must have exactly one child element.");
        }

        if (changed && mScrollView == null) {
            if (!(getChildAt(0) instanceof TextView)) {
                throw new RuntimeException("The child view of this MarqueeView must be a TextView instance.");
            }

            initView(getContext());

            prepareAnimation();

            if (mAutoStart) {
                startMarquee();
            }
        }
    }

    /**
     * Starts the configured marquee effect.
     */
    public void startMarquee() {
        if (mMarqueeNeeded) {
            startTextFieldAnimation();
        }

        mCancelled = false;
        mStarted = true;
    }

    private void startTextFieldAnimation() {
        mAnimationStartRunnable = new Runnable() {
            public void run() {
                mTextField.startAnimation(mMoveTextOut);
            }
        };
        postDelayed(mAnimationStartRunnable, mAnimationDelay);
    }

    /**
     * Disables the animations.
     */
    public void reset() {
        mCancelled = true;

        if (mAnimationStartRunnable != null) {
            removeCallbacks(mAnimationStartRunnable);
        }

        mTextField.clearAnimation();
        mStarted = false;

        mMoveTextOut.reset();
        mMoveTextIn.reset();

        cutTextView();

        invalidate();
    }

    private void prepareAnimation() {
        // Measure
        mPaint.setTextSize(mTextField.getTextSize());
        mPaint.setTypeface(mTextField.getTypeface());
        float mTextWidth = mPaint.measureText(mTextField.getText().toString());
        textViewActualWidth = (int) mTextWidth;

        // See how much functions are needed at all
        mMarqueeNeeded = mTextWidth > getMeasuredWidth();

        mTextDifference = Math.abs((mTextWidth - getMeasuredWidth())) + 5;

        final int duration = (int) (mTextDifference * mSpeed);

        mMoveTextOut = new TranslateAnimation(0, -mTextDifference, 0, 0);
        mMoveTextOut.setDuration(duration);
        mMoveTextOut.setInterpolator(mInterpolator);
        mMoveTextOut.setFillAfter(true);

        mMoveTextIn = new TranslateAnimation(-mTextDifference, 0, 0, 0);
        mMoveTextIn.setDuration(duration);
        mMoveTextIn.setStartOffset(mAnimationDelay);
        mMoveTextIn.setInterpolator(mInterpolator);
        mMoveTextIn.setFillAfter(true);

        mMoveTextOut.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
                expandTextView();
            }

            public void onAnimationEnd(Animation animation) {
                if (mCancelled) {
                    reset();
                    return;
                }

                mTextField.startAnimation(mMoveTextIn);
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });

        mMoveTextIn.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {

                cutTextView();

                if (mCancelled) {
                    reset();
                    return;
                }
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void initView(Context context) {
        // Scroll View
        LayoutParams sv1lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        sv1lp.gravity = Gravity.CENTER_HORIZONTAL;
        mScrollView = new ScrollView(context);

        // Scroll View 1 - Text Field
        mTextField = (TextView) getChildAt(0);
        removeView(mTextField);

        mScrollView.addView(mTextField, new ScrollView.LayoutParams(textViewActualWidth, LayoutParams.WRAP_CONTENT));

        mTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                final boolean continueAnimation = mStarted;

                reset();
                prepareAnimation();

                cutTextView();

                post(new Runnable() {
                    @Override
                    public void run() {
                        if (continueAnimation) {
                            startMarquee();
                        }
                    }
                });
            }
        });

        addView(mScrollView, sv1lp);
    }

    private void expandTextView() {
        ViewGroup.LayoutParams lp = mTextField.getLayoutParams();
        lp.width = textViewActualWidth;
        mTextField.setLayoutParams(lp);
    }

    private void cutTextView() {
        if (mTextField.getWidth() != getMeasuredWidth()) {
            ViewGroup.LayoutParams lp = mTextField.getLayoutParams();
            lp.width = getMeasuredWidth();
            mTextField.setLayoutParams(lp);
        }
    }
}
