package Database;

import Algorithems.Algo2;
import Algorithems.Data.Algo2UserInput;
import CSV.Combo.ComboCSV;
import CSV.Combo.ComboLine;
import CSV.Combo.ComboLines;
import CSV.Data.AP_WifiData;
import CSV.Data.Basic.AbstractCSV;
import CSV.Data.GeoPoint;
import CSV.Wigle.WigleCSV;
import Database.Concurrency.ComboLineTask;
import Database.Concurrency.ComboLinesRunner;
import GUI.Panels.Panel_Database;
import Utils.Paths;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Database extends AbstractCSV implements Serializable {

    private static Database INSTANCE;

    static {
        try {
            String path = "Database.csv";
            try {
                Files.createFile(java.nio.file.Paths.get(path));
            } catch (FileAlreadyExistsException e) {

            }
            INSTANCE = new Database(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ComboLines data;
    public int lineCount = 0;
    //private ArrayList<Pair<File, FileType>> filesInDatabase;

    /**
     * Creates empty Database wich represents a csv file with bunch of information. This file
     * can change it's type: Wigle, Combo, OR KML.
     * @throws IOException
     */
    private Database(File fileOfDatabase) throws IOException, ClassNotFoundException {
        super(fileOfDatabase);
        System.out.println("Database initialized successfuly.");
    }


    public void writeDatabaseClassToFile(File fileToWriteTo, Database database) throws IOException {
        Files.createFile(java.nio.file.Paths.get(fileToWriteTo.getAbsolutePath()));
        FileOutputStream fos = new FileOutputStream(fileToWriteTo);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(database);
        oos.close();
        fos.close();
    }

    public Database readDatabase(File databaseFile) throws IOException, ClassNotFoundException {
        if(databaseFile == null)
            return null;
        FileInputStream fis = new FileInputStream(databaseFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        return (Database)ois.readObject();
    }

    /**
     *
     * @return This instance.
     */
    public static Database getINSTANCE() {
        return INSTANCE;
    }

    /**
     * Append wigleCSV into database.
     * @param wigleCSV
     * @throws IOException
     */
    public void append(WigleCSV wigleCSV) throws IOException {
        String outPath = Paths.OUT + "/tmp.csv";
        ComboCSV comboCSV = new ComboCSV(outPath, wigleCSV);
        lineCount += comboCSV.appendToFile(this);
    }

    /**
     * Append comboCSV into the database without checking duplicates.
     * @param comboCSV
     * @throws IOException
     */
    public void append(ComboCSV comboCSV) throws IOException {
        lineCount += comboCSV.appendToFile(this);
    }

    /**
     * Append list of wigle csv.
     * @param wigles
     * @throws IOException
     */
    public void append(List<WigleCSV> wigles) throws IOException {
        if(wigles == null || wigles.size() ==0)
            return;
        System.out.println("Appending " + wigles.size()+ " wigles...");
        int numOfSuccess = 0;
        for(WigleCSV wigleCSV : wigles) {
            try {
                append(wigleCSV);
                numOfSuccess++;
            }catch (Throwable e) {
                e.printStackTrace();
            }
        }
        System.out.println("Successfully added: " + numOfSuccess + " wigles to database.");
    }

    /**
     * Delete this database's data. Empty file.
     * @return
     */
    public boolean delete() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(this);
            writer.print("");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        data.clear();
        return true;
    }

    /**
     *
     * @return Number of lines in the database.
     */
    public int lineCount() {
        return lineCount;
    }

    /**
     * Reads from CSV (After insertions, deletions... after all that) and puts that in RAM. (Init data)
     */
    public void updateDatas() throws Exception {
        data = new ComboLines();
        ArrayList<String[]> lines = new ArrayList<>();
        List<String[]> linesToAdd = read(this);
        if(linesToAdd != null)
            lines.addAll(linesToAdd);
        //TODO: This is so strange! It reads more lines than there actually are!
        System.out.println("[UpdateDatas] Lines: " + lines.size());
        int i = 0;
        for(String[] line : lines) {
            try {
                data.add(new ComboLine(line));
            } catch (Exception e) {
                System.out.println("ERROR LINE: " + i + "AT :" + Arrays.toString(line));
            }
            i++;
        }
        updateDatabaseUI();
    }

    /**
     * Updates the GUI for user of the database.
     */
    private void updateDatabaseUI() {
        if(Panel_Database.getINSTANCE() != null) {
            Panel_Database.getINSTANCE().updateStatistics();
        }
    }
//
//    public void add(List<WigleCSV> wigleCSVList) throws IOException {
//        for(WigleCSV wigleCSV : wigleCSVList) {
//            filesInDatabase.add(new Pair<>(wigleCSV, FileType.Wigle));
//        }
//        String outFilePath = Paths.OUT + "/tmpComboCSV.csv";
//        ComboCSV comboCSV = new ComboCSV(outFilePath,wigleCSVList);
//        add(comboCSV);
//    }

    /**
     * Appends entire comboCSV to database without checking duplicates. (For fast database)
     * @param comboCSV
     * @throws IOException
     */
    public void appendToDatabase(ComboCSV comboCSV) throws IOException {
        comboCSV.appendToFile(this);
    }

    /**
     * Applies filter.
     * @param filter
     * @return Returns list of lines which are true to the filter
     */
    public List<ComboLine> applyFilter(Predicate filter) {
        return (List<ComboLine>) data
                .stream()
                .parallel()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     *
     * @param mac Mac to search.
     * @return Array of AP_WifiData that has the same mac
     * @throws InterruptedException
     */
    public String[] findRoutersByMac(String mac) throws InterruptedException {
        final int indexFrom = 0;
        final int indexTo = data.size();

        ArrayBlockingQueue<AP_WifiData> macsFound = new ArrayBlockingQueue<AP_WifiData>(100);

        ComboLineTask searchMacTask = new ComboLineTask() {
            @Override
            public void run(ComboLine comboLine) {
                for(int i = 0; i < comboLine.size(); i++) {
                    if(comboLine.get(i).mac.equals(mac)) {
                        macsFound.offer(comboLine.get(i));
                    }
                }
            }
        };

        ComboLinesRunner runner1 = new ComboLinesRunner(indexFrom, (indexFrom/4), data, searchMacTask);
        ComboLinesRunner runner2 = new ComboLinesRunner((indexFrom/4)+1, (indexFrom/2), data, searchMacTask);
        ComboLinesRunner runner3 = new ComboLinesRunner((indexFrom/2)+1, (3*(indexFrom/4)), data, searchMacTask);
        ComboLinesRunner runner4 = new ComboLinesRunner((3*(indexFrom/4))+1, indexTo, data, searchMacTask);

        runner1.run();
        runner2.run();
        runner3.run();
        runner4.run();

        runner1.join();
        runner2.join();
        runner3.join();
        runner4.join();

        AP_WifiData[] datasFound = (AP_WifiData[]) macsFound.toArray();
        String[] result = new String[datasFound.length];
        for(int i = 0; i < datasFound.length; i++) {
            result[i] = datasFound[i].location.toString();
        }
        return result;
    }

    //TODO: Finish function
//    /**
//     * No gps line is as described in matala 2 question 2
//     * @param no_gps_line
//     * @return
//     */
//    public GeoPoint calculateLocationByNoGPSLine(String no_gps_line) throws Exception {
//        String[] splitted = no_gps_line.split(",");
//        ComboLine comboLine = new ComboLine(splitted);
//        return Algo1.algo1(data, )
//    }

    public GeoPoint calcLocByInput(Algo2UserInput input1, Algo2UserInput input2, Algo2UserInput input3) {
        return Algo2.algo2(data, input1, input2, input3);
    }
}
