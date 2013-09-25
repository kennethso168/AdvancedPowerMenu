package hk.kennethso168.xposed.advancedrebootmenu.actions;

import de.robv.android.xposed.XposedBridge;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.ImageView;
import android.widget.TextView;

public class ScreenshotAction extends SinglePressAction {
    private static Context mContext;
    private String mScreenshotLabel;
    private Drawable mScreenshotIcon;
    
    private static final Object mScreenshotLock = new Object();
    private static ServiceConnection mScreenshotConnection = null;  
    
    public ScreenshotAction(Context context, String screenshotLabel, Drawable screenshotIcon) {
        super(context);
        mContext = context;
        mScreenshotLabel = screenshotLabel;
        mScreenshotIcon = screenshotIcon;
    }
    
    @Override
    protected void setupLabel(TextView labelView) {
        labelView.setText(mScreenshotLabel);
    }
    
    @Override
    protected void setupIcon(ImageView icon) {
        icon.setImageDrawable(mScreenshotIcon);
    }

    @Override
    protected void onPress() {
        final Handler handler = new Handler();
        //take a screenshot after a 0.5s delay
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                takeScreenshot(handler);
            }
        }, 500);
    }
    
    private static void takeScreenshot(final Handler mHandler) {  
        synchronized (mScreenshotLock) {  
            if (mScreenshotConnection != null) {  
                return;  
            }  
            ComponentName cn = new ComponentName("com.android.systemui",  
                    "com.android.systemui.screenshot.TakeScreenshotService");  
            Intent intent = new Intent();  
            intent.setComponent(cn);  
            ServiceConnection conn = new ServiceConnection() {  
                @Override  
                public void onServiceConnected(ComponentName name, IBinder service) {  
                    synchronized (mScreenshotLock) {  
                        if (mScreenshotConnection != this) {  
                            return;  
                        }  
                        Messenger messenger = new Messenger(service);  
                        Message msg = Message.obtain(null, 1);  
                        final ServiceConnection myConn = this;  
                                                
                        Handler h = new Handler(mHandler.getLooper()) {  
                            @Override  
                            public void handleMessage(Message msg) {  
                                synchronized (mScreenshotLock) {  
                                    if (mScreenshotConnection == myConn) {  
                                        mContext.unbindService(mScreenshotConnection);  
                                        mScreenshotConnection = null;  
                                        mHandler.removeCallbacks(mScreenshotTimeout);  
                                    }  
                                }  
                            }  
                        };  
                        msg.replyTo = new Messenger(h);  
                        msg.arg1 = msg.arg2 = 0;  
                        try {  
                            messenger.send(msg);  
                        } catch (RemoteException e) {
                            XposedBridge.log(e);
                        }  
                    }  
                }  
                @Override  
                public void onServiceDisconnected(ComponentName name) {}  
            };  
            if (mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {  
                mScreenshotConnection = conn;  
                mHandler.postDelayed(mScreenshotTimeout, 10000);  
            }  
        }
    }
    
    private static final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };
}
