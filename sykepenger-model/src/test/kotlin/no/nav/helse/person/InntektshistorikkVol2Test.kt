package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.person.InntektshistorikkVol2.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class InntektshistorikkVol2Test {

    private lateinit var historikk: InntektshistorikkVol2
    private val inspektør get() = Inntektsinspektør(historikk)

    private companion object {
        const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"
        val INNTEKT = 31000.00.månedlig
    }

    @BeforeEach
    fun setup() {
        historikk = InntektshistorikkVol2()
    }

    @Test
    fun `Inntekt fra inntektsmelding blir lagt til i inntektshistorikk`() {
        inntektsmelding().addInntekt(historikk, 1.januar)
        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(1, inspektør.inntektTeller.first())
    }

    @Test
    fun `Inntekt fra inntektsmelding brukes til å beregne sykepengegrunnlaget`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, 1.januar)
        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(1, inspektør.inntektTeller.first())
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar(2018)))
    }

    @Test
    fun `Inntekt fra andre inntektsmelding overskriver ikke inntekt fra første, gitt samme første fraværsdag`() {
        inntektsmelding(førsteFraværsdag = 1.januar, beregnetInntekt = 30000.månedlig).addInntekt(historikk, 1.januar)
        inntektsmelding(førsteFraværsdag = 1.januar, beregnetInntekt = 29000.månedlig).addInntekt(historikk, 1.januar)
        inntektsmelding(førsteFraværsdag = 1.februar, beregnetInntekt = 31000.månedlig).addInntekt(historikk, 1.februar)
        assertEquals(30000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar(2018)))
        assertEquals(31000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.februar(2018)))
    }

    @Test
    fun `Inntekt fra inntektsmelding brukes ikke til å beregne sykepengegrunnlaget på annen dato`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, 1.januar)
        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(1, inspektør.inntektTeller.first())
        assertNull(historikk.grunnlagForSykepengegrunnlag(2.januar))
    }

    @Test
    fun `Inntekt fra infotrygd brukes til å beregne sykepengegrunnlaget`() {
        utbetalingshistorikk(
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, INNTEKT, ORGNUMMER, true)
            )
        ).addInntekter(UUID.randomUUID(), ORGNUMMER, historikk)
        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(1, inspektør.inntektTeller.first())
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar(2018)))
    }

    @Test
    fun `Bruker inntekt fra infotrygd fremfor inntekt fra inntektsmelding for å beregne sykepengegrunnlaget`() {
        inntektsmelding(beregnetInntekt = 20000.månedlig).addInntekt(historikk, 1.januar)
        utbetalingshistorikk(
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 25000.månedlig, ORGNUMMER, true)
            )
        ).addInntekter(UUID.randomUUID(), ORGNUMMER, historikk)
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(2, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
        assertEquals(25000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar(2018)))
    }

    @Test
    fun `Bruker inntekt fra infotrygd fremfor inntekt fra skatt for å beregne sykepengegrunnlaget - skatt kommer først`() {
        inntektperioder {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
            1.desember(2016) til 1.september(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar, UUID.randomUUID()) }
        utbetalingshistorikk(
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 25000.månedlig, ORGNUMMER, true)
            )
        ).addInntekter(UUID.randomUUID(), ORGNUMMER, historikk)
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(24, inspektør.inntektTeller.first())
        assertEquals(23, inspektør.inntektTeller.last())
        assertEquals(25000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar(2018)))
    }

    @Test
    fun `Bruker inntekt fra infotrygd fremfor inntekt fra skatt for å beregne sykepengegrunnlaget - skatt kommer sist`() {
        utbetalingshistorikk(
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 25000.månedlig, ORGNUMMER, true)
            )
        ).addInntekter(UUID.randomUUID(), ORGNUMMER, historikk)
        inntektperioder {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
            1.desember(2016) til 1.september(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar, UUID.randomUUID()) }
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(24, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
        assertEquals(25000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar(2018)))
    }

    @Test
    fun `intrikat test for sammenligningsgrunnlag der første fraværsdag er 31 desember`() {
        inntektperioder {
            inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
            1.desember(2016) til 1.desember(2016) inntekter {
                ORGNUMMER inntekt 10000
            }
            1.desember(2016) til 1.august(2017) inntekter {
                ORGNUMMER inntekt 20000
            }
            1.oktober(2017) til 1.oktober(2017) inntekter {
                ORGNUMMER inntekt 30000
            }
            1.november(2017) til 1.januar(2018) inntekter {
                ORGNUMMER inntekt 12000
                ORGNUMMER inntekt 22000
            }
        }.forEach { it.lagreInntekter(historikk, 31.desember(2017), UUID.randomUUID()) }
        inntektperioder {
            inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SYKEPENGEGRUNNLAG
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt 15000

            }
        }.forEach { it.lagreInntekter(historikk, 31.desember(2017), UUID.randomUUID()) }
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(30, inspektør.inntektTeller.first())
        assertEquals(17, inspektør.inntektTeller.last())
        assertEquals(254000.årlig, historikk.grunnlagForSammenligningsgrunnlag(31.desember(2017)))
    }

    @Test
    fun `intrikat test for sammenligningsgrunnlag der første fraværsdag er 1 januar`() {
        inntektperioder {
            inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
            1.desember(2016) til 1.desember(2016) inntekter {
                ORGNUMMER inntekt 10000
            }
            1.desember(2016) til 1.august(2017) inntekter {
                ORGNUMMER inntekt 20000
            }
            1.oktober(2017) til 1.oktober(2017) inntekter {
                ORGNUMMER inntekt 30000
            }
            1.november(2017) til 1.januar(2018) inntekter {
                ORGNUMMER inntekt 12000
                ORGNUMMER inntekt 22000
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar, UUID.randomUUID()) }
        inntektperioder {
            inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SYKEPENGEGRUNNLAG
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt 15000

            }
        }.forEach { it.lagreInntekter(historikk, 1.januar, UUID.randomUUID()) }
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(30, inspektør.inntektTeller.first())
        assertEquals(17, inspektør.inntektTeller.last())
        assertEquals(258000.årlig, historikk.grunnlagForSammenligningsgrunnlag(1.januar))
    }

    @Test
    fun `intrikat test for sykepengegrunnlag der første fraværsdag er 31 desember`() {
        inntektperioder {
            inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SYKEPENGEGRUNNLAG
            1.desember(2016) til 1.desember(2016) inntekter {
                ORGNUMMER inntekt 10000
            }
            1.desember(2016) til 1.august(2017) inntekter {
                ORGNUMMER inntekt 20000
            }
            1.oktober(2017) til 1.oktober(2017) inntekter {
                ORGNUMMER inntekt 30000
            }
            1.november(2017) til 1.januar(2018) inntekter {
                ORGNUMMER inntekt 12000
                ORGNUMMER inntekt 22000
            }
        }.forEach { it.lagreInntekter(historikk, 31.desember(2017), UUID.randomUUID()) }
        inntektperioder {
            inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt 15000

            }
        }.forEach { it.lagreInntekter(historikk, 31.desember(2017), UUID.randomUUID()) }
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(30, inspektør.inntektTeller.first())
        assertEquals(17, inspektør.inntektTeller.last())
        assertEquals(256000.årlig, historikk.grunnlagForSykepengegrunnlag(31.desember(2017)))
    }

    @Test
    fun `intrikat test for sykepengegrunnlag der første fraværsdag er 1 januar`() {
        inntektperioder {
            inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SYKEPENGEGRUNNLAG
            1.desember(2016) til 1.desember(2016) inntekter {
                ORGNUMMER inntekt 10000
            }
            1.desember(2016) til 1.august(2017) inntekter {
                ORGNUMMER inntekt 20000
            }
            1.oktober(2017) til 1.oktober(2017) inntekter {
                ORGNUMMER inntekt 30000
            }
            1.november(2017) til 1.januar(2018) inntekter {
                ORGNUMMER inntekt 12000
                ORGNUMMER inntekt 22000
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar, UUID.randomUUID()) }
        inntektperioder {
            inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt 15000

            }
        }.forEach { it.lagreInntekter(historikk, 1.januar, UUID.randomUUID()) }
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(30, inspektør.inntektTeller.first())
        assertEquals(17, inspektør.inntektTeller.last())
        assertEquals(392000.årlig, historikk.grunnlagForSykepengegrunnlag(1.januar))
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
        }.forEach { it.lagreInntekter(historikk, 1.januar, UUID.randomUUID()) }
        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(22, inspektør.inntektTeller.first())
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar))
    }

    @Test
    fun `Inntekter med forskjellig dato konflikterer ikke`() {
        inntektperioder {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar(2018), UUID.randomUUID()) }
        inntektperioder {
            1.desember(2016) til 1.august(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 15.januar(2018), UUID.randomUUID()) }
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(22, inspektør.inntektTeller.first())
        assertEquals(13, inspektør.inntektTeller.last())
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar(2018)))
        assertNull(historikk.grunnlagForSykepengegrunnlag(15.januar(2018)))
    }

    @Test
    fun `Senere inntekter for samme dato overskriver eksisterende inntekter`() {
        inntektperioder {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar(2018), UUID.randomUUID()) }
        Thread.sleep(10) // Nødvendig for konsistent resultat på windows
        inntektperioder {
            1.desember(2016) til 1.august(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar(2018), UUID.randomUUID()) }
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(9, inspektør.inntektTeller.first())
        assertEquals(13, inspektør.inntektTeller.last())
        assertNull(historikk.grunnlagForSykepengegrunnlag(1.januar(2018)))
    }

    @Test
    fun `Inntekt fra skatt skal bare brukes en gang`() {
        repeat(3) { _ ->
            val meldingsreferanseId = UUID.randomUUID()
            inntektperioder {
                (1.desember(2016) til 1.desember(2017)) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
                1.desember(2016) til 1.august(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.forEach { it.lagreInntekter(historikk, 1.januar, meldingsreferanseId) }
            Thread.sleep(10) // Nødvendig for konsistent resultat på windows
        }

        assertEquals(3, inspektør.inntektTeller.size)
        inspektør.inntektTeller.forEach {
            assertEquals(22, it)
        }
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar(2018)))
    }

    @Test
    fun `Inntekt fra skatt skal bare brukes én gang i beregning av sammenligningsgrunnlag`() {
        repeat(3) { _ ->
            val meldingsreferanseId = UUID.randomUUID()
            inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.desember(2016) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.forEach { it.lagreInntekter(historikk, 1.januar, meldingsreferanseId) }
        }
        assertEquals(13, inspektør.inntektTeller.first())
        assertEquals(INNTEKT, historikk.grunnlagForSammenligningsgrunnlag(1.januar(2018)))
    }

    @Test
    fun `Inntekt for annen dato og samme kilde erstatter ikke eksisterende`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, 1.januar)
        inntektsmelding(
            førsteFraværsdag = 2.januar,
            arbeidsgiverperioder = listOf(2.januar til 17.januar)
        ).addInntekt(historikk, 1.januar)
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(2, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
    }

    @Test
    fun `Inntekt for samme dato og annen kilde erstatter ikke eksisterende`() {
        inntektsmelding().addInntekt(historikk, 1.januar)
        utbetalingshistorikk(
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, INNTEKT, ORGNUMMER, true)
            )
        ).addInntekter(UUID.randomUUID(), ORGNUMMER, historikk)
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(2, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
    }

    @Test
    fun `Finner nærmeste inntekt fra Infotrygd, hvis det ikke finnes inntekt for skjæringstidspunkt`() {
        utbetalingshistorikk(
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(10.januar, 30000.månedlig, ORGNUMMER, true),
                Utbetalingshistorikk.Inntektsopplysning(5.januar, 25000.månedlig, ORGNUMMER, true)
            )
        ).addInntekter(UUID.randomUUID(), ORGNUMMER, historikk)
        assertEquals(30000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar, 11.januar))
        assertEquals(25000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar, 9.januar))
        assertNull(historikk.grunnlagForSykepengegrunnlag(1.januar, 4.januar))
    }

    private class Inntektsinspektør(historikk: InntektshistorikkVol2) : InntekthistorikkVisitor {
        var inntektTeller = mutableListOf<Int>()

        init {
            historikk.accept(this)
        }

        override fun preVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {
            inntektTeller.clear()
        }

        override fun preVisitInnslag(innslag: InntektshistorikkVol2.Innslag) {
            inntektTeller.add(0)
        }

        override fun visitInntektVol2(
            inntektsopplysning: Inntektsopplysning,
            id: UUID,
            fom: LocalDate,
            tidsstempel: LocalDateTime
        ) {
            inntektTeller.add(inntektTeller.removeLast() + 1)
        }

        override fun visitInntektSkattVol2(
            id: UUID,
            fom: LocalDate,
            måned: YearMonth,
            tidsstempel: LocalDateTime
        ) {
            inntektTeller.add(inntektTeller.removeLast() + 1)
        }

        override fun visitInntektSaksbehandlerVol2(
            id: UUID,
            fom: LocalDate,
            tidsstempel: LocalDateTime
        ) {
            inntektTeller.add(inntektTeller.removeLast() + 1)
        }

        override fun visitSaksbehandler(
            saksbehandler: InntektshistorikkVol2.Saksbehandler,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektTeller.add(inntektTeller.removeLast() + 1)
        }

        override fun visitInntektsmelding(
            inntektsmelding: InntektshistorikkVol2.Inntektsmelding,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektTeller.add(inntektTeller.removeLast() + 1)
        }

        override fun visitInfotrygd(
            infotrygd: InntektshistorikkVol2.Infotrygd,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektTeller.add(inntektTeller.removeLast() + 1)
        }

        override fun visitSkattSykepengegrunnlag(
            sykepengegrunnlag: InntektshistorikkVol2.Skatt.Sykepengegrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: InntektshistorikkVol2.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            inntektTeller.add(inntektTeller.removeLast() + 1)
        }

        override fun visitSkattSammenligningsgrunnlag(
            sammenligningsgrunnlag: InntektshistorikkVol2.Skatt.Sammenligningsgrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: InntektshistorikkVol2.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            inntektTeller.add(inntektTeller.removeLast() + 1)
        }
    }

    private fun inntektsmelding(
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        arbeidsgiverperioder: List<Periode> = listOf(1.januar til 16.januar)
    ) = Inntektsmelding(
        meldingsreferanseId = UUID.randomUUID(),
        refusjon = Inntektsmelding.Refusjon(null, INNTEKT, emptyList()),
        orgnummer = ORGNUMMER,
        fødselsnummer = UNG_PERSON_FNR_2018,
        aktørId = AKTØRID,
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        arbeidsgiverperioder = arbeidsgiverperioder,
        ferieperioder = emptyList(),
        arbeidsforholdId = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt = null
    )

    private fun utbetalingshistorikk(inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning>) =
        Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = UUID.randomUUID().toString(),
            utbetalinger = emptyList(),
            inntektshistorikk = inntektshistorikk
        )
}
