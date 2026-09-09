package com.slm.common.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import com.slm.common.context.TraceContext;

public class TraceIdConverter extends CompositeConverter<ILoggingEvent> {

    @Override
    protected String transform(ILoggingEvent event, String in) {
        String traceId = TraceContext.getTrace();
        return traceId != null ? traceId : "";
    }

}
