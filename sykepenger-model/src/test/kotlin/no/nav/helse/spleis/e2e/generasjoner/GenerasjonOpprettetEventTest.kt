package no.nav.helse.spleis.e2e.generasjoner

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.AbstractPersonTest.Companion.UNG_PERSON_FNR_2018
import no.nav.helse.person.PersonObserver
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GenerasjonOpprettetEventTest : AbstractDslTest() {

    @Test
    fun `event om opprettet generasjon`() {
        a1 {
            val søknadId = UUID.randomUUID()
            val opprettet = LocalDateTime.now()
            val innsendt = opprettet.minusHours(2)
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), søknadId = søknadId, sendtTilNAVEllerArbeidsgiver = innsendt, opprettet = opprettet)
            val generasjonOpprettetEvent = observatør.generasjonOpprettetEventer.last()
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                val generasjonId = generasjoner.single().id
                val forventetGenerasjonEvent = PersonObserver.GenerasjonOpprettetEvent(
                    fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                    aktørId = "42",
                    organisasjonsnummer = a1,
                    vedtaksperiodeId = 1.vedtaksperiode,
                    generasjonId = generasjonId,
                    type = PersonObserver.GenerasjonOpprettetEvent.Type.Førstegangsbehandling,
                    kilde = PersonObserver.GenerasjonOpprettetEvent.Kilde(
                        meldingsreferanseId = søknadId,
                        innsendt = innsendt,
                        registert = opprettet,
                        avsender = Avsender.SYKMELDT
                    )
                )
                assertEquals(forventetGenerasjonEvent, generasjonOpprettetEvent)
            }
        }
    }

    @Test
    fun `revurdering`() {
        a1 {
            nyttVedtak(1.januar, 31.januar, 100.prosent)
            håndterSøknad(Sykdom(1.januar, 31.januar, 80.prosent))
            val generasjonOpprettetEventer = observatør.generasjonOpprettetEventer
            assertEquals(2, generasjonOpprettetEventer.size)
            val førsteEvent = generasjonOpprettetEventer.first()
            val andreEvent = generasjonOpprettetEventer.last()
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.Førstegangsbehandling, førsteEvent.type)
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.Revurdering, andreEvent.type)
        }
    }

    @Test
    fun `omgjøring`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterInntektsmelding(listOf(25.desember(2017) til 10.januar), beregnetInntekt = INNTEKT)
            val generasjonOpprettetEventer = observatør.generasjonOpprettetEventer
            assertEquals(2, generasjonOpprettetEventer.size)
            val førsteEvent = generasjonOpprettetEventer.first()
            val andreEvent = generasjonOpprettetEventer.last()
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.Førstegangsbehandling, førsteEvent.type)
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.Omgjøring, andreEvent.type)
        }
    }
}
