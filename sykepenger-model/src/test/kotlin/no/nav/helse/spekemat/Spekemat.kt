package no.nav.helse.spekemat

import no.nav.helse.Toggle
import no.nav.helse.person.PersonObserver
import no.nav.helse.serde.api.SpekematDTO
import no.nav.helse.serde.api.SpekematDTO.PølsepakkeDTO
import no.nav.helse.serde.api.SpekematDTO.PølsepakkeDTO.PølseradDTO
import no.nav.helse.serde.api.SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO
import no.nav.helse.serde.api.SpekematDTO.PølsepakkeDTO.PølseradDTO.PølseDTO.PølsestatusDTO
import no.nav.helse.spekemat.fabrikk.Pølse
import no.nav.helse.spekemat.fabrikk.Pølsefabrikk
import no.nav.helse.spekemat.fabrikk.PølseradDto
import no.nav.helse.spekemat.fabrikk.Pølsestatus

class Spekemat : PersonObserver {
    private val hendelser = mutableListOf<Any>()
    private val arbeidsgivere = mutableMapOf<String, Pølsefabrikk>()

    fun resultat() = SpekematDTO(
        pakker = arbeidsgivere.map { resultat(it.key) }
    )
    fun resultat(orgnr: String) =
        arbeidsgivere.getValue(orgnr).pakke().mapTilPølsepakkeDTO(orgnr)

    private fun List<PølseradDto>.mapTilPølsepakkeDTO(orgnr: String) =
        PølsepakkeDTO(
            yrkesaktivitetidentifikator = orgnr,
            rader = map { rad ->
                PølseradDTO(
                    kildeTilRad = rad.kildeTilRad,
                    pølser = rad.pølser.map { pølseDto ->
                        PølseDTO(
                            vedtaksperiodeId = pølseDto.vedtaksperiodeId,
                            behandlingId = pølseDto.behandlingId,
                            kilde = pølseDto.kilde,
                            status = when (pølseDto.status) {
                                Pølsestatus.ÅPEN -> PølsestatusDTO.ÅPEN
                                Pølsestatus.LUKKET -> PølsestatusDTO.LUKKET
                                Pølsestatus.FORKASTET -> PølsestatusDTO.FORKASTET
                            }
                        )
                    }
                )
            }
        )

    override fun nyGenerasjon(event: PersonObserver.GenerasjonOpprettetEvent) {
        if (Toggle.Spekemat.disabled) return
        hendelser.add(event)
        arbeidsgivere.getOrPut(event.organisasjonsnummer) { Pølsefabrikk() }
            .nyPølse(Pølse(
                vedtaksperiodeId = event.vedtaksperiodeId,
                behandlingId = event.generasjonId,
                status = Pølsestatus.ÅPEN,
                kilde = event.kilde.meldingsreferanseId
            ))
    }

    override fun generasjonLukket(event: PersonObserver.GenerasjonLukketEvent) {
        if (Toggle.Spekemat.disabled) return
        hendelser.add(event)
        arbeidsgivere.getValue(event.organisasjonsnummer)
            .oppdaterPølse(event.vedtaksperiodeId, event.generasjonId, Pølsestatus.LUKKET)
    }

    override fun generasjonForkastet(event: PersonObserver.GenerasjonForkastetEvent) {
        if (Toggle.Spekemat.disabled) return
        hendelser.add(event)
        arbeidsgivere.getValue(event.organisasjonsnummer)
            .oppdaterPølse(event.vedtaksperiodeId, event.generasjonId, Pølsestatus.FORKASTET)
    }
}