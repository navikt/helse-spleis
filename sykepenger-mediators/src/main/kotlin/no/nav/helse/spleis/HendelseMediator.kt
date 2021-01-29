package no.nav.helse.spleis

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.db.VedtaksperiodeIdTilstand
import no.nav.helse.spleis.meldinger.model.*
import org.slf4j.LoggerFactory
import java.util.*

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    private val rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
    hendelseRepository: HendelseRepository,
    private val lagrePersonDao: LagrePersonDao
) : IHendelseMediator {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)
    private val replayMediator = ReplayMediator(this, hendelseRepository)

    override fun behandle(message: HendelseMessage) {
        try {
            message.behandle(this)
        } catch (err: Aktivitetslogg.AktivitetException) {
            withMDC(err.kontekst()) {
                sikkerLogg.error("alvorlig feil i aktivitetslogg: ${err.message}\n\t${message.toJson()}", err)
            }
        }
    }

    override fun behandle(message: NySøknadMessage, sykmelding: Sykmelding) {
        håndter(message, sykmelding) { person ->
            HendelseProbe.onSykmelding()
            person.håndter(sykmelding)
        }
    }

    override fun behandle(message: SendtSøknadArbeidsgiverMessage, søknad: SøknadArbeidsgiver) {
        håndter(message, søknad) { person ->
            HendelseProbe.onSøknadArbeidsgiver()
            person.håndter(søknad)
        }
    }

    override fun behandle(message: SendtSøknadNavMessage, søknad: Søknad) {
        håndter(message, søknad) { person ->
            HendelseProbe.onSøknadNav()
            person.håndter(søknad)
        }
    }

    override fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding) {
        håndter(message, inntektsmelding) { person ->
            HendelseProbe.onInntektsmelding()
            person.håndter(inntektsmelding)
        }
    }

    override fun behandle(message: InntektsmeldingReplayMessage, inntektsmelding: InntektsmeldingReplay) {
        håndter(message, inntektsmelding) { person ->
            HendelseProbe.onInntektsmeldingReplay()
            person.håndter(inntektsmelding)
        }
    }

    override fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk) {
        håndter(message, utbetalingshistorikk) { person ->
            HendelseProbe.onUtbetalingshistorikk()
            person.håndter(utbetalingshistorikk)
        }
    }

    override fun behandle(message: YtelserMessage, ytelser: Ytelser) {
        håndter(message, ytelser) { person ->
            HendelseProbe.onYtelser()
            person.håndter(ytelser)
        }
    }

    override fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag) {
        håndter(message, vilkårsgrunnlag) { person ->
            HendelseProbe.onVilkårsgrunnlag()
            person.håndter(vilkårsgrunnlag)
        }
    }

    override fun behandle(message: SimuleringMessage, simulering: Simulering) {
        håndter(message, simulering) { person ->
            HendelseProbe.onSimulering()
            person.håndter(simulering)
        }
    }

    override fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        håndter(message, utbetalingsgodkjenning) { person ->
            HendelseProbe.onUtbetalingsgodkjenning()
            person.håndter(utbetalingsgodkjenning)
        }
    }

    override fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført) {
        håndter(message, utbetaling) { person ->
            person.håndter(utbetaling)
        }
    }

    override fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse) {
        håndter(message, utbetaling) { person ->
            HendelseProbe.onUtbetaling()
            person.håndter(utbetaling)
        }
    }

    override fun behandle(message: UtbetalingpåminnelseMessage, påminnelse: Utbetalingpåminnelse) {
        håndter(message, påminnelse) { person ->
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse) {
        håndter(message, påminnelse) { person ->
            HendelseProbe.onPåminnelse(påminnelse)
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: PersonPåminnelseMessage, påminnelse: PersonPåminnelse) {
        håndter(message, påminnelse) { person ->
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling) {
        håndter(message, annullerUtbetaling) { person ->
            HendelseProbe.onAnnullerUtbetaling()
            person.håndter(annullerUtbetaling)
        }
    }

    override fun behandle(message: AvstemmingMessage, avstemming: Avstemming) {
        håndter(message, avstemming) { person ->
            person.håndter(avstemming)
            lagrePersonDao.personAvstemt(avstemming)
        }
    }

    override fun behandle(message: RollbackMessage, rollback: Rollback) {
        val nyestePersonId = requireNotNull(personRepository.hentNyestePersonId(rollback.fødselsnummer()))
        val vedtaksperiodeIderFørRollback = personRepository.hentVedtaksperiodeIderMedTilstand(nyestePersonId)
        val vedtaksperiodeIderEtterRollback =
            personRepository.hentVedtaksperiodeIderMedTilstand(rollback.personVersjon())

        val vedtaksperioderEndretTilstand = vedtaksperiodeIderFørRollback - vedtaksperiodeIderEtterRollback
        val vedtaksperioderSlettet =
            vedtaksperiodeIderFørRollback.map(VedtaksperiodeIdTilstand::id) - vedtaksperiodeIderEtterRollback.map(
                VedtaksperiodeIdTilstand::id
            )

        sjekkForVedtaksperioderTilUtbetaling(vedtaksperioderEndretTilstand)

        val rollbackPerson = personForId(rollback.personVersjon())
        val personMediator = PersonMediator(rollbackPerson, message, rollback)
        rollbackPerson.håndter(rollback)
        val tilbakerulledePerioder =
            personRepository.markerSomTilbakerullet(rollback.fødselsnummer(), rollback.personVersjon())
        sikkerLogg.info(
            "Rullet tilbake {} person for {}, gikk fra {} til {}",
            keyValue("antallPersonIder", tilbakerulledePerioder),
            keyValue("fødselsnummer", rollback.fødselsnummer()),
            keyValue("forigePersonId", nyestePersonId),
            keyValue("rullesTilbakeTilPersonId", rollback.personVersjon())
        )
        finalize(personMediator, message, rollback)


        publiserPersonRulletTilbake(rollback, message, vedtaksperioderSlettet)
    }

    override fun behandle(message: RollbackDeleteMessage, rollback: RollbackDelete) {
        val nyestePersonId = requireNotNull(personRepository.hentNyestePersonId(rollback.fødselsnummer()))
        val vedtaksperioderFørRollback = personRepository.hentVedtaksperiodeIderMedTilstand(nyestePersonId)

        sjekkForVedtaksperioderTilUtbetaling(vedtaksperioderFørRollback)
        val person = Person(rollback.aktørId(), rollback.fødselsnummer())
        val personMediator = PersonMediator(person, message, rollback)
        person.håndter(rollback)
        val tilbakerulledePerioder = personRepository.markerSomTilbakerullet(rollback.fødselsnummer(), 0)
        sikkerLogg.info(
            "Resetter person {} perioder for {}, original {}",
            keyValue("antallPersonIder", tilbakerulledePerioder),
            keyValue("fødselsnummer", rollback.fødselsnummer()),
            keyValue("forigePersonId", nyestePersonId)
        )
        finalize(personMediator, message, rollback)
        publiserPersonRulletTilbake(rollback, message, vedtaksperioderFørRollback.map { it.id })
    }

    override fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje) {
        håndter(message, overstyrTidslinje) { person ->
            HendelseProbe.onOverstyrTidslinje()
            person.håndter(overstyrTidslinje)
        }
    }

    override fun behandle(message: EtterbetalingMessage, grunnbeløpsregulering: Grunnbeløpsregulering) {
        håndter(message, grunnbeløpsregulering) { person ->
            person.håndter(grunnbeløpsregulering)
        }
    }

    private fun publiserPersonRulletTilbake(
        rollback: PersonHendelse,
        message: HendelseMessage,
        vedtaksperioderSlettet: List<UUID>
    ) {
        rapidsConnection.publish(
            rollback.fødselsnummer(), JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "person_rullet_tilbake",
                    "hendelseId" to message.id,
                    "fødselsnummer" to rollback.fødselsnummer(),
                    "vedtaksperioderSlettet" to vedtaksperioderSlettet
                )
            ).toJson()
        )
    }

    private fun sjekkForVedtaksperioderTilUtbetaling(vedtaksperioderEndretTilstand: List<VedtaksperiodeIdTilstand>) {
        require(vedtaksperioderEndretTilstand.none { it.tilstand in listOf("AVSLUTTET", "TIL_UTBETALING") }) {
            "Prøvde å rulle tilbake en person med en utbetalt periode"
        }
    }

    private fun <Hendelse : PersonHendelse> håndter(
        message: HendelseMessage,
        hendelse: Hendelse,
        handler: (Person) -> Unit
    ) {
        person(message, hendelse).also {
            val personMediator = PersonMediator(it, message, hendelse)
            handler(it)
            finalize(personMediator, message, hendelse)
        }
    }

    private fun person(message: HendelseMessage, hendelse: PersonHendelse): Person {
        return initPerson(
            personRepository.hentPerson(hendelse.fødselsnummer()) ?: Person(
                aktørId = hendelse.aktørId(),
                fødselsnummer = hendelse.fødselsnummer()
            )
        )
    }

    private fun personForId(id: Long): Person {
        return initPerson(personRepository.hentPerson(id))
    }

    private fun initPerson(person: Person): Person {
        person.addObserver(replayMediator)
        person.addObserver(VedtaksperiodeProbe)
        return person
    }

    private fun finalize(personMediator: PersonMediator, message: HendelseMessage, hendelse: PersonHendelse) {
        personMediator.finalize(rapidsConnection, lagrePersonDao)
        replayMediator.finalize()
        if (!hendelse.hasActivities()) return
        if (hendelse.hasErrorsOrWorse()) sikkerLogg.info("aktivitetslogg inneholder errors:\n${hendelse.toLogString()}")
        else sikkerLogg.info("aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}")
        behovMediator.håndter(message, hendelse)
    }

}

internal interface IHendelseMediator {
    fun behandle(message: HendelseMessage)
    fun behandle(message: NySøknadMessage, sykmelding: Sykmelding)
    fun behandle(message: SendtSøknadArbeidsgiverMessage, søknad: SøknadArbeidsgiver)
    fun behandle(message: SendtSøknadNavMessage, søknad: Søknad)
    fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding)
    fun behandle(message: InntektsmeldingReplayMessage, inntektsmelding: InntektsmeldingReplay)
    fun behandle(message: UtbetalingpåminnelseMessage, påminnelse: Utbetalingpåminnelse)
    fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse)
    fun behandle(message: PersonPåminnelseMessage, påminnelse: PersonPåminnelse)
    fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk)
    fun behandle(message: YtelserMessage, ytelser: Ytelser)
    fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag)
    fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning)
    fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført)
    fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse)
    fun behandle(message: SimuleringMessage, simulering: Simulering)
    fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling)
    fun behandle(message: AvstemmingMessage, avstemming: Avstemming)
    fun behandle(message: RollbackMessage, rollback: Rollback)
    fun behandle(message: RollbackDeleteMessage, rollback: RollbackDelete)
    fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje)
    fun behandle(message: EtterbetalingMessage, grunnbeløpsregulering: Grunnbeløpsregulering)
}
