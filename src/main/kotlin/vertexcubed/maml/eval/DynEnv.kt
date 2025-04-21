package vertexcubed.maml.eval

import vertexcubed.maml.ast.StructEval
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.UnboundExternalException
import vertexcubed.maml.core.UnboundModuleException
import vertexcubed.maml.core.UnboundVarException

class DynEnv(vararg pairs: Pair<String, MValue>) {

    val modules = mutableMapOf<String, StructEval>()
    val bindings = mutableMapOf<String, MValue>()
    private val javaFuncs = mutableMapOf<String, (Array<MValue>) -> MValue>()



    init {
        bindings.putAll(pairs)
    }

    fun copy(): DynEnv {
        val ret = DynEnv()
        ret.bindings.putAll(bindings)
        ret.modules.putAll(modules)
        ret.javaFuncs.putAll(javaFuncs)
        return ret
    }

    fun lookupBinding(binding: String): MValue {
        return bindings.getOrElse(binding, {
            throw UnboundVarException(binding) })
    }

    fun lookupModule(binding: MIdentifier): StructEval {
        var lastEnv = this
        var cur: StructEval? = null
        for(i in binding.path.indices) {
            cur = lastEnv.lookupModule(binding.path[i])
            lastEnv = cur.bindings
        }
        return cur?: throw UnboundModuleException(binding.toString())
    }

    fun lookupModule(name: String): StructEval {
        return modules.getOrElse(name, { throw UnboundModuleException(name) })
    }

    fun lookupBinding(binding: MIdentifier): MValue {
        var lastEnv = this
        for(i in 0 until binding.path.lastIndex) {
            val cur = lastEnv.lookupModule(binding.path[i])
            lastEnv = cur.bindings
        }
        return lastEnv.lookupBinding(binding.path[binding.path.lastIndex])
    }

    fun addModule(struct: StructEval) {
        modules[struct.name] = struct
    }

    fun addBinding(binding: String, type: MValue) {
        bindings[binding] = type
    }
    fun addBinding(pair: Pair<String, MValue>) {
        bindings += pair
    }

    fun addAllBindings(from: Map<String, MValue>) {
        bindings.putAll(from)
    }

    fun callJavaFunc(name: String, args: Array<MValue>): MValue {
        return javaFuncs.getOrElse(name, { throw UnboundExternalException(name) })(args)
    }

    fun addJavaFunc(name: String, func: (Array<MValue>) -> MValue) {
        javaFuncs[name] = func
    }
}