package com.eg.videouploader;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;

public class MainActivity extends AppCompatActivity {
    private TextView tv_info;

    private static final int RQ_PICK_VIDEO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_info = findViewById(R.id.tv_info);

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
                String videoId = IdUtil.fastSimpleUUID();
                File videoFile = new File(getCacheDir(), videoId + "." + ext);
                IoUtil.copy(inputStream, new FileOutputStream(videoFile));
                handleVideoFile(videoFile, videoId);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void transcodeVideo(File sourceFile, String videoId) {
        //-i "/data/user/0/com.eg.videouploader/cache/16cc33a300fb4e348741d99f9ccbd25a.mp4"
        // -codec copy -vbsf h264_mp4toannexb -map 0
        // -f segment -segment_list "/data/user/0/com.eg.videouploader/cache/16cc33a300fb4e348741d99f9ccbd25a.mp4.m3u8"
        // -segment_time 1 "/data/user/0/com.eg.videouploader/cache/bca67fbc59de42fcbc97a58491d0f6c9-%d.ts"

        File baseFolder = sourceFile.getParentFile();
        File transcodeFolder = new File(baseFolder, "transcode");
        File m3u8File = new File(transcodeFolder, videoId + ".m3u8");
        String cmd = "-i \"" + sourceFile.getAbsolutePath()
                + "\" -codec copy -vbsf h264_mp4toannexb -map 0 -f segment -segment_list \""
                + m3u8File.getAbsolutePath() + " -segment_time 1 \""
                + transcodeFolder.getAbsolutePath() + File.separator + videoId + "-%d.ts\"";
        tv_info.setText(cmd);
        FFmpeg.execute(cmd);
    }

    private void handleVideoFile(File sourceFile, String videoId) {
        transcodeVideo(sourceFile, videoId);
        new Thread(() -> {
//            BosUtil.uploadObjectStorage(videoFile, "test/" + videoFile.getName());
//            videoFile.delete();
        }).start();
    }
}