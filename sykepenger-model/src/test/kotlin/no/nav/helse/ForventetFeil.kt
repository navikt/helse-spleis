package no.nav.helse

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import org.junit.platform.commons.logging.LoggerFactory.getLogger
import org.junit.platform.commons.support.AnnotationSupport.findAnnotation
import java.lang.reflect.Method

@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@ExtendWith(ForventetFeilInterceptor::class)
@Deprecated(message = "Bruk assertForventetFeil istedenfor")
internal annotation class ForventetFeil(val forklaring: String)

private class ForventetFeilInterceptor: InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        val forklaring = findAnnotation(extensionContext.element, ForventetFeil::class.java).get().forklaring
        val forventetFeil = assertThrows<Throwable>(forklaring) { invocation.proceed() }
        getLogger(invocationContext.targetClass).info(forventetFeil) { forklaring }
    }
}
