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

import de.ipbhalle.metfrag.massbankParser.Peak;
import de.ipbhalle.metfrag.spectrum.WrapperSpectrum;
import de.ipbhalle.metfrag.tools.PPMTool;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;
import misc.Configuration;

/**
 *
 * @author Christian Hildebrandt - Leibniz Institut Halle - Tel.: +49 (0) 345 5582 1473
 */
public class UnknownCompound {

    private WrapperSpectrum UNKNOWN;
    private Vector<String> databases = new Vector<String>();
    private HashMap<String, Double> SortedCandidates = new HashMap<String, Double>();
    private Configuration config = null;
    private Connection CONN;
    private Double PPM = 10.0;
    private Double MZ_ABS = 0.1;
    private String SORTED_QUERY;
    /**
     * Database related values
     */
    private static String HOST;
    private static String DATABASE;
    private static String DATABASE_DRIVER_JAVA;
    private static String DB_USER;
    private static String DB_PASSWORD;
    /**
     * Table MZ_CLUSTER related values
     */
    private static String TAB_MZ_CLUSTER;
    private static String COL_MZCL_ID;
    private static String COL_MIN_MZ;
    private static String COL_MAX_MZ;
    /**
     * Table COMPOUND related values
     */
    private static String TAB_COMPOUND;
    private static String COL_COMP_ID;
    private static String COL_MOL_STRUCTURE;
    private static String COL_EXACT_MASS;
    private static String COL_INCHI_KEY_1;
    /**
     * Table SUBSTANCE related values
     */
    private static String TAB_SUBSTANCE;
    private static String COL_COMPOUND_ID;
    private static String COL_LIBRARY_ID;
    private static String COL_ACCESSION;
    /**
     * Table LIBRARY related values
     */
    private static String TAB_LIBRARY;
    private static String COL_LIBR_ID;
    private static String COL_LIBRARY_NAME;
    /**
     * Table MCS related values
     */
    private static String TAB_MCS;
    private static String COL_MCS_STRUCTURE;
    private static String COL_MZ_CLUSTER_ID;
    /**
     * Database entries searched from
     */
    private static String FAVORITE_CHEM_DB;

    /**
     * Constructor for an unknown compound spectrum
     *
     * @param ws                    a WrapperSpectrum read from a spectrum file
     * @param config                specified configuration file
     */
    public UnknownCompound(WrapperSpectrum ws, Configuration config) {
        this.UNKNOWN = ws;
        this.config = config;
        setVariables();
        this.CONN = connectToDatabase();
    }

    /**
     * Constructor for an unknown compound spectrum
     *
     * @param ws                    a WrapperSpectrum read from a spectrum file
     * @param ppm                   the ppm value specified by the measure machine
     * @param config                specified configuration file
     */
    public UnknownCompound(WrapperSpectrum ws, Double ppm, Configuration config) {
        this.UNKNOWN = ws;
        this.config = config;
        setVariables();
        this.CONN = connectToDatabase();
        this.PPM = ppm;
    }

    /**
     * Constructor for an unknown compound spectrum
     *
     * @param ws                    a WrapperSpectrum read from a spectrum file
     * @param ppm                   the ppm value specified by the measure machine
     * @param databasesToSearchIn   select here databases to search in
     * @param config                specified configuration file
     */
    public UnknownCompound(WrapperSpectrum ws, Double ppm, Vector<String> databasesToSearchIn, Configuration config) {
        this.UNKNOWN = ws;
        this.config = config;
        setVariables();
        this.CONN = connectToDatabase();
        this.PPM = ppm;
        this.databases = databasesToSearchIn;
    }

    /**
     * Method to execute the dynamically query
     *
     * @return the sorted candidate list
     */
    public HashMap<String, Double> getSortedCandidateList() {

        ResultSet rs = null;
        try {
            Statement st = null;
            st = CONN.createStatement();
            rs = st.executeQuery(SortedQuery());
            while (rs.next()) {
                SortedCandidates.put(rs.getString(1), Double.parseDouble(rs.getString(2)));
            }
            rs.close();
            if (rs.isClosed()) {
                st.close();
            } else {
                rs.close();
                st.close();
            }
        } catch (Exception e) {
            System.err.println("Could not close statement. Leave unclosed.");
            System.err.println(e.getLocalizedMessage());
        }
        return SortedCandidates;
    }

    /**
     * Method to connect to a database specified by parameter in configuration
     *
     * @return a connection object
     */
    private Connection connectToDatabase() {

        Connection cn = null;

        if (null != DATABASE_DRIVER_JAVA
                && 0 < DATABASE_DRIVER_JAVA.length()
                && null != HOST
                && 0 < HOST.length()) {


            try {
                // Select fitting database driver and connect:
                try {
                    Class.forName(DATABASE_DRIVER_JAVA);
                } catch (ClassNotFoundException cnfe) {
                    System.err.println("Class not Found " + cnfe.getLocalizedMessage());
                }
                try {
                    cn = DriverManager.getConnection("jdbc:postgresql://" + HOST + "/" + DATABASE + "/test?user=" + DB_USER + "&password=" + DB_PASSWORD);
                } catch (SQLException sqle) {
                    System.out.println("Couldn't connect: print out a stack trace and exit.");
                    sqle.printStackTrace();
                    System.exit(1);

                } catch (Exception e) {
                    System.err.println(e.getLocalizedMessage());
                }

            } catch (Exception ex) {
                System.err.println(ex.getLocalizedMessage());
            }
        }
        return cn;
    }

    /**
     * Method to finally close the established connection to the database
     *
     */
    public void closeConnection() {
        try {
            if (null != this.CONN) {
                this.CONN.close();
            }
        } catch (Exception e) {
            System.err.println("Couldn't close connection. Leave unclosed.");
            System.err.println("Be aware to close connection manually.");
            System.err.println(e.getLocalizedMessage());
        }
    }

    /**
     * Method to build the dynamically query out of the spectrums properties
     *
     * @return the query as string
     */
    private String SortedQuery() {

        this.SORTED_QUERY = "SELECT "
                + TAB_SUBSTANCE + "." + COL_ACCESSION
                + ", Score"
                + " FROM "
                + TAB_SUBSTANCE + ", "
                + TAB_COMPOUND + ", "
                + TAB_LIBRARY + ", "
                +       "(SELECT "
                +       COL_INCHI_KEY_1 + ","
                +       " COUNT(DISTINCT MCS." + COL_MZCL_ID + ") AS Score"
                +       " FROM "
                +       TAB_SUBSTANCE + ", "
                +       TAB_LIBRARY + ", "
                +           "(SELECT MIN(" + COL_COMP_ID + ") AS firstcompound_id"
                +          " FROM " + TAB_COMPOUND
                +           " WHERE " + COL_EXACT_MASS + " BETWEEN " + calcMinValue(this.UNKNOWN.getExactMass())
                +           " AND " + calcMaxValue(this.UNKNOWN.getExactMass())
                +          " GROUP BY " + COL_INCHI_KEY_1
                +           ") AS firstcandidate, "
                +       TAB_COMPOUND + " AS Candidates"
                +   " LEFT OUTER JOIN "
                +       "(SELECT "
                +       TAB_MCS + "." + COL_MCS_STRUCTURE + ", "
                +       TAB_MZ_CLUSTER + "." + COL_MZCL_ID
                +       " FROM " + TAB_MCS + ", "
                +       TAB_MZ_CLUSTER
                +       " WHERE " + TAB_MCS + "." + COL_MZ_CLUSTER_ID + " = " + TAB_MZ_CLUSTER + "." + COL_MZCL_ID
                +       " AND (";
        for (Peak peak : this.UNKNOWN.getPeakList()) {

            if (peak.equals(this.UNKNOWN.getPeakList().lastElement())) {
                this.SORTED_QUERY   += "(" + COL_MIN_MZ + " >= "
                                    + calcMinValue(peak.getMass())
                                    + " AND " + COL_MAX_MZ + " <= "
                                    + calcMaxValue(peak.getMass())

                        +           "))) AS MCS "
                        +   "ON (MCS." + COL_MCS_STRUCTURE + " <= Candidates." + COL_MOL_STRUCTURE + ")"
                        +       " WHERE " + TAB_SUBSTANCE + "." + COL_COMPOUND_ID + " = firstcompound_id"
                        +       " AND   " + TAB_SUBSTANCE + "." + COL_LIBRARY_ID + " = " + TAB_LIBRARY + "." + COL_LIBR_ID
                        +       " AND   " + COL_LIBRARY_NAME + " = '" + FAVORITE_CHEM_DB + "'"
                        +       " AND   Candidates." + COL_COMP_ID + " = firstcompound_id"
                        +       " GROUP BY " + COL_ACCESSION + ", " + COL_INCHI_KEY_1
                        +       " ORDER BY Score DESC) AS results"
                        + " WHERE " + COL_EXACT_MASS + " BETWEEN " + calcMinValue(this.UNKNOWN.getExactMass())
                        + " AND " + calcMaxValue(this.UNKNOWN.getExactMass())
                        + " AND " + TAB_SUBSTANCE + "." + COL_COMPOUND_ID + " = " + TAB_COMPOUND + "." + COL_COMP_ID
                        + " AND " + TAB_SUBSTANCE + "." + COL_LIBRARY_ID + " = " + TAB_LIBRARY + "." + COL_LIBR_ID
                        + " AND " + COL_LIBRARY_NAME + " = '" + FAVORITE_CHEM_DB + "'"
                        + " AND " + TAB_COMPOUND + "." + COL_INCHI_KEY_1 + " = results." + COL_INCHI_KEY_1 + ";";
            } else {
                this.SORTED_QUERY   += "(" + COL_MIN_MZ + " >= "
                                    + calcMinValue(peak.getMass())
                                    + " AND " + COL_MAX_MZ + " <= "
                                    + calcMaxValue(peak.getMass())
                                    + ") OR ";
            }
        }
        return this.SORTED_QUERY;
    }

    private void setVariables() {
        try {

            HOST                    = this.config.getStringValue("HOST");
            DATABASE                = this.config.getStringValue("DATABASE");
            DATABASE_DRIVER_JAVA    = this.config.getStringValue("DATABASE_DRIVER_JAVA");
            DB_USER                 = this.config.getStringValue("DB_USER");
            DB_PASSWORD             = this.config.getStringValue("DB_PASSWORD");

            TAB_MZ_CLUSTER          = this.config.getStringValue("TAB_MZ_CLUSTER");
            COL_MZCL_ID             = this.config.getStringValue("COL_MZCL_ID");
            COL_MIN_MZ              = this.config.getStringValue("COL_MIN_MZ");
            COL_MAX_MZ              = this.config.getStringValue("COL_MAX_MZ");

            TAB_COMPOUND            = this.config.getStringValue("TAB_COMPOUND");
            COL_COMP_ID             = this.config.getStringValue("COL_COMP_ID");
            COL_MOL_STRUCTURE       = this.config.getStringValue("COL_MOL_STRUCTURE");
            COL_EXACT_MASS          = this.config.getStringValue("COL_EXACT_MASS");
            COL_INCHI_KEY_1         = this.config.getStringValue("COL_INCHI_KEY_1");

            TAB_SUBSTANCE           = this.config.getStringValue("TAB_SUBSTANCE");
            COL_COMPOUND_ID         = this.config.getStringValue("COL_COMPOUND_ID");
            COL_LIBRARY_ID          = this.config.getStringValue("COL_LIBRARY_ID");
            COL_ACCESSION           = this.config.getStringValue("COL_ACCESSION");

            TAB_LIBRARY             = this.config.getStringValue("TAB_LIBRARY");
            COL_LIBR_ID             = this.config.getStringValue("COL_LIBR_ID");
            COL_LIBRARY_NAME        = this.config.getStringValue("COL_LIBRARY_NAME");

            TAB_MCS                 = this.config.getStringValue("TAB_MCS");
            COL_MCS_STRUCTURE       = this.config.getStringValue("COL_MCS_STRUCTURE");
            COL_MZ_CLUSTER_ID       = this.config.getStringValue("COL_MZ_CLUSTER_ID");

            FAVORITE_CHEM_DB        = this.config.getStringValue("FAVORITE_CHEM_DB");

        } catch (Exception e) {
            System.err.println("Couldn't read configuration file.");
            System.err.println(e.getLocalizedMessage());
        }
    }

    public String getSortedQuery() {
        return this.SORTED_QUERY;
    }

    public String getKEGGID() {
        return this.UNKNOWN.getKEGG();
    }

    public String getPUBCHEMID() {
        return this.UNKNOWN.getCID() + "";
    }

    public WrapperSpectrum getSpectrum() {
        return this.UNKNOWN;
    }

    public String getCompoundName() {
        return this.UNKNOWN.getTrivialName();
    }

    public boolean setDatabases(String db) {
        this.databases.add(db);
        if (this.databases.contains(db)) {
            return true;
        } else {
            return false;
        }
    }

    private Double calcMinValue(Double d) {
        return d - PPMTool.getPPMDeviation(d, this.PPM) - this.MZ_ABS;
    }

    private Double calcMaxValue(Double d) {
        return d + PPMTool.getPPMDeviation(d, this.PPM) + this.MZ_ABS;
    }
}
