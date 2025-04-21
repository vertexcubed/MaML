package vertexcubed.maml.core


/**
 * Represents a "long" identifier, to handle modules and such.
 */
data class MIdentifier(val path: List<String>) {
    constructor(path: String): this(path.split("."))

    init {
        if(path.isEmpty()) throw IllegalArgumentException("Cannot have empty path for identifier!")
    }

    fun isWildCard(): Boolean {
        return path.size == 1 && path[0] == "_"
    }

    override fun toString(): String {
        return path.joinToString(".")
    }
}