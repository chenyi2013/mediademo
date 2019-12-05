package com.surprise.videoviewdemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_media_player.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MediaPlayerActivity : AppCompatActivity(), MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnBufferingUpdateListener {

    private var seekBarAutoFlag: Boolean = false
    private var videoTimeString: String = ""
    private var playPosition = 0
    private var surfaceHolder: SurfaceHolder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var hasPrepared = false


    /**
     * 滑动条变化线程
     */
    private val runnable = Runnable {
        // 增加对异常的捕获，防止在判断mediaPlayer.isPlaying的时候，报IllegalStateException异常
        try {
            while (seekBarAutoFlag) {
                /*
                     * mediaPlayer不为空且处于正在播放状态时，使进度条滚动。
                     * 通过指定类名的方式判断mediaPlayer防止状态发生不一致
                     */

                mediaPlayer?.apply {
                    if (null != mediaPlayer && isPlaying) {
                        seekBar.progress = currentPosition
                    }
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)
        // 设置surfaceHolder
        surfaceHolder = surfaceView.holder
        // 设置Holder类型,该类型表示surfaceView自己不管理缓存区,虽然提示过时，但最好还是要设置
        surfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        // 设置surface回调
        surfaceHolder?.addCallback(SurfaceCallback())
    }


    /**
     * 播放视频
     */
    fun playVideo() {
        // 初始化MediaPlayer
        mediaPlayer = MediaPlayer()
        // 重置mediaPaly,建议在初始滑mediaplay立即调用。
        mediaPlayer?.reset()
        // 设置声音效果
        mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        // 设置播放完成监听
        mediaPlayer?.setOnCompletionListener(this)
        // 设置媒体加载完成以后回调函数。
        mediaPlayer?.setOnPreparedListener(this)
        // 错误监听回调函数
        mediaPlayer?.setOnErrorListener(this)
        // 设置缓存变化监听
        mediaPlayer?.setOnBufferingUpdateListener(this)
        val url = "android.resource://" + packageName + "/" + R.raw.aa
        val uri = Uri.parse(url)
        try {
            // mediaPlayer.reset();
            mediaPlayer?.setDataSource(this, uri)
            // 设置异步加载视频，包括两种方式 prepare()同步，prepareAsync()异步
            mediaPlayer?.prepareAsync()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }


    /**
     * 视频加载完毕监听
     *
     * @param mp
     */

    override fun onPrepared(mp: MediaPlayer) {
        // 当视频加载完毕以后，隐藏加载进度条
        hasPrepared = true
        progressBar.visibility = View.GONE
        // 判断是否有保存的播放位置,防止屏幕旋转时，界面被重新构建，播放位置丢失。
        if (playPosition >= 0) {
            mediaPlayer?.seekTo(playPosition)
            playPosition = -1
            // surfaceHolder.unlockCanvasAndPost(Constants.getCanvas());
        }
        // 播放视频
        mediaPlayer?.start()
        // 设置显示到屏幕
        mediaPlayer?.setDisplay(surfaceHolder)
        // 设置surfaceView保持在屏幕上
        mediaPlayer?.setScreenOnWhilePlaying(true)
        surfaceHolder?.setKeepScreenOn(true)
        // 设置控制条,放在加载完成以后设置，防止获取getDuration()错误
        seekBar.setProgress(0)

        mediaPlayer?.apply {
            seekBar.max = duration
            // 设置播放时间
            videoTimeString = getShowTime(duration.toLong())
            vedioTiemTextView.text = "00:00:00/$videoTimeString"
        }


        // 设置拖动监听事件
        seekBar.setOnSeekBarChangeListener(SeekBarChangeListener())
        // 设置按钮监听事件
        // 重新播放
        replayButton.setOnClickListener {
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()

        }
        // 暂停和播放
        playButton.setOnClickListener {
            mediaPlayer?.apply {
                if (isPlaying) {
                    mediaPlayer?.pause()
                } else {
                    mediaPlayer?.start()
                }
            }

        }

        videoSizeButton.setOnClickListener {
            changeVideoSize()
        }
        // 截图按钮
        screenShotButton.setOnClickListener {
            savaScreenShot(playPosition.toLong())
        }
        seekBarAutoFlag = true
        // 开启线程 刷新进度条

       Thread(runnable).start()

    }


    // SurfaceView的callBack
    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            // SurfaceView的大小改变
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            // surfaceView被创建
            // 设置播放资源
            playVideo()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            // surfaceView销毁
            // 如果MediaPlayer没被销毁，则销毁mediaPlayer
            if (null != mediaPlayer) {
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
    }


    /**
     * seekBar拖动监听类
     *
     * @author shenxiaolei
     */
    private inner class SeekBarChangeListener : OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (progress >= 0) {
                // 如果是用户手动拖动控件，则设置视频跳转。
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
                playPosition = progress
                // 设置当前播放时间
                vedioTiemTextView.text = (getShowTime(progress.toLong()) + "/" + videoTimeString)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {

        }

    }

    /**
     * 转换播放时间
     *
     * @param milliseconds 传入毫秒值
     * @return 返回 hh:mm:ss或mm:ss格式的数据
     */
    @SuppressLint("SimpleDateFormat")
    fun getShowTime(milliseconds: Long): String {
        // 获取日历函数
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliseconds
        var dateFormat: SimpleDateFormat? = null
        // 判断是否大于60分钟，如果大于就显示小时。设置日期格式
        if (milliseconds / 60000 > 60) {
            dateFormat = SimpleDateFormat("hh:mm:ss")
        } else {
            dateFormat = SimpleDateFormat("mm:ss")
        }
        return dateFormat!!.format(calendar.getTime())
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {

        when (what) {
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> Log.e("text", "发生未知错误")
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> Log.e("text", "媒体服务器死机")
            else -> Log.e("text", "onError+$what")
        }

        when (extra) {
            MediaPlayer.MEDIA_ERROR_IO ->
                //io读写错误
                Log.e("text", "文件或网络相关的IO操作错误")
            MediaPlayer.MEDIA_ERROR_MALFORMED ->
                //文件格式不支持
                Log.e("text", "比特流编码标准或文件不符合相关规范")
            MediaPlayer.MEDIA_ERROR_TIMED_OUT ->
                //一些操作需要太长时间来完成,通常超过3 - 5秒。
                Log.e("text", "操作超时")
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED ->
                //比特流编码标准或文件符合相关规范,但媒体框架不支持该功能
                Log.e("text", "比特流编码标准或文件符合相关规范,但媒体框架不支持该功能")
            else -> Log.e("text", "onError+$extra")
        }

        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
    }


    /**
     * 获得屏幕高度,单位是px
     *
     * @param context
     * @return
     */
    fun getScreenWidth(context: Context): Int {
        val wm = context
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.widthPixels
    }

    /**
     * 获得屏幕宽度，单位是px
     *
     * @param context
     * @return
     */
    fun getScreenHeight(context: Context): Int {
        val wm = context
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.heightPixels
    }

    /**
     * 改变视频的显示大小，全屏，窗口，内容
     */
    fun changeVideoSize() {
        // 改变视频大小
        val videoSizeString = videoSizeButton.getText().toString()
        // 获取视频的宽度和高度


        val screenWidth = getScreenWidth(this)
        val screenHeight = getScreenHeight(this)

        var width = mediaPlayer?.videoWidth ?: screenWidth
        var height = mediaPlayer?.videoHeight ?: screenHeight


        // 如果按钮文字为窗口则设置为窗口模式
        if ("窗口" == videoSizeString) {
            /*
             * 如果为全屏模式则改为适应内容的，前提是视频宽高小于屏幕宽高，如果大于宽高 我们要做缩放
             * 如果视频的宽高度有一方不满足我们就要进行缩放. 如果视频的大小都满足就直接设置并居中显示。
             */


            if (width > screenWidth || height > screenHeight) {
                // 计算出宽高的倍数
                val vWidth = width.toFloat() / screenWidth as Float
                val vHeight = height.toFloat() / screenHeight as Float
                // 获取最大的倍数值，按大数值进行缩放
                val max = Math.max(vWidth, vHeight)
                // 计算出缩放大小,取接近的正值
                width = Math.ceil((width.toFloat() / max).toDouble()).toInt()
                height = Math.ceil((height.toFloat() / max).toDouble()).toInt()
            }
            // 设置SurfaceView的大小并居中显示
            val layoutParams = RelativeLayout.LayoutParams(
                width,
                height
            )
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
            surfaceView.layoutParams = layoutParams
            videoSizeButton.setText("全屏")
        } else if ("全屏" == videoSizeString) {
            // 设置全屏
            // 设置SurfaceView的大小并居中显示
            val layoutParams = RelativeLayout.LayoutParams(
                screenWidth,
                screenHeight
            )
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
            surfaceView.layoutParams = layoutParams
            videoSizeButton.setText("窗口")
        }
    }


    /**
     * 保存视频截图.该方法只能支持本地视频文件
     *
     * @param time视频当前位置
     */
    private fun savaScreenShot(time: Long) {
        // 标记是否保存成功
        var isSave = false
        // 获取文件路径
        var path: String? = null
        // 文件名称
        var fileName: String? = null
        if (time >= 0) {
            try {
                val mediaMetadataRetriever = MediaMetadataRetriever()
                val url = "android.resource://" + packageName + "/" + R.raw.aa
                mediaMetadataRetriever?.setDataSource(this, Uri.parse(url))
                // 获取视频的播放总时长单位为毫秒
                val timeString = mediaMetadataRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                // 转换格式为微秒
                val timelong = java.lang.Long.parseLong(timeString) * 1000
                // 计算当前视频截取的位置
                val index = time * timelong / (mediaPlayer?.getDuration() ?: 1)
                // 获取当前视频指定位置的截图,时间参数的单位是微秒,做了*1000处理
                // 第二个参数为指定位置，意思接近的位置截图
                val bitmap = mediaMetadataRetriever.getFrameAtTime(
                    time * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                // 释放资源
                mediaMetadataRetriever.release()
                // 判断外部设备SD卡是否存在

                // 不存在获取内部存储
                path = cacheDir.getPath()
                // 设置文件名称 ，以事件毫秒为名称
                fileName = Calendar.getInstance().timeInMillis.toString() + ".jpg"
                // 设置保存文件
                val file = File("$path/$fileName")

                if (!file.exists()) {
                    file.createNewFile()
                }
                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(CompressFormat.JPEG, 100, fileOutputStream)
                isSave = true


                if (isSave) {
                    val imageView = ImageView(this)
                    imageView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    imageView.setImageBitmap(BitmapFactory.decodeFile(path + File.separator + fileName))
                    AlertDialog.Builder(this).setView(imageView).show()
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }

}
