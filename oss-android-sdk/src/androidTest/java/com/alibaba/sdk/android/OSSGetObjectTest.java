package com.alibaba.sdk.android;

import android.test.AndroidTestCase;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.utils.BinaryUtil;
import com.alibaba.sdk.android.oss.common.utils.IOUtils;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.GetObjectRequest;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.Range;

import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Created by zhouzhuo on 11/24/15.
 */
public class OSSGetObjectTest extends AndroidTestCase {

    OSS oss;

    @Override
    public void setUp() throws Exception {
        OSSTestConfig.instance(getContext());
        if (oss == null) {
            Thread.sleep(5 * 1000); // for logcat initialization
            OSSLog.enableLog();
            ClientConfiguration conf = new ClientConfiguration();
            conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
            conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
            conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
            conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
            oss = new OSSClient(getContext(), OSSTestConfig.ENDPOINT,
                    OSSTestConfig.authCredentialProvider,
                    conf);
        }
    }

    public void testAsyncGetObject() throws Exception {
        GetObjectRequest request = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, "file1m");
        OSSTestConfig.TestGetCallback getCallback = new OSSTestConfig.TestGetCallback();

        request.setProgressListener(new OSSProgressCallback<GetObjectRequest>() {
            @Override
            public void onProgress(GetObjectRequest request, long currentSize, long totalSize) {
                OSSLog.logDebug("getobj_progress: " + currentSize + "  total_size: " + totalSize, false);
            }
        });

        OSSAsyncTask task = oss.asyncGetObject(request, getCallback);
        task.waitUntilFinished();

        GetObjectRequest rq = getCallback.request;
        GetObjectResult result = getCallback.result;

        assertEquals("file1m", rq.getObjectKey());
        assertEquals(OSSTestConfig.ANDROID_TEST_BUCKET, rq.getBucketName());
        byte[] content = IOUtils.readStreamAsBytesArray(result.getObjectContent());
        assertEquals(1024 * 1000, content.length);
        result.getObjectContent().close();
    }

    public void testAsyncGetImageWithXOssProcess() throws Exception {
        String jpgObject = OSSTestConfig.JPG_OBJECT_KEY;
        GetObjectRequest request = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, jpgObject);
        request.setxOssProcess("image/resize,m_lfit,w_100,h_100");
        OSSTestConfig.TestGetCallback getCallback = new OSSTestConfig.TestGetCallback();

        request.setProgressListener(new OSSProgressCallback<GetObjectRequest>() {
            @Override
            public void onProgress(GetObjectRequest request, long currentSize, long totalSize) {
                OSSLog.logDebug("getobj_progress: " + currentSize + "  total_size: " + totalSize, false);
            }
        });

        OSSAsyncTask task = oss.asyncGetObject(request, getCallback);
        task.waitUntilFinished();

        assertEquals(jpgObject, getCallback.request.getObjectKey());
        assertEquals(OSSTestConfig.ANDROID_TEST_BUCKET, getCallback.request.getBucketName());
        assertNull(getCallback.clientException);
        assertNull(getCallback.serviceException);
        assertNotNull(getCallback.result);
    }

    public void testSyncGetObject() throws Exception {
        GetObjectRequest request = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, "file1m");

        request.setProgressListener(new OSSProgressCallback<GetObjectRequest>() {
            @Override
            public void onProgress(GetObjectRequest request, long currentSize, long totalSize) {
                OSSLog.logDebug("getobj_progress: " + currentSize + "  total_size: " + totalSize, false);
            }
        });

        GetObjectResult result = oss.getObject(request);

        byte[] content = IOUtils.readStreamAsBytesArray(result.getObjectContent());
        assertEquals(1024 * 1000, content.length);

        result.getObjectContent().close();

        assertNotNull(result.getMetadata().getContentType());
    }

    public void testGetObjectRange() throws Exception {
        GetObjectRequest request = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, "file1m");
        Range range = new Range(1, 100);
        request.setRange(range);

        request.setProgressListener(new OSSProgressCallback<GetObjectRequest>() {
            @Override
            public void onProgress(GetObjectRequest request, long currentSize, long totalSize) {
                OSSLog.logDebug("getobj_progress: " + currentSize + "  total_size: " + totalSize, false);
            }
        });

        OSSLog.logDebug("Range: begin " + range.getBegin() + " end " + range.getEnd() + " isValid " + range.checkIsValid(), false);

        GetObjectResult result = oss.getObject(request);

        byte[] content = IOUtils.readStreamAsBytesArray(result.getObjectContent());
        assertEquals(100, content.length);
        result.getObjectContent().close();

        request.setRange(new Range(Range.INFINITE, 100));

        result = oss.getObject(request);

        content = IOUtils.readStreamAsBytesArray(result.getObjectContent());
        assertEquals(100, content.length);
        result.getObjectContent().close();

        request.setRange(new Range(100, Range.INFINITE));

        result = oss.getObject(request);

        content = IOUtils.readStreamAsBytesArray(result.getObjectContent());
        assertEquals(1024 * 1000 - 100, content.length);

        result.getObjectContent().close();
    }

    public void testGetObjectWithInvalidBucketName() throws Exception {
        GetObjectRequest get = new GetObjectRequest("#bucketName", "file1m");
        OSSTestConfig.TestGetCallback getCallback = new OSSTestConfig.TestGetCallback();
        OSSAsyncTask task = oss.asyncGetObject(get, getCallback);
        task.waitUntilFinished();
        assertNotNull(getCallback.clientException);
        assertTrue(getCallback.clientException.getMessage().contains("The bucket name is invalid"));
    }

    public void testGetObjectWithInvalidObjectKey() throws Exception {
        GetObjectRequest get = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, "//file1m");
        OSSTestConfig.TestGetCallback getCallback = new OSSTestConfig.TestGetCallback();
        OSSAsyncTask task = oss.asyncGetObject(get, getCallback);
        task.waitUntilFinished();
        assertNotNull(getCallback.clientException);
        assertTrue(getCallback.clientException.getMessage().contains("The object key is invalid"));
    }

    public void testGetObjectWithNullObjectKey() throws Exception {
        GetObjectRequest get = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, null);
        OSSTestConfig.TestGetCallback getCallback = new OSSTestConfig.TestGetCallback();
        OSSAsyncTask task = oss.asyncGetObject(get, getCallback);
        task.waitUntilFinished();
        assertNotNull(getCallback.clientException);
        assertTrue(getCallback.clientException.getMessage().contains("The object key is invalid"));
    }

    public void testSyncGetNotExistObject() throws Exception {
        GetObjectRequest get = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, "nofile");
        GetObjectResult result;
        try {
            result = oss.getObject(get);
        } catch (ServiceException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("NoSuchKey", e.getErrorCode());
        }
    }

    public void testAsyncGetNotExistObject() throws Exception {
        GetObjectRequest get = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, "nofile");
        OSSTestConfig.TestGetCallback getCallback = new OSSTestConfig.TestGetCallback();
        OSSAsyncTask task = oss.asyncGetObject(get, getCallback);
        task.waitUntilFinished();
        assertNotNull(getCallback.serviceException);
        assertEquals(404, getCallback.serviceException.getStatusCode());
        assertEquals("NoSuchKey", getCallback.serviceException.getErrorCode());
    }

    public void testDownloadObjectToFile() throws Exception {
        // upload file
        PutObjectRequest put = new PutObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, "file1m",
                OSSTestConfig.FILE_DIR + "file1m");
        OSSTestConfig.TestPutCallback putCallback = new OSSTestConfig.TestPutCallback();
        String srcFileBase64Md5 = BinaryUtil.toBase64String(BinaryUtil.calculateMd5(OSSTestConfig.FILE_DIR + "file1m"));
        OSSAsyncTask putTask = oss.asyncPutObject(put, putCallback);
        putTask.waitUntilFinished();
        assertEquals(200, putCallback.result.getStatusCode());

        // download object to file
        GetObjectRequest get = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, "file1m");
        OSSTestConfig.TestGetCallback getCallback = new OSSTestConfig.TestGetCallback();
        OSSAsyncTask getTask = oss.asyncGetObject(get, getCallback);
        getTask.waitUntilFinished();
        assertEquals(200, getCallback.result.getStatusCode());
        long length = getCallback.result.getContentLength();
        byte[] buffer = new byte[(int) length];
        int readCount = 0;
        while (readCount < length) {
            readCount += getCallback.result.getObjectContent().read(buffer, readCount, (int) length - readCount);
        }
        try {
            FileOutputStream fout = new FileOutputStream(OSSTestConfig.FILE_DIR + "download_file1m");
            fout.write(buffer);
            fout.close();
        } catch (Exception e) {
            OSSLog.logInfo(e.toString());
        }
        String downloadFileBase64Md5 = BinaryUtil.toBase64String(BinaryUtil.calculateMd5(OSSTestConfig.FILE_DIR + "download_file1m"));
        assertEquals(srcFileBase64Md5, downloadFileBase64Md5);

    }

    public void testConcurrentGetObject() throws Exception {
        final String fileNameArr[] = {"file1k", "file10k", "file100k", "file1m", "file10m"};
        final int fileSizeArr[] = {1024, 10240, 102400, 1024000, 10240000};
        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            final int index = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        GetObjectRequest get = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, fileNameArr[index]);
                        OSSTestConfig.TestGetCallback getCallback = new OSSTestConfig.TestGetCallback();
                        latch1.await();
                        OSSAsyncTask getTask = oss.asyncGetObject(get, getCallback);
                        getTask.waitUntilFinished();
                        assertEquals(200, getCallback.result.getStatusCode());
                        GetObjectResult result = getCallback.result;
                        byte[] content = IOUtils.readStreamAsBytesArray(result.getObjectContent());
                        assertEquals(fileSizeArr[index], content.length);
                        latch2.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        latch1.countDown();
        latch2.await();
        OSSLog.logDebug("testConcurrentGetObject success!");
    }

    public void testPutAndGetObjectWithSpecialFileKey() throws Exception {
        final String specialFileKey = "+&~?、测试文件";
        // put object
        PutObjectRequest put = new PutObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, specialFileKey,
                OSSTestConfig.FILE_DIR + "file1m");
        String srcFileBase64Md5 = BinaryUtil.toBase64String(BinaryUtil.calculateMd5(OSSTestConfig.FILE_DIR + "file1m"));
        OSSTestConfig.TestPutCallback putCallback = new OSSTestConfig.TestPutCallback();
        OSSAsyncTask putTask = oss.asyncPutObject(put, putCallback);
        putTask.waitUntilFinished();
        assertEquals(200, putCallback.result.getStatusCode());
        // get object
        GetObjectRequest get = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, specialFileKey);
        OSSTestConfig.TestGetCallback getCallback = new OSSTestConfig.TestGetCallback();
        OSSAsyncTask getTask = oss.asyncGetObject(get, getCallback);
        getTask.waitUntilFinished();
        assertEquals(200, getCallback.result.getStatusCode());
        long length = getCallback.result.getContentLength();
        byte[] buffer = new byte[(int) length];
        int readCount = 0;
        while (readCount < length) {
            readCount += getCallback.result.getObjectContent().read(buffer, readCount, (int) length - readCount);
        }
        try {
            FileOutputStream fout = new FileOutputStream(OSSTestConfig.FILE_DIR + "download_specialkey_file");
            fout.write(buffer);
            fout.close();
        } catch (Exception e) {
            OSSLog.logInfo(e.toString());
        }
        String downloadFileBase64Md5 = BinaryUtil.toBase64String(BinaryUtil.calculateMd5(OSSTestConfig.FILE_DIR + "download_specialkey_file"));
        assertEquals(srcFileBase64Md5, downloadFileBase64Md5);
    }

    public void testNotUseCRC64GetObject() throws Exception {
        ClientConfiguration conf = new ClientConfiguration();
        conf.setCheckCRC64(false);
        OSS oss = new OSSClient(getContext(), OSSTestConfig.ENDPOINT,
                OSSTestConfig.authCredentialProvider,
                conf);

        GetObjectRequest request = new GetObjectRequest(OSSTestConfig.ANDROID_TEST_BUCKET, "file1m");

        request.setProgressListener(new OSSProgressCallback<GetObjectRequest>() {
            @Override
            public void onProgress(GetObjectRequest request, long currentSize, long totalSize) {
                OSSLog.logDebug("getobj_progress: " + currentSize + "  total_size: " + totalSize, false);
            }
        });

        GetObjectResult result = oss.getObject(request);

        byte[] content = IOUtils.readStreamAsBytesArray(result.getObjectContent());
        assertEquals(1024 * 1000, content.length);

        result.getObjectContent().close();

        assertNotNull(result.getMetadata().getContentType());
    }
}
