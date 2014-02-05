package hk.kennethso168.xposed.advancedrebootmenu.actions;

import hk.kennethso168.xposed.advancedrebootmenu.Main;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import de.robv.android.xposed.XposedBridge;

public class FakePowerOffAction extends SinglePressAction {
	public static final String PACKAGE_NAME = "android";
	private static final String CLASS = "FakePowerOffAction.java";
	
	private static Context mContext;
	private static String mPowerOffLabel;
	private static String mShutdownConfirmMsg;
	private static String mShutdownProgressMsg;
	private Drawable mPowerOffIcon;
	private Resources mRes;
	
	private static void log(String message) {
    	if(Main.writeLogs()) XposedBridge.log(CLASS + ": " + message);
    }
	
	private static void log(Throwable t){
    	if(Main.writeLogs()){
    		XposedBridge.log(CLASS + ": ");
    		XposedBridge.log(t);
    	}    	
    }

	public FakePowerOffAction(Context context, Drawable icon) {
		super(context);
		mContext = context;
		mRes = mContext.getResources();
		int powerOffStrId = mRes.getIdentifier("power_off", "string", PACKAGE_NAME);		
        mPowerOffLabel =  (powerOffStrId == 0) ? "Power off" : mRes.getString(powerOffStrId);
        mPowerOffIcon = icon;
        int shutdownConfirmMsgId = mRes.getIdentifier("shutdown_confirm", "string", PACKAGE_NAME);
        mShutdownConfirmMsg = (shutdownConfirmMsgId == 0) ? "Your phone will shut down." : mRes.getString(shutdownConfirmMsgId);
        int shutdownProgressMsgId = mRes.getIdentifier("shutdown_progress", "string", PACKAGE_NAME);
        mShutdownProgressMsg = (shutdownProgressMsgId == 0) ? "Shutting down..." : mRes.getString(shutdownProgressMsgId);
	}

	@Override
	protected void setupIcon(ImageView icon) {
		icon.setImageDrawable(mPowerOffIcon);
	}

	@Override
	protected void setupLabel(TextView labelView) {
		labelView.setText(mPowerOffLabel);

	}

	@Override
	protected void onPress() {
		showConfirmDialog();
	}
	
	private static void showConfirmDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
        .setTitle(mPowerOffLabel)
        .setMessage(mShutdownConfirmMsg)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        	@Override
            public void onClick(DialogInterface dialog, int which) {
        		showProgressDialog();
            }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        	@Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    	AlertDialog dialog = builder.create();
    	dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
    	dialog.show();
    }
	
	private static void showProgressDialog(){
		ProgressDialog progress = new ProgressDialog(mContext);
		progress.setTitle(mPowerOffLabel);
		progress.setMessage(mShutdownProgressMsg);
		progress.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		progress.show();
	}

}
