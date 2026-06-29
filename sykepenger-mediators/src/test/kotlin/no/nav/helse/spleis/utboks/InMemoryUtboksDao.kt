package no.nav.helse.spleis.utboks

import java.sql.Connection
import java.util.UUID
import no.nav.helse.Personidentifikator

class InMemoryUtboksDao: UtboksDao {
    private val usendte = mutableListOf<UtgåendeMelding>()

    override fun lagre(connection: Connection, meldinger: List<UtgåendeMelding>, forårsaketAv: UUID) {
        usendte.addAll(meldinger)
    }

    override fun usendte(personidentifikator: Personidentifikator, send: (meldinger: List<UtgåendeMelding>) -> Kvittering) {
        val relevante = usendte.filter { it.key in setOf(null, personidentifikator.toString()) }
        val kvittering = send(relevante)
        usendte.removeAll(kvittering.ok)
    }

    override fun personerMedUsendteMeldinger(): Set<Personidentifikator> {
        return usendte.mapNotNull { it.key }.map { Personidentifikator(it) }.toSet()
    }
}
