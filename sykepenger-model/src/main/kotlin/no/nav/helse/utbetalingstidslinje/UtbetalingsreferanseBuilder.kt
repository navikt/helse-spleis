package no.nav.helse.utbetalingstidslinje

import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import java.util.*

internal fun genererUtbetalingsreferanse(uuid: UUID): String {
    return uuid.base32Encode()
}

private fun UUID.base32Encode(): String {
    val pad = '='
    return Base32(pad.code)
        .encodeAsString(this.byteArray())
        .replace(pad.toString(), "")
}

private fun UUID.byteArray() = ByteBuffer.allocate(Long.SIZE_BYTES * 2).apply {
    putLong(this@byteArray.mostSignificantBits)
    putLong(this@byteArray.leastSignificantBits)
}.array()

