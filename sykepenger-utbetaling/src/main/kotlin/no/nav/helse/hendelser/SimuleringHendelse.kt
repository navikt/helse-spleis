package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Fagområde

interface SimuleringHendelse : IAktivitetslogg {
    val utbetalingId: UUID
    val fagsystemId: String
    val fagområde: Fagområde
    val simuleringOK: Boolean
    val melding: String
    val simuleringsResultat: SimuleringResultatDto?
}