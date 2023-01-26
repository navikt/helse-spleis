package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.etterspurtBehov
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Dødsinfo
import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagHendelseTest : AbstractPersonTest() {
    private lateinit var hendelse: ArbeidstakerHendelse

    @Test
    fun `ingen inntekt`() {
        håndterVilkårsgrunnlag(inntekter = emptyList(), arbeidsforhold = ansattSidenStart2017())
        assertTrue(person.aktivitetslogg.harFunksjonelleFeilEllerVerre())
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
    fun `ikke egen ansatt og ingen avvik i inntekt`() {
        val månedslønn = 1000.0.månedlig
        håndterVilkårsgrunnlag(
            beregnetInntekt = månedslønn,
            inntekter = tolvMånederMedInntekt(månedslønn),
            arbeidsforhold = ansattSidenStart2017(),
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `benytter forrige måned som utgangspunkt for inntektsberegning`() {
        person.håndter(sykmelding(perioder = listOf(Sykmeldingsperiode(8.januar, 31.januar, 100.prosent))))
        person.håndter(søknad(perioder = listOf(Sykdom(8.januar, 31.januar, 100.prosent))))
        person.håndter(
            inntektsmelding(
                beregnetInntekt = 30000.månedlig,
                arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar))
            )
        )
        val inntektsberegningStart =
            hendelse.etterspurtBehov<String>(
                1.vedtaksperiode.id(ORGNUMMER),
                Behovtype.InntekterForSammenligningsgrunnlag,
                "beregningStart"
            )
        val inntektsberegningSlutt =
            hendelse.etterspurtBehov<String>(
                1.vedtaksperiode.id(ORGNUMMER),
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
        listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 1.januar(2017)))


    private fun tolvMånederMedInntekt(beregnetInntekt: Inntekt) = inntektperioderForSammenligningsgrunnlag {
        1.januar(2017) til 1.desember(2017) inntekter {
            ORGNUMMER inntekt beregnetInntekt
        }
    }

    private fun håndterVilkårsgrunnlag(
        beregnetInntekt: Inntekt = 1000.månedlig,
        inntekter: List<ArbeidsgiverInntekt>,
        inntekterForSykepengegrunnlag: List<ArbeidsgiverInntekt> = emptyList(),
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>
    ) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding(beregnetInntekt = beregnetInntekt))
        person.håndter(ytelser())
        person.håndter(vilkårsgrunnlag(inntekter = inntekter, inntekterForSykepengegrunnlag = inntekterForSykepengegrunnlag, arbeidsforhold = arbeidsforhold))
        person.håndter(ytelser())
    }

    private fun sykmelding(
        perioder: List<Sykmeldingsperiode> = listOf(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
    ) = a1Hendelsefabrikk.lagSykmelding(
        sykeperioder = perioder.toTypedArray(),
        sykmeldingSkrevet = Sykmeldingsperiode.periode(perioder)?.start?.atStartOfDay() ?: LocalDateTime.now()
    ).apply {
        hendelse = this
    }

    private fun søknad(
        perioder: List<Søknadsperiode> = listOf(Sykdom(1.januar, 31.januar, 100.prosent))
    ) = a1Hendelsefabrikk.lagSøknad(
        perioder = perioder.toTypedArray(),
        sendtTilNAVEllerArbeidsgiver = 31.januar
    ).apply {
        hendelse = this
    }

    private fun inntektsmelding(
        beregnetInntekt: Inntekt,
        arbeidsgiverperioder: List<Periode> = listOf(Periode(1.januar, 16.januar))
    ) = a1Hendelsefabrikk.lagInntektsmelding(
            refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag(
        inntekter: List<ArbeidsgiverInntekt>,
        inntekterForSykepengegrunnlag: List<ArbeidsgiverInntekt>,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>
    ) =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode.id(ORGNUMMER)}",
            aktørId = "aktørId",
            personidentifikator = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            inntektsvurdering = Inntektsvurdering(inntekter),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekterForSykepengegrunnlag, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold
        ).apply {
            hendelse = this
        }

    private fun ytelser() = Ytelser(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018.toString(),
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = "${1.vedtaksperiode.id(ORGNUMMER)}",
        infotrygdhistorikk = null,
        foreldrepermisjon = Foreldrepermisjon(null, null),
        pleiepenger = Pleiepenger(emptyList()),
        omsorgspenger = Omsorgspenger(emptyList()),
        opplæringspenger = Opplæringspenger(emptyList()),
        institusjonsopphold = Institusjonsopphold(emptyList()),
        dødsinfo = Dødsinfo(null),
        arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
        dagpenger = Dagpenger(emptyList()),
        aktivitetslogg = Aktivitetslogg()
    ).apply {
        hendelse = this
    }
}
