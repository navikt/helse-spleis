package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.*
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class FeriepengeMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Beregner feriepenger korrekt for enkel spleisperiode med en utbetaling i infotrygd`() = Toggles.SendFeriepengeOppdrag.enable {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.juni(2020), tom = 30.juni(2020), sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.juni(2020), tom = 30.juni(2020), sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.juni(2020), tom = 16.juni(2020))), førsteFraværsdag = 1.juni(2020))
        sendYtelser(0)
        sendVilkårsgrunnlag(0, (1..12).map { YearMonth.of(2019, it).plusMonths(5) to INNTEKT })
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendUtbetalingshistorikkForFeriepenger(
            TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata(
                fom = 1.januar(2020),
                tom = 31.desember(2020),
                utbetalinger = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Utbetaling(
                        fom = 1.mars(2020),
                        tom = 31.mars(2020),
                        dagsats = 1431.0,
                        typekode = "5",
                        utbetalingsgrad = "100",
                        organisasjonsnummer = ORGNUMMER,
                        utbetalt = 4.april(2020)
                    )
                ),
                feriepengehistorikk = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Feriepenger(
                        orgnummer = ORGNUMMER,
                        beløp = 3211,
                        fom = 1.mai(2021),
                        tom = 31.mai(2021)
                    )
                ),
                arbeidskategorikoder = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Arbeidskategori(
                        kode = "01",
                        fom = 1.mars(2020),
                        tom = 31.mars(2020)
                    )
                )
            )
        )

        val behov = testRapid.inspektør.melding(testRapid.inspektør.antall() - 1)
        val linjer = behov.path("Utbetaling").path("linjer")

        assertTrue(testRapid.inspektør.behovtypeSisteMelding(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling))
        assertEquals(1, linjer.size())
        assertEquals("Utbetaling", behov.path("@behov")[0].asText())
        assertEquals(ORGNUMMER, behov.path("organisasjonsnummer").asText())
        assertEquals("SPREFAGFER-IOP", linjer[0].path("klassekode").asText())
        assertEquals("ENG", linjer[0].path("satstype").asText())
        assertEquals(1460, linjer[0].path("sats").asInt())
        assertEquals(1460, linjer[0].path("totalbeløp").asInt())
    }

    @Test
    fun `Ser bort fra perioder med arbeidskategori som ikke gir rett til feriepenger`() = Toggles.SendFeriepengeOppdrag.enable {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.juni(2020), tom = 30.juni(2020), sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.juni(2020), tom = 30.juni(2020), sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.juni(2020), tom = 16.juni(2020))), førsteFraværsdag = 1.juni(2020))
        sendYtelser(0)
        sendVilkårsgrunnlag(0, (1..12).map { YearMonth.of(2019, it).plusMonths(5) to INNTEKT })
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendUtbetalingshistorikkForFeriepenger(
            TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata(
                fom = 1.januar(2020),
                tom = 31.desember(2020),
                utbetalinger = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Utbetaling(
                        fom = 1.januar(2020),
                        tom = 31.januar(2020),
                        utbetalt = 31.januar(2020),
                        dagsats = 1431.0,
                        typekode = "5",
                        utbetalingsgrad = "100",
                        organisasjonsnummer = ORGNUMMER
                    ),
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Utbetaling(
                        fom = 1.mars(2020),
                        tom = 31.mars(2020),
                        utbetalt = 31.mars(2020),
                        dagsats = 1431.0,
                        typekode = "5",
                        utbetalingsgrad = "100",
                        organisasjonsnummer = ORGNUMMER
                    )
                ),
                feriepengehistorikk = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Feriepenger(
                        orgnummer = ORGNUMMER,
                        beløp = 3211,
                        fom = 1.mai(2021),
                        tom = 31.mai(2021)
                    )
                ),
                arbeidskategorikoder = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Arbeidskategori(
                        kode = "07",
                        fom = 1.januar(2020),
                        tom = 31.januar(2020)
                    ),
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Arbeidskategori(
                        kode = "01",
                        fom = 1.mars(2020),
                        tom = 31.mars(2020)
                    )
                )
            )
        )

        val behov = testRapid.inspektør.melding(testRapid.inspektør.antall() - 1)
        val linjer = behov.path("Utbetaling").path("linjer")

        assertTrue(testRapid.inspektør.behovtypeSisteMelding(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling))
        assertEquals(1, linjer.size())
        assertEquals("Utbetaling", behov.path("@behov")[0].asText())
        assertEquals(ORGNUMMER, behov.path("organisasjonsnummer").asText())
        assertEquals("SPREFAGFER-IOP", linjer[0].path("klassekode").asText())
        assertEquals("ENG", linjer[0].path("satstype").asText())
        assertEquals(1460, linjer[0].path("sats").asInt())
        assertEquals(1460, linjer[0].path("totalbeløp").asInt())
    }

    @Test
    fun `Ukjent arbeidskategorikode tolkes som tom`() = Toggles.SendFeriepengeOppdrag.enable {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.juni(2020), tom = 30.juni(2020), sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.juni(2020), tom = 30.juni(2020), sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.juni(2020), tom = 16.juni(2020))), førsteFraværsdag = 1.juni(2020))
        sendYtelser(0)
        sendVilkårsgrunnlag(0, (1..12).map { YearMonth.of(2019, it).plusMonths(5) to INNTEKT })
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendUtbetalingshistorikkForFeriepenger(
            TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata(
                fom = 1.januar(2020),
                tom = 31.desember(2020),
                utbetalinger = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Utbetaling(
                        fom = 1.januar(2020),
                        tom = 31.januar(2020),
                        utbetalt = 31.januar(2020),
                        dagsats = 1431.0,
                        typekode = "5",
                        utbetalingsgrad = "100",
                        organisasjonsnummer = ORGNUMMER
                    ),
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Utbetaling(
                        fom = 1.mars(2020),
                        tom = 31.mars(2020),
                        utbetalt = 31.mars(2020),
                        dagsats = 1431.0,
                        typekode = "5",
                        utbetalingsgrad = "100",
                        organisasjonsnummer = ORGNUMMER
                    )
                ),
                feriepengehistorikk = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Feriepenger(
                        orgnummer = ORGNUMMER,
                        beløp = 3211,
                        fom = 1.mai(2021),
                        tom = 31.mai(2021)
                    )
                ),
                arbeidskategorikoder = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Arbeidskategori(
                        kode = "30",
                        fom = 1.januar(2020),
                        tom = 31.januar(2020)
                    ),
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Arbeidskategori(
                        kode = "01",
                        fom = 1.mars(2020),
                        tom = 31.mars(2020)
                    )
                )
            )
        )

        val behov = testRapid.inspektør.melding(testRapid.inspektør.antall() - 1)
        val linjer = behov.path("Utbetaling").path("linjer")

        assertTrue(testRapid.inspektør.behovtypeSisteMelding(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling))
        assertEquals(1, linjer.size())
        assertEquals("Utbetaling", behov.path("@behov")[0].asText())
        assertEquals(ORGNUMMER, behov.path("organisasjonsnummer").asText())
        assertEquals("SPREFAGFER-IOP", linjer[0].path("klassekode").asText())
        assertEquals("ENG", linjer[0].path("satstype").asText())
        assertEquals(1460, linjer[0].path("sats").asInt())
        assertEquals(1460, linjer[0].path("totalbeløp").asInt())
    }

    @Test
    fun `Sjekker at orgnummer på utbetalingsbehov blir riktig med flere arbeidsgivere`() = Toggles.SendFeriepengeOppdrag.enable {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.juni(2020), tom = 30.juni(2020), sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.juni(2020), tom = 30.juni(2020), sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.juni(2020), tom = 16.juni(2020))), førsteFraværsdag = 1.juni(2020))
        sendYtelser(0)
        sendVilkårsgrunnlag(0, (1..12).map { YearMonth.of(2019, it).plusMonths(5) to INNTEKT })
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendUtbetalingshistorikkForFeriepenger(
            TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata(
                fom = 1.januar(2020),
                tom = 31.desember(2020),
                utbetalinger = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Utbetaling(
                        fom = 1.mars(2020),
                        tom = 31.mars(2020),
                        utbetalt = 31.mars(2020),
                        dagsats = 1431.0,
                        typekode = "5",
                        utbetalingsgrad = "100",
                        organisasjonsnummer = ORGNUMMER
                    ),
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Utbetaling(
                        fom = 1.september(2020),
                        tom = 30.september(2020),
                        utbetalt = 30.september(2020),
                        dagsats = 546.0,
                        typekode = "5",
                        utbetalingsgrad = "100",
                        organisasjonsnummer = "321654987"
                    )
                ),
                feriepengehistorikk = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Feriepenger(
                        orgnummer = ORGNUMMER,
                        beløp = 3211,
                        fom = 1.mai(2021),
                        tom = 31.mai(2021)
                    ),
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Feriepenger(
                        orgnummer = "321654987",
                        beløp = 1225,
                        fom = 1.mai(2021),
                        tom = 31.mai(2021)
                    )
                ),
                arbeidskategorikoder = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Arbeidskategori(
                        kode = "01",
                        fom = 1.mars(2020),
                        tom = 30.september(2020)
                    )
                )
            )
        )

        val behov = testRapid.inspektør.melding(testRapid.inspektør.antall() - 2)
        val linjer = behov.path("Utbetaling").path("linjer")

        assertTrue(testRapid.inspektør.behovtypeSisteMelding(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling))
        assertEquals(1, linjer.size())
        assertEquals("Utbetaling", behov.path("@behov")[0].asText())
        assertEquals(ORGNUMMER, behov.path("organisasjonsnummer").asText())
        assertEquals("SPREFAGFER-IOP", linjer[0].path("klassekode").asText())
        assertEquals("ENG", linjer[0].path("satstype").asText())
        assertEquals(1460, linjer[0].path("sats").asInt())
        assertEquals(1460, linjer[0].path("totalbeløp").asInt())

        val behov2 = testRapid.inspektør.melding(testRapid.inspektør.antall() - 1)
        val linjer2 = behov2.path("Utbetaling").path("linjer")

        assertTrue(testRapid.inspektør.behovtypeSisteMelding(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling))
        assertEquals(1, linjer2.size())
        assertEquals("Utbetaling", behov2.path("@behov")[0].asText())
        assertEquals("321654987", behov2.path("organisasjonsnummer").asText())
        assertEquals("SPREFAGFER-IOP", linjer2[0].path("klassekode").asText())
        assertEquals("ENG", linjer2[0].path("satstype").asText())
        assertEquals(-334, linjer2[0].path("sats").asInt())
        assertEquals(-334, linjer2[0].path("totalbeløp").asInt())
    }
}
