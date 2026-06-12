import Foundation

@objc class SwiftEventTracker: NSObject {
    private var activeSpans: [String: SpanState] = [:]

    struct SpanState {
        let traceId: String
        let spanId: String
        let operationName: String
        let startTime: Date
        let traceFlags: String
    }

    @objc func trackEventStarted(
        _ operationName: String,
        properties: [String: String]
    ) -> EventSpan {
        let traceId = properties["traceId"] ?? generateTraceId()
        let spanId = generateSpanId()
        let traceFlags = properties["traceFlags"] ?? "01"
        let environment = getCurrentEnvironment()

        // Create and store span state
        let spanState = SpanState(
            traceId: traceId,
            spanId: spanId,
            operationName: operationName,
            startTime: Date(),
            traceFlags: traceFlags
        )

        let spanKey = "\(traceId)-\(spanId)"
        activeSpans[spanKey] = spanState

        print("🔍 [EVENT] Started: \(operationName) (spanId: \(spanId))")

        // Delegate to analytics
        AnalyticsManager.shared.trackEventStarted(
            operationName: operationName,
            traceId: traceId,
            spanId: spanId,
            properties: properties
        )

        return EventSpan(
            traceId: traceId,
            spanId: spanId,
            eventType: "EVENT_STARTED",
            operationName: operationName,
            traceFlags: traceFlags,
            isSampled: traceFlags == "01",
            environment: environment,
            durationMs: nil
        )
    }

    @objc func trackEventCompleted(
        _ startedSpan: EventSpan,
        properties: [String: String]
    ) -> EventSpan {
        let spanKey = "\(startedSpan.traceId)-\(startedSpan.spanId)"

        guard let spanState = activeSpans.removeValue(forKey: spanKey) else {
            print("❌ [EVENT] Span not found: \(spanKey)")
            return createErrorEventSpan(
                operationName: startedSpan.operationName,
                traceId: startedSpan.traceId,
                environment: startedSpan.environment
            )
        }

        let durationMs = Int64(Date().timeIntervalSince(spanState.startTime) * 1000)

        print("🔍 [EVENT] Completed: \(spanState.operationName) - \(durationMs)ms")

        // Delegate to analytics
        AnalyticsManager.shared.trackEventCompleted(
            operationName: startedSpan.operationName,
            traceId: startedSpan.traceId,
            spanId: startedSpan.spanId,
            durationMs: durationMs,
            properties: properties
        )

        return EventSpan(
            traceId: startedSpan.traceId,
            spanId: startedSpan.spanId,
            eventType: "EVENT_COMPLETED",
            operationName: startedSpan.operationName,
            traceFlags: spanState.traceFlags,
            isSampled: spanState.traceFlags == "01",
            environment: startedSpan.environment,
            durationMs: durationMs
        )
    }

    @objc func trackError(
        _ operationName: String,
        errorMessage: String,
        startedSpan: EventSpan?,
        properties: [String: String]
    ) -> EventSpan {
        let traceId = startedSpan?.traceId ?? generateTraceId()
        let spanId = startedSpan?.spanId ?? generateSpanId()
        let environment = startedSpan?.environment ?? getCurrentEnvironment()

        // End the span if it was started
        if let startedSpan = startedSpan {
            let spanKey = "\(startedSpan.traceId)-\(startedSpan.spanId)"
            activeSpans.removeValue(forKey: spanKey)
        }

        print("🔍 [EVENT] Error: \(operationName) - \(errorMessage)")

        // Delegate to analytics
        AnalyticsManager.shared.trackError(
            operationName: operationName,
            errorMessage: errorMessage,
            traceId: traceId,
            spanId: spanId,
            properties: properties
        )

        return EventSpan(
            traceId: traceId,
            spanId: spanId,
            eventType: "ERROR",
            operationName: operationName,
            traceFlags: "01",
            isSampled: true,
            environment: environment,
            durationMs: nil
        )
    }

    @objc func setUserId(_ userId: String) {
        UserManager.shared.setUserId(userId)
    }

    private func getCurrentEnvironment() -> String {
        #if DEBUG
        return "DEVELOPMENT"
        #else
        return "PRODUCTION"
        #endif
    }

    private func generateTraceId() -> String {
        UUID().uuidString.lowercased().replacingOccurrences(of: "-", with: "")
    }

    private func generateSpanId() -> String {
        String(format: "%016llx", UInt64.random(in: 0..<UInt64.max))
    }

    private func createErrorEventSpan(
        operationName: String,
        traceId: String,
        environment: String
    ) -> EventSpan {
        return EventSpan(
            traceId: traceId,
            spanId: generateSpanId(),
            eventType: "ERROR",
            operationName: operationName,
            traceFlags: "00",
            isSampled: false,
            environment: environment,
            durationMs: nil
        )
    }
}

// Swift data model - matches Kotlin EventSpan
@objc class EventSpan: NSObject {
    @objc let traceId: String
    @objc let spanId: String
    @objc let eventType: String
    @objc let operationName: String
    @objc let traceFlags: String
    @objc let isSampled: Bool
    @objc let environment: String
    @objc let durationMs: NSNumber?

    init(
        traceId: String,
        spanId: String,
        eventType: String,
        operationName: String,
        traceFlags: String,
        isSampled: Bool,
        environment: String,
        durationMs: Int64?
    ) {
        self.traceId = traceId
        self.spanId = spanId
        self.eventType = eventType
        self.operationName = operationName
        self.traceFlags = traceFlags
        self.isSampled = isSampled
        self.environment = environment
        self.durationMs = durationMs.map { NSNumber(value: $0) }
        super.init()
    }
}

// Delegate implementations - to be filled in with actual analytics
class AnalyticsManager {
    static let shared = AnalyticsManager()

    func trackEventStarted(
        operationName: String,
        traceId: String,
        spanId: String,
        properties: [String: String]
    ) {
        // TODO: Implement with actual analytics (Crashlytics, Segment, etc.)
    }

    func trackEventCompleted(
        operationName: String,
        traceId: String,
        spanId: String,
        durationMs: Int64,
        properties: [String: String]
    ) {
        // TODO: Implement with actual analytics
    }

    func trackError(
        operationName: String,
        errorMessage: String,
        traceId: String,
        spanId: String,
        properties: [String: String]
    ) {
        // TODO: Implement with actual analytics
    }
}

class UserManager {
    static let shared = UserManager()

    func setUserId(_ userId: String) {
        // TODO: Implement with actual analytics
    }
}
