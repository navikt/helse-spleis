package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.til
import no.nav.helse.person.InntekthistorikkVol2.Inntektsendring
import no.nav.helse.testhelpers.august
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InntekthistorikkVol2Test {

    private lateinit var historikk: InntekthistorikkVol2
    private val inspektør get() = Inntektsinspektør(historikk)

    private companion object {
        const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"
        val INNTEKT = 31000.00.månedlig
    }

    @BeforeEach
    fun setup() {
        historikk = InntekthistorikkVol2()
    }

    @Test
    fun `Inntekt fra inntektsmelding blir lagt til i inntektshistorikk`() {
        inntektsmelding().addInntekt(historikk)
        assertEquals(1, inspektør.inntektTeller)
    }

    @Test
    fun `Inntekt fra inntektsmelding brukes til å beregne sykepengegrunnlaget`() {
        inntektsmelding().addInntekt(historikk)
        assertEquals(1, inspektør.inntektTeller)
        assertEquals(INNTEKT, historikk.sykepengegrunnlag(31.desember(2017)))
    }

    @Test
    fun `Inntekt fra inntektsmelding brukes ikke til å beregne sykepengegrunnlaget på annen dato`() {
        inntektsmelding().addInntekt(historikk)
        assertEquals(1, inspektør.inntektTeller)
        assertEquals(INGEN, historikk.sykepengegrunnlag(1.januar))
    }

    @Test
    fun `Inntekt fra infotrygd brukes til å beregne sykepengegrunnlaget`() {
        utbetalingshistorikk(
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, INNTEKT, ORGNUMMER, true)
            )
        ).addInntekter(UUID.randomUUID(), ORGNUMMER, historikk)
        assertEquals(1, inspektør.inntektTeller)
        assertEquals(INNTEKT, historikk.sykepengegrunnlag(31.desember(2017)))
    }

    @Test
    fun `Bruker inntekt fra inntektsmelding fremfor inntekt fra infotrygd for å beregne sykepengegrunnlaget`() {
        inntektsmelding(beregnetInntekt = 20000.månedlig).addInntekt(historikk)
        utbetalingshistorikk(
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 25000.månedlig, ORGNUMMER, true)
            )
        ).addInntekter(UUID.randomUUID(), ORGNUMMER, historikk)
        assertEquals(2, inspektør.inntektTeller)
        assertEquals(20000.månedlig, historikk.sykepengegrunnlag(31.desember(2017)))
    }

    @Test
    fun `Inntekt fra skatt siste tre måneder brukes til å beregne sykepengegrunnlaget`() {
        inntektperioder {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
            1.desember(2016) til 1.august(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, UUID.randomUUID()) }
        assertEquals(13, inspektør.inntektTeller)
        assertEquals(INNTEKT, historikk.sykepengegrunnlag(31.desember(2017)))
    }

    @Test
    fun `Inntekt fra skatt skal bare brukes en gang`() {
        repeat(3) { i ->
            val meldingsreferanseId = UUID.randomUUID()
            val tidsstempel = LocalDateTime.now().plusDays(i % 2L)
            inntektperioder {
                (1.desember(2016) til 1.desember(2017)) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
                1.desember(2016) til 1.august(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.forEach { it.lagreInntekter(historikk, meldingsreferanseId, tidsstempel) }
        }
        assertEquals(13, inspektør.inntektTeller)
        assertEquals(INNTEKT, historikk.sykepengegrunnlag(31.desember(2017)))
    }

    @Test
    fun `Inntekt fra skatt skal bare brukes en gang i beregning av sammenligningsgrunnlag`() {
        repeat(3) { i ->
            val meldingsreferanseId = UUID.randomUUID()
            val tidsstempel = LocalDateTime.now().plusDays(i % 2L)
            inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.desember(2016) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.forEach { it.lagreInntekter(historikk, meldingsreferanseId, tidsstempel) }
        }
        assertEquals(13, inspektør.inntektTeller)
        assertEquals(INNTEKT, historikk.sammenligningsgrunnlag(31.desember(2017)))
    }

    @Test
    fun `Inntekt for samme dato og samme kilde erstatter eksisterende`() {
        inntektsmelding().addInntekt(historikk)
        inntektsmelding().addInntekt(historikk)
        assertEquals(1, inspektør.inntektTeller)
    }

    @Test
    fun `Inntekt for annen dato og samme kilde erstatter ikke eksisterende`() {
        inntektsmelding().addInntekt(historikk)
        inntektsmelding(førsteFraværsdag = 2.januar).addInntekt(historikk)
        assertEquals(2, inspektør.inntektTeller)
    }

    @Test
    fun `Inntekt for samme dato og annen kilde erstatter ikke eksisterende`() {
        inntektsmelding().addInntekt(historikk)
        utbetalingshistorikk(
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, INNTEKT, ORGNUMMER, true)
            )
        ).addInntekter(UUID.randomUUID(), ORGNUMMER, historikk)
        assertEquals(2, inspektør.inntektTeller)
    }

    @Test
    fun `Inntekt for flere datoer og samme kilde erstatter ikke hverandre`() {
        inntektperioder {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, UUID.randomUUID()) }
        assertEquals(13, inspektør.inntektTeller)
    }

    @Test
    fun `Onehsot add skatt`(){
        historikk.endring {
            inntektperioder {
                1.desember(2016) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.forEach {

                it.lagreInntekter(this, UUID.randomUUID()) }
        }
        assertEquals(13, inspektør.inntektTeller)
    }

    private class Inntektsinspektør(historikk: InntekthistorikkVol2) : InntekthistorikkVisitor {
        var inntektTeller = 0

        init {
            historikk.accept(this)
        }

        override fun preVisitInntekthistorikkVol2(inntekthistorikk: InntekthistorikkVol2) {
            inntektTeller = 0
        }

        override fun visitInntektVol2(
            inntektsendring: Inntektsendring,
            id: UUID,
            kilde: Inntektsendring.Kilde,
            fom: LocalDate
        ) {
            inntektTeller += 1
        }

        override fun visitInntektSkattVol2(
            inntektsendring: Inntektsendring.Skatt,
            id: UUID,
            kilde: Inntektsendring.Kilde,
            fom: LocalDate
        ) {
            inntektTeller += 1
        }

        override fun visitInntektSaksbehandlerVol2(
            inntektsendring: Inntektsendring.Saksbehandler,
            id: UUID,
            kilde: Inntektsendring.Kilde,
            fom: LocalDate
        ) {
            inntektTeller += 1
        }
    }

    private fun inntektsmelding(
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar
    ) = Inntektsmelding(
        meldingsreferanseId = UUID.randomUUID(),
        refusjon = Inntektsmelding.Refusjon(null, INNTEKT, emptyList()),
        orgnummer = ORGNUMMER,
        fødselsnummer = UNG_PERSON_FNR_2018,
        aktørId = AKTØRID,
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        arbeidsgiverperioder = listOf(1.januar til 16.januar),
        ferieperioder = emptyList(),
        arbeidsforholdId = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt = null
    )

    private fun utbetalingshistorikk(inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning>) =
        Utbetalingshistorikk(
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = UUID.randomUUID().toString(),
            utbetalinger = emptyList(),
            inntektshistorikk = inntektshistorikk
        )
}
