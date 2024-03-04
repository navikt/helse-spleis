package no.nav.helse.spleis.e2e.generasjoner

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.onsdag
import no.nav.helse.person.AbstractPersonTest.Companion.UNG_PERSON_FNR_2018
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.*
import no.nav.helse.søndag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GenerasjonLukketEventTest : AbstractDslTest() {

    @Test
    fun `generasjon lukkes når vedtak fattes`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true)
            val generasjonLukketEvent = observatør.generasjonLukketEventer.single()
            val sisteGenerasjon = inspektør(1.vedtaksperiode).generasjoner.single()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonLukketEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_UTBETALING)
            assertEquals(VEDTAK_FATTET, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonLukketEvent)
        }
    }

    @Test
    fun `generasjon lukkes ikke når vedtak avvises`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertEquals(0, observatør.generasjonLukketEventer.size)
            val sisteGenerasjon = inspektørForkastet(1.vedtaksperiode).generasjoner.single()
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(TIL_INFOTRYGD, sisteGenerasjon.tilstand)
        }
    }

    @Test
    fun `generasjon lukkes når revurdert vedtak avvises`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            val generasjoner = inspektør(1.vedtaksperiode).generasjoner
            assertEquals(2, observatør.generasjonLukketEventer.size)
            assertEquals(2, generasjoner.size)
            val sisteGenerasjon = generasjoner.last()
            assertTilstand(1.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING_REVURDERING)
            assertEquals(REVURDERT_VEDTAK_AVVIST, sisteGenerasjon.tilstand)
        }
    }

    @Test
    fun `generasjon lukkes når vedtak uten utbetaling fattes`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true)
            val generasjonLukketEvent = observatør.generasjonLukketEventer.single()
            val sisteGenerasjon = inspektør(1.vedtaksperiode).generasjoner.single()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonLukketEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId
            )
            assertTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
            assertEquals(VEDTAK_IVERKSATT, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonLukketEvent)
        }
    }

    @Test
    fun `generasjon lukkes når periode går til auu`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            val generasjonLukketEvent = observatør.generasjonLukketEventer.single()
            val sisteGenerasjon = inspektør(1.vedtaksperiode).generasjoner.single()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonLukketEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId
            )
            assertTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
            assertEquals(AVSLUTTET_UTEN_VEDTAK, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonLukketEvent)
        }
    }

    @Test
    fun `generasjon lukkes når revurdering fattes`() {
        a1 {
            nyttVedtak(1.januar, onsdag den 31.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(onsdag den 31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true)

            val generasjonLukketEvent = observatør.generasjonLukketEventer.last()
            val sisteGenerasjon = inspektør(1.vedtaksperiode).generasjoner.last()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonLukketEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_UTBETALING)
            assertEquals(VEDTAK_FATTET, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonLukketEvent)
        }
    }

    @Test
    fun `generasjon lukkes når revurdering avvises`() {
        a1 {
            nyttVedtak(1.januar, onsdag den 31.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(onsdag den 31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)

            val generasjonLukketEvent = observatør.generasjonLukketEventer.last()
            val sisteGenerasjon = inspektør(1.vedtaksperiode).generasjoner.last()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonLukketEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId
            )
            assertTilstand(1.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING_REVURDERING)
            assertEquals(REVURDERT_VEDTAK_AVVIST, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonLukketEvent)
        }
    }

    @Test
    fun `generasjon lukkes når revurdering uten utbetaling fattes`() {
        a1 {
            nyttVedtak(1.januar, søndag den 28.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(søndag den 28.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true)

            val generasjonLukketEvent = observatør.generasjonLukketEventer.last()
            val sisteGenerasjon = inspektør(1.vedtaksperiode).generasjoner.last()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonLukketEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId
            )
            assertTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
            assertEquals(VEDTAK_IVERKSATT, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonLukketEvent)
        }
    }

    @Test
    fun `generasjon lukkes når revurdering gjør om til auu - med tidligere utbetaling`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterOverstyrTidslinje((17.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Feriedag) })
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true)
            håndterUtbetalt()

            val generasjonLukketEvent = observatør.generasjonLukketEventer.last()
            val sisteGenerasjon = inspektør(1.vedtaksperiode).generasjoner.last()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonLukketEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId
            )
            assertTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
            assertEquals(VEDTAK_IVERKSATT, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonLukketEvent)
        }
    }

    @Test
    fun `generasjon lukkes når vedtak uten utbetaling fattes - uten tidligere utbetaling`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true)

            håndterOverstyrTidslinje((17.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Feriedag) })
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true)

            val generasjonLukketEvent = observatør.generasjonLukketEventer.last()
            val sisteGenerasjon = inspektør(1.vedtaksperiode).generasjoner.last()
            val forventetGenerasjonId = sisteGenerasjon.id
            val forventetGenerasjonEvent = PersonObserver.GenerasjonLukketEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = "42",
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                generasjonId = forventetGenerasjonId
            )
            assertTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
            assertEquals(VEDTAK_IVERKSATT, sisteGenerasjon.tilstand)
            assertEquals(forventetGenerasjonEvent, generasjonLukketEvent)
        }
    }
}
