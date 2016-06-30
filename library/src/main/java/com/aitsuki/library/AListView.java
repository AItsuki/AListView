package com.aitsuki.library;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Created by AItsuki on 2016/6/21.
 * 下拉刷新：支持多点触控，支持自定义header，完美体验！
 * 加载更多：在这里有两种方式
 * 1. 监听scrollState，在idle状态下并且到达listView底部，显示加载更多
 * 2. 监听scrollState，滚动到某个位置（比如倒数第3个item时就开始loadmore）
 */
public class AListView extends ListView implements AbsListView.OnScrollListener, AdapterView.OnItemClickListener,Refreshing, Loading {

    private int activePointerId;
    private float lastY;
    private OnRefreshListener onRefreshListener;
    private OnLoadMoreListener onLoadMoreListener;
    private RefreshHeader refreshHeader;
    private LoadMoreFooter loadMoreFooter;

    private OnScrollListener scrollListener; // user's scroll listener
    private OnItemClickListener onItemClickListener;    // user's itemClick listener

    private boolean waitingLoadingCompletedToRefresh;   // 等待加载更多成功后去调用刷新
    private boolean waitingRefreshCompletedToLoadMore;  // 等待刷新成功后去调用加载更多

    public enum CallBackMode {
        NORMAL, ENQUEUE
    }

    private CallBackMode callBackMode = CallBackMode.ENQUEUE;

    public AListView(Context context) {
        super(context);
        initView();
    }

    public AListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public AListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public void setCanLoadMore(boolean canLoadMore) {
        loadMoreFooter.setCanLoadMore(canLoadMore);
    }

    private void initView() {
        super.setOnScrollListener(this);
        super.setOnItemClickListener(this);

        setOverScrollMode(OVER_SCROLL_NEVER);

        QQHeader QQHeader = new QQHeader(getContext());
        setRefreshHeader(QQHeader);

        QQFooter footer = new QQFooter(getContext());
        setLoadMoreFooter(footer);

    }

    /**
     * 设置刷新头部
     * @param refreshHeader header
     */
    public void setRefreshHeader(RefreshHeader refreshHeader) {
        if (refreshHeader != null && this.refreshHeader != refreshHeader) {
            removeHeaderView(this.refreshHeader);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            refreshHeader.setLayoutParams(lp);
            refreshHeader.bindRefreshing(this);
            addHeaderView(refreshHeader, null, false);
            this.refreshHeader = refreshHeader;
        }
    }

    /**
     * 设置加载更多的footer
     * @param footer footer
     */
    public void setLoadMoreFooter(LoadMoreFooter footer) {
        if (footer != null && loadMoreFooter != footer) {
            removeFooterView(footer);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            footer.setLayoutParams(lp);
            footer.bindLoading(this);
            addFooterView(footer);
            this.loadMoreFooter = footer;
        }
    }

    /**
     * 刷新侦听
     */
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    /**
     * 加载更多侦听
     */
    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    /**
     * 当数据加载完毕后，需要调用此方法让header回到原位
     * @param success 刷新成功，刷新失败（header的状态不一致：complete或者failure）
     */
    public void refreshComplete(boolean success) {
        refreshHeader.setRefreshComplete(success);

        // 刷新成功后，将failure状态重置
        if (success && loadMoreFooter.getState() == LoadMoreFooter.State.FAILURE) {
            loadMoreFooter.changeState(LoadMoreFooter.State.RESET);
        }

        // 刷新成功后，如果需要加载更多，那么回调onLoadMore
        if (waitingRefreshCompletedToLoadMore) {
            onLoadMoreCallBack();
            waitingRefreshCompletedToLoadMore = false;
        }
    }

    /**
     * 当数据加载完毕后，需要调用此方法重置footer的状态
     * @param success 刷新成功：隐藏了。  刷新失败，会显示失败状态（点击重试）
     */
    public void loadMoreComplete(boolean success) {
        loadMoreFooter.setLoadMoreComplete(success);
        Log.e("123123", "loadMoreComplete");
        // 加载成功后，如果需要刷新，那么回调onRefresh
        if (waitingLoadingCompletedToRefresh) {
            onRefreshCallBack();
            waitingLoadingCompletedToRefresh = false;
        }
    }

    /**
     * 自动刷新
     */
    public void autoRefresh() {
        refreshHeader.autoRefresh();
    }

    /**
     * 当刷新和加载更多同时进行时，可能会导致分页加载时的页面错误。<br/>
     * 可以通过此方法设置处理方式：<br/>
     * NORMAL: 不做处理，但是开发者必须自己处理。<br/>
     * ENQUEUE: 队列模式，刷新和加载更多同时进行时，只会回调其中一个，成功后再回调另一个（默认ENQUEUE）
     */
    public void setCallBackMode(CallBackMode callBackMode) {
        this.callBackMode = callBackMode;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int actionMasked = ev.getActionMasked();

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = ev.getPointerId(0);
                lastY = ev.getY(0);
                refreshHeader.onPress();
                loadMoreFooter.onPress();
                break;
            case MotionEvent.ACTION_MOVE:
                float y = ev.getY(MotionEventCompat.findPointerIndex(ev, activePointerId));
                float diffY = y - lastY;
                lastY = y;

                // --------------header----------------------
                if (getFirstVisiblePosition() == 0 && diffY > 0) {
                    refreshHeader.onMove(diffY);
                } else if (diffY < 0 && refreshHeader.getVisibleHeight() > 0) {
                    // 往上滑动，header回去的过程中，listView也会跟着滚动,导致滑动致header消失后，其实header高度还没有到0
                    // setSelection可以保证header一直处于顶部
                    refreshHeader.onMove(diffY);
                    setSelection(0);
                }

                // --------------footer--------------------
                int lastVisible = getLastVisiblePosition();
                int lastPosition = getAdapter().getCount() - 1;
                if (getFirstVisiblePosition() != 0 && lastVisible == lastPosition) {
                    loadMoreFooter.onMove(diffY);
                }


                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                onSecondaryPointerDown(ev);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                refreshHeader.onRelease();
                loadMoreFooter.onRelease();
                break;
        }
        return super.onTouchEvent(ev);
    }

    private void onSecondaryPointerDown(MotionEvent ev) {
        int pointerIndex = MotionEventCompat.getActionIndex(ev);
        lastY = ev.getY(pointerIndex);
        activePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == activePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            lastY = ev.getY(newPointerIndex);
            activePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * Refreshing接口的方法， 当header进入刷新状态时会回调此方法
     */
    @Override
    public void onRefreshCallBack() {
        if (onRefreshListener != null) {
            if (loadMoreFooter.getState() == LoadMoreFooter.State.LOADING
                    && callBackMode == CallBackMode.ENQUEUE) {
                // 等待加载更多完毕后回调onRefresh方法
                waitingLoadingCompletedToRefresh = true;
            } else {
                onRefreshListener.onRefresh();
            }
        }
    }

    /**
     * Loading接口的方法，当footer进入加载状态时会回调此方法
     */
    @Override
    public void onLoadMoreCallBack() {
        if (onLoadMoreListener != null) {
            if (refreshHeader.getState() == RefreshHeader.State.REFRESHING
                    && callBackMode == CallBackMode.ENQUEUE) {
                // 等待刷新完毕后回调onLoadMore方法
                waitingRefreshCompletedToLoadMore = true;
            } else {
                onLoadMoreListener.onLoadMore();
            }
        }
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        this.scrollListener = l;
    }

    /**
     * onScrollStateChanged和onScroll的区别：
     * <p/>
     * ListView滚动时，会一直执行onScroll。 而scrollState只会执行三次
     * <p/>
     * 在初始化时， onScroll会执行， 而ScrollState不会
     * <p/>
     * 在顶部或者底部滑动时，onScroll不会执行， 而ScrollState执行
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollListener != null) {
            scrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (scrollListener != null) {
            scrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
        int lastVisiblePosition = getLastVisiblePosition();
        int count = totalItemCount - getHeaderViewsCount() - getFooterViewsCount();
        if (loadMoreFooter != null) {
            loadMoreFooter.onScroll(lastVisiblePosition, count);
        }
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        /*
         * 在加载更多成功的那一刻点击item，可能会报空指针
         * 因为此时loadMoreFooter这个item已经从listView中暂时移除
         * 所以在这里做了parent判断
         */
        int footerPosition = -1;
        ViewParent viewParent = loadMoreFooter.getParent();
        if(viewParent != null) {
            footerPosition = getPositionForView(loadMoreFooter);
            if(position == footerPosition) {
                loadMoreFooter.onItemClick();
            }
        }

        if(onItemClickListener != null && position != footerPosition) {
            onItemClickListener.onItemClick(parent, view, position, id);
        }
    }
}
