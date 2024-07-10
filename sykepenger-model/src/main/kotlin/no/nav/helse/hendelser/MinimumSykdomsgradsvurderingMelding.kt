package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.MinimumSykdomsgradsvurdering
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

/**
 * Melding om perioder saksbehandler har vurdert dithet at bruker har tapt nok arbeidstid til å ha rett på sykepenger,
 * tross < 20% tapt inntekt
 */
class MinimumSykdomsgradsvurderingMelding(
    private val perioderMedMinimumSykdomsgradVurdertOK: Set<Periode>,
    private val perioderMedMinimumSykdomsgradVurdertIkkeOK: Set<Periode>,
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg()) {

    init {
        sjekkForOverlapp()
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