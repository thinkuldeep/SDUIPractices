package com.thinkuldeep.sdui.client.viewmodel

import com.thinkuldeep.sdui.client.data.UiDataSource
import com.thinkuldeep.sdui.client.data.UiRepository
import com.thinkuldeep.sdui.client.model.UiComponent
import com.thinkuldeep.sdui.client.threading.threadSafeExecute
import com.thinkuldeep.sdui.client.tracing.Environment
import com.thinkuldeep.sdui.client.tracing.SamplingConfig
import com.thinkuldeep.sdui.client.tracing.TraceContext
import com.thinkuldeep.sdui.client.tracing.TraceContextHolder
import com.thinkuldeep.sdui.client.tracing.TraceSamplerHolder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LandingViewModel(
    private val repository: UiDataSource = UiRepository(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(dispatcher)
    private val _uiState = MutableStateFlow<UiComponent?>(null)
    val uiState: StateFlow<UiComponent?> = _uiState

    private val _traceContext = MutableStateFlow<TraceContext?>(null)
    val traceContext: StateFlow<TraceContext?> = _traceContext

    private val featureIndexes = mutableMapOf<String, Int>()
    private val featureIndexLock = Any()
    private var originalTree: UiComponent? = null

    init {
        println("🔥 ViewModel INIT")
    }

    private fun load() {
        scope.launch {
            println("🔥 Calling API...")
            try {
                val root = repository.fetchLanding()
                originalTree = root
                _uiState.value = applyFeatureFilter(root)
            } catch (e: Exception) {
                println("❌ ViewModel error: ${e.message}")
            }
        }
    }

    fun reload() {
        load()
    }

    fun dispatch(action: String, componentId: String?) {
        when (action) {
            "load_next_feature" -> {
                componentId?.let { id ->
                    threadSafeExecute(featureIndexLock) {
                        val current = featureIndexes[id] ?: 0
                        featureIndexes[id] = current + 1
                    }

                    originalTree?.let {
                        _uiState.value = applyFeatureFilter(it)
                    }
                }
            }
        }
    }

    fun setTraceContext(traceparent: String, tracestate: String = "") {
        val context = TraceContext(
            traceId = extractTraceId(traceparent),
            spanId = extractSpanId(traceparent),
            traceState = tracestate
        )
        TraceContextHolder.set(context)
        _traceContext.value = context
        println("🔍 [TRACE] Context set - TraceID: ${context.traceId}")
    }

    fun setTraceContext(context: TraceContext) {
        TraceContextHolder.set(context)
        _traceContext.value = context
        println("🔍 [TRACE] Context set - TraceID: ${context.traceId}")
    }

    fun getCurrentTraceContext(): TraceContext? = _traceContext.value

    private fun extractTraceId(traceparent: String): String {
        val parts = traceparent.split("-")
        return if (parts.size >= 2) parts[1] else ""
    }

    private fun extractSpanId(traceparent: String): String {
        val parts = traceparent.split("-")
        return if (parts.size >= 3) parts[2] else ""
    }

    fun configureSampling(config: SamplingConfig) {
        TraceSamplerHolder.setConfig(config)
        println("🔍 [TRACE] Sampling configured - Environment: ${config.environment}, IsQaUser: ${config.isQaUser}, SampleRate: ${getSampleRateForEnvironment(config.environment)}")
        // Initialize trace and load after sampling is configured
        if (_traceContext.value == null) {
            val context = TraceContext.create()
            TraceContextHolder.set(context)
            _traceContext.value = context
            load()
        }
    }

    fun configureSampling(environment: Environment, isQaUser: Boolean = false) {
        val config = SamplingConfig(environment = environment, isQaUser = isQaUser)
        configureSampling(config)
    }

    private fun getSampleRateForEnvironment(environment: Environment): String = when (environment) {
        Environment.PRODUCTION -> "1%"
        Environment.STAGING -> "20%"
        Environment.QA -> "100%"
        Environment.DEVELOPMENT -> "50%"
    }

    private fun applyFeatureFilter(component: UiComponent): UiComponent {
        return when (component) {

            is UiComponent.Column -> {
                component.copy(
                    children = component.children.map {
                        applyFeatureFilter(it)
                    }
                )
            }

            is UiComponent.FeaturedItems -> {
                val items = component.children
                if (items.isEmpty()) return component

                val index = featureIndexes[component.button.id] ?: 0
                val safeIndex = index % items.size

                component.copy(
                    children = listOf(items[safeIndex])
                )
            }

            else -> component
        }
    }
}