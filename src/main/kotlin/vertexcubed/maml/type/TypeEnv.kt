package vertexcubed.maml.type

import vertexcubed.maml.ast.StructType
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.UnboundModuleException
import vertexcubed.maml.core.UnboundTyConException
import vertexcubed.maml.core.UnboundVarException

class TypeEnv(val typeSystem: TypeSystem) {

    val modules = mutableMapOf<String, StructType>()
    val bindingTypes: MutableMap<String, ForAll> = mutableMapOf()
    val typeDefs: MutableMap<String, ForAll> = mutableMapOf()
    val varLabelBindings: MutableMap<String, MType> = mutableMapOf()

    fun copy(): TypeEnv {
        val ret = TypeEnv(typeSystem)
        ret.modules.putAll(modules)
        ret.bindingTypes.putAll(bindingTypes)
        ret.typeDefs.putAll(typeDefs)
        ret.varLabelBindings.putAll(varLabelBindings)
        return ret
    }

    fun lookupModule(binding: MIdentifier): StructType {
        var lastEnv = this
        var cur: StructType? = null
        for(i in binding.path.indices) {
            cur = lastEnv.lookupModule(binding.path[i])
            lastEnv = cur.types
        }
        return cur?: throw UnboundModuleException(binding.toString())
    }

    fun lookupModule(name: String): StructType {
        return modules.getOrElse(name, { throw UnboundModuleException(name) })
    }

    fun lookupBinding(binding: String): ForAll {
        return bindingTypes.getOrElse(binding, {
            throw UnboundVarException(binding) })
    }

    fun lookupBinding(binding: MIdentifier): ForAll {
        var lastEnv = this
        for (i in 0 until binding.path.lastIndex) {
            val cur = lastEnv.lookupModule(binding.path[i])
            lastEnv = cur.types
        }
        return lastEnv.lookupBinding(binding.path.last())
    }

    fun lookupType(type: String): ForAll {
        return typeDefs.getOrElse(type, { throw UnboundTyConException(type) })
    }

    fun lookupType(binding: MIdentifier): ForAll {

        var lastEnv = this
        for(i in 0 until binding.path.lastIndex) {
            val cur = lastEnv.lookupModule(binding.path[i])
            lastEnv = cur.types
        }
        return lastEnv.lookupType(binding.path.last())

    }

    fun lookupVarLabel(binding: String, orElse: () -> MType): MType {
        return varLabelBindings.getOrElse(binding, orElse)
    }

    /**
     * Represents var labels -> vars. For type dummies. E.g. x: 'a -> find the type of "a" in the context.
     */
    fun addVarLabel(binding: String, type: MType) {
        varLabelBindings[binding] = type
    }

    fun addVarLabel(pair: Pair<String, MType>) {
        varLabelBindings += pair
    }
    fun addModule(struct: StructType) {
        modules[struct.name] = struct
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