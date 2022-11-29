package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
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

internal class RevurderingHendelseTest : AbstractEndToEndTest() {

    @Test
    fun `happy case`() {
        nyttVedtak(1.januar, 31.januar)
        val overstyring = håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag))).meldingsreferanseId()
        val vedtaksperiode = observatør.utbetalteVedtaksperioder.single()

        revurderingIgangsattEvent {
            this bleForårsaketAv "SYKDOMSTIDSLINJE" grunnet overstyring
            this bleInitiertAv vedtaksperiode medSkjæringstidspunkt 1.januar
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
            this bleForårsaketAv "SYKDOMSTIDSLINJE" grunnet overstyring
            this bleInitiertAv januar medSkjæringstidspunkt 1.januar
            this medførteRevurderingAv (januar og februar)
        }
    }

    @Test
    fun `flere revurderinger`() {
        nyttVedtak(1.januar, 31.januar)
        val søknadId = håndterSøknad(Søknad.Søknadsperiode.Sykdom(29.januar, 30.januar, 50.prosent))
        val overstyringId = UUID.randomUUID()
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar, meldingsreferanseId = overstyringId)

        revurderingIgangsattEvent(0) {
            this bleForårsaketAv "KORRIGERT_SØKNAD" grunnet søknadId
        }
        revurderingIgangsattEvent(1) {
            this bleForårsaketAv "INNTEKT" grunnet overstyringId
        }
    }

    @Test
    fun `flere arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        val overstyringId = UUID.randomUUID()
        håndterOverstyrInntekt(30000.månedlig, skjæringstidspunkt = 1.januar, meldingsreferanseId = overstyringId)

        val periode1 = observatør.utbetalteVedtaksperioder.first()
        val periode2 = observatør.utbetalteVedtaksperioder.last()

        revurderingIgangsattEvent {
            this bleForårsaketAv "INNTEKT" grunnet overstyringId
            this medførteRevurderingAv (periode1 og periode2)
            assertEquals(setOf(a1, a2), this.berørtePerioder.keys)
        }
    }

    @Test
    fun `tidligere skjæringstidspunkt`() {
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
            this bleForårsaketAv "INNTEKT" grunnet overstyringId
            this bleInitiertAv januar medSkjæringstidspunkt 1.januar
            this medførteRevurderingAv (januar og februar og mars og april)
        }
    }

    private fun revurderingIgangsattEvent(nr: Int = 0, assertBlock: PersonObserver.RevurderingIgangsattEvent.() -> Unit) {
       val revurderingIgangsattEvent = observatør.revurderingIgangsattEvent[nr]
        revurderingIgangsattEvent.run(assertBlock)
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.bleInitiertAv(vedtaksperiodeId: UUID): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(vedtaksperiodeId, this.initiertAvVedtaksperiode)
        return this
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.bleForårsaketAv(årsak: String): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(listOf(årsak), this.revurderingsÅrsak)
        return this
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.grunnet(hendelseId: UUID): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(hendelseId, this.kilde)
        return this
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.medSkjæringstidspunkt(dato: LocalDate): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(dato, this.skjæringstidspunkt)
        return this
    }

    private infix fun PersonObserver.RevurderingIgangsattEvent.medførteRevurderingAv(vedtaksperiodeIder: List<UUID>): PersonObserver.RevurderingIgangsattEvent {
        assertEquals(vedtaksperiodeIder, this.berørtePerioder.values.flatten())
        return this
    }

    private infix fun UUID.og(other: UUID) = listOf(this, other)
    private infix fun List<UUID>.og(other: UUID) = this + other

}
