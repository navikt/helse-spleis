package no.nav.helse.serde.reflection

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDateTime
import java.util.*

internal class ArbeidsgiverReflect(arbeidsgiver: Arbeidsgiver) {
    private val organisasjonsnummer: String = arbeidsgiver["organisasjonsnummer"]
    private val id: UUID = arbeidsgiver["id"]
    private val beregnetUtbetalingstidslinjer = arbeidsgiver.get<List<Triple<String, Utbetalingstidslinje, LocalDateTime>>>("beregnetUtbetalingstidslinjer")
        .map { (orgnr, utbetalingstidslinje, tidsstempel) ->
            mapOf(
                "organisasjonsnummer" to orgnr,
                "utbetalingstidslinje" to UtbetalingstidslinjeReflect(utbetalingstidslinje).toMap(),
                "tidsstempel" to tidsstempel
            )
        }

    internal fun toMap(): Map<String, Any?> = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "id" to id,
        "beregnetUtbetalingstidslinjer" to beregnetUtbetalingstidslinjer
    )

    internal fun toSpeilMap() = toMap()
}
