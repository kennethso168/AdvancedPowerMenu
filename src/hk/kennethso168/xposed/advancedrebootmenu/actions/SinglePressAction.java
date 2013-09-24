package hk.kennethso168.xposed.advancedrebootmenu.actions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public abstract class SinglePressAction implements InvocationHandler {

    protected Context mContext;
    
    public SinglePressAction(Context context) {
        mContext = context;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
    
        if (methodName.equals("create")) {
            return create((Context) args[0], (View) args[1], (ViewGroup) args[2], (LayoutInflater) args[3]);
        } else if (methodName.equals("onPress")) {
            onPress();
            return null;
        } else if (methodName.equals("onLongPress")) {
            return onLongPress();
        } else if (methodName.equals("showDuringKeyguard")) {
            return showDuringKeyguard();
        } else if (methodName.equals("showBeforeProvisioning")) {
            return showBeforeProvisioning();
        } else if (methodName.equals("isEnabled")) {
            return isEnabled();
        } else {
            return null;
        }
    }
    
    protected View create(Context context, View convertView, ViewGroup parent, LayoutInflater inflater) {
        mContext = context;
        Resources res = mContext.getResources();
        int layoutId = res.getIdentifier(
                "global_actions_item", "layout", "android");
        View v = inflater.inflate(layoutId, parent, false);

        ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                "icon", "id", "android"));
        setupIcon(icon);

        TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                "message", "id", "android"));
        setupLabel(messageView);

        TextView statusView = (TextView) v.findViewById(res.getIdentifier(
                "status", "id", "android"));
        statusView.setVisibility(View.GONE);

        return v;
    }
    
    protected abstract void setupIcon(ImageView icon);

    protected abstract void setupLabel(TextView labelView);
    
    protected abstract void onPress();
    
    protected boolean onLongPress() {
        return false;
    }
    
    protected boolean showDuringKeyguard() {
        return true;
    }

    protected boolean showBeforeProvisioning() {
        return true;
    }
    
    protected boolean isEnabled() {
        return true;
    }
}
