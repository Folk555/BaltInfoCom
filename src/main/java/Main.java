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

    private static String regex = "(^([\"]{1})([0-9]+)([\"]{1})$)|(^([\"]{1})([\"]{1}))";
    private static String fileWithStrings = "M:\\educationJava\\interview\\БалтИнфоКом\\lng.txt";
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
     * 1. Составляем Set в Map2 в Map1.
     * Set - множество строчек единой группы
     * Map1 - слово(физически: его хеш) указывающий на группы, в которых это слово есть
     * Map2 - позиция слова указывающая на те группы, в которых это слово в соответствующей позиции
     * 2. Ищем хеши/позиции, слов в строке кандидате, в Map1/Map2.
     * Если есть хоть одно.
     * 2.1 Формируем временную новую группу Set, куда сбрасываем строку-кандидат + все элементы групп соответствующих найденным в Map хешам/позициям слов.
     * 2.2 Так как Map содержит хеш и !указатель! на Set, достаточно обновить группу по любому из найденных хеш слов + позиции.
     * Остальные указывают на тот же сет(согласно реализации алгоритма).
     * 2.3 Добавляем слова(хеши) строки кандидата которых нет в Map + указатель на существующую группу, которой они соответствуют.
     * Если ни одного нет.
     * 2.1 Формируем временную новую группу Set, куда сбрасываем строку-кандидат.
     * 2.2 Добавляем слова(хеши) строки кандидата + !указатель! на временную новую группу.
     * P.S.    Все слова в строке-кандитат будут указывать на одну и туже группу Set.
     * Временная группа станет постоянной в конце текущего шага. Для каждой строки-кандидата, создается новая временная группа.
     */
    private static class GroupLinesCollector implements Collector<String[], Map<String, Map<Integer, Set<String[]>>>, List<Set<String>>> {

        @Override
        public Supplier<Map<String, Map<Integer, Set<String[]>>>> supplier() {
            return () -> new HashMap<>();
        }

        @Override
        public BiConsumer<Map<String, Map<Integer, Set<String[]>>>, String[]> accumulator() {
            return (Map<String, Map<Integer, Set<String[]>>> acc, String[] candidate) -> {

                var tempGroup = new HashSet<String[]>();
                tempGroup.add(candidate);

                for (int i = 0; i < candidate.length; ++i) {
                    if (("".equals(candidate[i])) || ("\"\"".equals(candidate[i])))
                        continue;
                    if (acc.containsKey(candidate[i])) {
                        if (acc.get(candidate[i]).containsKey(i)) {
                            tempGroup.addAll(acc.get(candidate[i]).get(i));
                        }
                    }
                }

                if (tempGroup.size() > 1) {
                    //Ищем "существующую группу". Этот указатель понадобится для добавления несуществующих раннее слов.
                    Set<String[]> pointerToExistingGroup = new HashSet<>();
                    for (int indexWord = 0; indexWord < candidate.length; ++indexWord) {
                        if (acc.containsKey(candidate[indexWord])) {
                            Set<Integer> setOfPositions = acc.get(candidate[indexWord]).keySet();
                            for (int indexWordPosition : setOfPositions) {
                                pointerToExistingGroup = acc.get(candidate[indexWord]).get(indexWordPosition);
                                break;
                            }
                        }
                    }

                    for (int i = 0; i < candidate.length; ++i) {
                        if (("".equals(candidate[i])) || ("\"\"".equals(candidate[i])))
                            continue;
                        if (acc.containsKey(candidate[i])) {
                            if (acc.get(candidate[i]).containsKey(i))               //Можно упростить, после 1 обновления Set
                                acc.get(candidate[i]).get(i).addAll(tempGroup);
                            else {
                                acc.get(candidate[i]).put(i, pointerToExistingGroup);
                            }
                        } else {
                            Map newMap = new HashMap<Integer, Set<String[]>>();
                            newMap.put(i, pointerToExistingGroup);
                            acc.put(candidate[i], newMap);
                        }
                    }
                    return;
                }

                for (int i = 0; i < candidate.length; ++i) {
                    if (("".equals(candidate[i])) || ("\"\"".equals(candidate[i])))
                        continue;
                    if (acc.containsKey(candidate[i])) {
                        acc.get(candidate[i]).put(i, tempGroup);
                    } else {
                        Map newMap = new HashMap<Integer, Set<String[]>>();
                        newMap.put(i, tempGroup);
                        acc.put(candidate[i], newMap);
                    }
                }

            };
        }

        @Override
        public BinaryOperator<Map<String, Map<Integer, Set<String[]>>>> combiner() {
//            return (map1, map2) -> {
//                for (Map.Entry<String, Set<String[]>> entry : map2.entrySet()) {
//                    var setFromRightArg = entry.getValue();
//                    if (map1.containsKey(entry.getKey())) {
//                        map1.get(entry.getKey()).addAll(setFromRightArg);
//                        /**
//                         * Нужно добавить хеш-слова из группы.
//                         */
//                    } else {
//                        map1.put(entry.getKey(), setFromRightArg);
//                    }
//                }
//                return map1;
            return (a1, a2) -> {
                throw new UnsupportedOperationException("Параллельность не реализована");
            };
        }


        @Override
        public Function<Map<String, Map<Integer, Set<String[]>>>, List<Set<String>>> finisher() {
            return (Map<String, Map<Integer, Set<String[]>>> inputMap) -> {

                Set processedGroups = new HashSet<Set<String[]>>();
                var result = new ArrayList<Set<String>>();

                for (String keyWord : inputMap.keySet()) {
                    for (int keyPosition : inputMap.get(keyWord).keySet()) {
                        if (processedGroups.contains(inputMap.get(keyWord).get(keyPosition)))
                            continue;
                        processedGroups.add(inputMap.get(keyWord).get(keyPosition));
                        var group = new HashSet<String>();
                        for (String[] arrayStrings : inputMap.get(keyWord).get(keyPosition)) {
                            String arrayStringsToSingle = arrayStrings[0];
                            for (int i = 1; i < arrayStrings.length; ++i) {
                                arrayStringsToSingle += ";";
                                arrayStringsToSingle += arrayStrings[i];
                            }
                            group.add(arrayStringsToSingle);
                        }
                        result.add(group);

                    }
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
