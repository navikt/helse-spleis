package no.nav.helse.opptjening.domain

import java.time.Instant
import java.time.LocalDate

/**
 * Resultatet av en fullført vilkårsprøving.
 *
 * En vilkårsvurdering er immutabel og finnes aldri i en delvis tilstand: er den først konstruert,
 * er den komplett. Prosessen som ledet fram til den er modellert separat, se [Opptjeningsprøving].
 */
internal sealed interface Vilkårsvurdering {
    val id: VurderingId

    /** Prøvingen som produserte vurderingen. */
    val prøvingId: PrøvingId
    val fødselsnummer: String
    val skjæringstidspunkt: LocalDate
    val kodeverkkode: Kodeverkkode
    val kilde: Kilde
    val vurdertTidspunkt: Instant

    val utfall: Utfall get() = kodeverkkode.utfall
}

/** Hvem som vurderte, og på hvilket regelverk. Manuell vurdering er en kilde, ikke en egen resultattype. */
internal sealed interface Kilde {
    data class Automatisk(val regelversjon: String) : Kilde
    data class Manuell(val saksbehandlerIdent: String, val fritekstbegrunnelse: String) : Kilde
}
