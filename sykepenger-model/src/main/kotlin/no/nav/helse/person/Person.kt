package no.nav.helse.person

import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.*
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.util.*

class Person private constructor(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val arbeidsgivere: MutableList<Arbeidsgiver>,
    private val aktivitetslogger: Aktivitetslogger
) : VedtaksperiodeMediator {

    constructor(
        aktørId: String,
        fødselsnummer: String
    ) : this(aktørId, fødselsnummer, mutableListOf(), Aktivitetslogger())

    private val observers = mutableListOf<PersonObserver>()

    fun håndter(nySøknad: NySøknad) = håndter(nySøknad, "ny søknad")

    fun håndter(sendtSøknad: SendtSøknad) = håndter(sendtSøknad, "sendt søknad")

    fun håndter(inntektsmelding: Inntektsmelding) = håndter(inntektsmelding, "inntektsmelding")

    private fun håndter(hendelse: SykdomstidslinjeHendelse, hendelsesmelding: String) {
        registrer(hendelse, "Behandler $hendelsesmelding")
        Validation(hendelse).also {
            it.onError { invaliderAllePerioder(hendelse) }
            it.valider { ValiderSykdomshendelse(hendelse) }
            val arbeidsgiver = finnEllerOpprettArbeidsgiver(hendelse)
            it.valider { ValiderKunEnArbeidsgiver(arbeidsgivere) }
            it.valider { ArbeidsgiverHåndterHendelse(hendelse, arbeidsgiver) }
        }
        hendelse.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(ytelser: Ytelser) {
        registrer(ytelser, "Behandler historiske utbetalinger og inntekter")
        finnArbeidsgiver(ytelser)?.håndter(ytelser)
        ytelser.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(manuellSaksbehandling: ManuellSaksbehandling) {
        registrer(manuellSaksbehandling, "Behandler manuell saksbehandling")
        finnArbeidsgiver(manuellSaksbehandling)?.håndter(manuellSaksbehandling)
        manuellSaksbehandling.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        registrer(vilkårsgrunnlag, "Behandler vilkårsgrunnlag")
        finnArbeidsgiver(vilkårsgrunnlag)?.håndter(vilkårsgrunnlag)
        vilkårsgrunnlag.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(påminnelse: Påminnelse) {
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

    fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
        observers.forEach { it.vedtaksperiodePåminnet(påminnelse) }
    }

    fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
        observers.forEach {
            it.vedtaksperiodeEndret(event)
            it.personEndret(
                PersonObserver.PersonEndretEvent(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    person = this
                )
            )
        }
    }

    fun addObserver(observer: PersonObserver) {
        observers.add(observer)
    }

    internal fun accept(visitor: PersonVisitor) {
        visitor.preVisitPerson(this, aktørId, fødselsnummer)
        visitor.visitPersonAktivitetslogger(aktivitetslogger)
        visitor.preVisitArbeidsgivere()
        arbeidsgivere.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgivere()
        visitor.postVisitPerson(this, aktørId, fødselsnummer)
    }

    private fun registrer(hendelse: ArbeidstakerHendelse, melding: String) {
        hendelse.info(melding)
    }

    private fun invaliderAllePerioder(arbeidstakerHendelse: ArbeidstakerHendelse) {
        aktivitetslogger.info("Invaliderer alle perioder for alle arbeidsgivere")
        arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.invaliderPerioder(arbeidstakerHendelse)
        }
    }

    private fun finnEllerOpprettArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.finnEllerOpprett(orgnr) {
                hendelse.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", orgnr)
                arbeidsgiver(orgnr)
            }
        }

    private fun finnArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.finn(orgnr).also {
                if (it == null) hendelse.error("Finner ikke arbeidsgiver")
            }
        }

    private fun MutableList<Arbeidsgiver>.finn(orgnr: String) = find { it.organisasjonsnummer() == orgnr }

    private fun MutableList<Arbeidsgiver>.finnEllerOpprett(orgnr: String, creator: () -> Arbeidsgiver) =
        finn(orgnr) ?: run {
            val newValue = creator()
            add(newValue)
            newValue
        }

    private fun arbeidsgiver(organisasjonsnummer: String) =
        Arbeidsgiver(this, organisasjonsnummer)
}
