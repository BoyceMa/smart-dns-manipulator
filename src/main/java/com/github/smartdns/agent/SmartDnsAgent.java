package com.github.smartdns.agent;

import com.github.smartdns.SmartDnsManager;

import java.lang.instrument.Instrumentation;

public class SmartDnsAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        // JVM启动时自动执行
        SmartDnsManager.loadDnsCacheConfig();
    }
    
    public static void agentmain(String agentArgs, Instrumentation inst) {
        // 运行时动态附加
        SmartDnsManager.loadDnsCacheConfig();
    }
}