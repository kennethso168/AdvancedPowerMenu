package hk.kennethso168.xposed.advancedrebootmenu.actions;

import android.content.Context;
import android.content.Intent;
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

    public QuickDialAction(Context context, String labelText, String dialNumber) {
        super(context);
        mLabelText = labelText;
        mDialNumber = dialNumber;
    }
    
    @Override
    protected void setupLabel(TextView labelView) {
        labelView.setText(mLabelText);
    }
    
    @Override
    protected void setupIcon(ImageView icon) {
        icon.setImageResource(android.R.drawable.ic_menu_call);
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
