package no.nav.helse.serde.reflection

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning
import java.time.LocalDate
import java.util.*

internal class ArbeidsgiverReflect(arbeidsgiver: Arbeidsgiver) {
    private val organisasjonsnummer: String = arbeidsgiver["organisasjonsnummer"]
    private val id: UUID = arbeidsgiver["id"]
    private val refusjonOpphører: List<LocalDate?> = arbeidsgiver["refusjonOpphører"]
    private val beregnetUtbetalingstidslinjer = arbeidsgiver.get<List<Utbetalingstidslinjeberegning>>("beregnetUtbetalingstidslinjer")
        .map { Utbetalingstidslinjeberegning.save(it) }

    internal fun toMap(): Map<String, Any?> = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "id" to id,
        "beregnetUtbetalingstidslinjer" to beregnetUtbetalingstidslinjer,
        "refusjonOpphører" to refusjonOpphører
    )

    internal fun toSpeilMap() = toMap()
}
