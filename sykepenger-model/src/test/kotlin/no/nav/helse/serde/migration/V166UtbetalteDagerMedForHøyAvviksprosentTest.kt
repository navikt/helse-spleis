package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.readResource
import no.nav.helse.serde.migration.V166UtbetalteDagerMedForHøyAvviksprosent.Companion.Avvik
import no.nav.helse.serde.serdeObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V166UtbetalteDagerMedForHøyAvviksprosentTest: MigrationTest(V166UtbetalteDagerMedForHøyAvviksprosent()) {

    @Test
    fun `migrering endrer kun vesjonsnummer`(){
        val original = "/migrations/166/enArbeidsgiverMedAvvik.json".readResource()
        val expected = (serdeObjectMapper.readTree(original) as ObjectNode).put("skjemaVersjon", 166)
        assertMigrationRaw("$expected", original)
    }

    @Test
    fun `en arbeidsgiver med avvik`() {
        val avvik = "/migrations/166/enArbeidsgiverMedAvvik.json".avvik()
        val forventetAvvik = listOf(
            forventetAvvik(
                skjæringstidspunkt = 3.januar,
                avvik = 0.25001,
                utbetaltePerioder = listOf(19.januar til 19.januar, 22.januar til 26.januar)
            )
        )
        assertEquals(forventetAvvik, avvik)
    }

    @Test
    fun `en arbeidsgiver uten avvik`() {
        assertEquals(emptyList<Avvik>(), "/migrations/166/enArbeidsgiverUtenAvvik.json".avvik())
    }

    @Test
    fun `to arbeidsgivere med avvik`() {
        val avvik = "/migrations/166/flereArbeidsgivere.json".avvik()
        val forventetAvvik = listOf(
            forventetAvvik(
                skjæringstidspunkt = 1.januar(2021),
                avvik = 0.5,
                utbetaltePerioder = listOf(18.januar(2021) til 22.januar(2021), 25.januar(2021) til 29.januar(2021))
            ),
            forventetAvvik(
                arbeidsgiver = "654321987",
                skjæringstidspunkt = 1.januar(2021),
                avvik = 0.5,
                utbetaltePerioder = listOf(18.januar(2021) til 22.januar(2021), 25.januar(2021) til 29.januar(2021))
            )
        )
        assertEquals(forventetAvvik, avvik)
    }

    @Test
    fun `annullert utbetaling med avvik`() {
        assertEquals(emptyList<Avvik>(), "/migrations/166/annullertMedAvvik.json".avvik())
    }

    @Test
    fun `to arbeidsgivere med avvik i forskjellige perioder`() {
        val avvik = "/migrations/166/toArbeidsgivereMedAvvik.json".avvik()
        val forventetAvvik = listOf(
            forventetAvvik(
                skjæringstidspunkt = 1.mai,
                avvik = 0.4,
                utbetaltePerioder = listOf(17.mai til 18.mai)
            ),
            forventetAvvik(
                arbeidsgiver = "654321987",
                skjæringstidspunkt = 1.mars,
                avvik = 0.26,
                utbetaltePerioder = listOf(19.mars til 20.mars)
            )
        )
        assertEquals(forventetAvvik, avvik)
    }


    private companion object {
        private fun String.avvik() = V166UtbetalteDagerMedForHøyAvviksprosent.finnAvvik(
            serdeObjectMapper.readTree(this.readResource()) as ObjectNode
        )

        private fun forventetAvvik(
            sykmeldt: String = "12029240045",
            aktørId: String = "42",
            arbeidsgiver: String = "987654321",
            skjæringstidspunkt: LocalDate,
            avvik: Double,
            utbetaltePerioder: List<Periode>
        ) = Avvik(sykmeldt, aktørId, arbeidsgiver, skjæringstidspunkt, avvik, utbetaltePerioder.flatMap { it.toList() })
    }
}