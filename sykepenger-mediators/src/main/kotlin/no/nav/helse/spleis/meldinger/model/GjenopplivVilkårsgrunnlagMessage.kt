package no.nav.helse.spleis.meldinger.model

import java.util.UUID
import no.nav.helse.hendelser.GjenopplivVilkårsgrunnlag
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

internal class GjenopplivVilkårsgrunnlagMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    private val vilkårsgrunnlagId = packet["vilkårsgrunnlagId"].asText().let { UUID.fromString(it) }
    private val nyttSkjæringstidspunkt = packet["nyttSkjæringstidspunkt"].asOptionalLocalDate()
    private val arbeidsgiveropplysninger = packet["arbeidsgivere"].takeUnless { it.isMissingOrNull() }?.associate { it["organisasjonsnummer"].asText() to it["månedligInntekt"].asDouble().månedlig } ?: emptyMap()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()

    private val gjenopplivVilkårsgrunnlag
        get() = GjenopplivVilkårsgrunnlag(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            nyttSkjæringstidspunkt = nyttSkjæringstidspunkt,
            arbeidsgiveropplysninger = arbeidsgiveropplysninger
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, gjenopplivVilkårsgrunnlag, context)
    }
}
