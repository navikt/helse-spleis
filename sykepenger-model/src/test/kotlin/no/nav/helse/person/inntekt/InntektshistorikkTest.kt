package no.nav.helse.person.inntekt

import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.EmptyLog
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.InntektshistorikkInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class InntektshistorikkTest {
    private lateinit var historikk: Inntektshistorikk
    private val inspektør get() = InntektshistorikkInspektør(historikk.view())

    private companion object {
        const val ORGNUMMER = "987654321"
        val INNTEKT = 31000.00.månedlig
        val hendelsefabrikk =
            ArbeidsgiverHendelsefabrikk(
                organisasjonsnummer = ORGNUMMER,
            )
    }

    @BeforeEach
    fun setup() {
        resetSeed()
        historikk = Inntektshistorikk()
    }

    @Test
    fun `Inntekt fra inntektsmelding brukes til å beregne sykepengegrunnlaget`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, EmptyLog)
        assertEquals(1, inspektør.size)
        assertEquals(
            INNTEKT,
            historikk
                .avklarSykepengegrunnlag(
                    1.januar,
                    1.januar,
                    null,
                )?.inspektør
                ?.beløp,
        )
    }

    @Test
    fun `Inntekt fra andre inntektsmelding overskriver inntekt fra første, gitt samme første fraværsdag`() {
        inntektsmelding(førsteFraværsdag = 1.januar, beregnetInntekt = 30000.månedlig).addInntekt(historikk, EmptyLog)
        inntektsmelding(førsteFraværsdag = 1.januar, beregnetInntekt = 29000.månedlig).addInntekt(historikk, EmptyLog)
        resetSeed(1.februar)
        inntektsmelding(førsteFraværsdag = 1.februar, beregnetInntekt = 31000.månedlig).addInntekt(historikk, EmptyLog)
        assertEquals(29000.månedlig, historikk.avklarSykepengegrunnlag(1.januar, 1.januar, null)?.inspektør?.beløp)
        assertEquals(31000.månedlig, historikk.avklarSykepengegrunnlag(1.februar, 1.februar, null)?.inspektør?.beløp)
    }

    @Test
    fun `Inntekt fra inntektsmelding brukes ikke til å beregne sykepengegrunnlaget på annen dato`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, EmptyLog)
        assertEquals(1, inspektør.size)
        assertNull(historikk.avklarSykepengegrunnlag(2.januar, 2.januar, null))
    }

    @Test
    fun `Inntekt for annen dato og samme kilde erstatter ikke eksisterende`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, EmptyLog)
        inntektsmelding(førsteFraværsdag = 2.januar, arbeidsgiverperioder = listOf(2.januar til 17.januar)).addInntekt(
            historikk,
            EmptyLog,
        )
        assertEquals(2, inspektør.size)
    }

    private fun inntektsmelding(
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        arbeidsgiverperioder: List<Periode> = listOf(1.januar til 16.januar),
    ) = hendelsefabrikk
        .lagInntektsmelding(
            arbeidsgiverperioder = arbeidsgiverperioder,
            beregnetInntekt = beregnetInntekt,
            førsteFraværsdag = førsteFraværsdag,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
        ).also {
            it.valider(
                object : Inntektsmelding.Valideringsgrunnlag {
                    override fun vedtaksperiode(vedtaksperiodeId: UUID) = null

                    override fun inntektsmeldingIkkeHåndtert(inntektsmelding: Inntektsmelding) {}
                },
                Aktivitetslogg(),
            )
        }
}
