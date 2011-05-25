/*
 *
 * Copyright (C) 2010-2011 IPB Halle, Christian Hildebrandt
 *
 * Contact: Christian.Hildebrandt@ipb-halle.de
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package masstruct;

import de.ipbhalle.metfrag.spectrum.WrapperSpectrum;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Scanner;
import misc.Configuration;

/**
 *
 * @author Christian Hildebrandt - Leibniz Institut Halle - Tel.: +49 (0) 345 5582 1473
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    /**
     * Setting default values
     */
    static File CONFIGFILE = new File("./config");
    static File SPECTRUMFILE = new File("./test_spectrum.txt");
    static File RESULTFILE = new File("./Result.txt");
    static File SQLFILE = new File("./Query.sql");
    static Configuration CONFIG;
    /**
     * Calc. Variables
     */
    static WrapperSpectrum SPECTRUM;
    static UnknownCompound UNKNOWN;
    static double PPM = 0.0;
    static HashMap RESULTS;
    /**
     * Misc. Variables
     */
    static String LINESEP = System.getProperty("line.separator");

    public static void main(String[] args) {
        /**
         * Reading arguments
         */
        for (int i = 0; i < args.length; i++) {
            if ((args.length % 2) != 0) {

                System.err.println("Too few arguments.");
                Main.usage();
                System.exit(1);

            } else if (args.length > 1) {

                if (args[i].equalsIgnoreCase("-c") || args[i].equalsIgnoreCase("--config")) {
                    i++;
                    CONFIGFILE = new File(args[i]);
                    Main.CONFIG = new Configuration(CONFIGFILE);

                } else if (args[i].equalsIgnoreCase("-s") || args[i].equalsIgnoreCase("--spectrum")) {
                    i++;
                    SPECTRUMFILE = new File(args[i]);

                } else if (args[i].equalsIgnoreCase("-r") || args[i].equalsIgnoreCase("--resultfile")) {
                    i++;
                    RESULTFILE = new File(args[i]);

                } else if (args[i].equalsIgnoreCase("-q") || args[i].equalsIgnoreCase("--sqlfile")) {
                    i++;
                    SQLFILE = new File(args[i]);

                } else if (args[i].equalsIgnoreCase("-p") || args[i].equalsIgnoreCase("--ppm")) {
                    i++;
                    try {
                        PPM = Double.parseDouble(args[i]);
                    } catch (Exception e) {
                        System.err.println("Couldn't read ppm value. Make sure this is a number like this: 123.45");
                        System.exit(1);
                    }
                }
            }
        }

        /**
         * Print settings and ask if this is correct
         */
        Scanner in = new Scanner(System.in);
        System.out.println("Settings:");
        System.out.println("=========" + LINESEP);
        System.out.println("Configuration file:  " + CONFIGFILE.getPath());
        System.out.println("Spectrum file:       " + SPECTRUMFILE.getPath());
        System.out.println("Result file:         " + RESULTFILE.getPath());
        System.out.println("SQL file:            " + SQLFILE.getPath() + LINESEP);
        System.out.println("ppm value:           " + PPM + LINESEP);
        System.out.println("Do you agree with these settings? (y/n)");
        String decision = in.nextLine();
        if (decision.equalsIgnoreCase("n")) {
            System.out.println(LINESEP + "Try to define your settings as follows!" + LINESEP);
            Main.usage();
            System.exit(1);
        } else if (decision.equalsIgnoreCase("y")) {
            Main.start();
        } else {
            System.err.println("Unknown input." + LINESEP);
            System.out.println("Exiting...");
            System.exit(1);
        }
    }

    private static void start() {
        SPECTRUM = new WrapperSpectrum(SPECTRUMFILE.getPath());
        UNKNOWN = new UnknownCompound(SPECTRUM, PPM, CONFIG);
        RESULTS = UNKNOWN.getSortedCandidateList();
        LOG();
    }

    /**
     * logging the reached results to the specified outputfile
     *
     */
    private static void LOG() {
        if (!RESULTFILE.exists()) {

            if (!RESULTFILE.getParentFile().exists()) {
                RESULTFILE.getParentFile().mkdirs();
            }

            try {
                RESULTFILE.createNewFile();
            } catch (IOException ioe) {
                System.err.println("Couldn't create file " + RESULTFILE.getName());
                System.err.println("ERROR" + ioe.getLocalizedMessage());
                System.out.println("Exiting...");
                System.exit(1);
            }
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(RESULTFILE, true));
                bw.write("rank" + "\t" + "DBID" + "\n");
                bw.close();
            } catch (IOException ioe) {
                System.err.println("Couldn't write to file " + RESULTFILE.getName());
                System.err.println("ERROR: " + ioe.getLocalizedMessage());
                System.out.println("Exiting...");
                System.exit(1);
            }
            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(RESULTFILE));
                out.writeObject(RESULTS);
                out.close();

            } catch (IOException ioe) {
                System.err.println("Couldn't write to file " + RESULTFILE.getName());
                System.err.println("ERROR: " + ioe.getLocalizedMessage());
                System.out.println("Exiting...");
                System.exit(1);
            }
        }

        /**
         * print also SQL query into a sql file
         *
         */
        if (!SQLFILE.exists()) {
            if (!SQLFILE.getParentFile().exists()) {
                SQLFILE.getParentFile().mkdirs();
            }
            try {
                SQLFILE.createNewFile();
            } catch (IOException ioe) {
                System.err.println("Couldn't create file " + SQLFILE.getName());
                System.err.println("ERROR" + ioe.getLocalizedMessage());
                System.out.println("Exiting...");
                System.exit(1);
            }
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(SQLFILE, true));
                bw.write(UNKNOWN.getSortedQuery() + "\n");
                bw.close();
            } catch (Exception e) {
                System.err.println("Konnte nicht in Datei " + SQLFILE + " schreiben!");
            }
        }
    }

    /**
     * print out usage of this program
     *
     */
    private static void usage() {
        System.out.println("Usage: java -jar ....jar [Options]" + LINESEP + LINESEP);
        System.out.println("[Options]" + LINESEP);
        System.out.println("-c --config\tconfigfile");
        System.out.println("-s --spectrum\tspectrumfile");
        System.out.println("-r --resultfile\tWhere the result has to be written to.");
        System.out.println("-q --sqlfile\tWhere the query has to be written to.");
        System.out.println("-p --ppm\tThe ppm value which should be used for the spectrum.");
    }

    public static File getConfig() {
        return CONFIGFILE;
    }

    public static void setConfig(File config) {
        CONFIGFILE = config;
    }
}
