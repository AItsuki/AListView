package com.aitsuki.alistview.widget;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.aitsuki.alistview.R;

/**
 * Created by AItsuki on 2016/6/22.
 *
 */
public class DefaultHeader extends RefreshHeader {

    private final Animation rotate_up;
    private final Animation rotate_down;
    private final Animation rotate_infinite;
    private TextView textView;
    private View arrowIcon;
    private View successIcon;
    private View loadingIcon;
    private int currentPos;
    private int lastPos;

    public DefaultHeader(Context context) {
        super(context);

        // 初始化动画
        rotate_up = AnimationUtils.loadAnimation(context, R.anim.rotate_up);
        rotate_down = AnimationUtils.loadAnimation(context, R.anim.rotate_down);
        rotate_infinite = AnimationUtils.loadAnimation(context, R.anim.rotate_infinite);
    }

    @Override
    public View getContentView() {
        View view = View.inflate(getContext(),R.layout.header_default, null);
        textView = (TextView) view.findViewById(R.id.text);
        arrowIcon = view.findViewById(R.id.arrowIcon);
        successIcon = view.findViewById(R.id.successIcon);
        loadingIcon = view.findViewById(R.id.loadingIcon);
        return view;
    }

    @Override
    public void onReset() {
        textView.setText(getResources().getText(R.string.default_header_reset));
        successIcon.setVisibility(INVISIBLE);
        arrowIcon.setVisibility(VISIBLE);
        arrowIcon.clearAnimation();
        loadingIcon.setVisibility(INVISIBLE);
        loadingIcon.clearAnimation();
    }

    @Override
    public void onPullStart() {
        lastPos = currentPos = getVisibleHeight();
    }

    @Override
    public void onPositionChange() {
        currentPos = getVisibleHeight();
        int refreshPos = getRefreshHeight();
        // 往上拉
        if (currentPos < refreshPos && lastPos >= refreshPos) {
            if (isTouch() && getState() == State.PULL) {
                textView.setText(getResources().getText(R.string.default_header_pull));
                arrowIcon.clearAnimation();
                arrowIcon.startAnimation(rotate_down);
            }
            // 往下拉
        } else if (currentPos > refreshPos && lastPos <= refreshPos) {
            if (isTouch() && getState() == State.PULL) {
                textView.setText(getResources().getText(R.string.default_header_pull_over));
                arrowIcon.clearAnimation();
                arrowIcon.startAnimation(rotate_up);
            }
        }
        lastPos = currentPos;
    }

    @Override
    public void onRefreshing() {
        arrowIcon.setVisibility(INVISIBLE);
        loadingIcon.setVisibility(VISIBLE);
        textView.setText(getResources().getText(R.string.default_header_refreshing));
        arrowIcon.clearAnimation();
        loadingIcon.startAnimation(rotate_infinite);
    }

    @Override
    public void onComplete() {
        loadingIcon.setVisibility(INVISIBLE);
        loadingIcon.clearAnimation();
        successIcon.setVisibility(VISIBLE);
        textView.setText(getResources().getText(R.string.default_header_completed));
    }

    @Override
    public void onFailure() {
        loadingIcon.setVisibility(INVISIBLE);
        loadingIcon.clearAnimation();
        textView.setText(getResources().getText(R.string.default_header_failure));
    }
}
