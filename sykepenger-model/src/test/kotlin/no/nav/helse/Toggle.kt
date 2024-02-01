package no.nav.helse

import java.lang.reflect.Method
import no.nav.helse.Toggle.Companion.disable
import no.nav.helse.Toggle.Companion.enable
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import org.junit.platform.commons.support.AnnotationSupport
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(EnableFeriepengerInterceptor::class)
internal annotation class EnableFeriepenger

private class EnableFeriepengerInterceptor: InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        Toggle.SendFeriepengeOppdrag.enable { invocation.proceed() }
    }
}
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(EnableSpekematInterceptor::class)
internal annotation class EnableSpekemat

private class EnableSpekematInterceptor: InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        Toggle.Spekemat.enable { invocation.proceed() }
    }
}
