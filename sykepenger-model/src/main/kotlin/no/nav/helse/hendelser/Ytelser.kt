package no.nav.helse.hendelser


import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import java.time.LocalDate
import java.util.*

class Ytelser(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    internal val vedtaksperiodeId: String,
    private val utbetalingshistorikk: Utbetalingshistorikk?,
    private val foreldrepermisjon: Foreldrepermisjon,
    private val pleiepenger: Pleiepenger,
    private val omsorgspenger: Omsorgspenger,
    private val opplæringspenger: Opplæringspenger,
    private val institusjonsopphold: Institusjonsopphold,
    private val dødsinfo: Dødsinfo,
    private val arbeidsavklaringspenger: Arbeidsavklaringspenger,
    private val dagpenger: Dagpenger,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(meldingsreferanseId, aktivitetslogg) {

    internal fun oppdaterHistorikk(historikk: Infotrygdhistorikk) {
        utbetalingshistorikk?.oppdaterHistorikk(historikk)
    }

    internal fun lagreDødsdato(person: Person) {
        if (dødsinfo.dødsdato == null) return
        person.lagreDødsdato(dødsinfo.dødsdato)
    }

    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        arbeidsavklaringspenger.valider(this, skjæringstidspunkt)
        dagpenger.valider(this, skjæringstidspunkt)
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
