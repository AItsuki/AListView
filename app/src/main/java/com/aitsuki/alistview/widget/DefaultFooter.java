package com.aitsuki.alistview.widget;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.aitsuki.alistview.R;

/**
 * Created by AItsuki on 2016/6/23.
 *
 */
public class DefaultFooter extends LoadMoreFooter {

    private final Animation rotate_infinite;
    private View loadingIcon;
    private TextView textView;

    public DefaultFooter(Context context) {
        super(context);
        rotate_infinite = AnimationUtils.loadAnimation(context, R.anim.rotate_infinite);
    }

    @Override
    public View getContentView() {
        View view = View.inflate(getContext(), R.layout.footer_default, null);
        loadingIcon = view.findViewById(R.id.loadingIcon);
        textView = (TextView) view.findViewById(R.id.text);
        onReset();
        return view;
    }

    @Override
    public void onReset() {
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
    public void onLoading() {
        loadingIcon.startAnimation(rotate_infinite);
        loadingIcon.setVisibility(VISIBLE);
        textView.setVisibility(VISIBLE);
        textView.setText(getResources().getString(R.string.default_footer_loading));
    }

    @Override
    public void onFailure() {
        loadingIcon.clearAnimation();
        loadingIcon.setVisibility(INVISIBLE);
        textView.setText(getResources().getString(R.string.default_footer_failure));
    }
}
