package net.vortexdevelopment.vortexcore.hooks.plugin.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ConditionParser {

    private final static boolean placeholderAPIEnabled;

    static {
        boolean enabled = false;
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            enabled = true;
        } catch (ClassNotFoundException e) {
            // PlaceholderAPI not found
        }
        placeholderAPIEnabled = enabled;
    }

    // Parse and evaluate a condition
    public static boolean evaluateCondition(String condition, Player player) {
        if (!placeholderAPIEnabled) {
            VortexPlugin.getInstance().getLogger().warning("PlaceholderAPI is not enabled. Conditions will not be evaluated.");
            return false; // PlaceholderAPI is not enabled, return false
        }
        try {
            List<String> tokens = tokenize(condition);
            List<String> postfix = toPostfix(tokens);
            return evaluatePostfix(postfix, player);
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Return false if there's an error
        }
    }

    // Tokenize the condition string
    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                continue; // Skip whitespace
            }

            // Handle multi-character operators like ==, !=, >=, <=, &&, ||
            if ((c == '<' || c == '>' || c == '=' || c == '!' || c == '&' || c == '|') && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if ((c == '&' && next == '&') || (c == '|' && next == '|') || next == '=') {
                    if (!current.isEmpty()) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                    tokens.add(c + "" + next);
                    i++; // Skip the next character
                    continue;
                }
            }

            // Handle single-character operators and parentheses
            if ("()<>!=&|".indexOf(c) != -1) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                current.append(c); // Build multi-character token (e.g., variable names, numbers)
            }
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    // Convert tokens to postfix (Reverse Polish Notation)
    private static List<String> toPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();

        Map<String, Integer> precedence = Map.of(
                "||", 1,
                "&&", 2,
                "==", 3, "!=", 3,
                ">", 4, "<", 4, ">=", 4, "<=", 4,
                "(", 0, ")", 0
        );

        for (String token : tokens) {
            if (token.matches("[a-zA-Z0-9._%]+")) {
                output.add(token); // Operand
            } else if (token.equals("(")) {
                operators.push(token);
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }
                operators.pop(); // Remove "(" from stack
            } else {
                while (!operators.isEmpty() && precedence.getOrDefault(token, 0) <= precedence.getOrDefault(operators.peek(), 0)) {
                    output.add(operators.pop());
                }
                operators.push(token);
            }
        }

        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }

        return output;
    }

    // Evaluate the postfix expression
    private static boolean evaluatePostfix(List<String> postfix, Player player) {
        Stack<Object> stack = new Stack<>();

        for (String token : postfix) {
            if (token.matches("[a-zA-Z0-9._%]+")) {
                // Resolve placeholder or treat as a literal value
                String resolved = player == null ? token : PlaceholderAPI.setPlaceholders(player, token);

                // Check if the placeholder wasn't properly resolved (still contains % characters)
                if (resolved.contains("%") && resolved.matches(".*%[a-zA-Z0-9._]+%.*")) {
                    // Extract the unparsed placeholder for the warning message
                    String unparsedPlaceholder = resolved.replaceAll(".*?(%[a-zA-Z0-9._]+%).*", "$1");

                    // Print a warning message with the unparsed placeholder
                    VortexPlugin.getInstance().getLogger().warning("[ConditionParser] Warning: Unable to parse placeholder " + unparsedPlaceholder +
                            " for player " + (player != null ? player.getName() : "null"));

                    // Placeholder couldn't be parsed, return false immediately
                    return false;
                }

                // Try to parse as number, boolean, or leave as string
                if (resolved.equalsIgnoreCase("true") || resolved.equalsIgnoreCase("false")) {
                    stack.push(Boolean.parseBoolean(resolved));
                } else {
                    try {
                        stack.push(Double.parseDouble(resolved));
                    } catch (NumberFormatException e) {
                        stack.push(resolved); // Treat as string
                    }
                }
            } else if ("||&&==!=><>=<=".contains(token)) {
                if (stack.size() < 2) {
                    throw new IllegalStateException("Invalid postfix expression: insufficient operands for operator " + token);
                }

                Object b = stack.pop();
                Object a = stack.pop();
                stack.push(evaluateOperation(a, b, token));
            }
        }

        if (stack.size() != 1) {
            throw new IllegalStateException("Invalid postfix expression: stack size after evaluation is " + stack.size());
        }

        return (Boolean) stack.pop();
    }


    // Evaluate a single operation
    private static boolean evaluateOperation(Object a, Object b, String operator) {
        return switch (operator) {
            case "||" -> (Boolean) a || (Boolean) b;
            case "&&" -> (Boolean) a && (Boolean) b;
            case "==" -> a.equals(b);
            case "!=" -> !a.equals(b);
            case ">" -> ((Double) a) > ((Double) b);
            case "<" -> ((Double) a) < ((Double) b);
            case ">=" -> ((Double) a) >= ((Double) b);
            case "<=" -> ((Double) a) <= ((Double) b);
            default -> throw new IllegalArgumentException("Invalid operator: " + operator);
        };
    }
}

