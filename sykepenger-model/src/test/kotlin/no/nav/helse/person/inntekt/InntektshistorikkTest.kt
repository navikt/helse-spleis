package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.lagreInntekter
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.Inntektsinspektør
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somPersonidentifikator
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InntektshistorikkTest {

    private lateinit var historikk: Inntektshistorikk
    private val inspektør get() = Inntektsinspektør(historikk)

    private companion object {
        const val UNG_PERSON_FNR_2018 = "12029240045"
        val UNG_PERSON_FØDSELSDATO = 12.februar(1992)

        const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"
        val INNTEKT = 31000.00.månedlig
        val hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
            aktørId = AKTØRID,
            personidentifikator = UNG_PERSON_FNR_2018.somPersonidentifikator(),
            organisasjonsnummer = ORGNUMMER
        )
    }

    @BeforeEach
    fun setup() {
        historikk = Inntektshistorikk()
    }

    @Test
    fun `sykepengegrunnlag for arbeidsgiver med nytt arbeidsforhold`() {
        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()
        arbeidsforholdhistorikk.lagre(listOf(
            Arbeidsforholdhistorikk.Arbeidsforhold(1.januar, null, false)
        ), 1.februar)
        val opplysning = historikk.omregnetÅrsinntekt(1.februar, 1.februar, arbeidsforholdhistorikk)
        assertNotNull(opplysning)
        assertEquals(IkkeRapportert::class, opplysning::class)
        assertEquals(INGEN, opplysning.omregnetÅrsinntekt())
    }

    @Test
    fun `sykepengegrunnlag for arbeidsgiver med nytt deaktivert arbeidsforhold`() {
        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()
        arbeidsforholdhistorikk.lagre(listOf(
            Arbeidsforholdhistorikk.Arbeidsforhold(1.januar, null, true)
        ), 1.februar)
        val opplysning = historikk.omregnetÅrsinntekt(1.februar, 1.februar, arbeidsforholdhistorikk)
        assertNotNull(opplysning)
        assertEquals(IkkeRapportert::class, opplysning::class)
        assertEquals(INGEN, opplysning.omregnetÅrsinntekt())
    }

    @Test
    fun `Inntekt fra inntektsmelding brukes til å beregne sykepengegrunnlaget`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, 1.januar, MaskinellJurist())
        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(1, inspektør.inntektTeller.first())
        assertEquals(INNTEKT, historikk.omregnetÅrsinntekt(1.januar, 1.januar, Arbeidsforholdhistorikk())?.omregnetÅrsinntekt())
    }

    @Test
    fun `Inntekt fra andre inntektsmelding overskriver ikke inntekt fra første, gitt samme første fraværsdag`() {
        inntektsmelding(førsteFraværsdag = 1.januar, beregnetInntekt = 30000.månedlig).addInntekt(historikk, 1.januar, MaskinellJurist())
        inntektsmelding(førsteFraværsdag = 1.januar, beregnetInntekt = 29000.månedlig).addInntekt(historikk, 1.januar, MaskinellJurist())
        inntektsmelding(førsteFraværsdag = 1.februar, beregnetInntekt = 31000.månedlig).addInntekt(historikk, 1.februar, MaskinellJurist())
        assertEquals(30000.månedlig, historikk.omregnetÅrsinntekt(1.januar, 1.januar, Arbeidsforholdhistorikk())?.omregnetÅrsinntekt())
        assertEquals(31000.månedlig, historikk.omregnetÅrsinntekt(1.februar, 1.februar, Arbeidsforholdhistorikk())?.omregnetÅrsinntekt())
    }

    @Test
    fun `Inntekt fra inntektsmelding brukes ikke til å beregne sykepengegrunnlaget på annen dato`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, 1.januar, MaskinellJurist())
        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(1, inspektør.inntektTeller.first())
        assertNull(historikk.omregnetÅrsinntekt(2.januar, 2.januar, Arbeidsforholdhistorikk()))
    }

    @Test
    fun `sammenligningsgrunnlag for arbeidsgiver med nytt arbeidsforhold`() {
        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()
        arbeidsforholdhistorikk.lagre(listOf(
            Arbeidsforholdhistorikk.Arbeidsforhold(1.januar, null, false)
        ), 1.februar)
        val opplysning = historikk.rapportertInntekt(1.februar)
        assertNull(opplysning)
    }

    @Test
    fun `sammenligningsgrunnlag for arbeidsgiver med nytt deaktivert arbeidsforhold`() {
        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()
        arbeidsforholdhistorikk.lagre(listOf(
            Arbeidsforholdhistorikk.Arbeidsforhold(1.januar, null, true)
        ), 1.februar)
        val opplysning = historikk.rapportertInntekt(1.februar)
        assertNull(opplysning)
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
            1.november(2017) til 1.januar inntekter {
                ORGNUMMER inntekt 12000
                ORGNUMMER inntekt 22000
            }
        }.lagreInntekter(ORGNUMMER, historikk, 31.desember(2017), UUID.randomUUID())
        inntektperioderForSykepengegrunnlag {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt 15000

            }
        }.lagreInntekter(ORGNUMMER, historikk, 31.desember(2017), UUID.randomUUID())
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(30, inspektør.inntektTeller.first())
        assertEquals(17, inspektør.inntektTeller.last())
        assertEquals(254000.årlig, historikk.rapportertInntekt(31.desember(2017))?.rapportertInntekt())
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
            1.november(2017) til 1.januar inntekter {
                ORGNUMMER inntekt 12000
                ORGNUMMER inntekt 22000
            }
        }.lagreInntekter(ORGNUMMER, historikk, 1.januar, UUID.randomUUID())
        inntektperioderForSykepengegrunnlag {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt 15000

            }
        }.lagreInntekter(ORGNUMMER, historikk, 1.januar, UUID.randomUUID())
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(30, inspektør.inntektTeller.first())
        assertEquals(17, inspektør.inntektTeller.last())
        assertEquals(258000.årlig, historikk.rapportertInntekt(1.januar)?.rapportertInntekt())
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
            1.november(2017) til 1.januar inntekter {
                ORGNUMMER inntekt 12000
                ORGNUMMER inntekt 22000
            }
        }.lagreInntekter(ORGNUMMER, historikk, 31.desember(2017), UUID.randomUUID())

        inntektperioderForSammenligningsgrunnlag {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt 15000

            }
        }.lagreInntekter(ORGNUMMER, historikk, 31.desember(2017), UUID.randomUUID())
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(30, inspektør.inntektTeller.first())
        assertEquals(17, inspektør.inntektTeller.last())
        assertEquals(256000.årlig, historikk.omregnetÅrsinntekt(31.desember(2017), 31.desember(2017), Arbeidsforholdhistorikk())?.omregnetÅrsinntekt())
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
            1.november(2017) til 1.januar inntekter {
                ORGNUMMER inntekt 12000
                ORGNUMMER inntekt 22000
            }
        }.lagreInntekter(ORGNUMMER, historikk, 1.januar, UUID.randomUUID())
        inntektperioderForSammenligningsgrunnlag {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt 15000

            }
        }.lagreInntekter(ORGNUMMER, historikk, 1.januar, UUID.randomUUID())
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(30, inspektør.inntektTeller.first())
        assertEquals(17, inspektør.inntektTeller.last())
        assertEquals(392000.årlig, historikk.omregnetÅrsinntekt(1.januar, 1.januar, Arbeidsforholdhistorikk())?.omregnetÅrsinntekt())
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
        }.lagreInntekter(ORGNUMMER, historikk, 1.januar, UUID.randomUUID())
        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(22, inspektør.inntektTeller.first())
        assertEquals(INNTEKT, historikk.omregnetÅrsinntekt(1.januar, 1.januar, Arbeidsforholdhistorikk())?.omregnetÅrsinntekt())
    }

    @Test
    fun `Inntekt fra skatt siste tre måneder som tilsammen er et negativt beløp`() {
        inntektperioderForSykepengegrunnlag {
            1.oktober(2017) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT * -1
            }
        }.lagreInntekter(ORGNUMMER, historikk, 1.januar, UUID.randomUUID())
        val inntektsopplysning = historikk.omregnetÅrsinntekt(1.januar, 1.januar, Arbeidsforholdhistorikk())
        assertTrue(inntektsopplysning is SkattComposite)
        assertEquals(INGEN, inntektsopplysning?.omregnetÅrsinntekt())
    }

    @Test
    fun `Inntekt fra skatt er minst 0 kroner`() {
        val skattComposite = SkattComposite(
            UUID.randomUUID(), inntektsopplysninger = listOf(
                Skatt.Sykepengegrunnlag(
                    dato = 1.januar,
                    hendelseId = UUID.randomUUID(),
                    beløp = (-2500).daglig,
                    måned = desember(2017),
                    type = Skatt.Inntekttype.LØNNSINNTEKT,
                    fordel = "fordel",
                    beskrivelse = "beskrivelse"
                )
            )
        )
        assertEquals(INGEN, skattComposite.omregnetÅrsinntekt())
    }

    @Test
    fun `Inntekter med forskjellig dato konflikterer ikke`() {
        inntektperioderForSykepengegrunnlag {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.lagreInntekter(ORGNUMMER, historikk, 1.januar, UUID.randomUUID())
        inntektperioderForSykepengegrunnlag {
            1.desember(2016) til 1.august(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.lagreInntekter(ORGNUMMER, historikk, 15.januar, UUID.randomUUID())
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(22, inspektør.inntektTeller.first())
        assertEquals(13, inspektør.inntektTeller.last())
        assertEquals(INNTEKT, historikk.omregnetÅrsinntekt(1.januar, 1.januar, Arbeidsforholdhistorikk())?.omregnetÅrsinntekt())
        assertNull(historikk.omregnetÅrsinntekt(15.januar, 15.januar, Arbeidsforholdhistorikk()))
    }

    @Test
    fun `Senere inntekter for samme dato overskriver eksisterende inntekter`() {
        inntektperioderForSykepengegrunnlag {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.lagreInntekter(ORGNUMMER, historikk, 1.januar, UUID.randomUUID())
        inntektperioderForSykepengegrunnlag {
            1.desember(2016) til 1.august(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        }.lagreInntekter(ORGNUMMER, historikk, 1.januar, UUID.randomUUID())
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(9, inspektør.inntektTeller.first())
        assertEquals(13, inspektør.inntektTeller.last())
        assertNull(historikk.omregnetÅrsinntekt(1.januar, 1.januar, Arbeidsforholdhistorikk()))
    }

    @Test
    fun `Inntekt fra skatt skal bare brukes en gang`() {
        repeat(3) { _ ->
            val meldingsreferanseId = UUID.randomUUID()
            inntektperioderForSykepengegrunnlag {
                1.desember(2016) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
                1.desember(2016) til 1.august(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.lagreInntekter(ORGNUMMER, historikk, 1.januar, meldingsreferanseId)
        }

        assertEquals(1, inspektør.inntektTeller.size)
        inspektør.inntektTeller.forEach {
            assertEquals(22, it)
        }
        assertEquals(INNTEKT, historikk.omregnetÅrsinntekt(1.januar, 1.januar, Arbeidsforholdhistorikk())?.omregnetÅrsinntekt())
    }

    @Test
    fun `Inntekt fra skatt skal bare brukes én gang i beregning av sammenligningsgrunnlag`() {
        repeat(3) { _ ->
            val meldingsreferanseId = UUID.randomUUID()
            inntektperioderForSammenligningsgrunnlag {
                1.desember(2016) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.lagreInntekter(ORGNUMMER, historikk, 1.januar, meldingsreferanseId)
        }
        assertEquals(13, inspektør.inntektTeller.first())
        assertEquals(INNTEKT, historikk.rapportertInntekt(1.januar)?.rapportertInntekt())
    }

    @Test
    fun `Inntekt for annen dato og samme kilde erstatter ikke eksisterende`() {
        inntektsmelding(førsteFraværsdag = 1.januar).addInntekt(historikk, 1.januar, MaskinellJurist())
        inntektsmelding(
            førsteFraværsdag = 2.januar,
            arbeidsgiverperioder = listOf(2.januar til 17.januar)
        ).addInntekt(historikk, 1.januar, MaskinellJurist())
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(2, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
    }

    private fun inntektsmelding(
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        arbeidsgiverperioder: List<Periode> = listOf(1.januar til 16.januar)
    ) = hendelsefabrikk.lagInntektsmelding(
        refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        arbeidsgiverperioder = arbeidsgiverperioder,
        arbeidsforholdId = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt = null
    )
}
