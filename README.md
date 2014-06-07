NOTE
====
I restricted editing to collaborators only. GitHub is really confusing and I accidentally reverted the hard work today! So this will prevent future occurrances.
If you are a translator, please directly send me the strings. If you want to contribute, negotiate with me. Thanks.

AdvancedPowerMenu
==================

Advanced Power Menu Xposed Mod

Features
========
- A dedicate first-level option for rebooting
- Fully working reboot to recovery (no reboot wrapper required) and soft reboot
- Screenshot function
- High quality xxhdpi icons
Thanks to Xposed framework,
- Easy Installation. No flashing required. (except reboot wrapper)
- Works both for odexed and deodexed ROMs
- Quite future proof (should work for Android 4.3 if there is no big framework change from Sony)
- Should work for a number of similar devices (not tested)

Prerequisites
=============
All Xperia Phones with rooted stock 4.0+ ROM / Nexus devices / AOSP(Please do test and tell me)
Xposed framework installed

Installation
============
- You will need xposed framework and for this mod to work! See prerequisites above.
After you meet the prerequisites,
- Download and install the apk in the next post
- Go to Xposed Installer and enable "Advanced Power Menu"
- Reboot and you are done! (Sometimes you might need to reboot more than once for all functions to work)
- If you use the reboot functions for the first time you may need to grant superuser permission.

Disclaimer
==========
I'm not responsible for any damage caused by this mod! Use this at your own risk.

FAQ
===
Q: The mod doesn't work!
A: Other xposed mods might interfere with this mod. Disable them, reboot and try again.

Q: Rebooting into recovery doesn't work!
A: Flashing the DoomLorD's reboot wrapper MAY help. However, as the wrapper is intended to be optional, there might be something wrong. So it'd be nice if you could send me some logs 

Q: There's something wrong with the mod...
A: Just disable the mod in Xposed Installer and reboot. Then everything should be fine again (Should this happens please tell me and send me the debug.log and debug.log.old files. This helps me to fix the issue)]

Q: I have a bootloop! What should I do now?
A: You can follow the following methods (quoted from xposed framework main thread) to disable the xposed framework
Quote:
In case you get into a boot loop:
You can flash the attached Xposed-Disabler-CWM.zip by Tungstwenty. It will be copied to your (external) SD card when you install Xposed as well. The only thing it does is copying /system/bin/app_process.orig back to /system/bin/app_process, which you can also do yourself (e.g. with adb shell in recovery mode).
You could also create a file /data/xposed/disabled, which causes Xposed to be bypassed as well.

Release downloads & support thread
==================================
http://forum.xda-developers.com/showthread.php?t=2404042
