package vertexcubed.maml.type

data class ForAll(val typeVars: List<MGeneralTypeVar>, val type: MType) {

    companion object {
        fun generalize(type: MType, types: TypeSystem): ForAll {

            val typeList = ArrayList<MGeneralTypeVar>()

            val freeVars = recursiveFind(type)
            if(freeVars.isEmpty()) {
                return ForAll(typeList, type)
            }
            typeList.add(types.newGeneralType())
            var retType = type.substitute(freeVars[0], typeList[0])
            for(i in 1 until freeVars.size) {
                typeList.add(types.newGeneralType())
                retType = retType.substitute(freeVars[i], typeList[i])
            }
            return ForAll(typeList, retType)
        }

        fun empty(type: MType): ForAll {
            return ForAll(emptyList(), type)
        }


        private fun recursiveFind(type: MType): List<MTypeVar> {
            return when(val real = type.find()) {
                is MFunction -> {
                    val ret = HashSet<MTypeVar>()
                    val first = recursiveFind(real.arg)
                    val second = recursiveFind(real.ret)
                    ret.addAll(first)
                    ret.addAll(second)
                    ret.toList()
                }
                is MGeneralTypeVar -> emptyList()
                is MBaseTypeVar -> emptyList()
                is MTuple -> {
                    val ret = ArrayList<MTypeVar>()
                    for(t in real.types) {
                        val vars = recursiveFind(t)
                        ret.addAll(vars)
                    }
                    ret
                }
                is MVariantType -> real.args.filter { a -> a.second is MTypeVar }.map { a -> a.second as MTypeVar }
                is MTypeVar -> listOf(real)

                MBool, MChar, MFloat, MInt, MString, MUnit, MEmptyRow -> emptyList()

                is MConstr -> {
                    val ret = HashSet<MTypeVar>()
                    ret.addAll(recursiveFind(real.type))
                    if(real.argType.isPresent) {
                        ret.addAll(recursiveFind(real.argType.get()))
                    }
                    ret.toList()
                }

                is MTypeAlias -> {
                    recursiveFind(real.real)
                }
                is MRecord -> {
                    val ret = HashSet<MTypeVar>()
                    for(v in real.fields.values) {
                        ret.addAll(recursiveFind(v))
                    }
                    ret.addAll(recursiveFind(real.rest))
                    ret.toList()
                }

                is MDummyCons -> real.args.filter { a -> a.second is MTypeVar }.map { a -> a.second as MTypeVar }
            }
        }
    }


    fun instantiate(types: TypeSystem): MType {

        var ret = type

//        val polyRecords = findPolyRecords(ret)
//        for(record in polyRecords) {
//            if(record.rowVar !is MGeneralTypeVar) continue
//            val newRecord = MPolyRecord(record.fields, types.newTypeVar())
//            ret = ret.substitute(record, newRecord)
//        }



        for(i in typeVars.indices) {
            ret = ret.substitute(typeVars[i], types.newTypeVar())
        }
        return ret
    }

    fun instantiateBase(types: TypeSystem): MType {
        var ret = type
        for(i in typeVars.indices) {
            ret = ret.substitute(typeVars[i], types.newBaseType())
        }
        return ret
    }
}