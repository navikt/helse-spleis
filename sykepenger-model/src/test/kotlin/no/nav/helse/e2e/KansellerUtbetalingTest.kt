package no.nav.helse.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KansellerUtbetalingTest: AbstractEndToEndTest() {

    @BeforeEach internal fun setup() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(0, Søknad.Periode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterSimulering(0)
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
    }

    @Test internal fun `avvis hvis arbeidsgiver er ukjent`() {
        håndterKansellerUtbetaling(orgnummer = "999999")
        inspektør.also {
            assertTrue(it.personLogg.hasErrors(), it.personLogg.toString())
        }
    }

    @Test internal fun `avvis hvis vi ikke finner fagsystemId`() {
        håndterKansellerUtbetaling(fagsystemId = "unknown")
        inspektør.also {
            assertTrue(it.personLogg.hasErrors(), it.personLogg.toString())
        }
    }

    @Test internal fun `kanseller siste utbetaling`() {
        håndterKansellerUtbetaling()
        inspektør.also {
            assertFalse(it.personLogg.hasErrors(), it.personLogg.toString())
        }
    }
}
