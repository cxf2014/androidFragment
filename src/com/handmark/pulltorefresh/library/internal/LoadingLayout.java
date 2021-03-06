/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.handmark.pulltorefresh.library.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.ILoadingLayout;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Orientation;
import com.xyb100.framework.R;

@SuppressLint("ViewConstructor")
public abstract class LoadingLayout extends FrameLayout implements ILoadingLayout {

	static final String LOG_TAG = "PullToRefresh-LoadingLayout";

	static final Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

	private FrameLayout mInnerLayout;

	protected final FirstSetpView mHeaderImage;
	protected final SecondStepView mHeaderProgress;
	protected final AnimationDrawable secondAnim;

	private boolean mUseIntrinsicAnimation;

	private final TextView mHeaderText;
	private final TextView mSubHeaderText;

	protected final Mode mMode;
	protected final Orientation mScrollDirection;

	private CharSequence mPullLabel;
	private CharSequence mRefreshingLabel;
	private CharSequence mReleaseLabel;

	public LoadingLayout(Context context, final Mode mode, final Orientation scrollDirection, TypedArray attrs) {
		super(context);
		mMode = mode;
		mScrollDirection = scrollDirection;

		switch (scrollDirection) {
		case HORIZONTAL:
			LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_header_horizontal, this);
			break;
		case VERTICAL:
		default:
			LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_header_vertical, this);
			break;
		}

		mInnerLayout = (FrameLayout) findViewById(R.id.fl_inner);
		mHeaderText = (TextView) mInnerLayout.findViewById(R.id.pull_to_refresh_text);
		mHeaderProgress = (SecondStepView) mInnerLayout.findViewById(R.id.second_view);
		mHeaderProgress.setBackgroundResource(R.drawable.second_step_animation);
		secondAnim = (AnimationDrawable) mHeaderProgress.getBackground();
		mSubHeaderText = (TextView) mInnerLayout.findViewById(R.id.pull_to_refresh_sub_text);
		mHeaderImage = (FirstSetpView) mInnerLayout.findViewById(R.id.pull_to_refresh_image);

		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mInnerLayout.getLayoutParams();

		switch (mode) {
		case PULL_FROM_END:
			lp.gravity = scrollDirection == Orientation.VERTICAL ? Gravity.TOP : Gravity.LEFT;

			// Load in labels
			mPullLabel = context.getString(R.string.pull_to_refresh_from_bottom_pull_label);
			mRefreshingLabel = context.getString(R.string.pull_to_refresh_from_bottom_refreshing_label);
			mReleaseLabel = context.getString(R.string.pull_to_refresh_from_bottom_release_label);
			mHeaderProgress.setVisibility(View.VISIBLE);
			mHeaderImage.setVisibility(View.VISIBLE);
			mSubHeaderText.setVisibility(View.GONE);
			break;

		case PULL_FROM_START:
		default:
			lp.gravity = scrollDirection == Orientation.VERTICAL ? Gravity.BOTTOM : Gravity.RIGHT;

			// Load in labels
			mPullLabel = context.getString(R.string.pull_to_refresh_pull_label_01);
			mRefreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label_01);
			mReleaseLabel = context.getString(R.string.pull_to_refresh_release_label_01);
			break;
		}

		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderBackground)) {
			Drawable background = attrs.getDrawable(R.styleable.PullToRefresh_ptrHeaderBackground);
			if (null != background) {
				ViewCompat.setBackground(this, background);
			}
		}

		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderTextAppearance)) {
			TypedValue styleID = new TypedValue();
			attrs.getValue(R.styleable.PullToRefresh_ptrHeaderTextAppearance, styleID);
			setTextAppearance(styleID.data);
		}
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrSubHeaderTextAppearance)) {
			TypedValue styleID = new TypedValue();
			attrs.getValue(R.styleable.PullToRefresh_ptrSubHeaderTextAppearance, styleID);
			setSubTextAppearance(styleID.data);
		}

		// Text Color attrs need to be set after TextAppearance attrs
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderTextColor)) {
			ColorStateList colors = attrs.getColorStateList(R.styleable.PullToRefresh_ptrHeaderTextColor);
			if (null != colors) {
				setTextColor(colors);
			}
		}
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderSubTextColor)) {
			ColorStateList colors = attrs.getColorStateList(R.styleable.PullToRefresh_ptrHeaderSubTextColor);
			if (null != colors) {
				setSubTextColor(colors);
			}
		}

		// Try and get defined drawable from Attrs
		Drawable imageDrawable = null;
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawable)) {
			imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawable);
		}

		// Check Specific Drawable from Attrs, these overrite the generic
		// drawable attr above
		switch (mode) {
		case PULL_FROM_START:
		default:
			if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawableStart)) {
				imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawableStart);
			} else if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawableTop)) {
				Utils.warnDeprecation("ptrDrawableTop", "ptrDrawableStart");
				imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawableTop);
			}
			break;

		case PULL_FROM_END:
			if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawableEnd)) {
				imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawableEnd);
			} else if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawableBottom)) {
				Utils.warnDeprecation("ptrDrawableBottom", "ptrDrawableEnd");
				imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawableBottom);
			}
			break;
		}

		// If we don't have a user defined drawable, load the default
		if (null == imageDrawable) {
			imageDrawable = context.getResources().getDrawable(getDefaultDrawableResId());
		}

		// Set Drawable, and save width/height
		if (mode == Mode.PULL_FROM_START) {
			setLoadingDrawable(imageDrawable);
		}
		reset();
	}

	public final void setHeight(int height) {
		ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) getLayoutParams();
		lp.height = height;
		requestLayout();
	}

	public final void setWidth(int width) {
		ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) getLayoutParams();
		lp.width = width;
		requestLayout();
	}

	public final int getContentSize() {
		switch (mScrollDirection) {
		case HORIZONTAL:
			return mInnerLayout.getWidth();
		case VERTICAL:
		default:
			return mInnerLayout.getHeight();
		}
	}

	public final void hideAllViews() {
		if (View.VISIBLE == mHeaderText.getVisibility()) {
			mHeaderText.setVisibility(View.INVISIBLE);
		}
		if (mMode == Mode.PULL_FROM_START) {
			if (View.VISIBLE == mHeaderImage.getVisibility()) {
				mHeaderImage.setVisibility(View.INVISIBLE);
			}
		} else {
			mHeaderImage.setVisibility(View.INVISIBLE);
		}
		if (mMode == Mode.PULL_FROM_START) {
			if (View.VISIBLE == mSubHeaderText.getVisibility()) {
				mSubHeaderText.setVisibility(View.INVISIBLE);
			}
		} else {
			mSubHeaderText.setVisibility(View.GONE);
		}
	}

	public final void onPull(float scaleOfLayout) {
		if (!mUseIntrinsicAnimation) {
			onPullImpl(scaleOfLayout);
		}
	}

	public final void pullToRefresh() {
		if (null != mHeaderText) {
			if (mMode == Mode.PULL_FROM_END) {
				mHeaderText.setText(mRefreshingLabel);
			} else {
				mHeaderText.setText(mPullLabel);
			}
		}

		// Now call the callback
		pullToRefreshImpl();
	}

	public final void refreshing() {
		if (null != mHeaderText) {
			mHeaderText.setText(mRefreshingLabel);
		}

		if (mMode == Mode.PULL_FROM_START) {
			if (mUseIntrinsicAnimation) {
				// 注释
				// ((AnimationDrawable) mHeaderImage.getDrawable()).start();
			} else {
				// Now call the callback
				refreshingImpl();
			}
		}

		if (null != mSubHeaderText) {
			mSubHeaderText.setVisibility(View.GONE);
		}
	}

	public final void releaseToRefresh() {
		if (null != mHeaderText) {
			if (mMode == Mode.PULL_FROM_START) {
				mHeaderText.setText(mReleaseLabel);
			} else {
				mHeaderText.setText(mRefreshingLabel);
			}
		}

		if (mMode == Mode.PULL_FROM_START) {
			// Now call the callback
			releaseToRefreshImpl();
		}
	}

	public final void reset() {
		// if (null != mHeaderText) {
		// mHeaderText.setText(mPullLabel);
		// }
		if (mMode == Mode.PULL_FROM_START) {
			mHeaderImage.setVisibility(View.VISIBLE);
			if (mUseIntrinsicAnimation) {
				// 注释
				// ((AnimationDrawable) mHeaderImage.getDrawable()).stop();
			} else {
				// Now call the callback
				resetImpl();
			}
		} else {
			mHeaderImage.setVisibility(View.GONE);
		}
		// if (null != mSubHeaderText) {
		// if (TextUtils.isEmpty(mSubHeaderText.getText())) {
		// mSubHeaderText.setVisibility(View.GONE);
		// } else {
		// mSubHeaderText.setVisibility(View.VISIBLE);
		// }
		// }
	}

	@Override
	public void setLastUpdatedLabel(CharSequence label) {
		setSubHeaderText(label);
	}

	public final void setLoadingDrawable(Drawable imageDrawable) {
		// Set Drawable
		// 注释
		// mHeaderImage.setImageDrawable(imageDrawable);
		mUseIntrinsicAnimation = (imageDrawable instanceof AnimationDrawable);

		// Now call the callback
		onLoadingDrawableSet(imageDrawable);
	}

	public void setPullLabel(CharSequence pullLabel) {
		mPullLabel = pullLabel;
	}

	public void setRefreshingLabel(CharSequence refreshingLabel) {
		mRefreshingLabel = refreshingLabel;
	}

	public void setReleaseLabel(CharSequence releaseLabel) {
		mReleaseLabel = releaseLabel;
	}

	@Override
	public void setTextTypeface(Typeface tf) {
		mHeaderText.setTypeface(tf);
	}

	public final void showInvisibleViews() {
		if (View.INVISIBLE == mHeaderText.getVisibility()) {
			mHeaderText.setVisibility(View.VISIBLE);
		}
		if (mMode == Mode.PULL_FROM_START) {
			if (View.INVISIBLE == mHeaderProgress.getVisibility()) {
				mHeaderProgress.setVisibility(View.VISIBLE);
			}
		} else {
			mHeaderProgress.setVisibility(View.VISIBLE);
		}
		if (mMode == Mode.PULL_FROM_START) {
			if (View.INVISIBLE == mHeaderImage.getVisibility()) {
				mHeaderImage.setVisibility(View.VISIBLE);
			}
		} else {
			mHeaderImage.setVisibility(View.GONE);
		}
		if (mMode == Mode.PULL_FROM_START) {
			if (View.INVISIBLE == mSubHeaderText.getVisibility()) {
				mSubHeaderText.setVisibility(View.VISIBLE);
			}
		} else {
			mSubHeaderText.setVisibility(View.GONE);
		}
	}

	public void setProgress(float progress) {
		mHeaderImage.setCurrentProgress((float) progress);
		mHeaderImage.postInvalidate();
	}

	/**
	 * Callbacks for derivative Layouts
	 */

	protected abstract int getDefaultDrawableResId();

	protected abstract void onLoadingDrawableSet(Drawable imageDrawable);

	protected abstract void onPullImpl(float scaleOfLayout);

	protected abstract void pullToRefreshImpl();

	protected abstract void refreshingImpl();

	protected abstract void releaseToRefreshImpl();

	protected abstract void resetImpl();

	public abstract void initPullToRefresh();

	private void setSubHeaderText(CharSequence label) {
		if (null != mSubHeaderText) {
			if (TextUtils.isEmpty(label)) {
				mSubHeaderText.setVisibility(View.GONE);
			} else {
				mSubHeaderText.setText(label);

				// Only set it to Visible if we're GONE, otherwise VISIBLE will
				// be set soon
				if (View.GONE == mSubHeaderText.getVisibility()) {
					if (mMode == Mode.PULL_FROM_START) {
						mSubHeaderText.setVisibility(View.VISIBLE);
					} else {
						mSubHeaderText.setVisibility(View.GONE);
					}
				}
			}
		}
	}

	private void setSubTextAppearance(int value) {
		if (null != mSubHeaderText) {
			mSubHeaderText.setTextAppearance(getContext(), value);
		}
	}

	private void setSubTextColor(ColorStateList color) {
		if (null != mSubHeaderText) {
			mSubHeaderText.setTextColor(color);
		}
	}

	private void setTextAppearance(int value) {
		if (null != mHeaderText) {
			mHeaderText.setTextAppearance(getContext(), value);
		}
		if (null != mSubHeaderText) {
			mSubHeaderText.setTextAppearance(getContext(), value);
		}
	}

	private void setTextColor(ColorStateList color) {
		if (null != mHeaderText) {
			mHeaderText.setTextColor(color);
		}
		if (null != mSubHeaderText) {
			mSubHeaderText.setTextColor(color);
		}
	}

}
