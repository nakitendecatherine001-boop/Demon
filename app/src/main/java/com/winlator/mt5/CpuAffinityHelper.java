package com.winlator.mt5;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CpuAffinityHelper {
    private static final String TAG = "MT5-CpuAffinity";

    public static long applyTradingAffinity(Context context, int pid) {
        List<Integer> bigCores = detectBigCores();
        if (bigCores.isEmpty()) { Log.w(TAG, "No big cores detected"); return -1; }
        long mask = buildAffinityMask(bigCores);
        Log.i(TAG, String.format("Big cores: %s  mask: 0x%X", bigCores, mask));
        boolean ok = setAffinityViaTaskset(pid, mask);
        return ok ? mask : -1;
    }

    public static long applyToCurrentProcess() {
        return applyTradingAffinity(null, android.os.Process.myPid());
    }

    public static List<Integer> detectBigCores() {
        int numCpus = Runtime.getRuntime().availableProcessors();
        long maxFreq = 0;
        long[] freqs = new long[numCpus];
        for (int i = 0; i < numCpus; i++) {
            freqs[i] = readMaxFreq(i);
            if (freqs[i] > maxFreq) maxFreq = freqs[i];
        }
        long threshold = (long)(maxFreq * 0.90);
        List<Integer> bigCores = new ArrayList<>();
        for (int i = 0; i < numCpus; i++)
            if (freqs[i] >= threshold) bigCores.add(i);
        if (bigCores.isEmpty())
            for (int i = numCpus/2; i < numCpus; i++) bigCores.add(i);
        return bigCores;
    }

    private static long readMaxFreq(int core) {
        try (BufferedReader br = new BufferedReader(new FileReader(
                "/sys/devices/system/cpu/cpu" + core + "/cpufreq/cpuinfo_max_freq"))) {
            String line = br.readLine();
            if (line != null) return Long.parseLong(line.trim());
        } catch (Exception ignored) {}
        return 0;
    }

    private static long buildAffinityMask(List<Integer> cores) {
        long mask = 0;
        for (int c : cores) mask |= (1L << c);
        return mask;
    }

    private static boolean setAffinityViaTaskset(int pid, long mask) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                "su", "-c", String.format("taskset -p 0x%X %d", mask, pid)});
            return p.waitFor() == 0;
        } catch (Exception e) {
            Log.d(TAG, "taskset: " + e.getMessage());
            return false;
        }
    }

    public static Map<String, String> getBox64PerfEnv() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("BOX64_DYNAREC",           "1");
        env.put("BOX64_DYNAREC_STRONGMEM", "1");
        env.put("BOX64_DYNAREC_BIGBLOCK",  "1");
        env.put("BOX64_DYNAREC_SAFEFLAGS", "1");
        env.put("BOX64_DYNAREC_FASTNAN",   "1");
        env.put("BOX64_DYNAREC_FASTROUND", "1");
        env.put("BOX64_DYNAREC_X87DOUBLE", "1");
        env.put("BOX64_LOG",               "0");
        env.put("BOX64_PAGESIZE",          "4096");
        env.put("WINEDEBUG",               "-all");
        env.put("WINE_LARGE_ADDRESS_AWARE","1");
        return env;
              }
