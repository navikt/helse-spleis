package no.nav.helse.spleis.e2e

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.Avstemming
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.reflection.castAsList
import no.nav.helse.serde.reflection.castAsMap
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AvstemmingTest : AbstractEndToEndTest() {

    @Test
    fun avstemmer() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent)) // delvis overlappende søknad
        nyttVedtak(1.mars, 20.mars, 100.prosent)
        nyttVedtak(1.april, 30.april, 100.prosent)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.april, Dagtype.Feriedag)))
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode, automatiskBehandling = true)
        håndterUtbetalt()
        tilYtelser(1.juni, 30.juni, 100.prosent, 1.juni)
        val avstemming = Avstemming(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018.toString()
        )
        person.håndter(avstemming)

        val avstemmingresultat = observatør.avstemming

        val arbeidsgivere = avstemmingresultat["arbeidsgivere"].castAsList<Map<String, Any>>()
        assertEquals(1, arbeidsgivere.size)
        val forkastede = arbeidsgivere[0]["forkastedeVedtaksperioder"].castAsList<Map<String, Any>>()
        val aktive = arbeidsgivere[0]["vedtaksperioder"].castAsList<Map<String, Any>>()
        val utbetalinger = arbeidsgivere[0]["utbetalinger"].castAsList<Map<String, Any>>()
        assertEquals(ORGNUMMER, arbeidsgivere[0]["organisasjonsnummer"])
        assertEquals(2, forkastede.size)
        assertEquals(3, aktive.size)
        assertEquals(4, utbetalinger.size)
        assertEquals(1.vedtaksperiode.id(ORGNUMMER), forkastede[0]["id"])
        assertEquals(TilstandType.TIL_INFOTRYGD, forkastede[0]["tilstand"])
        assertTrue(forkastede[0]["opprettet"] is LocalDateTime)
        assertTrue(forkastede[0]["oppdatert"] is LocalDateTime)
        assertEquals(0, forkastede[0]["utbetalinger"].castAsList<String>().size)
        assertEquals(2.vedtaksperiode.id(ORGNUMMER), forkastede[1]["id"])
        assertEquals(TilstandType.TIL_INFOTRYGD, forkastede[1]["tilstand"])
        assertTrue(forkastede[1]["opprettet"] is LocalDateTime)
        assertTrue(forkastede[1]["oppdatert"] is LocalDateTime)
        assertEquals(0, forkastede[1]["utbetalinger"].castAsList<String>().size)

        assertEquals(3.vedtaksperiode.id(ORGNUMMER), aktive[0]["id"])
        assertEquals(TilstandType.AVSLUTTET, aktive[0]["tilstand"])
        assertTrue(aktive[0]["opprettet"] is LocalDateTime)
        assertTrue(aktive[0]["oppdatert"] is LocalDateTime)
        assertEquals(1, aktive[0]["utbetalinger"].castAsList<String>().size)
        assertEquals(4.vedtaksperiode.id(ORGNUMMER), aktive[1]["id"])
        assertEquals(TilstandType.AVSLUTTET, aktive[1]["tilstand"])
        assertTrue(aktive[1]["opprettet"] is LocalDateTime)
        assertTrue(aktive[1]["oppdatert"] is LocalDateTime)
        assertEquals(2, aktive[1]["utbetalinger"].castAsList<String>().size)
        assertEquals(5.vedtaksperiode.id(ORGNUMMER), aktive[2]["id"])
        assertEquals(TilstandType.AVVENTER_SIMULERING, aktive[2]["tilstand"])
        assertTrue(aktive[2]["opprettet"] is LocalDateTime)
        assertTrue(aktive[2]["oppdatert"] is LocalDateTime)
        assertEquals(1, aktive[2]["utbetalinger"].castAsList<String>().size)

        assertEquals(1.utbetaling(ORGNUMMER), utbetalinger[0]["id"])
        assertEquals(Utbetalingstatus.UTBETALT, utbetalinger[0]["status"])
        assertEquals("UTBETALING", utbetalinger[0]["type"])
        assertTrue(utbetalinger[0]["opprettet"] is LocalDateTime)
        assertTrue(utbetalinger[0]["oppdatert"] is LocalDateTime)
        assertTrue(utbetalinger[0]["avsluttet"] is LocalDateTime)
        assertEquals("Ola Nordmann", utbetalinger[0]["vurdering"].castAsMap<String, Any>()["ident"]) // Normalt en saksbehandler-navident, ikke navn på person
        assertEquals(false, utbetalinger[0]["vurdering"].castAsMap<String, Any>()["automatiskBehandling"])
        assertEquals(true, utbetalinger[0]["vurdering"].castAsMap<String, Any>()["godkjent"])
        assertTrue(utbetalinger[0]["vurdering"].castAsMap<String, Any>()["tidspunkt"] is LocalDateTime)

        assertEquals(2.utbetaling(ORGNUMMER), utbetalinger[1]["id"])
        assertEquals(Utbetalingstatus.UTBETALT, utbetalinger[1]["status"])
        assertEquals("UTBETALING", utbetalinger[1]["type"])
        assertTrue(utbetalinger[1]["opprettet"] is LocalDateTime)
        assertTrue(utbetalinger[1]["oppdatert"] is LocalDateTime)
        assertTrue(utbetalinger[1]["avsluttet"] is LocalDateTime)
        assertEquals("Ola Nordmann", utbetalinger[1]["vurdering"].castAsMap<String, Any>()["ident"]) // Normalt en saksbehandler-navident, ikke navn på person
        assertEquals(false, utbetalinger[1]["vurdering"].castAsMap<String, Any>()["automatiskBehandling"])
        assertEquals(true, utbetalinger[1]["vurdering"].castAsMap<String, Any>()["godkjent"])
        assertTrue(utbetalinger[1]["vurdering"].castAsMap<String, Any>()["tidspunkt"] is LocalDateTime)

        assertEquals(3.utbetaling(ORGNUMMER), utbetalinger[2]["id"])
        assertEquals(Utbetalingstatus.UTBETALT, utbetalinger[2]["status"])
        assertEquals("REVURDERING", utbetalinger[2]["type"])
        assertTrue(utbetalinger[2]["opprettet"] is LocalDateTime)
        assertTrue(utbetalinger[2]["oppdatert"] is LocalDateTime)
        assertTrue(utbetalinger[2]["avsluttet"] is LocalDateTime)
        assertEquals("Ola Nordmann", utbetalinger[2]["vurdering"].castAsMap<String, Any>()["ident"]) // Normalt en saksbehandler-navident, ikke navn på person
        assertEquals(true, utbetalinger[2]["vurdering"].castAsMap<String, Any>()["automatiskBehandling"])
        assertEquals(true, utbetalinger[2]["vurdering"].castAsMap<String, Any>()["godkjent"])
        assertTrue(utbetalinger[2]["vurdering"].castAsMap<String, Any>()["tidspunkt"] is LocalDateTime)

        assertEquals(4.utbetaling(ORGNUMMER), utbetalinger[3]["id"])
        assertEquals(Utbetalingstatus.IKKE_UTBETALT, utbetalinger[3]["status"])
        assertEquals("UTBETALING", utbetalinger[3]["type"])
        assertTrue(utbetalinger[3]["opprettet"] is LocalDateTime)
        assertTrue(utbetalinger[3]["oppdatert"] is LocalDateTime)
        assertNull(utbetalinger[3]["avsluttet"])
        assertNull(utbetalinger[3]["vurdering"])
    }
}
