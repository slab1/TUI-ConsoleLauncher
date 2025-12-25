package ohi.andre.consolelauncher.commands.smartlauncher.productivity;

import android.content.Context;

import java.util.Stack;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Smart Launcher Calculator Command
 * Evaluates mathematical expressions
 */
public class CalculatorCommand implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Context context = pack.context;
        
        // Get the expression from arguments
        String expression = "";
        for (Object arg : pack.mArgs) {
            if (arg instanceof String) {
                expression += " " + arg;
            }
        }
        expression = expression.trim();
        
        if (expression.isEmpty()) {
            return "Usage: calc <expression>\nExample: calc 2 + 2 * 3";
        }
        
        try {
            double result = evaluateExpression(expression);
            return "\nResult: " + result + "\n";
        } catch (Exception e) {
            return "Error: " + e.getMessage() + "\nCheck your expression syntax.";
        }
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.PLAIN_TEXT};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return 0;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return "Calculator requires an expression";
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return "Calculator requires an expression\nExample: calc 2 + 2";
    }

    /**
     * Evaluates a mathematical expression using reverse Polish notation (RPN)
     */
    private double evaluateExpression(String expression) throws Exception {
        // Clean expression
        expression = expression.replaceAll("\\s+", "");
        expression = expression.replace("×", "*");
        expression = expression.replace("÷", "/");
        
        // Convert to RPN
        String rpn = infixToRPN(expression);
        
        // Evaluate RPN
        Stack<Double> stack = new Stack<>();
        String[] tokens = rpn.split(" ");
        
        for (String token : tokens) {
            if (isOperator(token)) {
                if (stack.size() < 2) {
                    throw new Exception("Invalid expression");
                }
                double b = stack.pop();
                double a = stack.pop();
                stack.push(applyOperator(token, a, b));
            } else {
                try {
                    stack.push(Double.parseDouble(token));
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid number: " + token);
                }
            }
        }
        
        if (stack.size() != 1) {
            throw new Exception("Invalid expression");
        }
        
        return stack.pop();
    }

    /**
     * Converts infix expression to Reverse Polish Notation
     */
    private String infixToRPN(String expression) throws Exception {
        Stack<String> operators = new Stack<>();
        StringBuilder output = new StringBuilder();
        
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            if (Character.isDigit(c) || c == '.') {
                // Read full number
                StringBuilder number = new StringBuilder();
                while (i < expression.length() && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    number.append(expression.charAt(i));
                    i++;
                }
                i--; // Backtrack one position
                output.append(number).append(" ");
            } else if (c == '(') {
                operators.push(String.valueOf(c));
            } else if (c == ')') {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.append(operators.pop()).append(" ");
                }
                if (operators.isEmpty()) {
                    throw new Exception("Mismatched parentheses");
                }
                operators.pop(); // Remove '('
            } else if (isOperator(String.valueOf(c))) {
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(String.valueOf(c))) {
                    output.append(operators.pop()).append(" ");
                }
                operators.push(String.valueOf(c));
            }
        }
        
        while (!operators.isEmpty()) {
            String op = operators.pop();
            if (op.equals("(") || op.equals(")")) {
                throw new Exception("Mismatched parentheses");
            }
            output.append(op).append(" ");
        }
        
        return output.toString().trim();
    }

    private boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/");
    }

    private int precedence(String op) {
        switch (op) {
            case "+":
            case "-":
                return 1;
            case "*":
            case "/":
                return 2;
            default:
                return 0;
        }
    }

    private double applyOperator(String op, double a, double b) throws Exception {
        switch (op) {
            case "+":
                return a + b;
            case "-":
                return a - b;
            case "*":
                return a * b;
            case "/":
                if (b == 0) {
                    throw new Exception("Division by zero");
                }
                return a / b;
            default:
                throw new Exception("Unknown operator: " + op);
        }
    }
}
