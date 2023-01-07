package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import no.nav.helse.førsteArbeidsdag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.nesteDag
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person

internal class InntektOgRefusjonFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding,
    private val førsteFraværsdag: LocalDate?,
    private val sisteDagIArbeidsgiverperioden: LocalDate?
): IAktivitetslogg by inntektsmelding {

    internal fun meldingsreferanseId() = inntektsmelding.meldingsreferanseId()
    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) = inntektsmelding.leggTil(hendelseIder)
    internal fun nyeRefusjonsopplysninger(skjæringstidspunkt: LocalDate, person: Person) =
        person.nyeRefusjonsopplysninger(skjæringstidspunkt, inntektsmelding)

    private val førsteFraværsdagEtterArbeidsgiverperioden =
        førsteFraværsdag != null && sisteDagIArbeidsgiverperioden != null && førsteFraværsdag > sisteDagIArbeidsgiverperioden

    private val ingenArbeidsgiverperiode = sisteDagIArbeidsgiverperioden == null

    private var erHåndtert: Boolean = false
    internal fun skalHåndteresAv(periode: Periode): Boolean {
        if (erHåndtert) return false
        if (førsteFraværsdag == null && sisteDagIArbeidsgiverperioden == null) return false
        val bestemmendeDag = when (ingenArbeidsgiverperiode || førsteFraværsdagEtterArbeidsgiverperioden) {
            true -> førsteFraværsdag!!
            false -> sisteDagIArbeidsgiverperioden!!.nesteDag
        }
        erHåndtert = bestemmendeDag in periode || bestemmendeDag.førsteArbeidsdag() in periode
        return erHåndtert
    }
}