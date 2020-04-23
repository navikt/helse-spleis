package no.nav.helse.spleis

import org.slf4j.MDC

internal fun withMDC(context: Map<String, String>, block: () -> Unit) {
    val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
    try {
        MDC.setContextMap(contextMap + context)
        block()
    } finally {
        MDC.setContextMap(contextMap)
    }
}
