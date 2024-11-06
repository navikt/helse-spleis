package no.nav.helse.spleis.graphql.dto

import java.time.LocalDate

data class GraphQLPerson(
    val aktorId: String,
    val fodselsnummer: String,
    val arbeidsgivere: List<GraphQLArbeidsgiver>,
    val dodsdato: LocalDate?,
    val versjon: Int,
    val vilkarsgrunnlag: List<GraphQLVilkarsgrunnlag>
)

