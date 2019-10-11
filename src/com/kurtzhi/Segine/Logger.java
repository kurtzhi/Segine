package com.kurtzhi.Segine;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

class Logger {
    public static boolean enableConsoleOutput = true;
    public static boolean terminateProgramOnErr = true;
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    private static boolean enableDebug = true;
    private static String logFile;

    synchronized public static void setLogFile(String filename) {
        if (logFile != null) {
            return;
        }
        logFile = filename;
    }

    synchronized public static void log(TraceLevel level, String mod, String msg) {
        if (level == TraceLevel.Debug && !enableDebug) {
            return;
        }

        String record = "[" + level.toString() + "] ";
        record += dateFormatter.format(new Date()) + " ";
        record += "(" + Thread.currentThread().getId() + ") ";
        record += "(" + mod + ") ";
        record += msg;

        if (level == TraceLevel.Debug) {
            System.out.println(record);
        } else {
            System.err.println(record);
        }

        if (logFile != null) {

            FileWriter fw = null;
            try {
                fw = new FileWriter(logFile, true);
                fw.append(record);
            } catch (IOException e) {
                System.err.print("Log file " + logFile + " cannot be created.");
            } finally {
                if (fw != null) {
                    try {
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (terminateProgramOnErr && level == TraceLevel.Error) {
            System.exit(-1);
        }
    }

    public enum TraceLevel {
        Error, Debug
    }
}
