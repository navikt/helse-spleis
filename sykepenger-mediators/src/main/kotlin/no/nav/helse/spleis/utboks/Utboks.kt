package no.nav.helse.spleis.utboks

internal class Utboks {
    private val utgåendeMeldinger = mutableListOf<UtgåendeMelding>()

    fun nyMelding(melding: UtgåendeMelding) {
        utgåendeMeldinger.add(melding)
    }
}
