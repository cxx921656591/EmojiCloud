package mlearn.sabachina.com.cn.mygithub.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

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

/*共享库与个人库的搜索结果界面*/
public class SearchActivity extends AppCompatActivity {
    private static String TAG="MainActivity";

    //设三个常量，在handler中作为消息进行传递
    public  static final int REFRESH=1;
    public static final int CREATEIMAGE=2;
    public static final int CREATELAYOUT=3;

    private ImageView imageView;
    private Bitmap bitmap;

    private Connection con;

    private ViewGroup group;           //对应xml中的外层linearlayout

    private LinearLayout linearlayout;       //表示当前图片应放置的内层linearlayout

    private String[] PictureLinks=new String[100];      //用来存放搜索到的图片链接
    private int[] PictureIds=new int[100];              //用来存放搜索到的图片EmojiId
    private int iii=0;

    private String UserId;

//连接数据库
public void getConnection(){

    final Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            // 反复尝试连接，直到连接成功后退出循环
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(100);  // 每隔0.1秒尝试连接
                } catch (InterruptedException e) {
                    Log.e(TAG, e.toString());
                }


                // 2.设置好IP/端口/数据库名/用户名/密码等必要的连接信息
                String url = "jdbc:mysql://192.168.1.108:3306/emoji?autoReconnect=true"; // 构建连接mysql的字符串
                String user = "root";
                String password = "yuanshuhan123";

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
thread.start();
}

//根据图片标签搜索数据库,搜索到的图片链接和Id存在成员变量PictureLinks和PictureIds中
public void searchPictureByLabel(String text){
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

    String []labels=text.split("\\s");         //将搜索框的输入内容按空格分成几个关键字
    int m=0;
    for(int n=0;n<labels.length;n++) {         //对每一个关键字进行搜索
        Log.i(TAG,labels[n]);
        String sql = "select * from Total_Emoji,Total_Label where Total_Label.UserId="+UserId+" and Total_Emoji.EmojiId=Total_Label.EmojiId and Total_Label.EmojiLabel like '%" + labels[n] + "%';";
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

}

//根据图片链接得到图片资源
public Bitmap getPicture(String path){
    Bitmap bm=null;
    Log.i(TAG,"path:"+path);
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
                    imageView = new ImageView(SearchActivity.this);
                    MarginLayoutParams mp = new MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT);
                    mp.setMargins(10,10,10,10);           //设置图片间距
                    LayoutParams lp= new LinearLayout.LayoutParams(mp);
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
                    linearlayout=new LinearLayout(SearchActivity.this);
                    LayoutParams layoutParams=new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 400);       //高度为400像素
                    //将以上的属性赋给LinearLayout
                    linearlayout.setLayoutParams(layoutParams);
                    group.addView(linearlayout);       //在最外层的linearlayout中增加这个新建的linearlayout
                    break;
                }
            }
        }
    };

    /*图片点击监听*/
    class ImageListener implements View.OnClickListener {
        public void onClick(View v) {
            Intent intent = new Intent();
            String EmojiId=v.getTag().toString();           //获取附在图片上的EmojiId
            Log.i(TAG,"EmojiId:"+EmojiId);
            intent.putExtra("EmojiId",EmojiId);             //将EmojiId作为参数传到BigPictureActivity
            intent.putExtra("UserId",UserId);
            if(UserId.equals("0"))             //若是共享图片，则跳转到共享图片对应的大图界面
                intent.setClass(SearchActivity.this, AuthTestActivity.class);
            else         //私有图片
                intent.setClass(SearchActivity.this, PersonalBigPictureActivity.class);
            SearchActivity.this.startActivity(intent);
        }
    }

 //根据搜索框的输入搜索数据库，显示搜索结果
public void ShowPicture(final String text){
    //新建一个子进程
    new Thread(new Runnable() {
        @Override
        public void run() {

            searchPictureByLabel(text);        //根据搜索框输入得到搜索结果
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
}
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_search);       //使用activity_search.xml

    Intent intent = getIntent();
    String text = intent.getStringExtra("text");         //获取MainActivity中传递来的text参数
    Log.i(TAG,"search:"+text);
    UserId=intent.getStringExtra("UserId");
    group = (ViewGroup) findViewById(R.id.viewGroup);     //设置group对应xml中的最外层linearlayout
    ShowPicture(text);                                    //显示搜索结果
}

public void main(String []args){

}
}
