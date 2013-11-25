package hk.kennethso168.xposed.advancedrebootmenu;

import hk.kennethso168.xposed.advancedrebootmenu.actions.ExpandStatusBarAction;
import hk.kennethso168.xposed.advancedrebootmenu.actions.QuickDialAction;
import hk.kennethso168.xposed.advancedrebootmenu.actions.ScreenshotAction;
import hk.kennethso168.xposed.advancedrebootmenu.adapters.BasicIconListItem;
import hk.kennethso168.xposed.advancedrebootmenu.adapters.IIconListAdapterItem;
import hk.kennethso168.xposed.advancedrebootmenu.adapters.IconListAdapter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModRebootMenu {
    private static final String CLASS = "ModRebootMenu.java";
    public static final String PACKAGE_NAME = "android";
    public static final String CLASS_GLOBAL_ACTIONS = "com.android.internal.policy.impl.GlobalActions";
    public static final String CLASS_ACTION = "com.android.internal.policy.impl.GlobalActions.Action";
    
    private static Context mContext;
    private static Context armContext;
    private static String mRebootStr;
    private static String mRebootSoftStr;
    private static String mRecoveryStr;
    private static String mBootloaderStr;
    private static String mScreenshotLabel;
    private static String mQuickDialLabel;
    private static String mExpandStatusBarLabel;
    private static String mToggleDataLabel;
    private static Drawable mRebootIcon;
    private static Drawable mRebootSoftIcon;
    private static Drawable mRecoveryIcon;
    private static Drawable mBootloaderIcon;
    private static Drawable mScreenshotIcon;
    private static Drawable mQuickDialIcon;
    private static Drawable mExpandStatusBarIcon;
    private static Drawable mToggleDataIcon;
    private static int[] rebootSubMenu = new int[4];
    private static boolean normalRebootOnly = false;
    private static boolean noLockedOff = false;
    private static List<IIconListAdapterItem> mRebootItemList;
    private static String mRebootConfirmStr;
    private static String mRebootConfirmRecoveryStr;
    private static String mRebootConfirmBootloaderStr;
    private static String noLockedOffDialogTitle;
    private static String noLockedOffDialogMsg;
    private static Unhook mRebootActionHook;
    private static Unhook mPowerOffActionHook;
    private static Method mPowerOffOrigMethod;
    private static XSharedPreferences xPref;
    
    //reboot shell commands
    //  \n executes the command
    private static String mRebootCmd = "reboot\n";
    private static String mRebootRecoveryCmd = 
    		"mkdir -p /cache/recovery\n" +
    		"touch /cache/recovery/boot\n";
    private static String mRebootSoftCmd = 
    		"setprop ctl.restart surfaceflinger\n" +
    		"setprop ctl.restart zygote\n";
    private static String mPowerOffCmd = "reboot -p\n";
    
    //constants for reboot sub-menu sequence
    private static final int SEQ_REBOOT_NORMAL = 0;
    private static final int SEQ_REBOOT_SOFT = 1;
    private static final int SEQ_REBOOT_RECOVERY = 2;
    private static final int SEQ_REBOOT_BOOTLOADER = 3;
    
    //constants for modes of showing confirmation dialogs
    private static final int VALUE_ENABLE_DIALOGS = 0;
    private static final int VALUE_DISABLE_DIALOGS = 1;

    private static void log(String message) {
    	if(Main.writeLogs()) XposedBridge.log(CLASS + ": " + message);
    }
    
    private static void log(Throwable t){
    	if(Main.writeLogs()){
    		XposedBridge.log(CLASS + ": ");
    		XposedBridge.log(t);
    	}    	
    }

    public static void init(final XSharedPreferences pref, final ClassLoader classLoader) {
    	xPref = pref;
        try {
            final Class<?> globalActionsClass = XposedHelpers.findClass(CLASS_GLOBAL_ACTIONS, classLoader);
            final Class<?> actionClass = XposedHelpers.findClass(CLASS_ACTION, classLoader);

            XposedBridge.hookAllConstructors(globalActionsClass, new XC_MethodHook() {
               @Override
               protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                   mContext = (Context) param.args[0];

                   armContext = mContext.createPackageContext(
                           Main.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                   
                   Resources res = mContext.getResources();
                   Resources armRes = armContext.getResources();

                   int rebootStrId = res.getIdentifier("factorytest_reboot", "string", PACKAGE_NAME);
                   mRebootStr  = (rebootStrId == 0) ? "Reboot" : res.getString(rebootStrId);
                   mRebootSoftStr = armRes.getString(R.string.reboot_soft);
                   mRecoveryStr = armRes.getString(R.string.reboot_recovery);
                   mBootloaderStr = armRes.getString(R.string.reboot_bootloader);
                   
                   mScreenshotLabel = armRes.getString(R.string.take_screenshot);
                   mQuickDialLabel = armRes.getString(R.string.quick_dial);
                   mExpandStatusBarLabel = armRes.getString(R.string.expand_statusbar_title);
                   mToggleDataLabel = armRes.getString(R.string.data_toggle);
                   noLockedOffDialogTitle = armRes.getString(R.string.no_locked_off_title);
                   noLockedOffDialogMsg = armRes.getString(R.string.no_locked_off_dialog);
                   
                   //Get user's preference for the menu icon color theme
                   xPref.reload();
                   String IconColorMode = xPref.getString("pref_icon_color", "0");
                   log("IconColorMode = " + IconColorMode);
                   int IconColorInt = Integer.parseInt(IconColorMode);
                   
                   //Create "sets" of icons with different color themes of the same icon in one set
                   int[] mRebootIconSet = {R.drawable.ic_lock_reboot, R.drawable.ic_lock_reboot_dark, R.drawable.ic_lock_reboot_color};
                   int[] mScreenshotIconSet = {R.drawable.ic_screenshot, R.drawable.ic_screenshot_dark, R.drawable.ic_screenshot_color};
                   int[] mQuickDialIconSet = {R.drawable.ic_call, R.drawable.ic_call_dark, R.drawable.ic_call_color};
                   int[] mRebootSoftIconSet = {R.drawable.ic_lock_reboot_soft, R.drawable.ic_lock_reboot_soft_dark, R.drawable.ic_lock_reboot_soft_color};
                   int[] mRecoveryIconSet = {R.drawable.ic_lock_recovery, R.drawable.ic_lock_recovery_dark, R.drawable.ic_lock_recovery_color};
                   int[] mBootloaderIconSet = {R.drawable.ic_lock_reboot_bootloader, R.drawable.ic_lock_reboot_bootloader_dark, R.drawable.ic_lock_reboot_bootloader_color};
                   int[] mExpandStatusBarIconSet = {R.drawable.ic_expand_statusbar, R.drawable.ic_expand_statusbar_dark, R.drawable.ic_expand_statusbar_color};
                   
                   //Set the icons appropriately
                   //1st level icons
                   mRebootIcon = armRes.getDrawable(mRebootIconSet[IconColorInt]);                 
                   mScreenshotIcon = armRes.getDrawable(mScreenshotIconSet[IconColorInt]);
                   mQuickDialIcon = armRes.getDrawable(mQuickDialIconSet[IconColorInt]);
                   mExpandStatusBarIcon = armRes.getDrawable(mExpandStatusBarIconSet[IconColorInt]);
                   mToggleDataIcon = armRes.getDrawable(R.drawable.ic_wip);
                   //2nd level icons
                   //note that the icon for normal reboot is reused.
                   mRebootSoftIcon = armRes.getDrawable(mRebootSoftIconSet[IconColorInt]);
                   mRecoveryIcon = armRes.getDrawable(mRecoveryIconSet[IconColorInt]);
                   mBootloaderIcon = armRes.getDrawable(mBootloaderIconSet[IconColorInt]);

                   noLockedOff = pref.getBoolean("pref_no_locked_off", false);
                   boolean SoftEnabled = pref.getBoolean("pref_rebootsub_soft", true);
                   boolean RecoveryEnabled = pref.getBoolean("pref_rebootsub_recovery", true);
                   boolean BootloaderEnabled = pref.getBoolean("pref_rebootsub_bootloader", true);
                   int cnt = 0;
                   
                   mRebootItemList = new ArrayList<IIconListAdapterItem>();
                   
                   mRebootItemList.add(new BasicIconListItem(mRebootStr, null, mRebootIcon, null));
                   rebootSubMenu[cnt] = SEQ_REBOOT_NORMAL;
                   cnt++;
                   if(SoftEnabled){
                	   mRebootItemList.add(new BasicIconListItem(mRebootSoftStr, null, mRebootSoftIcon, null));
                	   rebootSubMenu[cnt] = SEQ_REBOOT_SOFT;
                	   cnt++;
                   }
                   if(RecoveryEnabled){
                	   mRebootItemList.add(new BasicIconListItem(mRecoveryStr, null, mRecoveryIcon, null));
                	   rebootSubMenu[cnt] = SEQ_REBOOT_RECOVERY;
                	   cnt++;
                   }
                   if(BootloaderEnabled){
                	   mRebootItemList.add(new BasicIconListItem(mBootloaderStr, null, mBootloaderIcon, null));
                	   rebootSubMenu[cnt] = SEQ_REBOOT_BOOTLOADER;
                	   cnt++;
                   }
                   if(cnt==1) normalRebootOnly = true;
                   mRebootConfirmStr = armRes.getString(R.string.reboot_confirm);
                   mRebootConfirmRecoveryStr = armRes.getString(R.string.reboot_confirm_recovery);
                   mRebootConfirmBootloaderStr = armRes.getString(R.string.reboot_confirm_bootloader);

                   log("GlobalActions constructed, resources set.");
               }
            });

            XposedHelpers.findAndHookMethod(globalActionsClass, "createDialog", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mRebootActionHook != null) {
                        log("Unhooking previous hook of reboot action item");
                        mRebootActionHook.unhook();
                        mRebootActionHook = null;
                    }
                }

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mContext == null) return;

                    /*prefs.reload();
                    if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_POWEROFF_ADVANCED, false)) {
                        return;
                    }*/
                    pref.reload();
                    boolean advRebootEnabled = pref.getBoolean("pref_enable_reboot", false);
                    boolean screenshotEnabled = pref.getBoolean("pref_enable_screenshot", false);
                    @SuppressWarnings("unchecked")
                    List<Object> mItems = (List<Object>) XposedHelpers.getObjectField(param.thisObject, "mItems");

                    // try to find out if reboot/screenshot action item 
                    // already exists in the list of GlobalActions items
                    // strategy:
                    // 1) check if Action has mIconResId field or mMessageResId field
                    // 2) check if the name of the corresponding resource contains "reboot" or "restart" substring
                    log("Searching for existing reboot & screenshot action item...");
                    Object rebootActionItem = null;
                    Object screenshotActionItem = null;
                    Object powerOffActionItem = null;
                    Resources res = mContext.getResources();
                    for (Object o : mItems) {
                    	// search for reboot/restart
                    	// search for drawable
                        try {
                            Field f = XposedHelpers.findField(o.getClass(), "mIconResId");
                            String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                            log("Drawable resName = " + resName);
                            if (resName.contains("reboot") || resName.contains("restart")) {
                                rebootActionItem = o;
                                break;
                            }
                        } catch (NoSuchFieldError nfe) {
                            // continue
                        } catch (Resources.NotFoundException resnfe) { 
                            // continue
                        } catch (IllegalArgumentException iae) {
                            // continue
                        }
                        // search for text
                        try {
                            Field f = XposedHelpers.findField(o.getClass(), "mMessageResId");
                            String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                            log("Text resName = " + resName);
                            if (resName.contains("reboot")|| resName.contains("restart")) {
                                rebootActionItem = o;
                                break;
                            }
                        } catch (NoSuchFieldError nfe) {
                        	// continue
                        } catch (Resources.NotFoundException resnfe) { 
                        	// continue
                        } catch (IllegalArgumentException iae) {
                        	// continue
                        }
                         
                        // search for screenshot
                        // search for drawable
                        try {
                            Field f = XposedHelpers.findField(o.getClass(), "mIconResId");
                            String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                            log("Drawable resName = " + resName);
                            if (resName.contains("screenshot")) {
                                screenshotActionItem = o;
                                break;
                            }
                        } catch (NoSuchFieldError nfe) {
                            // continue
                        } catch (Resources.NotFoundException resnfe) { 
                            // continue
                        } catch (IllegalArgumentException iae) {
                            // continue
                        }
                        // search for text
                        try {
                            Field f = XposedHelpers.findField(o.getClass(), "mMessageResId");
                            String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                            log("Text resName = " + resName);
                            if (resName.contains("screenshot")) {
                                screenshotActionItem = o;
                                break;
                            }
                        } catch (NoSuchFieldError nfe) {
                        	// continue
                        } catch (Resources.NotFoundException resnfe) { 
                        	// continue
                        } catch (IllegalArgumentException iae) {
                        	// continue
                        }
                        
                        // TODO NOT WORKING
                        // search for power off
                        // search for drawable
                        try {
                            Field f = XposedHelpers.findField(o.getClass(), "mIconResId");
                            String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                            log("Drawable resName = " + resName);
                            if (resName.contains("power off") || resName.contains("shutdown") 
                            		|| resName.contains("shut down")||resName.contains("off")) {
                                powerOffActionItem = o;
                                break;
                            }
                        } catch (NoSuchFieldError nfe) {
                            // continue
                        } catch (Resources.NotFoundException resnfe) { 
                            // continue
                        } catch (IllegalArgumentException iae) {
                            // continue
                        }
                        // search for text
                        try {
                            Field f = XposedHelpers.findField(o.getClass(), "mMessageResId");
                            String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                            log("Text resName = " + resName);
                            if (resName.contains("power off") || resName.contains("shutdown") 
                            		|| resName.contains("shut down")||resName.contains("off")) {
                                powerOffActionItem = o;
                                break;
                            }
                        } catch (NoSuchFieldError nfe) {
                        	// continue
                        } catch (Resources.NotFoundException resnfe) { 
                        	// continue
                        } catch (IllegalArgumentException iae) {
                        	// continue
                        }
                    }
                    if(advRebootEnabled){
	                    if (rebootActionItem != null) {
	                        log("Existing Reboot action item found! Replacing onPress()");
	                        mRebootActionHook = XposedHelpers.findAndHookMethod(rebootActionItem.getClass(), 
	                                "onPress", new XC_MethodReplacement () {
	                            @Override
	                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
	                                if(normalRebootOnly){
	                                	// TODO handle this when shutdown protection is on
	                                	handleReboot(mContext, mRebootStr, SEQ_REBOOT_NORMAL);
	                                }else{
	                                	showDialog();
	                                }
	                                return null;
	                            }
	                        });
	                    } else {
	                        log("Existing Reboot action item NOT found! Adding new RebootAction item");
	                        Object action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass }, 
	                                new RebootAction());
	                        // add to the second position
	                        mItems.add(1, action);
	                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
	                        mAdapter.notifyDataSetChanged(); 
	                    }
                    }
                    if(screenshotEnabled){
	                    if (screenshotActionItem != null) {
	                    	log("Existing Screenshot action item found! Nothing is done.");
	                    	//no need to do anything!
	                    	//As the original screenshot action item can be left intact
	                    } else {
	                    	log("Existing Screenshot action item NOT found! Adding new ScreenshotAction item");
	                    	Object action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass }, 
	                                new ScreenshotAction(mContext, mScreenshotLabel, mScreenshotIcon));
	                    	// add to the second/third position
	                    	mItems.add(advRebootEnabled?2:1, action);
	                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
	                        mAdapter.notifyDataSetChanged(); 
	                    }
                    }
                    String quickDial = pref.getString("pref_quick_dial_number", "");
                    if (quickDial.length() > 0) {
                        Object action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                                new QuickDialAction(mContext, mQuickDialLabel, quickDial, mQuickDialIcon));
                        // add to the second/third position (before Screenshot, if it exists)
                        mItems.add(advRebootEnabled?2:1, action);
                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mAdapter.notifyDataSetChanged();
                    }
                    boolean expandStatusBarEnabled = pref.getBoolean("pref_expand_statusbar", false);
                    if (expandStatusBarEnabled){
                    	Object action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                                new ExpandStatusBarAction(mContext, mExpandStatusBarLabel, mExpandStatusBarIcon));
                        // add to the second/third position (before Screenshot, if it exists)
                        mItems.add(advRebootEnabled?2:1, action);
                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mAdapter.notifyDataSetChanged();
                    }
                    boolean dataToggleEnabled = pref.getBoolean("pref_data_toggle", false);
                    if (dataToggleEnabled){
                    	//TODO change new class
                    	Object action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                                new ExpandStatusBarAction(mContext, mToggleDataLabel, mToggleDataIcon));
                        // add to the second/third position (before Screenshot, if it exists)
                        mItems.add(advRebootEnabled?2:1, action);
                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mAdapter.notifyDataSetChanged();
                    }
                    
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void showDialog() {
        if (mContext == null) {
            log("mContext is null - aborting");
            return;
        }

        try {
            log("about to build reboot dialog");

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(mRebootStr)
                .setAdapter(new IconListAdapter(mContext, mRebootItemList), new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        log("onClick() item = " + which);
                        handleReboot(mContext, mRebootStr, rebootSubMenu[which]);
                        
                    }
                })
                .setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
    
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                });
            AlertDialog dialog = builder.create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
            dialog.show();
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void handleReboot(Context context, String caption, final int mode) {
        try {
            String message;
            if(mode == 0 || mode == 1){
            	message = mRebootConfirmStr;
            }else if (mode == 2){
            	message = mRebootConfirmRecoveryStr;
            }else{
            	message = mRebootConfirmBootloaderStr;
            }
            xPref.reload();
            String showDialogMode = xPref.getString("pref_confirm_dialog", "3");  //3 is a temp indicator for error
            log("ShowDialogMode = " + showDialogMode);
            int showDialogModeInt = Integer.parseInt(showDialogMode);
	            if (showDialogModeInt == VALUE_DISABLE_DIALOGS){
	            	handleRebootCore(mode);
	            }else{
	            	AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
	                .setTitle(caption)
	                .setMessage(message)
	                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                    	dialog.dismiss();
	                    	handleRebootCore(mode);
	                    }
	                })
	                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	                    
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                        dialog.dismiss();
	                    }
	                });
	            	AlertDialog dialog = builder.create();
	            	dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
	            	dialog.show();
	            }
            	
            
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
    
    private static void handleRebootCore(final int mode){
    	if (mode == SEQ_REBOOT_NORMAL) {
			final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
			pm.reboot(null);
        } else if (mode == SEQ_REBOOT_SOFT) {
        	try {
        		Process proc = Runtime.getRuntime().exec("sh");
        		DataOutputStream stdin = new DataOutputStream(proc.getOutputStream()); 
        		stdin.writeBytes(mRebootSoftCmd);
        		
        	} catch (Exception e) {
        			XposedBridge.log(e);
        	}   
        } else if (mode == SEQ_REBOOT_RECOVERY) {
        	Process p;
        	try {
				p = Runtime.getRuntime().exec("sh");
				DataOutputStream stdin = new DataOutputStream(p.getOutputStream()); 
        		stdin.writeBytes(mRebootRecoveryCmd);
				final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
				pm.reboot("recovery");
			} catch (IOException e) {
				XposedBridge.log(e);
			}
        	
        } else if (mode == SEQ_REBOOT_BOOTLOADER){
        	final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
			pm.reboot("bootloader");
        }
    }
    
    
    private static class RebootAction implements InvocationHandler {
        private Context mContext;

        public RebootAction() {
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("create")) {
                mContext = (Context) args[0];
                Resources res = mContext.getResources();
                LayoutInflater li = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier(
                        "global_actions_item", "layout", "android");
                View v = li.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) v.findViewById(res.getIdentifier(
                        "icon", "id", "android"));
                icon.setImageDrawable(mRebootIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                messageView.setText(mRebootStr);

                TextView statusView = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                statusView.setVisibility(View.GONE);

                return v;
            } else if (methodName.equals("onPress")) {
                KeyguardManager myKM = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
                if( myKM.inKeyguardRestrictedInputMode()&&noLockedOff) {
                	//TODO show a dialog
                	AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
	                .setTitle(noLockedOffDialogTitle)
	                .setMessage(noLockedOffDialogMsg)
	                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                    	dialog.dismiss();
	                    }
	                });
	            	AlertDialog dialog = builder.create();
	            	dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
	            	dialog.show();
                } else {
                 //it is not locked
                	if(normalRebootOnly){
                    	handleReboot(mContext, mRebootStr, SEQ_REBOOT_NORMAL);
                    }else{
                    	showDialog();
                    }
                }
                return null;
            } else if (methodName.equals("onLongPress")) {
            	KeyguardManager myKM = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
                if( myKM.inKeyguardRestrictedInputMode()&&noLockedOff) {
                	//TODO show a dialog
                	AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
	                .setTitle(noLockedOffDialogTitle)
	                .setMessage(noLockedOffDialogMsg)
	                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                    	dialog.dismiss();
	                    }
	                });
	            	AlertDialog dialog = builder.create();
	            	dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
	            	dialog.show();
                } else {
                 //it is not locked
                	handleReboot(mContext, mRebootStr, SEQ_REBOOT_NORMAL);
                }
                return true;
            } else if (methodName.equals("showDuringKeyguard")) {
                return true;
            } else if (methodName.equals("showBeforeProvisioning")) {
                return true;
            } else if (methodName.equals("isEnabled")) {
                return true;
            } else {
                return null;
            }
        }
     
    }

}
