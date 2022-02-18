package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.opptjeningsperiode
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.toEtterlevelseMap
import no.nav.helse.person.etterlevelse.MaskinellJurist
import java.time.LocalDate

internal class Opptjening private constructor(
    val arbeidsforhold: Map<String, List<Arbeidsforholdhistorikk.Arbeidsforhold>>,
    val opptjeningsperiode: Periode
) {

    fun erOppfylt(): Boolean = opptjeningsperiode.dagerMellom() >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

    companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28

        fun opptjening(
            arbeidsforhold: Map<String, List<Arbeidsforholdhistorikk.Arbeidsforhold>>,
            skjæringstidspunkt: LocalDate,
            jurist: MaskinellJurist
        ): Opptjening {
            val opptjeningsperiode = arbeidsforhold.values.flatten().opptjeningsperiode(skjæringstidspunkt)
            val opptjening = Opptjening(arbeidsforhold, opptjeningsperiode)
            val arbeidsforholdForJurist = arbeidsforhold.flatMap { (orgnummer, arbeidsforhold) -> arbeidsforhold.toEtterlevelseMap(orgnummer) }

            jurist.`§ 8-2 ledd 1`(
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
