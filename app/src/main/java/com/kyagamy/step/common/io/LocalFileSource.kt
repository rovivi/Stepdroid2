package com.kyagamy.step.common.io

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/** Simple [FileSource] backed by a local [File]. */
class LocalFileSource(private val file: File) : FileSource {
    private var input: InputStream? = null

    @Throws(IOException::class)
    override fun open(): InputStream {
        close()
        input = FileInputStream(file)
        return input as InputStream
    }

    @Throws(IOException::class)
    override fun read(): ByteArray {
        if (input == null) {
            open()
        }
        return input!!.readBytes()
    }

    override fun close() {
        input?.close()
        input = null
    }
}
