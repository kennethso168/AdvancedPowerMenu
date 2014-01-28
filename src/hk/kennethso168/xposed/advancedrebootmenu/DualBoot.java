package hk.kennethso168.xposed.advancedrebootmenu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

public class DualBoot {
	private static int syspart = -1;

	public static boolean supportsDualboot() {
		return android.os.Build.DEVICE.equals("aries")
				|| android.os.Build.DEVICE.equals("taurus")
				|| android.os.Build.DEVICE.equals("cancro");
	}

	public static void init() {
		if (supportsDualboot()) {
			File misc = new File("/dev/block/platform/msm_sdcc.1/by-name/misc");
			misc.setReadable(true, false);
			misc.setWritable(true, false);
			File cmdline = new File("/proc/cmdline");
			BufferedReader reader;
			try {
				reader = new BufferedReader(new InputStreamReader(
						new FileInputStream(cmdline)));
				String line = null;
				while ((line = reader.readLine()) != null) {
					String[] keyvals = line.split(" ");
					for (String keyval : keyvals) {
						String[] kv = keyval.split("=");
						if (kv.length != 2)
							continue;
						if (kv[0].trim().equals("syspart")) {
							String syspartVal = kv[1].trim();
							if (syspartVal.equals("system"))
								syspart = 0;
							if (syspartVal.equals("system1"))
								syspart = 1;
						}
					}
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void setDualSystemBootmode(String bootmode) {
		try {
			RandomAccessFile file = new RandomAccessFile(
					"/dev/block/platform/msm_sdcc.1/by-name/misc", "rw");
			file.seek(0x1000);
			file.write((bootmode).getBytes());
			file.write(new byte[] { 0 });
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static int getSyspart() {
		return syspart;
	}
}
