package no.nav.helse.spleis.graphql

import no.nav.helse.person.PersonObserver
import no.nav.helse.spekemat.fabrikk.Pølse
import no.nav.helse.spekemat.fabrikk.Pølsefabrikk
import no.nav.helse.spekemat.fabrikk.PølseradDto
import no.nav.helse.spekemat.fabrikk.Pølsestatus
import no.nav.helse.spleis.speil.SpekematDTO

class Spekemat : PersonObserver {
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

    override fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        hendelser.add(event)
        arbeidsgivere.getOrPut(event.organisasjonsnummer) { Pølsefabrikk() }
            .nyPølse(
                Pølse(
                    vedtaksperiodeId = event.vedtaksperiodeId,
                    behandlingId = event.behandlingId,
                    status = Pølsestatus.ÅPEN,
                    kilde = event.kilde.meldingsreferanseId
                )
            )
    }

    override fun behandlingLukket(event: PersonObserver.BehandlingLukketEvent) {
        hendelser.add(event)
        arbeidsgivere.getValue(event.organisasjonsnummer)
            .oppdaterPølse(event.vedtaksperiodeId, event.behandlingId, Pølsestatus.LUKKET)
    }

    override fun behandlingForkastet(event: PersonObserver.BehandlingForkastetEvent) {
        hendelser.add(event)
        arbeidsgivere.getValue(event.organisasjonsnummer)
            .oppdaterPølse(event.vedtaksperiodeId, event.behandlingId, Pølsestatus.FORKASTET)
    }
}