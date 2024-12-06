package qwerdsa53;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private final List<String> lines;
    private int currentLine;
    private final Map<String, Object> constants = new HashMap<>();

    public Parser(String input) {
        this.lines = Arrays.asList(input.split("\n"));
        this.currentLine = 0;
    }

    public Map<String, Object> parse() throws ParseException {
        Map<String, Object> root = new HashMap<>();
        while (currentLine < lines.size()) {
            String line = lines.get(currentLine).trim();
            if (line.isEmpty() || line.startsWith("*>")) {
                currentLine++;
                continue;
            }
            if (line.startsWith("const ")) {
                parseConst(line);
            } else if (line.startsWith("([")) {
                Map<String, Object> dict = parseDictionary(true);
                root.putAll(dict);
            } else {
                throw new ParseException("Неожиданный токен: " + line, currentLine + 1);
            }
            currentLine++;
        }
        return root;
    }

    private void parseConst(String line) throws ParseException {
        Pattern pattern = Pattern.compile("^const\\s+([a-zA-Z][_a-zA-Z0-9]*)\\s*=\\s*(.+)$");
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            throw new ParseException("Неверный синтаксис объявления константы: " + line, currentLine + 1);
        }
        String name = matcher.group(1);
        String valueStr = matcher.group(2).trim();
        Object value = parseValue(valueStr);
        constants.put(name, value);
    }

    private Map<String, Object> parseDictionary(boolean expectStart) throws ParseException {
        Map<String, Object> dict = new HashMap<>();
        String startLine = lines.get(currentLine).trim();
        if (expectStart) {
            if (!startLine.startsWith("([")) {
                throw new ParseException("Ожидался старт словаря ([", currentLine + 1);
            }
            currentLine++;
        }
        while (currentLine < lines.size()) {
            String line = lines.get(currentLine).trim();
            if (line.startsWith("])")) {
                break;
            }
            if (line.isEmpty() || line.startsWith("*>")) {
                currentLine++;
                continue;
            }
            Pattern entryPattern = Pattern.compile("^([a-zA-Z][_a-zA-Z0-9]*)\\s*:\\s*(.+?)(,?)$");
            Matcher matcher = entryPattern.matcher(line);
            if (!matcher.find()) {
                throw new ParseException("Неверный синтаксис записи словаря: " + line, currentLine + 1);
            }
            String key = matcher.group(1);
            String valueStr = matcher.group(2).trim();
            if (valueStr.endsWith(",")) {
                valueStr = valueStr.substring(0, valueStr.length() - 1).trim();
            }
            Object value = parseValue(valueStr);
            dict.put(key, value);
            currentLine++;
        }
        if (currentLine >= lines.size()) {
            throw new ParseException("Не найдено закрывающих символов ]) для словаря", currentLine + 1);
        }
        return dict;
    }

    private Object parseValue(String valueStr) throws ParseException {
        if (valueStr.startsWith("@{") && valueStr.endsWith("}")) {
            String expr = valueStr.substring(2, valueStr.length() - 1).trim();
            return evaluateExpression(expr);
        } else if (valueStr.equals("([")) {
            currentLine++;
            return parseDictionary(false);
        } else {
            if (isNumeric(valueStr)) {
                return parseNumber(valueStr);
            } else {
                if (constants.containsKey(valueStr)) {
                    return constants.get(valueStr);
                } else {
                    throw new ParseException("Неизвестное значение или константа: " + valueStr, currentLine + 1);
                }
            }
        }
    }

    private Object evaluateExpression(String expr) throws ParseException {
        String[] tokens = expr.split("\\s+");
        Stack<BigDecimal> stack = new Stack<>();
        for (String token : tokens) {
            if (isNumeric(token)) {
                stack.push(new BigDecimal(token));
            } else if (constants.containsKey(token)) {
                Object constVal = constants.get(token);
                if (constVal instanceof BigDecimal) {
                    stack.push((BigDecimal) constVal);
                } else if (constVal instanceof Long) {
                    stack.push(new BigDecimal((Long) constVal));
                } else if (constVal instanceof Integer) {
                    stack.push(new BigDecimal((Integer) constVal));
                } else {
                    throw new ParseException("Константа " + token + " не является числом", currentLine + 1);
                }
            } else {
                switch (token) {
                    case "+":
                        if (stack.size() < 2)
                            throw new ParseException("Недостаточно операндов для +", currentLine + 1);
                        BigDecimal bAdd = stack.pop();
                        BigDecimal aAdd = stack.pop();
                        stack.push(aAdd.add(bAdd));
                        break;
                    case "-":
                        if (stack.size() < 2)
                            throw new ParseException("Недостаточно операндов для -", currentLine + 1);
                        BigDecimal bSub = stack.pop();
                        BigDecimal aSub = stack.pop();
                        stack.push(aSub.subtract(bSub));
                        break;
                    case "*":
                        if (stack.size() < 2)
                            throw new ParseException("Недостаточно операндов для *", currentLine + 1);
                        BigDecimal bMul = stack.pop();
                        BigDecimal aMul = stack.pop();
                        stack.push(aMul.multiply(bMul));
                        break;
                    case "/":
                        if (stack.size() < 2)
                            throw new ParseException("Недостаточно операндов для /", currentLine + 1);
                        BigDecimal bDiv = stack.pop();
                        BigDecimal aDiv = stack.pop();
                        if (bDiv.compareTo(BigDecimal.ZERO) == 0)
                            throw new ParseException("Деление на ноль", currentLine + 1);
                        stack.push(aDiv.divide(bDiv, 10, RoundingMode.HALF_UP));
                        break;
                    case "min()":
                        if (stack.size() < 2)
                            throw new ParseException("Недостаточно операндов для min()", currentLine + 1);
                        BigDecimal bMin = stack.pop();
                        BigDecimal aMin = stack.pop();
                        stack.push(aMin.min(bMin));
                        break;
                    default:
                        throw new ParseException("Неизвестный оператор или функция: " + token, currentLine + 1);
                }
            }
        }
        if (stack.size() != 1) {
            throw new ParseException("Неверное количество операндов после вычисления выражения", currentLine + 1);
        }
        BigDecimal result = stack.pop();
        result = result.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
        return result;
    }


    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private Object parseNumber(String str) {
        if (str.contains(".")) {
            return new BigDecimal(str);
        } else {
            return Long.parseLong(str);
        }
    }

}
