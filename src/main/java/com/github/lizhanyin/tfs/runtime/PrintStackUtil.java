package com.github.lizhanyin.tfs.runtime;


import java.io.PrintStream;
import java.io.PrintWriter;

public class PrintStackUtil {

    static public void printChildren(IStatus status, PrintStream output) {
        IStatus[] children = status.getChildren();
        if (children == null) {
            return;
        }
        for (IStatus child : children) {
            output.println("Contains: " + child.getMessage()); //$NON-NLS-1$
            Throwable exception = child.getException();
            if (exception != null) {
                exception.printStackTrace(output);
            }
            printChildren(child, output);
        }
    }

    static public void printChildren(IStatus status, PrintWriter output) {
        IStatus[] children = status.getChildren();
        if (children == null) {
            return;
        }
        for (IStatus child : children) {
            output.println("Contains: " + child.getMessage()); //$NON-NLS-1$
            output.flush(); // call to synchronize output
            Throwable exception = child.getException();
            if (exception != null) {
                exception.printStackTrace(output);
            }
            printChildren(child, output);
        }
    }

}
