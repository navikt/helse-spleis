package no.nav.helse.spleis.e2e.generasjoner

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
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
            val registrert = LocalDateTime.now()
            val innsendt = registrert.minusHours(2)
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), søknadId = søknadId, sendtTilNAVEllerArbeidsgiver = innsendt, registrert = registrert)
            val generasjonOpprettetEvent = observatør.generasjonOpprettetEventer.last()
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                val generasjonId = generasjoner.single().id
                val forventetGenerasjonEvent = PersonObserver.GenerasjonOpprettetEvent(
                    fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                    aktørId = "42",
                    organisasjonsnummer = a1,
                    vedtaksperiodeId = 1.vedtaksperiode,
                    generasjonId = generasjonId,
                    type = PersonObserver.GenerasjonOpprettetEvent.Type.Søknad,
                    kilde = PersonObserver.GenerasjonOpprettetEvent.Kilde(
                        meldingsreferanseId = søknadId,
                        innsendt = innsendt,
                        registert = registrert,
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
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.Søknad, førsteEvent.type)
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
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.Søknad, førsteEvent.type)
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.Omgjøring, andreEvent.type)
        }
    }

    @Test
    fun `til infotrygd`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            forlengVedtak(1.februar, 28.februar)
            håndterAnnullering(inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            val generasjonOpprettetEventer = observatør.generasjonOpprettetEventer
            assertEquals(4, generasjonOpprettetEventer.size)
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.Søknad, generasjonOpprettetEventer[0].type)
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.Søknad, generasjonOpprettetEventer[1].type)
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.TilInfotrygd, generasjonOpprettetEventer[2].type)
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.TilInfotrygd, generasjonOpprettetEventer[3].type)
        }
    }
}
