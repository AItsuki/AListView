package com.aitsuki.alistview.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Scroller;

/**
 * Created by AItsuki on 2016/6/23.
 * 逻辑：
 * 拉到最底部的时候显示footer -- 从reset进入loading状态
 * 加载成功后，隐藏footer
 * 加载失败，点击footer可以重新加载
 * <p/>
 * 1. loading失败 -- 到failure状态
 * 2. loading成功 -- 到Reset状态，隐藏footer
 */
public abstract class LoadMoreFooter extends LinearLayout {


    private static final float DRAG_RATE = 0.5f;
    private final AutoScroll autoScroll;

    private View contentView;   // footer
    private State state = State.RESET;
    private final int footerHeight;
    private Loading loading;
    private boolean canLoadMore = true;

    public LoadMoreFooter(Context context) {
        super(context);
        autoScroll = new AutoScroll();
        contentView = getContentView();
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(contentView, lp);
        measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerHeight = getMeasuredHeight();
    }

    public void bindLoading(Loading loading) {
        this.loading = loading;
    }

    public void setVisibleHeight(int height) {
        LayoutParams lp = (LayoutParams) contentView.getLayoutParams();
        lp.height = height;
        contentView.setLayoutParams(lp);
    }

    public int getVisibleHeight() {
        return contentView.getLayoutParams().height;
    }

    public int getFooterHeight() {
        return footerHeight;
    }

    public void setLoadMoreComplete(boolean successful) {
        if (successful) {
            changeState(State.RESET);
        } else {
            changeState(State.FAILURE);
        }
    }

    public void onScroll(int lastVisiblePosition, int count) {
        // 当没有数据的时候，不显示footer
        if(count <= 0) {
            contentView.setVisibility(GONE);
        } else if(count > 0 && contentView.getVisibility() == GONE) {
            contentView.setVisibility(VISIBLE);
        }

        if (count > 0 && lastVisiblePosition >= count && state == State.RESET && canLoadMore) {
            changeState(LoadMoreFooter.State.LOADING);
        }
    }

    public void onPress() {

    }

    public void onRelease() {
        int height = canLoadMore ? footerHeight : 0;
        if(getVisibleHeight() > height) {
            autoScroll.scrollTo(height, 250);
        }
    }

    public State getState() {
        return state;
    }

    public void changeState(State state) {
        this.state = state;
        switch (state) {
            case RESET:
                onReset();
                break;
            case LOADING:
                onLoading();
                if (loading != null) {
                    loading.onLoadMoreCallBack();
                }
                break;
            case FAILURE:
                onFailure();
                break;
            case NOMORE:
                onNoMore();
                break;
        }
    }

    public void onMove(float offset) {
        if (offset == 0) {
            return;
        }

        if (offset < 0) {
            offset = offset * DRAG_RATE;
        }

        // 往上滑动的时候才增加footer的高度，所以offset为负数。
        int height = canLoadMore ? footerHeight : 0;
        int target = (int) Math.max(height, getVisibleHeight() - offset);
        setVisibleHeight(target);
    }

    public void setCanLoadMore(boolean canLoadMore) {
        // 设置不能加载更多
        if(!canLoadMore && state != State.NOMORE) {
            changeState(State.NOMORE);
            setVisibleHeight(0);
            this.canLoadMore = false;
        } else if(canLoadMore && state == State.NOMORE) {
            changeState(State.RESET);
            setVisibleHeight(footerHeight);
            this.canLoadMore = true;
        }
    }

    private class AutoScroll implements Runnable {
        private Scroller scroller;
        private int lastY;

        public AutoScroll() {
            scroller = new Scroller(getContext());
        }

        @Override
        public void run() {
            boolean finished = !scroller.computeScrollOffset() || scroller.isFinished();
            if (!finished) {
                int currY = scroller.getCurrY();
                int offset = currY - lastY;
                lastY = currY;
                onMove(offset);
                post(this);
            } else {
                stop();
            }
        }

        public void scrollTo(int to, int duration) {
            int from = getVisibleHeight();
            int distance =  to - from;
            stop();
            if (distance == 0) {
                return;
            }
            scroller.startScroll(0, 0, 0, -distance, duration);
            post(this);
        }

        private void stop() {
            removeCallbacks(this);
            if (!scroller.isFinished()) {
                scroller.forceFinished(true);
            }
            lastY = 0;
        }
    }
    public void onItemClick() {
        if(state == State.FAILURE) {
            changeState(State.LOADING);
        }
    }

    public enum State {
        RESET, LOADING, FAILURE, NOMORE
    }

    public abstract View getContentView();

    public abstract void onReset();

    protected abstract void onNoMore();

    public abstract void onLoading();

    public abstract void onFailure();
}
