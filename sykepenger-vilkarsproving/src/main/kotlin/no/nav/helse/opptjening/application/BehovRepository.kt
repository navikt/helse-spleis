package no.nav.helse.opptjening.application

import java.util.UUID

interface OpptjeningsbehovRepository {
    fun finnUbesvart(behovId: UUID): Opptjeningsbehov?
    fun lagre(behov: Opptjeningsbehov)
}
