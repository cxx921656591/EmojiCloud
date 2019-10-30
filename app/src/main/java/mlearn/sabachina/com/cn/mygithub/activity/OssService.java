package mlearn.sabachina.com.cn.mygithub.activity;

import android.graphics.Bitmap;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.GetObjectRequest;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.alibaba.sdk.android.oss.model.OSSRequest;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import mlearn.sabachina.com.cn.mygithub.activity.UIDisplayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by mOss on 2015/12/7 0007.
 * 支持普通上传，普通下载
 */
public class OssService {

    public OSS mOss;
    private String mBucket;
    private UIDisplayer mDisplayer;
    private String mCallbackAddress;
    private static String mResumableObjectKey = "resumableObject";

    private boolean NeedDownload;

    public void setNeedDownLoad(boolean b){
        NeedDownload=b;
    }

    public OssService(OSS oss, String bucket, UIDisplayer uiDisplayer) {
        this.mOss = oss;
        this.mBucket = bucket;
        this.mDisplayer = uiDisplayer;
    }

    public void setBucketName(String bucket) {
        this.mBucket = bucket;
    }

    public void initOss(OSS _oss) {
        this.mOss = _oss;
    }

    public void setCallbackAddress(String callbackAddress) {
        this.mCallbackAddress = callbackAddress;
    }

    public void asyncGetImage(final String object) {

        final long get_start = System.currentTimeMillis();
        if ((object == null) || object.equals("")) {
            Log.w("AsyncGetImage", "ObjectNull");
            return;
        }

        GetObjectRequest get = new GetObjectRequest(mBucket, object);
        get.setCRC64(OSSRequest.CRC64Config.YES);
        get.setProgressListener(new OSSProgressCallback<GetObjectRequest>() {
            @Override
            public void onProgress(GetObjectRequest request, long currentSize, long totalSize) {
                Log.d("GetObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
                //int progress = (int) (100 * currentSize / totalSize);
                //mDisplayer.updateProgress(progress);
                //mDisplayer.displayInfo("下载进度: " + String.valueOf(progress) + "%");
            }
        });
        OSSAsyncTask task = mOss.asyncGetObject(get, new OSSCompletedCallback<GetObjectRequest, GetObjectResult>() {
            @Override
            public void onSuccess(GetObjectRequest request, GetObjectResult result) {
                // 请求成功
                InputStream inputStream = result.getObjectContent();
                //Bitmap bm = BitmapFactory.decodeStream(inputStream);
                try {
                    //需要根据对应的View大小来自适应缩放
                    Bitmap bm = mDisplayer.autoResizeFromStream(inputStream);
                    long get_end = System.currentTimeMillis();
                    OSSLog.logDebug("get cost: " + (get_end - get_start) / 1000f);
                    //mDisplayer.downloadComplete(bm);
                    if(NeedDownload)
                        mDisplayer.downloadComplete(bm,object);
                    else
                        mDisplayer.showPicture(bm,object);
                    mDisplayer.displayInfo("Bucket: " + mBucket + "\nObject: " + request.getObjectKey() + "\nRequestId: " + result.getRequestId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(GetObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                String info = "";
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                    info = clientExcepion.toString();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                    info = serviceException.toString();
                }
                mDisplayer.downloadFail(info);
                mDisplayer.displayInfo(info);
            }
        });


        Log.i("tagconvertstr", "yupianaaaaaaaaaaaaaaaaaaaa");
    }


    public void asyncPutImage(String object, String localFile) {
        final long upload_start = System.currentTimeMillis();
        //Log.d(object,"图片名称");
        //Log.d(localFile,"图片url");

        if (object.equals("")) {
            Log.w("AsyncPutImage", "ObjectNull");
            return;
        }

        File file = new File(localFile);
        if (!file.exists()) {
            Log.w("AsyncPutImage", "FileNotExist");
            Log.w("LocalFile", localFile);
            return;
        }


        // 构造上传请求
        PutObjectRequest put = new PutObjectRequest(mBucket, object, localFile);
        put.setCRC64(OSSRequest.CRC64Config.YES);
        if (mCallbackAddress != null) {
            // 传入对应的上传回调参数，这里默认使用OSS提供的公共测试回调服务器地址
            put.setCallbackParam(new HashMap<String, String>() {
                {
                    put("callbackUrl", mCallbackAddress);
                    //callbackBody可以自定义传入的信息
                    put("callbackBody", "filename=${object}");
                }
            });
        }

        // 异步上传时可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
                int progress = (int) (100 * currentSize / totalSize);
                //mDisplayer.updateProgress(progress);
                //mDisplayer.displayInfo("上传进度: " + String.valueOf(progress) + "%");
            }
        });

        OSSAsyncTask task = mOss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d("PutObject", "UploadSuccess");

                Log.d("ETag", result.getETag());
                Log.d("RequestId", result.getRequestId());

                long upload_end = System.currentTimeMillis();
                OSSLog.logDebug("upload cost: " + (upload_end - upload_start) / 1000f);
                mDisplayer.uploadComplete();
                mDisplayer.displayInfo("Bucket: " + mBucket
                        + "\nObject: " + request.getObjectKey()
                        + "\nETag: " + result.getETag()
                        + "\nRequestId: " + result.getRequestId()
                        + "\nCallback: " + result.getServerCallbackReturnBody());
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                String info = "";
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                    info = clientExcepion.toString();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                    info = serviceException.toString();
                }
                mDisplayer.uploadFail(info);
                mDisplayer.displayInfo(info);
            }
        });


    }




}
