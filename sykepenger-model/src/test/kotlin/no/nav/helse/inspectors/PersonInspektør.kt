package no.nav.helse.inspectors

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import java.time.LocalDate
import java.time.LocalDateTime

internal class PersonInspektør(person: Person): PersonVisitor {
    internal lateinit var vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
        private set
    init {
        person.accept(this)
    }

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
}
