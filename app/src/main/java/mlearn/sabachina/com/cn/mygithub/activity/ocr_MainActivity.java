package mlearn.sabachina.com.cn.mygithub.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import mlearn.sabachina.com.cn.mygithub.activity.Config;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import mlearn.sabachina.com.cn.mygithub.activity.BatchUploadSamples;
import mlearn.sabachina.com.cn.mygithub.activity.OssService;
import mlearn.sabachina.com.cn.mygithub.activity.UIDisplayer;
import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralBasicParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.WordSimple;
import com.tangxiaolv.telegramgallery.GalleryActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import mlearn.sabachina.com.cn.mygithub.R;
import mlearn.sabachina.com.cn.photoselect.bean.Photo;
import mlearn.sabachina.com.cn.photoselect.request.AlbumOperation;
import mlearn.sabachina.com.cn.photoselect.request.AlbumTarget;
import mlearn.sabachina.com.cn.photoselect.request.CheckMarkStyle;
import mlearn.sabachina.com.cn.photoselect.request.IconLocation;
import mlearn.sabachina.com.cn.photoselect.request.PhotoPicker;
import mlearn.sabachina.com.cn.photoselect.util.FileUtil;

import static mlearn.sabachina.com.cn.mygithub.activity.Config.MESSAGE_UPLOAD_2_OSS;
import java.sql.Connection;
import android.content.Intent;


public class ocr_MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Uri uri;

    //////////////////
    private static final int REQUEST_CODE_GENERAL_BASIC = 106;

    private boolean hasGotToken = false;

    private AlertDialog.Builder alertDialog;



    private String imgEndpoint = "http://img-cn-shanghai.aliyuncs.com";
    private String mRegion = "";
    //服务器地址 需将ip地址改为自己的电脑的IP地址
    String stsServer="http://192.168.1.108:7080";
    //负责所有的界面更新
    private UIDisplayer mUIDisplayer;
    //OSS的上传下载
    private OssService ossService;
    private static final int RESULT_LOAD_IMAGE = 1;
    private static final String FILE_DIR = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "oss/";
    private BatchUploadSamples batchUploadSamples;

    private String UserId;
    private String EmojiId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_upload_method);
        alertDialog = new AlertDialog.Builder(this);

        Intent intent=getIntent();
        UserId=intent.getStringExtra("UserId");     //获取UserId

/*
        // 通用文字识别（拍照）
        findViewById(R.id.take_photo).setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (!checkTokenStatus()) {
                    return;
                }
                /*
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH,
                        FileUtil.getSaveFile(getApplication()).getAbsolutePath());
                intent.putExtra(CameraActivity.KEY_CONTENT_TYPE,
                        CameraActivity.CONTENT_TYPE_GENERAL);
                startActivityForResult(intent, REQUEST_CODE_GENERAL_BASIC);
            }
        });
/*
        // 通用文字识别（相册）
        findViewById(R.id.album).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkTokenStatus()) {
                    return;
                }
                /*
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH,
                        FileUtil.getSaveFile(getApplication()).getAbsolutePath());
                intent.putExtra(CameraActivity.KEY_CONTENT_TYPE,
                        CameraActivity.CONTENT_TYPE_GENERAL);

                startActivityForResult(intent, REQUEST_CODE_GENERAL_BASIC);
            }
        });
*/

        // 请选择您的初始化方式
        // initAccessToken();
        initAccessTokenWithAkSk();

        //super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
//        FragmentManager manager = getSupportFragmentManager();
//        FragmentTransaction transaction = manager.beginTransaction();
//        transaction.add(R.id.content,new MainFragment());
//        transaction.commit();


        //Log.d("test", "running... " );///////////////////////


        initRegion();
        ossService = initOSS(Config.endpoint, Config.bucket, mUIDisplayer);



        View takePhoto = findViewById(R.id.take_photo);
        View album = findViewById(R.id.album);
        takePhoto.setOnClickListener(this);
        album.setOnClickListener(this);

    }
    /////////////


    private boolean checkTokenStatus() {
        if (!hasGotToken) {
            Toast.makeText(getApplicationContext(), "token还未成功获取", Toast.LENGTH_LONG).show();
        }
        return hasGotToken;
    }

    private void initAccessTokenWithAkSk() {
        OCR.getInstance().initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken result) {
                String token = result.getAccessToken();
                hasGotToken = true;
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
                //alertText("AK，SK方式获取token失败", error.getMessage());
                Log.d("error", "onOcrResult: " + "AK，SK方式获取token失败");
            }
        }, getApplicationContext(), "V9Gd2qSFVPEuR9HGUZ9SSDMn", "zFEpgv6oa6v64i3yCdNBot9GHunASElo");
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放内存资源
        OCR.getInstance().release();
    }
    //////////////


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.take_photo:
                uri = FileUtil.getDefaultUri();
                PhotoPicker.getCamera()
                        .requestCode(3)
                        .uri(uri)
                        .start(this);
                if (!checkTokenStatus()) {
                    return;
                }
                break;
            case R.id.album:

                AlbumOperation operation = new AlbumOperation.Builder()
                        //图片选中指示器离四周的距离,单位像素
                        .marginSelectedSign(12)
                        //最大选择数
                        .maxNum(4)
                        //展示的列数
                        .column(3)
                        //指示器的风格，可选图片标识和数字标识
                        .style(CheckMarkStyle.DIGIT)
                        //图片选中指示相对位置，左上，左下，右上，右下
                        .location(IconLocation.TOP_RIGHT)
                        //图片标识中选中之后的资源文件图片
                        .selectResId(R.drawable.checkbox)
                        //图片标识中未选中的资源文件图片
                        .unSelectResId(R.drawable.checkbox_un)
                        //数字标识中内圈圆颜色
                        .digitInSideColor(R.color.colorAlbum)
                        //相册页面顶布局背景颜色(带状态栏)
                        .albumTitleBarColor(R.color.colorAlbum)
                        .build();
                PhotoPicker.getAlbum()
                        .requestCode(4)
                        //相册页面的属性设置
                        .albumOperation(operation)
                        //设置一下自己的加载图片方式
                        .imageLoader(new ImageLoader())
                        .start(this);
                if (!checkTokenStatus()) {
                    return;
                }
                break;
        }
    }

    String res="123";
    //result:OCR识别标签 objectName:图片名称
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //相机返回resultCode 为0


        //Log.d("test", "running... " );/////////////////////////



        if (resultCode == RESULT_OK) {
            if (requestCode == 3) {
                //相机
                Log.d("Photo", "onActivityResult: " + uri.getPath());



                ossService = initOSS(Config.endpoint, Config.bucket, mUIDisplayer);
                String []temp=uri.getPath().split("/");
                final String objectName=temp[temp.length-1];

                //ossService.asyncPutImage(objectName, uri.getPath());
                ossService.asyncPutImage("1.jpg.jpg", "/storage/emulated/0/DCIM/Camera/1.jpg.jpg");
                Log.d(objectName,"图片名称");
                copyLocalFile();
                initLocalFiles();

                super.onActivityResult(requestCode, resultCode, data);
                if (requestCode == Config.REQUESTCODE_AUTH && resultCode == RESULT_OK) {
                    if (data != null) {
                        ClientConfiguration conf = new ClientConfiguration();
                        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
                        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
                        conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
                        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次

                    }
                }

                if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursor.moveToFirst();

                }

                // 识别成功回调，通用文字识别
                if (/*requestCode == REQUEST_CODE_GENERAL_BASIC &&*/ resultCode == Activity.RESULT_OK) {
                    RecognizeService.recGeneralBasic(/*FileUtil.getSaveFile(getApplicationContext()).getAbsolutePath()*/uri.getPath(),
                            new RecognizeService.ServiceListener() {
                                @Override
                                public void onResult(String result) {
                                    //infoPopText(result);
                                    Log.d("Photo", "OcrResult: " + result);
                                    res=result;
                                    System.out.println("res="+res);

                                    InsertIntoDatabase(objectName,res);
                                    try {
                                        Thread.sleep(1000);             //当前线程睡眠1秒
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    Intent intent = new Intent();
                                    intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
                                    intent.putExtra("EmojiId",EmojiId);
                                    intent.setClass(ocr_MainActivity.this, PersonalBigPictureActivity.class);      //跳转到PersonalSpaceActivity
                                    ocr_MainActivity.this.startActivity(intent);
                                    finish();

                                }
                            });

                }



            }
            if (requestCode == 4) {
                ArrayList<Photo> photos = data.getParcelableArrayListExtra(AlbumTarget.ALBUM_SELECT_PHOTO);
                for (Photo photo : photos) {
                    Log.d("Photo", "onActivityResult: " + photo.getFilePath());


                        ossService = initOSS(Config.endpoint, Config.bucket, mUIDisplayer);
                        String []temp=photo.getFilePath().split("/");
                        final String objectName=temp[temp.length-1];

                        ossService.asyncPutImage(objectName, photo.getFilePath());
                        Log.d(objectName,"图片名称");
                        copyLocalFile();
                        initLocalFiles();

                        super.onActivityResult(requestCode, resultCode, data);
                        if (requestCode == Config.REQUESTCODE_AUTH && resultCode == RESULT_OK) {
                            if (data != null) {
                                ClientConfiguration conf = new ClientConfiguration();
                                conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
                                conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
                                conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
                                conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次

                            }
                        }

                        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
                            Uri selectedImage = data.getData();
                            String[] filePathColumn = {MediaStore.Images.Media.DATA};

                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            cursor.moveToFirst();


                        }

                    // 识别成功回调，通用文字识别
                    if (/*requestCode == REQUEST_CODE_GENERAL_BASIC && */resultCode == Activity.RESULT_OK) {
                        RecognizeService.recGeneralBasic(/*FileUtil.getSaveFile(getApplicationContext()).getAbsolutePath()*/photo.getFilePath(),
                                new RecognizeService.ServiceListener() {
                                    @Override
                                    public void onResult(String result) {
                                        //infoPopText(result);
                                        Log.d("Photo", "OcrResult: " + result);
                                        res=result;
                                        System.out.println("res="+res);

                                        InsertIntoDatabase(objectName,res);
                                        try {
                                            Thread.sleep(1000);             //当前线程睡眠0.1秒
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        Intent intent = new Intent();
                                        intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
                                        intent.putExtra("EmojiId",EmojiId);
                                        intent.setClass(ocr_MainActivity.this, PersonalBigPictureActivity.class);      //跳转到PersonalSpaceActivity
                                        ocr_MainActivity.this.startActivity(intent);
                                        finish();
                                    }
                                });


                    }




                }
            }


        }




    }

    private Connection con;

    /*连接数据库，得到的连接存在成员con中*/
    public void getConnection(){

        //连接数据库不能在主线程中，因此另开一个子线程
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 反复尝试连接，直到连接成功后退出循环
                while (!Thread.interrupted()) {
                    try {
                        Thread.sleep(100);  // 每隔0.1秒尝试连接
                    } catch (InterruptedException e) {
                        System.out.println(e.toString());
                    }


                    // 2.设置好IP/端口/数据库名/用户名/密码等必要的连接信息
                    String url = "jdbc:mysql://192.168.1.108:3306/emoji?autoReconnect=true";  //此处不能用localhost，必须用10.0.2.2
                    String user = "root";
                    String password = "yuanshuhan123";                   //数据库的密码和用户名

                    // 3.连接JDBC
                    try {
                        Class.forName("com.mysql.jdbc.Driver");
                        con = DriverManager.getConnection(url, user, password);
                        System.out.println("连接成功");
                        return;

                    } catch (ClassNotFoundException e) {
                        System.out.println("连接失败1");
                        System.out.println(e.toString());
                    } catch (SQLException e) {
                        System.out.println("连接失败2");
                        System.out.println(e.toString());
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.out.println("连接失败3");
                        System.out.println(e.toString());
                    }
                }
            }
        });
        thread.start();                    //所有子线程必需start

    }
    /*连接数据，插入新图片的数据*/
    public void InsertIntoDatabase(final String path,final String label){
        if(con==null)
            getConnection();                  //连接数据库
        while(con==null){                //由于每个线程是并行的，所以需要等待数据库连接成功后才能进行下一步
            System.out.println("数据库连接中");
            try {
                Thread.currentThread().sleep(100);          //当前线程睡眠0.1秒
            }catch(InterruptedException e){
                System.out.println("sleep失败");
                e.printStackTrace();
            }
        }
        System.out.println("数据库连接成功");
        System.out.println("UserId="+UserId);
        System.out.println("label="+label);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String sql = "select * from Total_Emoji";
                PreparedStatement pstmt;
                ResultSet rs;
                int id;
                try {
                    pstmt = (PreparedStatement)con.prepareStatement(sql);
                    rs=pstmt.executeQuery();
                    id=0;
                    while(rs.next())
                        id++;
                    System.out.println("max EmojiId="+id);
                    id=id+1;
                    EmojiId=Integer.toString(id);

                    sql = "insert into Total_Emoji(EmojiId,Type,Link) values("+id+",2,'"+path+"');";
                    pstmt = (PreparedStatement)con.prepareStatement(sql);
                    pstmt.execute();
                    sql = "insert into Total_Label(EmojiId,UserId,EmojiLabel) values("+id+",'"+UserId+"','"+label+"');";
                    pstmt = (PreparedStatement)con.prepareStatement(sql);
                    pstmt.execute();
                    } catch (SQLException e) {
                    e.printStackTrace();
                    System.out.println("sql执行失败");
                }

            }
        }).start();
    }

    //OSS初始化
    public OssService initOSS(String endpoint, String bucket, UIDisplayer displayer) {

//        移动端是不安全环境，不建议直接使用阿里云主账号ak，sk的方式。建议使用STS方式。具体参
//        https://help.aliyun.com/document_detail/31920.html
//        注意：SDK 提供的 PlainTextAKSKCredentialProvider 只建议在测试环境或者用户可以保证阿里云主账号AK，SK安全的前提下使用。具体使用如下
//        主账户使用方式
//        String AK = "******";
//        String SK = "******";
//        credentialProvider = new PlainTextAKSKCredentialProvider(AK,SK)
//        以下是使用STS Sever方式。

        OSSCredentialProvider credentialProvider;
        //使用自己的获取STSToken的类
        //String stsServer="http://192.168.1.108:7080";

        // String stsServer = ((EditText) findViewById(R.id.stsserver)).getText().toString();
        if (TextUtils.isEmpty(stsServer)) {
            credentialProvider = new OSSAuthCredentialsProvider(Config.STSSERVER);
            // ((EditText) findViewById(R.id.stsserver)).setText(Config.STSSERVER);
        } else {
            credentialProvider = new OSSAuthCredentialsProvider(stsServer);
        }

        String editBucketName = "biaoqingyun2";
        if (TextUtils.isEmpty(editBucketName)) {
            editBucketName = bucket;
        }
        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
        OSS oss = new OSSClient(getApplicationContext(), endpoint, credentialProvider, conf);
        return new OssService(oss, editBucketName, displayer);

    }

    protected void initRegion() {
        if (TextUtils.isEmpty(Config.endpoint)) {
            return;
        }
        if (Config.endpoint.contains("oss-cn-hangzhou")) {
            mRegion = "杭州";
            imgEndpoint = getImgEndpoint();
        } else if (Config.endpoint.contains("oss-cn-qingdao")) {
            mRegion = "青岛";
            imgEndpoint = getImgEndpoint();
        } else if (Config.endpoint.contains("oss-cn-beijing")) {
            mRegion = "北京";
            imgEndpoint = getImgEndpoint();
        } else if (Config.endpoint.contains("oss-cn-shenzhen")) {
            mRegion = "深圳";
            imgEndpoint = getImgEndpoint();
        } else if (Config.endpoint.contains("oss-us-west-1")) {
            mRegion = "美国";
            imgEndpoint = getImgEndpoint();
        } else if (Config.endpoint.contains("oss-cn-shanghai")) {
            mRegion = "上海";
            imgEndpoint = getImgEndpoint();
        } else {
//            Toast.makeText(AuthTestActivity.this, "错误的区域", Toast.LENGTH_SHORT).show();
            Log.d("OSS", "错误的区域");/////////////////////
//            new AlertDialog.Builder(AuthTestActivity.this).setTitle("错误的区域").setMessage(mRegion).show();
        }
    }

    protected String getImgEndpoint() {
        String imgEndpoint = "";
        if (mRegion.equals("杭州")) {
            imgEndpoint = "http://img-cn-hangzhou.aliyuncs.com";
        } else if (mRegion.equals("青岛")) {
            imgEndpoint = "http://img-cn-qingdao.aliyuncs.com";
        } else if (mRegion.equals("北京")) {
            imgEndpoint = "http://img-cn-beijing.aliyuncs.com";
        } else if (mRegion.equals("深圳")) {
            imgEndpoint = "http://img-cn-shenzhen.aliyuncs.com";
        } else if (mRegion.equals("美国")) {
            imgEndpoint = "http://img-us-west-1.aliyuncs.com";
        } else if (mRegion.equals("上海")) {
            imgEndpoint = "http://img-cn-shanghai.aliyuncs.com";
        } else {
            //new android.app.AlertDialog.Builder(AuthTestActivity.this).setTitle("错误的区域").setMessage(mRegion).show();
            Log.d("OSS", "错误的区域");/////////////////////
            imgEndpoint = "";
        }
        return imgEndpoint;
    }

    private void copyLocalFile() {
        String zipFile = "wangwang.zip";
        String filePath = this.FILE_DIR + zipFile;
        try {
            File path = new File(this.FILE_DIR);
            File file = new File(filePath);
            if (!path.exists()) {
                OSSLog.logDebug("MULTIPART_UPLOAD", "Create the path:" + path.getAbsolutePath());
                path.mkdir();
            }
            if (!file.exists()) {
                file.createNewFile();
                OSSLog.logDebug("MULTIPART_UPLOAD", "create : " + file.getAbsolutePath());
            } else {
                return;
            }
            InputStream input = getBaseContext().getAssets().open(zipFile);

            OSSLog.logDebug("input.available() : " + input.available());

            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[500 * 1024];
            int byteCount = 0;
            int totalReadByte = 0;
            while ((byteCount = input.read(buffer)) != -1) {//循环从输入流读取 buffer字节
                fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                totalReadByte += byteCount;
            }
            OSSLog.logDebug("totalReadByte : " + totalReadByte);
            fos.flush();//刷新缓冲区
            input.close();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initLocalFiles() {
        String[] fileNames = {"file1k", "file10k", "file100k", "file1m", "file10m"};
        int[] fileSize = {1024, 10240, 102400, 1024000, 10240000};

        for (int i = 0; i < fileNames.length; i++) {
            try {
                String filePath = FILE_DIR + fileNames[i];
                OSSLog.logDebug("OSSTEST", "filePath : " + filePath);
                File path = new File(FILE_DIR);
                File file = new File(filePath);
                if (!path.exists()) {
                    OSSLog.logDebug("OSSTEST", "Create the path:" + path.getAbsolutePath());
                    path.mkdir();
                }
                if (!file.exists()) {
                    file.createNewFile();
                    OSSLog.logDebug("OSSTEST", "create : " + file.getAbsolutePath());
                } else {
                    return;
                }
                OSSLog.logDebug("OSSTEST", "write file : " + filePath);
                InputStream in = new FileInputStream(file);
                FileOutputStream fos = new FileOutputStream(file);
                long index = 0;
                int buf_size = 1024;
                int part = fileSize[i] / buf_size;
                while (index < part) {
                    byte[] buf = new byte[1024];
                    fos.write(buf);
                    index++;
                }
                in.close();
                fos.close();
                OSSLog.logDebug("OSSTEST", "file write" + fileNames[i] + " ok");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
