package no.nav.helse

internal fun String.readResource() =
        object {}.javaClass.getResource(this)?.readText(Charsets.UTF_8) ?: throw RuntimeException("did not find resource <$this>")
