package hk.kennethso168.xposed.advancedrebootmenu.actions;

import hk.kennethso168.xposed.advancedrebootmenu.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class AntiTheftHelperAction extends SinglePressAction {

	String mLabelText;
    Drawable mAntiTheftHelperIcon;
    String mAntiTheftHelperDialogTitle;
    String mAntiTheftHelperDialogMsg;
	
	public AntiTheftHelperAction(Context context, String labelText, Drawable antiTheftHelperIcon, 
			String antiTheftHelperDialogTitle, String antiTheftHelperDialogMsg) {
        super(context);
        mLabelText = labelText;
        mAntiTheftHelperIcon = antiTheftHelperIcon;
        mAntiTheftHelperDialogMsg = antiTheftHelperDialogMsg;
        mAntiTheftHelperDialogTitle = antiTheftHelperDialogTitle;
    }
	
	@Override
	protected void setupIcon(ImageView icon) {
		icon.setImageDrawable(mAntiTheftHelperIcon);
	}

	@Override
	protected void setupLabel(TextView labelView) {
		labelView.setText(mLabelText);
	}

	@Override
	protected void onPress() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
        .setTitle(mAntiTheftHelperDialogTitle)
        .setMessage(mAntiTheftHelperDialogMsg)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
            	dialog.dismiss();
            }
        });
    	AlertDialog dialog = builder.create();
    	dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
    	dialog.show();
	}

}
