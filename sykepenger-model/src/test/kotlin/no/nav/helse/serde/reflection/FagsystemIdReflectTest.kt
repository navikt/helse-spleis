package no.nav.helse.serde.reflection

import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.TypeData.NavDag
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.UtbetalingsdagData
import no.nav.helse.serde.reflection.FagsystemTilstandType.*
import no.nav.helse.serde.reflection.FagsystemTilstandType.Companion.fraTilstand
import no.nav.helse.serde.reflection.FagsystemTilstandType.Companion.tilTilstand
import no.nav.helse.testhelpers.HELG
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingslinjer.FagsystemId.*
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Utbetalingtype.ANNULLERING
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Utbetalingtype.UTBETALING
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

internal class FagsystemIdReflectTest : AbstractFagsystemIdTest() {

    private val reflect get() = FagsystemIdReflect(fagsystemId)

    @Test
    fun `serialize tilstand`() {
        assertEquals(AKTIV, fraTilstand(Aktiv))
        assertEquals(ANNULLERING_OVERFØRT, fraTilstand(AnnulleringOverført))
        assertEquals(ANNULLERING_SENDT, fraTilstand(AnnulleringSendt))
        assertEquals(ANNULLERT, fraTilstand(Annullert))
        assertEquals(AVVIST, fraTilstand(Avvist))
        assertEquals(INITIELL, fraTilstand(Initiell))
        assertEquals(NY, fraTilstand(Ny))
        assertEquals(UBETALT, fraTilstand(Ubetalt))
        assertEquals(UTBETALING_OVERFØRT, fraTilstand(UtbetalingOverført))
        assertEquals(UTBETALING_SENDT, fraTilstand(UtbetalingSendt))
    }

    @Test
    fun `deserialize tilstand`() {
        assertEquals(Aktiv, tilTilstand(AKTIV))
        assertEquals(AnnulleringOverført, tilTilstand(ANNULLERING_OVERFØRT))
        assertEquals(AnnulleringSendt, tilTilstand(ANNULLERING_SENDT))
        assertEquals(Annullert, tilTilstand(ANNULLERT))
        assertEquals(Avvist, tilTilstand(AVVIST))
        assertEquals(Ny, tilTilstand(NY))
        assertEquals(Ubetalt, tilTilstand(UBETALT))
        assertEquals(UtbetalingOverført, tilTilstand(UTBETALING_OVERFØRT))
        assertEquals(UtbetalingSendt, tilTilstand(UTBETALING_SENDT))
        assertThrows<IllegalArgumentException> { tilTilstand(INITIELL) }
    }

    @Test
    fun `serialize fagsystemId`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV)
        val result = reflect.toMap()

        assertEquals(inspektør.fagsystemId(0), result["fagsystemId"])
        assertEquals(Fagområde.SykepengerRefusjon.verdi, result["fagområde"])
        assertEquals(AKTIV, result["tilstand"])
        val utbetalinger = result["utbetalinger"] as List<Map<String, Any?>>
        assertEquals(1, utbetalinger.size)

        assertNotNull(utbetalinger.first()["oppdrag"])
        assertNotNull(utbetalinger.first()["utbetalingstidslinje"])
        assertEquals(UTBETALING, utbetalinger.first()["type"])
        assertEquals(MAKSDATO, utbetalinger.first()["maksdato"])
        assertTrue(utbetalinger.first()["opprettet"] is LocalDateTime)
        val godkjentAv = utbetalinger.first()["godkjentAv"] as Map<String, Any>
        assertEquals(IDENT, godkjentAv["ident"])
        assertEquals(EPOST, godkjentAv["epost"])
        assertTrue(godkjentAv["tidsstempel"] is LocalDateTime)
        assertTrue(utbetalinger.first()["sendt"] is LocalDateTime)
        assertTrue(utbetalinger.first()["avstemmingsnøkkel"] is Long)
        assertTrue(utbetalinger.first()["overføringstidspunkt"] is LocalDateTime)
        assertTrue(utbetalinger.first()["avsluttet"] is LocalDateTime)
    }

    @Test
    fun `serialize fagsystemId med forkastet`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV)
        opprett(5.NAV, 2.HELG, 5.NAV, 10.NAV)
        annuller(0)

        val result = reflect.toMap()

        assertEquals(inspektør.fagsystemId(0), result["fagsystemId"])
        assertEquals(Fagområde.SykepengerRefusjon.verdi, result["fagområde"])
        assertEquals(ANNULLERING_SENDT, result["tilstand"])
        val forkastede = result["forkastet"] as List<Map<String, Any?>>
        assertEquals(1, forkastede.size)

        assertNotNull(forkastede.first()["oppdrag"])
        assertNotNull(forkastede.first()["utbetalingstidslinje"])
        assertEquals(UTBETALING, forkastede.first()["type"])
        assertEquals(MAKSDATO, forkastede.first()["maksdato"])
        assertTrue(forkastede.first()["opprettet"] is LocalDateTime)
        assertNull(forkastede.first()["godkjentAv"])
        assertNull(forkastede.first()["sendt"])
        assertNull(forkastede.first()["avstemmingsnøkkel"])
        assertNull(forkastede.first()["overføringstidspunkt"])
        assertTrue(forkastede.first()["avsluttet"] is LocalDateTime)

        val utbetalinger = result["utbetalinger"] as List<Map<String, Any?>>
        assertEquals(2, utbetalinger.size)
    }

    @Test
    fun `serialize fagsystemId med annullering`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV)
        annuller(0)

        val result = reflect.toMap()

        assertEquals(inspektør.fagsystemId(0), result["fagsystemId"])
        assertEquals(Fagområde.SykepengerRefusjon.verdi, result["fagområde"])
        assertEquals(ANNULLERING_SENDT, result["tilstand"])

        val utbetalinger = result["utbetalinger"] as List<Map<String, Any?>>
        assertEquals(2, utbetalinger.size)

        assertNotNull(utbetalinger.first()["oppdrag"])
        assertNotNull(utbetalinger.first()["utbetalingstidslinje"])
        assertEquals(ANNULLERING, utbetalinger.first()["type"])
        assertNull(utbetalinger.first()["maksdato"])
        assertTrue(utbetalinger.first()["opprettet"] is LocalDateTime)
        val godkjentAv = utbetalinger.first()["godkjentAv"] as Map<String, Any>
        assertEquals(IDENT, godkjentAv["ident"])
        assertEquals(EPOST, godkjentAv["epost"])
        assertTrue(godkjentAv["tidsstempel"] is LocalDateTime)
        assertTrue(utbetalinger.first()["sendt"] is LocalDateTime)
        assertNull(utbetalinger.first()["avstemmingsnøkkel"])
        assertNull(utbetalinger.first()["overføringstidspunkt"])
        assertNull(utbetalinger.first()["avsluttet"])
    }

    @Test
    fun reconstruct() {
        val fagsystemIdId = "12345"
        val data = PersonData.FagsystemIdData(
            fagsystemId = fagsystemIdId,
            fagområde = Fagområde.SykepengerRefusjon.verdi,
            tilstand = AKTIV,
            utbetalinger = listOf(
                PersonData.FagsystemIdData.UtbetalingData(
                    PersonData.OppdragData(
                        mottaker = "orgnr",
                        fagområde = Fagområde.SykepengerRefusjon.verdi,
                        linjer = listOf(
                            PersonData.UtbetalingslinjeData(
                                1.januar,
                                31.januar,
                                1000,
                                1000,
                                100.0,
                                null,
                                1,
                                null,
                                "NY",
                                Klassekode.RefusjonIkkeOpplysningspliktig.verdi,
                                null
                            )
                        ),
                        fagsystemId = fagsystemIdId,
                        endringskode = "NY",
                        sisteArbeidsgiverdag = null,
                        nettoBeløp = 1000
                    ),
                    PersonData.UtbetalingstidslinjeData(
                        (1..16).map { UtbetalingsdagData(ArbeidsgiverperiodeDag, it.januar, 1000.0, 1000.0, null, 100.0, 100.0, 1000, 0, false) } +
                        (17..31).map { UtbetalingsdagData(NavDag, it.januar, 1000.0, 1000.0, null, 100.0, 100.0, 1000, 0, false) }
                    ),
                    type = UTBETALING,
                    maksdato = 28.desember,
                    opprettet = LocalDateTime.now(),
                    godkjentAv = PersonData.FagsystemIdData.UtbetalingData.GodkjentAvData("Z999999", "saksbehandler@nav.no", LocalDateTime.now()),
                    sendt = LocalDateTime.now(),
                    avstemmingsnøkkel = 12345L,
                    overføringstidspunkt = LocalDateTime.now(),
                    avsluttet = LocalDateTime.now()
                )
            ),
            forkastet = emptyList()
        )

        lateinit var fagsystemId: FagsystemId
        assertDoesNotThrow { fagsystemId = data.konverterTilFagsystemId() }

        val inspektør = FagsystemIdInspektør(listOf(fagsystemId))
        assertEquals(fagsystemIdId, inspektør.fagsystemId(0))
        assertEquals("Aktiv", inspektør.tilstand(0))
    }

}
