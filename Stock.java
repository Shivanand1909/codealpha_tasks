import java.io.Serializable;
import java.util.*;

public class Stock implements Serializable {
    private static final long serialVersionUID = 1001L;

    private final String symbol;
    private final String name;
    private final String sector;
    private double currentPrice;
    private double previousPrice;
    private double openPrice;
    private double dayHigh;
    private double dayLow;
    private double high52Week;
    private double low52Week;
    private long   volume;
    private final List<Double> priceHistory = new ArrayList<>();

    public Stock(String symbol, String name, String sector, double initialPrice) {
        this.symbol       = symbol;
        this.name         = name;
        this.sector       = sector;
        this.currentPrice = initialPrice;
        this.previousPrice= initialPrice;
        this.openPrice    = initialPrice;
        this.dayHigh      = initialPrice;
        this.dayLow       = initialPrice;
        this.high52Week   = Math.round(initialPrice * 1.30 * 100.0) / 100.0;
        this.low52Week    = Math.round(initialPrice * 0.70 * 100.0) / 100.0;
        this.volume       = (long)(Math.random() * 5_000_000 + 500_000);
        priceHistory.add(initialPrice);
    }

    public void updatePrice(double newPrice) {
        newPrice       = Math.max(0.01, Math.round(newPrice * 100.0) / 100.0);
        previousPrice  = currentPrice;
        currentPrice   = newPrice;
        if (newPrice > dayHigh)     dayHigh     = newPrice;
        if (newPrice < dayLow)      dayLow      = newPrice;
        if (newPrice > high52Week)  high52Week  = newPrice;
        if (newPrice < low52Week)   low52Week   = newPrice;
        volume = (long)(Math.random() * 8_000_000 + 200_000);
        priceHistory.add(newPrice);
        if (priceHistory.size() > 40) priceHistory.remove(0);
    }

    // --- Getters ---
    public String getSymbol()        { return symbol; }
    public String getName()          { return name; }
    public String getSector()        { return sector; }
    public double getCurrentPrice()  { return currentPrice; }
    public double getPreviousPrice() { return previousPrice; }
    public double getOpenPrice()     { return openPrice; }
    public double getDayHigh()       { return dayHigh; }
    public double getDayLow()        { return dayLow; }
    public double getHigh52Week()    { return high52Week; }
    public double getLow52Week()     { return low52Week; }
    public long   getVolume()        { return volume; }
    public List<Double> getPriceHistory() { return Collections.unmodifiableList(priceHistory); }

    // --- Computed ---
    public double getChangeAmount() { return currentPrice - previousPrice; }

    public double getChangePercent() {
        if (previousPrice == 0) return 0;
        return ((currentPrice - previousPrice) / previousPrice) * 100.0;
    }

    public String getTrend() {
        double c = getChangeAmount();
        return c > 0 ? "^" : c < 0 ? "v" : "-";
    }

    @Override
    public String toString() {
        return String.format("%-5s  $%8.2f  %+6.2f%%  %-14s  %s",
                symbol, currentPrice, getChangePercent(), sector, name);
    }
}
