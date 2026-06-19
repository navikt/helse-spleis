package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.sql.Connection
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.utboks.Utboks

internal data class BehandlingContext(
    val messageContext: MessageContext
) {
    val utboks = Utboks()

    fun sendMeldingerIUtboks(message: HendelseMessage) = utboks.send(messageContext, message)
    fun lagreMeldingerIUtboks(connection: Connection, message: HendelseMessage) = utboks.lagre(connection, message)
}
