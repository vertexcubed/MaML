package vertexcubed.maml.type

import vertexcubed.maml.core.BindException
import vertexcubed.maml.core.UnifyException

data class MRecord(val fields: Map<String, MType>, val rest: MType): MTypeCon(RECORD_ID, fields.values.toList() + listOf(rest)) {


    /**
     * Why do this iteratively? Uhh avoid concating maps i guess
     */
    fun flatten(): Pair<Map<String, MType>, MType> {
        val out = mutableMapOf<String, MType>()
        var trav: MType = this
        while(trav !is MEmptyRow) {
            if(trav !is MRecord) {
                break
            }
            out.putAll(trav.fields)
            trav = trav.rest.find()
        }
        return out to trav
    }


    override fun contains(other: MType): Boolean {
        for(v in fields.values) {
            if(v.contains(other)) return true
        }
        return rest.contains(other)
    }

    override fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        val otherType = other.find()
        if(otherType is MTypeVar) {
            if(looser) throw BindException(this, otherType)

            return otherType.unify(this, typeSystem, looser)
        }
        if(otherType !is MRecord) {
            throw UnifyException(this, otherType)
        }

        val (myFields, myRest) = flatten()
        val (otherFields, otherRest) = otherType.flatten()

        val myMissing = mutableMapOf<String, MType>()
        val otherMissing = mutableMapOf<String, MType>()

        val allKeys = myFields.keys + otherFields.keys
        for(k in allKeys) {
            val myVal = myFields[k]
            val otherVal = otherFields[k]
            if(myVal != null && otherVal != null) {
                myVal.unify(otherVal, typeSystem, looser)
            }
            else if(myVal == null) {
                if(otherVal == null) throw UnifyException(this, otherType)
                myMissing[k] = otherVal
            }
            else {
                //We know otherVal == false and myVal == true
                otherMissing[k] = myVal
            }
        }
        if(myMissing.isEmpty() && otherMissing.isEmpty()) {
            return myRest.unify(otherRest, typeSystem, looser)
        }
        if(myMissing.isEmpty()) {
            return otherRest.unify(MRecord(otherMissing, myRest), typeSystem, looser)
        }
        if(otherMissing.isEmpty()) {
            return myRest.unify(MRecord(myMissing, otherRest), typeSystem, looser)
        }

        val newType = typeSystem.newTypeVar()
        myRest.unify(MRecord(myMissing, newType), typeSystem, looser)
        otherRest.unify(MRecord(otherMissing, newType), typeSystem, looser)
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType is MTypeVar) return otherType.isSame(this)
        if(otherType !is MRecord) return false
        val allFields = fields.keys + otherType.fields.keys
        for(k in allFields) {
            if(k !in otherType.fields || k !in fields) {
                return false
            }
            if(!fields[k]!!.isSame(otherType.fields[k]!!)) {
                return false
            }
        }
        return rest.isSame(otherType.rest)
    }


    override fun substitute(from: MType, to: MType): MType {
        return MRecord(fields.mapValues { (_, v) -> v.substitute(from, to) }, rest.substitute(from, to))
    }

    override fun toString(): String {
        var str = "{"
        str += fields.toList().joinToString("; ") { (k, v) -> "$k: $v" }
        if(rest is MEmptyRow) {
            return "$str }"
        }
        return "$str; ... $rest}"
    }

    override fun asString(env: TypeEnv): String {
        val (fields, rest) = flatten()
        var str = "{"
        str += fields.toList().joinToString("; ") { (k, v) -> "$k: ${v.asString(env)}" }
        if(rest is MEmptyRow) {
            return "$str }"
        }
        if(fields.isNotEmpty()) {
            str += ";"
        }
        return "$str .. }"
    }
}

data object MEmptyRow: MTypeCon(EMPTY_ROW_ID, emptyList()) {
    override fun substitute(from: MType, to: MType): MType {
        return MEmptyRow
    }
}