package no.nav.helse.person.inntekt

import java.time.YearMonth
import java.util.UUID
import no.nav.helse.etterlevelse.SammenligningsgrunnlagBuilder.Companion.subsumsjonsformat
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SammenligningsgrunnlagTest {
    @Test
    fun sammenligningsgrunnlag() {
        val AG1 = "123456789"
        val AG2 = "987654321"
        val sammenligningsgrunnlag = Sammenligningsgrunnlag(
            sammenligningsgrunnlag = 40000.årlig,
            arbeidsgiverInntektsopplysninger = listOf(
                ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
                    AG1,
                    listOf(
                        Skatteopplysning(
                            hendelseId = UUID.randomUUID(),
                            beløp = 20000.månedlig,
                            måned = YearMonth.of(2069, 12),
                            type = Skatteopplysning.Inntekttype.LØNNSINNTEKT,
                            fordel = "fordel",
                            beskrivelse = "beskrivelse"
                        ),
                        Skatteopplysning(
                            hendelseId = UUID.randomUUID(),
                            beløp = 20000.månedlig,
                            måned = YearMonth.of(2069, 11),
                            type = Skatteopplysning.Inntekttype.LØNNSINNTEKT,
                            fordel = "fordel",
                            beskrivelse = "beskrivelse"
                        )
                    )
                ),
                ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
                    AG2,
                    listOf(
                        Skatteopplysning(
                            hendelseId = UUID.randomUUID(),
                            beløp = 15000.månedlig,
                            måned = YearMonth.of(2069, 10),
                            type = Skatteopplysning.Inntekttype.LØNNSINNTEKT,
                            fordel = "fordel",
                            beskrivelse = "beskrivelse"
                        ),
                        Skatteopplysning(
                            hendelseId = UUID.randomUUID(),
                            beløp = 15000.månedlig,
                            måned = YearMonth.of(2069, 9),
                            type = Skatteopplysning.Inntekttype.LØNNSINNTEKT,
                            fordel = "fordel",
                            beskrivelse = "beskrivelse"
                        )
                    )
                )
            )
        )
        val subsumsjonsformat = sammenligningsgrunnlag.subsumsjonsformat()
        assertEquals(40000.0, subsumsjonsformat.sammenligningsgrunnlag)
        assertEquals(
            mapOf(
                "123456789" to listOf(
                    mapOf(
                        "beløp" to 20000.0,
                        "årMåned" to YearMonth.of(2069, 12),
                        "type" to "LØNNSINNTEKT",
                        "fordel" to "fordel",
                        "beskrivelse" to "beskrivelse"
                    ),
                    mapOf(
                        "beløp" to 20000.0,
                        "årMåned" to YearMonth.of(2069, 11),
                        "type" to "LØNNSINNTEKT",
                        "fordel" to "fordel",
                        "beskrivelse" to "beskrivelse"
                    )
                ),
                "987654321" to listOf(
                    mapOf(
                        "beløp" to 15000.0,
                        "årMåned" to YearMonth.of(2069, 10),
                        "type" to "LØNNSINNTEKT",
                        "fordel" to "fordel",
                        "beskrivelse" to "beskrivelse"
                    ),
                    mapOf(
                        "beløp" to 15000.0,
                        "årMåned" to YearMonth.of(2069, 9),
                        "type" to "LØNNSINNTEKT",
                        "fordel" to "fordel",
                        "beskrivelse" to "beskrivelse"
                    )
                )
            ),
            subsumsjonsformat.inntekterFraAOrdningen
        )
    }
}