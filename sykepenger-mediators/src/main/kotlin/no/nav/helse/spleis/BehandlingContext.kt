package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.sql.Connection
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.utboks.Utboks
import no.nav.helse.spleis.utboks.UtgåendeMelding
import no.nav.helse.spleis.utboks.Utsender

internal class BehandlingContext(
    val messageContext: MessageContext,
    message: HendelseMessage,
    utsender: Utsender
) {
    private val personidentifikator = Personidentifikator(message.meldingsporing.fødselsnummer)
    private val utboks = Utboks(utsender, message)

    fun sendMeldingerIUtboks() = utboks.send(messageContext)
    fun lagreMeldingerIUtboks(connection: Connection) = utboks.lagre(connection)
    fun leggIUtboks(block: (personidentifikator: Personidentifikator) -> UtgåendeMelding) {
        utboks.nyMelding(block(personidentifikator))
    }
}
