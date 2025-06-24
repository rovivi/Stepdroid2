package com.kyagamy.step.common.io

import java.io.Closeable
import java.io.IOException
import java.io.InputStream

/**
 * Abstraction over any source that can provide file-like data.
 * Implementations can read from local files, assets or even remote
 * locations in the future.
 */
interface FileSource : Closeable {
    /** Opens the underlying resource and returns an [InputStream]. */
    @Throws(IOException::class)
    fun open(): InputStream

    /** Reads the entire content of the source as a [ByteArray]. */
    @Throws(IOException::class)
    fun read(): ByteArray

    /** Convenience method to read the content as a String using UTF-8. */
    @Throws(IOException::class)
    fun readText(): String = String(read())
}
