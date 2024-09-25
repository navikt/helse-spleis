package no.nav.helse.person.refusjon

import java.time.LocalDate
import java.util.SortedMap
import no.nav.helse.hendelser.Periode
import no.nav.helse.økonomi.Inntekt

data class Refusjonstidslinje private constructor(
    private val dager: SortedMap<LocalDate, EtBeløpMedKildePåSeg>
) {
    internal constructor(vararg dager: Pair<LocalDate, EtBeløpMedKildePåSeg>): this(dager.toMap())

    internal constructor(periode: Periode, etBeløpMedKildePåSeg: EtBeløpMedKildePåSeg): this(periode.associateWith { etBeløpMedKildePåSeg })

    internal constructor(dager: Map<LocalDate, EtBeløpMedKildePåSeg>): this(dager.toSortedMap())

    internal operator fun get(dato: LocalDate): EtBeløpMedKildePåSeg = dager[dato] ?: error("Du er tullegutt")
}

data class EtBeløpMedKildePåSeg(
    val beløp: Inntekt,
    val kilde: Kilde
)
