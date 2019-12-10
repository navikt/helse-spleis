package no.nav.helse.hendelser.påminnelse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.VedtaksperiodeHendelse

class Påminnelse(private val json: JsonNode): ArbeidstakerHendelse, VedtaksperiodeHendelse {
    internal val antallGangerPåminnet get() = json["antallGangerPåminnet"].intValue()

    override fun aktørId(): String = json["aktørId"].textValue()

    override fun fødselsnummer() = json["fødselnummer"].textValue()

    override fun organisasjonsnummer() = json["organisasjonsnummer"].textValue()
    override fun vedtaksperiodeId(): String = json["vedtaksperiodeId"].textValue()

}
