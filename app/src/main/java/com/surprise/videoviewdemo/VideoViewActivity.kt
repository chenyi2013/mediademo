package com.surprise.videoviewdemo

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.MediaController
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_video_view.*


class VideoViewActivity : AppCompatActivity(), MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, View.OnTouchListener {


    private var intPositionWhenPause: Int = 0

    override fun onTouch(v: View, event: MotionEvent?): Boolean {
        return v.onTouchEvent(event)
    }

    override fun onPrepared(mp: MediaPlayer?) {
        progressBar.visibility = View.GONE
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_view)

        val mediaController = MediaController(this)
        //设置videoview的控制条
        videoView.setMediaController(mediaController)
        //设置显示控制条
        mediaController.show(0)
        //设置播放完成以后监听
        videoView.setOnCompletionListener(this)
        //设置发生错误监听，如果不设置videoview会向用户提示发生错误
        videoView.setOnErrorListener(this)
        //设置在视频文件在加载完毕以后的回调函数
        videoView.setOnPreparedListener(this)
        //设置videoView的点击监听
        videoView.setOnTouchListener(this)
        //设置网络视频路径

        val url = "android.resource://" + packageName + "/" + R.raw.aa

        val uri = Uri.parse(url)
        videoView.setVideoURI(uri)
        //设置为全屏模式播放
        setVideoViewLayoutParams(0)

    }


    /**
     * 设置videiview的全屏和窗口模式
     * @param paramsType 标识 1为全屏模式 2为窗口模式
     */
    private fun setVideoViewLayoutParams(paramsType: Int) {
        //全屏模式
        if (1 == paramsType) {
            //设置充满整个父布局
            val LayoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            //设置相对于父布局四边对齐
            LayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            LayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            LayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            LayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            //为VideoView添加属性
            videoView.layoutParams = LayoutParams
        } else {
            //窗口模式
            //获取整个屏幕的宽高
            val displayMetrics = DisplayMetrics()
            this.windowManager.defaultDisplay.getMetrics(displayMetrics)
            //设置窗口模式距离边框50
            val videoHeight = displayMetrics.heightPixels - 50
            val videoWidth = displayMetrics.widthPixels - 50
            val layoutParams = RelativeLayout.LayoutParams(videoWidth, videoHeight)
            //设置居中
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
            //为VideoView添加属性
            videoView.layoutParams = layoutParams
        }
    }

    override fun onStart() {
        super.onStart()
        //启动视频播放
        videoView.start()
        //设置获取焦点
        videoView.isFocusable = true

    }


    /**
     * 页面暂停效果处理
     */
    override fun onPause() {
        super.onPause()
        //如果当前页面暂停则保存当前播放位置，全局变量保存
        intPositionWhenPause = videoView.currentPosition
        //停止回放视频文件
        videoView.stopPlayback()
    }

    /**
     * 页面从暂停中恢复
     */
    override fun onResume() {
        super.onResume()
        //跳转到暂停时保存的位置
        if (intPositionWhenPause >= 0) {
            videoView.seekTo(intPositionWhenPause)
            //初始播放位置
            intPositionWhenPause = -1
        }
    }


}
