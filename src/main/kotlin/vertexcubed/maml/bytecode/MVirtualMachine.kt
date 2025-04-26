package vertexcubed.maml.bytecode

import java.util.*
import vertexcubed.maml.bytecode.MVirtualMachine.OpCode.*
import kotlin.collections.ArrayList

class MVirtualMachine {

    val argStack: Deque<Value> = LinkedList()
    val retStack: Deque<Value> = LinkedList()


    var accumulator: Value = IntValue(0)

    var env: LinkedList<Value> = LinkedList()

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
                    accumulator = Closure(instr.operand, LinkedList(env))
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
                        accumulator = Closure(ip, LinkedList(env))
                        val ret = retStack.pop() as Closure
                        ip = ret.ip
                        env = ret.env
                    }
                    else {
                        env.push(top)
                    }
                }

                // Tail recursive application: does not create a new closure
                APPLYTERM -> {

                    val closure = accumulator as Closure
                    // Pop the first arg off the stack - saves a GRAB call
                    val arg = argStack.pop()
                    ip = closure.ip
                    env = closure.env
                    // Push the arg we popped off into the environment
                    env.push(arg)
                }

                // Applies the closure currently in the accumulator.
                APPLY -> {
                    // Push return point to the stack
                    retStack.push(Closure(ip, LinkedList(env)))

                    // Pop the first arg off the stack - saves a GRAB call
                    val arg = argStack.pop()
                    val closure = accumulator as Closure

                    // Applies the current closure
                    ip = closure.ip
                    env = closure.env
                    // Pushes the arg we popped off into the environment
                    env.push(arg)
                }

                // Accesses a local variable from the environment, and stores it in the accumulator
                ACCESS -> {
                    val value = env[instr.operand]
                    accumulator = value
                }

                // Returns back after a function call
                RETURN -> {
                    val top = argStack.pop()
                    if(top is Mark) {
                        val ret = retStack.pop() as Closure
                        ip = ret.ip
                        env = ret.env
                    }
                    else {
                        // Over application
                        val ret = accumulator as Closure
                        ip = ret.ip
                        env = ret.env
                        env.push(top)
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
                    env.push(accumulator)
                }

                // Destroys the last environment value, aka. the local let
                ENDLET -> {
                    env.pop()
                }

                // Adds a "Dummy" value to the environment, for use in recursive lets.
                // This dummy will be updated later.
                DUMMY -> {
                    env.push(Dummy)
                }

                UPDATE -> {
                    val top = env.pop()
                    if(top !is Dummy) {
                        //TODO: do something here in case of this failing - probably like throwing an error

                    }
                    env.push(accumulator)
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


sealed class Value()

data class IntValue(val value: Int) : Value()

data object Mark: Value()

data class Closure(val ip: Int, val env: LinkedList<Value>): Value()

data object Dummy: Value()
