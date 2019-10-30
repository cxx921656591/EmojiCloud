package mlearn.sabachina.com.cn.mygithub.activity;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by lenovo on 2018/3/15.
 */

public class ToastUtils {

    protected static Toast toast = null;

    private static volatile mlearn.sabachina.com.cn.mygithub.activity.ToastUtils mToastUtils;

    private ToastUtils(Context context) {
        toast = Toast.makeText(context.getApplicationContext(), null, Toast.LENGTH_SHORT);
    }

   /* public static ToastUtils getInstance(Context context) {
        if (null == mToastUtils) {
            synchronized (ToastUtils.class) {
                if (null == mToastUtils) {
                    mToastUtils = new ToastUtils(context);
                }
            }
        }
        return mToastUtils;
    }

    public static void showMessage(int toastMsg) {
        toast.setText(toastMsg);
        toast.show();
    }*/

    public static void showMessage(String toastMsg) {
        toast.setText(toastMsg);
        toast.show();
    }

    /*public static void toastCancel() {
        if (null != toast) {
            toast.cancel();
            toast = null;
        }
        mToastUtils = null;
    }*/

}
