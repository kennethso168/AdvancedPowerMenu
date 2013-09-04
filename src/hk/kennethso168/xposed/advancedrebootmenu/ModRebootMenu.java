package hk.kennethso168.xposed.advancedrebootmenu;

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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
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
    private static String mRebootStr;
    private static String mRebootSoftStr;
    private static String mRecoveryStr;
    private static String mScreenshotStr;
    private static Drawable mRebootIcon;
    private static Drawable mRebootSoftIcon;
    private static Drawable mRecoveryIcon;
    private static Drawable mScreenshotIcon;
    private static List<IIconListAdapterItem> mRebootItemList;
    private static String mRebootConfirmStr;
    private static String mRebootConfirmRecoveryStr;
    private static Unhook mRebootActionHook;
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
    
    //constants for reboot sub-menu sequence
    private static final int SEQ_REBOOT_NORMAL = 0;
    private static final int SEQ_REBOOT_SOFT = 1;
    private static final int SEQ_REBOOT_RECOVERY = 2;
    
    //constants for modes of showing confirmation dialogs
    private static final int VALUE_ENABLE_DIALOGS = 0;
    private static final int VALUE_DISABLE_DIALOGS = 1;
    
    //variables for screenshot function
    static final Object mScreenshotLock = new Object();
	static ServiceConnection mScreenshotConnection = null;  

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

                   Context armContext = mContext.createPackageContext(
                           Main.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                   
                   Resources res = mContext.getResources();
                   Resources armRes = armContext.getResources();

                   int rebootStrId = res.getIdentifier("factorytest_reboot", "string", PACKAGE_NAME);
                   mRebootStr  = (rebootStrId == 0) ? "Reboot" : res.getString(rebootStrId);
                   mRebootSoftStr = armRes.getString(R.string.reboot_soft);
                   mRecoveryStr = armRes.getString(R.string.reboot_recovery);
                   
                   mScreenshotStr = armRes.getString(R.string.take_screenshot);

                   mRebootIcon = armRes.getDrawable(R.drawable.ic_lock_reboot);
                   mRebootSoftIcon = armRes.getDrawable(R.drawable.ic_lock_reboot_soft);
                   mRecoveryIcon = armRes.getDrawable(R.drawable.ic_lock_recovery);
                   
                   
                   mScreenshotIcon = armRes.getDrawable(R.drawable.ic_screenshot);

                   mRebootItemList = new ArrayList<IIconListAdapterItem>();
                   mRebootItemList.add(new BasicIconListItem(mRebootStr, null, mRebootIcon, null));
                   mRebootItemList.add(new BasicIconListItem(mRebootSoftStr, null, mRebootSoftIcon, null));
                   mRebootItemList.add(new BasicIconListItem(mRecoveryStr, null, mRecoveryIcon, null));

                   mRebootConfirmStr = armRes.getString(R.string.reboot_confirm);
                   mRebootConfirmRecoveryStr = armRes.getString(R.string.reboot_confirm_recovery);

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
                    }
                    if(advRebootEnabled){
	                    if (rebootActionItem != null) {
	                        log("Existing Reboot action item found! Replacing onPress()");
	                        mRebootActionHook = XposedHelpers.findAndHookMethod(rebootActionItem.getClass(), 
	                                "onPress", new XC_MethodReplacement () {
	                            @Override
	                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
	                                showDialog();
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
	                                new ScreenshotAction());
	                    	// add to the second/third position
	                    	mItems.add(advRebootEnabled?2:1, action);
	                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
	                        mAdapter.notifyDataSetChanged(); 
	                    }
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
                        handleReboot(mContext, mRebootStr, which);
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
            final String message = (mode == 0 || mode == 1) ? mRebootConfirmStr : mRebootConfirmRecoveryStr;
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
        	
        }
    }
    
    private static void handleScreenshot(Context context) {
    	final Handler handler = new Handler();
    	//take a screenshot after a 0.5s delay
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            	takeScreenshot(handler);
            }
        }, 500);
    	
    	
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
                showDialog();
                return null;
            } else if (methodName.equals("onLongPress")) {
                handleReboot(mContext, mRebootStr, 0);
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
    
    private static class ScreenshotAction implements InvocationHandler {
    	private Context mContext;

        public ScreenshotAction() {
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
                icon.setImageDrawable(mScreenshotIcon);

                TextView messageView = (TextView) v.findViewById(res.getIdentifier(
                        "message", "id", "android"));
                messageView.setText(mScreenshotStr);

                TextView statusView = (TextView) v.findViewById(res.getIdentifier(
                        "status", "id", "android"));
                statusView.setVisibility(View.GONE);

                return v;
            } else if (methodName.equals("onPress")) {
                handleScreenshot(mContext);
                return null;
            } else if (methodName.equals("onLongPress")) {
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
    
    static final Runnable mScreenshotTimeout = new Runnable() {
        @Override public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };
    
	

}
