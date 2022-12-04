package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.RevurderingIgangsattEvent.VedtaksperiodeData
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RevurderingseventyrEventTest : AbstractEndToEndTest() {

    @Test
    fun `happy case`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag))).meldingsreferanseId()

        revurderingIgangsattEvent {
            this bleForårsaketAv "SYKDOMSTIDSLINJE"
            this medSkjæringstidspunkt 1.januar
        }
    }

    @Test
    fun `to vedtaksperioder berørt av en revurdering -- første håndterer`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag))).meldingsreferanseId()
        val januar = observatør.utbetalteVedtaksperioder.first()
        val februar = observatør.utbetalteVedtaksperioder.last()

        revurderingIgangsattEvent {
            this bleForårsaketAv "SYKDOMSTIDSLINJE"
            this medSkjæringstidspunkt 1.januar
            this medførteRevurderingAv (januar og februar)
        }
    }
    @Test
    fun `to vedtaksperioder berørt av en revurdering av inntekt -- første håndterer`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        val overstyringId = UUID.randomUUID()
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar, meldingsreferanseId = overstyringId)
        val januar = observatør.utbetalteVedtaksperioder.first()
        val februar = observatør.utbetalteVedtaksperioder.last()

        revurderingIgangsattEvent {
            this bleForårsaketAv "ARBEIDSGIVEROPPLYSNINGER"
            this medSkjæringstidspunkt 1.januar
            this medførteRevurderingAv (januar og februar)
        }
    }

    @Test
    fun `to vedtaksperioder berørt av en revurdering av sykdsomtidslinje -- siste håndterer`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.februar, Dagtype.Feriedag))).meldingsreferanseId()
        val februar = observatør.utbetalteVedtaksperioder.last()

        revurderingIgangsattEvent {
            this bleForårsaketAv "SYKDOMSTIDSLINJE"
            this medSkjæringstidspunkt 1.januar
            this medførteRevurderingAv listOf(februar)
        }
    }

    @Test
    fun `flere revurderinger`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(29.januar, 30.januar, 50.prosent))
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar)

        revurderingIgangsattEvent(0) {
            this bleForårsaketAv "KORRIGERT_SØKNAD"
        }
        revurderingIgangsattEvent(1) {
            this bleForårsaketAv "ARBEIDSGIVEROPPLYSNINGER"
        }
    }

    @Test
    fun `flere arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        val overstyringId = UUID.randomUUID()
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar, meldingsreferanseId = overstyringId)

        val periodeAG1 = observatør.utbetalteVedtaksperioder.first()
        val periodeAG2 = observatør.utbetalteVedtaksperioder.last()

        revurderingIgangsattEvent {
            this bleForårsaketAv "ARBEIDSGIVEROPPLYSNINGER"
            this medSkjæringstidspunkt 1.januar
            this medførteRevurderingAv (periodeAG1 og periodeAG2)
            assertEquals(
                listOf(
                    VedtaksperiodeData(
                        orgnummer = a1,
                        vedtaksperiodeId = periodeAG1,
                        periode = 1.januar til 31.januar,
                        skjæringstidspunkt = 1.januar
                    ),
                    VedtaksperiodeData(
                        orgnummer = a2,
                        vedtaksperiodeId = periodeAG2,
                        periode = 1.januar til 31.januar,
                        skjæringstidspunkt = 1.januar
                    )
                ), this.berørtePerioder
            )
        }
    }

    @Test
    fun `tidligere skjæringstidspunkt -- revurderer inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 15.februar)
        nyttVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)

        val januar = observatør.utbetalteVedtaksperioder[0]
        val februar = observatør.utbetalteVedtaksperioder[1]
        val mars = observatør.utbetalteVedtaksperioder[2]
        val april = observatør.utbetalteVedtaksperioder[3]

        val overstyringId = UUID.randomUUID()
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar, meldingsreferanseId = overstyringId)

        revurderingIgangsattEvent {
            this bleForårsaketAv "ARBEIDSGIVEROPPLYSNINGER"
            this medSkjæringstidspunkt 1.januar
            this medførteRevurderingAv (januar og februar og mars og april)
            assertEquals(
                listOf(
                        VedtaksperiodeData(orgnummer = a1, vedtaksperiodeId = januar, periode = 1.januar til 31.januar, skjæringstidspunkt = 1.januar),
                        VedtaksperiodeData(orgnummer = a1, vedtaksperiodeId = februar, periode = 1.februar til 15.februar, skjæringstidspunkt = 1.januar),
                        VedtaksperiodeData(orgnummer = a1, vedtaksperiodeId = mars, periode = 1.mars til 31.mars, skjæringstidspunkt = 1.mars),
                        VedtaksperiodeData(orgnummer = a1, vedtaksperiodeId = april, periode = 1.april til 30.april, skjæringstidspunkt = 1.mars)
                    )
                , this.berørtePerioder
            )
        }
    }
    @Test
    fun `tidligere skjæringstidspunkt -- revurderer tidslinje`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 15.februar)
        nyttVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)

        val februar = observatør.utbetalteVedtaksperioder[1]
        val mars = observatør.utbetalteVedtaksperioder[2]
        val april = observatør.utbetalteVedtaksperioder[3]

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Dagtype.Feriedag)))

        revurderingIgangsattEvent {
            this bleForårsaketAv "SYKDOMSTIDSLINJE"
            this medSkjæringstidspunkt 1.januar
            this medførteRevurderingAv (februar og mars og april)
        }
    }

    private fun revurderingIgangsattEvent(nr: Int = 0, assertBlock: PersonObserver.RevurderingIgangsattEvent.() -> Unit) {
       val revurderingIgangsattEvent = observatør.revurderingIgangsattEvent[nr]
        revurderingIgangsattEvent.run(assertBlock)
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.bleForårsaketAv(årsak: String): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(årsak, this.årsak)
        return this
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.medSkjæringstidspunkt(dato: LocalDate): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(dato, this.skjæringstidspunkt)
        return this
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.medførteRevurderingAv(vedtaksperiodeIder: List<UUID>): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(vedtaksperiodeIder, this.berørtePerioder.map { it.vedtaksperiodeId })
        return this
    }

    private infix fun UUID.og(other: UUID) = listOf(this, other)
    private infix fun List<UUID>.og(other: UUID) = this + other

}
