/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Properties;

/**The Configuration is a file which contains important information for the calculating and the process of each optimization algorithm. It also includes some settings for the GUI and the adjustment of the sun gridengine if used.
 *
 * @author Christian Hildebrandt
 */
public class Configuration {

    private File CONFIGFILE, LOCKFILE;
    private Properties property = new Properties();

    /**configuration file which contains several values for the GUI or calculation
     *
     * @param configFile configuration file object
     * @param LockFolder folder where the lock is written temporary
     */
    public Configuration(File ConfigFile) {
        this.CONFIGFILE = ConfigFile;
        this.LOCKFILE = new File(this.CONFIGFILE.getParentFile(), ".lock");

        if (!this.CONFIGFILE.exists()) {
            System.err.println("Configuration file does not exist!");
            System.out.println("Exiting...");
            System.exit(1);
        }
        if (!this.LOCKFILE.exists()) {
            this.CONFIGFILE.getParentFile().mkdirs();
            this.LOCKFILE.mkdirs();
            try {
                FileOutputStream out = new FileOutputStream(this.CONFIGFILE);
                PrintStream p = new PrintStream(out);
                property.setProperty("AUTHOR", "Christian Hildebrandt");
                property.setProperty("INSTITUTION", "IPB Halle");
                property.setProperty("WEB", "http://www.ipb-halle.de");
                property.store(p, "");
                p.close();
            } catch (Exception e) {
            }
        }
    }

    /**set a string value into the configuration file
     *
     * @param key key as which a value should be stored
     * @param value string which shoulb be stored into configuration
     */
    public void setStringValue(String key, String value) {

        property.setProperty(key, value);
        try {
            FileOutputStream out = new FileOutputStream(this.CONFIGFILE);
            PrintStream p = new PrintStream(out);
            property.store(p, "");
            p.close();
        } catch (Exception e) {
            setStringValue(key, value);
        }
    }

    /**set a integer value into the configuration file
     *
     * @param key key as which a value should be stored
     * @param value integer which shoulb be stored into configuration
     */
    public void setIntValue(String key, int value) {

        property.setProperty(key, String.valueOf(value));
        try {
            FileOutputStream out = new FileOutputStream(this.CONFIGFILE);
            PrintStream p = new PrintStream(out);
            property.store(p, "");
            p.close();
        } catch (Exception e) {
            setIntValue(key, value);
        }
    }

    /**set a double value into the configuration file
     *
     * @param key key as which a value should be stored
     * @param value double which shoulb be stored into configuration
     */
    public void setDoubleValue(String key, double value) {

        property.setProperty(key, String.valueOf(value));
        try {
            FileOutputStream out = new FileOutputStream(this.CONFIGFILE);
            PrintStream p = new PrintStream(out);
            property.store(p, "");
            p.close();
        } catch (Exception e) {
            setDoubleValue(key, value);
        }
    }

    /**set a boolean value into the configuration file
     *
     * @param key key as which a value should be stored
     * @param value boolean which shoulb be stored into configuration
     */
    public void setBooleanValue(String key, boolean value) {

        property.setProperty(key, String.valueOf(value));
        try {
            FileOutputStream out = new FileOutputStream(this.CONFIGFILE);
            PrintStream p = new PrintStream(out);
            property.store(p, "");
            p.close();
        } catch (Exception e) {
            setBooleanValue(key, value);
        }
    }

    public void deleteKey(String key) {
        property.keySet().remove(key);
        try {
            FileOutputStream out = new FileOutputStream(this.CONFIGFILE);
            PrintStream p = new PrintStream(out);
            property.store(p, "");
            p.close();
        } catch (Exception e) {
            deleteKey(key);
        }
    }

    /**get a by key specified value from the configuration
     *
     * @param key key as which a value is stored in the configuration file
     * @return string value
     */
    public String getStringValue(String key) {
        try {
            FileInputStream in = new FileInputStream(this.CONFIGFILE);
            property.loadFromXML(in);
            in.close();
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }
        return property.getProperty(key);
    }

    /**get a by key specified value from the configuration
     *
     * @param key key as which a value is stored in the configuration file
     * @return integer value
     */
    public Integer getIntValue(String key) {
        try {
            FileInputStream in = new FileInputStream(this.CONFIGFILE);
            property.loadFromXML(in);
            in.close();
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }

        return Integer.valueOf(property.getProperty(key));
    }

    /**get a by key specified value from the configuration
     *
     * @param key key as which a value is stored in the configuration file
     * @return double value
     */
    public Double getDoubleValue(String key) {
        try {
            FileInputStream in = new FileInputStream(this.CONFIGFILE);
            property.loadFromXML(in);
            in.close();
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }
        try {
            return Double.valueOf(property.getProperty(key));
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            return Double.valueOf(property.getProperty(key));

        }

    }

    /**get a by key specified value from the configuration
     *
     * @param key key as which a value is stored in the configuration file
     * @return boolean value
     */
    public Boolean getBooleanValue(String key) {
        try {
            FileInputStream in = new FileInputStream(this.CONFIGFILE);
            property.loadFromXML(in);
            in.close();
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }

        return Boolean.valueOf(property.getProperty(key));
    }

    /**get the configuration file
     *
     * @return file object which equals the configuration file
     */
    public File getConfigFile() {
        return CONFIGFILE;
    }

    /**set the configuration file
     *
     * @param f file object which equals the configuration file
     */
    public void setConfigFile(File f) {

        this.CONFIGFILE = f;
    }

    /**get the filename of the configuration file
     *
     * @return string name
     */
    public String getConfigFileName() {
        return this.CONFIGFILE.getName();
    }

    /**set the filename of the configuration file
     *
     * @param str filename string
     */
    public void setConfigFileName(String str) {
        this.CONFIGFILE = new File(str);
    }
}
