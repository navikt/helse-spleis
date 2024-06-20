package no.nav.helse.hendelser

import no.nav.helse.person.MinimumSykdomsgradsvurdering

/**
 * Melding om perioder saksbehandler har vurdert dithet at bruker har tapt nok arbeidstid til å ha rett på sykepenger,
 * tross < 20% tapt inntekt
 */
class MinimumSykdomsgradsvurderingMelding(
    private val perioderMedTilstrekkeligTaptArbeidstid: Set<Periode>,
    private val perioderUtenTilstrekkeligTaptArbeidstid: Set<Periode>
) {

    internal fun apply(vurdering: MinimumSykdomsgradsvurdering) {
        vurdering.leggTil(perioderMedTilstrekkeligTaptArbeidstid)
        vurdering.trekkFra(perioderUtenTilstrekkeligTaptArbeidstid)
    }

}