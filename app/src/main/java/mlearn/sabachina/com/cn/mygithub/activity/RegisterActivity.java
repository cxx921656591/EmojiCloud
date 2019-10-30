package mlearn.sabachina.com.cn.mygithub.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import mlearn.sabachina.com.cn.mygithub.R;

/*注册*/
public class RegisterActivity extends AppCompatActivity {
    private static String TAG="MainActivity";                 //这个用于Android Monitor中筛选测试输出信息，后面所有的Log xx均用于测试或输出错误信息

    public  static final int SHOWERROR=1;    //用于handler的消息
    private EditText et;
    private TextView tv;
    private String error_info=null;

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

    //由于更新UI只能在主线程中进行，因此需要设置一个handler处理子线程传递来的消息，从而进行各种UI的操作
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case SHOWERROR:               //更新界面，显示错误信息
                    tv.setText(error_info);
                    break;
            }
        }
    };

    /*注册按钮对应的点击事件*/
    public void Register(View view){
        et=(EditText)findViewById(R.id.phonenumber);
        final String phonenumber=et.getText().toString();        //获取输入的手机号
        et=(EditText)findViewById(R.id.password);
        final String password=et.getText().toString();           //获取输入的密码
        et=(EditText)findViewById(R.id.password_ensure);
        final String password_ensure=et.getText().toString();     //获取第二次输入的密码

        if(phonenumber.equals("")||password.equals("")||password_ensure.equals("")){        //输入为空
            error_info="请输入手机号、密码及确认密码！";
            Message msg2=new Message();
            msg2.what=SHOWERROR;
            handler.sendMessage(msg2);           //显示提示消息
            return;
        }
        if(!password.equals(password_ensure)){      //两遍密码不同
            Log.e(TAG,"密码错误");
            error_info="密码输入错误，请重新输入";
            Message msg=new Message();
            msg.what=SHOWERROR;
            handler.sendMessage(msg);  //发送消息，显示错误信息
            return;
        }


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

        new Thread(new Runnable() {
            @Override
            public void run() {
            String sql = "insert into Login_Info(PhoneNumber,Password) values("+phonenumber+","+password+");";     //增加一个用户登录信息
            PreparedStatement pstmt;
            try {
                pstmt = (PreparedStatement)con.prepareStatement(sql);
                pstmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                Log.e(TAG,"sql执行失败");
            }

            sql = "insert into User_Info(UserId,UserName,TotalCapacity,CurrentCapacity,Num) values("+phonenumber+","+phonenumber+",100,0,0);"; //登录一个用户基本信息，用户初始的用户名为手机号
            try {
                pstmt = (PreparedStatement)con.prepareStatement(sql);
                pstmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                Log.e(TAG,"sql执行失败");
            }
            }
        }).start();

        error_info="注册成功";
        Message msg1=new Message();
        msg1.what=SHOWERROR;
        handler.sendMessage(msg1);   //显示提示信息

        Intent intent = new Intent();
        intent.putExtra("UserId", phonenumber);                 //用intent来传递UserId到另一个activity
        intent.setClass(RegisterActivity.this, PersonalSpaceActivity.class);      //跳转到PersonalSpaceActivity
        RegisterActivity.this.startActivity(intent);
        finish();             //结束当前Activity
    }


    /*onCreate函数相当于构造函数，在Activity创建时自动调用，因此界面上的按钮、文字什么的在这里设置*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);            //表示采用activity_register.xml

        tv=(TextView)findViewById(R.id.error_info);         //对应xml中的提示信息文字框
    }


    public void main(String []args){

    }
    }
