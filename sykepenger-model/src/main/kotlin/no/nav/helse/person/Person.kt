package no.nav.helse.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import java.util.*

private const val CURRENT_SKJEMA_VERSJON = 3

class Person(private val aktørId: String, private val fødselsnummer: String) : VedtaksperiodeObserver {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()
    private var skjemaVersjon = CURRENT_SKJEMA_VERSJON

    private val observers = mutableListOf<PersonObserver>()

    fun håndter(nySøknad: ModelNySøknad) {
        nySøknad.valider()
        if (nySøknad.hasErrors()) {
            invaliderAllePerioder(nySøknad)
            nySøknad.expectNoErrors()
        }
        finnEllerOpprettArbeidsgiver(nySøknad).håndter(nySøknad)
    }

    fun håndter(sendtSøknad: ModelSendtSøknad) {
        sendtSøknad.valider()
        if (sendtSøknad.hasErrors()) {
            invaliderAllePerioder(sendtSøknad)
            sendtSøknad.expectNoErrors()
        }
        finnEllerOpprettArbeidsgiver(sendtSøknad).håndter(sendtSøknad)
    }

    fun håndter(inntektsmelding: ModelInntektsmelding) {
        inntektsmelding.valider()
        if (inntektsmelding.hasErrors()) {
            invaliderAllePerioder(inntektsmelding)
            inntektsmelding.expectNoErrors()
        }
        finnEllerOpprettArbeidsgiver(inntektsmelding).håndter(inntektsmelding)
    }


    fun håndter(ytelser: ModelYtelser) {
        finnArbeidsgiver(ytelser)?.håndter(this, ytelser)
    }

    fun håndter(manuellSaksbehandling: ModelManuellSaksbehandling) {
        finnArbeidsgiver(manuellSaksbehandling)?.håndter(manuellSaksbehandling)
    }

    fun håndter(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
        finnArbeidsgiver(vilkårsgrunnlag)?.håndter(vilkårsgrunnlag)
    }

    fun håndter(påminnelse: ModelPåminnelse) {
        if (true == finnArbeidsgiver(påminnelse)?.håndter(påminnelse)) return
        observers.forEach {
            it.vedtaksperiodeIkkeFunnet(
                PersonObserver.VedtaksperiodeIkkeFunnetEvent(
                    vedtaksperiodeId = UUID.fromString(påminnelse.vedtaksperiodeId()),
                    aktørId = påminnelse.aktørId(),
                    fødselsnummer = påminnelse.fødselsnummer(),
                    organisasjonsnummer = påminnelse.organisasjonsnummer()
                )
            )
        }
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
        observers.forEach {
            it.personEndret(
                PersonObserver.PersonEndretEvent(
                    aktørId = aktørId,
                    sykdomshendelse = event.sykdomshendelse,
                    memento = memento(),
                    fødselsnummer = fødselsnummer
                )
            )
        }
    }

    fun addObserver(observer: PersonObserver) {
        observers.add(observer)
        arbeidsgivere.values.forEach { it.addObserver(observer) }
    }

    internal fun accept(visitor: PersonVisitor) {
        visitor.preVisitPerson(this)
        visitor.preVisitArbeidsgivere()
        arbeidsgivere.values.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgivere()
        visitor.postVisitPerson(this)
    }

    private fun harAndreArbeidsgivere(hendelse: ArbeidstakerHendelse): Boolean {
        if (arbeidsgivere.isEmpty()) return false
        if (arbeidsgivere.size > 1) return true
        return !arbeidsgivere.containsKey(hendelse.organisasjonsnummer())
    }

    private fun invaliderAllePerioder(arbeidstakerHendelse: ArbeidstakerHendelse) {
        arbeidsgivere.forEach { (_, arbeidsgiver) ->
            arbeidsgiver.invaliderPerioder(arbeidstakerHendelse)
        }
    }

    private fun finnArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { arbeidsgivere[it] }

    private fun finnEllerOpprettArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.getOrPut(orgnr) {
                arbeidsgiver(orgnr)
            }.also {
                if (arbeidsgivere.size > 1) {
                    invaliderAllePerioder(hendelse)
                    hendelse.severe("Forsøk på å legge til arbeidsgiver nummer to: %s", hendelse.organisasjonsnummer())
                }
            }
        }

    private fun arbeidsgiver(organisasjonsnummer: String) =
        Arbeidsgiver(organisasjonsnummer).also {
            it.addObserver(this)
            observers.forEach { observer ->
                it.addObserver(observer)
            }
        }

    fun memento() =
        Memento(
            aktørId = this.aktørId,
            fødselsnummer = this.fødselsnummer,
            skjemaVersjon = this.skjemaVersjon,
            arbeidsgivere = this.arbeidsgivere.values.map { it.memento() }
        )

    class Memento internal constructor(
        internal val aktørId: String,
        internal val fødselsnummer: String,
        internal val skjemaVersjon: Int,
        internal val arbeidsgivere: List<Arbeidsgiver.Memento>
    ) {

        companion object {
            private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

            fun fromString(state: String): Memento {
                val jsonNode = objectMapper.readTree(state)

                if (!jsonNode.hasNonNull("skjemaVersjon")) throw PersonskjemaForGammelt(-1, CURRENT_SKJEMA_VERSJON)

                val skjemaVersjon = jsonNode["skjemaVersjon"].intValue()

                if (skjemaVersjon < CURRENT_SKJEMA_VERSJON) throw PersonskjemaForGammelt(
                    skjemaVersjon,
                    CURRENT_SKJEMA_VERSJON
                )

                return Memento(
                    aktørId = jsonNode["aktørId"].textValue(),
                    fødselsnummer = jsonNode["fødselsnummer"].textValue(),
                    skjemaVersjon = skjemaVersjon,
                    arbeidsgivere = jsonNode["arbeidsgivere"].map {
                        Arbeidsgiver.Memento.fromString(it.toString())
                    }
                )
            }
        }

        fun state(): String =
            objectMapper.convertValue<ObjectNode>(
                mapOf(
                    "aktørId" to this.aktørId,
                    "fødselsnummer" to this.fødselsnummer,
                    "skjemaVersjon" to this.skjemaVersjon
                )
            ).also {
                this.arbeidsgivere.fold(it.putArray("arbeidsgivere")) { result, current ->
                    result.addRawValue(RawValue(current.state()))
                }
            }.toString()
    }

    companion object {
        fun restore(memento: Memento): Person {
            return Person(memento.aktørId, memento.fødselsnummer)
                .apply {
                    this.arbeidsgivere.putAll(memento.arbeidsgivere.map { arbeidsgiverMemento ->
                        Arbeidsgiver.restore(arbeidsgiverMemento).also { arbeidsgiver ->
                            arbeidsgiver.addObserver(this)
                        }.let { arbeidsgiver ->
                            arbeidsgiver.organisasjonsnummer() to arbeidsgiver
                        }
                    })
                }
        }
    }
}
