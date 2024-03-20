package no.nav.helse.spleis.graphql

import no.nav.helse.person.PersonObserver
import no.nav.helse.serde.api.SpekematDTO
import no.nav.helse.spekemat.fabrikk.Pølse
import no.nav.helse.spekemat.fabrikk.Pølsefabrikk
import no.nav.helse.spekemat.fabrikk.PølseradDto
import no.nav.helse.spekemat.fabrikk.Pølsestatus

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

    override fun nyGenerasjon(event: PersonObserver.GenerasjonOpprettetEvent) {
        hendelser.add(event)
        arbeidsgivere.getOrPut(event.organisasjonsnummer) { Pølsefabrikk() }
            .nyPølse(
                Pølse(
                    vedtaksperiodeId = event.vedtaksperiodeId,
                    generasjonId = event.generasjonId,
                    behandlingId = event.generasjonId,
                    status = Pølsestatus.ÅPEN,
                    kilde = event.kilde.meldingsreferanseId
                )
            )
    }

    override fun generasjonLukket(event: PersonObserver.GenerasjonLukketEvent) {
        hendelser.add(event)
        arbeidsgivere.getValue(event.organisasjonsnummer)
            .oppdaterPølse(event.vedtaksperiodeId, event.generasjonId, Pølsestatus.LUKKET)
    }

    override fun generasjonForkastet(event: PersonObserver.GenerasjonForkastetEvent) {
        hendelser.add(event)
        arbeidsgivere.getValue(event.organisasjonsnummer)
            .oppdaterPølse(event.vedtaksperiodeId, event.generasjonId, Pølsestatus.FORKASTET)
    }
}