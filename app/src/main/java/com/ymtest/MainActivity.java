package com.ymtest;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * 视频的合并与裁剪
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        findViewById(R.id.btn_mergeVideos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //合并视频
                //截取视频
                final VideoUtils videoUtils = new VideoUtils();
                videoUtils.setVideoUtilsExecuteListener(new VideoUtils.VideoUtilsExecuteListener() {
                    @Override
                    public void onPreExecute() {
                        Log.e("tag","开始了");
                    }

                    @Override
                    public void onPostExecute(String filePath) {
                        Log.e("tag","合并后的视频路径：" + filePath);
                    }

                    @Override
                    public void onerrorExecute(Exception e) {
                        Log.e("tag","合并出错了," + e.getMessage());
                    }
                });
                //需要合并的视频路径
                ArrayList<String> videosToMerge = new ArrayList<String>();
                videosToMerge.add(Environment.getExternalStorageDirectory() + "/DCIM/534741582.mp4");
                videosToMerge.add(Environment.getExternalStorageDirectory() + "/DCIM/533408809.mp4");
                videosToMerge.add(Environment.getExternalStorageDirectory() + "/DCIM/533039648.mp4");
                videoUtils.videoMerge(Environment.getExternalStorageDirectory() + "/DCIM", videosToMerge);
            }
        });


        findViewById(R.id.btn_trimmVideo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //截取视频
                final VideoUtils videoUtils = new VideoUtils();
                videoUtils.setVideoUtilsExecuteListener(new VideoUtils.VideoUtilsExecuteListener() {
                    @Override
                    public void onPreExecute() {
                        Log.e("tag","开始了");
                    }

                    @Override
                    public void onPostExecute(String filePath) {
                        Log.e("tag","剪辑后的视频路径：" + filePath);
                    }

                    @Override
                    public void onerrorExecute(Exception e) {
                        Log.e("tag","裁剪出错了," + e.getMessage());
                    }
                });
                videoUtils.videoTrimm(Environment.getExternalStorageDirectory() + "/DCIM/Camera/VID_20170707_094951.mp4",0,4);
            }
        });
    }
}
