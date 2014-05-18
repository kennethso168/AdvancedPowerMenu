package hk.kennethso168.xposed.advancedrebootmenu.actions;

import hk.kennethso168.xposed.advancedrebootmenu.Main;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;

public class ExpandStatusBarAction extends SinglePressAction{
	private String mLabelText;
    private Drawable mIcon;
    private static Context mContext;
	
	public ExpandStatusBarAction(Context context, String expandStatusBarLabel, Drawable expandStatusBarIcon) {
		super(context);
		mLabelText = expandStatusBarLabel;
		mIcon = expandStatusBarIcon;
		mContext = context;
	}

	@Override
	protected void setupIcon(ImageView icon) {
		icon.setImageDrawable(mIcon);
	}

	@Override
	protected void setupLabel(TextView labelView) {
		labelView.setText(mLabelText);	
	}

	@Override
	protected void onPress() {
		// TODO Auto-generated method stub
		int currentApiVersion = android.os.Build.VERSION.SDK_INT;
	    try {
	        Object service = mContext.getSystemService("statusbar");
	        Class<?> statusbarManager = Class
	                .forName("android.app.StatusBarManager");
	        XposedBridge.log(statusbarManager.getName());
	        Method expand = null;
	        if (service != null) {
	            if (currentApiVersion <= 16) {
	                expand = statusbarManager.getMethod("expand");
	            } else {
	                expand = statusbarManager
	                        .getMethod("expandNotificationsPanel");
	            }
	            expand.invoke(service);
	        } else {
	        	XposedBridge.log("APM: service is null");
	        }

	    } catch (Exception e) {
	    	XposedBridge.log(e);
	    }
	}

}
