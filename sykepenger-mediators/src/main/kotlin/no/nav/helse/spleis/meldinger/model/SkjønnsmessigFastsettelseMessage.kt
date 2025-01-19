package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.LocalDateTime
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.økonomi.Inntekt.Companion.årlig

internal class SkjønnsmessigFastsettelseMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val arbeidsgiveropplysninger = packet["arbeidsgivere"].asArbeidsgiveropplysninger()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) =
        mediator.behandle(
            this, SkjønnsmessigFastsettelse(
            meldingsreferanseId = meldingsporing.id,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiveropplysninger = arbeidsgiveropplysninger,
            opprettet = opprettet
        ),
            context
        )

    private fun JsonNode.asArbeidsgiveropplysninger() = map { arbeidsgiveropplysning ->
        val orgnummer = arbeidsgiveropplysning["organisasjonsnummer"].asText()
        val årlig = arbeidsgiveropplysning["årlig"].asDouble().årlig

        SkjønnsmessigFastsettelse.SkjønnsfastsattInntekt(orgnummer, Inntektsdata(meldingsporing.id, skjæringstidspunkt, årlig, LocalDateTime.now()))
    }
}


