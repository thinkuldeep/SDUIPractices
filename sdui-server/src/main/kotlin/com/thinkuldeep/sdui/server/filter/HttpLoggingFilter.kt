package com.thinkuldeep.sdui.server.filter

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

        try {
            filterChain.doFilter(request, wrappedResponse)
        } finally {
            val duration = System.currentTimeMillis() - startTime

            val method = request.method
            val uri = request.requestURI
            val query = request.queryString
            val status = wrappedResponse.status

            log.info(
                "HTTP {} {}{} → {} ({} ms)",
                method,
                uri,
                if (query != null) "?$query" else "",
                status,
                duration
            )

            wrappedResponse.copyBodyToResponse()
        }
    }
}