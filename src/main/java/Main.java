import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Collector.Characteristics.UNORDERED;

public class Main {

    private static String regex = "(^([\"]{1})([0-9]+)([\"]{1})$)|()";
    private static String fileWithStrings = "M:\\educationJava\\interview\\BaltInfoCom\\lng.csv";
    private static String directoryToSaveResults = "M:\\educationJava\\interview\\BaltInfoCom\\src\\main\\resources";

    public static void main(String[] args) throws IOException {
        Stream<String> streamFromFile = Files.lines(Paths.get(fileWithStrings));
        List<Set<String>> groups = streamFromFile
                .distinct()
                .map(x -> x.split(";"))
                .filter(Main::isStringValid)
                .collect(new Main.GroupLinesCollector());
        groups.sort(Comparator.comparing(Set::size, (size1, size2) -> size2.compareTo(size1)));
        saveGroupsIntoFile(groups);
    }

    public static boolean isStringValid(String[] candidate) {
//        if (candidate.length != 3)
//            return false;
        for (String subString : candidate)
            if (!Pattern.matches(regex, subString))
                return false;
        return true;
    }

    public static void saveGroupsIntoFile(List<Set<String>> results) throws IOException {
        Path pathToResultFile = Paths.get(directoryToSaveResults + "\\Results.txt");
        if (Files.notExists(pathToResultFile))
            Files.createFile(pathToResultFile);

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(pathToResultFile.toString()))) {

            for (int i = 0; i < results.size(); ++i) {
                if ((results.get(i).size() <= 1) || (i == results.size() - 1)) {
                    bufferedWriter.write("Число групп с более чем одним элементом: " + (i));
                    bufferedWriter.write("\n");
                    break;
                }
            }
            for (int i = 0; i < results.size(); ++i) {
                Iterator<String> iterator = results.get(i).iterator();
                if (iterator.hasNext()) {
                    bufferedWriter.write("Группа: " + i);
                    bufferedWriter.write("\n");
                }
                while (iterator.hasNext()) {
                    bufferedWriter.write(iterator.next());
                    bufferedWriter.write("\n");
                }
            }

        }
    }

    /**
     * candidate - новая строка.
     * <p>
     * Общий алгоритм:
     * 1. Составляем Set в Map.
     * Set - множество строчек единой группы
     * Map - слов(физически: его хеш) указывающий на группу, в которой это слово есть
     * 2. Ищем хеши, слов в строке кандидате, в Map.
     * Если есть хоть одно.
     * 2.1 Формируем временную новую группу Set, куда сбрасываем строку-кандидат + все элементы групп соответствующих найденным в Map хешам слов.
     * 2.2 Так как Map содержит хеш и !указатель! на Set, достаточно обновить группу по любому из найденных хеш слов.
     * Остальные указывают на тот же сет(согласно реализации алгоритма).
     * 2.3 Добавляем слова(хеши) строки кандидата которых нет в Map + указатель на существующую группу, которой они соответствуют.
     * Если ни одного нет.
     * 2.1 Формируем временную новую группу Set, куда сбрасываем строку-кандидат.
     * 2.2 Добавляем слова(хеши) строки кандидата + !указатель! на временную новую группу.
     * P.S.    Все слова в строке-кандитат будут указывать на одну и туже группу Set.
     * Временная группа станет постоянной в конце текущего шага. Для каждой строки-кандидата, создается новая временная группа.
     */
    private static class GroupLinesCollector implements Collector<String[], Map<String, Set<String[]>>, List<Set<String>>> {

        @Override
        public Supplier<Map<String, Set<String[]>>> supplier() {
            return () -> new HashMap<>();
        }

        @Override
        public BiConsumer<Map<String, Set<String[]>>, String[]> accumulator() {
            return (Map<String, Set<String[]>> acc, String[] candidate) -> {

                var tempGroup = new HashSet<String[]>();    //Временная новая группа. Для обновления существующих групп
                tempGroup.add(candidate);                   //или создания новой.
                for (int i = 0; i < candidate.length; ++i) { //Добавляем во временную группу существующие, если они могут быть группой
                    if ("".equals(candidate[i]))
                        continue;
                    if (acc.containsKey(candidate[i])) {
                        tempGroup.addAll(acc.get(candidate[i]));
                    }
                }
                if (tempGroup.size() > 1) {                                       //Если были найдены существующие группы
                    for (int i = 0; i < candidate.length; ++i) {
                        if ("".equals(candidate[i]))
                            continue;
                        if (acc.containsKey(candidate[i])) {                    //Если слово уже есть в Map.
                            acc.get(candidate[i]).addAll(tempGroup);            //Добавляем в существующую группу новые значения из временной группы
                        } else {                                                //Если слова нет в Map.
                            int existingGroupFromMap = 0;
                            while (!(acc.containsKey(candidate[existingGroupFromMap])))     //Ищем индекс (хеш) на указатель уже существующей в Map группы
                                ++existingGroupFromMap;
                            acc.put(candidate[i], acc.get(candidate[existingGroupFromMap]));//Добавляем слова(хеши) с указателем на уже существующую группу
                        }
                    }
                    return;
                }
                //Если существующих групп нет
                for (int i = 0; i < candidate.length; ++i) {
                    if ("".equals(candidate[i]))
                        continue;
                    acc.put(candidate[i], tempGroup);
                }
            };
        }

        @Override
        public BinaryOperator<Map<String, Set<String[]>>> combiner() {
            return (a1, a2) -> {
                throw new UnsupportedOperationException("Parallel still not implemented for GroupLinesCollector");
            };
        }

        @Override
        public Function<Map<String, Set<String[]>>, List<Set<String>>> finisher() {
            return (Map<String, Set<String[]>> inputMap) -> {

                var keySetForDuplicateGroups = new HashSet<String>(); //Набор ключей(хеш слов), группы которых уже добавлены в результирующий список
                var result = new ArrayList<Set<String>>();

                for (String key : inputMap.keySet()) {
                    if (keySetForDuplicateGroups.contains(key))
                        continue;
                    Set<String[]> setArrayStrings = inputMap.get(key);
                    var group = new HashSet<String>();
                    for (String[] arrayStrings : setArrayStrings) {
                        keySetForDuplicateGroups.addAll(Arrays.stream(arrayStrings).toList()); //Добавляем все слова из группы в keySetForDuplicateGroups, чтоб не обрабатывать их повторно
                        String arrayStringsToSingle = arrayStrings[0];
                        for (int i = 1; i < arrayStrings.length; ++i) {
                            arrayStringsToSingle += ";";
                            arrayStringsToSingle += arrayStrings[i];
                        }
                        group.add(arrayStringsToSingle);
                    }
                    result.add(group);
                }
                return result;
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.unmodifiableSet(EnumSet.of(UNORDERED));
        }
    }


}
