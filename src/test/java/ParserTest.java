import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qwerdsa53.ParseException;
import qwerdsa53.Parser;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    @DisplayName("Тестирование парсинга корректного конфигурационного файла с вложенными словарями")
    void testParseValidConfig() throws ParseException {
        String config = """
                *> Это комментарий
                const pi = 3.1415
                const radius = 10
                ([
                    area : @{radius radius * pi *},
                    circumference : @{radius 2 * pi *},
                    nested : ([
                        innerConst : @{radius 5 +}
                    ])
                ])
                """;

        Parser parser = new Parser(config);
        Map<String, Object> result = parser.parse();

        assertNotNull(result, "Результат парсинга не должен быть null");
        assertTrue(result.containsKey("area"), "Результат должен содержать ключ 'area'");
        assertTrue(result.containsKey("circumference"), "Результат должен содержать ключ 'circumference'");
        assertTrue(result.containsKey("nested"), "Результат должен содержать ключ 'nested'");

        Object area = result.get("area");
        assertTrue(area instanceof BigDecimal, "'area' должен быть типа BigDecimal");
        assertEquals(new BigDecimal("314.15"), (BigDecimal) area, "Значение 'area' неверно");

        Object circumference = result.get("circumference");
        assertTrue(circumference instanceof BigDecimal, "'circumference' должен быть типа BigDecimal");
        assertEquals(new BigDecimal("62.83"), (BigDecimal) circumference, "Значение 'circumference' неверно");

        Object nested = result.get("nested");
        assertTrue(nested instanceof Map, "'nested' должен быть словарём");
        Map<?, ?> nestedMap = (Map<?, ?>) nested;
        assertTrue(nestedMap.containsKey("innerConst"), "Вложенный словарь должен содержать ключ 'innerConst'");

        Object innerConst = nestedMap.get("innerConst");

        assertTrue(innerConst instanceof Object, "'innerConst' должен быть типа Long");
        assertEquals(new BigDecimal("15"), innerConst, "Значение 'innerConst' неверно");
    }

    @Test
    @DisplayName("Тестирование парсинга конфигурационного файла с неверным объявлением константы")
    void testParseInvalidConst() {
        String config = """
                const 123invalid = 10
                ([
                    key : 5
                ])
                """;

        Parser parser = new Parser(config);
        ParseException exception = assertThrows(
                ParseException.class,
                () -> parser.parse(),
                "Ожидалось выбросить ParseException при неверном объявлении константы"
        );

        assertTrue(exception.getMessage().contains("Неверный синтаксис объявления константы"),
                "Сообщение об ошибке должно содержать информацию о неверном синтаксисе константы");
    }

    @Test
    @DisplayName("Тестирование парсинга конфигурационного файла с отсутствующим закрывающим символом для словаря")
    void testParseMissingClosingBracket() {
        String config = """
                ([
                    key1 : 10,
                    key2 : 20
                """;

        Parser parser = new Parser(config);
        ParseException exception = assertThrows(
                ParseException.class,
                () -> parser.parse(),
                "Ожидалось выбросить ParseException при отсутствии закрывающего символа для словаря"
        );

        assertTrue(exception.getMessage().contains("Не найдено закрывающих символов ]) для словаря"),
                "Сообщение об ошибке должно содержать информацию о отсутствии закрывающих символов");
    }


    @Test
    @DisplayName("Тестирование выражения с делением на ноль")
    void testEvaluateExpressionDivisionByZero() {
        String config = """
                const a = 10
                const b = 0
                ([
                    result : @{a b /}
                ])
                """;

        Parser parser = new Parser(config);
        ParseException exception = assertThrows(
                ParseException.class,
                () -> parser.parse(),
                "Ожидалось выбросить ParseException при делении на ноль"
        );

        assertTrue(exception.getMessage().contains("Деление на ноль"),
                "Сообщение об ошибке должно содержать информацию о делении на ноль");
    }

    @Test
    @DisplayName("Тестирование выражения с неизвестным оператором")
    void testEvaluateExpressionUnknownOperator() {
        String config = """
                ([
                    key : @{10 5 ^}
                ])
                """;

        Parser parser = new Parser(config);
        ParseException exception = assertThrows(
                ParseException.class,
                () -> parser.parse(),
                "Ожидалось выбросить ParseException при использовании неизвестного оператора"
        );

        assertTrue(exception.getMessage().contains("Неизвестный оператор или функция"),
                "Сообщение об ошибке должно содержать информацию о неизвестном операторе");
    }

    @Test
    @DisplayName("Тестирование метода isNumeric с числовыми и нечисловыми строками")
    void testIsNumeric() {
        String config = """
                const validInt = 100
                const validDouble = 3.14
                const invalid = abc
                """;

        Parser parser = new Parser(config);
        ParseException exception = assertThrows(
                ParseException.class,
                () -> parser.parse(),
                "Ожидалось выбросить ParseException при использовании нечисловой константы"
        );

        assertTrue(exception.getMessage().contains("Неизвестное значение или константа"),
                "Сообщение об ошибке должно содержать информацию о неизвестной константе");
    }

    @Test
    @DisplayName("Тестирование метода parseNumber с целыми и дробными числами")
    void testParseNumber() throws ParseException {
        String config = """
                const wholeNumber = 42
                const decimalNumber = 3.1415
                const scaledNumber = 2.71828
                ([
                    result1 : @{wholeNumber 2 *},
                    result2 : @{decimalNumber 2 /},
                    result3 : @{scaledNumber 3 *}
                ])
                """;

        Parser parser = new Parser(config);
        Map<String, Object> resultMap = parser.parse();

        assertNotNull(resultMap, "Результат парсинга не должен быть null");
        assertEquals(new BigDecimal("84"), resultMap.get("result1"), "Значение 'result1' должно быть 84");
        assertEquals(new BigDecimal("1.571"), resultMap.get("result2"), "Значение 'result2' должно быть 1.571");
        assertEquals(new BigDecimal("8.155"), resultMap.get("result3"), "Значение 'result3' должно быть 8.154");
    }
}
