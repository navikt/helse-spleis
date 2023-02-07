package no.nav.helse.dsl

import no.nav.helse.hendelser.Periode.Companion.overlapper
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.arbeidsgiver

internal class UgyldigeSituasjonerObservatør(private val person: Person): PersonObserver {

    private val arbeidsgivereMap = mutableMapOf<String, Arbeidsgiver>()
    private val arbeidsgivere get() = arbeidsgivereMap.values

    init {
        person.addObserver(this)
    }

    override fun vedtaksperiodeEndret(
        event: PersonObserver.VedtaksperiodeEndretEvent
    ) {
        arbeidsgivereMap.getOrPut(event.organisasjonsnummer) { person.arbeidsgiver(event.organisasjonsnummer) }
        bekreftIngenUgyldigeSituasjoner()
    }

    internal fun bekreftIngenUgyldigeSituasjoner() {
        bekreftIngenOverlappende()
        validerSykdomshistorikk()
    }

    private fun validerSykdomshistorikk() {
        arbeidsgivere.forEach { arbeidsgiver ->
            val perioderPerHendelse = arbeidsgiver.inspektør.sykdomshistorikk.inspektør.perioderPerHendelse()
            perioderPerHendelse.forEach { (hendelseId, perioder) ->
                check(!perioder.overlapper()) {
                    "Sykdomshistorikk inneholder overlappende perioder fra hendelse $hendelseId"
                }
            }
        }
    }

    private fun bekreftIngenOverlappende() {
        person.inspektør.vedtaksperioder()
            .filterValues { it.size > 1 }
            .forEach { (orgnr, perioder) ->
                var nåværende = perioder.first().inspektør
                perioder.subList(1, perioder.size).forEach { periode ->
                    val inspektør = periode.inspektør
                    check(!inspektør.periode.overlapperMed(nåværende.periode)) {
                        "For Arbeidsgiver $orgnr overlapper Vedtaksperiode ${inspektør.id} (${inspektør.periode}) og Vedtaksperiode ${nåværende.id} (${nåværende.periode}) med hverandre!"
                    }
                    nåværende = inspektør
                }
            }
    }
}