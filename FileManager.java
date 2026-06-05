import java.io.*;
import java.util.*;

/**
 * Handles persistence using Java Object Serialization.
 * Saves/loads the entire users map and market state to disk.
 *
 * Data directory: ./trading_data/
 *   users.dat   – all registered users + portfolios
 *   market.dat  – stock universe + price history
 */
public class FileManager {

    private static final String DIR      = "trading_data";
    private static final String USERS_F  = DIR + File.separator + "users.dat";
    private static final String MARKET_F = DIR + File.separator + "market.dat";

    // ----------------------------------------------------------------
    //  SAVE
    // ----------------------------------------------------------------
    public static void save(Map<String, User> users, Market market) {
        try {
            new File(DIR).mkdirs();

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(USERS_F)))) {
                oos.writeObject(users);
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(MARKET_F)))) {
                oos.writeObject(market);
            }

            System.out.println("  [OK] Data saved to ./" + DIR + "/");

        } catch (IOException e) {
            System.out.println("  [!] Save error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    //  LOAD USERS
    // ----------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public static Map<String, User> loadUsers() {
        File f = new File(USERS_F);
        if (!f.exists()) return new LinkedHashMap<>();

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {
            return (Map<String, User>) ois.readObject();
        } catch (Exception e) {
            System.out.println("  [!] Could not load users: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    // ----------------------------------------------------------------
    //  LOAD MARKET
    // ----------------------------------------------------------------
    public static Market loadMarket() {
        File f = new File(MARKET_F);
        if (!f.exists()) return new Market();

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {
            return (Market) ois.readObject();
        } catch (Exception e) {
            System.out.println("  [!] Could not load market: " + e.getMessage());
            return new Market();
        }
    }

    public static boolean hasSavedData() {
        return new File(USERS_F).exists();
    }
}
