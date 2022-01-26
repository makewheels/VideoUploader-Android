package com.eg.videouploader;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ZipUtil;

public class MainActivity extends AppCompatActivity {
    private TextView tv_info;

    private static final int RQ_PICK_VIDEO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_info = findViewById(R.id.tv_info);

        ensureFfmpeg();

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.INTERNAL_CONTENT_URI);

        startActivityForResult(intent, RQ_PICK_VIDEO);
        FileUtil.del(getCacheDir());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        if (requestCode == RQ_PICK_VIDEO) {
            Uri uri = intent.getData();

            ContentResolver contentResolver = getContentResolver();
            String type = contentResolver.getType(uri);
            String ext = type.split("/")[1];

            InputStream inputStream;
            try {
                inputStream = contentResolver.openInputStream(uri);
                File videoFile = new File(getCacheDir(), IdUtil.fastSimpleUUID() + "." + ext);
                IoUtil.copy(inputStream, new FileOutputStream(videoFile));
                handleVideoFile(videoFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void ensureFfmpeg() {
        File filesDir = getFilesDir();
        File ffmpegFile = new File(filesDir, "ffmpeg");
        if (!ffmpegFile.exists()) {
            AssetManager assetManager = getAssets();
            try {
                InputStream ffmpegInputStream = assetManager.open("ffmpeg.zip");
                File ffmpegZip = new File(filesDir, "ffmpeg.zip");
                IoUtil.copy(ffmpegInputStream, new FileOutputStream(ffmpegZip));
                ZipUtil.unzip(ffmpegZip, filesDir);
                ffmpegZip.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File ffprobeFile = new File(filesDir, "ffprobe");
        if (!ffprobeFile.exists()) {
            AssetManager assetManager = getAssets();
            try {
                InputStream ffprobeInputStream = assetManager.open("ffprobe.zip");
                File ffprobeZip = new File(filesDir, "ffprobe.zip");
                IoUtil.copy(ffprobeInputStream, new FileOutputStream(ffprobeZip));
                ZipUtil.unzip(ffprobeZip, filesDir);
                ffprobeZip.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void transcodeVideo() {
        Log.e("tag", Arrays.toString(Build.SUPPORTED_ABIS));
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        String[] cmd = {"-version"};
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
                @Override
                public void onProgress(String message) {
                    Log.e("tag", message);
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    private void handleVideoFile(File videoFile) {
        transcodeVideo();
        new Thread(() -> {
//            BosUtil.uploadObjectStorage(videoFile, "test/" + videoFile.getName());
//            videoFile.delete();
        }).start();
    }
}