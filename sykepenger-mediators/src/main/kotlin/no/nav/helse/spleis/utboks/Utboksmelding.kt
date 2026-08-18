package no.nav.helse.spleis.utboks

internal sealed interface Utboksmelding {
    val utgåendeMelding: UtgåendeMelding
    data class BeholdEtterSending(override val utgåendeMelding: UtgåendeMelding) : Utboksmelding
    data class ForkastEtterSending(override val utgåendeMelding: UtgåendeMelding) : Utboksmelding
}
