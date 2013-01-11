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
	 * ?��??????
	 */
	public enum RefreshStatusEnum {
		/** ?��??��??��?�?��????��? **/
		CLICK_TO_REFRESH,
		/** �????ayout�?���??�??????��?�??????��??????**/
		DROP_DOWN_TO_REFRESH,
		/** �????ayout�?���??�??????��?????��??��? **/
		RELEASE_TO_REFRESH,
		/** ?��?�?**/
		REFRESHING
	}

	/** �???��????离�?header top??????�?**/
	private static final float HEADER_PADDING_RATE = 1.5f;

	/** �???��?�??离顶?��?大�?�?**/
	private static final int MAX_DISTANCE_TOP = 50;

	/** header height????????**/
	private static final int HEADER_HEIGHT_UPPER_LEVEL = 10;

	/** ?��?�?�� **/
	private OnRefreshListener mOnRefreshListener;

	private OnScrollListener mOnScrollListener;

	/** ?????iew **/
	private RelativeLayout mRefreshViewLayout;
	private TextView mRefreshViewTipsText;
	private ImageView mRefreshViewImage;
	private ProgressBar mRefreshViewProgress;
	private TextView mRefreshViewLastUpdatedText;

	/** �??????��???**/
	private int mCurrentScrollState;
	/** �??????��???**/
	private RefreshStatusEnum mCurrentRefreshState;

	/** 正�?翻转??nimation **/
	private RotateAnimation mFlipAnimation;
	/** ???翻转??nimation **/
	private RotateAnimation mReverseFlipAnimation;

	/** header(?��?View layout)???�??�?**/
	private int mHeaderOriginalHeight;
	/** header(?��?View layout)???�?op padding **/
	private int mHeaderOriginalTopPadding;
	/** ?��???????�??�??touch???y??? **/
	private float mActionDownPointY;
	/** ?????���???��?顶�????记为true **/
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
	 * 设置?��?�?��??
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
		 * ListView�?CROLL_STATE_TOUCH_SCROLL?��?(???�??�??�?并�??��??��?�?��REFRESHING a.
		 * ?��?对�???tem????��??��???ayout�?���?????�??�???��???��RELEASE_TO_REFRESH�?
		 * ?��???ayout�?���??�?��???�??�???��???��DROP_DOWN_TO_REFRESH b.
		 * ?��?对�???tem�??�????��header ListView�?CROLL_STATE_FLING?��?(?��?�??�? a.
		 * ?��??��?�??item???并�??��??��?�?��REFRESHING�??�?osition�????��?�?��)item??? b.
		 * ?��?弹�??��?设置position�????��?�?��)item???
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
	 * ????��?
	 */
	public void onRefreshBegin() {
		setStatusRefreshing();
	}

	/**
	 * ?��?
	 */
	public void onRefresh() {
		if (mOnRefreshListener != null) {
			onRefreshBegin();
			mOnRefreshListener.onRefresh();
		}
	}

	/**
	 * ?��?�??
	 * 
	 * @param lastUpdatedText
	 *            �???��?信�?�??�?ull�???�示
	 */
	public void onRefreshComplete(CharSequence lastUpdatedText) {
		setLastUpdatedText(lastUpdatedText);
		onRefreshComplete();
	}

	/**
	 * ?��?�??�??�?iew?��?
	 */
	public void onRefreshComplete() {
		setStatusClickToRefresh();

		if (mRefreshViewLayout.getBottom() > 0) {
			invalidateViews();
			setSecondPositionVisible();
		}
	}

	/**
	 * ?��??��?View?��???br/>
	 * 主�???ist�??�??items�??�??????��??��??��??��???iew?��???
	 */
	private class OnClickRefreshListener implements OnClickListener {

		public void onClick(View v) {
			if (mCurrentRefreshState != RefreshStatusEnum.REFRESHING) {
				onRefresh();
			}
		}

	}

	/**
	 * ?��???ist?��???
	 * 
	 * @author Trinea 2012-5-31 �??11:15:39
	 */
	public interface OnRefreshListener {

		/**
		 * ?��???ist?��???
		 */
		public void onRefresh();
	}

	/**
	 * �??�??�??�??item position�?(?�为?��?View)�??�?osition�????��?�?��)item???
	 */
	public void setSecondPositionVisible() {
		if (getAdapter() != null && getAdapter().getCount() > 0 && getFirstVisiblePosition() == 0) {
			setSelection(1);
		}
	}

	/**
	 * 设置�???��?信�?
	 * 
	 * @param lastUpdatedText
	 *            �???��?信�?�??�?ull�???�示
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
	 * 设置�?LICK_TO_REFRESH?��?
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
	 * 设置�?ROP_DOWN_TO_REFRESH?��?
	 */
	private void setStatusDropDownToRefresh() {
		if (mCurrentRefreshState != RefreshStatusEnum.DROP_DOWN_TO_REFRESH) {
			mRefreshViewImage.setVisibility(View.VISIBLE);
			// CLICK_TO_REFRESH�??�???��???
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
	 * 设置�?ELEASE_TO_REFRESH?��?
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
	 * 设置�?EFRESHING?��?
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
	 * �??header??adding
	 * 
	 * @param ev
	 */
	private void adjustHeaderPadding(MotionEvent ev) {
		/**
		 * ????��?move???????��?�??设置header??adding
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
	 * ??��header??adding
	 */
	private void resetHeaderPadding() {
		mRefreshViewLayout.setPadding(mRefreshViewLayout.getPaddingLeft(), mHeaderOriginalTopPadding, mRefreshViewLayout.getPaddingRight(), mRefreshViewLayout.getPaddingBottom());
	}

	/**
	 * �??View???�??�?��
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
