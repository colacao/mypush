package com.ycao.mypush;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.ycao.mypush.R;

public class DropDownToRefreshListView extends ListView implements OnScrollListener {

	/**
	 * ?·æ??????
	 */
	public enum RefreshStatusEnum {
		/** ?¹å??·æ??¶æ?ï¼?¸º????¶æ? **/
		CLICK_TO_REFRESH,
		/** å½????ayouté«?º¦ä½??ä¸??????¶ï?ä¸??????¾å??????**/
		DROP_DOWN_TO_REFRESH,
		/** å½????ayouté«?º¦é«??ä¸??????¶ï?????³å??·æ? **/
		RELEASE_TO_REFRESH,
		/** ?·æ?ä¸?**/
		REFRESHING
	}

	/** ä¸???¶ä????ç¦»å?header top??????ä¾?**/
	private static final float HEADER_PADDING_RATE = 1.5f;

	/** ä¸???¶è?ç½??ç¦»é¡¶?¨æ?å¤§è?ç¦?**/
	private static final int MAX_DISTANCE_TOP = 50;

	/** header height????????**/
	private static final int HEADER_HEIGHT_UPPER_LEVEL = 10;

	/** ?·æ?äº?»¶ **/
	private OnRefreshListener mOnRefreshListener;

	private OnScrollListener mOnScrollListener;

	/** ?????iew **/
	private RelativeLayout mRefreshViewLayout;
	private TextView mRefreshViewTipsText;
	private ImageView mRefreshViewImage;
	private ProgressBar mRefreshViewProgress;
	private TextView mRefreshViewLastUpdatedText;

	/** å½??????¨ç???**/
	private int mCurrentScrollState;
	/** å½??????°ç???**/
	private RefreshStatusEnum mCurrentRefreshState;

	/** æ­£å?ç¿»è½¬??nimation **/
	private RotateAnimation mFlipAnimation;
	/** ???ç¿»è½¬??nimation **/
	private RotateAnimation mReverseFlipAnimation;

	/** header(?·æ?View layout)???å§??åº?**/
	private int mHeaderOriginalHeight;
	/** header(?·æ?View layout)???å§?op padding **/
	private int mHeaderOriginalTopPadding;
	/** ?¨æ???????è§??å¹??touch???y??? **/
	private float mActionDownPointY;
	/** ?????¼¹ï¼???¨å?é¡¶é????è®°ä¸ºtrue **/
	private boolean mIsBounceHack;

	public DropDownToRefreshListView(Context context) {
		super(context);
		init(context);
	}

	public DropDownToRefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public DropDownToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		mFlipAnimation = new RotateAnimation(0, 180, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mFlipAnimation.setInterpolator(new LinearInterpolator());
		mFlipAnimation.setDuration(250);
		mFlipAnimation.setFillAfter(true);
		mReverseFlipAnimation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
		mReverseFlipAnimation.setDuration(250);
		mReverseFlipAnimation.setFillAfter(true);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mRefreshViewLayout = (RelativeLayout) inflater.inflate(R.layout.drop_down_to_refresh_list_header, this, false);
		mRefreshViewTipsText = (TextView) mRefreshViewLayout.findViewById(R.id.drop_down_to_refresh_list_text);
		mRefreshViewImage = (ImageView) mRefreshViewLayout.findViewById(R.id.drop_down_to_refresh_list_image);
		mRefreshViewProgress = (ProgressBar) mRefreshViewLayout.findViewById(R.id.drop_down_to_refresh_list_progress);
		mRefreshViewLastUpdatedText = (TextView) mRefreshViewLayout.findViewById(R.id.drop_down_to_refresh_list_last_updated_text);
		mRefreshViewImage.setMinimumHeight(50);
		mRefreshViewLayout.setOnClickListener(new OnClickRefreshListener());
		mRefreshViewTipsText.setText(R.string.drop_down_to_refresh_list_refresh_view_tips);
		
		addHeaderView(mRefreshViewLayout);
		
	
		super.setOnScrollListener(this);

		measureView(mRefreshViewLayout);
		mHeaderOriginalHeight = mRefreshViewLayout.getMeasuredHeight();
		mHeaderOriginalTopPadding = mRefreshViewLayout.getPaddingTop();
		mCurrentRefreshState = RefreshStatusEnum.RELEASE_TO_REFRESH;
		//onRefreshBegin();
		
	//	mOnRefreshListener.onRefresh();
		
		
		
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		super.setAdapter(adapter);
		setSecondPositionVisible();
	}

	@Override
	public void setOnScrollListener(AbsListView.OnScrollListener listener) {
		mOnScrollListener = listener;
	}

	/**
	 * è®¾ç½®?·æ?äº?»¶??
	 * 
	 * @param onRefreshListener
	 */
	public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
		mOnRefreshListener = onRefreshListener;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mIsBounceHack = false;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mActionDownPointY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			adjustHeaderPadding(event);
			break;
		case MotionEvent.ACTION_UP:
			if (!isVerticalScrollBarEnabled()) {
				setVerticalScrollBarEnabled(true);
			}
			if (getFirstVisiblePosition() == 0 && mCurrentRefreshState != RefreshStatusEnum.REFRESHING) {
				switch (mCurrentRefreshState) {
				case CLICK_TO_REFRESH:
					setStatusClickToRefresh();
					break;
				case RELEASE_TO_REFRESH:
					onRefresh();
					break;
				case DROP_DOWN_TO_REFRESH:
					setStatusClickToRefresh();
					setSecondPositionVisible();
					break;
				default:
					break;
				}
			}
			break;
		}
		return super.onTouchEvent(event);
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		/**
		 * ListViewä¸?CROLL_STATE_TOUCH_SCROLL?¶æ?(???ä¸??æ»??ä¸?å¹¶ä??·æ??¶æ?ä¸?¸ºREFRESHING a.
		 * ?·æ?å¯¹å???tem????¶ï??¥å???ayouté«?º¦è¶?????ï¼??ç½???°ç???¸ºRELEASE_TO_REFRESHï¼?
		 * ?¥å???ayouté«?º¦ä½??é«?º¦???ï¼??ç½???°ç???¸ºDROP_DOWN_TO_REFRESH b.
		 * ?·æ?å¯¹å???temä¸??è§????½®header ListViewä¸?CROLL_STATE_FLING?¶æ?(?¾æ?æ»??ä¸? a.
		 * ?¥å??°å?åº??item???å¹¶ä??·æ??¶æ?ä¸?¸ºREFRESHINGï¼??ç½?ositionä¸????³ç?äº?¸ª)item??? b.
		 * ?¥å?å¼¹å??¥ï?è®¾ç½®positionä¸????³ç?äº?¸ª)item???
		 */
		//Log.i("adsf",view.getMeasuredHeight() +":"+view.getScrollY()+":"+getHeight());
		if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL && mCurrentRefreshState != RefreshStatusEnum.REFRESHING) {
			if (firstVisibleItem == 0) {
				mRefreshViewImage.setVisibility(View.VISIBLE);
				if (mRefreshViewLayout.getBottom() >= mHeaderOriginalHeight + HEADER_HEIGHT_UPPER_LEVEL || mRefreshViewLayout.getTop() >= 0) {
					
					setStatusReleaseToRefresh();
				} else if (mRefreshViewLayout.getBottom() < mHeaderOriginalHeight + HEADER_HEIGHT_UPPER_LEVEL) {
				
					setStatusDropDownToRefresh();
				}
			} else {
				
				setStatusClickToRefresh();
			}
		} else if (mCurrentScrollState == SCROLL_STATE_FLING && firstVisibleItem == 0 && mCurrentRefreshState != RefreshStatusEnum.REFRESHING) {
			setSecondPositionVisible();
			mIsBounceHack = true;
		} else if (mCurrentScrollState == SCROLL_STATE_FLING && mIsBounceHack) {
			setSecondPositionVisible();
		}

		if (mOnScrollListener != null) {
			mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
		}
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		mCurrentScrollState = scrollState;
	
		if (mCurrentScrollState == SCROLL_STATE_IDLE) {
			mIsBounceHack = false;
		}

		if (mOnScrollListener != null) {
			mOnScrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	/**
	 * ????·æ?
	 */
	public void onRefreshBegin() {
		setStatusRefreshing();
	}

	/**
	 * ?·æ?
	 */
	public void onRefresh() {
		if (mOnRefreshListener != null) {
			onRefreshBegin();
			mOnRefreshListener.onRefresh();
		}
	}

	/**
	 * ?·æ?ç»??
	 * 
	 * @param lastUpdatedText
	 *            ä¸???´æ?ä¿¡æ?ï¼??ä¸?ullï¼???¾ç¤º
	 */
	public void onRefreshComplete(CharSequence lastUpdatedText) {
		setLastUpdatedText(lastUpdatedText);
		onRefreshComplete();
	}

	/**
	 * ?·æ?ç»??ï¼??å¤?iew?¶æ?
	 */
	public void onRefreshComplete() {
		setStatusClickToRefresh();

		if (mRefreshViewLayout.getBottom() > 0) {
			invalidateViews();
			setSecondPositionVisible();
		}
	}

	/**
	 * ?¹å??·æ?View?¶è???br/>
	 * ä¸»è???istä»??å°??itemsï¼??æ³??????°å??½æ??¨ç??»å???iew?¶è???
	 */
	private class OnClickRefreshListener implements OnClickListener {

		public void onClick(View v) {
			if (mCurrentRefreshState != RefreshStatusEnum.REFRESHING) {
				onRefresh();
			}
		}

	}

	/**
	 * ?¨å???ist?¶è???
	 * 
	 * @author Trinea 2012-5-31 ä¸??11:15:39
	 */
	public interface OnRefreshListener {

		/**
		 * ?¨å???ist?¶è???
		 */
		public void onRefresh();
	}

	/**
	 * å¦??ç¬??ä¸??è§??item positionä¸?(?³ä¸º?·æ?View)ï¼??ç½?ositionä¸????³ç?äº?¸ª)item???
	 */
	public void setSecondPositionVisible() {
		if (getAdapter() != null && getAdapter().getCount() > 0 && getFirstVisiblePosition() == 0) {
			setSelection(1);
		}
	}

	/**
	 * è®¾ç½®ä¸???´æ?ä¿¡æ?
	 * 
	 * @param lastUpdatedText
	 *            ä¸???´æ?ä¿¡æ?ï¼??ä¸?ullï¼???¾ç¤º
	 */
	public void setLastUpdatedText(CharSequence lastUpdatedText) {
		if (lastUpdatedText == null) {
			mRefreshViewLastUpdatedText.setVisibility(View.GONE);
		} else {
			mRefreshViewLastUpdatedText.setVisibility(View.VISIBLE);
			mRefreshViewLastUpdatedText.setText(lastUpdatedText);
		}
	}

	/**
	 * è®¾ç½®ä¸?LICK_TO_REFRESH?¶æ?
	 */
	private void setStatusClickToRefresh() {
		if (mCurrentRefreshState != RefreshStatusEnum.CLICK_TO_REFRESH) {
			resetHeaderPadding();

			mRefreshViewImage.clearAnimation();
			mRefreshViewImage.setImageResource(R.drawable.drop_down_to_refresh_list_arrow);
			mRefreshViewImage.setVisibility(View.GONE);
			mRefreshViewProgress.setVisibility(View.GONE);
			mRefreshViewTipsText.setText(R.string.drop_down_to_refresh_list_refresh_view_tips);

			mCurrentRefreshState = RefreshStatusEnum.CLICK_TO_REFRESH;
		}
	}

	/**
	 * è®¾ç½®ä¸?ROP_DOWN_TO_REFRESH?¶æ?
	 */
	private void setStatusDropDownToRefresh() {
		if (mCurrentRefreshState != RefreshStatusEnum.DROP_DOWN_TO_REFRESH) {
			mRefreshViewImage.setVisibility(View.VISIBLE);
			// CLICK_TO_REFRESHä¸??è¦???¨å???
			if (mCurrentRefreshState != RefreshStatusEnum.CLICK_TO_REFRESH) {
				mRefreshViewImage.clearAnimation();
				mRefreshViewImage.startAnimation(mReverseFlipAnimation);
			}
			mRefreshViewProgress.setVisibility(View.GONE);
			mRefreshViewTipsText.setText(R.string.drop_down_to_refresh_list_pull_tips);

			if (isVerticalFadingEdgeEnabled()) {
				setVerticalScrollBarEnabled(false);
			}

			mCurrentRefreshState = RefreshStatusEnum.DROP_DOWN_TO_REFRESH;
		}
	}

	/**
	 * è®¾ç½®ä¸?ELEASE_TO_REFRESH?¶æ?
	 */
	private void setStatusReleaseToRefresh() {
		if (mCurrentRefreshState != RefreshStatusEnum.RELEASE_TO_REFRESH) {
			mRefreshViewImage.setVisibility(View.VISIBLE);
			mRefreshViewImage.clearAnimation();
			mRefreshViewImage.startAnimation(mFlipAnimation);
			mRefreshViewProgress.setVisibility(View.GONE);
			mRefreshViewTipsText.setText(R.string.drop_down_to_refresh_list_release_tips);

			mCurrentRefreshState = RefreshStatusEnum.RELEASE_TO_REFRESH;
		}
	}

	/**
	 * è®¾ç½®ä¸?EFRESHING?¶æ?
	 */
	private void setStatusRefreshing() {
		if (mCurrentRefreshState != RefreshStatusEnum.REFRESHING) {
			resetHeaderPadding();

			mRefreshViewImage.setVisibility(View.GONE);
			mRefreshViewImage.setImageDrawable(null);
			mRefreshViewProgress.setVisibility(View.VISIBLE);
			mRefreshViewTipsText.setText(R.string.drop_down_to_refresh_list_refreshing_tips);

			mCurrentRefreshState = RefreshStatusEnum.REFRESHING;
			setSelection(0);
		}
	}

	/**
	 * è°??header??adding
	 * 
	 * @param ev
	 */
	private void adjustHeaderPadding(MotionEvent ev) {
		/**
		 * ????·å?move???????¹ï?ä¸??è®¾ç½®header??adding
		 */

		int pointerCount = ev.getHistorySize();

		for (int i = 0; i < pointerCount; i++) {
			if (mCurrentRefreshState == RefreshStatusEnum.RELEASE_TO_REFRESH) {
				int paddingTop = (int) (((ev.getHistoricalY(i) - mActionDownPointY) - mHeaderOriginalHeight) / HEADER_PADDING_RATE);
				mRefreshViewLayout.setPadding(mRefreshViewLayout.getPaddingLeft(), paddingTop > MAX_DISTANCE_TOP ? MAX_DISTANCE_TOP : paddingTop,
						mRefreshViewLayout.getPaddingRight(), mRefreshViewLayout.getPaddingBottom());
			}
		}
	}

	/**
	 * ??½®header??adding
	 */
	private void resetHeaderPadding() {
		mRefreshViewLayout.setPadding(mRefreshViewLayout.getPaddingLeft(), mHeaderOriginalTopPadding, mRefreshViewLayout.getPaddingRight(), mRefreshViewLayout.getPaddingBottom());
	}

	/**
	 * æµ??View???åº??é«?º¦
	 * 
	 * @param child
	 */
	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}
}
