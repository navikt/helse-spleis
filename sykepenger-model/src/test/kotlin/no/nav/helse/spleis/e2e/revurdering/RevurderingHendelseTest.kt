package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.RevurderingÅrsak
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.nyttVedtak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RevurderingHendelseTest : AbstractEndToEndTest() {

    @Test
    fun `happy case`() {
        nyttVedtak(1.januar, 31.januar)
        val overstyring = håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag))).meldingsreferanseId()
        val vedtaksperiode = observatør.utbetalteVedtaksperioder.single()

        revurderingIgangsattEvent {
            this bleForårsaketAv RevurderingÅrsak.SYKDOMSTIDSLINJE grunnet overstyring
            this bleInitertAv vedtaksperiode medSkjæringstidspunkt 1.januar
        }
    }

    @Test
    fun `to vedtaksperioder berørt av en revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        val overstyring = håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag))).meldingsreferanseId()
        val januar = observatør.utbetalteVedtaksperioder.first()
        val februar = observatør.utbetalteVedtaksperioder.last()

        revurderingIgangsattEvent {
            this bleForårsaketAv RevurderingÅrsak.SYKDOMSTIDSLINJE grunnet overstyring
            this bleInitertAv januar medSkjæringstidspunkt 1.januar
            this medførteRevurderingAv (januar og februar)
        }
    }

    private fun revurderingIgangsattEvent(assertBlock:  PersonObserver.RevurderingIgangsattEvent.() -> Unit) {
       val revurderingIgangsattEvent = observatør.revurderingIgangsattEvent.single()
        revurderingIgangsattEvent.run(assertBlock)
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.bleInitertAv(vedtaksperiodeId: UUID): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(vedtaksperiodeId, this.initertAvVedtaksperiode)
        return this
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.bleForårsaketAv(årsak: RevurderingÅrsak): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(årsak, this.revurderingÅrsak)
        return this
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.grunnet(hendelseId: UUID): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(hendelseId, hendelseId) // TODO
        return this
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.medSkjæringstidspunkt(dato: LocalDate): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(dato, this.skjæringstidspunkt)
        return this
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.medførteRevurderingAv(vedtaksperiodeIder: List<UUID>): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(vedtaksperiodeIder, this.berørtePerioder)
        return this
    }

    private infix fun UUID.og(other: UUID) = listOf(this, other)

}
