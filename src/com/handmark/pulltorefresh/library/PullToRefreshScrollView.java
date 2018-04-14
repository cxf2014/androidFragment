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
package com.handmark.pulltorefresh.library;

import java.util.LinkedList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.xyb100.framework.R;

public class PullToRefreshScrollView extends PullToRefreshBase<ScrollView> {

	public PullToRefreshScrollView(Context context) {
		super(context);
	}

	public PullToRefreshScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PullToRefreshScrollView(Context context, Mode mode) {
		super(context, mode);
	}

	public PullToRefreshScrollView(Context context, Mode mode, AnimationStyle style) {
		super(context, mode, style);
	}

	@Override
	public final Orientation getPullToRefreshScrollDirection() {
		return Orientation.VERTICAL;
	}

	@Override
	protected ScrollView createRefreshableView(Context context, AttributeSet attrs) {
		ScrollView scrollView;
		if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
			scrollView = new InternalScrollViewSDK9(context, attrs, 0);
		} else {
			scrollView = new ScrollView(context, attrs);
		}

		scrollView.setId(R.id.scrollview);
		return scrollView;
	}

	@Override
	protected boolean isReadyForPullStart() {
		return mRefreshableView.getScrollY() == 0;
	}

	@Override
	protected boolean isReadyForPullEnd() {
		View scrollViewChild = mRefreshableView.getChildAt(0);
		if (null != scrollViewChild) {
			return mRefreshableView.getScrollY() >= (scrollViewChild.getHeight() - getHeight());
		}
		return false;
	}

	@TargetApi(9)
	final class InternalScrollViewSDK9 extends ScrollView {

		private static final String STICKY = "sticky";
		private View mCurrentStickyView;
		private Drawable mShadowDrawable;
		private List<View> mStickyViews;
		private int mStickyViewTopOffset;
		private int defaultShadowHeight = 10;
		private float density;
		private boolean redirectTouchToStickyView;

		/**
		 * 当点击Sticky的时候，实现某些背景的渐变
		 */
		private Runnable mInvalidataRunnable = new Runnable() {

			@Override
			public void run() {
				if (mCurrentStickyView != null) {
					int left = mCurrentStickyView.getLeft();
					int top = mCurrentStickyView.getTop();
					int right = mCurrentStickyView.getRight();
					int bottom = getScrollY() + (mCurrentStickyView.getHeight() + mStickyViewTopOffset);

					invalidate(left, top, right, bottom);
				}

				postDelayed(this, 16);

			}
		};

		public InternalScrollViewSDK9(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public InternalScrollViewSDK9(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			mShadowDrawable = context.getResources().getDrawable(R.drawable.sticky_shadow_default);
			mStickyViews = new LinkedList<View>();
			density = context.getResources().getDisplayMetrics().density;
		}

		@Override
		protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX,
				int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {

			final boolean returnValue = super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
					scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);

			// Does all of the hard work...
			OverscrollHelper.overScrollBy(PullToRefreshScrollView.this, deltaX, scrollX, deltaY, scrollY,
					getScrollRange(), isTouchEvent);

			return returnValue;
		}

		/**
		 * 找到设置tag的View
		 * 
		 * @param viewGroup
		 */
		private void findViewByStickyTag(ViewGroup viewGroup) {
			int childCount = ((ViewGroup) viewGroup).getChildCount();
			for (int i = 0; i < childCount; i++) {
				View child = viewGroup.getChildAt(i);

				if (getStringTagForView(child).contains(STICKY)) {
					mStickyViews.add(child);
				}

				if (child instanceof ViewGroup) {
					findViewByStickyTag((ViewGroup) child);
				}
			}

		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			super.onLayout(changed, l, t, r, b);
			if (changed) {
				findViewByStickyTag((ViewGroup) getChildAt(0));
			}
			showStickyView();
		}

		@Override
		protected void onScrollChanged(int l, int t, int oldl, int oldt) {
			super.onScrollChanged(l, t, oldl, oldt);
			if (scrollViewListener != null) {
				scrollViewListener.onScrollChanged(this, l, t, oldl, oldt);
			}
			showStickyView();
		}

		/**
		 * 
		 */
		private void showStickyView() {
			View curStickyView = null;
			View nextStickyView = null;

			for (View v : mStickyViews) {
				int topOffset = v.getTop() - getScrollY();

				if (topOffset <= 0) {
					if (curStickyView == null || topOffset > curStickyView.getTop() - getScrollY()) {
						curStickyView = v;
					}
				} else {
					if (nextStickyView == null || topOffset < nextStickyView.getTop() - getScrollY()) {
						nextStickyView = v;
					}
				}
			}

			if (curStickyView != null) {
				mStickyViewTopOffset = nextStickyView == null ? 0 : Math.min(0, nextStickyView.getTop() - getScrollY()
						- curStickyView.getHeight());
				mCurrentStickyView = curStickyView;
				post(mInvalidataRunnable);
			} else {
				mCurrentStickyView = null;
				removeCallbacks(mInvalidataRunnable);

			}

		}

		private String getStringTagForView(View v) {
			Object tag = v.getTag();
			return String.valueOf(tag);
		}

		/**
		 * 将sticky画出来
		 */
		@Override
		protected void dispatchDraw(Canvas canvas) {
			super.dispatchDraw(canvas);
			if (mCurrentStickyView != null) {
				// 先保存起来
				canvas.save();
				// 将坐标原点移动到(0, getScrollY() + mStickyViewTopOffset)
				canvas.translate(0, getScrollY() + mStickyViewTopOffset);

				if (mShadowDrawable != null) {
					int left = 0;
					int top = mCurrentStickyView.getHeight() + mStickyViewTopOffset;
					int right = mCurrentStickyView.getWidth();
					int bottom = top + (int) (density * defaultShadowHeight + 0.5f);
					mShadowDrawable.setBounds(left, top, right, bottom);
					mShadowDrawable.draw(canvas);
				}

				canvas.clipRect(0, mStickyViewTopOffset, mCurrentStickyView.getWidth(), mCurrentStickyView.getHeight());

				mCurrentStickyView.draw(canvas);

				// 重置坐标原点参数
				canvas.restore();
			}
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev) {
			if (ev.getAction() == MotionEvent.ACTION_DOWN) {
				redirectTouchToStickyView = true;
			}

			if (redirectTouchToStickyView) {
				redirectTouchToStickyView = mCurrentStickyView != null;

				if (redirectTouchToStickyView) {
					redirectTouchToStickyView = ev.getY() <= (mCurrentStickyView.getHeight() + mStickyViewTopOffset)
							&& ev.getX() >= mCurrentStickyView.getLeft() && ev.getX() <= mCurrentStickyView.getRight();
				}
			}

			if (redirectTouchToStickyView) {
				ev.offsetLocation(0, -1 * ((getScrollY() + mStickyViewTopOffset) - mCurrentStickyView.getTop()));
			}
			return super.dispatchTouchEvent(ev);
		}

		private boolean hasNotDoneActionDown = true;

		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			if (redirectTouchToStickyView) {
				ev.offsetLocation(0, ((getScrollY() + mStickyViewTopOffset) - mCurrentStickyView.getTop()));
			}

			if (ev.getAction() == MotionEvent.ACTION_DOWN) {
				hasNotDoneActionDown = false;
			}

			if (hasNotDoneActionDown) {
				MotionEvent down = MotionEvent.obtain(ev);
				down.setAction(MotionEvent.ACTION_DOWN);
				super.onTouchEvent(down);
				hasNotDoneActionDown = false;
			}

			if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
				hasNotDoneActionDown = true;
			}
			return super.onTouchEvent(ev);
		}

		/**
		 * Taken from the AOSP ScrollView source
		 */
		private int getScrollRange() {
			int scrollRange = 0;
			if (getChildCount() > 0) {
				View child = getChildAt(0);
				scrollRange = Math.max(0, child.getHeight() - (getHeight() - getPaddingBottom() - getPaddingTop()));
			}
			return scrollRange;
		}

		// 滑动距离及坐标
		private float xDistance, yDistance, xLast, yLast;

		public boolean onInterceptTouchEvent(MotionEvent ev) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				xDistance = yDistance = 0f;
				xLast = ev.getX();
				yLast = ev.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				final float curX = ev.getX();
				final float curY = ev.getY();

				xDistance += Math.abs(curX - xLast);
				yDistance += Math.abs(curY - yLast);
				xLast = curX;
				yLast = curY;
				if (yDistance > 0) {
					if (moveViewListener != null) {
						moveViewListener.hideView();
					}
				}
				if (xDistance > yDistance) {
					return false;
				}
				break;
			case MotionEvent.ACTION_UP:
				if (moveViewListener != null) {
					moveViewListener.showView();
				}
				break;

			}
			return super.onInterceptTouchEvent(ev);
		}
	}

	public interface OnMoveViewListener {
		public void hideView();

		public void showView();
	}

	private OnMoveViewListener moveViewListener;

	public void setOnMoveViewListener(OnMoveViewListener moveViewListener) {
		this.moveViewListener = moveViewListener;
	}

	public interface ScrollViewListener {
		void onScrollChanged(ScrollView scrollview, int x, int y, int oldx, int oldy);
	}

	private ScrollViewListener scrollViewListener = null;

	public void setScrollViewListener(ScrollViewListener scrollViewListener) {
		this.scrollViewListener = scrollViewListener;
	}
}
