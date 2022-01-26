package com.eg.videouploader;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;

public class MainActivity extends AppCompatActivity {
    private static final int RQ_PICK_VIDEO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.INTERNAL_CONTENT_URI);

        startActivityForResult(intent, RQ_PICK_VIDEO);
        FileUtil.del(getCacheDir());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        if (requestCode == RQ_PICK_VIDEO) {
            Uri uri = intent.getData();

            ContentResolver contentResolver = getContentResolver();
            InputStream inputStream;
            try {
                inputStream = contentResolver.openInputStream(uri);
                File videoFile = new File(getCacheDir(), IdUtil.fastSimpleUUID());
                IoUtil.copy(inputStream, new FileOutputStream(videoFile));
                Log.e("tag", videoFile.length() + "");

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }

        super.onActivityResult(requestCode, resultCode, intent);
    }
}