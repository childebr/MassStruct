/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package masstruct;

import de.ipbhalle.metfrag.spectrum.WrapperSpectrum;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import misc.Configuration;

/**
 *
 * @author childebr
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
    static int RANKCOUNTER = 0, BC = 0, WC = 0, TC = 0;
    static double SCORE = 0, CORRECTSCORE = 0, PPM = 0.0;
    static boolean CORRECTFOUND = false;
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
                    Main.CONFIG =new Configuration(CONFIGFILE);

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
        UNKNOWN = new UnknownCompound(SPECTRUM, PPM, Main.CONFIG);

        ArrayList<String> SortedCandidates = UNKNOWN.getSortedCandidateList();
        ArrayList<Double> SortedCandidatesScores = UNKNOWN.getSortedCandidateScores();
        TC = UNKNOWN.getAmountOfCandidates();

        for (int i = 0; i < SortedCandidates.size(); i++) {
            if (i == 0) {
                SCORE = SortedCandidatesScores.get(i);
            } else {
                if (SCORE != SortedCandidatesScores.get(i) && !CORRECTFOUND) {
                    BC = RANKCOUNTER;
                    SCORE = SortedCandidatesScores.get(i);
                } else if (SCORE != SortedCandidatesScores.get(i) && CORRECTFOUND) {
                    WC = TC - i;
                    break;
                }
            }
            RANKCOUNTER++;
            if (SortedCandidates.get(i).equals(UNKNOWN.getCorrectCandidateKEGGID()) || SortedCandidates.get(i).equals(UNKNOWN.getCorrectCandidatePUBCHEMID())) {
                CORRECTFOUND = true;
            }
        }
        CORRECTSCORE = 1 - (0.5 * (1.0 + ((double) BC - (double) WC) / ((double) TC - 1)));

        if (CORRECTFOUND) {
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(RESULTFILE, true));
                bw.write(UNKNOWN.getCorrectCandidateKEGGID() + "\t" + UNKNOWN.getCorrectCandidatePUBCHEMID() + "\t" + CORRECTSCORE + "\t" + BC + "\t" + WC + "\t" + TC + "\t" + UNKNOWN.getQueryRuntimeMS() + " ms\t" + "\n");
                bw.close();
            } catch (Exception e) {
                System.err.println("Couldn't write to file " + RESULTFILE.getName());
            }
        } else {
            CORRECTSCORE = 0;
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(RESULTFILE, true));
                bw.write(UNKNOWN.getCorrectCandidateKEGGID() + "\t" + UNKNOWN.getCorrectCandidatePUBCHEMID() + "\t" + CORRECTSCORE + "\t" + BC + "\t" + WC + "\t" + TC + "\t" + UNKNOWN.getQueryRuntimeMS() + " ms\t" + "CNF\n");
                bw.close();
            } catch (Exception e) {
                System.err.println("Couldn't write to file " + RESULTFILE.getName());
            }
        }
    }

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
                bw.write("KEGGID" + "\t" + "PCID" + "\t" + "Score-Correct" + "\t\t" + "BC" + "\t" + "WC" + "\t" + "TC" + "\t" + "Runtime" + "\n");
                bw.close();
            } catch (IOException ioe) {
                System.err.println("Couldn't write to file " + RESULTFILE.getName());
                System.err.println("ERROR: " + ioe.getLocalizedMessage());
                System.out.println("Exiting...");
                System.exit(1);
            }
        }
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

    private static void usage() {
        System.out.println("Usage: java -jar ....jar [Options]" + LINESEP + LINESEP);
        System.out.println("[Options]" + LINESEP);
        System.out.println("-c --config\tconfigfile");
        System.out.println("-s --spectrum\tspectrumfile");
        System.out.println("-r --resultfile\tWhere the result has to be written to.");
        System.out.println("-q --sqlfile\tWhere the query has to be written to.");
        System.out.println("-p --ppm\tThe ppm value which should be used for the spectrum.");
    }

    public static File getConfig(){
        return CONFIGFILE;
    }
}
