package vertexcubed.maml.eval

import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.UnboundExternalException
import vertexcubed.maml.core.UnboundVarException

class DynEnv(vararg pairs: Pair<String, MValue>) {

    val bindings = mutableMapOf<String, MValue>()
    private val javaFuncs = mutableMapOf<String, (Array<MValue>) -> MValue>()



    init {
        bindings.putAll(pairs)
    }

    fun copy(): DynEnv {
        val ret = DynEnv()
        ret.bindings.putAll(bindings)
        ret.javaFuncs.putAll(javaFuncs)
        return ret
    }

    fun lookupBinding(binding: String): MValue {
        return bindings.getOrElse(binding, {
            throw UnboundVarException(binding) })
    }

    fun lookupBinding(binding: MIdentifier): MValue {
        var lastEnv = this
        for(i in binding.path.indices) {
            val cur = lastEnv.lookupBinding(binding.path[i])
            if(cur is ModuleValue) {
                lastEnv = cur.bindings
            }
            else if(i != binding.path.lastIndex) {
                throw UnboundVarException(binding.path[i])
            }
            if(i == binding.path.lastIndex) {
                return cur
            }
        }
        throw AssertionError("Should not happen!")
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