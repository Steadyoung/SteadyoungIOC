# SteadyoungIOC注解框架

## ![LOGO图片](https://upload.jianshu.io/collections/images/1633997/SteadyoungIOC.png)  [SteadyoungIOC注解框架专栏](https://www.jianshu.com/c/3734b4eb3d17)

博客解说：[自己简易打造的IOC注解框架：SteadyoungIOC](https://www.jianshu.com/p/0c11f3f27ddc)

注解框架配套Android Studio生成代码插件：[SteadyoungIOC-CodePlug](https://github.com/Steadyoung/SteadyoungIOC-CodePlug)

插件下载：[SteadyoungIOC-CodePlug.jar](https://raw.githubusercontent.com/Steadyoung/SteadyoungIOC-CodePlug/master/SteadyoungIOC-CodePlug.jar)

插件可以使用 Alt + Insert 智能插入 呼出，也可以使用 Ctrl + Shift + I 或者 Ctrl + Shift + Alt + I 快捷键： 

![SteadyoungIOC框架代码生成演示](https://upload-images.jianshu.io/upload_images/8541415-8ae31adc7d03241f.png)

注入属性和注入事件的部分源码：

```
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

```

使用示例：

```
    @FindView(R.id.test_btn)
    private Button mTestBtn;
    
    ...
    
    @CheckNet("亲！网络不给力哦！")
    @OnClick(R.id.test_btn)
    private void testBtnClick(Button testBtn) {
        Toast.makeText(this,"点击了按钮",Toast.LENGTH_SHORT).show();
    }   

```

我的CSDN博客：[https://blog.csdn.net/wenwins](https://blog.csdn.net/wenwins)  

我的简书博客：[https://www.jianshu.com/u/eb8a1db752af](https://www.jianshu.com/u/eb8a1db752af)
