import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1002L;

    public enum Type { BUY, SELL }

    private final Type          type;
    private final String        symbol;
    private final String        stockName;
    private final int           quantity;
    private final double        pricePerShare;
    private final double        totalAmount;
    private final LocalDateTime timestamp;

    public Transaction(Type type, String symbol, String stockName,
                       int quantity, double pricePerShare) {
        this.type          = type;
        this.symbol        = symbol;
        this.stockName     = stockName;
        this.quantity      = quantity;
        this.pricePerShare = pricePerShare;
        this.totalAmount   = quantity * pricePerShare;
        this.timestamp     = LocalDateTime.now();
    }

    // --- Getters ---
    public Type          getType()          { return type; }
    public String        getSymbol()        { return symbol; }
    public String        getStockName()     { return stockName; }
    public int           getQuantity()      { return quantity; }
    public double        getPricePerShare() { return pricePerShare; }
    public double        getTotalAmount()   { return totalAmount; }
    public LocalDateTime getTimestamp()     { return timestamp; }

    public String getFormattedDate() {
        return timestamp.format(DateTimeFormatter.ofPattern("MM/dd/yy HH:mm"));
    }

    @Override
    public String toString() {
        return String.format("%-14s  %-4s  %-5s  %-22s  %5d  %9.2f  %12.2f",
                getFormattedDate(), type, symbol,
                stockName.length() > 22 ? stockName.substring(0, 21) + "." : stockName,
                quantity, pricePerShare, totalAmount);
    }
}
