package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal class V3BeregnerGjenståendeSykedagerFraMaksdato : JsonMigration(version = 3) {
    override val description = "Beregner gjenstående sykedager fra maksdato og legger til dette i vedtaksperioden"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            val utbetalingsdager =
                arbeidsgiver["utbetalinger"]
                    .flatMap { it["utbetalingstidslinje"]["dager"] }
                    .filter { it["type"].textValue() == "NavDag" }
                    .map { it["dato"].textValue().let(LocalDate::parse) }
            arbeidsgiver["vedtaksperioder"].forEach { periode ->
                val gjenståendeSykedager = periode
                    .takeIf { it.path("maksdato").isTextual }
                    ?.let { it["sykdomshistorikk"].first()["beregnetSykdomstidslinje"] }
                    ?.map { it["dagen"].textValue().let(LocalDate::parse) }
                    ?.lastOrNull { it in utbetalingsdager }
                    ?.let { sisteUtbetalingsdag ->
                        val maksdato = periode["maksdato"].textValue().let(LocalDate::parse)
                        (1..ChronoUnit.DAYS.between(sisteUtbetalingsdag, maksdato))
                            .map { sisteUtbetalingsdag.plusDays(it) }
                            .count { it.dayOfWeek !in arrayListOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
                    }
                (periode as ObjectNode).put("gjenståendeSykedager", gjenståendeSykedager)
            }
        }
    }
}
