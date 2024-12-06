package qwerdsa53;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConfigToJsonConverter {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Использование: java ConfigToJsonConverter <input_file> <output_file>");
            System.exit(1);
        }

        String inputPath = args[0];
        String outputPath = args[1];

        StringBuilder inputBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                inputBuilder.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            System.err.println("Входной файл не найден: " + inputPath);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Ошибка при чтении входного файла: " + e.getMessage());
            System.exit(1);
        }
        String input = inputBuilder.toString();

        Parser parser = new Parser(input);
        try {
            Map<String, Object> result = parser.parse();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(result);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8))) {
                writer.write(jsonOutput);
            } catch (IOException e) {
                System.err.println("Ошибка при записи в выходной файл: " + e.getMessage());
                System.exit(1);
            }
        } catch (ParseException e) {
            System.err.println("Синтаксическая ошибка: " + e.getMessage());
            System.exit(1);
        }
    }

}
