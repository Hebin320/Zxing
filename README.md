# 前言
二维码扫描的功能在很多APP上都会出现，较为常用的第三方库是zxing，zxing很强大，但是有时候我们并不需要那么复杂的功能，只需要简单的扫描、生成以及处理扫描结果，一般都是通过重写几个类来实现项目需求。我开发了几个项目都用到了扫描二维码的功能，第一个项目，写了个完整的功能，之后的项目都是从第一个项目里面复制代码的，几次之后，觉得有点繁琐，所以就单独封装成一个项目，传到jcenter上，以后再遇到扫描二维码的功能，只需要在gradle导入，即可实现快速开发了。

<h3>导入</h3>

```
compile 'com.hebin:hxbrzxing:1.0.1'
```
<h3>使用</h3>
导入库之后，就可以使用扫描二维码的功能了，只需要新建一个activity，然后继承CaptureActivity即可。

```
class MainActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}

```
<h3>自定义使用</h3>

这里提供了几个可以自定义的属性；

 **1. 标题栏自定义**

```
class MainActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(this@MainActivity).inflate(R.layout.simple_title,null)
        setTitleView(view)
    }
}

```
 **2. 背景图片自定义**
 

```
class MainActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBackground(R.mipmap.ic_launcher)
    }
}

```
 **3. 提示文字自定义**
 

```
class MainActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTipText("请扫描二维码")
    }
}
```
 **4. 附加功能**
 

```
class MainActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view= LayoutInflater.from(this@MainActivity).inflate(R.layout.simple_title,null)
        // 打开相册，识别图片二维码
        view.tvTitle.setOnClickListener { openPhoto() }
        // 打开闪光灯
        view.tvTitle.setOnClickListener { openLight() }
        setTitleView(view)
    }

}
```
<h3>扫描结果处理</h3>
继承CaptureActivity.ResultListener，并且在oncreat里面，写上setListener(this)即可实现监听，然后在onResult里面做逻辑处理。

```
class MainActivity : CaptureActivity(), CaptureActivity.ResultListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setListener(this)
    }

    override fun onResult(result: String) {
        if (result.contains("http")) {
            Toast.makeText(this@MainActivity, "跳转到网页", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "这个二维码不是网页", Toast.LENGTH_SHORT).show()
        }
    }
}
```
# 后话
至此，只要通过简单的几行代码就实现扫描二维码的功能，而且这个扫描二维码的功能，是支持连续扫描的，不需要退出重新进入即可再次扫描。项目源码已经传到[github][1]上了。

[1]: https://github.com/Hebin320/Zxing

