package no.nav.helse.spleis

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.*
import no.nav.helse.person.ArbeidstakerHendelse
import org.slf4j.LoggerFactory

// Understands how to translate json messages to events
// Uses GoF observer pattern to notify about events
// Uses GoF builder pattern to build the events from json
internal class HendelseDirector(stream: HendelseStream): HendelseStream.MessageListener {

    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

    private val listeners = mutableListOf<HendelseListener>()
    private val builders = mutableListOf<ArbeidstakerHendelseBuilder>()

    init {
        stream.addListener(this)

        builders.add(Inntektsmelding.Builder())
        builders.add(NySøknad.Builder())
        builders.add(SendtSøknad.Builder())
        builders.add(Ytelser.Builder())
        builders.add(Vilkårsgrunnlag.Builder())
        builders.add(ManuellSaksbehandling.Builder())
        builders.add(Påminnelse.Builder())
    }

    fun addListener(listener: HendelseListener) {
        listeners.add(listener)
    }

    override fun onMessage(message: String) {
        val hendelse = builders.fold<ArbeidstakerHendelseBuilder, ArbeidstakerHendelse?>(null) { result, builder ->
            result ?: builder.build(message)
        } ?: return listeners.forEach { it.onUnprocessedMessage(message) }

        sikkerLogg.info(message, keyValue("hendelsetype", hendelse.hendelsetype().name))

        listeners.forEach {
            when (hendelse) {
                is Inntektsmelding -> it.onInntektsmelding(hendelse)
                is NySøknad -> it.onNySøknad(hendelse)
                is SendtSøknad -> it.onSendtSøknad(hendelse)
                is Ytelser -> it.onYtelser(hendelse)
                is Påminnelse -> it.onPåminnelse(hendelse)
                is Vilkårsgrunnlag -> it.onVilkårsgrunnlag(hendelse)
                is ManuellSaksbehandling -> it.onManuellSaksbehandling(hendelse)
            }
        }
    }
}
