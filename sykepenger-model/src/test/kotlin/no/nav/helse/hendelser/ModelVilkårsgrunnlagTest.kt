package no.nav.helse.hendelser

import no.nav.helse.hendelser.ModelVilkårsgrunnlag.Inntekt
import no.nav.helse.hendelser.ModelVilkårsgrunnlag.Måned
import no.nav.helse.person.*
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class ModelVilkårsgrunnlagTest {
    private val aktivitetslogger = Aktivitetslogger()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val aktørId = "123"
    private val fødselsnummer = "234"
    private val orgnummer = "345"
    private val arbeidsgiver = Arbeidsgiver(orgnummer)
    private val person = Person(aktørId, fødselsnummer)

    @Test
    internal fun `skal kunne beregne avvik mellom innmeldt lønn fra inntektsmelding og lønn fra inntektskomponenten`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2017, it), listOf(Inntekt(1000.0))) })

        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(1000.00))
        assertTrue(vilkårsgrunnlag.harAvvikIOppgittInntekt(1250.01))
        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(1250.00))
        assertTrue(vilkårsgrunnlag.harAvvikIOppgittInntekt(749.99))
        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(750.00))

    }

    @Test
    internal fun `samme inntekt fra inntektskomponenten og inntektsmelding lagres i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2017, it), listOf(Inntekt(1000.0))) })

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        assertEquals(0.0, dataForVilkårsvurdering(vedtaksperiode)?.avviksprosent)
        assertEquals(12000.0, dataForVilkårsvurdering(vedtaksperiode)?.beregnetÅrsinntektFraInntektskomponenten)
    }

    @Test
    internal fun `verdiene fra vurderingen blir lagret i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2017, it), listOf(Inntekt(1250.0))) })

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(vilkårsgrunnlag)

        println(aktivitetslogger)

        assertEquals(0.20, dataForVilkårsvurdering(vedtaksperiode)?.avviksprosent)
        assertEquals(15000.00, dataForVilkårsvurdering(vedtaksperiode)?.beregnetÅrsinntektFraInntektskomponenten)
        assertEquals(false, dataForVilkårsvurdering(vedtaksperiode)?.erEgenAnsatt)
    }

    private fun dataForVilkårsvurdering(vedtaksperiode: Vedtaksperiode): ModelVilkårsgrunnlag.Grunnlagsdata? {
        var _dataForVilkårsvurdering: ModelVilkårsgrunnlag.Grunnlagsdata? = null
        vedtaksperiode.accept(object : VedtaksperiodeVisitor {
            override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: ModelVilkårsgrunnlag.Grunnlagsdata?) {
                _dataForVilkårsvurdering = dataForVilkårsvurdering
            }
        })
        return _dataForVilkårsvurdering
    }


    private fun vilkårsgrunnlag(inntektsmåneder: List<Måned>) = ModelVilkårsgrunnlag(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        aktørId = "987654321",
        fødselsnummer = "12345678901",
        orgnummer = "orgnummer",
        rapportertDato = LocalDateTime.now(),
        inntektsmåneder = inntektsmåneder,
        erEgenAnsatt = false,
        aktivitetslogger = aktivitetslogger
    )

    private fun vedtaksperiode() =
        Vedtaksperiode(
            id = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer
        ).also {
            it.håndter(nySøknad())
            it.håndter(sendtSøknad(), arbeidsgiver, person)
            it.håndter(inntektsmelding())
        }

    private fun nySøknad() = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        rapportertdato = LocalDateTime.now(),
        sykeperioder = listOf(Triple(16.januar, 30.januar, 100)),
        aktivitetslogger = aktivitetslogger
    )

    private fun sendtSøknad() = ModelSendtSøknad(
        hendelseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sendtNav = LocalDateTime.now(),
        perioder = listOf(ModelSendtSøknad.Periode.Sykdom(16.januar, 30.januar, 100)),
        aktivitetslogger = aktivitetslogger
    )

    private fun inntektsmelding() =
        ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(16.januar, 1000.0),
            orgnummer = orgnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            mottattDato = 20.januar.atTime(12, 30),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 1000.0,
            aktivitetslogger = aktivitetslogger,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            ferieperioder = listOf()
        )
}
