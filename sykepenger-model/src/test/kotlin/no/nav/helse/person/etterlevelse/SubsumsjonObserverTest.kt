package no.nav.helse.person.etterlevelse

import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.januar
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.Inntektshistorikk.Skatt
import no.nav.helse.person.Inntektshistorikk.SkattComposite
import no.nav.helse.person.Sammenligningsgrunnlag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Tidslinjedag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Tidslinjedag.Companion.dager
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*

internal class SubsumsjonObserverTest {

    @Test
    fun tidslinjedager() {
        val dager = List(31) { index ->
            val dagen = (index + 1).januar
            if (dagen < 16.januar) Tidslinjedag(dagen, "NAVDAG", 100)
            else Tidslinjedag(dagen, "FERIEDAG", 100)
        }

        assertEquals(
            listOf(
                mapOf(
                    "fom" to 1.januar,
                    "tom" to 15.januar,
                    "dagtype" to "NAVDAG",
                    "grad" to 100
                ),
                mapOf(
                    "fom" to 16.januar,
                    "tom" to 31.januar,
                    "dagtype" to "FERIEDAG",
                    "grad" to 100
                )
            ),
            dager.dager()
        )
    }

    @Test
    fun `tidslinjedager blir cappet til periode`() {
        val dager = List(31) { index ->
            val dagen = (index + 1).januar
            if (dagen < 16.januar) Tidslinjedag(dagen, "NAVDAG", 100)
            else Tidslinjedag(dagen, "FERIEDAG", 100)
        }

        assertEquals(
            listOf(
                mapOf(
                    "fom" to 10.januar,
                    "tom" to 15.januar,
                    "dagtype" to "NAVDAG",
                    "grad" to 100
                ),
                mapOf(
                    "fom" to 16.januar,
                    "tom" to 20.januar,
                    "dagtype" to "FERIEDAG",
                    "grad" to 100
                )
            ),
            dager.dager(Periode(10.januar, 20.januar))
        )
    }

    @Test
    fun erRettFør() {
        assertTrue(Tidslinjedag(1.januar, "dagtype", 100).erRettFør(2.januar))
        assertFalse(Tidslinjedag(1.januar, "dagtype", 100).erRettFør(3.januar))
    }

    @Test
    fun `tar med fridager på slutten av en sykdomsperiode`() {
        val utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV, 2.FRI)
        val tidslinjedager = utbetalingstidslinje.subsumsjonsformat().dager()

        assertEquals(
            listOf(
                mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100),
                mapOf("fom" to 1.februar, "tom" to 2.februar, "dagtype" to "FRIDAG", "grad" to 0)
            ),
            tidslinjedager
        )
    }

    @Test
    fun `tar ikke med fridager i oppholdsperiode`() {
        val utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV, 10.ARB, 2.FRI)
        val tidslinjedager = utbetalingstidslinje.subsumsjonsformat().dager()

        assertEquals(
            listOf(
                mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
            ),
            tidslinjedager
        )
    }

    @Test
    fun sammenligningsgrunnlag() {
        val AG1 = "123456789"
        val AG2 = "987654321"
        val sammenligningsgrunnlag = Sammenligningsgrunnlag(
            sammenligningsgrunnlag = 40000.årlig,
            arbeidsgiverInntektsopplysninger = listOf(
                ArbeidsgiverInntektsopplysning(
                    AG1,
                    SkattComposite(
                        UUID.randomUUID(),
                        listOf(
                            Skatt.Sammenligningsgrunnlag(
                                dato = 1.januar,
                                hendelseId = UUID.randomUUID(),
                                beløp = 20000.månedlig,
                                måned = YearMonth.of(2069, 12),
                                type = Skatt.Inntekttype.LØNNSINNTEKT,
                                fordel = "fordel",
                                beskrivelse = "beskrivelse"
                            ),
                            Skatt.Sammenligningsgrunnlag(
                                dato = 1.januar,
                                hendelseId = UUID.randomUUID(),
                                beløp = 20000.månedlig,
                                måned = YearMonth.of(2069, 11),
                                type = Skatt.Inntekttype.LØNNSINNTEKT,
                                fordel = "fordel",
                                beskrivelse = "beskrivelse"
                            )
                        )
                    )
                ),
                ArbeidsgiverInntektsopplysning(
                    AG2,
                    SkattComposite(
                        UUID.randomUUID(),
                        listOf(
                            Skatt.Sammenligningsgrunnlag(
                                dato = 1.januar,
                                hendelseId = UUID.randomUUID(),
                                beløp = 15000.månedlig,
                                måned = YearMonth.of(2069, 10),
                                type = Skatt.Inntekttype.LØNNSINNTEKT,
                                fordel = "fordel",
                                beskrivelse = "beskrivelse"
                            ),
                            Skatt.Sammenligningsgrunnlag(
                                dato = 1.januar,
                                hendelseId = UUID.randomUUID(),
                                beløp = 15000.månedlig,
                                måned = YearMonth.of(2069, 9),
                                type = Skatt.Inntekttype.LØNNSINNTEKT,
                                fordel = "fordel",
                                beskrivelse = "beskrivelse"
                            )
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
