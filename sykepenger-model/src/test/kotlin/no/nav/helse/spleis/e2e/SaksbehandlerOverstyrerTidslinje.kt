package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SaksbehandlerOverstyrerTidslinje : AbstractEndToEndTest() {

    @Test
    @Disabled
    fun `overstyrer sykedag på slutten av perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterSøknadMedValidering(0, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterSimulering(0)
        håndterOverstyring(listOf(manuellArbeidsdag(26.januar)))
        håndterUtbetalingsgodkjenning(0, true)

        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSA", inspektør.sykdomshistorikk.sykdomstidslinje())
    }

    private fun manuellSykedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, 100)
    private fun manuellArbeidsdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Arbeidsdag)
}
