package no.nav.helse.hendelser


import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Person
import java.time.LocalDate
import java.util.*

class Ytelser(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    internal val vedtaksperiodeId: String,
    private val utbetalingshistorikk: Utbetalingshistorikk,
    private val foreldrepermisjon: Foreldrepermisjon,
    private val pleiepenger: Pleiepenger,
    private val omsorgspenger: Omsorgspenger,
    private val opplæringspenger: Opplæringspenger,
    private val institusjonsopphold: Institusjonsopphold,
    private val dødsinfo: Dødsinfo,
    private val statslønn: Boolean = false,
    private val arbeidsavklaringspenger: Arbeidsavklaringspenger,
    private val dagpenger: Dagpenger,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(meldingsreferanseId, aktivitetslogg) {
    internal fun utbetalingshistorikk() = utbetalingshistorikk

    internal fun lagreVilkårsgrunnlag(person: Person, periodetype: Periodetype, skjæringstidspunkt: LocalDate) {
        if (periodetype !in listOf(Periodetype.OVERGANG_FRA_IT, Periodetype.INFOTRYGDFORLENGELSE)) return
        if (person.vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt) != null) return

        info("Lagrer vilkårsgrunnlag fra Infotrygd på %s", "$skjæringstidspunkt")
        person.vilkårsgrunnlagHistorikk.lagre(utbetalingshistorikk, skjæringstidspunkt)
    }

    internal fun lagreDødsdato(person: Person) {
        if (dødsinfo.dødsdato == null) return
        person.lagreDødsdato(dødsinfo.dødsdato)
    }

    internal fun addInntekter(person: Person) {
        utbetalingshistorikk.addInntekter(person)
    }

    internal fun valider(periode: Periode, avgrensetPeriode: Periode, periodetype: Periodetype, skjæringstidspunkt: LocalDate): Boolean {
        utbetalingshistorikk.valider(avgrensetPeriode, skjæringstidspunkt)
        arbeidsavklaringspenger.valider(this, skjæringstidspunkt)
        dagpenger.valider(this, skjæringstidspunkt)

        if (periodetype == Periodetype.OVERGANG_FRA_IT) {
            info("Perioden er en direkte overgang fra periode med opphav i Infotrygd")
            if (statslønn) warn("Det er lagt inn statslønn i Infotrygd, undersøk at utbetalingen blir riktig.")
        }

        if (foreldrepermisjon.overlapper(periode)) error("Har overlappende foreldrepengeperioder med syketilfelle")
        if (pleiepenger.overlapper(periode)) error("Har overlappende pleiepengeytelse med syketilfelle")
        if (omsorgspenger.overlapper(periode)) error("Har overlappende omsorgspengerytelse med syketilfelle")
        if (opplæringspenger.overlapper(periode)) error("Har overlappende opplæringspengerytelse med syketilfelle")
        if (institusjonsopphold.overlapper(periode)) error("Har overlappende institusjonsopphold med syketilfelle")

        return !hasErrorsOrWorse()
    }

    override fun aktørId(): String {
        return aktørId
    }

    override fun fødselsnummer(): String {
        return fødselsnummer
    }

    override fun organisasjonsnummer(): String {
        return organisasjonsnummer
    }
}
