/*
HTTP stub server written in Java with embedded Jetty

Copyright (C) 2012 Alexander Zagniotov, Isa Goksu and Eric Mrak

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.azagniotov.stubby4j;

import io.github.azagniotov.stubby4j.cli.ANSITerminal;
import io.github.azagniotov.stubby4j.cli.CommandLineInterpreter;
import io.github.azagniotov.stubby4j.exception.Stubby4JException;
import io.github.azagniotov.stubby4j.server.StubbyManager;
import io.github.azagniotov.stubby4j.server.StubbyManagerFactory;
import io.github.azagniotov.stubby4j.utils.ConsoleUtils;
import io.github.azagniotov.stubby4j.yaml.YAMLParser;
import io.github.azagniotov.stubby4j.yaml.stubs.StubHttpLifecycle;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.github.azagniotov.stubby4j.utils.FileUtils.BR;

public final class Main {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(1);
    private static CommandLineInterpreter commandLineInterpreter;

    private Main() {

    }

    public static void main(String[] args) {
        commandLineInterpreter = new CommandLineInterpreter();

        //String[] args2 = new String[]{"--data", "/Users/alexanderzagniotov/yaml/stubs.yaml"};
        //parseCommandLineArgs(args2);
        parseCommandLineArgs(args);
        if (printHelpIfRequested() || printVersionIfRequested()) {
            return;
        }

        verifyYamlDataProvided();
        startStubby4jUsingCommandLineArgs();
    }

    private static void parseCommandLineArgs(final String[] args) {
        try {
            commandLineInterpreter.parseCommandLine(args);
        } catch (final ParseException ex) {
            final String msg =
                    String.format("Could not parse provided command line arguments, error: %s",
                            ex.toString());

            throw new Stubby4JException(msg);
        }
    }

    private static boolean printHelpIfRequested() {
        if (!commandLineInterpreter.isHelp()) {
            return false;
        }

        commandLineInterpreter.printHelp();

        return true;
    }

    private static boolean printVersionIfRequested() {
        if (!commandLineInterpreter.isVersion()) {
            return false;
        }

        commandLineInterpreter.printVersion();

        return true;
    }

    private static void verifyYamlDataProvided() {
        if (commandLineInterpreter.isYamlProvided()) {
            return;
        }
        final String msg =
                String.format("YAML data was not provided using command line option '--%s'. %s"
                                + "To see all command line options run again with option '--%s'",
                        CommandLineInterpreter.OPTION_CONFIG, BR, CommandLineInterpreter.OPTION_HELP);

        throw new Stubby4JException(msg);
    }

    private static void startStubby4jUsingCommandLineArgs() {
        try {

            final long initialStart = System.currentTimeMillis();
            final Map<String, String> commandLineArgs = commandLineInterpreter.getCommandlineParams();
            final String configFilename = commandLineArgs.get(CommandLineInterpreter.OPTION_CONFIG);

            ANSITerminal.muteConsole(commandLineInterpreter.isMute());
            ConsoleUtils.enableDebug(commandLineInterpreter.isDebug());

            final File configFile = new File(configFilename);
            final Future<List<StubHttpLifecycle>> stubLoadComputation =
                    EXECUTOR_SERVICE.submit(() -> new YAMLParser().parse(configFile.getParent(), configFile));

            final StubbyManager stubbyManager = new StubbyManagerFactory().construct(configFile, commandLineArgs, stubLoadComputation);
            stubbyManager.startJetty();
            final long totalEnd = System.currentTimeMillis();

            ANSITerminal.status(String.format(BR + "stubby4j successfully started after %s milliseconds", (totalEnd - initialStart)) + BR);

            stubbyManager.statuses().forEach(ANSITerminal::status);

            ANSITerminal.info(BR + "Quit: ctrl-c" + BR);

        } catch (final Exception ex) {
            final String msg =
                    String.format("Could not init stubby4j, error: %s", ex.toString());

            throw new Stubby4JException(msg, ex);
        }
    }
}