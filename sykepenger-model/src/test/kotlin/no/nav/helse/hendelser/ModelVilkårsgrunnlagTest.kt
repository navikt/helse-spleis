package no.nav.helse.hendelser

import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelVilkårsgrunnlag.Inntekt
import no.nav.helse.hendelser.ModelVilkårsgrunnlag.Måned
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.toJson
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.*
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class ModelVilkårsgrunnlagTest {
    private val aktivitetslogger = Aktivitetslogger()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val aktørId = "123"
    private val fødselsnummer = "234"
    private val orgnummer = "345"
    private val sendtSøknad = ModelSendtSøknad(
        UUID.randomUUID(),
        fødselsnummer,
        aktørId,
        orgnummer,
        LocalDateTime.now(),
        listOf(ModelSendtSøknad.Periode.Sykdom(10.januar, 12.januar, 100)),
        aktivitetslogger,
        SykepengesoknadDTO(
            id = "123",
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = SoknadsstatusDTO.SENDT,
            aktorId = aktørId,
            fnr = fødselsnummer,
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(
                "Hello world",
                orgnummer
            ),
            fom = 10.januar,
            tom = 12.januar,
            opprettet = LocalDateTime.now(),
            sendtNav = LocalDateTime.now(),
            egenmeldinger = emptyList(),
            soknadsperioder = listOf(
                SoknadsperiodeDTO(10.januar, 12.januar, 100)
            ),
            fravar = emptyList()
        ).toJsonNode().toString()
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
        vedtaksperiode.håndter(inntektsmelding())
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
            sykdomstidslinje = ConcreteSykdomstidslinje.sykedager(10.januar, 12.januar, sendtSøknad),
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
            arbeidsgiverperioder = listOf(8.januar..10.januar),
            ferieperioder = listOf(),
            originalJson = Inntektsmelding(
                inntektsmeldingId = "",
                arbeidstakerFnr = "fødselsnummer",
                arbeidstakerAktorId = "aktørId",
                virksomhetsnummer = "virksomhetsnummer",
                arbeidsgiverFnr = null,
                arbeidsgiverAktorId = null,
                arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
                arbeidsforholdId = null,
                beregnetInntekt = BigDecimal.valueOf(1000),
                refusjon = Refusjon(beloepPrMnd = BigDecimal.valueOf(1000), opphoersdato = LocalDate.now()),
                endringIRefusjoner = listOf(
                    EndringIRefusjon(
                        endringsdato = LocalDate.now(),
                        beloep = BigDecimal.valueOf(1000)
                    )
                ),
                opphoerAvNaturalytelser = emptyList(),
                gjenopptakelseNaturalytelser = emptyList(),
                arbeidsgiverperioder = listOf(Periode(fom = 8.januar, tom = 10.januar)),
                status = Status.GYLDIG,
                arkivreferanse = "",
                ferieperioder = emptyList(),
                foersteFravaersdag = LocalDate.now(),
                mottattDato = LocalDateTime.now()
            ).toJson()
        )
}
