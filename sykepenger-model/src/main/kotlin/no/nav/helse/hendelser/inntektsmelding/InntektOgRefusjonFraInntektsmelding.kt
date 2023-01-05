package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.nesteArbeidsdag
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
    internal fun skalHåndteresAv(periode: Periode): Boolean {
        if (førsteFraværsdag == null && sisteDagIArbeidsgiverperioden == null) return false
        if (ingenArbeidsgiverperiode || førsteFraværsdagEtterArbeidsgiverperioden) return førsteFraværsdag in periode
        return sisteDagIArbeidsgiverperioden!!.nesteArbeidsdag() in periode
    }
}