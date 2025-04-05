package vertexcubed.maml.type

import java.util.*

data class MBinding(val binding: String, val type: Optional<MType>) {
    constructor(binding: String): this(binding, Optional.empty())

    override fun toString(): String {
        var str = "Binding($binding"
        if(type.isPresent) str += " : " + type.get()
        return "$str)"
    }
}