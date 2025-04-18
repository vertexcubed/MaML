package vertexcubed.maml.type

data class ForAll(val typeVars: List<MGeneralTypeVar>, val type: MType) {

    companion object {
        fun generalize(type: MType, types: TypeSystem): ForAll {
            var newType = type

            val typeList = ArrayList<MGeneralTypeVar>()

            val polyRecords = findPolyRecords(newType)
            for(record in polyRecords) {
                var newRecord: MType = MPolyRecord(record.fields, types.newGeneralType())


                val freeVars = recursiveFind(newRecord)
                if(freeVars.isEmpty()) {
                    continue
                }
                typeList.add(types.newGeneralType())
                newRecord = newRecord.substitute(freeVars[0], typeList[0])
                for(i in 1 until freeVars.size) {
                    typeList.add(types.newGeneralType())
                    newRecord = newRecord.substitute(freeVars[i], typeList[i])
                }

                newType = newType.substitute(record, newRecord)
            }





            val freeVars = recursiveFind(newType)
            if(freeVars.isEmpty()) {
                return ForAll(typeList, newType)
            }
            typeList.add(types.newGeneralType())
            var retType = newType.substitute(freeVars[0], typeList[0])
            for(i in 1 until freeVars.size) {
                typeList.add(types.newGeneralType())
                retType = retType.substitute(freeVars[i], typeList[i])
            }
            return ForAll(typeList, retType)
        }

        fun empty(type: MType): ForAll {
            return ForAll(emptyList(), type)
        }


        private fun findPolyRecords(type: MType): List<MPolyRecord> {
            return when(val real = type.find()) {
                is MFunction -> {
                    val ret = HashSet<MPolyRecord>()
                    val first = findPolyRecords(real.arg)
                    val second = findPolyRecords(real.ret)
                    ret.addAll(first)
                    ret.addAll(second)
                    ret.toList()
                }
                is MGeneralTypeVar -> emptyList()
                is MTuple -> {
                    val ret = ArrayList<MPolyRecord>()
                    for(t in real.types) {
                        val vars = findPolyRecords(t)
                        ret.addAll(vars)
                    }
                    ret
                }
                is MVariantType -> real.args.filter { a -> a.second is MPolyRecord }.map { a -> a.second as MPolyRecord }
                is MTypeVar -> emptyList()

                MBool, MChar, MFloat, MInt, MString, MUnit -> emptyList()

                is MConstr -> {
                    val ret = HashSet<MPolyRecord>()
                    ret.addAll(findPolyRecords(real.type))
                    if(real.argType.isPresent) {
                        ret.addAll(findPolyRecords(real.argType.get()))
                    }
                    ret.toList()
                }
                is MPolyRecord -> {
                    listOf(real)
                }
                is MStaticRecord -> {
                    val ret = HashSet<MPolyRecord>()
                    ret.addAll(real.fields.filterValues { v -> v is MPolyRecord }.toList().map { a -> a.second as MPolyRecord })
                    ret.toList()
                }
                is MTypeAlias -> {
                    findPolyRecords(real.real)
                }
                is ModuleType -> emptyList()
            }
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

                MBool, MChar, MFloat, MInt, MString, MUnit -> emptyList()

                is MConstr -> {
                    val ret = HashSet<MTypeVar>()
                    ret.addAll(recursiveFind(real.type))
                    if(real.argType.isPresent) {
                        ret.addAll(recursiveFind(real.argType.get()))
                    }
                    ret.toList()
                }
                is MPolyRecord -> {
                    val ret = HashSet<MTypeVar>()
                    ret.addAll(real.fields.filterValues { v -> v is MTypeVar }.toList().map { a -> a.second as MTypeVar })
                    ret.toList()
                }
                is MStaticRecord -> {
                    val ret = HashSet<MTypeVar>()
                    ret.addAll(real.fields.filterValues { v -> v is MTypeVar }.toList().map { a -> a.second as MTypeVar })
                    ret.toList()
                }
                is MTypeAlias -> {
                    recursiveFind(real.real)
                }
                is ModuleType -> emptyList()
            }
        }
    }


    fun instantiate(types: TypeSystem): MType {

        var ret = type

        val polyRecords = findPolyRecords(ret)
        for(record in polyRecords) {
            if(record.rowVar !is MGeneralTypeVar) continue
            val newRecord = MPolyRecord(record.fields, types.newTypeVar())
            ret = ret.substitute(record, newRecord)
        }



        for(i in typeVars.indices) {
            ret = ret.substitute(typeVars[i], types.newTypeVar())
        }
        return ret
    }
}