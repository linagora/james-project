package org.apache.james;

import org.apache.james.Picocli.Picocli;
import picocli.CommandLine;

public class WebAdminCli implements Runnable{

    @Override
    public void run() {

    }

    public static void main(String[] args) {
        new CommandLine(new Picocli()).execute(args);
    }
}
