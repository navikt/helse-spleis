package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Personopplysninger

// Understands a JSON message representing a Søknad
internal abstract class SøknadMessage(
    private val packet: JsonMessage,
    private val builder: SøknadBuilder,
    private val organisasjonsnummer: String = packet["arbeidsgiver.orgnummer"].asText()
) :
    HendelseMessage(packet) {

    private val sykmeldingSkrevet = packet["sykmeldingSkrevet"].asLocalDateTime()
    final override val fødselsnummer = packet["fnr"].asText()

    final override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        val personopplysninger = Personopplysninger(
            personidentifikator = fødselsnummer.somPersonidentifikator(),
            aktørId = packet["aktorId"].asText(),
            fødselsdato = packet["fødselsdato"].asLocalDate(),
            dødsdato = packet["dødsdato"].asOptionalLocalDate()
        )
        bygg()
        _behandle(mediator, personopplysninger, packet, context)
    }

    protected abstract fun _behandle(mediator: IHendelseMediator, personopplysninger: Personopplysninger, packet: JsonMessage, context: MessageContext)

    private fun bygg() {
        builder.meldingsreferanseId(this.id)
            .fnr(fødselsnummer)
            .aktørId(packet["aktorId"].asText())
            .fødselsdato(packet["fødselsdato"].asLocalDate())
            .sykmeldingSkrevet(sykmeldingSkrevet)
            .organisasjonsnummer(organisasjonsnummer)
            .fom(packet["fom"].asLocalDate())
            .tom(packet["tom"].asLocalDate())
            .arbeidUtenforNorge(packet["arbeidUtenforNorge"].asBoolean())
            .yrkesskade(packet["yrkesskade"].asBoolean())

        packet["soknadsperioder"].forEach {
            val arbeidshelse = it.path("faktiskGrad")
                .takeIf(JsonNode::isIntegralNumber)
                ?.asInt()
                ?.coerceIn(0, 100)
            builder.periode(
                fom = it.path("fom").asLocalDate(),
                tom = it.path("tom").asLocalDate(),
                grad = it.path("sykmeldingsgrad").asInt(),
                arbeidshelse = arbeidshelse
            )
        }
    }
}
