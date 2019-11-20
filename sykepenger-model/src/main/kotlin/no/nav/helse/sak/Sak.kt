package no.nav.helse.sak

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import java.util.UUID

private const val CURRENT_SKJEMA_VERSJON = 2

class Sak(val aktørId: String) : VedtaksperiodeObserver {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()
    private var skjemaVersjon = CURRENT_SKJEMA_VERSJON

    private val sakObservers = mutableListOf<SakObserver>()
    fun håndter(nySøknadHendelse: NySøknadHendelse) {
        if (!nySøknadHendelse.kanBehandles()) {
            throw UtenforOmfangException("kan ikke behandle ny søknad", nySøknadHendelse)
        }
        finnEllerOpprettArbeidsgiver(nySøknadHendelse).håndter(nySøknadHendelse)
    }

    fun håndter(sendtSøknadHendelse: SendtSøknadHendelse) {
        if (!sendtSøknadHendelse.kanBehandles()) {
            throw UtenforOmfangException("kan ikke behandle sendt søknad", sendtSøknadHendelse)
        }

        if (harVedtaksperioderForAndreArbeidsgivere(sendtSøknadHendelse)) {
            invaliderAlleSaker(sendtSøknadHendelse)
            throw UtenforOmfangException("kan ikke behandle sendt søknad", sendtSøknadHendelse)
        }

        finnEllerOpprettArbeidsgiver(sendtSøknadHendelse).håndter(sendtSøknadHendelse)
    }

    private fun harVedtaksperioderForAndreArbeidsgivere(sendtSøknadHendelse: SendtSøknadHendelse): Boolean {
        return arbeidsgivere.size > 1 && arbeidsgivere.filterValues {
            it.organisasjonsnummer != sendtSøknadHendelse.organisasjonsnummer()
        }.filterValues {
            it.tellVedtaksperioderSomIkkeErINySoknadTilstand() > 0
        }.isNotEmpty()
    }

    fun håndter(inntektsmeldingHendelse: InntektsmeldingHendelse) {
        if (!inntektsmeldingHendelse.kanBehandles()) {
            invaliderAlleSaker(inntektsmeldingHendelse)
            throw UtenforOmfangException("kan ikke behandle inntektsmelding", inntektsmeldingHendelse)
        }
        finnEllerOpprettArbeidsgiver(inntektsmeldingHendelse).håndter(inntektsmeldingHendelse)
    }

    fun håndter(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        finnArbeidsgiver(sykepengehistorikkHendelse)?.håndter(sykepengehistorikkHendelse)
    }

    fun håndter(manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
        finnArbeidsgiver(manuellSaksbehandlingHendelse)?.håndter(manuellSaksbehandlingHendelse)
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
        sakObservers.forEach {
            it.sakEndret(
                SakObserver.SakEndretEvent(
                    aktørId = aktørId,
                    sykdomshendelse = event.sykdomshendelse,
                    memento = memento()
                )
            )
        }
    }

    fun addObserver(observer: SakObserver) {
        sakObservers.add(observer)
        arbeidsgivere.values.forEach { it.addObserver(observer) }
    }

    private fun invaliderAlleSaker(arbeidstakerHendelse: ArbeidstakerHendelse) {
        arbeidsgivere.forEach { (_, arbeidsgiver) ->
            arbeidsgiver.invaliderSaker(arbeidstakerHendelse)
        }
    }

    private fun finnArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { arbeidsgivere[it] }

    private fun finnEllerOpprettArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.getOrPut(orgnr) {
                arbeidsgiver(orgnr)
            }
        }

    private fun arbeidsgiver(organisasjonsnummer: String) =
        Arbeidsgiver(organisasjonsnummer, UUID.randomUUID()).also {
            it.addObserver(this)
            sakObservers.forEach { sakObserver ->
                it.addObserver(sakObserver)
            }
        }

    internal inner class Arbeidsgiver(val organisasjonsnummer: String, val id: UUID) {

        internal constructor(arbeidsgiverJson: ArbeidsgiverJson) : this(
            arbeidsgiverJson.organisasjonsnummer,
            arbeidsgiverJson.id
        ) {
            saker.addAll(arbeidsgiverJson.saker.map { Vedtaksperiode.fromJson(it) })
        }

        private val saker = mutableListOf<Vedtaksperiode>()

        private val vedtaksperiodeObservers = mutableListOf<VedtaksperiodeObserver>()

        internal fun tellVedtaksperioderSomIkkeErINySoknadTilstand() = saker.count { it.erIkkeINySøknadTilstand() }

        fun håndter(nySøknadHendelse: NySøknadHendelse) {
            if (saker.none { it.håndter(nySøknadHendelse) }) {
                nyttVedtaksperiode().håndter(nySøknadHendelse)
            }
        }

        fun håndter(sendtSøknadHendelse: SendtSøknadHendelse) {
            if (saker.none { it.håndter(sendtSøknadHendelse) }) {
                nyttVedtaksperiode().håndter(sendtSøknadHendelse)
            }
        }

        fun håndter(inntektsmeldingHendelse: InntektsmeldingHendelse) {
            if (saker.none { it.håndter(inntektsmeldingHendelse) }) {
                nyttVedtaksperiode().håndter(inntektsmeldingHendelse)
            }
        }

        internal fun håndter(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
            saker.forEach { it.håndter(sykepengehistorikkHendelse) }
        }

        internal fun håndter(manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
            saker.forEach { it.håndter(manuellSaksbehandlingHendelse) }
        }

        internal fun invaliderSaker(hendelse: ArbeidstakerHendelse) {
            saker.forEach { it.invaliderSak(hendelse) }
        }

        fun addObserver(observer: VedtaksperiodeObserver) {
            vedtaksperiodeObservers.add(observer)
            saker.forEach { it.addVedtaksperiodeObserver(observer) }
        }

        private fun nyttVedtaksperiode(): Vedtaksperiode {
            return Vedtaksperiode(UUID.randomUUID(), aktørId, organisasjonsnummer).also {
                vedtaksperiodeObservers.forEach(it::addVedtaksperiodeObserver)
                saker.add(it)
            }
        }

        internal fun jsonRepresentation(): ArbeidsgiverJson {
            return ArbeidsgiverJson(
                organisasjonsnummer = organisasjonsnummer,
                saker = saker.map { it.jsonRepresentation() },
                id = id
            )
        }

    }

    private fun memento() =
        Memento(objectMapper.writeValueAsString(jsonRepresentation()))

    private fun jsonRepresentation(): SakJson {
        return SakJson(
            aktørId = aktørId,
            skjemaVersjon = skjemaVersjon,
            arbeidsgivere = arbeidsgivere.map { it.value.jsonRepresentation() }
        )
    }

    data class Memento(private val json: String) {
        override fun toString() = json
    }

    override fun toString() = memento().toString()

    internal data class ArbeidsgiverJson(
        val organisasjonsnummer: String,
        val saker: List<Vedtaksperiode.VedtaksperiodeJson>,
        val id: UUID
    )

    private data class SakJson(
        val aktørId: String,
        val skjemaVersjon: Int,
        val arbeidsgivere: List<ArbeidsgiverJson>
    )

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): Sak {
            val sakJson: SakJson = objectMapper.readValue(json)
            if (sakJson.skjemaVersjon < CURRENT_SKJEMA_VERSJON) {
                throw SakskjemaForGammelt(sakJson.aktørId, sakJson.skjemaVersjon, CURRENT_SKJEMA_VERSJON)
            }
            return Sak(sakJson.aktørId)
                .apply {
                    arbeidsgivere.putAll(sakJson.arbeidsgivere
                        .map {
                            it.organisasjonsnummer to Arbeidsgiver(it).also { arbeidsgiver ->
                                arbeidsgiver.addObserver(this)
                            }
                        })
                }
        }
    }
}
