package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.til
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Inntekt.Companion.årlig

internal class SkjønnsmessigFastsettelseMessage(packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val arbeidsgiveropplysninger = packet["arbeidsgivere"].asArbeidsgiveropplysninger()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) =
        mediator.behandle(
            this, SkjønnsmessigFastsettelse(
                meldingsreferanseId = id,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsgiveropplysninger = arbeidsgiveropplysninger,
                opprettet = opprettet
            ),
            context
        )

    private fun JsonNode.asArbeidsgiveropplysninger() = map { arbeidsgiveropplysning ->
        val orgnummer = arbeidsgiveropplysning["organisasjonsnummer"].asText()
        val årlig = arbeidsgiveropplysning["årlig"].asDouble().årlig

        val skjønnsmessigFastsattInntekt =
            SkjønnsmessigFastsatt(skjæringstidspunkt, id, årlig, opprettet)

        ArbeidsgiverInntektsopplysning(orgnummer, skjæringstidspunkt til LocalDate.MAX, skjønnsmessigFastsattInntekt, Refusjonsopplysninger())
    }
}


