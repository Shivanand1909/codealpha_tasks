import java.io.Serializable;
import java.util.*;

/**
 * Represents a registered platform user.
 * Owns a Portfolio and a personal watchlist.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1005L;

    private final String    username;
    private final String    password;      // plain-text for simplicity; hash in production
    private final Portfolio portfolio;
    private final List<String> watchlist = new ArrayList<>();

    public User(String username, String password, double initialBalance) {
        this.username  = username;
        this.password  = password;
        this.portfolio = new Portfolio(initialBalance);
    }

    /** @return true if the supplied password matches. */
    public boolean authenticate(String pwd) {
        return this.password.equals(pwd);
    }

    // --- Getters ---
    public String       getUsername()  { return username; }
    public Portfolio    getPortfolio() { return portfolio; }
    public List<String> getWatchlist() { return watchlist; }

    // --- Watchlist helpers ---
    public boolean addToWatchlist(String symbol) {
        String sym = symbol.toUpperCase();
        if (watchlist.contains(sym)) return false;
        return watchlist.add(sym);
    }

    public boolean removeFromWatchlist(String symbol) {
        return watchlist.remove(symbol.toUpperCase());
    }

    public boolean isWatching(String symbol) {
        return watchlist.contains(symbol.toUpperCase());
    }
}
