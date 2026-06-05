import java.io.Serializable;

/**
 * Represents a single stock position in a portfolio.
 * Uses weighted-average cost basis for P&L calculation.
 */
public class Holding implements Serializable {
    private static final long serialVersionUID = 1003L;

    private final String symbol;
    private int    quantity;
    private double totalCost;   // weighted average cost * quantity

    public Holding(String symbol, int quantity, double pricePerShare) {
        this.symbol    = symbol;
        this.quantity  = quantity;
        this.totalCost = quantity * pricePerShare;
    }

    /** Add shares (e.g. after a BUY trade). Updates average cost automatically. */
    public void addShares(int qty, double pricePerShare) {
        totalCost += qty * pricePerShare;
        quantity  += qty;
    }

    /**
     * Remove shares (e.g. after a SELL trade) using average-cost basis.
     * @return cost basis of the removed shares
     */
    public double removeShares(int qty) {
        double avgCost   = getAverageBuyPrice();
        double costBasis = qty * avgCost;
        quantity  -= qty;
        totalCost -= costBasis;
        if (quantity  < 0) quantity  = 0;
        if (totalCost < 0) totalCost = 0.0;
        return costBasis;
    }

    // --- Getters ---
    public String getSymbol()   { return symbol; }
    public int    getQuantity() { return quantity; }
    public double getTotalCost(){ return totalCost; }

    public double getAverageBuyPrice() {
        return quantity > 0 ? totalCost / quantity : 0.0;
    }

    public double getCurrentValue(double currentPrice) {
        return quantity * currentPrice;
    }

    public double getUnrealizedPnL(double currentPrice) {
        return getCurrentValue(currentPrice) - totalCost;
    }

    public double getUnrealizedPnLPct(double currentPrice) {
        return totalCost > 0 ? (getUnrealizedPnL(currentPrice) / totalCost) * 100.0 : 0.0;
    }
}
