package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.serde.reflection.UtbetalingstidslinjeberegningReflect
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Utbetalingstidslinjeberegning private constructor(
    private val id: UUID,
    private val tidsstempel: LocalDateTime,
    private val sykdomshistorikkElementId: UUID,
    private val organisasjonsnummer: String,
    private val utbetalingstidslinje: Utbetalingstidslinje
) {

    internal constructor(
        sykdomshistorikkElementId: UUID,
        organisasjonsnummer: String,
        utbetalingstidslinje: Utbetalingstidslinje
    ) : this(UUID.randomUUID(), LocalDateTime.now(), sykdomshistorikkElementId, organisasjonsnummer, utbetalingstidslinje)

    internal fun utbetalingstidslinje() = utbetalingstidslinje

    internal companion object {
        internal fun lagUtbetaling(
            beregnetUtbetalingstidslinjer: MutableList<Utbetalingstidslinjeberegning>,
            utbetalinger: MutableList<Utbetaling>,
            fødselsnummer: String,
            periode: Periode,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            forrige: Utbetaling?
        ): Utbetaling {
            val beregning = beregnetUtbetalingstidslinjer.last()
            return Utbetaling.lagUtbetaling(
                utbetalinger,
                fødselsnummer,
                beregning.organisasjonsnummer,
                beregning.utbetalingstidslinje,
                periode.endInclusive,
                aktivitetslogg,
                maksdato,
                forbrukteSykedager,
                gjenståendeSykedager,
                forrige
            )
        }

        internal fun restore(id: UUID, tidsstempel: LocalDateTime, sykdomshistorikkElementId: UUID, organisasjonsnummer: String, utbetalingstidslinje: Utbetalingstidslinje) =
            Utbetalingstidslinjeberegning(
                id, tidsstempel, sykdomshistorikkElementId, organisasjonsnummer, utbetalingstidslinje
            )

        internal fun save(utbetalingstidslinjeberegning: Utbetalingstidslinjeberegning) =
            UtbetalingstidslinjeberegningReflect(utbetalingstidslinjeberegning).toMap()
    }
}
