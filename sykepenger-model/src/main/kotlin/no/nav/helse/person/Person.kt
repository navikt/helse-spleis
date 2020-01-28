package no.nav.helse.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import java.util.*
import javax.swing.ViewportLayout
import kotlin.reflect.jvm.internal.ReflectProperties

private const val CURRENT_SKJEMA_VERSJON = 3

class Person(private val aktørId: String, private val fødselsnummer: String) : VedtaksperiodeObserver {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()
    private val skjemaVersjon = CURRENT_SKJEMA_VERSJON
    private val aktivitetslogger = Aktivitetslogger()
    private val hendelser = mutableListOf<ArbeidstakerHendelse>()

    private val observers = mutableListOf<PersonObserver>()

    fun håndter(nySøknad: ModelNySøknad) {
        registrer(nySøknad, "Behandler ny søknad")
        var arbeidsgiver: Arbeidsgiver? = null
        continueIfNoErrors(nySøknad,
            { nySøknad.valider() },
            { arbeidsgiver = finnEllerOpprettArbeidsgiver(nySøknad) },
            { arbeidsgiver?.håndter(nySøknad) })
        nySøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(sendtSøknad: ModelSendtSøknad) {
        registrer(sendtSøknad, "Behandler sendt søknad")
        var arbeidsgiver: Arbeidsgiver? = null
        fun validate(): ValidationStep = { sendtSøknad.valider() }
        fun arbeidsgiver(): ValidationStep = { arbeidsgiver = finnEllerOpprettArbeidsgiver(sendtSøknad) }
        fun håndterSendtSøknad(): ValidationStep = { arbeidsgiver?.håndter(sendtSøknad) }
        fun onError() {
            invaliderAllePerioder(sendtSøknad)
        }

        sendtSøknad.continueIfNoErrors(
            validate(),
            arbeidsgiver(),
            håndterSendtSøknad()
        ) { onError() }

        sendtSøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(inntektsmelding: ModelInntektsmelding) {
        registrer(inntektsmelding, "Behandler inntektsmelding")
        var arbeidsgiver: Arbeidsgiver? = null
        continueIfNoErrors(inntektsmelding,
            { inntektsmelding.valider() },
            { arbeidsgiver = finnEllerOpprettArbeidsgiver(inntektsmelding) },
            { arbeidsgiver?.håndter(inntektsmelding) })
        inntektsmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(ytelser: ModelYtelser) {
        registrer(ytelser, "Behandler historiske utbetalinger og inntekter")
        finnArbeidsgiver(ytelser)?.håndter(this, ytelser)
        ytelser.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(manuellSaksbehandling: ModelManuellSaksbehandling) {
        registrer(manuellSaksbehandling, "Behandler manuell saksbehandling")
        finnArbeidsgiver(manuellSaksbehandling)?.håndter(manuellSaksbehandling)
        manuellSaksbehandling.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
        registrer(vilkårsgrunnlag, "Behandler vilkårsgrunnlag")
        finnArbeidsgiver(vilkårsgrunnlag)?.håndter(vilkårsgrunnlag)
        vilkårsgrunnlag.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(påminnelse: ModelPåminnelse) {
        påminnelse.info("Behandler påminnelse")
        if (true == finnArbeidsgiver(påminnelse)?.håndter(påminnelse)) return
        påminnelse.warn("Fant ikke arbeidsgiver eller vedtaksperiode")
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
        påminnelse.kopierAktiviteterTil(aktivitetslogger)
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
        visitor.preVisitHendelser()
        hendelser.forEach { it.accept(visitor) }
        visitor.postVisitHendelser()
        visitor.visitPersonAktivitetslogger(aktivitetslogger)
        visitor.preVisitArbeidsgivere()
        arbeidsgivere.values.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgivere()
        visitor.postVisitPerson(this)
    }

    private fun continueIfNoErrors(hendelse: ArbeidstakerHendelse, vararg blocks: ValidationStep) {
        if (hendelse.hasErrors()) return
        blocks.forEach {
            it()
            if (hendelse.hasErrors()) return invaliderAllePerioder(hendelse)
        }
    }

    private fun registrer(hendelse: ArbeidstakerHendelse, melding: String) {
        hendelser.add(hendelse)
        hendelse.info(melding)
    }

    private fun invaliderAllePerioder(arbeidstakerHendelse: ArbeidstakerHendelse) {
        arbeidsgivere.forEach { (_, arbeidsgiver) ->
            arbeidsgiver.invaliderPerioder(arbeidstakerHendelse)
        }
    }

    private fun finnArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let {
            arbeidsgivere[it].also {
                if (it == null) hendelse.error("Finner ikke arbeidsgiver")
            }
        }

    private fun finnEllerOpprettArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.getOrPut(orgnr) {
                hendelse.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", orgnr)
                arbeidsgiver(orgnr)
            }.also {
                if (arbeidsgivere.size > 1) {
                    hendelse.error("Forsøk på å legge til arbeidsgiver nummer to: %s", hendelse.organisasjonsnummer())
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
