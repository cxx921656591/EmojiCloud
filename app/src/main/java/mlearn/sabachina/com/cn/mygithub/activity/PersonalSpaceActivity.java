package mlearn.sabachina.com.cn.mygithub.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import mlearn.sabachina.com.cn.mygithub.R;


public class PersonalSpaceActivity extends AppCompatActivity {
    private static String TAG="MainActivity";                 //这个用于Android Monitor中筛选测试输出信息，后面所有的Log xx均用于测试或输出错误信息
    private ImageView imageView;
    private EditText et;
    private Connection con;
    private Bitmap bitmap;
    private String UserId;

    //设三个常量，在handler中作为消息进行传递
    public  static final int REFRESH=1;
    public static final int CREATEIMAGE=2;
    public static final int CREATELAYOUT=3;

    private ViewGroup group;           //对应xml中的外层linearlayout

    private LinearLayout linearlayout;       //表示当前图片应放置的内层linearlayout

    private String[] PictureLinks=new String[100];      //用来存放搜索到的图片链接
    private int[] PictureIds=new int[100];              //用来存放搜索到的图片EmojiId
    private int iii=0;

    private TextView txt_my,txt_public,txt_setting;

    private Intent intent;

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

    //根据UserId搜索数据库，得到所有该用户收藏的图片，搜索结果的图片链接与EmojiId存在类成员变量PictureLinks与PictureIds中
    public void getMyPicture(){
        if(con==null)
            getConnection();            //连接数据库
        while(con==null){
            Log.e(TAG,"数据库连接失败");
            try {
                Thread.currentThread().sleep(100);
            }catch(InterruptedException e){
                Log.e(TAG,"sleep失败");
                e.printStackTrace();
            }
        }
        Log.i(TAG,"数据库连接成功");
        int m=0;
        String sql = "select Total_Emoji.Link,Total_Emoji.EmojiId from Total_Emoji,Total_Label where Total_Label.UserId="+UserId+" and Total_Emoji.EmojiId=Total_Label.EmojiId;";
        PreparedStatement pstmt;
        try {
            pstmt = (PreparedStatement) con.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                PictureLinks[m] = rs.getString("Link");
                PictureIds[m]=rs.getInt("EmojiId");
                m++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Log.e(TAG, "sql执行失败");
            Log.e(TAG,e.toString());
        }

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

    //由于更新UI只能在主线程中进行，因此需要设置一个handler处理子线程传递来的消息，从而进行各种UI的操作
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case REFRESH:               //更新界面，显示新增加的imageview
                    linearlayout.addView(imageView);        //在当前linearlayout中增加一个imageview
                    break;
                case CREATEIMAGE:           //创建一个imageview
                {
                    imageView = new ImageView(PersonalSpaceActivity.this);
                    ViewGroup.MarginLayoutParams mp = new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.WRAP_CONTENT, ViewGroup.MarginLayoutParams.WRAP_CONTENT);
                    mp.setMargins(10,10,10,10);           //设置图片间距
                    ViewGroup.LayoutParams lp= new LinearLayout.LayoutParams(mp);
                    lp.height=300;              //设置图片高和宽为300像素
                    lp.width=300;
                    imageView.setLayoutParams(lp);     //应用以上设置的参数
                    imageView.setTag(PictureIds[iii]);     //设置TAG为对应的EmojiId,以便实现点击图片显示大图的功能
                    imageView.setOnClickListener(new ImageListener());        //设置点击监听
                    imageView.setImageBitmap(bitmap);      //设置imageview的图片资源
                    break;
                }
                case CREATELAYOUT:             //创建一个linearlayout
                {
                    linearlayout=new LinearLayout(PersonalSpaceActivity.this);
                    ViewGroup.LayoutParams layoutParams=new ViewGroup.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 400);       //高度为400像素
                    //将以上的属性赋给LinearLayout
                    linearlayout.setLayoutParams(layoutParams);
                    group.addView(linearlayout);       //在最外层的linearlayout中增加这个新建的linearlayout
                    break;
                }
            }
        }
    };

    /*用于监视图片点击的内部类*/
    class ImageListener implements OnClickListener{
        public void onClick(View v) {              //点击图片，跳转到BigPictureActivity显示大图
            Intent intent = new Intent();
            String EmojiId=v.getTag().toString();        //获取之前附在每张图片上的Tag，即EmojiId
            Log.i(TAG,"EmojiId:"+EmojiId);
            intent.putExtra("EmojiId",EmojiId);          //将被点击图片的EmojiId作为参数传入
            intent.putExtra("UserId",UserId);
            intent.setClass(PersonalSpaceActivity.this, PersonalBigPictureActivity.class);      //跳转到BigPictureActivity
            PersonalSpaceActivity.this.startActivity(intent);
        }
    }

    /*用于监视底部导航栏的点击*/
    class BarListener implements OnClickListener{
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.txt_public:
                    intent = new Intent();
                    intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
                    intent.setClass(PersonalSpaceActivity.this, MainActivity.class);
                    PersonalSpaceActivity.this.startActivity(intent);
                    break;
                case R.id.txt_my:
                    break;
                case R.id.txt_setting:
                    intent = new Intent();
                    intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
                    intent.setClass(PersonalSpaceActivity.this, SettingActivity.class);
                    PersonalSpaceActivity.this.startActivity(intent);
                    break;
            }
        }
    }

    /*onCreate函数相当于构造函数，在Activity创建时自动调用，因此界面上的按钮、文字什么的在这里设置*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personalspace);            //表示采用activity_personalspace.xml

        Intent intent=getIntent();
        UserId=intent.getStringExtra("UserId");                   //获取从登陆界面传来的UserId
        Log.i(TAG,UserId);
        group = (ViewGroup) findViewById(R.id.viewGroup);     //设置group对应xml中的最外层linearlayout

        //联网操作必须在子线程中进行，否则会报错
        new Thread(new Runnable() {
            @Override
            public void run() {
                getMyPicture();             //搜索该用户所有收藏图片

                for(iii=0;PictureLinks[iii]!=null;iii++){             //对每一张图片进行操作
                    Log.i(TAG,"第"+iii+"张:"+PictureLinks[iii]);
                    if(iii%3==0){                //每一行用一个水平布局的内层linearlayout显示三张图片，因此当i%3==0时需要新建一个linearlayout
                        Message msg0=new Message();
                        msg0.what=CREATELAYOUT;
                        handler.sendMessage(msg0);        //用handler向主线程发送消息，在前面的handler中进行处理，新建一个linearlayout
                    }

                    bitmap = getPicture(PictureLinks[iii]);         //根据链接获取图片资源

                    Message msg = new Message();
                    msg.what=CREATEIMAGE;
                    handler.sendMessage(msg);           //发送消息，新建一个imageview
                    try {
                        Thread.sleep(100);             //当前线程睡眠0.1秒，等待主线程完成新建imageview操作
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Message msg2=new Message();
                    msg2.what=REFRESH;
                    handler.sendMessage(msg2);          //发送消息，更新界面，显示图片
                    try {
                        Thread.sleep(100);             //当前线程睡眠0.1秒，等待主线程完成更新操作
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            }
        }).start();

        txt_setting=(TextView)findViewById(R.id.txt_setting);
        txt_public=(TextView)findViewById(R.id.txt_public);
        txt_my=(TextView)findViewById(R.id.txt_my);
        txt_my.setOnClickListener(new BarListener());
        txt_public.setOnClickListener(new BarListener());
        txt_setting.setOnClickListener(new BarListener());
        txt_setting.setSelected(false);
        txt_public.setSelected(false);
        txt_my.setSelected(true);

    }

    /*搜索按钮对应的触发事件*/
    public void onClick(View view){
        Intent intent = new Intent();
        et=(EditText) findViewById(R.id.search_text);         //对应xml中的搜索框
        String text=et.getText().toString();            //获取搜索框的输入内容
        Log.i(TAG,text);
        intent.putExtra("text", text);                 //用intent来传递text到另一个activity
        intent.putExtra("UserId",UserId);
        intent.setClass(PersonalSpaceActivity.this, SearchActivity.class);      //跳转到SearchActivity
        PersonalSpaceActivity.this.startActivity(intent);
    }

    /*上传按钮对应的触发事件*/
    public void Upload(View v){
        Intent intent = new Intent();
        intent.putExtra("UserId",UserId);
        intent.setClass(PersonalSpaceActivity.this, ocr_MainActivity.class);      //跳转到SearchActivity
        PersonalSpaceActivity.this.startActivity(intent);
    }


    public void main(String []args){

    }
}
