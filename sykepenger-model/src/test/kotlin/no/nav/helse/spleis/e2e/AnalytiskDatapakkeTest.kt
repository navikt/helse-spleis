package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AnalytiskDatapakkeTest : AbstractDslTest() {
    private fun UUID.sisteBehandlingId(orgnr: String) = inspektør(orgnr).vedtaksperioder(this).inspektør.behandlinger.last().id

    @Test
    fun `Standard analytisk datapakke`() {
        a1 {
            nyttVedtak(januar)

            val event = observatør.analytiskDatapakkeEventer.last()
            val expected = PersonObserver.AnalytiskDatapakkeEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = 1.vedtaksperiode.sisteBehandlingId(a1),
                skjæringstidspunkt = 1.januar,
                beløpTilBruker = PersonObserver.AnalytiskDatapakkeEvent.Pengeinformasjon(
                    totalBeløp = 0.0,
                    nettoBeløp = 0.0
                ),
                beløpTilArbeidsgiver = PersonObserver.AnalytiskDatapakkeEvent.Pengeinformasjon(
                    totalBeløp = 15741.0,
                    nettoBeløp = 15741.0
                ),
                fom = 1.januar,
                tom = 31.januar,
                antallForbrukteSykedagerEtterPeriode = PersonObserver.AnalytiskDatapakkeEvent.Daginformasjon(
                    antallDager = 11,
                    nettoDager = 11
                ),
                antallGjenståendeSykedagerEtterPeriode = PersonObserver.AnalytiskDatapakkeEvent.Daginformasjon(
                    antallDager = 237,
                    nettoDager = 237
                ),
                harAndreInntekterIBeregning = false
            )

            assertEquals(expected, event)
        }
    }

    @Test
    fun `Standard analytisk datapakke - revurdering`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrArbeidsgiveropplysninger(
                1.januar,
                listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = 25000.månedlig,
                        refusjonsopplysninger = listOf(
                            Triple(1.januar, 25.januar, 25000.månedlig),
                            Triple(26.januar, null, Inntekt.INGEN),
                        )
                    )
                )
            )
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()

            val event = observatør.analytiskDatapakkeEventer.last()
            val expected = PersonObserver.AnalytiskDatapakkeEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = 1.vedtaksperiode.sisteBehandlingId(a1),
                skjæringstidspunkt = 1.januar,
                beløpTilBruker = PersonObserver.AnalytiskDatapakkeEvent.Pengeinformasjon(
                    totalBeløp = 4616.0,
                    nettoBeløp = 4616.0
                ),
                beløpTilArbeidsgiver = PersonObserver.AnalytiskDatapakkeEvent.Pengeinformasjon(
                    totalBeløp = 6924.0,
                    nettoBeløp = -8817.0
                ),
                fom = 1.januar,
                tom = 31.januar,
                antallForbrukteSykedagerEtterPeriode = PersonObserver.AnalytiskDatapakkeEvent.Daginformasjon(
                    antallDager = 10,
                    nettoDager = -1
                ),
                antallGjenståendeSykedagerEtterPeriode = PersonObserver.AnalytiskDatapakkeEvent.Daginformasjon(
                    antallDager = 238,
                    nettoDager = 1
                ),
                harAndreInntekterIBeregning = false
            )

            assertEquals(expected, event)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }
    }
}
