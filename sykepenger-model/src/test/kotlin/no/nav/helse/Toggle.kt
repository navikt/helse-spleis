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
@ExtendWith(EnableToggleInterceptor::class)
internal annotation class EnableToggle(vararg val toggle: KClass<out Toggle>)

private class EnableToggleInterceptor: InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        AnnotationSupport.findAnnotation(extensionContext.testClass, EnableToggle::class.java).ifPresentOrElse({ annotation ->
            annotation.toggle.mapNotNull { it.objectInstance }.enable { invocation.proceed() }
        }) {
            invocation.proceed()
        }
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(DisableToggleInterceptor::class)
internal annotation class DisableToggle(vararg val toggle: KClass<out Toggle>)

private class DisableToggleInterceptor: InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        AnnotationSupport.findAnnotation(extensionContext.testClass, DisableToggle::class.java).ifPresentOrElse({ annotation ->
            annotation.toggle.mapNotNull { it.objectInstance }.disable { invocation.proceed() }
        }) {
            invocation.proceed()
        }
    }
}