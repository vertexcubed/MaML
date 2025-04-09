package vertexcubed.maml.core

import vertexcubed.maml.parse.DummyType
import java.util.*

data class MBinding(val binding: String, val type: Optional<DummyType>) {
    constructor(binding: String): this(binding, Optional.empty())

    override fun toString(): String {
        var str = "Binding($binding"
        if(type.isPresent) str += " : " + type.get()
        return "$str)"
    }
}