package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.økonomi.Inntekt

internal class UgyldigeSituasjonerObservatør(private val person: Person): PersonObserver {

    init {
        person.addObserver(this)
    }

    override fun vedtaksperiodeEndret(
        hendelseskontekst: Hendelseskontekst,
        event: PersonObserver.VedtaksperiodeEndretEvent
    ) {
        bekreftIngenOverlappende()
        validerInntektshistorikk()
    }

    internal fun bekreftIngenOverlappende() {
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

    internal fun validerInntektshistorikk() {
        arbeidsgivere().forEach { arbeidsgiver  ->
            validerInntektshistorikk(arbeidsgiver)
        }
    }

    private fun validerInntektshistorikk(arbeidsgiver: Arbeidsgiver) {
        val forrige = arbeidsgiver.inspektør.inntektshistorikk.inspektør.inntekterForrigeInnslag()
        val siste = arbeidsgiver.inspektør.inntektshistorikk.inspektør.inntekterSisteInnslag()
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

    private fun arbeidsgivere() = person
        .inspektør
        .vedtaksperioder()
        .keys
        .map { orgnummer -> person.arbeidsgiver(orgnummer) }

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

        override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            this.skatt.add(id)
        }

        override fun postVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            leggTilInntekt("SkattComposite dato=$dato, inntekter=${skattInntekterFor(id)} ")
        }

        override fun visitInntektsmelding(inntektsmelding: Inntektshistorikk.Inntektsmelding, id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) =
            leggTilInntekt("Inntektsmelding id=$hendelseId, dato=$dato, beløp=$beløp")

        override fun visitInfotrygd(infotrygd: Inntektshistorikk.Infotrygd, id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) =
            leggTilInntekt("Infotrygd id=$hendelseId, dato=$dato, beløp=$beløp")

        override fun visitSaksbehandler(saksbehandler: Inntektshistorikk.Saksbehandler, id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) =
            leggTilInntekt("Saksbehandler id=$hendelseId, dato=$dato, beløp=$beløp")

        override fun visitIkkeRapportert(id: UUID, dato: LocalDate, tidsstempel: LocalDateTime) =
            leggTilInntekt("IkkeRapportert dato=$dato")

        override fun visitSkattSykepengegrunnlag(sykepengegrunnlag: Inntektshistorikk.Skatt.Sykepengegrunnlag, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, måned: YearMonth, type: Inntektshistorikk.Skatt.Inntekttype, fordel: String, beskrivelse: String, tidsstempel: LocalDateTime) =
            leggTilSkattInntekt("SkattSykepengegrunnlag id=$hendelseId, dato=$dato, beløp=$beløp, måned=$måned, type=$type, fordel=$fordel, beskrivelse=$beskrivelse")

        override fun visitSkattRapportertInntekt(rapportertInntekt: Inntektshistorikk.Skatt.RapportertInntekt, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, måned: YearMonth, type: Inntektshistorikk.Skatt.Inntekttype, fordel: String, beskrivelse: String, tidsstempel: LocalDateTime) =
            leggTilSkattInntekt("SkattRapportertInntekt id=$hendelseId, dato=$dato, beløp=$beløp, måned=$måned, type=$type, fordel=$fordel, beskrivelse=$beskrivelse")
    }

    private companion object {
        private fun List<String>.fremhevDuplikat(inntekt: String) =
            if (count { it == inntekt } == 1) "" else "➡️"

        private fun logCheck(check: Boolean, log:() -> String) {
            if (!check) println(log())
        }
    }
}