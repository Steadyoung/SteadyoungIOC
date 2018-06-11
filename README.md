#1 前言
上一篇文章分析了xUtils、ButterKnife注解框架的源码，这一次我们自己打造一款IOC注解框架，因为ButterKnife的源码实现难度过大，我先仿照xUtils的源码方式，反射注解实现。
虽说反射注解对性能有影响，但是影响是极小，相比I渲染和Bitmap以及Service和Handler上的内存泄露不是一个量级的，编程一开始不纠结完美化，实现这个IOC框架是为了提升自己的编码能力，也是提高自己对项目整体代码提高可控性。
#2 控件属性注入
Annotation注解需要了解[Java中Annotation用法](https://www.cnblogs.com/be-forward-to-help-others/p/6846821.html)、[Java Annotation 总结](https://www.cnblogs.com/renhui/p/5910300.html)
属性注入代码：
```
/*
 * ElementType.FIELD 代表annotation的位置
 * FIELD：属性注解
 * CONSTRUCTOR：构造器注解
 * METHOD：方法注解
 * TYPE：类上注解
 */
@Target(ElementType.FIELD)
/*
@Retention(RetentionPolicy.CLASS)什么时候生效
CLASS 编译时
RUNTIME 运行时
SOURCR 源码资源
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FindView {

    //@FindView(R.id.xxxx)
    @IdRes int value();
}
```
```
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
    }
```
```
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
```
#3 点击事件注入
我先实现setOnclickListener点击事件，其他事件后期实现。
```
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnClick {
    @IdRes int[] value();
}
```
```
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
                    //4.view.setOnClickListener
                    view.setOnClickListener(new DeclaredOnClickListener(method,object));
                }
            }
        }
    }

    private static class DeclaredOnClickListener implements View.OnClickListener {
        private Method mMethod;
        private Object mObject;

        public DeclaredOnClickListener(Method mMethod, Object mObject) {
            this.mMethod = mMethod;
            this.mObject = mObject;
        }

        @Override
        public void onClick(View v) {
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
```
#4 扩展动态检测网络注解
最后扩展动态检测网络注解，可以在点击某些按钮或者图片是需要访问网络前检测网络状态，避免打不开网页或者获取不到数据等网络问题的情况！
```
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckNet {
    String value();
}
```
```
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

```
下面放上测试代码和测试结果：
```
public class MainActivity extends AppCompatActivity {

    /****Hello World!****/
    @FindView(R.id.test_tv)
    private TextView mTestTv;
    @FindView(R.id.test_iv)
    private ImageView mTestIv;
    /****TestButton****/
    @FindView(R.id.test_btn)
    private Button mTestBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SteadyoungIOC.inject(this);
        mTestTv.setText("测试文本");
        mTestIv.setBackgroundColor(Color.RED);
        mTestBtn.setText("测试按钮");
    }

    @OnClick(R.id.test_tv)
    private void testTvClick(TextView testTv) {
        Toast.makeText(this,"点击了文字",Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.test_iv)
    private void testIvClick(ImageView testIv) {
        mTestIv.setBackgroundColor(Color.BLUE);
        Toast.makeText(this,"点击了图片",Toast.LENGTH_SHORT).show();
    }

    @CheckNet("亲！网络不给力哦！")
    @OnClick(R.id.test_btn)
    private void testBtnClick(Button testBtn) {
        Toast.makeText(this,"点击了按钮",Toast.LENGTH_SHORT).show();
    }
}
```
![steadyoungioctest.gif](https://upload-images.jianshu.io/upload_images/8541415-752def9d33018d65.gif?imageMogr2/auto-orient/strip)
![steadyoungioc.gif](https://upload-images.jianshu.io/upload_images/8541415-5f09b4db1d726c72.gif?imageMogr2/auto-orient/strip)
上图中生成代码的插件是我自己编写的后期我会写开源这个插件代码，并写博客解说

