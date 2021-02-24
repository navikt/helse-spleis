package no.nav.helse.hendelser


import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.IAktivitetslogg
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

    internal fun foreldrepenger() = foreldrepermisjon

    internal fun pleiepenger() = pleiepenger

    internal fun omsorgspenger() = omsorgspenger

    internal fun opplæringspenger() = opplæringspenger

    internal fun institusjonsopphold() = institusjonsopphold

    internal fun dødsinfo() = dødsinfo

    internal fun statslønn() = statslønn

    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate): IAktivitetslogg {
        utbetalingshistorikk.valider(periode, skjæringstidspunkt)
        arbeidsavklaringspenger.valider(this, skjæringstidspunkt)
        dagpenger.valider(this, skjæringstidspunkt)
        return this
    }

    internal fun addInntekter(person: Person) {
        utbetalingshistorikk.addInntekter(person, this)
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
