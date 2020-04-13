package no.nav.helse.spleis

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.model.*

internal class HendelseProcessor(private val hendelseMediator: HendelseMediator) : MessageProcessor {
    override fun process(message: NySøknadMessage) {
        håndter(message, message.asSykmelding()) { person, sykmelding ->
            HendelseProbe.onSykmelding()
            person.håndter(sykmelding)
        }
    }

    override fun process(message: SendtSøknadArbeidsgiverMessage) {
        håndter(message, message.asSøknadArbeidsgiver()) { person, søknad ->
            HendelseProbe.onSøknadArbeidsgiver()
            person.håndter(søknad)
        }
    }

    override fun process(message: SendtSøknadNavMessage) {
        håndter(message, message.asSøknad()) { person, søknad ->
            HendelseProbe.onSøknadNav()
            person.håndter(søknad)
        }
    }

    override fun process(message: InntektsmeldingMessage) {
        håndter(message, message.asInntektsmelding()) { person, inntektsmelding ->
            HendelseProbe.onInntektsmelding()
            person.håndter(inntektsmelding)
        }
    }

    override fun process(message: YtelserMessage) {
        håndter(message, message.asYtelser()) { person, ytelser ->
            HendelseProbe.onYtelser()
            person.håndter(ytelser)
        }
    }

    override fun process(message: VilkårsgrunnlagMessage) {
        håndter(message, message.asVilkårsgrunnlag()) { person, vilkårsgrunnlag ->
            HendelseProbe.onVilkårsgrunnlag()
            person.håndter(vilkårsgrunnlag)
        }
    }

    override fun process(message: SimuleringMessage) {
        håndter(message, message.asSimulering()) { person, simulering ->
            HendelseProbe.onSimulering()
            person.håndter(simulering)
        }
    }

    override fun process(message: ManuellSaksbehandlingMessage) {
        håndter(message, message.asManuellSaksbehandling()) { person, manuellSaksbehandling ->
            HendelseProbe.onManuellSaksbehandling()
            person.håndter(manuellSaksbehandling)
        }
    }

    override fun process(message: UtbetalingMessage) {
        håndter(message, message.asUtbetaling()) { person, utbetaling ->
            HendelseProbe.onUtbetaling()
            person.håndter(utbetaling)
        }
    }

    override fun process(message: PåminnelseMessage) {
        håndter(message, message.asPåminnelse()) { person, påminnelse ->
            HendelseProbe.onPåminnelse(påminnelse)
            person.håndter(påminnelse)
        }
    }

    private fun <Hendelse: ArbeidstakerHendelse> håndter(message: HendelseMessage, hendelse: Hendelse, handler: (Person, Hendelse) -> Unit) {
        hendelseMediator.person(message, hendelse).also {
            handler(it, hendelse)
            hendelseMediator.finalize(it, message, hendelse)
        }
    }
}
