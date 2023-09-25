package no.nav.helse.spleis.e2e


import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.serde.api.serializePersonForSpeil

internal class EtterspurtBehov(
    private val type: Aktivitet.Behov.Behovtype,
    private val tilstand: TilstandType,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID
) {
    companion object {
        internal fun fjern(liste: MutableList<EtterspurtBehov>, orgnummer: String, type: Aktivitet.Behov.Behovtype) {
            liste.removeIf { it.orgnummer == orgnummer && it.type == type }
        }

        internal fun finnEtterspurteBehov(behovsliste: List<Aktivitet.Behov>) =
            behovsliste
                .filter { "tilstand" in it.kontekst() }
                .filter { "organisasjonsnummer" in it.kontekst() }
                .filter { "vedtaksperiodeId" in it.kontekst() }
                .map {
                    EtterspurtBehov(
                        type = it.type,
                        tilstand = enumValueOf(it.kontekst()["tilstand"] as String),
                        orgnummer = (it.kontekst()["organisasjonsnummer"] as String),
                        vedtaksperiodeId = UUID.fromString(it.kontekst()["vedtaksperiodeId"] as String)
                    )
                }

        internal fun finnEtterspurtBehov(
            ikkeBesvarteBehov: MutableList<EtterspurtBehov>,
            type: Aktivitet.Behov.Behovtype,
            vedtaksperiodeIdInnhenter: IdInnhenter,
            orgnummer: String
        ) =
            ikkeBesvarteBehov.firstOrNull { it.type == type && it.orgnummer == orgnummer && it.vedtaksperiodeId == vedtaksperiodeIdInnhenter.id(orgnummer) }

        internal fun finnEtterspurtBehov(
            ikkeBesvarteBehov: MutableList<EtterspurtBehov>,
            type: Aktivitet.Behov.Behovtype,
            vedtaksperiodeIdInnhenter: IdInnhenter,
            orgnummer: String,
            tilstand: TilstandType
        ) =
            ikkeBesvarteBehov.firstOrNull {
                it.type == type && it.orgnummer == orgnummer && it.vedtaksperiodeId == vedtaksperiodeIdInnhenter.id(orgnummer) && it.tilstand == tilstand
            }
    }

    override fun toString() = "$type ($tilstand)"
}

internal fun AbstractEndToEndTest.finnSkjæringstidspunkt(orgnummer: String, vedtaksperiodeIdInnhenter: IdInnhenter) =
    inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)

internal fun AbstractEndToEndTest.speilApi(hendelser: List<HendelseDTO> = søknadDTOer + sykmeldingDTOer + inntektsmeldingDTOer) = serializePersonForSpeil(person, hendelser)

internal val AbstractEndToEndTest.søknadDTOer get() = søknader.map { (id, triple) ->
    val søknadsperiode = Søknadsperiode.søknadsperiode(triple.third.toList())!!
    HendelseDTO.sendtSøknadNav(
        id = id.toString(),
        eksternDokumentId = UUID.randomUUID().toString(),
        fom = søknadsperiode.first(),
        tom = søknadsperiode.last(),
        rapportertdato = triple.first.atStartOfDay(),
        sendtNav = triple.first.atStartOfDay()
    )
}

private val AbstractEndToEndTest.sykmeldingDTOer get() = sykmeldinger.map { (id, perioder) ->
    val sykmeldingsperiode = Sykmeldingsperiode.periode(perioder.toList())!!
    HendelseDTO.nySøknad(
        id = id.toString(),
        eksternDokumentId = UUID.randomUUID().toString(),
        fom = sykmeldingsperiode.first(),
        tom = sykmeldingsperiode.last(),
        rapportertdato = sykmeldingsperiode.last().atStartOfDay()
    )
}

private val AbstractEndToEndTest.inntektsmeldingDTOer get() = inntektsmeldinger.map { (id, _) ->
    HendelseDTO.inntektsmelding(
        id = id.toString(),
        eksternDokumentId = UUID.randomUUID().toString(),
        mottattDato = LocalDateTime.now(),
        beregnetInntekt = inntekter.getValue(id).reflection { årlig, _, _, _ -> årlig }
    )
}
