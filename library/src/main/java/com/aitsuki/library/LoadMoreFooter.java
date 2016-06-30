package com.aitsuki.library;

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
    private final int maxDragDistance;

    private View contentView;   // footer
    private State state = State.RESET;
    private final int footerHeight;
    private Loading loading;
    private boolean canLoadMore = true;

    public LoadMoreFooter(Context context) {
        super(context);
        maxDragDistance = Utils.dip2px(context, 500); // 默认500dp
        autoScroll = new AutoScroll();
        contentView = getContentView();
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(contentView, lp);
        measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerHeight = getMeasuredHeight();
    }

    protected void bindLoading(Loading loading) {
        this.loading = loading;
    }

    protected void setVisibleHeight(int height) {
        LayoutParams lp = (LayoutParams) contentView.getLayoutParams();
        lp.height = height;
        contentView.setLayoutParams(lp);
    }

    public int getVisibleHeight() {
        return contentView.getLayoutParams().height;
    }

    protected void setLoadMoreComplete(boolean successful) {
        if (successful) {
            changeState(State.RESET);
        } else {
            changeState(State.FAILURE);
        }
    }

    protected void onScroll(int lastVisiblePosition, int count) {
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

    protected void onPress() {
        autoScroll.stop();
    }

    protected void onRelease() {
        int height = canLoadMore ? footerHeight : 0;
        if(getVisibleHeight() > height) {
            autoScroll.scrollTo(height, 250);
        }
    }

    public State getState() {
        return state;
    }

    protected void changeState(State state) {
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
            case NO_MORE:
                onNoMore();
                break;
        }
    }

    protected void onMove(float offset) {
        if (offset == 0) {
            return;
        }

        float dragRate = DRAG_RATE;
        if (offset < 0) {
            offset = offset * dragRate - 0.5f;
            if(getVisibleHeight() > footerHeight) {
                // 因为忽略了refreshHeight，所以这里会超出最大高度，直接价格判断省事。
                if(getVisibleHeight() >= maxDragDistance) {
                    offset = 0;
                } else {
                    float extra = getVisibleHeight() - footerHeight;
                    float extraPercent = Math.min(1, extra / maxDragDistance);
                    dragRate = Utils.calcDragRate(extraPercent);
                    offset = offset * dragRate - 0.5f;
                }
            }
        }

        // 往上滑动的时候才增加footer的高度，所以offset为负数。
        int height = canLoadMore ? footerHeight : 0;
        int target = (int) Math.max(height, getVisibleHeight() - offset);
        setVisibleHeight(target);
    }

    protected void setCanLoadMore(boolean canLoadMore) {
        // 设置不能加载更多
        if(!canLoadMore && state != State.NO_MORE) {
            changeState(State.NO_MORE);
            setVisibleHeight(0);
            this.canLoadMore = false;
        } else if(canLoadMore && state == State.NO_MORE) {
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (autoScroll != null) {
            autoScroll.stop();
        }
    }

    protected void onItemClick() {
        if(state == State.FAILURE) {
            changeState(State.LOADING);
        }
    }

    public enum State {
        RESET, LOADING, FAILURE, NO_MORE
    }

    protected abstract View getContentView();

    protected abstract void onReset();

    protected abstract void onNoMore();

    protected abstract void onLoading();

    protected abstract void onFailure();
}
