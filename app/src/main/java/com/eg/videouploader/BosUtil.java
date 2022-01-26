package com.eg.videouploader;

import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;

import java.io.File;

public class BosUtil {
    private static BosClient client;
    private static final String BUCKET_NAME = "video-beijing";
    private static final String BASE_URL = "https://video-beijing.cdn.bcebos.com";

    public static void initClient() {
        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(
                "ace034ef06b040c0b578a668f292f493",
                "0922593ba25244d19bc7a340188fe57c"));
        config.setEndpoint("bj.bcebos.com");
        client = new BosClient(config);
    }

    public static BosClient getClient() {
        if (client == null) {
            initClient();
        }
        return client;
    }

    /**
     * 上传到对象存储
     *
     * @param file
     * @param objectKey
     * @return
     */
    public static String uploadObjectStorage(File file, String objectKey) {
        client.putObject(BUCKET_NAME, objectKey, file);
        return BASE_URL + objectKey;
    }
}
