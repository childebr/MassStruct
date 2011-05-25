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
    private Integer candidatesAmount = 0;
    private Double PPM = 10.0;
    private Double mz_abs = 0.1;
    private String sortedQuery, unsortedQuery;

    /**
     * Constructor for an unknown compound spectrum
     *
     * @param ws                    a WrapperSpectrum read from a spectrum file
     * @param config                specified configuration file
     */
    public UnknownCompound(WrapperSpectrum ws, Configuration config) {
        this.UNKNOWN = ws;
        this.config = config;
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

        String DbDrv = null, DbUrl = null, User = null, Passwd = null, DataBase = null;
        Connection cn = null;

        try {
            DbUrl = config.getStringValue("HOST");
            DbDrv = config.getStringValue("DATABASE_DRIVER_JAVA");
            User = config.getStringValue("DB_USER");
            Passwd = config.getStringValue("DB_PASSWORD");
            DataBase = config.getStringValue("DATABASE");
        } catch (Exception ex) {
            System.err.println("Could not read config file.");
            System.err.println(ex);
        }

        if (null != DbDrv
                && 0 < DbDrv.length()
                && null != DbUrl
                && 0 < DbUrl.length()) {


            try {
                // Select fitting database driver and connect:
                try {
                    Class.forName(DbDrv);
                } catch (ClassNotFoundException cnfe) {
                    System.err.println("Class not Found " + cnfe.getLocalizedMessage());
                }
                try {
                    cn = DriverManager.getConnection("jdbc:postgresql://" + DbUrl + "/" + DataBase + "/test?user=" + User + "&password=" + Passwd);
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

        String query = "SELECT"
                + " substance.accession, Score"
                + " FROM " + this.config.getStringValue("TAB_SUBSTANCE")
                + ", " + this.config.getStringValue("TAB_COMPOUND")
                + ", " + this.config.getStringValue("TAB_LIBRARY")
                + ", (SELECT "+this.config.getStringValue("COL_INCHI_KEY_1")+","
                + " COUNT(DISTINCT MCS.cluster_id) AS Score"
                + " FROM " 
                + this.config.getStringValue("TAB_SUBSTANCE")+", "
                + this.config.getStringValue("TAB_LIBRARY")+ ","
                + " (SELECT MIN(compound_id) AS firstcompound_id"
                + " FROM " + this.config.getStringValue("TAB_COMPOUND")
                + " WHERE exact_mass BETWEEN " + calcMinValue(this.UNKNOWN.getExactMass())
                + " AND " + calcMaxValue(this.UNKNOWN.getExactMass())
                + " GROUP BY "+this.config.getStringValue("COL_INCHI_KEY_1")+
                ") AS firstcandidate, "
                + this.config.getStringValue("compoundtable") + " AS Candidates"
                + " LEFT OUTER JOIN (SELECT mcs.mcs_structure,"
                + " mz_cluster.cluster_id"
                + " FROM mcs,"
                + " mz_cluster"
                + " WHERE mcs.mz_cluster_id = mz_cluster.cluster_id"
                + " AND (";
        for (Peak peak : this.UNKNOWN.getPeakList()) {

            if (peak.equals(this.UNKNOWN.getPeakList().lastElement())) {
                query += "(min >= "
                        + calcMinValue(peak.getMass())
                        + " AND max <= "
                        + calcMaxValue(peak.getMass())
                        + "))) AS MCS "
                        + " ON (MCS.mcs_structure <= Candidates.mol_structure)"
                        + " WHERE substance.compound_id = firstcompound_id"
                        + " AND   substance.library_id = library.library_id"
                        + " AND   library_name = 'pubchem'"
                        + " AND   Candidates.compound_id = firstcompound_id"
                        + " GROUP BY accession, inchi_key_1"
                        + " ORDER BY Score DESC) AS results"
                        + " WHERE exact_mass BETWEEN " + calcMinValue(this.UNKNOWN.getExactMass())
                        + " AND " + calcMaxValue(this.UNKNOWN.getExactMass())
                        + " AND substance.compound_id = compound.compound_id"
                        + " AND substance.library_id = library.library_id"
                        + " AND library_name = 'pubchem'"
                        + " AND " + this.config.getStringValue("compoundtable") + ".inchi_key_1 = results.inchi_key_1;";
            } else {
                query += "(min >= "
                        + calcMinValue(peak.getMass())
                        + " AND max <= "
                        + calcMaxValue(peak.getMass())
                        + ") OR ";
            }
        }
        return query;
    }

    public String getSortedQuery() {
        return this.sortedQuery;
    }

    public String getUnSortedQuery() {
        return this.unsortedQuery;
    }

    public String getCorrectCandidateKEGGID() {
        return this.UNKNOWN.getKEGG();
    }

    public String getCorrectCandidatePUBCHEMID() {
        return this.UNKNOWN.getCID() + "";
    }

    public WrapperSpectrum getSpectrum() {
        return this.UNKNOWN;
    }

    public String getCorrectCandidateName() {
        return this.UNKNOWN.getTrivialName();
    }

    public int getAmountOfCandidates() {
        return this.candidatesAmount;
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
        return d - PPMTool.getPPMDeviation(d, this.PPM) - this.mz_abs;
    }

    private Double calcMaxValue(Double d) {
        return d + PPMTool.getPPMDeviation(d, this.PPM) + this.mz_abs;
    }
}
