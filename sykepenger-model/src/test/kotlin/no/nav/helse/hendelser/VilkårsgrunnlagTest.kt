package no.nav.helse.hendelser

import no.nav.helse.hendelser.Vilkårsgrunnlag.*
import no.nav.helse.person.*
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*

internal class VilkårsgrunnlagTest {
    private val aktivitetslogger = Aktivitetslogger()
    private val aktivitetslogg = Aktivitetslogg()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val aktørId = "123"
    private val fødselsnummer = "234"
    private val orgnummer = "345"
    private val person = Person(aktørId, fødselsnummer)
    private val arbeidsgiver = Arbeidsgiver(person, orgnummer)

    @Test
    internal fun `skal kunne beregne avvik mellom innmeldt lønn fra inntektsmelding og lønn fra inntektskomponenten`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2017, it), listOf(1000.0)) })

        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(1000.00))
        assertTrue(vilkårsgrunnlag.harAvvikIOppgittInntekt(1250.01))
        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(1250.00))
        assertTrue(vilkårsgrunnlag.harAvvikIOppgittInntekt(749.99))
        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(750.00))

    }

    @Test
    internal fun `samme inntekt fra inntektskomponenten og inntektsmelding lagres i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2017, it), listOf(1000.0)) })

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        assertEquals(0.0, dataForVilkårsvurdering(vedtaksperiode)?.avviksprosent)
        assertEquals(12000.0, dataForVilkårsvurdering(vedtaksperiode)?.beregnetÅrsinntektFraInntektskomponenten)
    }

    @Test
    internal fun `verdiene fra vurderingen blir lagret i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2017, it), listOf(1250.0)) })

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(0.20, dataForVilkårsvurdering(vedtaksperiode)?.avviksprosent)
        assertEquals(15000.00, dataForVilkårsvurdering(vedtaksperiode)?.beregnetÅrsinntektFraInntektskomponenten)
        assertEquals(false, dataForVilkårsvurdering(vedtaksperiode)?.erEgenAnsatt)
        assertEquals(28, dataForVilkårsvurdering(vedtaksperiode)!!.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering(vedtaksperiode)?.harOpptjening)
    }

    @Test
    internal fun `27 dager opptjening fører til manuell saksbehandling`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            (1..12).map { Måned(YearMonth.of(2017, it), listOf(1250.0)) },
            listOf(Arbeidsforhold("orgnummer", 5.desember(2017)))
        )

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(27, dataForVilkårsvurdering(vedtaksperiode)?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering(vedtaksperiode)?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand(vedtaksperiode)?.type)

    }
    @Test
    internal fun `28 dager opptjening fører til OK opptjening`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            (1..12).map { Måned(YearMonth.of(2017, it), listOf(1250.0)) },
            listOf(Arbeidsforhold("orgnummer", 4.desember(2017)))
        )

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(28, dataForVilkårsvurdering(vedtaksperiode)?.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering(vedtaksperiode)?.harOpptjening)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand(vedtaksperiode)?.type)
    }

    @Test
    internal fun `arbeidsforhold kun for andre orgnr gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            (1..12).map { Måned(YearMonth.of(2017, it), listOf(1250.0)) },
            listOf(Arbeidsforhold("eitAnnaOrgNummer", 4.desember(2017)))
        )
        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(0, dataForVilkårsvurdering(vedtaksperiode)?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering(vedtaksperiode)?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand(vedtaksperiode)?.type)
    }

    @Test
    internal fun `ingen arbeidsforhold gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            (1..12).map { Måned(YearMonth.of(2017, it), listOf(1250.0)) },
            emptyList()
        )
        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(0, dataForVilkårsvurdering(vedtaksperiode)?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering(vedtaksperiode)?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand(vedtaksperiode)?.type)
    }

    private fun dataForVilkårsvurdering(vedtaksperiode: Vedtaksperiode): Grunnlagsdata? {
        var _dataForVilkårsvurdering: Grunnlagsdata? = null
        vedtaksperiode.accept(object : VedtaksperiodeVisitor {
            override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Grunnlagsdata?) {
                _dataForVilkårsvurdering = dataForVilkårsvurdering
            }
        })
        return _dataForVilkårsvurdering
    }

    private fun hentTilstand(vedtaksperiode: Vedtaksperiode): Vedtaksperiodetilstand? {
        var _tilstand: Vedtaksperiodetilstand? = null
        vedtaksperiode.accept(object : VedtaksperiodeVisitor {
            override fun visitTilstand(tilstand: Vedtaksperiodetilstand) {
                _tilstand = tilstand
            }
        })
        return _tilstand
    }

    private fun vilkårsgrunnlag(
        inntektsmåneder: List<Måned>,
        arbeidsforhold: List<Arbeidsforhold> = listOf(
            Arbeidsforhold(
                "orgnummer",
                4.desember(2017)
            )
        )
    ) = Vilkårsgrunnlag(
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        aktørId = "987654321",
        fødselsnummer = "12345678901",
        orgnummer = "orgnummer",
        inntektsmåneder = inntektsmåneder,
        arbeidsforhold = MangeArbeidsforhold(arbeidsforhold),
        erEgenAnsatt = false,
        aktivitetslogger = aktivitetslogger,
        aktivitetslogg = aktivitetslogg
    )

    private fun vedtaksperiode() =
        Vedtaksperiode(
            person = person,
            arbeidsgiver = arbeidsgiver,
            id = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer
        ).also {
            it.håndter(nySøknad())
            it.håndter(sendtSøknad())
            it.håndter(inntektsmelding())
        }

    private fun nySøknad() = NySøknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = listOf(Triple(16.januar, 30.januar, 100)),
        aktivitetslogger = aktivitetslogger,
        aktivitetslogg = aktivitetslogg
    )

    private fun sendtSøknad() = SendtSøknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        perioder = listOf(SendtSøknad.Periode.Sykdom(16.januar, 30.januar, 100)),
        aktivitetslogger = aktivitetslogger,
        aktivitetslogg = aktivitetslogg,
        harAndreInntektskilder = false
    )

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(16.januar, 1000.0),
            orgnummer = orgnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 1000.0,
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            ferieperioder = listOf()
        )
}
