package no.nav.helse.spleis.e2e

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avstemming
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.serde.reflection.castAsList
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AvstemmingTest : AbstractEndToEndTest() {

    @Test
    fun `avstemmer`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 3.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 2.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 3.januar, 100.prosent))
        nyttVedtak(4.januar, 26.januar, 100.prosent)
        nyttVedtak(1.mars, 31.mars, 100.prosent)
        tilYtelser(1.mai, 30.mai, 100.prosent, 1.mai)
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
        assertEquals(1, forkastede.size)
        assertEquals(3, aktive.size)
        assertEquals(3, utbetalinger.size)
        assertEquals(1.vedtaksperiode.id(ORGNUMMER), forkastede[0]["id"])
        assertEquals(TilstandType.TIL_INFOTRYGD, forkastede[0]["tilstand"])
        assertTrue(forkastede[0]["tidsstempel"] is LocalDateTime)

        assertEquals(2.vedtaksperiode.id(ORGNUMMER), aktive[0]["id"])
        assertEquals(TilstandType.AVSLUTTET, aktive[0]["tilstand"])
        assertTrue(aktive[0]["tidsstempel"] is LocalDateTime)
        assertEquals(3.vedtaksperiode.id(ORGNUMMER), aktive[1]["id"])
        assertEquals(TilstandType.AVSLUTTET, aktive[1]["tilstand"])
        assertTrue(aktive[1]["tidsstempel"] is LocalDateTime)
        assertEquals(4.vedtaksperiode.id(ORGNUMMER), aktive[2]["id"])
        assertEquals(TilstandType.AVVENTER_SIMULERING, aktive[2]["tilstand"])
        assertTrue(aktive[2]["tidsstempel"] is LocalDateTime)

        assertEquals(1.utbetaling(ORGNUMMER), utbetalinger[0]["id"])
        assertEquals(Utbetalingstatus.UTBETALT, utbetalinger[0]["status"])
        assertEquals("UTBETALING", utbetalinger[0]["type"])
        assertTrue(utbetalinger[0]["tidsstempel"] is LocalDateTime)
        assertEquals(2.utbetaling(ORGNUMMER), utbetalinger[1]["id"])
        assertEquals(Utbetalingstatus.UTBETALT, utbetalinger[1]["status"])
        assertEquals("UTBETALING", utbetalinger[1]["type"])
        assertTrue(utbetalinger[1]["tidsstempel"] is LocalDateTime)
        assertEquals(3.utbetaling(ORGNUMMER), utbetalinger[2]["id"])
        assertEquals(Utbetalingstatus.IKKE_UTBETALT, utbetalinger[2]["status"])
        assertEquals("UTBETALING", utbetalinger[2]["type"])
        assertTrue(utbetalinger[2]["tidsstempel"] is LocalDateTime)
    }
}
