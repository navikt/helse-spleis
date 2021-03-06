package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InntektshistorikkTest {

    private lateinit var historikk: Inntektshistorikk
    private val inspektør get() = Inntektsinspektør(historikk)

    private companion object {
        const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"
        val INNTEKT = 31000.00.månedlig
    }

    @BeforeEach
    fun setup() {
        historikk = Inntektshistorikk()
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
    fun `intrikat test for sammenligningsgrunnlag der første fraværsdag er 31 desember`() {
        inntektperioderForSammenligningsgrunnlag {
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
        inntektperioderForSykepengegrunnlag {
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
        inntektperioderForSammenligningsgrunnlag {
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
        inntektperioderForSykepengegrunnlag {
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
        inntektperioderForSykepengegrunnlag {
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
        inntektperioderForSammenligningsgrunnlag {
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
        inntektperioderForSykepengegrunnlag {
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
        inntektperioderForSammenligningsgrunnlag {
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
        inntektperioderForSykepengegrunnlag {
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
        inntektperioderForSykepengegrunnlag {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar(2018), UUID.randomUUID()) }
        inntektperioderForSykepengegrunnlag {
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
        inntektperioderForSykepengegrunnlag {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar(2018), UUID.randomUUID()) }
        Thread.sleep(10) // Nødvendig for konsistent resultat på windows
        inntektperioderForSykepengegrunnlag {
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
            inntektperioderForSykepengegrunnlag {
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
            inntektperioderForSammenligningsgrunnlag {
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
    fun `Kopier inntektsopplysning fra inntektsmelding`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, 1.januar)

        assertTrue(historikk.opprettReferanse(1.januar, 10.januar, UUID.randomUUID()))

        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar))
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(10.januar))

        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(2, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
    }

    @Test
    fun `Kopierer ikke inntektsmeldinginntekt med annen dato`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, 1.januar)

        assertFalse(historikk.opprettReferanse(5.januar, 10.januar, UUID.randomUUID()))

        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar))
        assertNull(historikk.grunnlagForSykepengegrunnlag(10.januar))

        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(1, inspektør.inntektTeller.first())
    }

    @Test
    fun `Oppretter ikke nytt innslag hvis ingen inntekter kopieres`() {
        assertFalse(historikk.opprettReferanse(1.januar, 10.januar, UUID.randomUUID()))

        assertNull(historikk.grunnlagForSykepengegrunnlag(1.januar))
        assertNull(historikk.grunnlagForSykepengegrunnlag(10.januar))

        assertTrue(inspektør.inntektTeller.isEmpty())
    }

    @Test
    fun `Kopier inntektsopplysning fra saksbehandler`() {
        historikk {
            addSaksbehandler(1.januar, UUID.randomUUID(), INNTEKT)
        }

        assertTrue(historikk.opprettReferanse(1.januar, 10.januar, UUID.randomUUID()))

        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar))
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(10.januar))

        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(2, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
    }

    @Test
    fun `Kopierer ikke saksbehandlerinntekt med annen dato`() {
        historikk {
            addSaksbehandler(1.januar, UUID.randomUUID(), INNTEKT)
        }

        assertFalse(historikk.opprettReferanse(5.januar, 10.januar, UUID.randomUUID()))

        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar))
        assertNull(historikk.grunnlagForSykepengegrunnlag(10.januar))

        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(1, inspektør.inntektTeller.first())
    }

    @Test
    fun `Kopier inntektsopplysningskopi`() {
        historikk {
            addSaksbehandler(1.januar, UUID.randomUUID(), INNTEKT)
        }

        assertTrue(historikk.opprettReferanse(1.januar, 10.januar, UUID.randomUUID()))
        assertTrue(historikk.opprettReferanse(10.januar, 20.januar, UUID.randomUUID()))

        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar))
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(10.januar))
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(20.januar))

        assertEquals(3, inspektør.inntektTeller.size)
        assertEquals(3, inspektør.inntektTeller[0])
        assertEquals(2, inspektør.inntektTeller[1])
        assertEquals(1, inspektør.inntektTeller[2])
    }

    @Test
    fun `Kopierer ikke inntektsopplysningskopiinntekt med annen dato`() {
        historikk {
            addSaksbehandler(1.januar, UUID.randomUUID(), INNTEKT)
        }

        assertTrue(historikk.opprettReferanse(1.januar, 10.januar, UUID.randomUUID()))
        assertFalse(historikk.opprettReferanse(15.januar, 20.januar, UUID.randomUUID()))

        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar))
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(10.januar))
        assertNull(historikk.grunnlagForSykepengegrunnlag(20.januar))

        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(2, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
    }

    @Test
    fun `Kopierer ikke skatteopplysninger`() {
        inntektperioderForSykepengegrunnlag {
            1.oktober(2017) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar, UUID.randomUUID()) }

        assertFalse(historikk.opprettReferanse(1.januar, 10.januar, UUID.randomUUID()))

        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar))
        assertNull(historikk.grunnlagForSykepengegrunnlag(10.januar))

        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(3, inspektør.inntektTeller.first())
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
        arbeidsforholdId = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt = null,
        mottatt = LocalDateTime.now()
    )
}
