package no.nav.helse.dbscript

internal object HoppOverMelding: DbScript() {
    override val beskrivelse = "Hopper over en melding på rapiden som Spleis typisk feiler på"

    override fun start(connectionInfo: ConnectionInfo) {
        error("ÅÅÅ, det var synd! For dette scriptet er ikke laget enda.")
    }
}
