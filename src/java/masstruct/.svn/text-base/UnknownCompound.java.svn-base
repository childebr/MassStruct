/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package masstruct;

import de.ipbhalle.metfrag.massbankParser.Peak;
import de.ipbhalle.metfrag.spectrum.WrapperSpectrum;
import de.ipbhalle.metfrag.tools.PPMTool;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;
import misc.Configuration;

/**
 *
 * @author childebr
 */
public class UnknownCompound {

    private WrapperSpectrum UNKNOWN;
    private Vector<String> databases = new Vector<String>();
    private ArrayList<String> SortedCandidates = new ArrayList<String>();
    private ArrayList<Double> SortedCandidatesScores = new ArrayList<Double>();
    private ArrayList<String> UnSortedCandidates = new ArrayList<String>();
    private Configuration config = null;
    private Connection CONN;
    private Integer candidatesAmount = 0;
    private Double PPM = 10.0;
    private Double mz_abs = 0.1;
    private long QueryRuntime;
    private String sortedQuery, unsortedQuery;

    public UnknownCompound(WrapperSpectrum ws) {
        this.UNKNOWN = ws;
        this.CONN = connectToDatabase();
    }

    public UnknownCompound(WrapperSpectrum ws, Double ppm) {
        this.UNKNOWN = ws;
        this.PPM = ppm;
        this.CONN = connectToDatabase();
    }

    public UnknownCompound(WrapperSpectrum ws, Double ppm, Configuration config) {
        this.UNKNOWN = ws;
        this.PPM = ppm;
        this.config = config;
        this.CONN = connectToDatabase();
    }

    public UnknownCompound(WrapperSpectrum ws, Double ppm, Vector<String> databasesToSearchIn) {
        this.UNKNOWN = ws;
        this.PPM = ppm;
        this.databases = databasesToSearchIn;
        this.CONN = connectToDatabase();
    }

    private Vector<String> getSortedCandidates(Connection cn, WrapperSpectrum ws, String exactMassRestriction) {
        Vector<String> SortedList = new Vector<String>();
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        String query = null;
        try {
            //try to parse restriction in ppm
            double ppm = Double.parseDouble(exactMassRestriction);
            query = "SELECT"
                    + " substance.accession, Score"
                    + " FROM substance, compound" + ", library,"
                    + " (SELECT inchi_key_1,"
                    + " COUNT(DISTINCT PeakFragments.mz_cluster_id) AS Score"
                    + " FROM substance,"
                    + " library,"
                    + " (SELECT MIN(compound_id) AS firstcompound_id"
                    + " FROM compound"
                    + " WHERE exact_mass BETWEEN " + calcMinValue(ws.getExactMass())
                    + " AND " + calcMaxValue(ws.getExactMass())
                    + " GROUP BY inchi_key_1) AS firstcandidate,"
                    + " compound AS Candidates"
                    + " LEFT OUTER JOIN (SELECT mcs.mcs_structure,"
                    + " mz_cluster.cluster_id"
                    + " FROM mcs,"
                    + " mz_cluster"
                    + " WHERE mcs.mz_cluster_id = mz_cluster.cluster_id"
                    + " AND (";
            for (Peak peak : ws.getPeakList()) {
                if (peak.equals(ws.getPeakList().lastElement())) {
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
                            + " WHERE exact_mass BETWEEN " + calcMinValue(ws.getExactMass())
                            + " AND " + calcMaxValue(ws.getExactMass())
                            + " AND substance.compound_id = compound.compound_id"
                            + " AND substance.library_id = library.library_id"
                            + " AND library_name = 'pubchem'"
                            + " AND compound.inchi_key_1 = results.inchi_key_1;";
                } else {
                    query += "(min >= "
                            + calcMinValue(peak.getMass())
                            + " AND max <= "
                            + calcMaxValue(peak.getMass())
                            + ") OR ";
                }
            }
        } catch (Exception e) {
            //it is not a ppm but a sum formula
            query = "SELECT"
                    + " substance.accession, Score"
                    + " FROM substance, compound" + ", library,"
                    + " (SELECT inchi_key_1,"
                    + " COUNT(DISTINCT MCS.cluster_id) AS Score"
                    + " FROM substance,"
                    + " library,"
                    + " (SELECT MIN(compound_id) AS firstcompound_id"
                    + " FROM compound"
                    + " WHERE formula = " + exactMassRestriction
                    + " GROUP BY inchi_key_1) AS firstcandidate,"
                    + " compound AS Candidates"
                    + " LEFT OUTER JOIN (SELECT mcs.mcs_structure,"
                    + " mz_cluster.cluster_id"
                    + " FROM mcs,"
                    + " mz_cluster"
                    + " WHERE mcs.mz_cluster_id = mz_cluster.cluster_id"
                    + " AND (";
            for (Peak peak : ws.getPeakList()) {
                if (peak.equals(ws.getPeakList().lastElement())) {
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
                            + " WHERE formula = " + exactMassRestriction
                            + " AND substance.compound_id = compound.compound_id"
                            + " AND substance.library_id = library.library_id"
                            + " AND library_name = 'pubchem'"
                            + " AND compound.inchi_key_1 = results.inchi_key_1;";
                } else {
                    query += "(min >= "
                            + calcMinValue(peak.getMass())
                            + " AND max <= "
                            + calcMaxValue(peak.getMass())
                            + ") OR ";
                }
            }
        }
        try {
            pstmt = cn.prepareStatement(query);
            rs = pstmt.executeQuery();
        } catch (Exception e) {
            System.err.println("Problems with Statement: "+e.getLocalizedMessage());
        }
        try {
            while (rs.next()) {
                SortedList.add(rs.getString(1));
            }
        } catch (Exception e) {
            System.err.println("Problems with Resultset: " + e.getLocalizedMessage());
        }
        return SortedList;
    }

    private String SortedQuery(boolean isppm, double ppm) {

        int selectcounter = 0, wherecounter = 0;
//        String query = "SELECT compound_id, COUNT(mcs_structure) AS Score FROM mcs, mz_cluster, "
        String query = "SELECT"
                + " substance.accession, Score"
                + " FROM substance, " + this.config.getStringValue("compoundtable") + ", library,"
                + " (SELECT inchi_key_1,"
                + " COUNT(DISTINCT MCS.cluster_id) AS Score"
                + " FROM substance,"
                + " library,"
                + " (SELECT MIN(compound_id) AS firstcompound_id"
                + " FROM " + this.config.getStringValue("compoundtable")
                + " WHERE exact_mass BETWEEN " + calcMinValue(this.UNKNOWN.getExactMass())
                + " AND " + calcMaxValue(this.UNKNOWN.getExactMass())
                + " GROUP BY inchi_key_1) AS firstcandidate,"
                + " " + this.config.getStringValue("compoundtable") + " AS Candidates"
                + " LEFT OUTER JOIN (SELECT mcs.mcs_structure,"
                + " mz_cluster.cluster_id"
                + " FROM mcs,"
                + " mz_cluster"
                + " WHERE mcs.mz_cluster_id = mz_cluster.cluster_id"
                + " AND (";
        for (Peak peak : this.UNKNOWN.getPeakList()) {
            selectcounter++;
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
        this.sortedQuery = query;
//        System.out.println("Correct KEGG_ID " + this.unknown.getKEGG() + " PUBCHEM_ID " + this.unknown.getCID());
//        System.out.println("\n");
//        System.out.println(query);
//        System.exit(1);
        return query;
    }

    public ArrayList<String> getUnSortedCandidateList() {
        UnSortedCandidates = new ArrayList<String>();
        ResultSet rs = null;

        try {
            Statement st = null;
            st = CONN.createStatement();
            long time = System.currentTimeMillis();
            rs = st.executeQuery(this.UnsortedQuery());
            this.QueryRuntime = System.currentTimeMillis() - time;
            int coloumns = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                UnSortedCandidates.add(rs.getString(2));

//                for (int i = 1; i <= coloumns; i++) // Attention: first column with 1 instead of 0
//                {
//                    Candidates.add(rs.getString(i) + " ");
//                }

            }
            rs.close();
            if (rs.isClosed()) {
                st.close();
            } else {
                rs.close();
                st.close();
            }
        } catch (Exception e) {
//            System.out.println(expression);
            System.err.println(e.getLocalizedMessage());
        }
        this.candidatesAmount = UnSortedCandidates.size();
        return UnSortedCandidates;
    }

    public ArrayList<String> getSortedCandidateList() {

        ResultSet rs = null;
        try {
            Statement st = null;
            st = CONN.createStatement();
            long time = System.currentTimeMillis();
            rs = st.executeQuery(this.SortedQuery());
            this.QueryRuntime = System.currentTimeMillis() - time;
            int coloumns = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                SortedCandidates.add(rs.getString(1));
                SortedCandidatesScores.add(Double.parseDouble(rs.getString(2)));
            }
            rs.close();
            if (rs.isClosed()) {
                st.close();
            } else {
                rs.close();
                st.close();
            }
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }
        this.candidatesAmount = SortedCandidates.size();
        return SortedCandidates;
    }

    

    private Connection connectToDatabase() {

        String DbDrv = null, DbUrl = null, User = null, Passwd = null, DataBase = null;
        Connection cn = null;

        try {
            DbUrl = config.getStringValue("url");
            DbDrv = config.getStringValue("DatabaseDriver");
            User = config.getStringValue("user");
            Passwd = config.getStringValue("password");
            DataBase = config.getStringValue("database");
        } catch (Exception ex) {
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
                    System.err.println("Class not Found" + cnfe.getLocalizedMessage());
                }
                try {
                    cn = DriverManager.getConnection("jdbc:postgresql://" + DbUrl + "/" + DataBase + "/test?user=" + User);
                } catch (SQLException se) {
                    System.out.println("Couldn't connect: print out a stack trace and exit.");
                    se.printStackTrace();
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

    public void closeConnection() {
        try {
            if (null != this.CONN) {
                this.CONN.close();
            }
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }


    }
    //PPMTool.getPPMDeviation(peak, ppm);

    private String SortedQuery() {

        int selectcounter = 0, wherecounter = 0;
//        String query = "SELECT compound_id, COUNT(mcs_structure) AS Score FROM mcs, mz_cluster, "
        String query = "SELECT"
                + " substance.accession, Score"
                + " FROM substance, " + this.config.getStringValue("compoundtable") + ", library,"
                + " (SELECT inchi_key_1,"
                + " COUNT(DISTINCT MCS.cluster_id) AS Score"
                + " FROM substance,"
                + " library,"
                + " (SELECT MIN(compound_id) AS firstcompound_id"
                + " FROM " + this.config.getStringValue("compoundtable")
                + " WHERE exact_mass BETWEEN " + calcMinValue(this.UNKNOWN.getExactMass())
                + " AND " + calcMaxValue(this.UNKNOWN.getExactMass())
                + " GROUP BY inchi_key_1) AS firstcandidate,"
                + " " + this.config.getStringValue("compoundtable") + " AS Candidates"
                + " LEFT OUTER JOIN (SELECT mcs.mcs_structure,"
                + " mz_cluster.cluster_id"
                + " FROM mcs,"
                + " mz_cluster"
                + " WHERE mcs.mz_cluster_id = mz_cluster.cluster_id"
                + " AND (";
        for (Peak peak : this.UNKNOWN.getPeakList()) {
            selectcounter++;
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
        this.sortedQuery = query;
//        System.out.println("Correct KEGG_ID " + this.unknown.getKEGG() + " PUBCHEM_ID " + this.unknown.getCID());
//        System.out.println("\n");
//        System.out.println(query);
//        System.exit(1);
        return query;
    }

    private String UnsortedQuery() {

        String query = "SELECT cid FROM " + this.config.getStringValue("compoundtable")
                + " WHERE (mass BETWEEN " + calcMinValue(this.UNKNOWN.getExactMass())
                + " AND " + calcMaxValue(this.UNKNOWN.getExactMass()) + ")";
        this.unsortedQuery = query;
        //System.out.println(query);
        return query;
    }

    public long getQueryRuntimeMS() {
        return this.QueryRuntime;
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

    public ArrayList<Double> getSortedCandidateScores() {
        return this.SortedCandidatesScores;
    }

    private Double calcMinValue(Double d) {
        return d - PPMTool.getPPMDeviation(d, this.PPM) - this.mz_abs;
    }

    private Double calcMaxValue(Double d) {
        return d + PPMTool.getPPMDeviation(d, this.PPM) + this.mz_abs;
    }
}
