package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.infotrygdhistorikk.Feriepenger
import no.nav.helse.person.infotrygdhistorikk.Feriepenger.Companion.utbetalteFeriepengerTilArbeidsgiver
import no.nav.helse.person.infotrygdhistorikk.Feriepenger.Companion.utbetalteFeriepengerTilPerson
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import java.time.LocalDate
import java.time.Year
import java.util.*

class UtbetalingshistorikkForFeriepenger(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val utbetalinger: List<Infotrygdperiode>,
    private val feriepengehistorikk: List<Feriepenger>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    private val harStatslønn: Boolean,
    private val arbeidskategorikoder: Map<String, LocalDate>,
    //FIXME: Internal?
    internal val opptjeningsår: Year,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, aktivitetslogg) {
    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        utbetalinger.forEach { it.accept(visitor) }
    }

    internal fun utbetalteFeriepengerTilPerson() =
        feriepengehistorikk.utbetalteFeriepengerTilPerson(opptjeningsår)

    internal fun utbetalteFeriepengerTilArbeidsgiver(orgnummer: String) =
        feriepengehistorikk.utbetalteFeriepengerTilArbeidsgiver(orgnummer, opptjeningsår)
}
