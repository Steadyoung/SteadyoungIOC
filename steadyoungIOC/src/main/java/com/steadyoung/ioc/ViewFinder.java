package com.steadyoung.ioc;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.view.View;

/**
 * File description.
 *
 * @author wayne
 * @date 2018/6/7
 */
public class ViewFinder {
    private Activity mActivity;
    private View mView;

    public ViewFinder(Activity activity) {
        this.mActivity = activity;
    }

    public ViewFinder(View view) {
        this.mView = view;
    }

    /**
     * 判断容器然后 根据容器和控件ID获取控件View
     * @param viewId
     * @return
     */
    public View findViewById(@IdRes int viewId){
        return mActivity!= null ? mActivity.findViewById(viewId) : mView.findViewById(viewId);
    }
}
