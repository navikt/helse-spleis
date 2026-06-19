package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.sql.Connection
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.utboks.Utboks
import no.nav.helse.spleis.utboks.UtgåendeMelding

internal data class BehandlingContext(
    val messageContext: MessageContext,
    private val message: HendelseMessage
) {
    private val personidentifikator = Personidentifikator(message.meldingsporing.fødselsnummer)
    private val utboks = Utboks()

    fun sendMeldingerIUtboks() = utboks.send(messageContext, message)
    fun lagreMeldingerIUtboks(connection: Connection) = utboks.lagre(connection, message)
    fun leggIUtboks(block: (personidentifikator: Personidentifikator) -> UtgåendeMelding) {
        utboks.nyMelding(block(personidentifikator))
    }
}
