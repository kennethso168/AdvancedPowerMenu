package hk.kennethso168.xposed.advancedrebootmenu.actions;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;
import de.robv.android.xposed.XposedBridge;

/**
 * Action to initiate a quick dial to a predetermined phone number
 */
public class QuickDialAction extends SinglePressAction {
    String mLabelText;
    String mDialNumber;
    Drawable mQuickDialIcon;

    public QuickDialAction(Context context, String labelText, String dialNumber, Drawable quickDialIcon) {
        super(context);
        mLabelText = labelText;
        mDialNumber = dialNumber;
        mQuickDialIcon = quickDialIcon;
    }
    
    @Override
    protected void setupLabel(TextView labelView) {
        labelView.setText(mLabelText);
    }
    
    @Override
    protected void setupIcon(ImageView icon) {
    	icon.setImageDrawable(mQuickDialIcon);
    }
    
    @Override
    protected void onPress() {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("tel:"  + mDialNumber));
            mContext.startActivity(intent);
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}
