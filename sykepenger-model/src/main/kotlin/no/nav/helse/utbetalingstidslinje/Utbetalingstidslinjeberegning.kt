package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.UtbetalingstidslinjeberegningVisitor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingtype

internal class Utbetalingstidslinjeberegning private constructor(
    private val id: UUID,
    private val tidsstempel: LocalDateTime,
    private val sykdomshistorikkElementId: UUID,
    private val vilkårsgrunnlagHistorikkInnslagId: UUID,
    private val organisasjonsnummer: String,
    private val utbetalingstidslinje: Utbetalingstidslinje
) {

    internal constructor(
        sykdomshistorikkElementId: UUID,
        vilkårsgrunnlagHistorikkInnslagId: UUID,
        organisasjonsnummer: String,
        utbetalingstidslinje: Utbetalingstidslinje
    ) : this(
        id = UUID.randomUUID(),
        tidsstempel = LocalDateTime.now(),
        sykdomshistorikkElementId = sykdomshistorikkElementId,
        vilkårsgrunnlagHistorikkInnslagId = vilkårsgrunnlagHistorikkInnslagId,
        organisasjonsnummer = organisasjonsnummer,
        utbetalingstidslinje = utbetalingstidslinje
    )

    internal fun utbetalingstidslinje() = utbetalingstidslinje

    internal fun accept(visitor: UtbetalingstidslinjeberegningVisitor) {
        visitor.preVisitUtbetalingstidslinjeberegning(
            id,
            tidsstempel,
            organisasjonsnummer,
            sykdomshistorikkElementId,
            vilkårsgrunnlagHistorikkInnslagId
        )
        utbetalingstidslinje.accept(visitor)
        visitor.postVisitUtbetalingstidslinjeberegning(
            id,
            tidsstempel,
            organisasjonsnummer,
            sykdomshistorikkElementId,
            vilkårsgrunnlagHistorikkInnslagId
        )
    }

    internal companion object {
        internal fun lagUtbetaling(
            beregnetUtbetalingstidslinjer: List<Utbetalingstidslinjeberegning>,
            utbetalinger: List<Utbetaling>,
            fødselsnummer: String,
            periode: Periode,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            type: Utbetalingtype,
            organisasjonsnummer: String
        ): Pair<Utbetaling, List<Utbetaling>> {
            val beregning = beregnetUtbetalingstidslinjer.last()
            return Utbetaling.lagUtbetaling(
                utbetalinger,
                fødselsnummer,
                beregning.id,
                organisasjonsnummer,
                beregning.utbetalingstidslinje,
                periode,
                aktivitetslogg,
                maksdato,
                forbrukteSykedager,
                gjenståendeSykedager,
                type
            )
        }

        internal fun restore(
            id: UUID,
            tidsstempel: LocalDateTime,
            sykdomshistorikkElementId: UUID,
            vilkårsgrunnlagHistorikkInnslagId: UUID,
            organisasjonsnummer: String,
            utbetalingstidslinje: Utbetalingstidslinje
        ) = Utbetalingstidslinjeberegning(
            id = id,
            tidsstempel = tidsstempel,
            sykdomshistorikkElementId = sykdomshistorikkElementId,
            vilkårsgrunnlagHistorikkInnslagId = vilkårsgrunnlagHistorikkInnslagId,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingstidslinje = utbetalingstidslinje
        )
    }
}
