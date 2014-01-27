package hk.kennethso168.xposed.advancedrebootmenu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.IXposedHookZygoteInit;

public class Main implements IXposedHookLoadPackage, IXposedHookZygoteInit{
	
	public static final String PACKAGE_NAME = Main.class.getPackage().getName();
    public static String MODULE_PATH = null;
    
    private static XSharedPreferences pref;
    private static int syspart = -1;
    
    //should set to false for release. Exceptions should be always logged
    public static final boolean WRITE_LOGS = false;
    
    public static final boolean writeLogs(){
    	pref.reload();
    	return pref.getBoolean("pref_verbose_log", true);
    }
    
    private static final String LOG_APP_NAME = "APM";
    
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
    	pref = new XSharedPreferences(PACKAGE_NAME);
    	
	    if(supportsDualboot()) {
	        File misc = new File("/dev/block/platform/msm_sdcc.1/by-name/misc");
	        misc.setReadable(true, false);
	        misc.setWritable(true, false);
	        File cmdline = new File("/proc/cmdline");
	        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cmdline)));
			
	        String line = null;
	        while ((line = reader.readLine()) != null) {
	            String[] keyvals = line.split(" ");
	            for(String keyval : keyvals) {
	                String[] kv = keyval.split("=");
	                if(kv.length!=2)
	                    continue;
	                if(kv[0].trim().equals("syspart")) {
	                    String syspartVal = kv[1].trim();
	                    if(syspartVal.equals("system"))
	                        syspart = 0;
	                    if(syspartVal.equals("system1"))
	                        syspart =1;
	                }
	            }
	        }
	        reader.close();
	    }
    }
    
    public static boolean supportsDualboot() {
        return android.os.Build.DEVICE.equals("aries")
        	|| android.os.Build.DEVICE.equals("taurus")
            || android.os.Build.DEVICE.equals("cancro");
    }
    
    public static void setDualSystemBootmode(String bootmode) {
        try {
            RandomAccessFile file = new RandomAccessFile("/dev/block/platform/msm_sdcc.1/by-name/misc", "rw");
            file.seek(0x1000);
            file.write((bootmode).getBytes());
            file.write(new byte[]{0});
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static int getSyspart() {
        return syspart;
    }
    
    private static void log(String message) {
    	if(WRITE_LOGS) XposedBridge.log(LOG_APP_NAME + ": " + message);
    }
    
    @Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(ModRebootMenu.PACKAGE_NAME)){
        	ModRebootMenu.init(pref, lpparam.classLoader);
        }
    }
}
