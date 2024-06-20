#pragma once
#include <string>
#include <cstring>
#include <vector>
#include <algorithm>
#include <unistd.h>
#include <dobby.h>
#include <sys/stat.h>
#include <unordered_map>

std::vector<std::string> fstatat64_ignore = {
        "/xposed.prop", "/system/bin/app_process.orig", "/system/lib/libxposed_art.so",
        "/system/lib64/libxposed_art.so", "/system/bin/app_process32_xposed", "/system/bin/app_process64_xposed",
        "/data/data/com.keramidas.TitaniumBackup", "/data/user/0/com.keramidas.TitaniumBackup",
        "/data/user_de/0/com.keramidas.TitaniumBackup", "/data/data/com.ljmu.andre.snaptools",
        "/data/user/0/com.ljmu.andre.snaptools", "/data/user_de/0/com.ljmu.andre.snaptools",
        "/data/data/local.interceptmod", "/data/user/0/local.interceptmod", "/data/user_de/0/local.interceptmod",
        "/data/data/local.snapintercept", "/data/user/0/local.snapintercept", "/data/user_de/0/local.snapintercept",
        "/data/data/com.marz.snapprefs", "/data/user/0/com.marz.snapprefs", "/data/user_de/0/com.marz.snapprefs",
        "/data/data/com.topjohnwu.magisk", "/data/user/0/com.topjohnwu.magisk", "/data/user_de/0/com.topjohnwu.magisk",
        "/sbin/magisk", "/sbin/magiskhide", "/init.magisk.rc", "/dev/magisk/bin/busybox",
        "/system/addon.d/99-magisk.sh", "/data/user/0/magisk.db", "/data/user_de/0/magisk.db",
        "/system/addon.d/99-magisk.sh", "/root/magisk", "/magisk", "/sbin_orig", "/data/magisk", "/sbin/.core",
        "/dev/socket/bstfolderd", "/boot/bstmods", "/mnt/prebundledapps", "/fstab.nox", "/system/bin/noxd",
        "/ueventd.nox.rc", "/system/bin/noxscreen", "/etc/init.nox.sh", "/dev/socket/genyd", "/dev/socket/baseband_genyd",
        "/data/data/com.genymotion.superuser", "/data/user/0/com.genymotion.superuser",
        "/data/user_de/0/com.genymotion.superuser", "/system/genymotion", "/data/youwave_id",
        "/system/bin/androVM-prop", "/dev/vboxuser", "/sys/module/vboxguest", "/sys/bus/pci/drivers/vboxguest",
        "/system/app/Superuser.apk", "/system/app/Superuser/Superuser.apk", "/init.supersu.rc", "/system/.supersu",
        "/data/.supersu", "/dev/block/supersu/su", "/sbin/daemonsu", "/dev/__properties__/u:object_r:supersu_prop:s0",
        "/dev/__properties__/u:object_r:supersu_prop:s0", "/data/data/eu.chainfire.supersu",
        "/data/user/0/eu.chainfire.supersu", "/data/user_de/0/eu.chainfire.supersu", "/data/data/eu.chainfire.suhide",
        "/data/user/0/eu.chainfire.suhide", "/data/user_de/0/eu.chainfire.suhide", "/sbin/su", "/system/bin/su",
        "/system/xbin/su", "/system/xbin/nu", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
        "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su", "/sbin/su", "/system/sbin/su", "/system/bin/su",
        "/system/xbin/su", "/vendor/bin/su", "/vendor/xbin/su", "/sbin/magisk", "/system/sbin/magisk", "/system/bin/magisk",
        "/system/xbin/magisk", "/vendor/bin/magisk", "/vendor/xbin/magisk", "/su", "/nu",
        "/data/data/de.robv.android.xposed.installer", "/data/user/0/de.robv.android.xposed.installer",
        "/data/user_de/0/de.robv.android.xposed.installer", "/data/data/com.zachspong.temprootremovejb",
        "/system/bin/app_process_init", "/data/data/com.chelpus.lackypatch", "/data/app-lib/net.snclab.RootKeepSurvival-1",
        "/system/lib/libsubstrate-dvm.so", "/dev/.su.d", "/system/xbin/supolicy", "/data/data/com.formyhm.hiderootPremium",
        "/sbin/magiskpolicy", "/system/xbin/sugote-mksh", "/data/app-lib/org.projectvoodoo.otarootkeeper-1",
        "/system/lib64/libsupol.so", "/system/etc/init.d/99SuperSUDaemon", "/data/data/com.amphoras.hidemyroot",
        "/data/data/com.saurik.substrate/lib/libAndroidLoader.so", "/system/xbin/sugote", "/data/data/com.ramdroid.appquarantinepro",
        "/data/data/com.noshufou.android.su.elite", "/data/app-lib/sa.root.toolkit-1", "/data/app-lib/com.troy1103.hideyourroot-1",
        "/data/app-lib/com.formyhm.hideroot-1", "/data/data/com.devadvance.rootcloakplus", "/data/data/jp.co.cyberagent.stf",
        "/data/user/0/jp.co.cyberagent.stf", "/data/user_de/0/jp.co.cyberagent.stf"
};

std::vector<std::string> openIgnore = {
        "/data/user_de/0/de.robv.android.xposed.installer/conf/modules.list",
        "/data/user/0/de.robv.android.xposed.installer/conf/modules.list",
        "/data/data/de.robv.android.xposed.installer/conf/modules.list"
};

namespace FstatHook {
    // Custom fstat hook function
    HOOK_DEF(int, fstat_hook, int fd, struct stat *buf) {
        char name[256];
        memset(name, 0, sizeof(name));
        snprintf(name, sizeof(name), "/proc/self/fd/%d", fd);
        readlink(name, name, sizeof(name));

        std::string fileName(name);

        if (common::native_config->disable_metrics && fileName.find("files/blizzardv2/queues") != std::string::npos) {
            unlink(name);
            return -1;
        }

        if (common::native_config->disable_bitmoji && fileName.find("com.snap.file_manager_4_SCContent") != std::string::npos) {
            return -1;
        }
        return fstat_hook_original(fd, buf);
    }

    HOOK_DEF(int, fstatat64_hook, int dirfd, const char *pathname, struct stat *buf, int flags) {
        std::string fileName(pathname);
        if (std::find(fstatat64_ignore.begin(), fstatat64_ignore.end(), fileName) != fstatat64_ignore.end()) {
            return -1;
        }
        return fstatat64_hook_original(dirfd, pathname, buf, flags);
    }
    
    HOOK_DEF(int, open_hook, const char *pathname, int flags) {
        std::string fileName(pathname);

        if (std::find(openIgnore.begin(), openIgnore.end(), fileName) != openIgnore.end()) {
            return -1;
        }

        return open_hook_original(pathname, flags);
    }


    void init() {
        SafeHook((void *)DobbySymbolResolver("libc.so", "fstat"), (void *)fstat_hook, (void **)&fstat_hook_original);
        SafeHook((void *)DobbySymbolResolver("libc.so", "fstatat64"), (void *)fstatat64_hook, (void **)&fstatat64_hook_original);
        SafeHook((void *)DobbySymbolResolver("libc.so", "open"), (void *)open_hook, (void **)&open_hook_original);
    }
}
