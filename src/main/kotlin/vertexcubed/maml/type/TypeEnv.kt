package vertexcubed.maml.type

class TypeEnv() {

    val types = ArrayList<MType>()
    val generalizedTypes = ArrayList<MType>()
    private var id = 0
    private var genId = 0

    fun newTypeVar(): MTypeVar {
        val ret = MTypeVar(id++)
        types.add(ret)
        return ret
    }

    fun newGeneralType(): MGeneralTypeVar {
        val ret = MGeneralTypeVar(genId++)
        generalizedTypes.add(ret)
        return ret
    }



    private fun increment(old: String): String {
        if(old.isEmpty()) return "a"

        var char = old.last()
        if(char == 'z') {
            return increment(old.substring(0, old.length - 1)) + 'a'
        }
        else {
            char++
            return old.substring(0, old.length - 1) + char
        }
    }
}