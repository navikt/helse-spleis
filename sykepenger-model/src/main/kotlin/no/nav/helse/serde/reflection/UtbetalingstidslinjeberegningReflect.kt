package no.nav.helse.serde.reflection

import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingstidslinjeberegningReflect(utbetalingstidslinjeberegning: Utbetalingstidslinjeberegning) {
    private val id: UUID = utbetalingstidslinjeberegning["id"]
    private val sykdomshistorikkElementId: UUID = utbetalingstidslinjeberegning["sykdomshistorikkElementId"]
    private val inntektshistorikkInnslagId: UUID = utbetalingstidslinjeberegning["inntektshistorikkInnslagId"]
    private val vilkårsgrunnlagHistorikkInnslagId: UUID = utbetalingstidslinjeberegning["vilkårsgrunnlagHistorikkInnslagId"]
    private val tidsstempel: LocalDateTime = utbetalingstidslinjeberegning["tidsstempel"]
    private val organisasjonsnummer: String = utbetalingstidslinjeberegning["organisasjonsnummer"]
    private val utbetalingstidslinje = UtbetalingstidslinjeReflect(utbetalingstidslinjeberegning["utbetalingstidslinje"]).toMap()

    internal fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "sykdomshistorikkElementId" to sykdomshistorikkElementId,
        "vilkårsgrunnlagHistorikkInnslagId" to vilkårsgrunnlagHistorikkInnslagId,
        "inntektshistorikkInnslagId" to inntektshistorikkInnslagId,
        "tidsstempel" to tidsstempel,
        "organisasjonsnummer" to organisasjonsnummer,
        "utbetalingstidslinje" to utbetalingstidslinje
    )
}
