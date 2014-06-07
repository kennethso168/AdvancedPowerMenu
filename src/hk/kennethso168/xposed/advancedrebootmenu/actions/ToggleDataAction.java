package hk.kennethso168.xposed.advancedrebootmenu.actions;

import hk.kennethso168.xposed.advancedrebootmenu.Main;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.widget.ImageView;
import android.widget.TextView;
import de.robv.android.xposed.XposedBridge;


public class ToggleDataAction extends SinglePressAction {
	private static final String CLASS = "ToggleDataAction.java";
	
	private static Context mContext;
	private String mToggleDataLabelOn;
	private String mToggleDataLabelOff;
	private Drawable mToggleDataIcon;
	
	private static void log(String message) {
    	if(Main.writeLogs()) XposedBridge.log(CLASS + ": " + message);
    }
	
	private static void log(Throwable t){
    	if(Main.writeLogs()){
    		XposedBridge.log(CLASS + ": ");
    		XposedBridge.log(t);
    	}    	
    }
	
	private Boolean dataEnabled(){
		boolean mobileDataEnabled = false; // Assume disabled
	    ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	    try {
	        Class<?> cmClass = Class.forName(cm.getClass().getName());
	        Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
	        method.setAccessible(true); // Make the method callable
	        // get the setting for "mobile data"
	        mobileDataEnabled = (Boolean)method.invoke(cm);
	    } catch (Exception e) {
	        // Some problem accessible private API
	    	log("Can't determine mobile data status.");
	    }
	    return mobileDataEnabled;
	}

	public ToggleDataAction(Context context, String labelOn, String labelOff, Drawable icon) {

		super(context);
		
		mContext = context;
        mToggleDataLabelOn = labelOn;
        mToggleDataLabelOff = labelOff;
        mToggleDataIcon = icon;
	}

	@Override
	protected void setupIcon(ImageView icon) {
		icon.setImageDrawable(mToggleDataIcon);

	}

	@Override
	protected void setupLabel(TextView labelView) {
		if(dataEnabled()){
			labelView.setText(mToggleDataLabelOff);
		}else{
			labelView.setText(mToggleDataLabelOn);
		}
	}

	@Override
	protected void onPress() {
		try {
			ConnectivityManager dataManager;
			dataManager  = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			Method dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
			dataMtd.setAccessible(true);
			if(dataEnabled()){
				dataMtd.invoke(dataManager, false);
			}else{
				dataMtd.invoke(dataManager, true);
			}
			
		} catch (IllegalArgumentException e) {
			log(e);
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			log(e);
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			log(e);
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			log(e);
			e.printStackTrace();
		}
		
		
	}

}
