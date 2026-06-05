import java.io.Serializable;
import java.util.*;

/**
 * Simulated stock market.
 * Holds 20 stocks across 5 sectors and provides realistic price updates
 * using a Gaussian random walk with occasional volatility spikes.
 */
public class Market implements Serializable {
    private static final long serialVersionUID = 1006L;

    private final Map<String, Stock> stocks = new LinkedHashMap<>();
    private final Random random = new Random();
    private int    updateCount   = 0;
    private String lastMarketNews = "Markets opened. Welcome to the trading session.";

    public Market() { initializeStocks(); }

    // ----------------------------------------------------------------
    //  Stock universe  (20 stocks, 5 sectors)
    // ----------------------------------------------------------------
    private void initializeStocks() {
        // Technology
        add("AAPL",  "Apple Inc.",                "Technology",    182.52);
        add("MSFT",  "Microsoft Corporation",     "Technology",    415.31);
        add("GOOGL", "Alphabet Inc.",             "Technology",    171.84);
        add("NVDA",  "NVIDIA Corporation",        "Technology",    875.42);
        add("AMD",   "Advanced Micro Devices",    "Technology",    162.33);
        add("META",  "Meta Platforms Inc.",       "Technology",    497.20);

        // Finance
        add("JPM",   "JPMorgan Chase & Co.",      "Finance",       201.54);
        add("BAC",   "Bank of America Corp.",     "Finance",        38.92);
        add("V",     "Visa Inc.",                 "Finance",       274.81);
        add("MA",    "Mastercard Inc.",           "Finance",       462.15);

        // Healthcare
        add("JNJ",   "Johnson & Johnson",         "Healthcare",    153.43);
        add("PFE",   "Pfizer Inc.",               "Healthcare",     26.82);
        add("MRNA",  "Moderna Inc.",              "Healthcare",     89.52);

        // Consumer / Retail
        add("AMZN",  "Amazon.com Inc.",           "Consumer",      186.35);
        add("WMT",   "Walmart Inc.",              "Consumer",       67.81);
        add("KO",    "The Coca-Cola Company",     "Consumer",       62.52);

        // Energy
        add("XOM",   "ExxonMobil Corporation",    "Energy",        111.73);
        add("CVX",   "Chevron Corporation",       "Energy",        152.43);

        // Entertainment
        add("NFLX",  "Netflix Inc.",              "Entertainment", 631.25);
        add("DIS",   "The Walt Disney Company",   "Entertainment",  96.43);
    }

    private void add(String sym, String name, String sector, double price) {
        stocks.put(sym, new Stock(sym, name, sector, price));
    }

    // ----------------------------------------------------------------
    //  Price simulation  (Gaussian random walk + spike events)
    // ----------------------------------------------------------------
    public void simulateUpdate() {
        updateCount++;

        // Rotate market news headlines
        String[] headlines = {
            "Mixed economic signals keep investors cautious.",
            "Tech sector rallies on strong earnings forecasts.",
            "Energy stocks slip as oil prices ease.",
            "Banking shares climb on rate-hike speculation.",
            "Healthcare sector steady amid regulatory updates.",
            "Consumer spending beats expectations; retail rises.",
            "Global trade uncertainty weighs on markets.",
            "Inflation data triggers broad market volatility.",
            "Federal Reserve signals potential policy change.",
            "Semiconductor demand drives tech stock surge.",
            "Pharma sector gains on new drug approval news.",
            "Market reaches new intraday high before pullback."
        };
        lastMarketNews = headlines[random.nextInt(headlines.length)];

        for (Stock s : stocks.values()) {
            // Base volatility per sector
            double vol = switch (s.getSector()) {
                case "Technology"    -> 0.035;
                case "Entertainment" -> 0.028;
                case "Energy"        -> 0.025;
                case "Healthcare"    -> 0.020;
                case "Finance"       -> 0.018;
                default              -> 0.015;
            };

            // Core random walk (Gaussian)
            double pctChange = random.nextGaussian() * vol;

            // 6% chance of a news-driven spike (±8%)
            if (random.nextDouble() < 0.06) {
                pctChange += (random.nextBoolean() ? 1 : -1) * 0.08;
            }

            // Soft mean-reversion: nudge back toward open price
            double openPrice  = s.getOpenPrice();
            double currPrice  = s.getCurrentPrice();
            if (currPrice > openPrice * 1.15) pctChange -= 0.005;
            if (currPrice < openPrice * 0.85) pctChange += 0.005;

            double newPrice = Math.max(1.0, s.getCurrentPrice() * (1 + pctChange));
            s.updatePrice(newPrice);
        }
    }

    // ----------------------------------------------------------------
    //  Query helpers
    // ----------------------------------------------------------------
    public List<Stock> getTopGainers(int n) {
        List<Stock> list = new ArrayList<>(stocks.values());
        list.sort(Comparator.comparingDouble(Stock::getChangePercent).reversed());
        return list.subList(0, Math.min(n, list.size()));
    }

    public List<Stock> getTopLosers(int n) {
        List<Stock> list = new ArrayList<>(stocks.values());
        list.sort(Comparator.comparingDouble(Stock::getChangePercent));
        return list.subList(0, Math.min(n, list.size()));
    }

    public Map<String, List<Stock>> getStocksBySector() {
        Map<String, List<Stock>> map = new TreeMap<>();
        for (Stock s : stocks.values())
            map.computeIfAbsent(s.getSector(), k -> new ArrayList<>()).add(s);
        return map;
    }

    public Stock            getStock(String symbol)  { return stocks.get(symbol.toUpperCase()); }
    public Map<String, Stock> getAllStocks()          { return Collections.unmodifiableMap(stocks); }
    public int              getUpdateCount()          { return updateCount; }
    public String           getLastMarketNews()       { return lastMarketNews; }
}
