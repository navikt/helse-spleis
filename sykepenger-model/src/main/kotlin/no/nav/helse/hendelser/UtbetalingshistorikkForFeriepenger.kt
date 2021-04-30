package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.infotrygdhistorikk.Feriepenger
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import java.time.LocalDate
import java.time.Year
import java.util.*

class UtbetalingshistorikkForFeriepenger(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    val utbetalinger: List<Infotrygdperiode>,
    val feriepengehistorikk: List<Feriepenger>,
    val inntektshistorikk: List<Inntektsopplysning>,
    val harStatslønn: Boolean,
    val arbeidskategorikoder: Map<String, LocalDate>,
    val feriepengeår: Year,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, aktivitetslogg) {
    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer
}
