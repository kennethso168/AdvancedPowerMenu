package hk.kennethso168.xposed.advancedrebootmenu;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookLoadPackage{
	
	public static final String PACKAGE_NAME = Main.class.getPackage().getName();
    public static String MODULE_PATH = null;
    
    //should set to false for release. Exceptions should be always logged
    public static final boolean WRITE_LOGS = false; 
    
    private static final String APP_NAME = "Advanced Reboot Menu";
    
    private static void log(String message) {
    	if(WRITE_LOGS) XposedBridge.log(APP_NAME + ": " + message);
    }
    
    @Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(ModRebootMenu.PACKAGE_NAME)){
        	ModRebootMenu.init(lpparam.classLoader);
        }
    }
}
