package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.serde.serdeObjectMapper
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag
import org.slf4j.LoggerFactory

class Simulering(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: String,
    aktørId: String,
    fødselsnummer: String,
    orgnummer: String,
    private val fagsystemId: String,
    fagområde: String,
    private val simuleringOK: Boolean,
    private val melding: String,
    val simuleringResultat: SimuleringResultat?,
    private val utbetalingId: UUID
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, orgnummer) {

    companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
    init {
        simuleringResultat?.totalbeløp?.let {
            if (it < 0) {
                sikkerlogg.info("Negativt totalbeløp på simulering: ${serdeObjectMapper.writeValueAsString(simuleringResultat.toMap())} for aktørId: ${aktørId}")
            }
        }
    }

    private val fagområde = Fagområde.from(fagområde)

    fun erRelevant(other: UUID) = other.toString() == vedtaksperiodeId
    fun erRelevantForUtbetaling(utbetalingId: UUID) = this.utbetalingId == utbetalingId
    fun erSimulert(fagområde: Fagområde, fagsystemId: String) =
        this.fagområde == fagområde && this.fagsystemId == fagsystemId && simuleringOK

    fun valider(oppdrag: Oppdrag) = this.apply {
        if (!oppdrag.erRelevant(fagsystemId, fagområde)) return@apply
        if (!simuleringOK) return@apply info("Feil under simulering: $melding")
        if (harNegativtTotalbeløp()) varsel(Varselkode.RV_SI_3)
        if (simuleringResultat == null) info("Ingenting ble simulert")
    }

    private fun harNegativtTotalbeløp() = simuleringResultat?.let { it.totalbeløp < 0  } ?: false

}
