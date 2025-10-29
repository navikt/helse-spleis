package no.nav.helse.spleis.graphql

import no.nav.helse.person.EventSubscription
import no.nav.helse.spekemat.fabrikk.Pølse
import no.nav.helse.spekemat.fabrikk.Pølsefabrikk
import no.nav.helse.spekemat.fabrikk.PølseradDto
import no.nav.helse.spekemat.fabrikk.Pølsestatus
import no.nav.helse.spleis.speil.SpekematDTO
import no.nav.helse.spleis.testhelpers.somOrganisasjonsnummer

class Spekemat : EventSubscription {
    private val hendelser = mutableListOf<Any>()
    private val arbeidsgivere = mutableMapOf<String, Pølsefabrikk>()

    fun resultat() = SpekematDTO(
        pakker = arbeidsgivere.mapNotNull { resultat(it.key) }
    )

    fun resultat(orgnr: String) = arbeidsgivere.getValue(orgnr).pakke().mapTilPølsepakkeDTO(orgnr)

    private fun List<PølseradDto>.mapTilPølsepakkeDTO(orgnr: String) =
        SpekematDTO.PølsepakkeDTO(
            yrkesaktivitetidentifikator = orgnr,
            rader = map { rad ->
                SpekematDTO.PølsepakkeDTO.PølseradDTO(
                    kildeTilRad = rad.kildeTilRad,
                    pølser = rad.pølser.map { pølseDto ->
                        SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO(
                            vedtaksperiodeId = pølseDto.vedtaksperiodeId,
                            behandlingId = pølseDto.behandlingId,
                            kilde = pølseDto.kilde,
                            status = when (pølseDto.status) {
                                Pølsestatus.ÅPEN -> SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO.PølsestatusDTO.ÅPEN
                                Pølsestatus.LUKKET -> SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO.PølsestatusDTO.LUKKET
                                Pølsestatus.FORKASTET -> SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO.PølsestatusDTO.FORKASTET
                            }
                        )
                    }
                )
            }
        )

    override fun nyBehandling(event: EventSubscription.BehandlingOpprettetEvent) {
        hendelser.add(event)
        arbeidsgivere.getOrPut(event.yrkesaktivitetssporing.somOrganisasjonsnummer) { Pølsefabrikk() }
            .nyPølse(
                Pølse(
                    vedtaksperiodeId = event.vedtaksperiodeId,
                    behandlingId = event.behandlingId,
                    status = Pølsestatus.ÅPEN,
                    kilde = event.kilde.meldingsreferanseId
                )
            )
    }

    override fun behandlingLukket(event: EventSubscription.BehandlingLukketEvent) {
        hendelser.add(event)
        arbeidsgivere.getValue(event.yrkesaktivitetssporing.somOrganisasjonsnummer)
            .oppdaterPølse(event.vedtaksperiodeId, event.behandlingId, Pølsestatus.LUKKET)
    }

    override fun behandlingForkastet(event: EventSubscription.BehandlingForkastetEvent) {
        hendelser.add(event)
        arbeidsgivere.getValue(event.yrkesaktivitetssporing.somOrganisasjonsnummer)
            .oppdaterPølse(event.vedtaksperiodeId, event.behandlingId, Pølsestatus.FORKASTET)
    }
}
