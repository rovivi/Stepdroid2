package com.kyagamy.step.engine

data class LongNoteLayout(
    val bodyTop: Int,
    val bodyBottom: Int,
    val headBottom: Int,
    val tailBottom: Int
)

object NoteLayoutCalculator {
    fun calculateLongNote(
        startY: Int,
        endY: Int,
        scaledNoteSize: Int,
        bodyOffsetFactor: Float,
        tailDivisor: Int
    ): LongNoteLayout {
        val bodyOffsetPx = (scaledNoteSize * bodyOffsetFactor).toInt()
        val tailDiv = scaledNoteSize / tailDivisor
        val bodyTop = startY + bodyOffsetPx
        val bodyBottom = endY + tailDiv
        val headBottom = startY + scaledNoteSize
        val tailBottom = endY + scaledNoteSize
        return LongNoteLayout(bodyTop, bodyBottom, headBottom, tailBottom)
    }
}
