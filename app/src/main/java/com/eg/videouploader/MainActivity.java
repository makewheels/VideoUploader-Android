package com.eg.videouploader;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;

public class MainActivity extends AppCompatActivity {
    private TextView tv_info;

    private static final int RQ_PICK_VIDEO = 1;

    private static final int HW_SET_TEXT = 1;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            if (what == HW_SET_TEXT) {
                tv_info.setText((String) msg.obj);
            }
        }
    };

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
                tv_info.setText(uri.toString() + "\n" + videoFile.getPath());
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
        if (!transcodeFolder.exists()) {
            transcodeFolder.mkdirs();
        }
        File m3u8File = new File(transcodeFolder, videoId + ".m3u8");
        String cmd = "-i \"" + sourceFile.getAbsolutePath()
                + "\" -codec copy -vbsf h264_mp4toannexb -map 0 -f segment -segment_list \""
                + m3u8File.getAbsolutePath() + "\" -segment_time 1 \""
                + transcodeFolder.getAbsolutePath() + File.separator + videoId + "-%d.ts\"";
        tv_info.setText(cmd);
        FFmpeg.execute(cmd);
    }

    public static String getObjectStoragePrefix(String videoId) {
        //准备对象存储前缀
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        String monthString = month + "";
        if (month <= 9) {
            monthString = "0" + month;
        }
        return "test/videos/" + year + "-" + monthString + "/" + videoId + "/";
    }

    private void handleVideoFile(File sourceFile, String videoId) {
        transcodeVideo(sourceFile, videoId);
        new Thread(() -> {
            File transcodeFolder = new File(sourceFile.getParentFile(), "transcode");
            int tsAmount;
            String m3u8FileUrl = null;
            File[] files = transcodeFolder.listFiles();
            tsAmount = files.length - 1;
            for (File file : files) {
                //上传对象存储
                String objectKey = getObjectStoragePrefix(videoId) + file.getName();
                Message message = new Message();
                message.what = HW_SET_TEXT;
                message.obj = objectKey;
                handler.sendMessage(message);
                String url = BosUtil.uploadObjectStorage(file, objectKey);
                if (file.getName().endsWith("m3u8")) {
                    m3u8FileUrl = url;
                }
                file.delete();
            }
            sourceFile.delete();
            //发请求到我的服务器，新建视频
            String sourceFileName = sourceFile.getName();
            String body = "password=N9Q0HsaSniSNiQ94"
                    + "&videoId=" + videoId
                    + "&type=hls"
                    + "&playFileUrl="
                    + "&m3u8FileUrl=" + m3u8FileUrl
                    + "&tsAmount=" + tsAmount
                    + "&videoFileFullName=" + sourceFileName
                    + "&videoFileBaseName=" + FileNameUtil.mainName(sourceFileName)
                    + "&videoFileExtension=" + FileNameUtil.extName(sourceFileName);
            String watchUrl = HttpUtil.post("https://www.itube.work/notifyNewVideo", body);
            Message message = new Message();
            message.what = HW_SET_TEXT;
            message.obj = watchUrl;
            handler.sendMessage(message);
            //新建视频完成，复制watchUrl到剪切版
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("Label", watchUrl);
            clipboardManager.setPrimaryClip(clipData);
        }).start();
    }
}