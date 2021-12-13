package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.UtbetalingstidslinjeberegningVisitor
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Utbetalingstidslinjeberegning private constructor(
    private val id: UUID,
    private val tidsstempel: LocalDateTime,
    private val sykdomshistorikkElementId: UUID,
    private val inntektshistorikkInnslagId: UUID,
    private val vilkårsgrunnlagHistorikkInnslagId: UUID,
    private val organisasjonsnummer: String,
    private val utbetalingstidslinje: Utbetalingstidslinje
) {

    internal constructor(
        sykdomshistorikkElementId: UUID,
        inntektshistorikkInnslagId: UUID,
        vilkårsgrunnlagHistorikkInnslagId: UUID,
        organisasjonsnummer: String,
        utbetalingstidslinje: Utbetalingstidslinje
    ) : this(
        id = UUID.randomUUID(),
        tidsstempel = LocalDateTime.now(),
        sykdomshistorikkElementId = sykdomshistorikkElementId,
        inntektshistorikkInnslagId = inntektshistorikkInnslagId,
        vilkårsgrunnlagHistorikkInnslagId = vilkårsgrunnlagHistorikkInnslagId,
        organisasjonsnummer = organisasjonsnummer,
        utbetalingstidslinje = utbetalingstidslinje
    )

    internal fun utbetalingstidslinje() = utbetalingstidslinje

    internal fun accept(visitor: UtbetalingstidslinjeberegningVisitor) {
        visitor.preVisitUtbetalingstidslinjeberegning(id, tidsstempel, organisasjonsnummer, sykdomshistorikkElementId, inntektshistorikkInnslagId, vilkårsgrunnlagHistorikkInnslagId)
        utbetalingstidslinje.accept(visitor)
        visitor.postVisitUtbetalingstidslinjeberegning(id, tidsstempel, organisasjonsnummer, sykdomshistorikkElementId, inntektshistorikkInnslagId, vilkårsgrunnlagHistorikkInnslagId)
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
            forrige: Utbetaling?,
            organisasjonsnummer: String
        ): Utbetaling {
            val beregning = beregnetUtbetalingstidslinjer.last()
            return Utbetaling.lagUtbetaling(
                utbetalinger,
                fødselsnummer,
                beregning.id,
                organisasjonsnummer,
                beregning.utbetalingstidslinje,
                periode.endInclusive,
                aktivitetslogg,
                maksdato,
                forbrukteSykedager,
                gjenståendeSykedager,
                forrige
            )
        }

        internal fun lagRevurdering(
            beregnetUtbetalingstidslinjer: List<Utbetalingstidslinjeberegning>,
            utbetalinger: List<Utbetaling>,
            fødselsnummer: String,
            periode: Periode,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            forrige: Utbetaling?,
            organisasjonsnummer: String
        ): Utbetaling {
            val beregning = beregnetUtbetalingstidslinjer.last()
            return Utbetaling.lagRevurdering(
                utbetalinger,
                fødselsnummer,
                beregning.id,
                organisasjonsnummer,
                beregning.utbetalingstidslinje,
                periode.endInclusive,
                aktivitetslogg,
                maksdato,
                forbrukteSykedager,
                gjenståendeSykedager,
                forrige
            )
        }

        internal fun restore(
            id: UUID,
            tidsstempel: LocalDateTime,
            sykdomshistorikkElementId: UUID,
            inntektshistorikkInnslagId: UUID,
            vilkårsgrunnlagHistorikkInnslagId: UUID,
            organisasjonsnummer: String,
            utbetalingstidslinje: Utbetalingstidslinje
        ) = Utbetalingstidslinjeberegning(
            id = id,
            tidsstempel = tidsstempel,
            sykdomshistorikkElementId = sykdomshistorikkElementId,
            inntektshistorikkInnslagId = inntektshistorikkInnslagId,
            vilkårsgrunnlagHistorikkInnslagId = vilkårsgrunnlagHistorikkInnslagId,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingstidslinje = utbetalingstidslinje
        )
    }
}
