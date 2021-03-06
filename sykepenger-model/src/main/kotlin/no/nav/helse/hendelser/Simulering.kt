package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.utbetalingslinjer.Oppdrag
import java.time.LocalDate
import java.util.*

class Simulering(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val simuleringOK: Boolean,
    private val melding: String,
    internal val simuleringResultat: SimuleringResultat?
) : ArbeidstakerHendelse(meldingsreferanseId) {

    internal fun erRelevant(other: UUID) = other.toString() == vedtaksperiodeId

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun valider(oppdrag: Oppdrag) = this.apply {
        if (!simuleringOK) {
            info("Feil under simulering", melding)
            error("Feil under simulering")
        }
        when {
            simuleringResultat == null -> {
                info("Ingenting ble simulert")
            }
            oppdrag.totalbeløp() != simuleringResultat.totalbeløp -> {
                info("Simulering kom frem til et annet totalbeløp. Kontroller beløpet til utbetaling")
            }
            oppdrag.map { Periode(it.fom, it.tom) }.any { oppdrag -> simuleringResultat.perioder.none { oppdrag.overlapperMed(it.periode) } } -> {
                warn("Simulering inneholder ikke alle periodene som skal betales")
            }
            oppdrag.erForskjelligFra(simuleringResultat) -> {
                warn("Simulering har endret dagsats eller antall på én eller flere utbetalingslinjer")
            }
        }
    }

    class SimuleringResultat(
        internal val totalbeløp: Int,
        internal val perioder: List<SimulertPeriode>
    )

    class SimulertPeriode(
        internal val periode: Periode,
        internal val utbetalinger: List<SimulertUtbetaling>
    )

    class SimulertUtbetaling(
        internal val forfallsdato: LocalDate,
        internal val utbetalesTil: Mottaker,
        internal val feilkonto: Boolean,
        internal val detaljer: List<Detaljer>
    )

    class Detaljer(
        internal val periode: Periode,
        internal val konto: String,
        internal val beløp: Int,
        internal val klassekode: Klassekode,
        internal val uføregrad: Int,
        internal val utbetalingstype: String,
        internal val tilbakeføring: Boolean,
        internal val sats: Sats,
        internal val refunderesOrgnummer: String
    )

    class Sats(
        internal val sats: Int,
        internal val antall: Int,
        internal val type: String
    )

    class Klassekode(
        internal val kode: String,
        internal val beskrivelse: String
    )

    class Mottaker(
        internal val id: String,
        internal val navn: String
    )
}
