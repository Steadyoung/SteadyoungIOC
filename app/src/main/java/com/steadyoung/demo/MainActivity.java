package com.steadyoung.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.steadyoung.ioc.CheckNet;
import com.steadyoung.ioc.FindView;
import com.steadyoung.ioc.OnClick;
import com.steadyoung.ioc.SteadyoungIOC;

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
