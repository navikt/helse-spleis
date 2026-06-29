package no.nav.helse.spleis.utboks

import java.sql.Connection
import java.util.UUID
import no.nav.helse.Personidentifikator

interface UtboksDao {

    /**
     * Lagrer ned alle meldingene i samme database-transaksjon som personen lagres ned med.
     */
    fun lagre(connection: Connection, meldinger: List<UtgåendeMelding>, forårsaketAv: UUID)

    /**
     * Henter alle usendte meldinger som er for den spesifikke personidentifikatoren,
     * eller som ikke har noen personidentifikator (key is null)
     * Metode som gjør faktisk sending av meldingen sendes inn og lagrer ned de som ble sendt OK.
     */
    fun usendte(personidentifikator: Personidentifikator, send: (meldinger: List<UtgåendeMelding>) -> Kvittering)

    /**
     * Henter alle personidentifikatorer som har usendte meldinger i utboksen.
     */
    fun personerMedUsendteMeldinger(): Set<Personidentifikator>
}
