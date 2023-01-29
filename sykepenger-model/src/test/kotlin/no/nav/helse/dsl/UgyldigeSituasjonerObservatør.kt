package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt

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
        validerInntektshistorikk()
    }

    private fun validerSykdomshistorikk() {
        arbeidsgivere.forEach { arbeidsgiver ->
            val hendelseIder = arbeidsgiver.inspektør.sykdomshistorikk.inspektør.hendelseIder()
            check(hendelseIder.size == hendelseIder.toSet().size) {
                "Sykdomshistorikken inneholder duplikate hendelseIder"
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

    private fun validerInntektshistorikk() {
        arbeidsgivere.forEach { arbeidsgiver  ->
            validerInntektshistorikk(arbeidsgiver)
        }
    }

    private fun validerInntektshistorikk(arbeidsgiver: Arbeidsgiver) {
        val inntektshistorikkInspektør = arbeidsgiver.inspektør.inntektshistorikk.inspektør
        val forrige = inntektshistorikkInspektør.inntekterForrigeInnslag()
        val siste = inntektshistorikkInspektør.inntekterSisteInnslag()
        if (siste != null) {
            check(siste.size == siste.toSet().size) {
                "Siste innslag i inntektshistorikken inneholder duplikate opplysninger:\n\t${siste.joinToString {"\n${siste.fremhevDuplikat(it)}\t$it"}}"
            }
            check(siste.isNotEmpty()) {
                "Siste innslag i inntektshistorikken er tomt"
            }
        }
        if (forrige != null && siste != null) {
            logCheck(forrige.toSet() != siste.toSet()) {
                "Forrige og siste innslag i inntektshistorikken er like"
            }
        }
    }

    private val Inntektshistorikk.inspektør get() = InntektshistorikkInspektør(this)

    private class InntektshistorikkInspektør(inntektshistorikk: Inntektshistorikk): InntekthistorikkVisitor {
        private val innslag = mutableListOf<UUID>()
        private val sisteInnslagId get() = innslag.last()
        private val inntekterPerInnslag = mutableMapOf<UUID, MutableList<String>>()
        private fun leggTilInntekt(inntekt: String) {
            inntekterPerInnslag.getOrPut(sisteInnslagId) { mutableListOf() }.add(inntekt)
        }
        private fun inntekterFor(innslag: UUID) = inntekterPerInnslag.getOrDefault(innslag, emptyList())

        fun inntekterForrigeInnslag() =
            innslag.elementAtOrNull(1)?.let { inntekterFor(it) }

        fun inntekterSisteInnslag() =
            innslag.elementAtOrNull(0)?.let { inntekterFor(it) }


        private val skatt = mutableListOf<UUID>()
        private val sisteSkattId get() = skatt.last()
        private val inntekterPerSkatt = mutableMapOf<UUID, MutableList<String>>()
        private fun leggTilSkattInntekt(inntekt: String) {
            check(!inntekterPerSkatt.filterKeys { id -> id != sisteSkattId }.values.flatten().contains(inntekt)) {
                "Skatteinntekt $inntekt finnes allerede i en annen SkattComposite"
            }
            inntekterPerSkatt.getOrPut(sisteSkattId) { mutableListOf() }.add(inntekt)
        }
        private fun skattInntekterFor(skatt: UUID) = inntekterPerSkatt.getOrDefault(skatt, emptyList())


        init {
            inntektshistorikk.accept(this)
        }


        override fun preVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {
            this.innslag.add(id)
        }

        override fun preVisitSkattSykepengegrunnlag(
            skattSykepengegrunnlag: SkattSykepengegrunnlag,
            id: UUID,
            dato: LocalDate,
            beløp: Inntekt
        ) {
            this.skatt.add(id)
        }

        override fun postVisitSkattSykepengegrunnlag(
            skattSykepengegrunnlag: SkattSykepengegrunnlag,
            id: UUID,
            dato: LocalDate,
            beløp: Inntekt
        ) {
            leggTilInntekt("SkattComposite dato=$dato, inntekter=${skattInntekterFor(id)} ")
        }

        override fun visitInntektsmelding(inntektsmelding: Inntektsmelding, id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) =
            leggTilInntekt("Inntektsmelding id=$hendelseId, dato=$dato, beløp=$beløp")

        override fun visitInfotrygd(infotrygd: Infotrygd, id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) =
            leggTilInntekt("Infotrygd id=$hendelseId, dato=$dato, beløp=$beløp")

        override fun visitSaksbehandler(
            saksbehandler: Saksbehandler,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            forklaring: String?,
            subsumsjon: Subsumsjon?,
            tidsstempel: LocalDateTime
        ) =
            leggTilInntekt("Saksbehandler id=$hendelseId, dato=$dato, beløp=$beløp")

        override fun visitIkkeRapportert(id: UUID, dato: LocalDate, tidsstempel: LocalDateTime) =
            leggTilInntekt("IkkeRapportert dato=$dato")

        override fun visitSkatteopplysning(skatteopplysning: Skatteopplysning, hendelseId: UUID, beløp: Inntekt, måned: YearMonth, type: Skatteopplysning.Inntekttype, fordel: String, beskrivelse: String, tidsstempel: LocalDateTime) =
            leggTilSkattInntekt("SkattSykepengegrunnlag id=$hendelseId, beløp=$beløp, måned=$måned, type=$type, fordel=$fordel, beskrivelse=$beskrivelse")
    }

    private companion object {
        private fun List<String>.fremhevDuplikat(inntekt: String) =
            if (count { it == inntekt } == 1) "" else "➡️"

        private fun logCheck(check: Boolean, log:() -> String) {
            if (!check) println(log())
        }
    }
}