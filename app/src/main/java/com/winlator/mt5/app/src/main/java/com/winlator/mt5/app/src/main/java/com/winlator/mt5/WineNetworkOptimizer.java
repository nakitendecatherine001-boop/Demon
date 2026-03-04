package com.winlator.mt5;

import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class WineNetworkOptimizer {
    private static final String TAG = "MT5-Network";

    public static boolean apply(String containerRoot) {
        boolean ok = writeTcpRegistry(containerRoot);
        ok &= writeWinsockRegistry(containerRoot);
        ok &= writeUserenvRegistry(containerRoot);
        Log.i(TAG, "Network optimization applied: " + ok);
        return ok;
    }

    private static boolean writeTcpRegistry(String root) {
        String content =
            "Windows Registry Editor Version 5.00\n\n" +
            "[HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters]\n" +
            "\"TcpAckFrequency\"=dword:00000001\n" +
            "\"TcpNoDelay\"=dword:00000001\n" +
            "\"TcpTimedWaitDelay\"=dword:0000001e\n" +
            "\"DefaultTTL\"=dword:00000040\n" +
            "\"MaxUserPort\"=dword:0000fffe\n" +
            "\"SackOpts\"=dword:00000001\n" +
            "\"Tcp1323Opts\"=dword:00000001\n";
        return writeReg(root, "system_net.reg", content);
    }

    private static boolean writeWinsockRegistry(String root) {
        String content =
            "Windows Registry Editor Version 5.00\n\n" +
            "[HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Services\\WinSock2\\Parameters]\n" +
            "\"MaxSockAddrLength\"=dword:00000010\n" +
            "\"MinSockAddrLength\"=dword:00000010\n";
        return writeReg(root, "winsock_net.reg", content);
    }

    private static boolean writeUserenvRegistry(String root) {
        String content =
            "Windows Registry Editor Version 5.00\n\n" +
            "[HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings]\n" +
            "\"ProxyEnable\"=dword:00000000\n" +
            "\"MaxConnectionsPerServer\"=dword:00000010\n" +
            "\"MaxConnectionsPer1_0Server\"=dword:00000010\n";
        return writeReg(root, "userenv_net.reg", content);
    }

    private static boolean writeReg(String root, String filename, String content) {
        String dirPath = root + "/mt5_regs/";
        new File(dirPath).mkdirs();
        try (FileWriter fw = new FileWriter(new File(dirPath, filename))) {
            fw.write(content);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write " + filename + ": " + e.getMessage());
            return false;
        }
    }

    public static String getRegeditImportCommands(String containerRoot) {
        return "for reg in \"" + containerRoot + "/mt5_regs/\"*.reg; do\n" +
               "    wine regedit /S \"$reg\" 2>/dev/null\n" +
               "done\n";
    }

    public static Map<String, String> getWineLowLatencyEnv() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("WINEESYNC",                "1");
        env.put("WINEFSYNC",                "1");
        env.put("WINE_DISABLE_WRITE_WATCH", "1");
        env.put("WINEDEBUG",                "-all");
        return env;
    }
}
