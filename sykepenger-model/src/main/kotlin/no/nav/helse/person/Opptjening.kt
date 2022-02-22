package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.opptjeningsperiode
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.toEtterlevelseMap
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import java.time.LocalDate

internal class Opptjening private constructor(
    private val arbeidsforhold: Map<String, List<Arbeidsforholdhistorikk.Arbeidsforhold>>,
    private val opptjeningsperiode: Periode
) {
    internal fun opptjeningsdager() = opptjeningsperiode.dagerMellom().toInt()
    internal fun erOppfylt(): Boolean = opptjeningsperiode.dagerMellom() >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        val erOppfylt = erOppfylt()
        if (!erOppfylt) aktivitetslogg.warn("Perioden er avslått på grunn av manglende opptjening")
        return erOppfylt
    }

    internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
        visitor.visitOpptjening(this, arbeidsforhold, opptjeningsperiode)
    }

    companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28

        fun opptjening(
            arbeidsforhold: Map<String, List<Arbeidsforholdhistorikk.Arbeidsforhold>>,
            skjæringstidspunkt: LocalDate,
            subsumsjonObserver: SubsumsjonObserver
        ): Opptjening {
            val opptjeningsperiode = arbeidsforhold.values.flatten().opptjeningsperiode(skjæringstidspunkt)
            val opptjening = Opptjening(arbeidsforhold, opptjeningsperiode)
            val arbeidsforholdForJurist = arbeidsforhold.flatMap { (orgnummer, arbeidsforhold) -> arbeidsforhold.toEtterlevelseMap(orgnummer) }

            subsumsjonObserver.`§ 8-2 ledd 1`(
                oppfylt = opptjening.erOppfylt(),
                skjæringstidspunkt = skjæringstidspunkt,
                tilstrekkeligAntallOpptjeningsdager = TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER,
                arbeidsforhold = arbeidsforholdForJurist,
                antallOpptjeningsdager = opptjening.opptjeningsperiode.dagerMellom().toInt()
            )
            return opptjening
        }
    }
}
