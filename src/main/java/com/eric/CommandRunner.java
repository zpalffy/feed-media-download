package com.eric;

import java.io.PrintStream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Simple wrapper for a command line parser using JCommander. This simple class handles help
 * message, and sets up the easy things for working with that library. Subclasses should provide
 * their own main method that delegates to the run method on this class:
 *
 * <pre>
 * public static void main(String[] args) {
 *     new MyClass().run(&quot;my-app-name&quot;, args);
 * }
 * </pre>
 */
public abstract class CommandRunner implements Runnable {

    @Parameter(names = {"-h", "--help"}, help = true, description = "Displays this help message.")
    protected boolean help;

    @Parameter(names = {"--debug"}, hidden = true)
    protected boolean debug;

    protected PrintStream out = System.out;

    protected PrintStream err = System.err;

    protected void validate(boolean passes, String message) {
        if (!passes) {
            throw new IllegalStateException(message);
        }
    }

    public void command(String programName, String[] args) {
        JCommander cmd = null;

        try {
            cmd = new JCommander(this);
            cmd.setProgramName(programName);

            if (help) {
                cmd.usage();
            } else {
                cmd.parse(args);
                run();
            }
        } catch (Throwable t) {
            if (t.getMessage() != null) {
                err.println(t.getMessage());
                err.println();
            }

            if (cmd != null) {
                cmd.usage();
            }

            if (debug) {
                t.printStackTrace();
            }

            System.exit(1);
        }
    }
}
