package vertexcubed.maml.type

import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.UnboundTyConException
import vertexcubed.maml.core.UnboundVarException
import vertexcubed.maml.parse.DummyType

class TypeEnv(val typeSystem: TypeSystem) {

    private var bindingTypes: MutableMap<String, ForAll> = mutableMapOf()
    private var typeDefs: MutableMap<String, ForAll> = mutableMapOf()

    fun copy(): TypeEnv {
        val ret = TypeEnv(typeSystem)
        ret.bindingTypes.putAll(bindingTypes)
        ret.typeDefs.putAll(typeDefs)
        return ret
    }

    fun lookupBinding(binding: String): ForAll {
        return bindingTypes.getOrElse(binding, { throw UnboundVarException(binding) })
    }

    fun lookupBinding(binding: MIdentifier): ForAll {
        return lookupBinding(binding.path.last())
    }

    fun lookupType(type: String): ForAll {
        return typeDefs.getOrElse(type, { throw UnboundTyConException(type) })
    }

    fun lookupType(binding: MIdentifier): ForAll {
        return lookupType(binding.path.last())
    }

    fun addBinding(binding: String, type: ForAll) {
        bindingTypes[binding] = type
    }
    fun addBinding(pair: Pair<String, ForAll>) {
        bindingTypes += pair
    }

    fun addAllBindings(from: Map<String, ForAll>) {
        bindingTypes.putAll(from)
    }


    fun addType(binding: String, type: ForAll) {
        typeDefs[binding] = type
    }

    fun addType(pair: Pair<String, ForAll>) {
        typeDefs += pair
    }

    fun addAllTypes(from: Map<String, ForAll>) {
        typeDefs.putAll(from)
    }

}