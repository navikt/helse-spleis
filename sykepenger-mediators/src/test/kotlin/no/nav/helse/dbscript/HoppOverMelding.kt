package no.nav.helse.dbscript

import java.util.UUID

internal object HoppOverMelding : DbScript() {
    override val beskrivelse = "Hopper over en melding på rapiden som Spleis typisk feiler på"

    override fun start(connectionInfo: ConnectionInfo) {
        println("## Velkommen til overhopping av melding")
        println(" - Dette blir gøyalt, men ikke like gøyalt som Personeditor 🎢")

        println("## Fyll inn fødselsnummer på personen det skal endres på")
        val fødselsnummer = Input.ventPåFødselsnummer()
        println()

        println("## Fyll inn hendelseId/meldingId/meldingsreferanseId (kjært barn har mange navn - @id UUID'en hvert fall..) på meldingen du vil hoppe over")
        val meldingId = Input.ventPåInput { runCatching { UUID.fromString(it) }.isSuccess }
        println()

        println("## Beskriv _hvorfor_ du gjør denne endringen (for auditlog) - minst 15 makreller lang 🤏")
        val beskrivelse = Input.ventPåBeskrivelse()
        println()

        databaseTransaksjon(connectionInfo) {
            check(
                1 ==
                    prepareStatement("UPDATE melding SET behandlet_tidspunkt=now() WHERE fnr=? AND melding_id=? AND behandlet_tidspunkt IS NULL").use { stmt ->
                        stmt.setLong(1, fødselsnummer.toLong())
                        stmt.setString(2, meldingId)
                        stmt.executeUpdate()
                    },
            ) { "forventet å oppdatere nøyaktig én rad ved å hoppe over en melding" }
        }
        println(" - Endringene dine er live ✅")
        gaal("Meldingen med meldingId '$meldingId' har fått 'behandlet_tidspunkt' satt til now() slik at Spleis hopper over meldingen og kan prosessere andre meldinger som står i kø. $beskrivelse")
    }
}
