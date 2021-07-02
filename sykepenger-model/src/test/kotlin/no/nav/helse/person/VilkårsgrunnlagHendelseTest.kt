package no.nav.helse.person

import no.nav.helse.etterspurtBehov
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagHendelseTest : AbstractPersonTest() {
    private lateinit var hendelse: ArbeidstakerHendelse

    @Test
    fun `ingen inntekt`() {
        håndterVilkårsgrunnlag(inntekter = emptyList(), arbeidsforhold = ansattSidenStart2017())
        assertTrue(person.aktivitetslogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `avvik i inntekt`() {
        håndterVilkårsgrunnlag(
            inntekter = tolvMånederMedInntekt(799.månedlig),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `latterlig avvik i inntekt`() {
        håndterVilkårsgrunnlag(
            inntekter = tolvMånederMedInntekt(1.månedlig),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `9 måneder med inntekt gir riktig sammenligningsgrunnlag`() {
        håndterVilkårsgrunnlag(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.april(2017) inntekter {
                    ORGNUMMER inntekt 12000.månedlig
                }
                1.mai(2017) til 1.september(2017) inntekter {
                    ORGNUMMER inntekt 20000.månedlig
                }
            },
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(148000.årlig, (inspektør.vilkårsgrunnlag(1.vedtaksperiode) as VilkårsgrunnlagHistorikk.Grunnlagsdata?)?.sammenligningsgrunnlag)
    }

    @Test
    fun `ikke egen ansatt og ingen avvik i inntekt`() {
        val månedslønn = 1000.0.månedlig
        håndterVilkårsgrunnlag(
            beregnetInntekt = månedslønn,
            inntekter = tolvMånederMedInntekt(månedslønn),
            arbeidsforhold = ansattSidenStart2017(),
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_HISTORIKK, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `benytter forrige måned som utgangspunkt for inntektsberegning`() {
        person.håndter(sykmelding(perioder = listOf(Sykmeldingsperiode(8.januar, 31.januar, 100.prosent))))
        person.håndter(søknad(perioder = listOf(Søknad.Søknadsperiode.Sykdom(8.januar, 31.januar, 100.prosent))))
        person.håndter(
            inntektsmelding(
                beregnetInntekt = 30000.månedlig,
                arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar))
            )
        )
        person.håndter(utbetalingsgrunnlag())
        person.håndter(ytelser())
        println()
        val inntektsberegningStart =
            hendelse.etterspurtBehov<String>(
                1.vedtaksperiode,
                Behovtype.InntekterForSammenligningsgrunnlag,
                "beregningStart"
            )
        val inntektsberegningSlutt =
            hendelse.etterspurtBehov<String>(
                1.vedtaksperiode,
                Behovtype.InntekterForSammenligningsgrunnlag,
                "beregningSlutt"
            )
        assertEquals("2017-01", inntektsberegningStart)
        assertEquals("2017-12", inntektsberegningSlutt)
    }

    @Test
    fun `ikke egen ansatt og mer enn 25 prosent avvik i inntekt`() {
        val månedslønn = 1000.0.månedlig
        val `mer enn 25 prosent` = månedslønn * 1.26
        håndterVilkårsgrunnlag(
            beregnetInntekt = `mer enn 25 prosent`,
            inntekter = tolvMånederMedInntekt(månedslønn),
            arbeidsforhold = ansattSidenStart2017(),
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `ikke egen ansatt og mindre enn 25 prosent avvik i inntekt`() {
        val månedslønn = 1000.0.månedlig
        val `mindre enn 25 prosent` = månedslønn * 0.74
        håndterVilkårsgrunnlag(
            beregnetInntekt = `mindre enn 25 prosent`,
            inntekter = tolvMånederMedInntekt(månedslønn),
            arbeidsforhold = ansattSidenStart2017(),
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    private fun ansattSidenStart2017() =
        listOf(Arbeidsforhold(ORGNUMMER, 1.januar(2017)))


    private fun tolvMånederMedInntekt(beregnetInntekt: Inntekt) = inntektperioderForSammenligningsgrunnlag {
        1.januar(2017) til 1.desember(2017) inntekter {
            ORGNUMMER inntekt beregnetInntekt
        }
    }

    private fun håndterVilkårsgrunnlag(
        beregnetInntekt: Inntekt = 1000.månedlig,
        inntekter: List<ArbeidsgiverInntekt>,
        arbeidsforhold: List<Arbeidsforhold>
    ) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding(beregnetInntekt = beregnetInntekt))
        person.håndter(utbetalingsgrunnlag())
        person.håndter(ytelser())
        person.håndter(vilkårsgrunnlag(inntekter = inntekter, arbeidsforhold = arbeidsforhold))
    }

    private fun sykmelding(
        perioder: List<Sykmeldingsperiode> = listOf(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
    ) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = "aktørId",
        orgnummer = ORGNUMMER,
        sykeperioder = perioder,
        sykmeldingSkrevet = Sykmeldingsperiode.periode(perioder)?.start?.atStartOfDay() ?: LocalDateTime.now(),
        mottatt = Sykmeldingsperiode.periode(perioder.toList())!!.endInclusive.atStartOfDay()
    ).apply {
        hendelse = this
    }

    private fun søknad(
        perioder: List<Søknad.Søknadsperiode> = listOf(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
    ) = Søknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = "aktørId",
        orgnummer = ORGNUMMER,
        perioder = perioder,
        andreInntektskilder = emptyList(),
        sendtTilNAV = 31.januar.atStartOfDay(),
        permittert = false,
        merknaderFraSykmelding = emptyList(),
        sykmeldingSkrevet = LocalDateTime.now()
    ).apply {
        hendelse = this
    }

    private fun inntektsmelding(
        beregnetInntekt: Inntekt,
        arbeidsgiverperioder: List<Periode> = listOf(Periode(1.januar, 16.januar))
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, beregnetInntekt, emptyList()),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = 1.januar,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag(
        inntekter: List<ArbeidsgiverInntekt>,
        arbeidsforhold: List<Arbeidsforhold>
    ) =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            inntektsvurdering = Inntektsvurdering(inntekter),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold)
        ).apply {
            hendelse = this
        }

    private fun utbetalingsgrunnlag() = Utbetalingsgrunnlag(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        orgnummer = ORGNUMMER,
        vedtaksperiodeId = 1.vedtaksperiode,
        inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
        arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER    , 1.januar, null))
    ).apply {
        hendelse = this
    }

    private fun ytelser() = Ytelser(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = "${1.vedtaksperiode}",
        utbetalingshistorikk = Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = "${1.vedtaksperiode}",
            arbeidskategorikoder = emptyMap(),
            harStatslønn = false,
            perioder = emptyList(),
            inntektshistorikk = emptyList(),
            ugyldigePerioder = emptyList(),
            besvart = LocalDateTime.now()
        ),
        foreldrepermisjon = Foreldrepermisjon(null, null, Aktivitetslogg()),
        pleiepenger = Pleiepenger(emptyList(), Aktivitetslogg()),
        omsorgspenger = Omsorgspenger(emptyList(), Aktivitetslogg()),
        opplæringspenger = Opplæringspenger(emptyList(), Aktivitetslogg()),
        institusjonsopphold = Institusjonsopphold(emptyList(), Aktivitetslogg()),
        dødsinfo = Dødsinfo(null),
        arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
        dagpenger = Dagpenger(emptyList()),
        aktivitetslogg = Aktivitetslogg()
    ).apply {
        hendelse = this
    }
}
