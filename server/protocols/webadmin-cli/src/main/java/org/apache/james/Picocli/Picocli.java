package org.apache.james.Picocli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "james-cli",
        description = "James Webadmin CLI"
)
public class Picocli implements Runnable{

    @CommandLine.Option(
            names = "--help",
            description = "Display help for commands"
    )
    public boolean isNeedHelp;

    @Override
    public void run() {
        final String helpMessage = "There is no help for any commands";
        if (isNeedHelp) System.out.println(helpMessage);
    }

}
