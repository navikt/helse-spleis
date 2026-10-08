package no.nav.helse.dto.serialisering

import no.nav.helse.dto.BeløpstidslinjeDto
import no.nav.helse.dto.RefusjonsservitørDto
import java.util.UUID

data class UbrukteRefusjonsopplysningerUtDto(
    val ubrukteRefusjonsopplysninger: RefusjonsservitørDto,
    val sisteRefusjonstidslinje: BeløpstidslinjeDto?,
    val sisteBehandlingId: UUID?,
)
