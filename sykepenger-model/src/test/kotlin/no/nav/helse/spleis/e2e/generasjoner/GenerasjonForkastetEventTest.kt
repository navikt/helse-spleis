package no.nav.helse.spleis.e2e.generasjoner

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.AbstractPersonTest.Companion.UNG_PERSON_FNR_2018
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.inspectors.VedtaksperiodeInspektør.Generasjon.Generasjontilstand.TIL_INFOTRYGD
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GenerasjonForkastetEventTest : AbstractDslTest() {

    @Test
    fun `uberegnet generasjon forkastes`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            val generasjonForkastetEvent = observatør.generasjonForkastetEventer.single()
            val sisteGenerasjon = inspektørForkastet(1.vedtaksperiode).generasjoner.single()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId,
                automatiskBehandling = true
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(TIL_INFOTRYGD, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonForkastetEvent)
        }
    }
    @Test
    fun `uberegnet generasjon forkastes manuelt`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false, automatiskBehandling = false)
            val generasjonForkastetEvent = observatør.generasjonForkastetEventer.single()
            val sisteGenerasjon = inspektørForkastet(1.vedtaksperiode).generasjoner.single()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId,
                automatiskBehandling = false
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(TIL_INFOTRYGD, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonForkastetEvent)
        }
    }

    @Test
    fun `generasjon uten vedtak forkastes`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            val generasjonForkastetEvent = observatør.generasjonForkastetEventer.single()
            val sisteGenerasjon = inspektørForkastet(1.vedtaksperiode).generasjoner.last()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId,
                automatiskBehandling = true
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(TIL_INFOTRYGD, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonForkastetEvent)
        }
    }

    @Test
    fun `annullering oppretter ny generasjon som forkastes`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterAnnullering(inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            val generasjonForkastetEvent = observatør.generasjonForkastetEventer.single()
            val sisteGenerasjon = inspektørForkastet(1.vedtaksperiode).generasjoner.last()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonForkastetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId,
                automatiskBehandling = false
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(TIL_INFOTRYGD, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonForkastetEvent)
            val generasjonOpprettetEventer = observatør.generasjonOpprettetEventer
            assertEquals(2, generasjonOpprettetEventer.size)
            val sisteGenerasjonOpprettet = generasjonOpprettetEventer.last()
            assertEquals(sisteGenerasjon.id, sisteGenerasjonOpprettet.generasjonId)
            assertEquals(PersonObserver.GenerasjonOpprettetEvent.Type.TilInfotrygd, sisteGenerasjonOpprettet.type)
        }
    }
}
