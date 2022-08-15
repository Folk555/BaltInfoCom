

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @ParameterizedTest
    @CsvFileSource(resources = "exampleSource.txt", delimiter = '|')
    public void isStringValid_shouldReturnCorrectValues(String string, String ans) {
        String[] stringNums = string.split(";");
        Boolean expected = Boolean.parseBoolean(ans);
        assertEquals(expected, Main.isStringValid(stringNums));
    }
}