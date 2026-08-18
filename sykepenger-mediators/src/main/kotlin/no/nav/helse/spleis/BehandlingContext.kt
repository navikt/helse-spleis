package no.nav.helse.spleis

import java.sql.Connection
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.utboks.InnkommendeMelding
import no.nav.helse.spleis.utboks.Utboks
import no.nav.helse.spleis.utboks.UtboksDao
import no.nav.helse.spleis.utboks.Utboksmelding
import no.nav.helse.spleis.utboks.Utsender

internal class BehandlingContext(
    message: HendelseMessage,
    utsender: Utsender,
    utboksDao: UtboksDao
) {
    private val utboks = Utboks(
        utsender = utsender,
        innkommendeMelding = InnkommendeMelding(
            navn = message.navn,
            meldingsreferanseId = message.meldingsporing.id,
            personidentifikator = Personidentifikator(message.meldingsporing.fødselsnummer),
            opprettet = message.opprettet,
            behov = message.behov
        ),
        utboksDao = utboksDao
    )
    fun sendMeldingerIUtboks() = utboks.send()
    fun lagreMeldingerIUtboks(connection: Connection) = utboks.lagre(connection)
    fun leggIUtboks(block: (personidentifikator: Personidentifikator) -> Utboksmelding) = utboks.nyMelding(block)
}
