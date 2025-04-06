package vertexcubed.maml.type

class TypeSystem() {

    val types = ArrayList<MTypeVar>()
    val generalizedTypes = ArrayList<MGeneralTypeVar>()
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

    fun normalizeTypeNames() {
        var i = 0
        val finishedSet = HashSet<MTypeVar>()
        for(type in types) {
            val t = type.find()
            if(t is MTypeVar && !t.isBound() && !finishedSet.contains(t)) {
                t.display = "'${idToStr(i++)}"
                finishedSet.add(t)
            }
        }
    }

    private fun idToStr(id: Int): String {
        var num = id + 1
        var str = ""
        while(num > 0) {
            val digit = num % 26
            //ASCII a is 96
            val letter = (digit + 96).toChar()
            str = letter + str
            num /= 26
        }
        return str
    }
}