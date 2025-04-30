package no.nav.helse.hendelser

import java.time.LocalDateTime
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.MinimumSykdomsgradsvurdering

/**
 * Melding om perioder saksbehandler har vurdert dithet at bruker har tapt nok arbeidstid til å ha rett på sykepenger,
 * tross < 20% tapt inntekt
 */
class MinimumSykdomsgradsvurderingMelding(
    private val perioderMedMinimumSykdomsgradVurdertOK: Set<Periode>,
    private val perioderMedMinimumSykdomsgradVurdertIkkeOK: Set<Periode>,
    meldingsreferanseId: MeldingsreferanseId
) : Hendelse {

    init {
        sjekkForOverlapp()
    }

    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

    internal fun oppdater(vurdering: MinimumSykdomsgradsvurdering) {
        vurdering.leggTil(perioderMedMinimumSykdomsgradVurdertOK)
        vurdering.trekkFra(perioderMedMinimumSykdomsgradVurdertIkkeOK)
        sjekkForOverlapp()
    }

    private fun sjekkForOverlapp() {
        perioderMedMinimumSykdomsgradVurdertOK.forEach {
            if (perioderMedMinimumSykdomsgradVurdertIkkeOK.contains(it)) {
                error("overlappende perioder i MinimumSykdomsgradsvurdering! $it er vurdert OK, men også vurdert til IKKE å være OK")
            }
        }
    }


    internal fun periodeForEndring(): Periode {
        val alle = perioderMedMinimumSykdomsgradVurdertOK + perioderMedMinimumSykdomsgradVurdertIkkeOK
        return Periode(alle.minOf { it.start }, alle.maxOf { it.endInclusive })
    }

    fun valider(): Boolean {
        if (perioderMedMinimumSykdomsgradVurdertOK.isEmpty() && perioderMedMinimumSykdomsgradVurdertIkkeOK.isEmpty()) return false
        if (perioderMedMinimumSykdomsgradVurdertOK.containsAll(perioderMedMinimumSykdomsgradVurdertIkkeOK) && perioderMedMinimumSykdomsgradVurdertIkkeOK.containsAll(
                perioderMedMinimumSykdomsgradVurdertOK
            )
        ) return false
        return true
    }

}
