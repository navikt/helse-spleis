package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest.Companion.INNTEKT
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.properties.Delegates

internal class SykepengegrunnlagTest {
    private companion object {
        private val FNR_67_ÅR_FEBRUAR_2021 = "01025400065".somFødselsnummer()
    }

    @Test
    fun equality() {
        val sykepengegrunnlag = INNTEKT.sykepengegrunnlag
        assertEquals(sykepengegrunnlag, sykepengegrunnlag)
        assertNotEquals(sykepengegrunnlag, INNTEKT.sykepengegrunnlag)
        assertNotEquals(INNTEKT.sykepengegrunnlag, INNTEKT.sykepengegrunnlag)
        assertNotEquals(INNTEKT.sykepengegrunnlag, INNTEKT.sykepengegrunnlag("annet orgnr"))
        assertNotEquals(INNTEKT.sykepengegrunnlag, INNTEKT.sykepengegrunnlag(31.desember))
    }

    @Test
    fun `minimum inntekt tom 67 år - må være 0,5 G`() {
        val alder = FNR_67_ÅR_FEBRUAR_2021.alder()
        val skjæringstidspunkt = 1.februar(2021)
        val halvG = Grunnbeløp.halvG.beløp(skjæringstidspunkt)

        var observer = MinsteinntektSubsumsjonObservatør()
        val sykepengegrunnlag = halvG.sykepengegrunnlag(alder, "orgnr", skjæringstidspunkt, observer)
        var aktivitetslogg = Aktivitetslogg()
        var validert = sykepengegrunnlag.valider(aktivitetslogg)
        assertTrue(validert)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
        assertTrue(sykepengegrunnlag.inspektør.oppfyllerMinsteinntektskrav)
        assertEquals(halvG, sykepengegrunnlag.inspektør.minsteinntekt)
        assertForventetFeil(
            forklaring = "må lage subsumsjon når Sykepengegrunnlag overtar validering av minsteinntekt",
            nå = {
                assertThrows<IllegalStateException> { observer.`§ 8-3 ledd 2 punktum 1` }
            },
            ønsket = {
                assertDoesNotThrow { observer.`§ 8-3 ledd 2 punktum 1` }
                assertTrue(observer.`§ 8-3 ledd 2 punktum 1`)
            }
        )

        observer = MinsteinntektSubsumsjonObservatør()
        aktivitetslogg = Aktivitetslogg()
        val forLiteSykepengegrunnlag = (halvG - 1.daglig).sykepengegrunnlag(alder, "orgnr", skjæringstidspunkt, observer)
        validert = forLiteSykepengegrunnlag.valider(aktivitetslogg)
        assertFalse(forLiteSykepengegrunnlag.inspektør.oppfyllerMinsteinntektskrav)
        assertEquals(halvG, forLiteSykepengegrunnlag.inspektør.minsteinntekt)
        assertForventetFeil(
            forklaring = "må lage subsumsjon når Sykepengegrunnlag overtar validering av minsteinntekt",
            nå = {
                assertTrue(validert)
                assertFalse(aktivitetslogg.hasWarningsOrWorse())
                assertThrows<IllegalStateException> { observer.`§ 8-3 ledd 2 punktum 1` }
            },
            ønsket = {
                assertFalse(validert)
                assertTrue(aktivitetslogg.hasWarningsOrWorse())
                assertDoesNotThrow { observer.`§ 8-3 ledd 2 punktum 1` }
                assertFalse(observer.`§ 8-3 ledd 2 punktum 1`)
            }
        )
    }

    @Test
    fun `minimum inntekt etter 67 år - må være 2 G`() {
        val alder = FNR_67_ÅR_FEBRUAR_2021.alder()
        val skjæringstidspunkt = 2.februar(2021)
        val `2G` = Grunnbeløp.`2G`.beløp(skjæringstidspunkt)

        var observer = MinsteinntektSubsumsjonObservatør()
        var aktivitetslogg = Aktivitetslogg()
        val sykepengegrunnlag = (`2G`).sykepengegrunnlag(alder, "orgnr", skjæringstidspunkt, observer)
        var validert = sykepengegrunnlag.valider(aktivitetslogg)
        assertTrue(sykepengegrunnlag.inspektør.oppfyllerMinsteinntektskrav)
        assertEquals(`2G`, sykepengegrunnlag.inspektør.minsteinntekt)
        assertTrue(validert)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
        assertForventetFeil(
            forklaring = "må lage subsumsjon når Sykepengegrunnlag overtar validering av minsteinntekt",
            nå = {
                assertThrows<IllegalStateException> { observer.`§ 8-51 ledd 2` }
            },
            ønsket = {
                assertDoesNotThrow { observer.`§ 8-51 ledd 2` }
                assertTrue(observer.`§ 8-51 ledd 2`)
            }
        )

        observer = MinsteinntektSubsumsjonObservatør()
        aktivitetslogg = Aktivitetslogg()
        val forLiteSykepengegrunnlag = (`2G` - 1.daglig).sykepengegrunnlag(alder, "orgnr", skjæringstidspunkt, observer)
        validert = forLiteSykepengegrunnlag.valider(aktivitetslogg)
        assertFalse(forLiteSykepengegrunnlag.inspektør.oppfyllerMinsteinntektskrav)
        assertEquals(`2G`, forLiteSykepengegrunnlag.inspektør.minsteinntekt)
        assertForventetFeil(
            forklaring = "må lage subsumsjon når Sykepengegrunnlag overtar validering av minsteinntekt",
            nå = {
                assertTrue(validert)
                assertFalse(aktivitetslogg.hasWarningsOrWorse())
                assertThrows<IllegalStateException> { observer.`§ 8-51 ledd 2` }
            },
            ønsket = {
                assertFalse(validert)
                assertTrue(aktivitetslogg.hasWarningsOrWorse())
                assertDoesNotThrow { observer.`§ 8-51 ledd 2` }
                assertFalse(observer.`§ 8-51 ledd 2`)
            }
        )
    }

    @Test
    fun `mindre enn 2G, men skjæringstidspunkt er før virkningen av minsteinntekt`() {
        val alder = FNR_67_ÅR_FEBRUAR_2021.alder()
        val skjæringstidspunkt = 23.mai(2021)
        val `2G_2021` = Grunnbeløp.`2G`.beløp(skjæringstidspunkt)
        val `2G_2020` = Grunnbeløp.`2G`.beløp(30.april(2021))

        var observer = MinsteinntektSubsumsjonObservatør()
        var aktivitetslogg = Aktivitetslogg()
        val sykepengegrunnlag = (`2G_2021`).sykepengegrunnlag(alder, "orgnr", skjæringstidspunkt, observer)
        var validert = sykepengegrunnlag.valider(aktivitetslogg)
        assertEquals(`2G_2020`, Grunnbeløp.`2G`.minsteinntekt(skjæringstidspunkt))
        assertTrue(sykepengegrunnlag.inspektør.oppfyllerMinsteinntektskrav)
        assertEquals(`2G_2020`, sykepengegrunnlag.inspektør.minsteinntekt)
        assertTrue(validert)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
        assertForventetFeil(
            forklaring = "må lage subsumsjon når Sykepengegrunnlag overtar validering av minsteinntekt",
            nå = {
                assertThrows<IllegalStateException> { observer.`§ 8-51 ledd 2` }
            },
            ønsket = {
                assertDoesNotThrow { observer.`§ 8-51 ledd 2` }
                assertTrue(observer.`§ 8-51 ledd 2`)
            }
        )

        aktivitetslogg = Aktivitetslogg()
        val forLiteSykepengegrunnlag = (`2G_2021` - 1.daglig).sykepengegrunnlag(alder, "orgnr", skjæringstidspunkt, observer)
        validert = forLiteSykepengegrunnlag.valider(aktivitetslogg)
        assertTrue(forLiteSykepengegrunnlag.inspektør.oppfyllerMinsteinntektskrav)
        assertEquals(`2G_2020`, forLiteSykepengegrunnlag.inspektør.minsteinntekt)
        assertForventetFeil(
            forklaring = "må lage subsumsjon når Sykepengegrunnlag overtar validering av minsteinntekt",
            nå = {
                assertTrue(validert)
                assertFalse(aktivitetslogg.hasWarningsOrWorse())
                assertThrows<IllegalStateException> { observer.`§ 8-51 ledd 2` }
            },
            ønsket = {
                assertFalse(validert)
                assertTrue(aktivitetslogg.hasWarningsOrWorse())
                assertDoesNotThrow { observer.`§ 8-51 ledd 2` }
                assertTrue(observer.`§ 8-51 ledd 2`)
            }
        )
    }
    @Test
    fun `mindre enn 2G, og skjæringstidspunkt er etter virkningen av minsteinntekt`() {
        val alder = FNR_67_ÅR_FEBRUAR_2021.alder()
        val skjæringstidspunkt = 24.mai(2021)
        val `2G_2021` = Grunnbeløp.`2G`.beløp(skjæringstidspunkt)
        var aktivitetslogg = Aktivitetslogg()
        val sykepengegrunnlag = (`2G_2021`).sykepengegrunnlag(alder, "orgnr", skjæringstidspunkt)
        var validert = sykepengegrunnlag.valider(aktivitetslogg)
        assertEquals(`2G_2021`, Grunnbeløp.`2G`.minsteinntekt(skjæringstidspunkt))
        assertTrue(sykepengegrunnlag.inspektør.oppfyllerMinsteinntektskrav)
        assertEquals(`2G_2021`, sykepengegrunnlag.inspektør.minsteinntekt)
        assertTrue(validert)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())

        aktivitetslogg = Aktivitetslogg()
        val forLiteSykepengegrunnlag = (`2G_2021` - 1.daglig).sykepengegrunnlag(alder, "orgnr", skjæringstidspunkt)
        validert = forLiteSykepengegrunnlag.valider(aktivitetslogg)
        assertFalse(forLiteSykepengegrunnlag.inspektør.oppfyllerMinsteinntektskrav)
        assertEquals(`2G_2021`, forLiteSykepengegrunnlag.inspektør.minsteinntekt)
        assertForventetFeil(
            forklaring = "må lage subsumsjon når Sykepengegrunnlag overtar validering av minsteinntekt",
            nå = {
                assertTrue(validert)
                assertFalse(aktivitetslogg.hasWarningsOrWorse())
            },
            ønsket = {
                assertFalse(validert)
                assertTrue(aktivitetslogg.hasWarningsOrWorse())
            }
        )
    }

    @Test
    fun `justerer grunnbeløpet`() {
        val sykepengegrunnlag = 60000.månedlig.sykepengegrunnlag("orgnr", 1.mai(2020), 1.mai(2020))
        val justert = sykepengegrunnlag.justerGrunnbeløp()
        assertNotEquals(sykepengegrunnlag, justert)
        assertNotEquals(sykepengegrunnlag.inspektør.sykepengegrunnlag, justert.inspektør.sykepengegrunnlag)
        assertNotEquals(sykepengegrunnlag.inspektør.`6G`, justert.inspektør.`6G`)
        assertTrue(sykepengegrunnlag.inspektør.`6G` < justert.inspektør.`6G`)
        assertTrue(sykepengegrunnlag.inspektør.sykepengegrunnlag < justert.inspektør.sykepengegrunnlag)
    }

    @Test
    fun `sykepengegrunnlaget skal rundes av - 6g-begrenset`() {
        val `6G` = Grunnbeløp.`6G`.beløp(1.januar)
        val sykepengegrunnlag = `6G`.sykepengegrunnlag
        assertNotEquals(`6G`, sykepengegrunnlag.inspektør.sykepengegrunnlag)
        assertEquals(`6G`.rundTilDaglig(), sykepengegrunnlag.inspektør.sykepengegrunnlag)
    }
    @Test
    fun `sykepengegrunnlaget skal rundes av - under 6`() {
        val daglig = 255.5.daglig
        val sykepengegrunnlag = daglig.sykepengegrunnlag
        assertNotEquals(daglig, sykepengegrunnlag.inspektør.sykepengegrunnlag)
        assertEquals(daglig.rundTilDaglig(), sykepengegrunnlag.inspektør.sykepengegrunnlag)
    }

    @Test
    fun `overstyrt sykepengegrunnlag`() {
        val inntekt = 10000.månedlig
        val overstyrt = 15000.månedlig
        val sykepengegrunnlag = Sykepengegrunnlag(
            alder = AbstractPersonTest.UNG_PERSON_FNR_2018.alder(),
            skjæringstidspunkt = 1.januar,
            arbeidsgiverInntektsopplysninger = listOf(
                ArbeidsgiverInntektsopplysning("orgnr", Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), inntekt))
            ),
            deaktiverteArbeidsforhold = emptyList(),
            vurdertInfotrygd = false,
            overstyrtGrunnlagForSykepengegrunnlag = overstyrt
        )
        assertNotEquals(inntekt.rundTilDaglig(), sykepengegrunnlag.sykepengegrunnlag)
        assertEquals(overstyrt.rundTilDaglig(), sykepengegrunnlag.sykepengegrunnlag)
    }

    @Test
    fun equals() {
        val inntektID = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()
        val sykepengegrunnlag1 = Sykepengegrunnlag(
            alder = AbstractPersonTest.UNG_PERSON_FNR_2018.alder(),
            skjæringstidspunkt = 1.januar,
            arbeidsgiverInntektsopplysninger = listOf(
                ArbeidsgiverInntektsopplysning(
                    orgnummer = "orgnummer",
                    inntektsopplysning = Inntektshistorikk.Infotrygd(
                        id = inntektID,
                        dato = 1.januar,
                        hendelseId = hendelseId,
                        beløp = 25000.månedlig,
                        tidsstempel = tidsstempel
                    )
                )
            ),
            deaktiverteArbeidsforhold = emptyList(),
            vurdertInfotrygd = false
        )

        assertEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                alder = AbstractPersonTest.UNG_PERSON_FNR_2018.alder(),
                skjæringstidspunkt = 1.januar,
                arbeidsgiverInntektsopplysninger = listOf(
                    ArbeidsgiverInntektsopplysning(
                        orgnummer = "orgnummer",
                        inntektsopplysning = Inntektshistorikk.Infotrygd(
                            id = inntektID,
                            dato = 1.januar,
                            hendelseId = hendelseId,
                            beløp = 25000.månedlig,
                            tidsstempel = tidsstempel
                        )
                    )
                ),
                deaktiverteArbeidsforhold = emptyList(),
                vurdertInfotrygd = false,
                overstyrtGrunnlagForSykepengegrunnlag = 25000.månedlig
            )
        )
        assertEquals(sykepengegrunnlag1, sykepengegrunnlag1.justerGrunnbeløp()) { "grunnbeløpet trenger ikke justering" }
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                alder = AbstractPersonTest.UNG_PERSON_FNR_2018.alder(),
                skjæringstidspunkt = 1.januar,
                arbeidsgiverInntektsopplysninger = emptyList(),
                deaktiverteArbeidsforhold = emptyList(),
                vurdertInfotrygd = false,
                overstyrtGrunnlagForSykepengegrunnlag = 25000.månedlig
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                alder = AbstractPersonTest.UNG_PERSON_FNR_2018.alder(),
                skjæringstidspunkt = 1.januar,
                arbeidsgiverInntektsopplysninger = listOf(
                    ArbeidsgiverInntektsopplysning(
                        orgnummer = "orgnummer",
                        inntektsopplysning = Inntektshistorikk.Infotrygd(
                            id = inntektID,
                            dato = 1.januar,
                            hendelseId = hendelseId,
                            beløp = 25000.månedlig,
                            tidsstempel = tidsstempel
                        )
                    )
                ),
                deaktiverteArbeidsforhold = emptyList(),
                vurdertInfotrygd = false,
                overstyrtGrunnlagForSykepengegrunnlag = 20000.månedlig
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                alder = AbstractPersonTest.UNG_PERSON_FNR_2018.alder(),
                skjæringstidspunkt = 1.januar,
                arbeidsgiverInntektsopplysninger = listOf(
                    ArbeidsgiverInntektsopplysning(
                        orgnummer = "orgnummer",
                        inntektsopplysning = Inntektshistorikk.Infotrygd(
                            id = inntektID,
                            dato = 1.januar,
                            hendelseId = hendelseId,
                            beløp = 25000.månedlig,
                            tidsstempel = tidsstempel
                        )
                    )
                ),
                deaktiverteArbeidsforhold = emptyList(),
                vurdertInfotrygd = true,
                overstyrtGrunnlagForSykepengegrunnlag = 25000.månedlig
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                alder = AbstractPersonTest.UNG_PERSON_FNR_2018.alder(),
                skjæringstidspunkt = 1.januar,
                arbeidsgiverInntektsopplysninger = listOf(
                    ArbeidsgiverInntektsopplysning(
                        orgnummer = "orgnummer",
                        inntektsopplysning = Inntektshistorikk.Infotrygd(
                            id = inntektID,
                            dato = 1.januar,
                            hendelseId = hendelseId,
                            beløp = 25000.månedlig,
                            tidsstempel = tidsstempel
                        )
                    )
                ),
                deaktiverteArbeidsforhold = listOf("orgnummer"),
                vurdertInfotrygd = false
            )
        )
    }

    private class MinsteinntektSubsumsjonObservatør : SubsumsjonObserver {
        var `§ 8-3 ledd 2 punktum 1` by Delegates.notNull<Boolean>()
        var `§ 8-51 ledd 2` by Delegates.notNull<Boolean>()

        override fun `§ 8-3 ledd 2 punktum 1`(
            oppfylt: Boolean,
            skjæringstidspunkt: LocalDate,
            grunnlagForSykepengegrunnlag: Inntekt,
            minimumInntekt: Inntekt
        ) {
            this.`§ 8-3 ledd 2 punktum 1` = oppfylt
        }

        override fun `§ 8-51 ledd 2`(
            oppfylt: Boolean,
            skjæringstidspunkt: LocalDate,
            alderPåSkjæringstidspunkt: Int,
            grunnlagForSykepengegrunnlag: Inntekt,
            minimumInntekt: Inntekt
        ) {
           this.`§ 8-51 ledd 2` = oppfylt
        }
    }
}