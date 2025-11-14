package no.nav.helse.person

import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt

internal sealed interface Mjau {
    val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt?

    data class AvklartInntektOgRefusjon(override val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt) : Mjau

    data class ManglerRefusjon(override val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt) : Mjau

    data object GirOppÅVentePåArbeidsgiver : Mjau {
        override val faktaavklartInntekt = null
    }

    data object TidligereVilkårsprøvd : Mjau {
        override val faktaavklartInntekt = null
    }

    data object ManglerSkjæringstidspunkt : Mjau {
        override val faktaavklartInntekt = null
    }

    data object ManglerInntektOgRefusjon : Mjau {
        override val faktaavklartInntekt = null
    }
}
