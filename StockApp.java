import java.util.*;
import java.util.stream.Collectors;

/**
 * StockApp — Main entry point for the Stock Trading Platform.
 *
 * Menu structure:
 *   [Guest]  Login | Register | View Market | Exit
 *   [User]   Market Overview | Trade | Portfolio | Watchlist | Refresh | Logout | Exit
 *
 * OOP design:
 *   Stock        – stock data & price history
 *   Transaction  – immutable BUY/SELL record
 *   Holding      – position in a portfolio (avg-cost basis)
 *   Portfolio    – cash + holdings + realized P&L
 *   User         – account (portfolio + watchlist)
 *   Market       – 20-stock universe + Gaussian price simulation
 *   FileManager  – Java serialization persistence
 */
public class StockApp {

    static final Scanner          scanner  = new Scanner(System.in);
    static       Market           market;
    static       Map<String, User> users;
    static       User             currentUser;

    static final double INITIAL_BALANCE   = 10_000.00;
    static final String LINE_THIN        = "  " + "-".repeat(72);
    static final String LINE_THICK       = "  " + "=".repeat(72);

    // ================================================================
    //  MAIN
    // ================================================================
    public static void main(String[] args) {
        System.out.println("\n  Loading saved data...");
        users  = FileManager.loadUsers();
        market = FileManager.loadMarket();
        System.out.printf("  Users: %d  |  Stocks: %d  |  Market updates: %d%n%n",
                users.size(), market.getAllStocks().size(), market.getUpdateCount());

        printBanner();

        // Auto-save hook on abnormal exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            FileManager.save(users, market);
        }));

        boolean running = true;
        while (running) {
            running = (currentUser == null) ? guestMenu() : mainMenu();
        }

        FileManager.save(users, market);
        printGoodbye();
        scanner.close();
    }

    // ================================================================
    //  GUEST MENU
    // ================================================================
    static boolean guestMenu() {
        System.out.println();
        System.out.println(LINE_THICK);
        System.out.println("  |            STOCK TRADING PLATFORM  —  GUEST             |");
        System.out.println(LINE_THICK);
        System.out.println("  |  [1]  Login                                             |");
        System.out.println("  |  [2]  Register New Account                              |");
        System.out.println("  |  [3]  Browse Market (Guest)                             |");
        System.out.println("  |  [0]  Exit                                              |");
        System.out.println(LINE_THICK);

        switch (intInput("  Choice: ")) {
            case 1  -> doLogin();
            case 2  -> doRegister();
            case 3  -> marketOverviewMenu(false);
            case 0  -> { return false; }
            default -> warn("Invalid choice.");
        }
        return true;
    }

    // ================================================================
    //  MAIN MENU  (logged-in)
    // ================================================================
    static boolean mainMenu() {
        Portfolio pf        = currentUser.getPortfolio();
        Map<String,Stock> m = market.getAllStocks();
        double totalVal     = pf.getTotalValue(m);
        double pnl          = pf.getTotalPnL(m);
        double pnlPct       = pf.getTotalPnLPercent(m);

        System.out.println();
        System.out.println(LINE_THICK);
        System.out.printf ("  |  User: %-10s   Portfolio: $%,.2f   P&L: %+.2f%% (%+,.2f)%n",
                currentUser.getUsername(), totalVal, pnlPct, pnl);
        System.out.printf ("  |  Cash: $%,.2f   Stocks: $%,.2f   Invested: $%,.2f%n",
                pf.getCashBalance(), pf.getStocksValue(m), pf.getTotalInvested());
        System.out.printf ("  |  Market Updates: %-4d   News: %s%n",
                market.getUpdateCount(),
                truncate(market.getLastMarketNews(), 40));
        System.out.println(LINE_THICK);
        System.out.println("  |  [1]  Market Overview           [5]  Refresh Market Prices  |");
        System.out.println("  |  [2]  Trade (Buy / Sell)         [6]  Logout                 |");
        System.out.println("  |  [3]  My Portfolio               [0]  Exit & Save            |");
        System.out.println("  |  [4]  Watchlist                                              |");
        System.out.println(LINE_THICK);

        switch (intInput("  Choice: ")) {
            case 1  -> marketOverviewMenu(true);
            case 2  -> tradeMenu();
            case 3  -> portfolioMenu();
            case 4  -> watchlistMenu();
            case 5  -> refreshMarket();
            case 6  -> { currentUser = null; ok("Logged out successfully."); }
            case 0  -> { return false; }
            default -> warn("Invalid choice.");
        }
        return true;
    }

    // ================================================================
    //  AUTH — LOGIN / REGISTER
    // ================================================================
    static void doLogin() {
        header("LOGIN");
        System.out.print("  Username : ");
        String uname = scanner.nextLine().trim();
        System.out.print("  Password : ");
        String pass  = scanner.nextLine().trim();

        User u = users.get(uname.toLowerCase());
        if (u == null || !u.authenticate(pass)) {
            warn("Invalid username or password.");
        } else {
            currentUser = u;
            ok("Welcome back, " + u.getUsername() + "!");
        }
    }

    static void doRegister() {
        header("CREATE ACCOUNT");
        System.out.print("  Choose username : ");
        String uname = scanner.nextLine().trim();
        if (uname.isEmpty())                          { warn("Username cannot be empty.");    return; }
        if (users.containsKey(uname.toLowerCase()))   { warn("Username is already taken.");   return; }

        System.out.print("  Choose password : ");
        String pass  = scanner.nextLine().trim();
        if (pass.isEmpty())                           { warn("Password cannot be empty.");    return; }

        System.out.print("  Confirm password: ");
        String pass2 = scanner.nextLine().trim();
        if (!pass.equals(pass2))                      { warn("Passwords do not match.");      return; }

        User u = new User(uname, pass, INITIAL_BALANCE);
        users.put(uname.toLowerCase(), u);
        currentUser = u;
        ok(String.format("Account created!  Starting balance: $%,.2f", INITIAL_BALANCE));
    }

    // ================================================================
    //  MARKET OVERVIEW  (shared by guest + logged-in)
    // ================================================================
    static void marketOverviewMenu(boolean loggedIn) {
        boolean loop = true;
        while (loop) {
            header("MARKET OVERVIEW");
            System.out.println("  [1]  All Stocks (table)");
            System.out.println("  [2]  View by Sector");
            System.out.println("  [3]  Top Gainers & Losers");
            System.out.println("  [4]  Stock Detail & Price Chart");
            System.out.println("  [0]  Back");
            System.out.println(LINE_THIN);

            switch (intInput("  Choice: ")) {
                case 1  -> showAllStocks();
                case 2  -> showBySector();
                case 3  -> showGainersLosers();
                case 4  -> showStockDetail(loggedIn ? currentUser : null);
                case 0  -> loop = false;
                default -> warn("Invalid choice.");
            }
        }
    }

    // --- All Stocks Table ---
    static void showAllStocks() {
        header("ALL STOCKS  (" + market.getAllStocks().size() + " listed)");
        System.out.printf("  %-5s  %-26s  %-14s  %8s  %9s  %7s  %8s%n",
                "Sym", "Name", "Sector", "Price", "Change", "Chg %", "Volume");
        System.out.println(LINE_THIN);

        for (Stock s : market.getAllStocks().values()) {
            double chg = s.getChangeAmount();
            double pct = s.getChangePercent();
            System.out.printf("  %-5s  %-26s  %-14s  %8.2f  %+9.2f  %+6.2f%%  %8s%n",
                    s.getSymbol(),
                    truncate(s.getName(), 26),
                    s.getSector(),
                    s.getCurrentPrice(),
                    chg, pct,
                    fmtVol(s.getVolume()));
        }
        System.out.println(LINE_THIN);
        System.out.println("  News: " + market.getLastMarketNews());
    }

    // --- By Sector ---
    static void showBySector() {
        header("STOCKS BY SECTOR");
        for (Map.Entry<String, List<Stock>> e : market.getStocksBySector().entrySet()) {
            System.out.println();
            System.out.println("  +--[ " + e.getKey().toUpperCase() + " ]" + "-".repeat(58));
            System.out.printf("  |  %-5s  %-26s  %8s  %9s  %7s%n",
                    "Sym", "Name", "Price", "Change", "Chg %");
            System.out.println("  |  " + "-".repeat(60));
            for (Stock s : e.getValue()) {
                System.out.printf("  |  %-5s  %-26s  %8.2f  %+9.2f  %+6.2f%%%n",
                        s.getSymbol(), truncate(s.getName(), 26),
                        s.getCurrentPrice(), s.getChangeAmount(), s.getChangePercent());
            }
        }
        System.out.println();
    }

    // --- Top Gainers & Losers ---
    static void showGainersLosers() {
        header("TOP GAINERS & LOSERS");
        List<Stock> gainers = market.getTopGainers(5);
        List<Stock> losers  = market.getTopLosers(5);

        System.out.println("  TOP 5 GAINERS:");
        System.out.println(LINE_THIN);
        int rank = 1;
        for (Stock s : gainers) {
            System.out.printf("  %d.  %-5s  %-26s  $%8.2f  %+6.2f%%%n",
                    rank++, s.getSymbol(), truncate(s.getName(), 26),
                    s.getCurrentPrice(), s.getChangePercent());
        }

        System.out.println();
        System.out.println("  TOP 5 LOSERS:");
        System.out.println(LINE_THIN);
        rank = 1;
        for (Stock s : losers) {
            System.out.printf("  %d.  %-5s  %-26s  $%8.2f  %+6.2f%%%n",
                    rank++, s.getSymbol(), truncate(s.getName(), 26),
                    s.getCurrentPrice(), s.getChangePercent());
        }
    }

    // --- Stock Detail with ASCII chart ---
    static void showStockDetail(User user) {
        System.out.print("  Enter stock symbol: ");
        String sym = scanner.nextLine().trim().toUpperCase();
        Stock s = market.getStock(sym);
        if (s == null) { warn("Stock '" + sym + "' not found."); return; }

        header(s.getSymbol() + "  --  " + s.getName());
        System.out.printf("  Current Price  : $%,.2f          Trend: %s%n",
                s.getCurrentPrice(), s.getTrend());
        System.out.printf("  Change         : %+.2f  (%+.2f%%)%n",
                s.getChangeAmount(), s.getChangePercent());
        System.out.printf("  Open           : $%,.2f%n", s.getOpenPrice());
        System.out.printf("  Day  High/Low  : $%,.2f  /  $%,.2f%n",
                s.getDayHigh(), s.getDayLow());
        System.out.printf("  52W  High/Low  : $%,.2f  /  $%,.2f%n",
                s.getHigh52Week(), s.getLow52Week());
        System.out.printf("  Volume         : %s%n", fmtVol(s.getVolume()));
        System.out.printf("  Sector         : %s%n", s.getSector());

        if (user != null) {
            int owned = user.getPortfolio().getSharesOwned(sym);
            System.out.println("  " + "-".repeat(40));
            System.out.printf("  Shares Owned   : %d%n", owned);
            if (owned > 0) {
                Holding h = user.getPortfolio().getHoldings().get(sym);
                System.out.printf("  Avg Buy Price  : $%,.2f%n", h.getAverageBuyPrice());
                System.out.printf("  Position Value : $%,.2f%n",
                        h.getCurrentValue(s.getCurrentPrice()));
                System.out.printf("  Unrealized P&L : %+,.2f  (%+.2f%%)%n",
                        h.getUnrealizedPnL(s.getCurrentPrice()),
                        h.getUnrealizedPnLPct(s.getCurrentPrice()));
            }
            System.out.printf("  Watching       : %s%n",
                    user.isWatching(sym) ? "Yes" : "No");
        }

        // ASCII price chart
        List<Double> hist = s.getPriceHistory();
        if (hist.size() > 2) {
            System.out.println();
            System.out.println("  Price History Chart  (last " + hist.size() + " ticks):");
            printAsciiChart(hist, Math.min(50, hist.size()), 7);
        }
    }

    // ================================================================
    //  TRADE MENU
    // ================================================================
    static void tradeMenu() {
        boolean loop = true;
        while (loop) {
            Portfolio pf = currentUser.getPortfolio();
            header("TRADE");
            System.out.printf("  Cash Available: $%,.2f%n", pf.getCashBalance());
            System.out.println("  [1]  Buy Stock");
            System.out.println("  [2]  Sell Stock");
            System.out.println("  [3]  Quick-View Holdings");
            System.out.println("  [0]  Back");
            System.out.println(LINE_THIN);

            switch (intInput("  Choice: ")) {
                case 1  -> doBuy();
                case 2  -> doSell();
                case 3  -> showHoldingsCompact();
                case 0  -> loop = false;
                default -> warn("Invalid choice.");
            }
        }
    }

    static void doBuy() {
        header("BUY STOCK");
        System.out.print("  Stock symbol (or LIST): ");
        String sym = scanner.nextLine().trim().toUpperCase();

        if (sym.equals("LIST")) { showAllStocks(); return; }

        Stock s = market.getStock(sym);
        if (s == null) { warn("Stock '" + sym + "' not found. Type LIST to browse."); return; }

        Portfolio pf = currentUser.getPortfolio();
        int maxShares = (int)(pf.getCashBalance() / s.getCurrentPrice());

        System.out.println();
        System.out.printf("  %-20s : %s%n", "Stock",         s.getName());
        System.out.printf("  %-20s : $%,.2f  (%+.2f%%)%n", "Current Price",
                s.getCurrentPrice(), s.getChangePercent());
        System.out.printf("  %-20s : $%,.2f%n", "Cash Balance",   pf.getCashBalance());
        System.out.printf("  %-20s : %d shares%n", "Max Affordable", maxShares);
        if (pf.hasHolding(sym))
            System.out.printf("  %-20s : %d shares (avg $%.2f)%n", "Currently Owned",
                    pf.getSharesOwned(sym),
                    pf.getHoldings().get(sym).getAverageBuyPrice());
        System.out.println();

        if (maxShares == 0) { warn("Insufficient funds to buy even 1 share."); return; }

        int qty = intInput("  Quantity to buy (0=cancel): ");
        if (qty <= 0) { System.out.println("  Cancelled."); return; }

        double total = qty * s.getCurrentPrice();
        System.out.printf("%n  >> Confirm BUY %d x %s @ $%.2f  =  $%,.2f ? [y/n]: ",
                qty, sym, s.getCurrentPrice(), total);
        String conf = scanner.nextLine().trim().toLowerCase();

        if (!conf.equals("y") && !conf.equals("yes")) {
            System.out.println("  Trade cancelled.");
            return;
        }

        if (pf.buyStock(s, qty)) {
            ok(String.format("Bought %d shares of %s @ $%.2f  |  Total: $%,.2f  |  Cash left: $%,.2f",
                    qty, sym, s.getCurrentPrice(), total, pf.getCashBalance()));
        } else {
            warn(String.format("Insufficient funds! Need $%,.2f, available $%,.2f",
                    total, pf.getCashBalance()));
        }
    }

    static void doSell() {
        header("SELL STOCK");
        Portfolio pf = currentUser.getPortfolio();

        if (pf.getHoldings().isEmpty()) {
            warn("You have no stock holdings to sell.");
            return;
        }

        showHoldingsCompact();

        System.out.print("\n  Stock symbol to sell (0=cancel): ");
        String sym = scanner.nextLine().trim().toUpperCase();
        if (sym.equals("0")) { System.out.println("  Cancelled."); return; }

        Stock s = market.getStock(sym);
        if (s == null) { warn("Stock '" + sym + "' not found."); return; }

        Holding h = pf.getHoldings().get(sym);
        if (h == null) { warn("You don't own any shares of " + sym + "."); return; }

        System.out.println();
        System.out.printf("  %-20s : %s%n",    "Stock",           s.getName());
        System.out.printf("  %-20s : %d%n",    "Shares Owned",    h.getQuantity());
        System.out.printf("  %-20s : $%,.2f%n","Avg Buy Price",   h.getAverageBuyPrice());
        System.out.printf("  %-20s : $%,.2f%n","Current Price",   s.getCurrentPrice());
        System.out.printf("  %-20s : %+,.2f (%+.2f%%)%n", "Unrealized P&L",
                h.getUnrealizedPnL(s.getCurrentPrice()),
                h.getUnrealizedPnLPct(s.getCurrentPrice()));
        System.out.println();

        int qty = intInput("  Quantity to sell (0=cancel, max=" + h.getQuantity() + "): ");
        if (qty <= 0)          { System.out.println("  Cancelled."); return; }
        if (qty > h.getQuantity()) { warn("You only own " + h.getQuantity() + " shares."); return; }

        double proceeds = qty * s.getCurrentPrice();
        double estPnL   = qty * (s.getCurrentPrice() - h.getAverageBuyPrice());

        System.out.printf("%n  >> Confirm SELL %d x %s @ $%.2f  =  $%,.2f  |  Est. P&L: %+,.2f ? [y/n]: ",
                qty, sym, s.getCurrentPrice(), proceeds, estPnL);
        String conf = scanner.nextLine().trim().toLowerCase();

        if (!conf.equals("y") && !conf.equals("yes")) {
            System.out.println("  Trade cancelled.");
            return;
        }

        if (pf.sellStock(s, qty)) {
            ok(String.format("Sold %d shares of %s @ $%.2f  |  Proceeds: $%,.2f  |  P&L: %+,.2f  |  Cash: $%,.2f",
                    qty, sym, s.getCurrentPrice(), proceeds, estPnL, pf.getCashBalance()));
        } else {
            warn("Sell failed. Please try again.");
        }
    }

    // ================================================================
    //  PORTFOLIO MENU
    // ================================================================
    static void portfolioMenu() {
        boolean loop = true;
        while (loop) {
            header("MY PORTFOLIO");
            System.out.println("  [1]  View Holdings");
            System.out.println("  [2]  Performance Summary");
            System.out.println("  [3]  Transaction History");
            System.out.println("  [0]  Back");
            System.out.println(LINE_THIN);

            switch (intInput("  Choice: ")) {
                case 1  -> showHoldings();
                case 2  -> showPerformance();
                case 3  -> showTransactions();
                case 0  -> loop = false;
                default -> warn("Invalid choice.");
            }
        }
    }

    static void showHoldings() {
        header("HOLDINGS");
        Portfolio pf = currentUser.getPortfolio();
        Map<String, Holding> h = pf.getHoldings();

        if (h.isEmpty()) {
            System.out.println("  No holdings yet. Go to Trade > Buy to get started!");
            return;
        }

        System.out.printf("  %-5s  %-22s  %5s  %9s  %9s  %11s  %10s  %7s%n",
                "Sym","Name","Qty","Avg Cost","Curr Px","Curr Value","Unrlzd P&L","P&L %");
        System.out.println(LINE_THIN);

        double totInvested = 0, totValue = 0, totPnL = 0;
        for (Holding holding : h.values()) {
            Stock s = market.getStock(holding.getSymbol());
            if (s == null) continue;
            double cv  = holding.getCurrentValue(s.getCurrentPrice());
            double pnl = holding.getUnrealizedPnL(s.getCurrentPrice());
            double pct = holding.getUnrealizedPnLPct(s.getCurrentPrice());
            totInvested += holding.getTotalCost();
            totValue    += cv;
            totPnL      += pnl;

            System.out.printf("  %-5s  %-22s  %5d  %9.2f  %9.2f  %11.2f  %+10.2f  %+6.2f%%%n",
                    holding.getSymbol(), truncate(s.getName(), 22),
                    holding.getQuantity(), holding.getAverageBuyPrice(),
                    s.getCurrentPrice(), cv, pnl, pct);
        }
        System.out.println(LINE_THIN);
        double overallPct = totInvested > 0 ? (totPnL / totInvested) * 100 : 0;
        System.out.printf("  %-28s  %9s  %9s  %11.2f  %+10.2f  %+6.2f%%%n",
                "STOCKS TOTAL","","", totValue, totPnL, overallPct);
        System.out.printf("  Cash Balance  : $%,.2f%n", pf.getCashBalance());
        System.out.printf("  TOTAL VALUE   : $%,.2f%n", pf.getTotalValue(market.getAllStocks()));
    }

    static void showHoldingsCompact() {
        Portfolio pf = currentUser.getPortfolio();
        Map<String, Holding> h = pf.getHoldings();
        if (h.isEmpty()) { System.out.println("  (No holdings)"); return; }

        System.out.println();
        System.out.printf("  %-5s  %5s  %9s  %9s  %10s%n",
                "Sym","Qty","Avg Cost","Curr Px","Unrlzd P&L");
        System.out.println("  " + "-".repeat(48));
        for (Holding holding : h.values()) {
            Stock s = market.getStock(holding.getSymbol());
            if (s == null) continue;
            System.out.printf("  %-5s  %5d  %9.2f  %9.2f  %+10.2f%n",
                    holding.getSymbol(), holding.getQuantity(),
                    holding.getAverageBuyPrice(), s.getCurrentPrice(),
                    holding.getUnrealizedPnL(s.getCurrentPrice()));
        }
    }

    static void showPerformance() {
        header("PERFORMANCE SUMMARY");
        Portfolio pf        = currentUser.getPortfolio();
        Map<String,Stock> m = market.getAllStocks();

        double totalVal   = pf.getTotalValue(m);
        double cash       = pf.getCashBalance();
        double stocksVal  = pf.getStocksValue(m);
        double invested   = pf.getTotalInvested();
        double unrealized = pf.getUnrealizedPnL(m);
        double realized   = pf.getRealizedPnL();
        double totalPnL   = pf.getTotalPnL(m);
        double initBal    = pf.getInitialBalance();
        double retPct     = pf.getTotalPnLPercent(m);

        System.out.println(LINE_THICK);
        System.out.printf("  %-30s : $%,.2f%n", "Starting Balance",    initBal);
        System.out.printf("  %-30s : $%,.2f%n", "Current Total Value", totalVal);
        System.out.println(LINE_THIN);
        System.out.printf("  %-30s : $%,.2f%n", "  Cash Balance",      cash);
        System.out.printf("  %-30s : $%,.2f%n", "  Stocks Value",      stocksVal);
        System.out.printf("  %-30s : $%,.2f%n", "  Total Invested",    invested);
        System.out.println(LINE_THIN);
        System.out.printf("  %-30s : %+,.2f%n", "Unrealized P&L",     unrealized);
        System.out.printf("  %-30s : %+,.2f%n", "Realized P&L",       realized);
        System.out.printf("  %-30s : %+,.2f%n", "Total P&L",          totalPnL);
        System.out.printf("  %-30s : %+.2f%%%n",  "Overall Return",     retPct);
        System.out.println(LINE_THICK);

        // Return bar
        int barFill = Math.min(40, (int)Math.abs(retPct));
        String bar  = (totalPnL >= 0 ? "+" : "-").repeat(barFill);
        System.out.printf("  Return  [%-40s]  %+.2f%%%n", bar, retPct);

        System.out.println();
        System.out.printf("  Holdings: %d stock(s)   |   Transactions: %d total%n",
                pf.getHoldings().size(), pf.getTransactions().size());
        System.out.printf("  Market updates: %d   |   News: %s%n",
                market.getUpdateCount(), truncate(market.getLastMarketNews(), 45));

        // Best / worst holding
        if (!pf.getHoldings().isEmpty()) {
            Holding best  = null, worst = null;
            double  bestP = Double.MIN_VALUE, worstP = Double.MAX_VALUE;
            for (Holding h : pf.getHoldings().values()) {
                Stock s = market.getStock(h.getSymbol());
                if (s == null) continue;
                double pct = h.getUnrealizedPnLPct(s.getCurrentPrice());
                if (pct > bestP)  { bestP  = pct;  best  = h; }
                if (pct < worstP) { worstP = pct;  worst = h; }
            }
            System.out.println();
            if (best  != null) System.out.printf("  Best Position  : %s  (%+.2f%%)%n",
                    best.getSymbol(),  bestP);
            if (worst != null && worst != best)
                System.out.printf("  Worst Position : %s  (%+.2f%%)%n",
                    worst.getSymbol(), worstP);
        }
    }

    static void showTransactions() {
        header("TRANSACTION HISTORY");
        List<Transaction> txns = currentUser.getPortfolio().getTransactions();

        if (txns.isEmpty()) {
            System.out.println("  No transactions yet.");
            return;
        }

        int show  = Math.min(30, txns.size());
        System.out.printf("  Showing last %d of %d transactions (most recent first):%n",
                show, txns.size());
        System.out.println(LINE_THIN);
        System.out.printf("  %-14s  %-4s  %-5s  %-22s  %5s  %9s  %12s%n",
                "Date/Time","Type","Sym","Stock Name","Qty","Price","Total");
        System.out.println(LINE_THIN);

        for (int i = txns.size() - 1; i >= txns.size() - show; i--) {
            System.out.println("  " + txns.get(i));
        }
        System.out.println(LINE_THIN);

        // Footer summary
        long   buyCnt  = txns.stream().filter(t -> t.getType() == Transaction.Type.BUY).count();
        long   sellCnt = txns.stream().filter(t -> t.getType() == Transaction.Type.SELL).count();
        double totBuy  = txns.stream().filter(t -> t.getType() == Transaction.Type.BUY)
                              .mapToDouble(Transaction::getTotalAmount).sum();
        double totSell = txns.stream().filter(t -> t.getType() == Transaction.Type.SELL)
                              .mapToDouble(Transaction::getTotalAmount).sum();

        System.out.printf("  Buys : %3d  ($%,.2f total)     Sells: %3d  ($%,.2f total)%n",
                buyCnt, totBuy, sellCnt, totSell);
        System.out.printf("  Realized P&L: %+,.2f%n",
                currentUser.getPortfolio().getRealizedPnL());
    }

    // ================================================================
    //  WATCHLIST MENU
    // ================================================================
    static void watchlistMenu() {
        boolean loop = true;
        while (loop) {
            header("WATCHLIST");
            System.out.println("  [1]  View Watchlist");
            System.out.println("  [2]  Add Stock to Watchlist");
            System.out.println("  [3]  Remove Stock from Watchlist");
            System.out.println("  [0]  Back");
            System.out.println(LINE_THIN);

            switch (intInput("  Choice: ")) {
                case 1  -> viewWatchlist();
                case 2  -> addToWatchlist();
                case 3  -> removeFromWatchlist();
                case 0  -> loop = false;
                default -> warn("Invalid choice.");
            }
        }
    }

    static void viewWatchlist() {
        List<String> wl = currentUser.getWatchlist();
        if (wl.isEmpty()) {
            System.out.println("  Watchlist is empty. Use [2] to add stocks.");
            return;
        }
        System.out.println();
        System.out.printf("  %-5s  %-26s  %8s  %9s  %7s  %5s%n",
                "Sym","Name","Price","Change","Chg %","Own?");
        System.out.println(LINE_THIN);
        for (String sym : wl) {
            Stock s = market.getStock(sym);
            if (s == null) { System.out.printf("  %-5s  [Data unavailable]%n", sym); continue; }
            int owned = currentUser.getPortfolio().getSharesOwned(sym);
            System.out.printf("  %-5s  %-26s  %8.2f  %+9.2f  %+6.2f%%  %5s%n",
                    s.getSymbol(), truncate(s.getName(), 26),
                    s.getCurrentPrice(), s.getChangeAmount(), s.getChangePercent(),
                    owned > 0 ? owned + "sh" : "No");
        }
        System.out.println(LINE_THIN);
        System.out.println("  " + wl.size() + " stock(s) on watchlist.");
    }

    static void addToWatchlist() {
        System.out.print("  Stock symbol to watch: ");
        String sym = scanner.nextLine().trim().toUpperCase();
        Stock s = market.getStock(sym);
        if (s == null) { warn("Stock '" + sym + "' not found."); return; }
        if (currentUser.addToWatchlist(sym))
            ok("Added " + sym + " — " + s.getName() + " to watchlist.");
        else
            System.out.println("  " + sym + " is already on your watchlist.");
    }

    static void removeFromWatchlist() {
        List<String> wl = currentUser.getWatchlist();
        if (wl.isEmpty()) { System.out.println("  Watchlist is empty."); return; }
        System.out.println("  Watching: " + String.join(", ", wl));
        System.out.print("  Symbol to remove: ");
        String sym = scanner.nextLine().trim().toUpperCase();
        if (currentUser.removeFromWatchlist(sym))
            ok("Removed " + sym + " from watchlist.");
        else
            warn(sym + " is not on your watchlist.");
    }

    // ================================================================
    //  REFRESH MARKET
    // ================================================================
    static void refreshMarket() {
        System.out.println("\n  Simulating market price update...");
        market.simulateUpdate();
        System.out.printf("  Update #%d complete.%n", market.getUpdateCount());
        System.out.println("  News: " + market.getLastMarketNews());
        System.out.println();

        List<Stock> gainers = market.getTopGainers(3);
        List<Stock> losers  = market.getTopLosers(3);

        System.out.print("  Top Gainers : ");
        gainers.forEach(s -> System.out.printf("%s(%+.1f%%)  ", s.getSymbol(), s.getChangePercent()));
        System.out.println();

        System.out.print("  Top Losers  : ");
        losers.forEach(s -> System.out.printf("%s(%+.1f%%)  ", s.getSymbol(), s.getChangePercent()));
        System.out.println();

        // Notify if any watchlist stocks moved significantly
        if (currentUser != null && !currentUser.getWatchlist().isEmpty()) {
            for (String sym : currentUser.getWatchlist()) {
                Stock s = market.getStock(sym);
                if (s != null && Math.abs(s.getChangePercent()) >= 3.0) {
                    System.out.printf("  [ALERT] %s moved %+.2f%%  ($%.2f)%n",
                            sym, s.getChangePercent(), s.getCurrentPrice());
                }
            }
        }
    }

    // ================================================================
    //  ASCII PRICE CHART
    // ================================================================
    static void printAsciiChart(List<Double> history, int width, int height) {
        int n = Math.min(width, history.size());
        List<Double> prices = new ArrayList<>(history.subList(history.size() - n, history.size()));

        double min   = prices.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max   = prices.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double range = max - min;
        if (range < 0.01) range = 0.01;

        char[][] grid = new char[height][n];
        for (char[] row : grid) Arrays.fill(row, '.');

        for (int i = 0; i < n; i++) {
            int row = (int)((max - prices.get(i)) / range * (height - 1));
            row = Math.max(0, Math.min(height - 1, row));
            grid[row][i] = 'o';
        }

        for (int r = 0; r < height; r++) {
            if (r == 0)
                System.out.printf("  $%9.2f |", max);
            else if (r == height - 1)
                System.out.printf("  $%9.2f |", min);
            else
                System.out.printf("  %11s |", "");
            for (char c : grid[r]) System.out.print(c);
            System.out.println();
        }
        System.out.println("  " + " ".repeat(12) + "+" + "-".repeat(n));
        System.out.println("  " + " ".repeat(13) + "Past" + " ".repeat(Math.max(1, n - 7)) + "Now");
    }

    // ================================================================
    //  UTILITY HELPERS
    // ================================================================
    static int intInput(String prompt) {
        while (true) {
            if (!prompt.isEmpty()) System.out.print(prompt);
            try   { return Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { warn("Enter a valid integer."); }
        }
    }

    static void header(String title) {
        String bar = "=".repeat(Math.max(0, 70 - title.length()) / 2);
        System.out.println();
        System.out.println("  " + bar + "[ " + title + " ]" + bar);
    }

    static void warn(String msg) { System.out.println("\n  [!] " + msg + "\n"); }
    static void ok  (String msg) { System.out.println("\n  [v] " + msg + "\n"); }

    static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max - 1) + ".";
    }

    static String fmtVol(long vol) {
        if (vol >= 1_000_000) return String.format("%.1fM", vol / 1_000_000.0);
        if (vol >= 1_000)     return String.format("%.1fK", vol / 1_000.0);
        return String.valueOf(vol);
    }

    // ================================================================
    //  BANNER & GOODBYE
    // ================================================================
    static void printBanner() {
        System.out.println();
        System.out.println("  +=======================================================+");
        System.out.println("  |                                                       |");
        System.out.println("  |          STOCK  TRADING  PLATFORM                    |");
        System.out.println("  |                                                       |");
        System.out.println("  |    20 Stocks | Buy/Sell | Portfolio | Watchlist       |");
        System.out.println("  |    OOP Design | File I/O Persistence                 |");
        System.out.println("  |                                                       |");
        System.out.println("  |    Starting Balance : $10,000.00                     |");
        System.out.println("  |    Tip: Use [5] to refresh market prices anytime.    |");
        System.out.println("  |                                                       |");
        System.out.println("  +=======================================================+");
        System.out.println();
    }

    static void printGoodbye() {
        System.out.println();
        System.out.println("  +=========================================+");
        System.out.println("  |   Thank you for using Stock Trader!    |");
        System.out.println("  |       Trade smart. Invest wisely.      |");
        System.out.println("  +=========================================+");
        System.out.println();
    }
}
