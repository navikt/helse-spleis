package no.nav.helse.opptjening.infra.db

import java.time.LocalDate
import no.nav.helse.opptjening.application.VilkårsprøvingRepository
import no.nav.helse.opptjening.domain.Opptjeningsprøving

internal class InMemoryVilkårsprøvingRepository : VilkårsprøvingRepository {
    private val prøvinger = mutableListOf<Opptjeningsprøving>()

    internal val alleProvinger: List<Opptjeningsprøving> get() = prøvinger.toList()

    override fun opprett(prøving: Opptjeningsprøving) {
        check(prøvinger.none { it.gjelderSammeSom(prøving) && !it.erAvsluttet }) {
            "Det pågår allerede en prøving for fødselsnummer ${prøving.fødselsnummer} med skjæringstidspunkt ${prøving.skjæringstidspunkt}"
        }
        prøvinger.add(prøving)
    }

    override fun oppdater(prøving: Opptjeningsprøving) {
        check(prøvinger.any { it.id == prøving.id }) { "Prøving ${prøving.id} er ikke opprettet" }
    }

    override fun finnSiste(fødselsnummer: String, skjæringstidspunkt: LocalDate) =
        prøvinger.lastOrNull { it.fødselsnummer == fødselsnummer && it.skjæringstidspunkt == skjæringstidspunkt }

    private fun Opptjeningsprøving.gjelderSammeSom(annen: Opptjeningsprøving) =
        fødselsnummer == annen.fødselsnummer && skjæringstidspunkt == annen.skjæringstidspunkt
}
