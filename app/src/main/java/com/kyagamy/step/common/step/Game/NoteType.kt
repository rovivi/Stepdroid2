package game

enum class NoteType(val code: Short) {
    EMPTY(0),
    TAP(1),
    LONG_START(2),
    LONG_END(3),
    FAKE(4),
    MINE(5),
    MINE_DEATH(6),
    POSION(7),
    LONG_BODY(50),
    LONG_TOUCHABLE(10),
    PRESSED(128),
    LONG_PRESSED(51);

    companion object {
        fun fromCode(code: Short): NoteType {
            return entries.find { it.code == code } ?: EMPTY
        }
    }
}
