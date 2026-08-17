package no.nav.helse.opptjening.domain

import java.time.LocalDate

/**
 * En vilkårsregel er en ren, total funksjon fra grunnlag til kodeverkkode.
 *
 * All I/O og asynkroni hører hjemme i [Vilkårsprøving] og applikasjonslaget. Reglene kan dermed
 * testes isolert, og kjøres om igjen på et historisk grunnlag.
 */
internal interface Vilkårsregel {
    val vilkår: Vilkår
    val versjon: String

    fun vurder(skjæringstidspunkt: LocalDate, grunnlag: Vilkårsgrunnlag): Kodeverkkode
}
