package no.nav.helse

import java.lang.reflect.Method
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import org.junit.platform.commons.support.AnnotationSupport

private const val ØnsketOppførsel = "✅ Koden oppfører seg nå som ønsket! Fjern bruken av 'assertForventetFeil', og behold kun assertions for ønsket oppførsel ✅"
private const val FeilITestkode = "☠️️ Feil i testkoden, feiler ikke på assertions ☠️️"

private fun Throwable.håndterNåOppførselFeil(harØnsketOppførsel: Boolean) {
    if (harØnsketOppførsel) throw AssertionError(ØnsketOppførsel)
    if (this is AssertionError) throw AssertionError("⚠️ Koden har endret nå-oppførsel, men ikke til ønsket oppførsel ⚠️️️", this)
    throw AssertionError(FeilITestkode, this)
}

private fun Throwable.håndterØnsketOppførselFeil(forklaring: String?)= when (this) {
    is AssertionError -> println("☹️ Det er kjent at vi ikke har ønsket oppførsel for ${forklaring?:"denne testen"} ☹️️")
    else -> throw AssertionError(FeilITestkode, this)
}

internal fun assertForventetFeil(forklaring: String? = null, nå: () -> Unit, ønsket: () -> Unit) {
    runCatching(nå).exceptionOrNull()?.håndterNåOppførselFeil(harØnsketOppførsel = runCatching(ønsket).isSuccess)
    assertThrows<Throwable>(ØnsketOppførsel) { ønsket() }.håndterØnsketOppførselFeil(forklaring)
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(FeilerMedHåndterInntektsmeldingOppdeltInterceptor::class)
internal annotation class FeilerMedHåndterInntektsmeldingOppdelt(val fordi: String)

private class FeilerMedHåndterInntektsmeldingOppdeltInterceptor: InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        if (Toggle.HåndterInntektsmeldingOppdelt.enabled) {
            assertThrows<Throwable> { invocation.proceed() }
            val fordi = AnnotationSupport.findAnnotation(extensionContext.element, FeilerMedHåndterInntektsmeldingOppdelt::class.java).get().fordi
            println("😭 Fungerer ikke med oppdelt håndtering av inntektsmelding $fordi")
        } else {
            invocation.proceed()
        }
    }
}