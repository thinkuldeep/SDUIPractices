package com.thinkuldeep.sdui.server.filter

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper

@Component
class HttpLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(HttpLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()

        val wrappedResponse = ContentCachingResponseWrapper(response)

        val traceparent = request.getHeader("traceparent")
        val tracestate = request.getHeader("tracestate")
        val xTraceId = request.getHeader("x-trace-id")

        val mobileSampled = isParentSampled(traceparent);

        val span = Span.current()

        span.setAttribute(
            "mobile.sampled",
            mobileSampled
        )

        try {
            filterChain.doFilter(request, wrappedResponse)
        } catch (ex: Exception) {

            val duration = System.currentTimeMillis() - startTime

            span.recordException(ex)
            span.setStatus(StatusCode.ERROR)

            span.setAttribute("http.error",true)
            span.setAttribute("request.duration.ms", duration)

            logRequest(request, 500, duration, traceparent, tracestate, xTraceId)
            wrappedResponse.copyBodyToResponse()
            throw ex
        }

        val duration = System.currentTimeMillis() - startTime

        span.setAttribute("request.duration.ms",duration)

        if (wrappedResponse.status >= 500) {

            span.setStatus(StatusCode.ERROR)
            span.setAttribute("http.error", true)
        }

        if (duration > 3000) {
            span.setAttribute("slow.request", true)
        }

        logRequest(request, wrappedResponse.status, duration, traceparent, tracestate, xTraceId)
        wrappedResponse.copyBodyToResponse()
    }

    private fun logRequest(
        request: HttpServletRequest,
        status: Int,
        duration: Long,
        traceparent: String?,
        tracestate: String?,
        xTraceId: String?
    ) {
        val method = request.method
        val uri = request.requestURI
        val query = request.queryString

        val traceInfo = buildString {
            if (traceparent != null) append(" [traceparent=$traceparent]")
            if (tracestate != null) append(" [tracestate=$tracestate]")
            if (xTraceId != null) append(" [x-trace-id=$xTraceId]")
        }

        log.info(
            "HTTP {} {}{} → {} ({} ms){}",
            method,
            uri,
            if (query != null) "?$query" else "",
            status,
            duration,
            traceInfo
        )
    }

    private fun isParentSampled(traceparent: String?): Boolean {

        if (traceparent.isNullOrBlank()) {
            return false
        }

        return traceparent.endsWith("-01")
    }
}