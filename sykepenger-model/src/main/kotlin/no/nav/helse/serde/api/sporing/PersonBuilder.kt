package no.nav.helse.serde.api.sporing

import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.VedtaksperiodeUtDto
import no.nav.helse.erRettFør
import no.nav.helse.person.Person

internal class PersonBuilder(private val person: Person) {
    internal fun build(): PersonDTO {
        val dto = person.dto()
        return PersonDTO(
            fødselsnummer = dto.fødselsnummer,
            aktørId = dto.aktørId,
            arbeidsgivere = dto.arbeidsgivere.map { mapArbeidsgiver(it) },
        )
    }

    private fun mapArbeidsgiver(arbeidsgiverDto: ArbeidsgiverUtDto): ArbeidsgiverDTO {
        val perioderBuilder = arbeidsgiverDto.vedtaksperioder.map { mapVedtaksperiode(it, false) }
        val forkastetPerioderBuilder = arbeidsgiverDto.vedtaksperioder.map { mapVedtaksperiode(it, true) }

        val perioder = (perioderBuilder + forkastetPerioderBuilder).sortedBy { it.fom }.toMutableList()
        return ArbeidsgiverDTO(
            organisasjonsnummer = arbeidsgiverDto.organisasjonsnummer,
            vedtaksperioder = perioder.onEachIndexed { index, denne ->
                val erForlengelse = if (index > 0) perioder[index - 1].forlenger(denne) else false
                val erForlenget = if (perioder.size > (index + 1)) denne.forlenger(perioder[index + 1]) else false
                perioder[index] = denne.copy(periodetype = when {
                    !erForlengelse && !erForlenget -> PeriodetypeDTO.GAP_SISTE
                    !erForlengelse && erForlenget -> PeriodetypeDTO.GAP
                    erForlengelse && !erForlenget -> PeriodetypeDTO.FORLENGELSE_SISTE
                    else -> PeriodetypeDTO.FORLENGELSE
                })
            }
        )
    }

    private fun mapVedtaksperiode(dto: VedtaksperiodeUtDto, forkastet: Boolean) = VedtaksperiodeDTO(
        id = dto.id,
        fom = dto.fom,
        tom = dto.tom,
        periodetype = PeriodetypeDTO.GAP,
        forkastet = forkastet
    )

    private fun VedtaksperiodeDTO.forlenger(other: VedtaksperiodeDTO) = this.tom.erRettFør(other.fom)
}
