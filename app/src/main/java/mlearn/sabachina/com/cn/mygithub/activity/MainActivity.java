package mlearn.sabachina.com.cn.mygithub.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import mlearn.sabachina.com.cn.mygithub.activity.UIDisplayer;

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
import mlearn.sabachina.com.cn.mygithub.activity.AuthTestActivity;


/*共享库*/
public class MainActivity extends AppCompatActivity {
    private static String TAG="MainActivity";                 //这个用于Android Monitor中筛选测试输出信息，后面所有的Log xx均用于测试或输出错误信息
    private ImageView imageView;
    private Button but;

    private EditText st;

    private Connection con;

    private String UserId;

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
                    String url = "jdbc:mysql://192.168.1.108/emoji?autoReconnect=true";  //此处不能用localhost，必须用10.0.2.2
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

    /*获取最新6张图片*/
    public ResultSet getLatestPictureLink(){
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
                                                    //Type=1位共享图片
        String sql = "select * from Total_Emoji where Type=1 order by EmojiId desc limit 6";    //从数据库中选出序号最大的6张共享图片
        PreparedStatement pstmt;
        ResultSet rs=null;
        try {
            pstmt = (PreparedStatement)con.prepareStatement(sql);
            rs = pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            Log.e(TAG,"sql执行失败");
        }
        return rs;
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

    /*用于监视图片点击的内部类*/
    class ImageListener implements OnClickListener{
        public void onClick(View v) {              //点击图片，跳转到BigPictureActivity显示大图
            Intent intent = new Intent();
            String EmojiId=v.getTag().toString();        //获取之前附在每张图片上的Tag，即EmojiId
            Log.i(TAG,"EmojiId:"+EmojiId);
            intent.putExtra("EmojiId",EmojiId);          //将被点击图片的EmojiId作为参数传入
            intent.putExtra("UserId",UserId);
            intent.setClass(MainActivity.this, AuthTestActivity.class);      //跳转到BigPictureActivity
            MainActivity.this.startActivity(intent);
        }
    }

    /*用于监视底部导航栏的点击*/
    class BarListener implements OnClickListener{
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.txt_my:
                    intent = new Intent();
                    intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
                    intent.setClass(MainActivity.this, PersonalSpaceActivity.class);
                    MainActivity.this.startActivity(intent);
                    break;
                case R.id.txt_public:
                    break;
                case R.id.txt_setting:
                    intent = new Intent();
                    intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
                    intent.setClass(MainActivity.this, SettingActivity.class);
                    MainActivity.this.startActivity(intent);
                    break;
            }
        }
    }

    /*onCreate函数相当于构造函数，在Activity创建时自动调用，因此界面上的按钮、文字什么的在这里设置*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);            //表示采用activity_main.xml

        Intent intent=getIntent();
        UserId=intent.getStringExtra("UserId");         //获取前一个Activity传来的参数

        //联网操作必须在子线程中进行，否则会报错
        new Thread(new Runnable() {
            @Override
            public void run() {
                ResultSet rs=getLatestPictureLink();         //获取最新6张图片
                for(int i=0;i<6;i++) {
                    String link=null;
                    int EmojiId=0;
                    try {
                        if (rs.next()) {             //获取每张图片的链接和EmojiId
                            link = rs.getString("Link");
                            EmojiId=rs.getInt("EmojiId");
                        }
                    }catch(SQLException e) {
                        Log.e(TAG, "sql error");
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(100);             //睡眠0.1s等待数据库操作完成
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                        e.printStackTrace();
                    }
                    //与xml中的imageview对应
                    if (i == 1)
                        imageView = (ImageView) findViewById(R.id.imageView1);
                    else if (i == 2)
                        imageView = (ImageView) findViewById(R.id.imageView2);
                    else if (i == 3)
                        imageView = (ImageView) findViewById(R.id.imageView3);
                    else if (i == 4)
                        imageView = (ImageView) findViewById(R.id.imageView4);
                    else if (i == 5)
                        imageView = (ImageView) findViewById(R.id.imageView5);
                    else
                        imageView = (ImageView) findViewById(R.id.imageView6);

                    imageView.setTag(EmojiId);           //将每张图片附上对应的EmojiId，以便点击显示相应大图
                    final Bitmap bitmap=getPicture(link);     //获取图片资源
                    //mUIDisplayer = new UIDisplayer(this,imageView, this);
                    imageView.post(new Runnable() {
                        @Override
                        public void run() {

                            imageView.setImageBitmap(bitmap);        //设置图片
                        }
                    });



                }

            }
        }).start();

        //UI操作只能在主线程中进行，因此在这里给每张图片增加点击监听
        imageView= (ImageView) findViewById(R.id.imageView1);
        imageView.setOnClickListener(new ImageListener());
        imageView= (ImageView) findViewById(R.id.imageView2);
        imageView.setOnClickListener(new ImageListener());
        imageView= (ImageView) findViewById(R.id.imageView3);
        imageView.setOnClickListener(new ImageListener());
        imageView= (ImageView) findViewById(R.id.imageView4);
        imageView.setOnClickListener(new ImageListener());
        imageView= (ImageView) findViewById(R.id.imageView5);
        imageView.setOnClickListener(new ImageListener());
        imageView= (ImageView) findViewById(R.id.imageView6);
        imageView.setOnClickListener(new ImageListener());

        txt_setting=(TextView)findViewById(R.id.txt_setting);
        txt_public=(TextView)findViewById(R.id.txt_public);
        txt_my=(TextView)findViewById(R.id.txt_my);
        txt_my.setOnClickListener(new BarListener());
        txt_public.setOnClickListener(new BarListener());
        txt_setting.setOnClickListener(new BarListener());
        txt_setting.setSelected(false);
        txt_public.setSelected(true);
        txt_my.setSelected(false);

    }

    /*搜索按钮对应的触发事件*/
    public void onClick(View view){
        Intent intent = new Intent();
        st=(EditText) findViewById(R.id.search_text);         //对应xml中的搜索框
        String text=st.getText().toString();            //获取搜索框的输入内容
        Log.i(TAG,text);
        intent.putExtra("text", text);                 //用intent来传递text到另一个activity
        intent.putExtra("UserId","0");                 //共享图片对应的UserId为0
        intent.setClass(MainActivity.this, SearchActivity.class);      //跳转到SearchActivity
        MainActivity.this.startActivity(intent);
    }


    public void main(String []args){

    }
}
