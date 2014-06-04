package hk.kennethso168.xposed.advancedrebootmenu;

import hk.kennethso168.xposed.advancedrebootmenu.actions.AntiTheftHelperAction;
import hk.kennethso168.xposed.advancedrebootmenu.actions.ExpandStatusBarAction;
import hk.kennethso168.xposed.advancedrebootmenu.actions.FakePowerOffAction;
import hk.kennethso168.xposed.advancedrebootmenu.actions.QuickDialAction;
import hk.kennethso168.xposed.advancedrebootmenu.actions.ScreenshotAction;
import hk.kennethso168.xposed.advancedrebootmenu.actions.ToggleDataAction;
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
import android.os.Build;
import android.os.PowerManager;
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
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class ModRebootMenu {
    private static final String CLASS = "ModRebootMenu.java";
    public static final String PACKAGE_NAME = "android";
    public static final String CLASS_GLOBAL_ACTIONS = "com.android.internal.policy.impl.GlobalActions";
    public static final String CLASS_ACTION = "com.android.internal.policy.impl.GlobalActions.Action";
    public static final String CLASS_SILENT_TRISTATE_ACTION = "com.android.internal.policy.impl.GlobalActions$SilentModeTriStateAction";
    
    private static Context mContext;
    private static Context armContext;
    private static String mRebootStr;
    private static String mRebootSoftStr;
    private static String mRecoveryStr;
    private static String mBootloaderStr;
    private static String mFlashmodeStr;
    private static String mRebootSystem1Str;
    private static String mRebootSystem2Str;
    private static String mRebootSafeStr;
    private static String mScreenshotLabel;
    private static String mQuickDialLabel;
    private static String mExpandStatusBarLabel;
    private static String mToggleDataLabel;
    private static String mToggleDataOnLabel;
    private static String mToggleDataOffLabel;
    private static String mDeviceLockedLabel;
    private static Drawable mRebootIcon;
    private static Drawable mRebootSoftIcon;
    private static Drawable mRecoveryIcon;
    private static Drawable mBootloaderIcon;
    private static Drawable mFlashmodeIcon;
    private static Drawable mScreenshotIcon;
    private static Drawable mQuickDialIcon;
    private static Drawable mExpandStatusBarIcon;
    private static Drawable mToggleDataIcon;
    private static Drawable mDeviceLockedIcon;
    private static Drawable mPowerOffIcon;
    private static int[] rebootSubMenu = new int[5];
    private static boolean normalRebootOnly = false;
    private static boolean antiTheftHelperOn = false;
    private static List<IIconListAdapterItem> mRebootItemList;
    private static String mRebootConfirmStr;
    private static String mRebootConfirmRecoveryStr;
    private static String mRebootConfirmBootloaderStr;
    private static String mRebootConfirmFlashmodeStr;
    private static String noLockedOffDialogTitle;
    private static String noLockedOffDialogMsg;
    private static Unhook mRebootActionHook;
    private static Unhook mPowerOffActionHook;
    private static Unhook mAirplaneActionHook;
    private static XSharedPreferences xPref;
    private static int afterPowerPos = 0;
    private static int afterRebootPos = 0;
    
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
    private static final int SEQ_REBOOT_FLASHMODE = 4;
    private static final int SEQ_REBOOT_SYSTEM1 = 5;
    private static final int SEQ_REBOOT_SYSTEM2 = 6;
    
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
                   mFlashmodeStr = armRes.getString(R.string.reboot_flashmode);
                   mRebootSystem1Str = armRes.getString(R.string.reboot_system1);
                   mRebootSystem2Str = armRes.getString(R.string.reboot_system2);
                   mRebootSafeStr = armRes.getString(R.string.reboot_safe);
                   
                   mScreenshotLabel = armRes.getString(R.string.take_screenshot);
                   mQuickDialLabel = armRes.getString(R.string.quick_dial);
                   mExpandStatusBarLabel = armRes.getString(R.string.expand_statusbar_title);
                   mToggleDataLabel = armRes.getString(R.string.data_toggle);
                   mToggleDataOnLabel = armRes.getString(R.string.mobile_data_on);
                   mToggleDataOffLabel = armRes.getString(R.string.mobile_data_off);
                   mDeviceLockedLabel = armRes.getString(R.string.device_is_locked);
                   noLockedOffDialogTitle = armRes.getString(R.string.no_locked_off_title);
                   noLockedOffDialogMsg = armRes.getString(R.string.no_locked_off_dialog);
                   
                   //Get user's preference for the menu icon color theme
                   xPref.reload();
                   String IconColorMode = xPref.getString("pref_icon_color", "0");
                   log("IconColorMode = " + IconColorMode);
                   int IconColorInt = Integer.parseInt(IconColorMode);
                   
                   //Create "sets" of icons with different color themes of the same icon in one set
                   int[] mRebootIconSet = {R.drawable.ic_lock_reboot, R.drawable.ic_lock_reboot_dark, R.drawable.ic_lock_reboot_color, R.drawable.ic_lock_reboot_existenz};
                   int[] mScreenshotIconSet = {R.drawable.ic_screenshot, R.drawable.ic_screenshot_dark, R.drawable.ic_screenshot_color, R.drawable.ic_screenshot_existenz};
                   int[] mQuickDialIconSet = {R.drawable.ic_call, R.drawable.ic_call_dark, R.drawable.ic_call_color, R.drawable.ic_call_existenz};
                   int[] mRebootSoftIconSet = {R.drawable.ic_lock_reboot_soft, R.drawable.ic_lock_reboot_soft_dark, R.drawable.ic_lock_reboot_soft_color, R.drawable.ic_lock_reboot_soft_existenz};
                   int[] mRecoveryIconSet = {R.drawable.ic_lock_recovery, R.drawable.ic_lock_recovery_dark, R.drawable.ic_lock_recovery_color, R.drawable.ic_lock_recovery_existenz};
                   int[] mBootloaderIconSet = {R.drawable.ic_lock_reboot_bootloader, R.drawable.ic_lock_reboot_bootloader_dark, R.drawable.ic_lock_reboot_bootloader_color, R.drawable.ic_lock_reboot_bootloader_existenz};
                   int[] mFlashmodeIconSet = {R.drawable.ic_lock_reboot_bootloader, R.drawable.ic_lock_reboot_bootloader_dark, R.drawable.ic_lock_reboot_bootloader_color, R.drawable.ic_lock_reboot_bootloader_existenz};
                   int[] mExpandStatusBarIconSet = {R.drawable.ic_expand_statusbar, R.drawable.ic_expand_statusbar_dark, R.drawable.ic_expand_statusbar_color, R.drawable.ic_expand_statusbar_existenz};
                   int[] mToggleDataIconSet = {R.drawable.ic_data, R.drawable.ic_data_dark, R.drawable.ic_data_color, R.drawable.ic_data_existenz};
                   int[] mDeviceLockedIconSet = {R.drawable.ic_device_locked, R.drawable.ic_device_locked_dark, R.drawable.ic_device_locked_color, R.drawable.ic_device_locked_existenz};
                   
                   //Set the icons appropriately
                   //1st level icons
                   mRebootIcon = armRes.getDrawable(mRebootIconSet[IconColorInt]);                 
                   mScreenshotIcon = armRes.getDrawable(mScreenshotIconSet[IconColorInt]);
                   mQuickDialIcon = armRes.getDrawable(mQuickDialIconSet[IconColorInt]);
                   mExpandStatusBarIcon = armRes.getDrawable(mExpandStatusBarIconSet[IconColorInt]);
                   mToggleDataIcon = armRes.getDrawable(mToggleDataIconSet[IconColorInt]);
                   mDeviceLockedIcon = armRes.getDrawable(mDeviceLockedIconSet[IconColorInt]);
                   //2nd level icons
                   //note that the icon for normal reboot is reused.
                   mRebootSoftIcon = armRes.getDrawable(mRebootSoftIconSet[IconColorInt]);
                   mRecoveryIcon = armRes.getDrawable(mRecoveryIconSet[IconColorInt]);
                   mBootloaderIcon = armRes.getDrawable(mBootloaderIconSet[IconColorInt]);
                   mFlashmodeIcon = armRes.getDrawable(mFlashmodeIconSet[IconColorInt]);
                   mPowerOffIcon = armRes.getDrawable(R.drawable.ic_wip); //as a fallback

                   antiTheftHelperOn = pref.getBoolean("pref_no_locked_off", false);
                   boolean SoftEnabled = pref.getBoolean("pref_rebootsub_soft", true);
                   boolean RecoveryEnabled = pref.getBoolean("pref_rebootsub_recovery", true);
                   boolean BootloaderEnabled = pref.getBoolean("pref_rebootsub_bootloader", true);
                   boolean FlashmodeEnabled = pref.getBoolean("pref_rebootsub_flashmode", true);
                   boolean prefSystem12Enabled = pref.getBoolean("pref_rebootsub_system12", false);
                   log("pref_rebootsub_system12 = " + prefSystem12Enabled);
                   boolean System2Enabled = false;
                   boolean System1Enabled = false;
                   if(prefSystem12Enabled) {
                	   System2Enabled = DualBoot.getSyspart()==0;
                	   System1Enabled = DualBoot.getSyspart()==1 || !System2Enabled;
                   }
                   log("System1Enabled = " + System1Enabled);
                   log("System2Enabled = " + System2Enabled);
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
				   if(BootloaderEnabled && Build.MANUFACTURER.toLowerCase().contains("sony")){
                	   mRebootItemList.add(new BasicIconListItem(mFlashmodeStr, null, mFlashmodeIcon, null));
                	   rebootSubMenu[cnt] = SEQ_REBOOT_FLASHMODE;
                	   cnt++;
                   }
                   if(System1Enabled&&DualBoot.supportsDualboot()){
                       mRebootItemList.add(new BasicIconListItem(mRebootSystem1Str, null, mRebootIcon, null));
                       rebootSubMenu[cnt] = SEQ_REBOOT_SYSTEM1;
                       cnt++;
                   }
                   if(System2Enabled&&DualBoot.supportsDualboot()){
                       mRebootItemList.add(new BasicIconListItem(mRebootSystem2Str, null, mRebootIcon, null));
                       rebootSubMenu[cnt] = SEQ_REBOOT_SYSTEM2;
                       cnt++;
                       }
                   if(cnt==1) normalRebootOnly = true;
                   mRebootConfirmStr = armRes.getString(R.string.reboot_confirm);
                   mRebootConfirmRecoveryStr = armRes.getString(R.string.reboot_confirm_recovery);
                   mRebootConfirmBootloaderStr = armRes.getString(R.string.reboot_confirm_bootloader);
                   mRebootConfirmFlashmodeStr = armRes.getString(R.string.reboot_confirm_flashmode);

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
                    if (mPowerOffActionHook != null) {
                    	log("Unhooking previous hook of poweroff action item");
                        mPowerOffActionHook.unhook();
                        mPowerOffActionHook = null;
                    }
                    if (mAirplaneActionHook != null) {
                    	log("Unhooking previous hook of airplane action item");
                    	mAirplaneActionHook.unhook();
                    	mAirplaneActionHook = null;
                    }
                }

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mContext == null) return;

                    pref.reload();
                    boolean advRebootEnabled = pref.getBoolean("pref_enable_reboot", false);
                    boolean screenshotEnabled = pref.getBoolean("pref_enable_screenshot", false);
                    @SuppressWarnings("unchecked")
                    List<Object> mItems = (List<Object>) XposedHelpers.getObjectField(param.thisObject, "mItems");

                    // I. Search for relevant existing items
                    // try to find out if reboot/screenshot/poweroff action item 
                    // already exists in the list of GlobalActions items
                    // strategy:
                    // 1) check if Action has mIconResId field or mMessageResId field
                    // 2) check if the name of the corresponding resource contains "reboot" or "restart" substring
                    log("Searching for existing reboot, screenshot and poweroff action item...");
                    final boolean removeVolumeATH = pref.getBoolean("pref_ath_volume_toggle", false);
                    final boolean removeVolumeATHWorkaround = pref.getBoolean("pref_ath_volume_toggle_workaround", false);
                    Object rebootActionItem = null;
                    Object screenshotActionItem = null;
                    Object powerOffActionItem = null;
                    Object airplaneActionItem = null;
                    Object volumeTristateActionItem = null;
                    Class<?> tristateClass = null;
                    Resources res = mContext.getResources();
                    if (removeVolumeATH){
                    	log("removeATH enabled");
                    	try{
                    		tristateClass = XposedHelpers.findClass(CLASS_SILENT_TRISTATE_ACTION, classLoader);
                    	}catch (ClassNotFoundError cnfe){
                    		log("error: tristateClass cannot be found!");
                    		if (removeVolumeATHWorkaround){
                    			log("removeATH workaround enabled. use the last object as the volume tristate object");
                    			volumeTristateActionItem = mItems.get(mItems.size()-1);
                    		}		
                    	}
                    }else{
                    	log("removeATH disabled");
                    }
                    
                    for (Object o : mItems) {
                    	if ((tristateClass != null)&&tristateClass.isInstance(o)){
                    		log("successfully found the volume tristate object");
                    		volumeTristateActionItem = o;
                    	}
                    	// search for drawable
                        try {
                            Field f = XposedHelpers.findField(o.getClass(), "mIconResId");
                            String resName = res.getResourceEntryName((Integer) f.get(o)).toLowerCase(Locale.US);
                            log("Drawable resName = " + resName);
                            if (resName.contains("reboot") || resName.contains("restart")) {
                                rebootActionItem = o;
                            }
                            if (resName.contains("screenshot")) {
                                screenshotActionItem = o;
                            }
                            if (resName.contains("power") || resName.contains("shutdown") 
                            		|| resName.contains("shut")||resName.contains("off")) {
                                powerOffActionItem = o;
                                mPowerOffIcon = res.getDrawable((Integer) f.get(o));
                            }
                            if (resName.contains("airplane")){
                            	airplaneActionItem = o;
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
                            }
                            if (resName.contains("screenshot")) {
                                screenshotActionItem = o;
                            }
                            if (resName.contains("power") || resName.contains("shutdown") 
                            		|| resName.contains("shut")||resName.contains("off")) {
                                powerOffActionItem = o;
                            }
                            if (resName.contains("airplane")){
                            	airplaneActionItem = o;
                            }
                            log(o.toString());
                        } catch (NoSuchFieldError nfe) {
                        	// continue
                        } catch (Resources.NotFoundException resnfe) { 
                        	// continue
                        } catch (IllegalArgumentException iae) {
                        	// continue
                        }
                         
                    }
                    
                    // II. Determine the suitable positions to insert new items
                    if(powerOffActionItem != null) afterPowerPos =  mItems.indexOf(powerOffActionItem) + 1;
                    if(rebootActionItem != null) 
                    	afterRebootPos = mItems.indexOf(rebootActionItem) + 1;
                    else
                    	afterRebootPos = afterPowerPos;
                    
                    // III. Remove action items and update positions accordingly
                    final boolean antiTheftHelperOn = pref.getBoolean("pref_no_locked_off", false);
                    final boolean hideATHDesc = pref.getBoolean("pref_ath_hide_desc", false);
                    final boolean removeReboot = pref.getBoolean("pref_remove_reboot", false);
                    final boolean removeScreenshot = pref.getBoolean("pref_remove_screenshot", false);
                    final boolean removeAirplane = pref.getBoolean("pref_remove_airplane", false);
                    
                    KeyguardManager myKM = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
                    if( myKM.inKeyguardRestrictedInputMode()&&antiTheftHelperOn) {
                    	if(powerOffActionItem != null){
                    		mItems.remove(powerOffActionItem);
                    		afterPowerPos--;
                    		afterRebootPos--;
                    		
                    	}
                    	if(rebootActionItem != null){
                    		mItems.remove(rebootActionItem);
                    		afterRebootPos--;
                    	}
                    	if(airplaneActionItem != null){
                    		mItems.remove(airplaneActionItem);
                    	}
                    	if(removeVolumeATH){
                    		mItems.remove(volumeTristateActionItem);
                    	}
                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mAdapter.notifyDataSetChanged();
                    }
                    if(removeReboot && !advRebootEnabled){
                    	if(mItems.remove(rebootActionItem)){
                    		afterRebootPos--;
                    	}
                    }
                    if(removeScreenshot && !screenshotEnabled){
                    	mItems.remove(screenshotActionItem);
                    }
                    if(removeAirplane){
                    	mItems.remove(airplaneActionItem);
                    }
                    
                    
                    
                    // IV. Add/replace action items and update positions accordingly

                    if( myKM.inKeyguardRestrictedInputMode()&&antiTheftHelperOn&&(!hideATHDesc)) {
                    	Boolean fakePowerOffEnabled = pref.getBoolean("pref_ath_fake_poweroff", false);
                    	Object action;
                    	if(fakePowerOffEnabled){
                    		action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                            	new FakePowerOffAction(mContext, mPowerOffIcon));
                    	}else{
                    		action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                                    new AntiTheftHelperAction(mContext, mDeviceLockedLabel, mDeviceLockedIcon, noLockedOffDialogTitle, noLockedOffDialogMsg));
                    	}
                        mItems.add(0, action);
                        afterPowerPos++;
                        afterRebootPos++;
                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mAdapter.notifyDataSetChanged();
                    }
                    
                    if(advRebootEnabled){
	                    if (rebootActionItem != null) {
	                        log("Existing Reboot action item found! Replacing onPress()");
	                        mRebootActionHook = XposedHelpers.findAndHookMethod(rebootActionItem.getClass(), 
	                                "onPress", new XC_MethodReplacement () {
	                            @Override
	                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
	                                if(normalRebootOnly){
	                                	KeyguardManager myKM = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
	                                    if( myKM.inKeyguardRestrictedInputMode()&&antiTheftHelperOn) {
	                                    	log("Is at lockscreen and shutdown protection is on");
	                                    	showLockedDialog();
	                                    }else{
		                                	handleReboot(mContext, mRebootStr, SEQ_REBOOT_NORMAL);
	                                    }
	                                }else{
	                                	showDialog();
	                                }
	                                return null;
	                            }
	                        });
	                    } else if(!(myKM.inKeyguardRestrictedInputMode()&&antiTheftHelperOn)){
	                        log("Existing Reboot action item NOT found! Adding new RebootAction item");

	                        Object action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass }, 
	                                new RebootAction());
	                        mItems.add(afterPowerPos, action);
	                        afterRebootPos++;
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
	                    	mItems.add(afterRebootPos, action);
	                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
	                        mAdapter.notifyDataSetChanged(); 
	                    }
                    }
                    String quickDial = pref.getString("pref_quick_dial_number", "");
                    String customQuickDialLbl = pref.getString("pref_quick_dial_label", "");
                    String QuickDialLbl = (customQuickDialLbl.length()>0)?customQuickDialLbl:mQuickDialLabel;
                    if (quickDial.length() > 0) {
                        Object action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                                new QuickDialAction(mContext, QuickDialLbl, quickDial, mQuickDialIcon));
                        mItems.add(afterRebootPos, action);
                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mAdapter.notifyDataSetChanged();
                    }
                    boolean expandStatusBarEnabled = pref.getBoolean("pref_expand_statusbar", false);
                    if (expandStatusBarEnabled){
                    	Object action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                                new ExpandStatusBarAction(mContext, mExpandStatusBarLabel, mExpandStatusBarIcon));
                        mItems.add(afterRebootPos, action);
                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mAdapter.notifyDataSetChanged();
                    }
                    boolean dataToggleEnabled = pref.getBoolean("pref_data_toggle", false);
                    if (dataToggleEnabled && !(myKM.inKeyguardRestrictedInputMode()&&antiTheftHelperOn)){
                    	Object action = Proxy.newProxyInstance(classLoader, new Class<?>[] { actionClass },
                                new ToggleDataAction(mContext, mToggleDataOnLabel, mToggleDataOffLabel, mToggleDataIcon));
                        mItems.add(afterRebootPos, action);
                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mAdapter.notifyDataSetChanged();
                    }

                	if (powerOffActionItem != null) {
                    	log("Existing Power off action item found!");
                        if( myKM.inKeyguardRestrictedInputMode()&&antiTheftHelperOn) {
                        	log("Is at lockscreen & shutdown protection is on. Replacing onPress");
                        	
                        	mPowerOffActionHook = XposedHelpers.findAndHookMethod(powerOffActionItem.getClass(), 
	                                "onPress", new XC_MethodReplacement () {
	                            @Override
	                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
	                            	showLockedDialog();
	                            	return null;
	                            }
	                        });
                        }
                    } else {
                    	log("Existing Power off action item NOT found!");
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
    
    private static void showLockedDialog(){
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
    }

    private static void handleReboot(Context context, String caption, final int mode) {
        try {
            String message;
            if (mode == SEQ_REBOOT_RECOVERY){
            	message = mRebootConfirmRecoveryStr;
            }else if (mode == SEQ_REBOOT_BOOTLOADER){
            	message = mRebootConfirmBootloaderStr;
            }else if (mode == SEQ_REBOOT_FLASHMODE){
            	message = mRebootConfirmFlashmodeStr;
            }else{
            	message = mRebootConfirmStr;
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
        } else if (mode == SEQ_REBOOT_FLASHMODE){
        	final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
			pm.reboot("oem-53");
	    } else if (mode == SEQ_REBOOT_SYSTEM1){
        	final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        	DualBoot.setDualSystemBootmode("boot-system0");
        	pm.reboot(null);
        } else if (mode == SEQ_REBOOT_SYSTEM2){
        	final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        	DualBoot.setDualSystemBootmode("boot-system1");
        	pm.reboot(null);
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
                if( myKM.inKeyguardRestrictedInputMode()&&antiTheftHelperOn) {
                	showLockedDialog();
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
                if( myKM.inKeyguardRestrictedInputMode()&&antiTheftHelperOn) {
                	showLockedDialog();
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
