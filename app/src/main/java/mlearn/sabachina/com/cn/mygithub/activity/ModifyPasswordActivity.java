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
import java.sql.ResultSet;
import java.sql.SQLException;

import mlearn.sabachina.com.cn.mygithub.R;

/*修改密码*/
public class ModifyPasswordActivity extends AppCompatActivity {
    private static String TAG="MainActivity";                 //这个用于Android Monitor中筛选测试输出信息，后面所有的Log xx均用于测试或输出错误信息

    public  static final int SHOWINFO=1;    //用于handler的消息
    private EditText et;
    private TextView tv;
    private String error_info=null;

    private Connection con;

    private String UserId;

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
                case SHOWINFO:               //更新界面，显示错误信息
                    tv.setText(error_info);
                    break;
            }
        }
    };

    /*确认按钮对应的点击事件*/
    public void ModifyPassword(View view){
        et=(EditText)findViewById(R.id.original_password);
        final String original_password=et.getText().toString();
        et=(EditText)findViewById(R.id.new_password);
        final String new_password=et.getText().toString();           //获取输入的密码
        et=(EditText)findViewById(R.id.new_password_ensure);
        final String new_password_ensure=et.getText().toString();     //获取第二次输入的密码

        if(original_password.equals("")||new_password.equals("")||new_password_ensure.equals("")){        //输入为空
            error_info="请输入完毕后再点击确定按钮！";
            Message msg2=new Message();
            msg2.what=SHOWINFO;
            handler.sendMessage(msg2);           //显示提示消息
            return;
        }
        if(!new_password.equals(new_password_ensure)){      //两遍密码不同
            Log.e(TAG,"密码错误");
            error_info="两遍密码输入不同，请重新输入";
            Message msg=new Message();
            msg.what=SHOWINFO;
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
        Log.i(TAG,"original password="+original_password);
        Log.i(TAG,"UserId="+UserId);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String sql = "select * from Login_Info where Password='" + original_password + "'and PhoneNumber='" + UserId + "';";
                    PreparedStatement pstmt;
                    ResultSet rs;

                    pstmt = (PreparedStatement) con.prepareStatement(sql);
                    rs = pstmt.executeQuery();
                    if (!rs.next()) {
                        Log.i(TAG, "原密码输入错误");
                        error_info = "原密码输入错误，请重新输入";
                        Message msg = new Message();
                        msg.what = SHOWINFO;
                        handler.sendMessage(msg);         //显示提示信息
                    } else {
                        sql = "update Login_Info set Password='" + new_password + "';";
                        pstmt = (PreparedStatement) con.prepareStatement(sql);
                        pstmt.execute();

                        Log.i(TAG, "密码修改成功");
                        error_info = "密码修改成功";
                        Message msg3 = new Message();
                        msg3.what = SHOWINFO;
                        handler.sendMessage(msg3);         //显示提示信息

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Log.e(TAG,e.toString());
                            e.printStackTrace();
                        }

                        Intent intent = new Intent();
                        intent.putExtra("UserId", UserId);                 //用intent来传递UserId到另一个activity
                        intent.setClass(ModifyPasswordActivity.this, SettingActivity.class);      //跳转到PersonalSpaceActivity
                        ModifyPasswordActivity.this.startActivity(intent);
                        finish();             //结束当前Activity
                    }
                }catch (SQLException e) {
                    e.printStackTrace();
                    Log.e(TAG,"sql执行失败");
                }
            }
        }).start();


    }


    /*onCreate函数相当于构造函数，在Activity创建时自动调用，因此界面上的按钮、文字什么的在这里设置*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modifypassword);

        tv=(TextView)findViewById(R.id.error_info);         //对应xml中的提示信息文字框

        Intent intent=getIntent();
        UserId=intent.getStringExtra("UserId");
    }


    public void main(String []args){

    }
    }
