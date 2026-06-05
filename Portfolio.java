import java.io.Serializable;
import java.util.*;

/**
 * A user's investment portfolio.
 * Tracks cash balance, stock holdings, realized P&L, and all transactions.
 */
public class Portfolio implements Serializable {
    private static final long serialVersionUID = 1004L;

    private double cashBalance;
    private final double initialBalance;
    private double realizedPnL = 0.0;

    private final Map<String, Holding>  holdings     = new LinkedHashMap<>();
    private final List<Transaction>     transactions = new ArrayList<>();

    public Portfolio(double initialBalance) {
        this.cashBalance    = initialBalance;
        this.initialBalance = initialBalance;
    }

    // =================================================================
    // BUY
    // =================================================================
    /**
     * Executes a market BUY order.
     * @return true if successful, false if insufficient funds.
     */
    public boolean buyStock(Stock stock, int qty) {
        if (qty <= 0) return false;
        double cost = stock.getCurrentPrice() * qty;
        if (cost > cashBalance) return false;

        cashBalance -= cost;

        Holding h = holdings.get(stock.getSymbol());
        if (h == null) {
            holdings.put(stock.getSymbol(),
                    new Holding(stock.getSymbol(), qty, stock.getCurrentPrice()));
        } else {
            h.addShares(qty, stock.getCurrentPrice());
        }

        transactions.add(new Transaction(
                Transaction.Type.BUY,
                stock.getSymbol(), stock.getName(),
                qty, stock.getCurrentPrice()));
        return true;
    }

    // =================================================================
    // SELL
    // =================================================================
    /**
     * Executes a market SELL order.
     * @return true if successful, false if insufficient shares.
     */
    public boolean sellStock(Stock stock, int qty) {
        if (qty <= 0) return false;
        Holding h = holdings.get(stock.getSymbol());
        if (h == null || h.getQuantity() < qty) return false;

        double proceeds  = qty * stock.getCurrentPrice();
        double costBasis = h.removeShares(qty);
        double profit    = proceeds - costBasis;

        cashBalance += proceeds;
        realizedPnL += profit;

        if (h.getQuantity() == 0) holdings.remove(stock.getSymbol());

        transactions.add(new Transaction(
                Transaction.Type.SELL,
                stock.getSymbol(), stock.getName(),
                qty, stock.getCurrentPrice()));
        return true;
    }

    // =================================================================
    // GETTERS
    // =================================================================
    public double getCashBalance()    { return cashBalance; }
    public double getInitialBalance() { return initialBalance; }
    public double getRealizedPnL()    { return realizedPnL; }
    public boolean hasHolding(String symbol) { return holdings.containsKey(symbol); }

    public int getSharesOwned(String symbol) {
        Holding h = holdings.get(symbol);
        return h != null ? h.getQuantity() : 0;
    }

    public Map<String, Holding>  getHoldings()     { return Collections.unmodifiableMap(holdings); }
    public List<Transaction>     getTransactions()  { return Collections.unmodifiableList(transactions); }

    // =================================================================
    // COMPUTED VALUES
    // =================================================================
    public double getStocksValue(Map<String, Stock> market) {
        return holdings.values().stream().mapToDouble(h -> {
            Stock s = market.get(h.getSymbol());
            return s != null ? h.getCurrentValue(s.getCurrentPrice()) : h.getTotalCost();
        }).sum();
    }

    public double getTotalValue(Map<String, Stock> market) {
        return cashBalance + getStocksValue(market);
    }

    public double getUnrealizedPnL(Map<String, Stock> market) {
        return holdings.values().stream().mapToDouble(h -> {
            Stock s = market.get(h.getSymbol());
            return s != null ? h.getUnrealizedPnL(s.getCurrentPrice()) : 0.0;
        }).sum();
    }

    public double getTotalPnL(Map<String, Stock> market) {
        return realizedPnL + getUnrealizedPnL(market);
    }

    public double getTotalPnLPercent(Map<String, Stock> market) {
        return initialBalance > 0
                ? ((getTotalValue(market) - initialBalance) / initialBalance) * 100.0
                : 0.0;
    }

    public double getTotalInvested() {
        return holdings.values().stream().mapToDouble(Holding::getTotalCost).sum();
    }
}
