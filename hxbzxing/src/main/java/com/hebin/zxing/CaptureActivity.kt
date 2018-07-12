package com.hebin.zxing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.*
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.hebin.zxing.camera.CameraManager
import com.hebin.zxing.camera.PlanarYUVLuminanceSource
import com.hebin.zxing.decoding.CaptureActivityHandler
import com.hebin.zxing.decoding.InactivityTimer
import com.hebin.zxing.decoding.RGBLuminanceSource
import com.hebin.zxing.decoding.Utils
import com.hebin.zxing.view.ViewfinderView
import kotlinx.android.synthetic.main.activity_capture.*
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*


/**
 * 拍照的Activity

 * @author Baozi
 */
@Suppress("DEPRECATION")
open class CaptureActivity : AppCompatActivity(), Callback {
    private var handler: CaptureActivityHandler? = null
    var viewfinderView: ViewfinderView? = null
        private set
    private var hasSurface: Boolean = false
    private var decodeFormats: Vector<BarcodeFormat>? = null
    private var characterSet: String? = null
    private var inactivityTimer: InactivityTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playBeep: Boolean = false
    private var vibrate: Boolean = false
    private var photo_path: String? = null
    private var scanBitmap: Bitmap? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        // 初始化 CameraManager
        CameraManager.init(application)
        viewfinderView = findViewById(R.id.mo_scanner_viewfinder_view)
        initView()
        hasSurface = false
        inactivityTimer = InactivityTimer(this)
    }

    private fun initView() {
    }

    internal var flag = true

    fun light() {
        if (flag == true) {
            flag = false
            // 开闪光灯
            CameraManager.get().openLight()

        } else {
            flag = true
            // 关闪光灯
            CameraManager.get().offLight()

        }

    }

    private fun photo() {

        val innerIntent = Intent() // "android.intent.action.GET_CONTENT"
        if (Build.VERSION.SDK_INT < 19) {
            innerIntent.action = Intent.ACTION_GET_CONTENT
        } else {
            innerIntent.action = Intent.ACTION_OPEN_DOCUMENT
        }
        // innerIntent.setAction(Intent.ACTION_GET_CONTENT);

        innerIntent.type = "image/*"

        val wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片")

        startActivityForResult(wrapperIntent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {

            when (requestCode) {

                REQUEST_CODE -> {

                    val proj = arrayOf(MediaStore.Images.Media.DATA)
                    // 获取选中图片的路径
                    val cursor = contentResolver.query(data.data,
                            proj, null, null, null)!!

                    if (cursor.moveToFirst()) {

                        val column_index = cursor
                                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        photo_path = cursor.getString(column_index)
                        if (photo_path == null) {
                            photo_path = Utils.getPath(applicationContext,
                                    data.data)
                            Log.i("123path  Utils", photo_path)
                        }
                        Log.i("123path", photo_path)

                    }

                    cursor.close()

                    Thread(Runnable {
                        val result = scanningImage(photo_path!!)
                        // String result = decode(photo_path);
                        if (result == null) {
                            Log.i("123", "   -----------")
                            Looper.prepare()
                            Toast.makeText(this@CaptureActivity, "图片格式有误", Toast.LENGTH_SHORT).show()
//                            ToastUtil.showToast(this@CaptureActivity, "")
                            Looper.loop()
                        } else {
                            Log.i("123result", result.toString())
                            // Log.i("123result", result.getText());
                            // 数据返回
                            val recode = recode(result.toString())
                            println("Hebin$recode")
                            resultListener?.onResult(recode)
                        }
                    }).start()
                }
            }

        }

    }

    // 解析部分图片
    protected fun scanningImage(path: String): Result? {
        if (TextUtils.isEmpty(path)) {
            return null
        }
        // DecodeHintType 和EncodeHintType
        val hints = Hashtable<DecodeHintType, String>()
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8") // 设置二维码内容的编码
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true // 先获取原大小
        scanBitmap = BitmapFactory.decodeFile(path, options)
        options.inJustDecodeBounds = false // 获取新的大小

        var sampleSize = (options.outHeight / 200.toFloat()).toInt()

        if (sampleSize <= 0)
            sampleSize = 1
        options.inSampleSize = sampleSize
        scanBitmap = BitmapFactory.decodeFile(path, options)

        // --------------测试的解析方法---PlanarYUVLuminanceSource-这几行代码对project没作功----------

        val source1 = PlanarYUVLuminanceSource(
                rgb2YUV(scanBitmap!!), scanBitmap!!.width,
                scanBitmap!!.height, 0, 0, scanBitmap!!.width,
                scanBitmap!!.height, false)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(
                source1))
        val reader1 = MultiFormatReader()
        val result1: Result
        try {
            result1 = reader1.decode(binaryBitmap)
            val content = result1.text
            Log.i("123content", content)
        } catch (e1: NotFoundException) {
            e1.printStackTrace()
        }

        // ----------------------------

        val source = RGBLuminanceSource(scanBitmap)
        val bitmap1 = BinaryBitmap(HybridBinarizer(source))
        val reader = QRCodeReader()
        try {

            return reader.decode(bitmap1, hints)

        } catch (e: NotFoundException) {

            e.printStackTrace()

        } catch (e: ChecksumException) {
            e.printStackTrace()
        } catch (e: FormatException) {
            e.printStackTrace()
        }

        return null

    }

    @SuppressLint("WrongConstant")
    override fun onResume() {
        super.onResume()
        val surfaceView = findViewById<SurfaceView>(R.id.mo_scanner_preview_view)
        val surfaceHolder = surfaceView.holder
        if (hasSurface) {
            initCamera(surfaceHolder)
        } else {
            surfaceHolder.addCallback(this)
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
        decodeFormats = null
        characterSet = null

        playBeep = true
        val audioService = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioService.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false
        }
        initBeepSound()
        vibrate = true
    }

    override fun onPause() {
        super.onPause()
        if (handler != null) {
            handler!!.quitSynchronously()
            handler = null
        }
        CameraManager.get().closeDriver()
    }

    override fun onDestroy() {
        inactivityTimer!!.shutdown()
        super.onDestroy()
    }

    private fun initCamera(surfaceHolder: SurfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder)
        } catch (ioe: IOException) {
            return
        } catch (ioe: RuntimeException) {
            return
        }

        if (handler == null) {
            handler = CaptureActivityHandler(this, decodeFormats,
                    characterSet)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int,
                                height: Int) {

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!hasSurface) {
            hasSurface = true
            initCamera(holder)
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        hasSurface = false

    }

    fun getHandler(): Handler {
        return handler!!
    }

    fun drawViewfinder() {}

    fun handleDecode(result: Result, barcode: Bitmap) {
        inactivityTimer!!.onActivity()
        playBeepSoundAndVibrate()
        val recode = recode(result.toString())
        Log.e("Hebin", recode)
        resultListener?.onResult(recode)

    }

    private fun initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            volumeControlStream = AudioManager.STREAM_MUSIC
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer!!.setOnCompletionListener(beepListener)

            val file = resources.openRawResourceFd(
                    R.raw.mo_scanner_beep)
            try {
                mediaPlayer!!.setDataSource(file.fileDescriptor,
                        file.startOffset, file.length)
                file.close()
                mediaPlayer!!.setVolume(BEEP_VOLUME, BEEP_VOLUME)
                mediaPlayer!!.prepare()
            } catch (e: IOException) {
                mediaPlayer = null
            }

        }
    }

    @SuppressLint("WrongConstant")
    private fun playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer!!.start()
        }
        if (vibrate) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VIBRATE_DURATION)
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private val beepListener = OnCompletionListener { mediaPlayer -> mediaPlayer.seekTo(0) }

    /**
     * 中文乱码
     *
     *
     * 暂时解决大部分的中文乱码 但是还有部分的乱码无法解决 .

     * @return
     */
    private fun recode(str: String): String {
        var formart = ""

        try {
            val ISO = Charset.forName("ISO-8859-1").newEncoder()
                    .canEncode(str)
            if (ISO) {
                formart = String(str.toByteArray(), charset("ISO-8859-1"))
                Log.i("1234      ISO8859-1", formart)
            } else {
                formart = str
                Log.i("1234      stringExtra", str)
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        return formart
    }

    /**
     * @param bitmap 转换的图形
     * *
     * @return YUV数据
     */
    fun rgb2YUV(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val len = width * height
        val yuv = ByteArray(len * 3 / 2)
        var y: Int
        var u: Int
        var v: Int
        for (i in 0..height - 1) {
            for (j in 0..width - 1) {
                val rgb = pixels[i * width + j] and 0x00FFFFFF

                val r = rgb and 0xFF
                val g = rgb shr 8 and 0xFF
                val b = rgb shr 16 and 0xFF

                y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128

                y = if (y < 16) 16 else if (y > 255) 255 else y
                u = if (u < 0) 0 else if (u > 255) 255 else u
                v = if (v < 0) 0 else if (v > 255) 255 else v

                yuv[i * width + j] = y.toByte()
                // yuv[len + (i >> 1) * width + (j & ~1) + 0] = (byte) u;
                // yuv[len + (i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv
    }

    fun setBackground(backgroundResource: Int) {
        mo_scanner_preview_view.setBackgroundResource(backgroundResource)
        mo_scanner_viewfinder_view.setBackgroundResource(backgroundResource)
    }

    fun setTitleView(view: View) {
        llTitle.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun setTipText(string: String) {
        tvTip.text = string
    }

    fun openLight() {
        light()
    }

    fun openPhoto() {
        photo()
    }


    fun setListener(resultListener: ResultListener){
        this.resultListener = resultListener
    }

    private var resultListener: ResultListener? = null


    interface ResultListener {
        fun onResult(result: String)
    }

    companion object {

        private val REQUEST_CODE = 234
        private val BEEP_VOLUME = 0.10f

        private val VIBRATE_DURATION = 200L
    }
}