package mlearn.sabachina.com.cn.mygithub.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import android.widget.Button;

import mlearn.sabachina.com.cn.mygithub.R;

import mlearn.sabachina.com.cn.mygithub.R;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
//import com.alibaba.sdk.android.oss.app.Config;
//import com.alibaba.sdk.android.oss.app.R;
import com.alibaba.sdk.android.oss.common.HttpMethod;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import com.alibaba.sdk.android.oss.internal.ObjectURLPresigner;
import com.alibaba.sdk.android.oss.model.GeneratePresignedUrlRequest;
//import com.alibaba.sdk.android.oss.sample.customprovider.OssService;
//import com.alibaba.sdk.android.oss.sample.customprovider.UIDisplayer;
import com.tangxiaolv.telegramgallery.*;
import com.afollestad.materialdialogs.MaterialDialog;
//import com.alibaba.sdk.android.oss.sample.BatchUploadSamples;

import static mlearn.sabachina.com.cn.mygithub.activity.Config.MESSAGE_UPLOAD_2_OSS;
import static mlearn.sabachina.com.cn.mygithub.activity.Config.STSSERVER;
import static mlearn.sabachina.com.cn.mygithub.activity.Config.UPLOAD_SUC;
import static java.security.AccessController.getContext;

/*私有图片对应的大图界面*/
public class PersonalBigPictureActivity extends AppCompatActivity {
    private static String TAG="MainActivity";                 //这个用于Android Monitor中筛选测试输出信息

    public static final int SHOWINFO=12;
    public static final int CREATELAYOUT=13;
    public static final int CREATETEXTVIEW=14;
    private ImageView imageView;
    private TextView textView;

    private Connection con;

    private String link=null;
    private String label=null;
    private String UserId;
    private String EmojiId;

    private String info;       //输出的提示信息
    private TextView tv;

    private LinearLayout linearlayout;           //与xml中图片标签的外层线性布局对应
    private LinearLayout layout;                 //每行标签的线性布局
    private TextView labeltv;
    private String l;

    private Dialog dialog,ensure_dialog;
    String label_operate;
    private TextView tv_operate;

    private EditText et;
    private View dialog_view,add_view;


    private String imgEndpoint = "http://img-cn-shanghai.aliyuncs.com";
    private String mRegion = "";
    //服务器地址 需将ip地址改为自己的电脑的IP地址
    String stsServer="http://192.168.1.108:7080";
    //负责所有的界面更新
    private UIDisplayer mUIDisplayer;

    //OSS的上传下载
    private OssService ossService;
    //在这里输入路径
    private String picturePath = "/storage/emulated/0/DCIM/Camera/1.jpg.jpg";

    private static final int RESULT_LOAD_IMAGE = 1;
    private static final String FILE_DIR = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "oss/";

    private BatchUploadSamples batchUploadSamples;



    /*标签点击触发事件*/
    class LabelListener implements View.OnClickListener {
        public void onClick(View v) {
            label_operate=v.getTag().toString();
            tv_operate=(TextView) v;
            Log.i(TAG,"被点击标签:"+label_operate);

            dialog=new Dialog(PersonalBigPictureActivity.this);
            View view = LayoutInflater.from(PersonalBigPictureActivity.this).inflate(R.layout.labeldialog,null);
            dialog.setContentView(view);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width =500;
            params.height=350;
            dialog.getWindow().setAttributes(params);
            //show之前设置返回键无效，触摸屏无效
            dialog.setCancelable(false);
            //显示对话框
            dialog.show();
        }
    }

    public void ModifyLabel(View v){
        Log.i(TAG,"修改标签");

        switch (v.getId()){
            case R.id.choose_modifylabel:
                dialog.dismiss();

                dialog=new Dialog(PersonalBigPictureActivity.this);
                dialog_view = LayoutInflater.from(PersonalBigPictureActivity.this).inflate(R.layout.editlabel,null);

                et=(EditText) dialog_view.findViewById(R.id.label_new);
                //et.setSaveEnabled(false);
                et.setText(label_operate);

                dialog.setContentView(dialog_view);
                WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
                params.width =1100;
                params.height=350;
                dialog.getWindow().setAttributes(params);
                //show之前设置返回键无效，触摸屏无效
                dialog.setCancelable(false);
                //显示对话框
                dialog.show();
                break;
            case R.id.submit_label_button:
                dialog.dismiss();

                et=(EditText) dialog_view.findViewById(R.id.label_new);
                if(et.getId()==R.id.label_new)
                    Log.i(TAG,"et id is right");
                Log.i(TAG,"et内容："+et.getText().toString());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String mmm=et.getText().toString();
                        Log.i(TAG,"输入="+mmm);
                        String []labels=label.split("\\s");
                        String label_new="";
                        for(int i=0;i<labels.length;i++){
                            if(labels[i].equals(label_operate))
                                label_new+=mmm+" ";
                            else
                                label_new+=labels[i]+" ";
                        }
                        Log.i(TAG,"编辑后的标签="+label_new);
                        updateLabel(label_new);
                        refreshPage();
                    }
                }).start();

        }

    }

    public void DeleteLabel(View v){
        Log.i(TAG,"删除标签");

        switch (v.getId()){
            case R.id.delete_label_button:
                Log.i(TAG,"点击了删除标签");
                dialog.dismiss();
                ensure_dialog=new Dialog(PersonalBigPictureActivity.this);
                View view = LayoutInflater.from(PersonalBigPictureActivity.this).inflate(R.layout.delete_ensure_dialog,null);
                ensure_dialog.setContentView(view);
                WindowManager.LayoutParams params = ensure_dialog.getWindow().getAttributes();
                params.width = 600;
                params.height=300;
                ensure_dialog.getWindow().setAttributes(params);
                //show之前设置返回键无效，触摸屏无效
                ensure_dialog.setCancelable(false);
                //显示对话框
                ensure_dialog.show();
                break;
            case R.id.delete_cancel_button:           //取消删除
                ensure_dialog.dismiss();
                break;
            case R.id.delete_ensure_button:          //确定删除
                ensure_dialog.dismiss();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String label_new="";
                        String []labels=label.split("\\s");
                        for(int i=0;i<labels.length;i++)
                            if(!labels[i].equals(label_operate))
                                label_new+=labels[i]+" ";
                        Log.i(TAG,"删除操作后的标签:"+label_new);
                        updateLabel(label_new);
                        refreshPage();

                    }
                }).start();
                break;
        }

    }

    public void AddLabel(View v){
        Log.i(TAG,"增加标签");

        switch (v.getId()) {
            case R.id.add_label:
                dialog = new Dialog(PersonalBigPictureActivity.this);
                add_view = LayoutInflater.from(PersonalBigPictureActivity.this).inflate(R.layout.label_add_dialog, null);
                dialog.setContentView(add_view);
                WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
                params.width = 1100;
                params.height = 350;
                dialog.getWindow().setAttributes(params);
                //show之前设置返回键无效，触摸屏无效
                dialog.setCancelable(false);
                //显示对话框
                dialog.show();
                break;
            case R.id.submit_addedlabel_button:
                dialog.dismiss();

                et = (EditText) add_view.findViewById(R.id.label_add);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String mmm = et.getText().toString();
                        Log.i(TAG, "输入=" + mmm);
                        String label_new = label + " " + mmm;
                        Log.i(TAG, "编辑后的标签=" + label_new);
                        updateLabel(label_new);
                        refreshPage();
                    }
                }).start();
        }
    }

    //由于更新UI只能在主线程中进行，因此需要设置一个handler处理子线程传递来的消息，从而进行各种UI的操作
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case UPLOAD_SUC:
                    //dismissLoading();

                case MESSAGE_UPLOAD_2_OSS:
                    //showLoading();
                    final List<String> localPhotos = (List<String>) msg.obj;
                    batchUploadSamples = new BatchUploadSamples(ossService.mOss, Config.bucket, localPhotos, handler);
                    batchUploadSamples.upload();

                case SHOWINFO:             //显示提示信息
                    tv.setText(info);
                    break;
                case CREATELAYOUT:             //创建一个linearlayout
                {
                    layout=new LinearLayout(PersonalBigPictureActivity.this);
                    ViewGroup.LayoutParams layoutParams=new ViewGroup.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);       //高度为400像素
                    //将以上的属性赋给LinearLayout
                    layout.setLayoutParams(layoutParams);
                    linearlayout.addView(layout);       //在最外层的linearlayout中增加这个新建的linearlayout
                    break;
                }
                case CREATETEXTVIEW:           //创建一个textview
                {
                    labeltv = new TextView(PersonalBigPictureActivity.this);
                    ViewGroup.MarginLayoutParams mp = new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.WRAP_CONTENT, ViewGroup.MarginLayoutParams.WRAP_CONTENT);
                    mp.setMargins(10,10,10,10);           //设置标签间距
                    labeltv.setLayoutParams(mp);
                    labeltv.setBackgroundResource(R.drawable.bg_edittext);
                    labeltv.setPadding(5,5,5,5);
                    labeltv.setText(l);
                    labeltv.setTag(l);
                    labeltv.setOnClickListener(new LabelListener());
                    layout.addView(labeltv);
                    break;
                }
            }
        }
    };

    /*重新加载当前页面*/
    public void refreshPage(){
        Intent intent = new Intent();
        intent.putExtra("UserId",UserId);
        intent.putExtra("EmojiId",EmojiId);
        intent.setClass(PersonalBigPictureActivity.this, PersonalBigPictureActivity.class);      //跳转到RegisterActivity
        PersonalBigPictureActivity.this.startActivity(intent);
        finish();    //结束当前Activity
    }

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
                    Log.e(TAG, e.toString());       //输出错误信息
                }


                // 2.设置好IP/端口/数据库名/用户名/密码等必要的连接信息
                String url = "jdbc:mysql://192.168.1.108:3306/emoji?autoReconnect=true";  //此处不能用localhost，必须用10.0.2.2
                String user = "root";
                String password = "yuanshuhan123";                   //数据库的密码和用户名

                // 3.连接JDBC
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    con = DriverManager.getConnection(url, user, password);
                    Log.e(TAG,"连接成功");
                    return;

                } catch (ClassNotFoundException e) {
                    Log.e(TAG,"连接失败1");
                    Log.e(TAG, e.toString());
                } catch (SQLException e) {
                    Log.e(TAG,"连接失败2");
                    Log.e(TAG, e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e(TAG,"连接失败3");
                    Log.e(TAG, e.toString());
                }
            }
        }
    });
thread.start();                    //所有子线程必需start

}

/*根据EmojiId获取图片链接*/
public String getPictureLink(){
    if(con==null)
        getConnection();                  //连接数据库
    while(con==null){                //由于每个线程是并行的，所以需要等待数据库连接成功后才能进行下一步
        Log.e(TAG,"数据库连接中...");
        try {
            Thread.currentThread().sleep(100);          //当前线程睡眠0.1秒
        }catch(InterruptedException e){
            Log.e(TAG,"sleep失败");
            e.printStackTrace();
        }
    }
    Log.i(TAG,"数据库连接成功");

    String sql = "select Link from Total_Emoji where EmojiId="+EmojiId;        //根据EmojiId获取图片链接
    PreparedStatement pstmt;
    String link=null;
    try {
        pstmt = (PreparedStatement)con.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        if(rs.next())
            link=rs.getString("Link");
    } catch (SQLException e) {
        e.printStackTrace();
        Log.e(TAG,"sql执行失败");
    }
    return link;
}

/*根据EmojiId和UserId获取图片标签*/
public String getPictureLabel(){
    if(con==null)
        getConnection();                  //连接数据库
    while(con==null){                //由于每个线程是并行的，所以需要等待数据库连接成功后才能进行下一步
        Log.e(TAG,"数据库连接中...");
        try {
            Thread.currentThread().sleep(100);          //当前线程睡眠0.1秒
        }catch(InterruptedException e){
            Log.e(TAG,"sleep失败");
            e.printStackTrace();
        }
    }
    Log.i(TAG,"数据库连接成功");

    String sql = "select EmojiLabel from Total_Label where UserId='"+UserId+"' and EmojiId="+EmojiId;
    PreparedStatement pstmt;
    String label=null;
    try {
        pstmt = (PreparedStatement)con.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        if(rs.next())
            label=rs.getString("EmojiLabel");
    } catch (SQLException e) {
        e.printStackTrace();
        Log.e(TAG,"sql执行失败");
    }
    return label;
}

/*根据图片链接获取网络图片，注意AndroidManifest.xml中要加一行网络允许才能进行联网操作*/
public Bitmap getPicture(String path){
    Bitmap bm=null;
    try{
        URL url=new URL(path);
        URLConnection connection=url.openConnection();
        connection.connect();
        InputStream inputStream=connection.getInputStream();
        bm= BitmapFactory.decodeStream(inputStream);
    } catch (MalformedURLException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }
    return  bm;
}

    /*更新数据库中的标签*/
    public void updateLabel(final String label_new){
        if(con==null)
            getConnection();                  //连接数据库
        while(con==null){                //由于每个线程是并行的，所以需要等待数据库连接成功后才能进行下一步
            Log.e(TAG,"数据库连接中...");
            try {
                Thread.currentThread().sleep(100);          //当前线程睡眠0.1秒
            }catch(InterruptedException e){
                Log.e(TAG,"sleep失败");
                e.printStackTrace();
            }
        }
        Log.i(TAG,"数据库连接成功");

        String sql = "update Total_Label set EmojiLabel='"+label_new+"' where EmojiId="+EmojiId+" and UserId='"+UserId+"';";   //修改对应的标签
        PreparedStatement pstmt;
        try {
            pstmt = (PreparedStatement)con.prepareStatement(sql);
            pstmt.execute();
            Log.i(TAG,"图片标签修改成功");

        } catch (SQLException e) {
            e.printStackTrace();
            Log.e(TAG,"sql执行失败");
        }

    }

    /*分别显示每个标签*/
    public void ShowPictureLabel(){
        String []labels=label.split("\\s");
        int i;
        int n=0;
        for(i=0;i<labels.length&&i<7;i++){            //最多只显示7个标签
            Log.i(TAG,"第"+i+"个标签:"+labels[i]);
            if(i==0||n>=50) {             //每行显示的标签的总字数不超过50，超过则另起一行
                Message msg = new Message();
                msg.what = CREATELAYOUT;
                handler.sendMessage(msg);                 //新建一个水平布局
                n=0;
                Log.i(TAG,"创建布局");
            }

            try {
                Thread.sleep(100);             //当前线程睡眠0.1秒，等待主线程完成更新操作
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            l=labels[i];
            Message msg1=new Message();
            msg1.what=CREATETEXTVIEW;
            handler.sendMessage(msg1);        //新建一个标签文字框并显示
            Log.i(TAG,"创建文字框");
            try {
                Thread.sleep(100);             //当前线程睡眠0.1秒，等待主线程完成更新操作
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            n+=labels[i].length();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

        if (requestCode == Config.REQUESTCODE_LOCALPHOTOS && resultCode == RESULT_OK) {
            List<String> localPhotos = (List<String>) data.getSerializableExtra(GalleryActivity.PHOTOS);
            Message message = handler.obtainMessage();
            message.what = MESSAGE_UPLOAD_2_OSS;
            message.obj = localPhotos;
            message.sendToTarget();
        }

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
            Toast.makeText(PersonalBigPictureActivity.this, "错误的区域", Toast.LENGTH_SHORT).show();
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
            new AlertDialog.Builder(PersonalBigPictureActivity.this).setTitle("错误的区域").setMessage(mRegion).show();
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

/*onCreate函数相当于构造函数，在Activity创建时自动调用，因此界面上的按钮、文字什么的在这里设置*/
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_personalbigpicture);            //表示采用activity_personalbigpicture.xml

    tv=(TextView)findViewById(R.id.info_text);           //对应于xml中的提示信息文字框
    imageView= (ImageView) findViewById(R.id.imageView);
    linearlayout=(LinearLayout) findViewById(R.id.LabelText);       //对应xml中的标签线性布局

    Intent intent = getIntent();
    EmojiId=intent.getStringExtra("EmojiId");       //获取前一个Activity传来的EmojiId和UserId
    UserId=intent.getStringExtra("UserId");

    Log.i(TAG,"EmojiId="+EmojiId);



    //联网操作必须在子线程中进行，否则会报错
    new Thread(new Runnable() {
        @Override
        public void run() {
            link=getPictureLink();     //根据EmojiId获取图片链接
            label=getPictureLabel();          //根据EmojiId获取图片标签

            Log.i(TAG,"label="+label);


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG,e.toString());
                e.printStackTrace();
            }
            if(Integer.parseInt(EmojiId)<=200) {
                final Bitmap bitmap = getPicture(link);   //联网获取图片资源

                //与xml中的imageview对应
                imageView = (ImageView) findViewById(R.id.imageView);

                imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);        //设置图片
                    }
                });
            }

            ShowPictureLabel();    //显示图片标签

        }
    }).start();

    if(Integer.parseInt(EmojiId)>200) {
        mUIDisplayer = new UIDisplayer(this, imageView, this);
        ossService = initOSS(Config.endpoint, Config.bucket, mUIDisplayer);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG,e.toString());
            e.printStackTrace();
        }

        ossService.setNeedDownLoad(false);
        ossService.asyncGetImage(link);

        copyLocalFile();
        initLocalFiles();

        LinearLayout ll=(LinearLayout)findViewById(R.id.utterlayout);
        Button but=new Button(PersonalBigPictureActivity.this);
        but.setText("确定");
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent();
                intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
                intent.setClass(PersonalBigPictureActivity.this, PersonalSpaceActivity.class);      //跳转到PersonalSpaceActivity
                PersonalBigPictureActivity.this.startActivity(intent);
                finish();
            }
        });
        ll.addView(but);
    }

}

    /*下载按钮对应的点击事件*/
    public void Download(View view){
        String link=getPictureLink();
        Intent intent = new Intent();
        intent.putExtra("Link", link);
        intent.setClass(PersonalBigPictureActivity.this, AuthTestActivity.class);
        PersonalBigPictureActivity.this.startActivity(intent);

        info="下载成功";
        Message msg=new Message();
        msg.what=SHOWINFO;
        handler.sendMessage(msg);
    }

/*分享到共享库*/
public void ShareToPublicSpace(View view){

    if(con==null)
        getConnection();                  //连接数据库
    while(con==null){                //由于每个线程是并行的，所以需要等待数据库连接成功后才能进行下一步
        Log.e(TAG,"数据库连接中...");
        try {
            Thread.currentThread().sleep(100);          //当前线程睡眠0.1秒
        }catch(InterruptedException e){
            Log.e(TAG,"sleep失败");
            e.printStackTrace();
        }
    }
    Log.i(TAG,"数据库连接成功");
    Log.i(TAG,"EmijiId:"+EmojiId);

    new Thread(new Runnable() {
        @Override
        public void run() {
            String sql = "select * from Total_Label where UserId='0' and EmojiId="+EmojiId;    //查询共享库中是否已存在该图片
            PreparedStatement pstmt;
            ResultSet rs;
            try {
                pstmt = (PreparedStatement)con.prepareStatement(sql);
                rs=pstmt.executeQuery();
                if(rs.next()){           //该图片已存在于共享库，则显示提示信息
                    Log.i(TAG,"该图片已存在于共享库");
                    info="该图片已在图片库中";
                    Message msg=new Message();
                    msg.what=SHOWINFO;
                    handler.sendMessage(msg);         //显示提示信息
                }
                else{          //共享库中没有该图片，则加入共享库,并输出提示信息
                    Log.i(TAG,"共享库中没有该图片");
                    sql = "insert into Total_Label(EmojiId,UserId,EmojiLabel) values("+EmojiId+",'0','"+label+"');";    //在标签数据库中增加一行
                    pstmt = (PreparedStatement)con.prepareStatement(sql);
                    pstmt.execute();
                    sql = "update Total_Emoji set Type=1 where EmojiId="+EmojiId;            //将图片总库中该图片的类型改为共享
                    pstmt = (PreparedStatement)con.prepareStatement(sql);
                    pstmt.execute();
                    info="分享成功";
                    Message msg=new Message();
                    msg.what=SHOWINFO;
                    handler.sendMessage(msg);         //显示提示信息
                }
            } catch (SQLException e) {
                e.printStackTrace();
                Log.e(TAG,"sql执行失败");
            }

        }
    }).start();
}

public void main(String []args){

}
}
