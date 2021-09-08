package no.nav.helse.spleis.e2e

import no.nav.helse.spleis.TestMessageFactory.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.november
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Test

internal class HendelseYtelserMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Periode med overlappende pleiepenger blir sendt til Infotrygd`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(vedtaksperiodeIndeks = 0, pleiepenger = listOf(PleiepengerTestdata(3.januar, 26.januar, 100)))

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `Periode med overlappende omsorgspenger blir sendt til Infotrygd`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(
            vedtaksperiodeIndeks = 0,
            omsorgspenger = listOf(OmsorgspengerTestdata(3.januar, 26.januar, 100))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `Periode med overlappende opplæringspenger blir sendt til Infotrygd`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(vedtaksperiodeIndeks = 0, opplæringspenger = listOf(OpplæringspengerTestdata(3.januar, 26.januar, 100)))

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `Periode med overlappende institusjonsopphold blir sendt til Infotrygd`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(
            vedtaksperiodeIndeks = 0,
            institusjonsoppholdsperioder = listOf(
                InstitusjonsoppholdTestdata(
                    startdato = 3.januar,
                    faktiskSluttdato = null,
                    institusjonstype = "FO",
                    kategori = "S"
                )
            )
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `Arbeidskategorikode ulik 01 kaster perioden til Infotrygd`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(
            vedtaksperiodeIndeks = 0,
            sykepengehistorikk = listOf(UtbetalingshistorikkTestdata(
                fom = 1.januar,
                tom = 2.januar,
                arbeidskategorikode = "07",
                utbetalteSykeperioder = listOf(
                    UtbetalingshistorikkTestdata.UtbetaltSykeperiode(
                        fom = 1.januar,
                        tom = 2.januar,
                        dagsats = 1400.0,
                        typekode = "0",
                        utbetalingsgrad = "100",
                        organisasjonsnummer = ORGNUMMER
                    )
                ),
                inntektsopplysninger = listOf(
                    UtbetalingshistorikkTestdata.Inntektsopplysninger(
                        sykepengerFom = 1.november(2017),
                        inntekt = 36000.0,
                        organisasjonsnummer = ORGNUMMER,
                        refusjonTilArbeidsgiver = true
                    )
                )
            ))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "TIL_INFOTRYGD"
        )
    }


    @Test
    fun `Arbeidskategorikode lik 01 passerer validering`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        val historikk = listOf(UtbetalingshistorikkTestdata(
            fom = 1.januar,
            tom = 26.januar,
            arbeidskategorikode = "01",
            utbetalteSykeperioder = listOf(
                UtbetalingshistorikkTestdata.UtbetaltSykeperiode(
                    fom = 1.januar,
                    tom = 2.januar,
                    dagsats = 1400.0,
                    typekode = "0",
                    utbetalingsgrad = "100",
                    organisasjonsnummer = ORGNUMMER
                ),
            ),
            inntektsopplysninger = listOf(
                UtbetalingshistorikkTestdata.Inntektsopplysninger(
                    sykepengerFom = 1.januar,
                    inntekt = 36000.0,
                    organisasjonsnummer = ORGNUMMER,
                    refusjonTilArbeidsgiver = true
                )
            )
        ))
        sendYtelser(vedtaksperiodeIndeks = 0, sykepengehistorikk = historikk)

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING"
        )
    }


    @Test
    fun `Passerer validering når utbetalte sykeperioder er tom`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        val historikk = listOf(UtbetalingshistorikkTestdata(
            fom = 1.januar,
            tom = 26.januar,
            arbeidskategorikode = "07",
            utbetalteSykeperioder = emptyList(),
            inntektsopplysninger = emptyList()
        ))
        sendYtelser(vedtaksperiodeIndeks = 0, sykepengehistorikk = historikk)
        sendVilkårsgrunnlag(0)
        sendYtelser(vedtaksperiodeIndeks = 0, sykepengehistorikk = historikk)

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING"
        )
    }
}


