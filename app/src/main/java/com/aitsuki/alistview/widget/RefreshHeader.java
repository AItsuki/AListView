package com.aitsuki.alistview.widget;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Scroller;

/**
 * Created by AItsuki on 2016/6/21.
 * RefreshHeader相当于一个容器，装着我们自定义的Header
 * child通过getContentView方法将header传进来。
 */
public abstract class RefreshHeader extends LinearLayout {

    private static final String TAG = "RefreshHeader";

    private static final float DRAG_RATE = 0.5f;
    // scroller duration
    private static final int SCROLL_TO_TOP_DURATION = 800;
    private static final int SCROLL_TO_REFRESH_DURATION = 250;
    private static final long SHOW_COMPLETED_TIME = 500;
    private static final long SHOW_FAILURE_TIME = 500;

    private long showCompletedTime = SHOW_COMPLETED_TIME;
    private long showFailureTime = SHOW_FAILURE_TIME;

    private View contentView;   // header
    private int refreshHeight;  // 刷新高度
    private boolean isTouch;
    private State state = State.RESET;
    private final AutoScroll autoScroll;
    private Refreshing refreshing;

    public RefreshHeader(Context context) {
        super(context);
        autoScroll = new AutoScroll();
        // content由子类创建
        contentView = getContentView();
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        addView(contentView, lp);

        measure(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        refreshHeight = getMeasuredHeight();
        Log.e(TAG, "refreshHeight = " + refreshHeight);
    }

    public void bindRefreshing(Refreshing refreshing) {
        this.refreshing = refreshing;
    }

    public void setVisibleHeight(int height) {
        height = Math.max(0, height);
        LayoutParams lp = (LayoutParams) contentView.getLayoutParams();
        lp.height = height;
        contentView.setLayoutParams(lp);
    }

    public void setShowCompletedTime(long time) {
        if(time >= 0) {
            showCompletedTime = time;
        }
    }

    public void setShowFailureTime(long time) {
        if(time >= 0) {
            showFailureTime = time;
        }
    }


    public int getVisibleHeight() {
        return contentView.getLayoutParams().height;
    }

    public int getRefreshHeight() {
        return refreshHeight;
    }

    public State getState() {
        return state;
    }

    public boolean isTouch() {
        return isTouch;
    }

    public void onPress() {
        isTouch = true;
        removeCallbacks(delayToScrollTopRunnable);
        autoScroll.stop();
    }

    public void onMove(float offset) {
        if(offset == 0) {
            return;
        }

        if(offset > 0) {
            offset = offset * DRAG_RATE;
        }

        int target = (int) Math.max(0, getVisibleHeight() + offset);

        // 1. 在RESET状态时，第一次下拉出现header的时候，设置状态变成PULL
        if (state == State.RESET && getVisibleHeight() == 0 && target > 0) {
            changeState(State.PULL);
        }

        // 2. 在PULL或者COMPLETE状态时，header回到顶部的时候，状态变回RESET
        if (getVisibleHeight() > 0 && target <= 0) {
            if (state == State.PULL || state == State.COMPLETE || state == State.FAILURE) {
                changeState(State.RESET);
            }
        }

        // 3. 如果是从底部回到顶部的过程(往上滚动)，并且手指是松开状态, 并且当前是PULL状态，状态变成LOADING
        if (state == State.PULL && !isTouch && getVisibleHeight() > refreshHeight && target <= refreshHeight) {
            // 这时候我们需要强制停止autoScroll
            autoScroll.stop();
            changeState(State.REFRESHING);
            target = refreshHeight;
        }
        setVisibleHeight(target);
        onPositionChange();
    }

    /**
     * 松开手指，滚动到刷新高度或者滚动回顶部
     */
    public void onRelease() {
        isTouch = false;
        if (state == State.REFRESHING) {
            if (getVisibleHeight() > refreshHeight) {
                autoScroll.scrollTo(refreshHeight, SCROLL_TO_REFRESH_DURATION);
            }
        } else {
            autoScroll.scrollTo(0, SCROLL_TO_TOP_DURATION);
        }
    }

    public void setRefreshComplete(boolean success) {
        if(success) {
            changeState(State.COMPLETE);
        } else {
            changeState(State.FAILURE);
        }


        if (getVisibleHeight() == 0) {
            changeState(State.RESET);
            return;
        }

        if(!isTouch && getVisibleHeight() > 0) {
            if(success) {
                postDelayed(delayToScrollTopRunnable, showCompletedTime);
            } else {
                postDelayed(delayToScrollTopRunnable, showFailureTime);
            }
        }
    }

    private void changeState(State state) {
        Log.e(TAG, "changeState: "+ state.name());
        this.state = state;
        switch (state) {
            case RESET:
                onReset();
                break;
            case PULL:
                onPullStart();
                break;
            case REFRESHING:
                onRefreshing();
                if(refreshing != null) {
                    refreshing.onRefreshCallBack();
                }
                break;
            case COMPLETE:
                onComplete();
                break;
            case FAILURE:
                onFailure();
                break;
        }
    }

    /**
     * header的内容
     * @return view
     */
    public abstract View getContentView();

    /**
     * header回到顶部了
     */
    public abstract void onReset();

    /**
     * header被下拉了
     */
    public abstract void onPullStart();

    /**
     * header高度变化的回调
     */
    public abstract void onPositionChange();

    /**
     * header正在刷新
     */
    public abstract void onRefreshing();

    /**
     * header刷新成功
     */
    public abstract void onComplete();

    /**
     * header刷新失败
     */
    public abstract void onFailure();

    /**
     * 自动刷新
     */
    public void autoRefresh() {
        if(state != State.RESET) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                setVisibleHeight(refreshHeight);
                changeState(State.REFRESHING);
            }
        });
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
            int distance = to - from;
            stop();
            if (distance == 0) {
                return;
            }
            scroller.startScroll(0, 0, 0, distance, duration);
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

    public enum State {
        RESET, PULL, REFRESHING, FAILURE, COMPLETE
    }

    // 刷新成功，显示500ms成功状态再滚动回顶部
    private Runnable delayToScrollTopRunnable = new Runnable() {
        @Override
        public void run() {
            autoScroll.scrollTo(0, SCROLL_TO_TOP_DURATION);
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(autoScroll != null) {
            autoScroll.stop();
        }

        if (delayToScrollTopRunnable != null) {
            removeCallbacks(delayToScrollTopRunnable);
            delayToScrollTopRunnable = null;
        }

    }
}
