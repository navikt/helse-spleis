package no.nav.helse

import org.junit.jupiter.api.assertThrows

private const val ØnsketOppførsel = "✅ Testen oppfører seg nå som ønsket! Fjern bruken av 'assertForventetFeil', og behold kun assertions for ønsket oppførsel ✅"
private const val FeilITestkode = "☠️️ Feil i testkoden, feiler ikke på assertions ☠️️"

private fun Throwable.håndterNåOppførselFeil(harØnsketOppførsel: Boolean) {
    if (harØnsketOppførsel) throw AssertionError(ØnsketOppførsel)
    if (this is AssertionError) throw AssertionError("⚠️ Testen har endret nå-oppførsel, men ikke til ønsket oppførsel ⚠️️️", this)
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
