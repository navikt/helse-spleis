package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.UtbetalingOverført
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.økonomi.betal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingReflectTest {

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
    }

    private lateinit var map: MutableMap<String, Any?>

    @Test
    fun `Reflect mapper riktige verdier`() {
        map = UtbetalingReflect(
            Utbetaling.lagUtbetaling(
                emptyList(),
                UNG_PERSON_FNR_2018,
                ORGNUMMER,
                tidslinjeMedDagsats(tidslinjeOf(4.NAV)),
                4.januar,
                Aktivitetslogg(),
                LocalDate.MAX,
                100,
                148
            )
        ).toMap()
        assertUtbetalingslinjer(ORGNUMMER, "mottaker")
        assertUtbetalingslinjer("SPREF", "fagområde")
        assertUtbetalingslinjer("NY", "endringskode")
        assertUtbetalingslinje(0, 1.januar.toString(), "fom")
        assertUtbetalingslinje(0, 4.januar.toString(), "tom")
        assertUtbetalingslinje(0, 1, "delytelseId")
        assertUtbetalingslinje(0, null, "refDelytelseId")
        assertUtbetalingslinje(0, "NY", "endringskode")
        assertUtbetalingslinje(0, "SPREFAG-IOP", "klassekode")
    }

    @Test
    fun `Reflect mapper riktige verdierm med opphør`() {
        val tidligereUtbetaling = Utbetaling.lagUtbetaling(
            emptyList(),
            UNG_PERSON_FNR_2018,
            ORGNUMMER,
            tidslinjeMedDagsats(tidslinjeOf(4.NAV)),
            4.januar,
            Aktivitetslogg(),
            LocalDate.MAX,
            100,
            148
        ).also { utbetaling ->
            var utbetalingId: String = ""
            Utbetalingsgodkjenning(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "ignore",
                fødselsnummer = "ignore",
                organisasjonsnummer = "ignore",
                vedtaksperiodeId = "ignore",
                saksbehandler = "Z999999",
                saksbehandlerEpost = "mille.mellomleder@nav.no",
                utbetalingGodkjent = true,
                godkjenttidspunkt = LocalDateTime.now(),
                automatiskBehandling = false
            ).also {
                utbetaling.håndter(it)
                utbetaling.utbetal(it)
                utbetalingId = it.behov().first { it.type == Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling }.kontekst()["utbetalingId"] ?: throw IllegalStateException("Finner ikke utbetalingId i: ${it.behov().first { it.type == Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling }.kontekst()}")
            }
            utbetaling.håndter(
                UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "ignore",
                fødselsnummer = "ignore",
                orgnummer = "ignore",
                vedtaksperiodeId = "ignore",
                fagsystemId = utbetaling.arbeidsgiverOppdrag().fagsystemId(),
                utbetalingId = utbetalingId,
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            ))
            utbetaling.håndter(
                UtbetalingHendelse(
                    meldingsreferanseId = UUID.randomUUID(),
                    vedtaksperiodeId = "ignore",
                    aktørId = "ignore",
                    fødselsnummer = UNG_PERSON_FNR_2018,
                    orgnummer = ORGNUMMER,
                    fagsystemId = utbetaling.arbeidsgiverOppdrag().fagsystemId(),
                    utbetalingId = utbetalingId,
                    status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
                    melding = "hei"
                )
            )
        }

        map = UtbetalingReflect(
            Utbetaling.lagUtbetaling(
                listOf(tidligereUtbetaling),
                UNG_PERSON_FNR_2018,
                ORGNUMMER,
                tidslinjeMedDagsats(tidslinjeOf(2.NAV)),
                2.januar,
                Aktivitetslogg(),
                LocalDate.MAX,
                100,
                148
            )
        ).toMap()

        assertUtbetalingslinjer(ORGNUMMER, "mottaker")
        assertUtbetalingslinjer("SPREF", "fagområde")
        assertUtbetalingslinjer("ENDR", "endringskode")
        assertUtbetalingslinje(0, 1.januar.toString(), "fom")
        assertUtbetalingslinje(0, 4.januar.toString(), "tom")
        assertUtbetalingslinje(0, "OPPH", "statuskode")
        assertUtbetalingslinje(0, 1.januar.toString(), "datoStatusFom")
        assertUtbetalingslinje(0, 1, "delytelseId")
        assertUtbetalingslinje(0, null, "refDelytelseId")
        assertUtbetalingslinje(0, "ENDR", "endringskode")
        assertUtbetalingslinje(0, "SPREFAG-IOP", "klassekode")

        assertUtbetalingslinje(1, 1.januar.toString(), "fom")
        assertUtbetalingslinje(1, 2.januar.toString(), "tom")
        assertUtbetalingslinje(1, null, "statuskode")
        assertUtbetalingslinje(1, null, "datoStatusFom")
        assertUtbetalingslinje(1, 2, "delytelseId")
        assertUtbetalingslinje(1, 1, "refDelytelseId")
        assertUtbetalingslinje(1, "NY", "endringskode")
        assertUtbetalingslinje(1, "SPREFAG-IOP", "klassekode")
    }

    private fun assertUtbetalingslinje(index: Int, expected: Any?, key: String) {
        assertEquals(
            expected,
            map["arbeidsgiverOppdrag"].castAsMap<String, String>()
                ["linjer"].castAsList<Map<String, String>>()
                [index]
                [key]
        )
    }

    private fun assertUtbetalingslinjer(expected: Any?, key: String) {
        assertEquals(
            expected, map["arbeidsgiverOppdrag"].castAsMap<String, String>()[key]
        )
    }

    private fun tidslinjeMedDagsats(tidslinje: Utbetalingstidslinje) =
        tidslinje.onEach { if (it is NavDag) listOf(it.økonomi).betal(it.dato) }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> Any?.castAsList() = this as List<T>

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any, U : Any> Any?.castAsMap() = this as Map<T, U>
