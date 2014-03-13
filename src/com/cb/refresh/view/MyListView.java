package com.cb.refresh.view;

import java.text.SimpleDateFormat;

import com.pptv.refresh.R;
import com.pptv.refresh.R.id;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

public class MyListView extends ListView implements OnScrollListener {

	private float mLastY = 0;
	private Scroller mScroller;
	private Context mContext;

	private MyListViewListener mMyListViewListener;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm");

	// -- header view
	private PullToRefreshListViewHeader mHeaderView;

	// header view content, use it to calculate the Header's height. And hide
	// it
	// when disable pull refresh.
	private RelativeLayout mHeaderViewContent;

	private TextView mHeaderTimeView;

	private int mHeaderViewHeight; // header view's height

	private boolean mEnablePullRefresh = true;

	private boolean mPullRefreshing = false; // is refreshing.

	// for mScroller, scroll back from header or footer.
	private int mScrollBack;

	private final static int SCROLLBACK_HEADER = 0;

	public MyListView(Context context) {
		super(context);
		initWithContext(context);
	}

	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initWithContext(context);
	}

	public MyListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initWithContext(context);
	}

	private void initWithContext(Context context) {
		Log.d("cb--MyListView", "initWithContext");
		mScroller = new Scroller(context, new DecelerateInterpolator());
		// XListView need the scroll event, and it will dispatch the event to
		// user's listener (as a proxy).
		super.setOnScrollListener(this);
		mContext = context;
		// init header view
		mHeaderView = new PullToRefreshListViewHeader(context);
		mHeaderViewContent = (RelativeLayout) mHeaderView.findViewById(R.id.xlistview_header_content);
		mHeaderTimeView = (TextView) mHeaderView.findViewById(R.id.xlistview_header_time);
		addHeaderView(mHeaderView);

		// init header height
		mHeaderView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				mHeaderViewHeight = mHeaderViewContent.getHeight();
				getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mLastY == 0) {
			mLastY = ev.getRawY();
		}

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mLastY = ev.getRawY();
			break;

		case MotionEvent.ACTION_MOVE:
			float deltaY = ev.getRawY() - mLastY;
			mLastY = ev.getRawY();

			/**
			 * 条件1：如果ListView最上面的元素处于完全显示状态，说明下拉刷新的布局mHeaderView可以被拉下来了；
			 * 条件2：mHeaderView是下拉状态
			 */
			if (getFirstVisiblePosition() == 0 && (mHeaderView.getVisiableHeight() > 0 || deltaY > 0)) {
				updateHeaderHeight(deltaY); // 更新下拉刷新布局的高度
			}
			break;

		case MotionEvent.ACTION_UP:
		default:
			mLastY = -1; // reset
			Log.d("cb-MyListView", "getFirstVisiblePosition() is " + getFirstVisiblePosition());
			if (getFirstVisiblePosition() == 0) {
				if (mEnablePullRefresh && !mPullRefreshing && mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
					mPullRefreshing = true;
					mHeaderView.setState(PullToRefreshListViewHeader.STATE_REFRESHING);
					mHeaderTimeView.setText(dateFormat.format(System.currentTimeMillis()));
					// BEGIN:下面可以调用方法去刷新啊什么的
					if (mMyListViewListener != null) {
						mMyListViewListener.onRefresh();
					}
					// END
				}
				resetHeaderHeight();
			}
			break;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	public void updateHeaderHeight(float deltaY) {
		mHeaderView.setVisiableHeight((int) deltaY + mHeaderView.getVisiableHeight());
		if (mEnablePullRefresh && !mPullRefreshing) {
			// 未处于刷新状态，更新箭头
			if (mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
				mHeaderView.setState(PullToRefreshListViewHeader.STATE_READY);
			} else {
				mHeaderView.setState(PullToRefreshListViewHeader.STATE_NORMAL);
			}
		}
	}

	public void resetHeaderHeight() {
		int height = mHeaderView.getVisiableHeight();

		if (height == 0) {
			return;
		}
		// refreshing and header isn't shown fully. do nothing.
		if (mPullRefreshing && height <= mHeaderViewHeight) {
			return;
		}
		int finalHeight = 0; // default: scroll back to dismiss header.
		// is refreshing, just scroll back to show all the header.
		if (mPullRefreshing && height > mHeaderViewHeight) {
			finalHeight = mHeaderViewHeight;
		}
		mScrollBack = SCROLLBACK_HEADER;
		// 整体的效果是下拉后mHeaderView缩回。具体操作是：根据高度，scroll to 不显示mHeaderView or
		// 显示mHeaderView的高度(进度条刷新中...)
		// 这个参数设计的很巧妙
		mScroller.startScroll(0, height, 0, finalHeight - height, 400);
		invalidate();
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			if (mScrollBack == SCROLLBACK_HEADER) {
				Log.d("cb--PTRLV", "computeScroll--1, mScroller.getCurrY() is " + mScroller.getCurrY());
				mHeaderView.setVisiableHeight(mScroller.getCurrY());
			}
			postInvalidate();
		}
		super.computeScroll();
	}

	/**
	 * enable or disable pull down refresh feature.
	 * 
	 * @param enable
	 */
	public void setPullRefreshEnable(boolean enable) {
		mEnablePullRefresh = enable;
		if (!mEnablePullRefresh) { // disable, hide the content
			mHeaderViewContent.setVisibility(View.INVISIBLE);
		} else {
			mHeaderViewContent.setVisibility(View.VISIBLE);
		}
	}

	public void stopRefresh() {
		if (mPullRefreshing == true) {
			mPullRefreshing = false;
			resetHeaderHeight();
		}
	}

	/**
	 * implements this interface to get refresh/load more event.
	 */
	public interface MyListViewListener {
		public void onRefresh();
	}

	public void setMyListViewListener(MyListViewListener l) {
		mMyListViewListener = l;
	}
}
