package no.nav.helse.person.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.inngangsvilkar.InngangsvilkårHendelse
import no.nav.helse.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.søknad.NySøknadHendelse
import no.nav.helse.søknad.SendtSøknadHendelse
import java.util.*

private const val CURRENT_SKJEMA_VERSJON=2

class Person(val aktørId: String) : SakskompleksObserver {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()
    private var skjemaVersjon = CURRENT_SKJEMA_VERSJON

    private val personObservers = mutableListOf<PersonObserver>()
    fun håndterNySøknad(nySøknadHendelse: NySøknadHendelse) {
        finnEllerOpprettArbeidsgiver(nySøknadHendelse).håndterNySøknad(nySøknadHendelse)
    }

    fun håndterSendtSøknad(sendtSøknadHendelse: SendtSøknadHendelse) {
        finnEllerOpprettArbeidsgiver(sendtSøknadHendelse).håndterSendtSøknad(sendtSøknadHendelse)
    }

    fun håndterInntektsmelding(inntektsmeldingHendelse: InntektsmeldingHendelse) {
        finnEllerOpprettArbeidsgiver(inntektsmeldingHendelse).håndterInntektsmelding(inntektsmeldingHendelse)
    }

    fun håndterSykepengehistorikk(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        finnArbeidsgiver(sykepengehistorikkHendelse)?.håndterSykepengehistorikk(sykepengehistorikkHendelse)
    }

    fun håndterInngangsvilkår(inngangsvilkårHendelse: InngangsvilkårHendelse) {
        finnArbeidsgiver(inngangsvilkårHendelse)?.håndterInngangsvilkår(inngangsvilkårHendelse)
    }

    override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
        personObservers.forEach {
            it.personEndret(PersonObserver.PersonEndretEvent(
                    aktørId = aktørId,
                    sykdomshendelse = event.sykdomshendelse,
                    memento = memento()
            ))
        }
    }

    fun addObserver(observer: PersonObserver) {
        personObservers.add(observer)
        arbeidsgivere.values.forEach { it.addObserver(observer) }
    }

    private fun finnArbeidsgiver(hendelse: PersonHendelse) =
            hendelse.organisasjonsnummer()?.let { arbeidsgivere[it] }

    private fun finnEllerOpprettArbeidsgiver(hendelse: PersonHendelse) =
        hendelse.organisasjonsnummer()?.let { orgnr ->
            arbeidsgivere.getOrPut(orgnr) {
                arbeidsgiver(orgnr)
            }
        } ?: throw UtenforOmfangException("dokument mangler virksomhetsnummer", hendelse)

    private fun arbeidsgiver(organisasjonsnummer: String) =
        Arbeidsgiver(organisasjonsnummer, UUID.randomUUID()).also {
            it.addObserver(this)
            personObservers.forEach { personObserver ->
                it.addObserver(personObserver)
            }
        }

    internal inner class Arbeidsgiver(val organisasjonsnummer: String, val id: UUID) {

        internal constructor(arbeidsgiverJson: ArbeidsgiverJson): this(arbeidsgiverJson.organisasjonsnummer, arbeidsgiverJson.id) {
            saker.addAll(arbeidsgiverJson.saker.map { Sakskompleks.fromJson(it) })
        }

        private val saker = mutableListOf<Sakskompleks>()
        private val sakskompleksObservers = mutableListOf<SakskompleksObserver>()

        fun håndterNySøknad(nySøknadHendelse: NySøknadHendelse) {
            if (saker.none { it.håndterNySøknad(nySøknadHendelse) }) {
                nyttSakskompleks().håndterNySøknad(nySøknadHendelse)
            }
        }

        fun håndterSendtSøknad(sendtSøknadHendelse: SendtSøknadHendelse) {
            if (saker.none { it.håndterSendtSøknad(sendtSøknadHendelse) }) {
                nyttSakskompleks().håndterSendtSøknad(sendtSøknadHendelse)
            }
        }

        fun håndterInntektsmelding(inntektsmeldingHendelse: InntektsmeldingHendelse) {
            if (saker.none { it.håndterInntektsmelding(inntektsmeldingHendelse) }) {
                nyttSakskompleks().håndterInntektsmelding(inntektsmeldingHendelse)
            }
        }

        internal fun håndterSykepengehistorikk(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
            saker.forEach { it.håndterSykepengehistorikk(sykepengehistorikkHendelse) }
        }

        fun håndterInngangsvilkår(inngangsvilkårHendelse: InngangsvilkårHendelse) {
            saker.forEach { it.håndterInngangsvilkår(inngangsvilkårHendelse) }
        }

        fun addObserver(observer: SakskompleksObserver) {
            sakskompleksObservers.add(observer)
            saker.forEach { it.addSakskompleksObserver(observer) }
        }

        private fun nyttSakskompleks(): Sakskompleks {
            return Sakskompleks(UUID.randomUUID(), aktørId, organisasjonsnummer).also {
                sakskompleksObservers.forEach(it::addSakskompleksObserver)
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

    internal fun memento() =
            Memento(objectMapper.writeValueAsString(jsonRepresentation()))

    private fun jsonRepresentation(): PersonJson {
        return PersonJson(
                aktørId = aktørId,
                skjemaVersjon = skjemaVersjon,
                arbeidsgivere = arbeidsgivere.map { it.value.jsonRepresentation() }
        )
    }

    data class Memento(private val json: String) {
        override fun toString() = json
    }

    internal data class ArbeidsgiverJson(
        val organisasjonsnummer: String,
        val saker: List<Sakskompleks.SakskompleksJson>,
        val id: UUID
    )

    private data class PersonJson(
            val aktørId: String,
            val skjemaVersjon: Int,
            val arbeidsgivere: List<ArbeidsgiverJson>
    )

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): Person {
            val personJson: PersonJson = objectMapper.readValue(json)
            if (personJson.skjemaVersjon < CURRENT_SKJEMA_VERSJON){
                throw PersonskjemaForGammelt(personJson.aktørId, personJson.skjemaVersjon,CURRENT_SKJEMA_VERSJON)
            }
            return Person(personJson.aktørId)
                    .apply {
                        arbeidsgivere.putAll(personJson.arbeidsgivere
                                .map {
                                    it.organisasjonsnummer to Arbeidsgiver(it).also { arbeidsgiver ->
                                        arbeidsgiver.addObserver(this)
                                    }
                                })
                    }
        }
    }
}
