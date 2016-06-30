package com.aitsuki.library;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;


/**
 * Created by AItsuki on 2016/6/23.
 *
 */
public class QQFooter extends LoadMoreFooter {

    private final Animation rotate_infinite;
    private View loadingIcon;
    private TextView textView;

    public QQFooter(Context context) {
        super(context);
        rotate_infinite = AnimationUtils.loadAnimation(context, R.anim.rotate_infinite);
    }

    @Override
    protected View getContentView() {
        View view = View.inflate(getContext(), R.layout.footer_default, null);
        loadingIcon = view.findViewById(R.id.loadingIcon);
        textView = (TextView) view.findViewById(R.id.text);
        onReset();
        return view;
    }

    @Override
    protected void onReset() {
        loadingIcon.clearAnimation();
        loadingIcon.setVisibility(INVISIBLE);
        textView.setVisibility(VISIBLE);
        textView.setText(getResources().getString(R.string.default_footer_reset));
    }

    @Override
    protected void onNoMore() {
        loadingIcon.clearAnimation();
        loadingIcon.setVisibility(INVISIBLE);
        textView.setVisibility(VISIBLE);
        textView.setText(getResources().getString(R.string.default_footer_no_more));
    }

    @Override
    protected void onLoading() {
        loadingIcon.startAnimation(rotate_infinite);
        loadingIcon.setVisibility(VISIBLE);
        textView.setVisibility(VISIBLE);
        textView.setText(getResources().getString(R.string.default_footer_loading));
    }

    @Override
    protected void onFailure() {
        loadingIcon.clearAnimation();
        loadingIcon.setVisibility(INVISIBLE);
        textView.setText(getResources().getString(R.string.default_footer_failure));
    }
}
