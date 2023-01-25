package no.nav.helse.hendelser

import java.time.LocalDate

class SimuleringResultat(
    val totalbeløp: Int,
    val perioder: List<SimulertPeriode>
) {
    fun toMap() = mapOf(
        "totalbeløp" to totalbeløp,
        "perioder" to perioder.map { periode ->
            mapOf(
                "fom" to periode.periode.start,
                "tom" to periode.periode.endInclusive,
                "utbetalinger" to periode.utbetalinger.map { utbetaling ->
                    mapOf(
                        "forfallsdato" to utbetaling.forfallsdato,
                        "utbetalesTil" to mapOf(
                            "id" to utbetaling.utbetalesTil.id,
                            "navn" to utbetaling.utbetalesTil.navn
                        ),
                        "feilkonto" to utbetaling.feilkonto,
                        "detaljer" to utbetaling.detaljer.map { detalj ->
                            mapOf(
                                "fom" to detalj.periode.start,
                                "tom" to detalj.periode.endInclusive,
                                "konto" to detalj.konto,
                                "beløp" to detalj.beløp,
                                "klassekode" to mapOf(
                                    "kode" to detalj.klassekode.kode,
                                    "beskrivelse" to detalj.klassekode.beskrivelse
                                ),
                                "uføregrad" to detalj.uføregrad,
                                "utbetalingstype" to detalj.utbetalingstype,
                                "tilbakeføring" to detalj.tilbakeføring,
                                "sats" to mapOf(
                                    "sats" to detalj.sats.sats,
                                    "antall" to detalj.sats.antall,
                                    "type" to detalj.sats.type
                                ),
                                "refunderesOrgnummer" to detalj.refunderesOrgnummer
                            )
                        }
                    )
                }
            )
        }
    )

    class SimulertPeriode(
        val periode: ClosedRange<LocalDate>,
        val utbetalinger: List<SimulertUtbetaling>
    )

    class SimulertUtbetaling(
        val forfallsdato: LocalDate,
        val utbetalesTil: Mottaker,
        val feilkonto: Boolean,
        val detaljer: List<Detaljer>
    )

    class Detaljer(
        val periode: ClosedRange<LocalDate>,
        val konto: String,
        val beløp: Int,
        val klassekode: Klassekode,
        val uføregrad: Int,
        val utbetalingstype: String,
        val tilbakeføring: Boolean,
        val sats: Sats,
        val refunderesOrgnummer: String
    )

    class Sats(
        val sats: Double,
        val antall: Int,
        val type: String
    )

    class Klassekode(
        val kode: String,
        val beskrivelse: String
    )

    class Mottaker(
        val id: String,
        val navn: String
    )
}