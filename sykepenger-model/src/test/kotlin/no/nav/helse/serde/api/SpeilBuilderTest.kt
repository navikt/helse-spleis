package no.nav.helse.serde.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.JsonBuilderTest.Companion.lagPerson
import no.nav.helse.serde.PersonVisitorProxy
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.streams.toList


internal class SpeilBuilderTest {

    @Test
    internal fun `dager før førsteFraværsdag og etter sisteSykedag skal kuttes vekk fra utbetalingstidslinje`() {
        val person = lagPerson()
        val jsonBuilder = SpeilBuilder()
        person.accept(
            DayPadderProxy(
                target = jsonBuilder,
                leftPadWithDays = 1.januar.minusDays(30).datesUntil(1.januar).toList(),
                rightPadWithDays = 1.februar.datesUntil(1.mars).toList()
            )
        )

        val json = jacksonObjectMapper().readTree(jsonBuilder.toString())
        assertEquals(
            1.januar,
            LocalDate.parse(json["arbeidsgivere"][0]["vedtaksperioder"][0]["utbetalingstidslinje"].first()["dato"].asText())
        )
        assertEquals(
            31.januar,
            LocalDate.parse(json["arbeidsgivere"][0]["vedtaksperioder"][0]["utbetalingstidslinje"].last()["dato"].asText())
        )
    }

    class DayPadderProxy(
        target: PersonVisitor,
        private val leftPadWithDays: List<LocalDate>,
        private val rightPadWithDays: List<LocalDate>
    ) : PersonVisitorProxy(target) {
        private var firstTime = true
        override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
            if (firstTime) {
                leftPadWithDays.forEach {
                    target.visitNavDag(Utbetalingstidslinje.Utbetalingsdag.NavDag(1000.0, it, 100.00))
                }
                firstTime = false
            }
            target.visitArbeidsgiverperiodeDag(dag)
        }

        override fun postVisitUtbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje) {
            rightPadWithDays.forEach {
                target.visitNavDag(Utbetalingstidslinje.Utbetalingsdag.NavDag(1000.0, it, 100.00))
            }
            target.postVisitUtbetalingstidslinje(utbetalingstidslinje)
        }

        override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            target.preVisitUtbetalingstidslinje(tidslinje)
            // legg på tulletidslinje som element 0 i arbeidsgiver.utbetalingstidslinjer-arrayen for å sikre at vi velger siste:
            leftPadWithDays.forEach {
                target.visitNavDag(Utbetalingstidslinje.Utbetalingsdag.NavDag(1000.0, it, 100.00))
            }
            target.postVisitUtbetalingstidslinje(tidslinje)

            target.preVisitUtbetalingstidslinje(tidslinje)
        }
    }


}

