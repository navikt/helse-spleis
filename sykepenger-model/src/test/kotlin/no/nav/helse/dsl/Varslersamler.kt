package no.nav.helse.dsl

import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter

internal class Varslersamler {
    private val varsler = mutableListOf<Aktivitet.Varsel>()

    fun registrerVarsler(varsler: List<Aktivitet.Varsel>) {
        this.varsler.addAll(varsler)
    }

    fun bekreftVarslerAssertet(assertetVarsler: AssertetVarsler) {
        val varslerIkkeAssertet = varsler
            .filterNot { varsel -> assertetVarsler.erVarselKvittert(varsel) }

        check(varslerIkkeAssertet.isEmpty()) {
            "Det er varsler som ikke er assertet på:\n${varslerIkkeAssertet.joinToString(separator = "\n") { "- ${it.kode} - ${it.kontekster.joinToString() { it.melding() }}" }}"
        }
    }

    class AssertetVarsler {
        private val varsler = mutableListOf<Pair<AktivitetsloggFilter, Varselkode>>()

        fun erVarselKvittert(aktivitet: Aktivitet.Varsel): Boolean {
            return varsler.any { (filter, varselkode) ->
                varselkode == aktivitet.kode && aktivitet.kontekster.any { spesifikkKontekst -> filter.filtrer(spesifikkKontekst) }
            }
        }

        fun kvitterVarsel(filter: AktivitetsloggFilter, varselkode: Varselkode) {
            varsler.add(Pair(filter, varselkode))
        }

        fun kvitterVarsel(filter: AktivitetsloggFilter, varselkode: Collection<Varselkode>) {
            varselkode.forEach { kvitterVarsel(filter, it) }
        }
    }
}
