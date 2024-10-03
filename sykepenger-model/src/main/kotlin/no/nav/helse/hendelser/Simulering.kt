package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag

class Simulering(
    meldingsreferanseId: UUID,
    val vedtaksperiodeId: String,
    aktørId: String,
    fødselsnummer: String,
    orgnummer: String,
    override val fagsystemId: String,
    fagområde: String,
    override val simuleringOK: Boolean,
    override val melding: String,
    override val simuleringsResultat: SimuleringResultatDto?,
    override val utbetalingId: UUID
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, orgnummer), SimuleringHendelse {
    override val fagområde = Fagområde.from(fagområde)
}
