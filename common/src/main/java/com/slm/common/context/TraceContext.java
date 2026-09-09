package com.slm.common.context;

public class TraceContext {

    private static final ThreadLocal<String> THREAD_TRACE = new ThreadLocal<>();

    public static void setTrace(String traceId) {
        THREAD_TRACE.set(traceId);
    }

    public static String getTrace() {
        return THREAD_TRACE.get();
    }

    public static void clear() {
        THREAD_TRACE.remove();
    }

}