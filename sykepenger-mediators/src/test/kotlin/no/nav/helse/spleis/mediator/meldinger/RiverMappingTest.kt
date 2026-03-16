package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlin.reflect.KClass
import kotlin.reflect.safeCast
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.HendelseMessage

internal abstract class RiverMappingTest<H: HendelseMessage>(
    private val hendelse: KClass<H>,
    registrer: (rapidsConnection: RapidsConnection, messageMediator: IMessageMediator) -> Unit
) {

    private var forrigeHendelseMessage: HendelseMessage? = null

    private val testMessageMediator = object: IMessageMediator {
        override fun onRecognizedMessage(message: HendelseMessage, context: MessageContext) {
            forrigeHendelseMessage = message
        }
        override fun onRiverError(riverName: String, problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
            forrigeHendelseMessage = null
        }
    }

    private val rapid = TestRapid().apply {
        registrer(this, testMessageMediator)
    }

    protected fun sendJson(json: String): H {
        forrigeHendelseMessage = null
        rapid.sendTestMessage(json)
        checkNotNull(forrigeHendelseMessage) { "Det ble ikke registrert noen hendelse fra denne meldingen!" }
        return checkNotNull(hendelse.safeCast(forrigeHendelseMessage)) { "Forventet ${hendelse.simpleName}, men var ${hendelse::class.simpleName}" }
    }
}
