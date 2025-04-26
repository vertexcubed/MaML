package vertexcubed.maml.bytecode

import java.util.*
import vertexcubed.maml.bytecode.MVirtualMachine.OpCode.*
import kotlin.collections.ArrayList

class MVirtualMachine {

    val argStack: Deque<Value> = LinkedList()


    var retTop: Pair<Int, LinkedList<Value>> = 0 to LinkedList()
    val retStack: Deque<RetBlock> = LinkedList()

    var env: ArrayList<Value> = ArrayList()




    var accumulator: Value = IntValue(0)


    fun run() {

        /*
            Function to add any two numbers would look like this:
            NOTE: this is basically just with the basic ZAM instructions

            BRANCH(L2)
            ==Function Body==
    L1:
            //First arg already grabbed
            GRAB -> grabs off the stack (second arg) and adds to env
            ACCESS(0) -> second argument (y)
            PUSH -> pushes from acc to stack
            ACCESS(1) -> first argument (x)
            ADD -> adds acc + first off stack, sets value into accumulator
            RETURN -> jumps back, applying the return closure and environment
            ==End Function Body==

    L2:     CLOSURE(L1) -> creates a closure with the current environment in the accumulator
            PUSH -> pushes to stack
            GRAB -> grabs from stack, puts in the environment
            PUSHMARK -> push a mark into the stack
            CONST(3) -> acc = 3
            PUSH -> push
            CONST(2) -> acc = 2
            PUSH
            ACCESS(0) -> access closure
            APPLY -> applies the closure to the arguments. Sets the pc and environment to the closure's

         */





        val instructions = listOf(

            //L2 = 7
            Instruction(BRANCH, 7),
            //L1 = 1
            Instruction(GRAB),
            Instruction(ACCESS, 0),
            Instruction(PUSH),
            Instruction(ACCESS, 1),
            Instruction(ADDINT),
            Instruction(RETURN),

            Instruction(CLOSURE, 1),
            Instruction(PUSH),
            Instruction(GRAB),
            Instruction(PUSHMARK),
            Instruction(CONST, 3),
            Instruction(PUSH),
            Instruction(CONST, 2),
            Instruction(PUSH),
            Instruction(ACCESS, 0),
            Instruction(APPLY),
        )

        var ip = 0


        while(ip < instructions.size) {
            val instr = instructions[ip++]
            when(instr.op) {

                // Creates a closure and stores it in the accumulator
                CLOSURE -> {

                    // Save volatile part of environment
                    val newEnv = ArrayList(retTop.second)
                    newEnv.addAll(env)
                    env = newEnv
                    // Wipe volatile part
                    retTop = 0 to LinkedList()

                    // Create closure
                    accumulator = Closure(instr.operand, ArrayList(env))
                }
                // Sets the accumulator to some constant
                CONST -> {
                    accumulator = IntValue(instr.operand)
                }
                // Pushes the current accumulator value to the arg stack.
                PUSH -> {
                    argStack.push(accumulator)
                }
                // Pushes a mark onto the arg stack, denoting the end of a function application.
                PUSHMARK -> {
                    argStack.push(Mark)
                }

                // Grabs the first value off the stack.
                // If this value is a mark, it applies the return closure
                // and adds the current closure to the accumulator
                // Failing on a grab enables partial application of functions
                GRAB -> {
                    val top = argStack.pop()
                    if(top is Mark) {
                        // new closure env
                        val newEnv = ArrayList(retTop.second)
                        newEnv.addAll(env)

                        accumulator = Closure(ip, newEnv)

                        // Apply closure on retStack
                        val ret = retStack.pop()

                        ip = ret.ip
                        env = ret.env
                        retTop = ret.volatileSize to ret.volatile

                    }
                    else {
                        // Push to volatile env
                        retTop.second.push(top)
                        retTop = retTop.first + 1 to retTop.second
                    }
                }

                // Tail recursive application: does not create a new closure
                APPLYTERM -> {

                    val closure = accumulator as Closure
                    // Pop the first arg off the stack - saves a GRAB call
                    val arg = argStack.pop()
                    ip = closure.progCounter
                    env = closure.env
                    // Push the arg we popped off into the environment
                    retTop.second.push(arg)
                    // Update size
                    retTop = retTop.first + 1 to retTop.second
                }

                // Applies the closure currently in the accumulator.
                APPLY -> {
                    // Push return point to the stack
                    retStack.push(RetBlock(ip, ArrayList(env), retTop.first, LinkedList(retTop.second)))

                    // Pop the first arg off the stack - saves a GRAB call
                    val arg = argStack.pop()
                    val closure = accumulator as Closure

                    // Applies the current closure
                    ip = closure.progCounter
                    env = closure.env

                    // Pushes the arg we popped off into the volatile environment
                    retTop = 1 to LinkedList()
                    retTop.second.push(arg)
                }

                // Accesses a local variable from the environment, and stores it in the accumulator
                ACCESS -> {
                    // access op is in the volatile cache
                    if(retTop.first > instr.operand) {
                        accumulator = retTop.second[instr.operand]
                    }
                    else {
                        // access op is in the stable env
                        accumulator = env[instr.operand]
                    }
                }

                // Returns back after a function call
                RETURN -> {
                    val top = argStack.pop()
                    if(top is Mark) {
                        // new closure env
                        val newEnv = ArrayList(retTop.second)
                        newEnv.addAll(env)

                        // Apply closure on retStack
                        val ret = retStack.pop()

                        ip = ret.ip
                        env = ret.env
                        retTop = ret.volatileSize to ret.volatile
                    }
                    else {

                        // Perform over application - f: 'a -> 'b -> 'c applied to f 1 2 3
                        val ret = accumulator as Closure
                        ip = ret.progCounter
                        env = ret.env
                        // Push from stack to volatile env
                        retTop = 1 to LinkedList()
                        retTop.second.push(top)
                    }
                }

                // Conditionless jump
                BRANCH -> {
                    ip = instr.operand
                }

                // Add instruction
                ADDINT -> {
                    val other = argStack.pop() as IntValue
                    accumulator = IntValue((accumulator as IntValue).value + other.value)
                }

                // Copies the value in the accumulator and puts it in the environment.
                LET -> {
                    retTop.second.push(accumulator)
                    retTop = retTop.first + 1 to retTop.second
                }

                // Destroys the last environment value, aka. the local let
                ENDLET -> {
                    if(retTop.first > 0) {
                        retTop.second.pop()
                        retTop = retTop.first - 1 to retTop.second
                    }
                    else {
                        // No volatile environment. Turn old environment into volatile.
                        retTop = (env.size - 1) to LinkedList()
                        for(i in 1 until env.size) {
                            retTop.second.addLast(env[i])
                        }
                        env = ArrayList()
                    }
                }

                // Adds a "Dummy" value to the environment, for use in recursive lets.
                // This dummy will be updated later.
                DUMMY -> {
                    // Push to volatile
                    retTop.second.push(Dummy)
                    retTop = retTop.first + 1 to retTop.second
                }

                // Replaces a dummy value with a proper closure stored in accumulator
                UPDATE -> {
                    // If volatile: override on volatile
                    if(retTop.first > 0) {
                        retTop.second[0] = (accumulator)
                    }
                    // If not volatile: override on regular env
                    else {
                        env[0] = accumulator
                    }
                }
            }
        }

        println(argStack)
        println(accumulator)

    }

    enum class OpCode {

        ADDINT,
        CONST,
        CLOSURE,
        ACCESS,
        PUSH,
        PUSHMARK,
        APPLY,
        APPLYTERM,
        GRAB,
        RETURN,
        BRANCH,
        LET,
        ENDLET,
        DUMMY,
        UPDATE,
    }

    data class Instruction(val op: OpCode, val operand: Int) {
        constructor(op: OpCode): this(op, 0)
    }
}


/*
    Values are represented differently from how they are in ZINC / Caml / OCaml
    Java doesn't have fine grained memory manipulation, and trying to implement
    my own systems for it would be out of scope for this project, so I'm just
    going to wrap them in types and let the JVM handle everything
 */

sealed class Value()

data class IntValue(val value: Int) : Value()

data object Mark: Value()

data class Closure(val progCounter: Int, val env: ArrayList<Value>): Value()

data object Dummy: Value()

data class ConstCtor(val id: Int)
data class Ctor(val id: Int, val args: Array<Value>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Ctor

        if (id != other.id) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + args.contentHashCode()
        return result
    }
}


// Used in retStack - easier to represent
data class RetBlock(val ip: Int, val env: ArrayList<Value>, val volatileSize: Int, val volatile: LinkedList<Value>)