package UnitTest;

import org.apache.james.Picocli.Picocli;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class HelpCommandTest {

    private static final String successHelpMessage = "There is no help for any commands";

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setup(){
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown(){
        System.setOut(standardOut);
    }

    @Test
    void allHelpCommandShouldWork(){
        new CommandLine(new Picocli()).execute("--help");
        Assertions.assertEquals(successHelpMessage, outputStreamCaptor.toString().trim());
    }

}
