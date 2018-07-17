package com.hebin

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.hebin.zxing.CaptureActivity
import kotlinx.android.synthetic.main.simple_title.view.*

class MainActivity : CaptureActivity(), CaptureActivity.ResultListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(this@MainActivity).inflate(R.layout.simple_title, null)
        // 打开相册，识别图片二维码
        view.ivBack.setOnClickListener { openPhoto() }
        // 打开闪光灯
        view.tvTitle.setOnClickListener { openLight() }
        // 设置标题啦
        setTitleView(view)
        // 设置提示文字
        setTipText("请对准二维码扫描")
        // 设置背景图片
//        setBackground(int)
        // 返回结果监听
        setListener(this)
    }

    override fun onResult(result: String) {
        Toast.makeText(this@MainActivity, "扫描结果是：“$result”", Toast.LENGTH_SHORT).show()
    }


}

