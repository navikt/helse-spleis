package no.nav.helse.inspectors

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal val Person.inspektør get() = PersonInspektør(this)

internal class PersonInspektør(person: Person): PersonVisitor {
    internal lateinit var vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
        private set

    private val arbeidsgivere = mutableSetOf<String>()
    private val infotrygdelementerLagretInntekt = mutableListOf<Boolean>()

    init {
        person.accept(this)
    }

    internal fun harLagretInntekt(indeks: Int) = infotrygdelementerLagretInntekt[indeks]
    internal fun harArbeidsgiver(organisasjonsnummer: String) = organisasjonsnummer in arbeidsgivere

    override fun preVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: Fødselsnummer,
        dødsdato: LocalDate?,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        this.vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk
    }

    override fun preVisitInfotrygdhistorikkElement(
        id: UUID,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        hendelseId: UUID?,
        lagretInntekter: Boolean,
        lagretVilkårsgrunnlag: Boolean,
        harStatslønn: Boolean
    ) {
        infotrygdelementerLagretInntekt.add(lagretInntekter)
    }

    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        this.arbeidsgivere.add(organisasjonsnummer)
    }
}
