package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.domain.Opptjeningsprøving

/**
 * Lager for pågående prøvinger. Invarianten "kun én aktiv prøving per (fødselsnummer,
 * skjæringstidspunkt)" håndheves av lageret selv — i en relasjonsdatabase av et partielt unikt
 * indeks over de aktive tilstandene, ikke av en sjekk i applikasjonskoden.
 */
internal interface VilkårsprøvingRepository {
    /** Kaster dersom det allerede finnes en aktiv prøving for samme fødselsnummer og skjæringstidspunkt. */
    fun opprett(prøving: Opptjeningsprøving)

    fun oppdater(prøving: Opptjeningsprøving)

    fun finnSiste(fødselsnummer: String, skjæringstidspunkt: LocalDate): Opptjeningsprøving?
}
