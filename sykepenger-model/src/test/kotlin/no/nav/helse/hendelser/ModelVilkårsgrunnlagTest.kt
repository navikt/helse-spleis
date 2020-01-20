package no.nav.helse.hendelser

import no.nav.helse.TestConstants
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelVilkårsgrunnlag.Inntekt
import no.nav.helse.hendelser.ModelVilkårsgrunnlag.Måned
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class ModelVilkårsgrunnlagTest {
    private val aktivitetslogger = Aktivitetslogger()
    val vedtaksperiodeId = UUID.randomUUID()
    val aktørId = "123"
    val fødselsnummer = "234"
    val orgnummer = "345"
    private val gammelInntektsmelding = TestConstants.inntektsmeldingHendelse(
        beregnetInntekt = 1000.toBigDecimal(),
        førsteFraværsdag = 10.januar,
        arbeidsgiverperioder = listOf(Periode(fom = 8.januar, tom = 10.januar))
    )
    private val gammelSendtSøknad = TestConstants.sendtSøknadHendelse(
        søknadsperioder = listOf(SoknadsperiodeDTO(fom = 10.januar, tom = 12.januar))
    )

    @Test
    internal fun `skal kunne beregne avvik mellom innmeldt lønn fra inntektsmelding og lønn fra inntektskomponenten`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2018, it), listOf(Inntekt(1000.0))) })

        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(1000.00))
        assertTrue(vilkårsgrunnlag.harAvvikIOppgittInntekt(1250.01))
        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(1250.00))
        assertTrue(vilkårsgrunnlag.harAvvikIOppgittInntekt(749.99))
        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(750.00))

    }

    @Test
    internal fun `samme inntekt fra inntektskomponenten og inntektsmelding lagres i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2018, it), listOf(Inntekt(1000.0))) })

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(inntektsmelding())
        vedtaksperiode.håndter(vilkårsgrunnlag)

        assertEquals(0.0, vedtaksperiode.dataForVilkårsvurdering()?.avviksprosent)
        assertEquals(12000.0, vedtaksperiode.dataForVilkårsvurdering()?.beregnetÅrsinntektFraInntektskomponenten)
    }

    @Test
    internal fun `lagring og restoring av memento fører til samme grunnlagsdata for vilkårsgrunnlag`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2018, it), listOf(Inntekt(1000.0))) })

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(gammelInntektsmelding)
        vedtaksperiode.håndter(vilkårsgrunnlag)

        val beforeRestore = vedtaksperiode.dataForVilkårsvurdering()
        assertNotNull(beforeRestore)

        val memento = vedtaksperiode.memento()

        assertEquals(beforeRestore, Vedtaksperiode.restore(memento).dataForVilkårsvurdering())
    }

    @Test
    internal fun `verdiene fra vurderingen blir lagret i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2018, it), listOf(Inntekt(1250.0))) })

        val vedtaksperiode = vedtaksperiode()
        vedtaksperiode.håndter(inntektsmelding())
        vedtaksperiode.håndter(vilkårsgrunnlag)

        assertEquals(0.20, vedtaksperiode.dataForVilkårsvurdering()?.avviksprosent)
        assertEquals(15000.00, vedtaksperiode.dataForVilkårsvurdering()?.beregnetÅrsinntektFraInntektskomponenten)
        assertEquals(false, vedtaksperiode.dataForVilkårsvurdering()?.erEgenAnsatt)
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
        aktivitetslogger = Aktivitetslogger()
    )

    private fun vedtaksperiode() =
        Vedtaksperiode(
            id = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer,
            sykdomstidslinje = ConcreteSykdomstidslinje.sykedager(10.januar, 12.januar, gammelSendtSøknad),
            tilstand = Vedtaksperiode.MottattSendtSøknad
        )

    private fun inntektsmelding() =
        ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(LocalDate.now(), 1000.0, null),
            orgnummer = orgnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = 10.januar,
            beregnetInntekt = 1000.0,
            aktivitetslogger = aktivitetslogger,
            arbeidsgiverperioder = listOf(),
            ferieperioder = listOf()
        )
}
