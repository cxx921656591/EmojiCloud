package mlearn.sabachina.com.cn.mygithub.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import mlearn.sabachina.com.cn.mygithub.R;


/*个人设置*/
public class SettingActivity extends AppCompatActivity {
    private static String TAG="MainActivity";                 //这个用于Android Monitor中筛选测试输出信息，后面所有的Log xx均用于测试或输出错误信息

    private Connection con;

    private String UserId;

    private TextView username_tv,space_tv,num_tv;

    private TextView txt_my,txt_public,txt_setting;

    private Intent intent;

    private String username;
    private int num;
    private double TotalCapacity,CurrentCapacity;

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

    /*根据UserId获取用户名及存储空间使用情况*/
    public void getUserInfo(){
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

        String sql = "select * from User_Info where UserId='"+UserId+"';";
        PreparedStatement pstmt;
        try {
            pstmt = (PreparedStatement)con.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                username=rs.getString("UserName");
                num=rs.getInt("Num");
                TotalCapacity=rs.getDouble("TotalCapacity");
                CurrentCapacity=rs.getDouble("CurrentCapacity");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Log.e(TAG,"sql执行失败");
        }
    }

    /*用于监视底部导航栏的点击*/
    class BarListener implements OnClickListener{
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.txt_my:
                    intent = new Intent();
                    intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
                    intent.setClass(SettingActivity.this, PersonalSpaceActivity.class);
                    SettingActivity.this.startActivity(intent);
                    finish();
                    break;
                case R.id.txt_public:
                    intent = new Intent();
                    intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
                    intent.setClass(SettingActivity.this, MainActivity.class);
                    SettingActivity.this.startActivity(intent);
                    finish();
                    break;
                case R.id.txt_setting:
                    break;
            }
        }
    }

    /*修改密码*/
    public void ModifyPassword(View v){
        intent = new Intent();
        intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
        intent.setClass(SettingActivity.this, ModifyPasswordActivity.class);
        SettingActivity.this.startActivity(intent);
    }

    /*onCreate函数相当于构造函数，在Activity创建时自动调用，因此界面上的按钮、文字什么的在这里设置*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);            //表示采用activity_main.xml

        Intent intent=getIntent();
        UserId=intent.getStringExtra("UserId");         //获取前一个Activity传来的参数
        Log.i(TAG,"Setting UserId="+UserId);

        txt_setting=(TextView)findViewById(R.id.txt_setting);
        txt_public=(TextView)findViewById(R.id.txt_public);
        txt_my=(TextView)findViewById(R.id.txt_my);
        txt_my.setOnClickListener(new BarListener());
        txt_public.setOnClickListener(new BarListener());
        txt_setting.setOnClickListener(new BarListener());
        txt_setting.setSelected(true);
        txt_public.setSelected(false);
        txt_my.setSelected(false);

        username_tv=(TextView)findViewById(R.id.setting_username);
        space_tv=(TextView)findViewById(R.id.space_used);
        num_tv=(TextView)findViewById(R.id.num_text);

        //联网操作必须在子线程中进行，否则会报错
        new Thread(new Runnable() {
            @Override
            public void run() {
                getUserInfo();

            }
        }).start();

        try {
            Thread.sleep(1000);             //当前线程睡眠0.1秒，等待主线程完成更新操作
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        username_tv.setText(username);
        space_tv.setText("存储空间使用情况："+CurrentCapacity+"/"+TotalCapacity);
        num_tv.setText("个人空间图片总数："+num);

    }

    public void main(String []args){

    }
}
