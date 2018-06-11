package com.steadyoung.ioc;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * File description.
 *
 * @author wayne
 * @date 2018/6/7
 */
public class SteadyoungIOC {
    //activity使用
    public static void inject(Activity activity){
        inject(new ViewFinder(activity),activity);
    }

    //View使用
    public static void inject(View view){
        inject(new ViewFinder(view),view);
    }

    //Fragment使用
    public static void inject(View view,Object object){
        inject(new ViewFinder(view),object);
    }

    //兼容上述三种方式
    private static void inject(ViewFinder finder,Object object){
        injectField(finder,object);
        injectEvent(finder,object);
    }

    /**
     * 注入属性
      * @param finder
     * @param object
     */
    private static void injectField(ViewFinder finder, Object object) {
        //1.获取类里面的所有属性
        Class<?> clazz = object.getClass();
        //获取所有属性包括私有和公有
        Field[] fields = clazz.getDeclaredFields();
        //2.获取ViewById的里面的value值
        for(Field field : fields){
            FindView viewById = field.getAnnotation(FindView.class);
            if(viewById != null){
                int viewId = viewById.value();
                //3.findViewById找到View
                View view = finder.findViewById(viewId);
                if (view != null) {
                    // 4. 反射注入View属性
                    // 设置所有属性都能注入包括私有和公有
                    field.setAccessible(true);
                    try {
                        field.set(object, view);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    throw new RuntimeException("Invalid @ViewInject for "
                            + clazz.getSimpleName() + "." + field.getName());
                }
            }
        }

    }

    /**
     * 注入事件
     * @param finder
     * @param object
     */
    private static void injectEvent(ViewFinder finder, Object object) {
        //TODO
        //1.获取类里面的所有方法
        Class<?> clazz = object.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        //2.获取OnClick的里面的Value值
        for ( Method method: methods ) {
            OnClick onClick = method.getAnnotation(OnClick.class);
            if(onClick != null){
                int[] viewIds = onClick.value();
                for( int viewId : viewIds){
                    //3.findViewById找到View
                    View view = finder.findViewById(viewId);
                    //判断是否有检测网络需求
                    boolean isCheckNet = method.getAnnotation(CheckNet.class) != null;
                    String message = null;
                    if(isCheckNet){
                        //获取无网络提示信息
                        message = method.getAnnotation(CheckNet.class).value();
                    }
                    //4.view.setOnClickListener
                    view.setOnClickListener(new DeclaredOnClickListener(method,object,isCheckNet,message));
                }
            }
        }
    }

    private static class DeclaredOnClickListener implements View.OnClickListener {
        private Method mMethod;
        private Object mObject;
        private boolean isCheckNet;
        private String message;

        public DeclaredOnClickListener(Method mMethod, Object mObject, boolean isCheckNet, String message) {
            this.mMethod = mMethod;
            this.mObject = mObject;
            this.isCheckNet = isCheckNet;
            this.message = message;
        }

        @Override
        public void onClick(View v) {
            if(isCheckNet){
                Log.d("isCheckNet", "onClick:111111111111111111 ");
                if(!isNetworkConnected(v.getContext())){
                    Toast.makeText(v.getContext(), TextUtils.isEmpty(message) ? "网络不给力，请检查网络连接！" : message ,Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            try {
                //打开权限
                mMethod.setAccessible(true);
                mMethod.invoke(mObject,v);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    mMethod.invoke(mObject,null);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     *获取网络连接状态
     * @param context
     * @return true网络连接正常 false无网络
     */
    private static boolean isNetworkConnected(Context context){
        if(context != null){
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo != null){
                return networkInfo.isAvailable();
            }
        }
        return false;
    }
}
