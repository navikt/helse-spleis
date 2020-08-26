package no.nav.helse.person

import no.nav.helse.etterspurtBehov
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsvurdering.ArbeidsgiverInntekt
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagHendelseTest {
    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNR = "12345"
    }

    private lateinit var person: Person
    private val inspektør get() = TestArbeidsgiverInspektør(person)
    private lateinit var hendelse: ArbeidstakerHendelse

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    fun `egen ansatt`() {
        håndterVilkårsgrunnlag(
            egenAnsatt = true,
            inntekter = tolvMånederMedInntekt(1000.0.månedlig),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `ingen inntekt`() {
        håndterVilkårsgrunnlag(egenAnsatt = false, inntekter = emptyList(), arbeidsforhold = ansattSidenStart2017())
        assertTrue(person.aktivitetslogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `avvik i inntekt`() {
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            inntekter = tolvMånederMedInntekt(799.månedlig),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `latterlig avvik i inntekt`() {
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            inntekter = tolvMånederMedInntekt(1.månedlig),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `ikke egen ansatt og ingen avvik i inntekt`() {
        val månedslønn = 1000.0.månedlig
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            beregnetInntekt = månedslønn,
            inntekter = tolvMånederMedInntekt(månedslønn),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_HISTORIKK, inspektør.sisteTilstand(0))
        val historikkFom = inspektør.sykdomstidslinje.førsteDag().minusYears(4)
        val historikkTom = inspektør.sykdomstidslinje.sisteDag()
        val vedtaksperiodeId = inspektør.vedtaksperiodeId(0)
        assertEquals(
            historikkFom.toString(),
            hendelse.etterspurtBehov(vedtaksperiodeId, Behovtype.Sykepengehistorikk, "historikkFom")
        )
        assertEquals(
            historikkTom.toString(),
            hendelse.etterspurtBehov(vedtaksperiodeId, Behovtype.Sykepengehistorikk, "historikkTom")
        )
    }

    @Test
    fun `benytter forrige måned som utgangspunkt for inntektsberegning`() {
        person.håndter(sykmelding(perioder = listOf(Sykmeldingsperiode(8.januar, 31.januar, 100))))
        person.håndter(søknad(perioder = listOf(Søknad.Søknadsperiode.Sykdom(8.januar, 31.januar, 100))))
        person.håndter(
            inntektsmelding(
                beregnetInntekt = 30000.månedlig,
                arbeidsgiverperioder = listOf(Periode(3.januar, 18.januar))
            )
        )
        val vedtaksperiodeId = inspektør.vedtaksperiodeId(0)

        val inntektsberegningStart =
            hendelse.etterspurtBehov<String>(
                vedtaksperiodeId,
                Behovtype.InntekterForSammenligningsgrunnlag,
                "beregningStart"
            )
        val inntektsberegningSlutt =
            hendelse.etterspurtBehov<String>(
                vedtaksperiodeId,
                Behovtype.InntekterForSammenligningsgrunnlag,
                "beregningSlutt"
            )
        assertEquals("2017-01", inntektsberegningStart)
        assertEquals("2017-12", inntektsberegningSlutt)
    }

    @Test
    fun `ikke egen ansatt og mer enn 25 % avvik i inntekt`() {
        val månedslønn = 1000.0.månedlig
        val `25 % mer` = månedslønn * 1.26
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            beregnetInntekt = `25 % mer`,
            inntekter = tolvMånederMedInntekt(månedslønn),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `ikke egen ansatt og mindre enn 25 % avvik i inntekt`() {
        val månedslønn = 1000.0.månedlig
        val `25 % mindre` = månedslønn * 0.74
        håndterVilkårsgrunnlag(
            egenAnsatt = false,
            beregnetInntekt = `25 % mindre`,
            inntekter = tolvMånederMedInntekt(månedslønn),
            arbeidsforhold = ansattSidenStart2017()
        )

        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    private fun ansattSidenStart2017() =
        listOf(Opptjeningvurdering.Arbeidsforhold(ORGNR, 1.januar(2017)))


    private fun tolvMånederMedInntekt(beregnetInntekt: Inntekt) = inntektperioder {
        1.januar(2017) til 1.desember(2017) inntekter {
            ORGNR inntekt beregnetInntekt
        }
    }

    private fun håndterVilkårsgrunnlag(
        egenAnsatt: Boolean,
        beregnetInntekt: Inntekt = 1000.månedlig,
        inntekter: List<ArbeidsgiverInntekt>,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold>
    ) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding(beregnetInntekt = beregnetInntekt))
        person.håndter(vilkårsgrunnlag(egenAnsatt = egenAnsatt, inntekter = inntekter, arbeidsforhold = arbeidsforhold))
    }

    private fun sykmelding(
        perioder: List<Sykmeldingsperiode> = listOf(Sykmeldingsperiode(1.januar, 31.januar, 100))
    ) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = "aktørId",
        orgnummer = ORGNR,
        sykeperioder = perioder,
        mottatt = perioder.map { it.fom }.min()?.atStartOfDay() ?: LocalDateTime.now()
    ).apply {
        hendelse = this
    }

    private fun søknad(
        perioder: List<Søknad.Søknadsperiode> = listOf(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100))
    ) = Søknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = "aktørId",
        orgnummer = ORGNR,
        perioder = perioder,
        harAndreInntektskilder = false,
        sendtTilNAV = 31.januar.atStartOfDay(),
        permittert = false
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
            orgnummer = ORGNR,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = 1.januar,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag(
        egenAnsatt: Boolean,
        inntekter: List<ArbeidsgiverInntekt>,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold>
    ) =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNR,
            inntektsvurdering = Inntektsvurdering(inntekter),
            erEgenAnsatt = egenAnsatt,
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            dagpenger = Dagpenger(emptyList()),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList())
        ).apply {
            hendelse = this
        }
}
