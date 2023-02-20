package calculator

import java.lang.management.BufferPoolMXBean
import java.security.spec.ECParameterSpec
import java.util.Scanner
import java.math.BigInteger

enum class ResultCheck() {POSITIVE, NEGATIVE}

class Stack<T>() {
    private val values = mutableListOf<T>()

    fun pop(): T {
        return values.removeLast()
    }

    fun push(element: T) {
        values.add(element)
    }

    fun seeTop(): T {
        return values.last()
    }

    fun isEmpty(): Boolean {
        return values.isEmpty()
    }

    fun clear() {
        values.clear()
    }

}

class Calculator() {
    private val operatorsPriority = mutableMapOf<String, Int>("+" to 1,
        "-" to 1, "/" to 2, "*" to 2)
    private val detectVal = """-?\d+""".toRegex()
    private val detectOp = """\s+[+-]\s+""".toRegex()
    private var variables = mutableMapOf<String,String>()

    private var operatorStack = Stack<String>()
    private var resultStack = Stack<BigInteger>()
    private var result = ""
    fun mainOperation() {
        val scanner = Scanner(System.`in`)
        while (scanner.hasNext()) {
            result = ""
            // scan next input
            var input = scanner.nextLine()

            // modify input with spaces to simplify regex matching
            input = " $input "
            input = removeRedundantOperator(input)

            // Check that input is not blank
            if (input.isBlank()) {
                continue
            }

            // Check that input is not a command
            if (input.trim() == "/exit") {
                println("Bye!")
                break
            }
            if (input.trim() == "/help") {
                printHelp()
                continue
            }

            if (input.trim().first() == '/') {
                println("Unknown command")
                continue
            }

            // Check if there are multiple * or /
            val multipleWrongOperator = """[*/]{2,}""".toRegex()
            if (input.contains(multipleWrongOperator)) {
                println("Invalid expression")
                continue
            }

            if (input.count {it == '(' } != input.count {it == ')' }) {
                println("Invalid expression")
                continue
            }

            //  modify input with spaces to simplify regex matching
            input = input.replace("="," = ")
            input = input.replace(")"," ) ")
            input = input.replace("("," ( ")
            for (op in operatorsPriority.keys) {
                if (op == "-") continue
                input = input.replace(op," $op ")
            }




            // Remove multiple operations and substitute variables when possible
            input = substituteVariables(input)



            // Check if expression is valid assignment and in case save variable
            if (isAssignment(input)) {
                if (isValidAssignment(input) == ResultCheck.POSITIVE) {
                    assignVar(input)
                    continue
                } else continue
            }

            // check that mathematical expression is correct
            if (!isValidExpression(input)) {
                continue
            }

            // obtain expression in Postfix Notation
            if (obtainPostfix(input) == ResultCheck.NEGATIVE) {
                continue
            }

            // compute result
            println(computeResult())
        }
    }

    private fun printHelp() {
        println(
            """This function can determine the result of any
                |expression such as "4 + 6 - 8".
                |Follows some special input explanation:
                | * /help -> print this help message
                | * /exit -> terminate programme
            """.trimMargin())
    }

    private fun mapOperation(op: String): (BigInteger, BigInteger) -> BigInteger {
        /**
         * This function return a lambda expression corresponding to the
         * equivalent string that represent the operation
         * */
        when (op) {
            "+" -> return { op1: BigInteger, op2: BigInteger -> op1 + op2}
            "-" -> return { op1: BigInteger, op2: BigInteger -> op1 - op2}
            "/" -> return { op1: BigInteger, op2: BigInteger -> op1 / op2}
            "*" -> return { op1: BigInteger, op2: BigInteger -> op1 * op2}
        }
        return { op1: BigInteger, op2: BigInteger -> op1 }
    }

    private fun substituteVariables(exp: String): String {
        /**
         * This function substitute all variables, unless it
         * detects that a variable is being reassigned, this makes
         * the data manipulation easier, having to work only with numbers
         * */
        var result = exp
        for (i in variables) {
            if (i.key == exp.split(" ").first { it != "" } && isAssignment(exp)) {
                continue //permitting re-assignment
            }
            val matchString = """\b([=-]?)${i.key}\b""".toRegex()
            result = result.replace(matchString) { it -> it.groupValues[1]+i.value}
        }
        return result
    }

    private fun removeRedundantOperator(exp: String): String {
        /**
         * This function simplify the input by the following mathematical rule
         * -- = + and --- = -, higher numbers of operators get simplified accordingly
         * */
        var result = ""
        val multiplePlus = """\++""".toRegex()
        val multipleMinus = """-+""".toRegex()
        result = exp.replace(multiplePlus, "+")
        result = result.replace(multipleMinus) { if (it.value.length % 2 == 0) "+" else "-" }
        return result
    }

    private fun isValidExpression(expression: String): Boolean {
        /**
         * This function return false if the expression contains
         * - Numbers with invalid suffix (5+)
         * - Strings which have legit variable names but are not variables,
         * as they were not substituted previously
         * - Strings that have invalid variable names
         * */
        val invalidNumber = """\d+[+-]""".toRegex()
        val unknownVariableMatch = """\b-?[a-zA-Z]+\b""".toRegex()
        val invalidChars = """[a-zA-Z]""".toRegex()

        if (expression.contains(invalidNumber)) {
            println("Invalid expression")
            return false
        }
        if (expression.trim().first() != '/' &&
            !isAssignment(expression)) {
            if (expression.contains(unknownVariableMatch)) {
                println("Unknown variable")
                return false
            } else if (expression.contains(invalidChars)) {
                println("Invalid identifier")
                return false
            }
        }
        return true

    }

    private fun isAssignment(expression: String): Boolean {
        return expression.contains("=")
    }

    private fun isValidAssignment(expression: String): ResultCheck {
        /**
         * This function return a negative check if the expression contains
         * - Strings which have legit variable names but are not variables,
         * as they were not substituted previously
         * - Strings that are using a saved variable for reassignment but
         * the right side is not a number (there is no reason to check for
         * a right side with variable since substituteVariables is called first)
         * - Strings that have invalid variable names
         * */
        val (left, right) = expression.split("=", limit = 2).map { it.trim() }

        if ((right !in variables.keys &&
                    right.matches("""[a-zA-Z]+""".toRegex()))
        ) {
            println("Unknown variable")
            return ResultCheck.NEGATIVE
        }


        if (left in variables.keys && !right.matches("""-?\d+""".toRegex())) {
            println("Invalid assignment")
            return ResultCheck.NEGATIVE
        }

        if (!left.matches("""[a-zA-Z]+""".toRegex()) || !right.matches("""-?\d+""".toRegex())) {
            println("Invalid identifier")
            return ResultCheck.NEGATIVE
        }
        return ResultCheck.POSITIVE
    }

    private fun assignVar(expression: String) {
        /**
         * This function takes a string assignment and add
         * the detected variable and value to a dictionary
         * */
        val variableRegex = """[a-zA-Z]+""".toRegex()
        val valueRegex = """-?\d+""".toRegex()
        val variable = variableRegex.find(expression)!!.value
        val value = valueRegex.find(expression)!!.value
        variables[variable] = value
    }

    // Functions that implement expression determination
    private fun obtainPostfix(expression: String): ResultCheck {
        /**
         * Implementation of the algorithm that takes an expression and
         * returns its equivalent in Reverse Polish Notation
         * */
        val units = expression.split(" ").filter { it != "" }
        for (unit in units) {
            if (unit !in operatorsPriority.keys && unit !in "()") {
                result += "$unit "
            } else if (operatorStack.isEmpty() || operatorStack.seeTop() == "(") {
                operatorStack.push(unit)
            } else if ( unit !in "()" && operatorsPriority[unit]!! > operatorsPriority[operatorStack.seeTop()]!!) {
                operatorStack.push(unit)
            } else if ( unit !in "()" && operatorsPriority[unit]!! <= operatorsPriority[operatorStack.seeTop()]!!) {
                val currPriority = operatorsPriority[operatorStack.seeTop()]!!
                while (true) {
                    if ( operatorStack.isEmpty() || operatorStack.seeTop() == "(" || currPriority > operatorsPriority[operatorStack.seeTop()]!!) {
                        break
                    }
                    result += "${operatorStack.pop()} "
                }
                operatorStack.push(unit)
            } else if (unit == "(") {
                operatorStack.push(unit)
            } else if (unit == ")") {
                while (true) {
                    if (operatorStack.seeTop() == "(") {
                        operatorStack.pop()
                        break
                    }
                    result += "${operatorStack.pop()} "
                }
            }


        }

        while (!operatorStack.isEmpty()) {
            result += "${operatorStack.pop()} "
        }
        if (result.contains("(") || result.contains(")")) {
            println("Invalid expression")
            return ResultCheck.NEGATIVE
        }
        return ResultCheck.POSITIVE
    }

    private fun computeResult(): BigInteger {
        /**
         * This function takes the expression in RPN and thanks to a
         * stack computes the result, considering operation priority
         * */
        val components = result.split(" ").filter { it != "" }
        for (unit in components) {
            if (unit !in operatorsPriority.keys) {
                resultStack.push(unit.toBigInteger())
            } else {
                val secondOp = resultStack.pop()
                val firstOp = resultStack.pop()
                val partial = mapOperation(unit)(firstOp, secondOp)
                resultStack.push(partial)
            }
        }
        val result = resultStack.seeTop()
        resultStack.clear()
        return result
    }


}

fun main() {
    val calculator = Calculator()
    calculator.mainOperation()
}
