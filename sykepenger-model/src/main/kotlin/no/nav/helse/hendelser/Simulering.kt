package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse
import java.math.BigDecimal
import java.time.LocalDate

class Simulering(
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val simuleringOK: Boolean,
    private val melding: String,
    internal val simuleringResultat: SimuleringResultat?
) : ArbeidstakerHendelse() {
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    fun valider() = aktivitetslogg.apply {
        if (!simuleringOK) error("Feil under simulering: %s", melding)
    }

    class SimuleringResultat(
        internal val totalbeløp: BigDecimal,
        internal val perioder: List<SimulertPeriode>
    )

    class SimulertPeriode(
        internal val fom: LocalDate,
        internal val tom: LocalDate,
        internal val utbetalinger: List<SimulertUtbetaling>
    )

    class SimulertUtbetaling(
        internal val forfallsdato: LocalDate,
        internal val utbetalesTil: Mottaker,
        internal val feilkonto: Boolean,
        internal val detaljer: List<Detaljer>
    )

    class Detaljer(
        internal val fom: LocalDate,
        internal val tom: LocalDate,
        internal val konto: String,
        internal val beløp: BigDecimal,
        internal val klassekode: Klassekode,
        internal val uføregrad: Int,
        internal val utbetalingstype: String,
        internal val tilbakeføring: Boolean,
        internal val sats: Sats,
        internal val refunderesOrgnummer: String
    )

    class Sats(
        internal val sats: BigDecimal,
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
