/**
 *  HotelApp.java  -  Hotel Reservation System
 *
 *  Compile :  javac HotelApp.java
 *  Run     :  java -cp . HotelApp
 *
 *  Admin password : admin123
 *  Data saved to  : ./hotel_data/hotel.dat  (auto on exit)
 */

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

public class HotelApp {

    // =========================================================
    // ENUMS
    // =========================================================

    enum RoomType {
        STANDARD("Standard",  80.0, "Queen Bed | WiFi | AC | TV"),
        DELUXE  ("Deluxe",   150.0, "King Bed | WiFi | AC | Smart TV | Mini-bar | Breakfast"),
        SUITE   ("Suite",    300.0, "2 Rooms | King Bed | Jacuzzi | WiFi | Smart TV | Full Board");

        final String label;
        final double rate;
        final String amenities;

        RoomType(String label, double rate, String amenities) {
            this.label    = label;
            this.rate     = rate;
            this.amenities = amenities;
        }
    }

    enum BookingStatus { CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED }

    enum PaymentMethod { CASH, CREDIT_CARD, DEBIT_CARD, UPI }

    // =========================================================
    // CLASS: Room
    // =========================================================
    static class Room implements Serializable {
        private static final long serialVersionUID = 101L;
        private final int      roomNo;
        private final int      floor;
        private final RoomType type;
        private final int      capacity;

        Room(int roomNo, RoomType type) {
            this.roomNo   = roomNo;
            this.floor    = roomNo / 100;
            this.type     = type;
            this.capacity = (type == RoomType.SUITE)   ? 4
                          : (type == RoomType.DELUXE)  ? 3 : 2;
        }

        int      getRoomNo()  { return roomNo; }
        int      getFloor()   { return floor; }
        RoomType getType()    { return type; }
        int      getCapacity(){ return capacity; }
        double   getRate()    { return type.rate; }
    }

    // =========================================================
    // CLASS: Guest
    // =========================================================
    static class Guest implements Serializable {
        private static final long serialVersionUID = 102L;
        private final String guestId;
        private final String name;
        private final String phone;
        private final String email;
        private final String password;

        Guest(String name, String phone, String email, String password) {
            this.guestId  = "G" + String.format("%04d", (new Random()).nextInt(9000) + 1000);
            this.name     = name;
            this.phone    = phone;
            this.email    = email;
            this.password = password;
        }

        String  getGuestId()            { return guestId; }
        String  getName()               { return name; }
        String  getPhone()              { return phone; }
        String  getEmail()              { return email; }
        boolean match(String pwd)       { return password.equals(pwd); }
    }

    // =========================================================
    // CLASS: Payment
    // =========================================================
    static class Payment implements Serializable {
        private static final long serialVersionUID = 103L;
        private final String        txnId;
        private final double        amount;
        private final PaymentMethod method;
        private final String        paidAt;
        private       boolean       success;

        Payment(double amount, PaymentMethod method) {
            this.txnId   = "TXN" + (System.currentTimeMillis() % 1_000_000L);
            this.amount  = amount;
            this.method  = method;
            this.paidAt  = LocalDateTime.now()
                           .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            this.success = false;
        }

        void    process()   { this.success = true; }
        String  getTxnId()  { return txnId; }
        double  getAmount() { return amount; }
        PaymentMethod getMethod() { return method; }
        String  getPaidAt() { return paidAt; }
        boolean isSuccess() { return success; }
    }

    // =========================================================
    // CLASS: Reservation
    // =========================================================
    static class Reservation implements Serializable {
        private static final long serialVersionUID = 104L;
        private final String        bookingId;
        private final String        guestId;
        private final String        guestName;
        private final int           roomNo;
        private final RoomType      roomType;
        private final LocalDate     checkIn;
        private final LocalDate     checkOut;
        private final int           numGuests;
        private final double        ratePerNight;
        private final String        bookedAt;
        private final String        specialReq;
        private       BookingStatus status;
        private       Payment       payment;

        Reservation(Guest g, Room r, LocalDate checkIn, LocalDate checkOut,
                    int numGuests, String specialReq) {
            this.bookingId   = "BK" + String.format("%06d",
                                (new Random()).nextInt(900_000) + 100_000);
            this.guestId     = g.getGuestId();
            this.guestName   = g.getName();
            this.roomNo      = r.getRoomNo();
            this.roomType    = r.getType();
            this.checkIn     = checkIn;
            this.checkOut    = checkOut;
            this.numGuests   = numGuests;
            this.ratePerNight = r.getRate();
            this.bookedAt    = LocalDateTime.now()
                               .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            this.specialReq  = specialReq;
            this.status      = BookingStatus.CONFIRMED;
        }

        // Computed
        long   nights()    { return ChronoUnit.DAYS.between(checkIn, checkOut); }
        double subtotal()  { return nights() * ratePerNight; }
        double tax()       { return subtotal() * 0.12; }
        double grandTotal(){ return subtotal() + tax(); }
        boolean isPaid()   { return payment != null && payment.isSuccess(); }

        boolean overlaps(LocalDate ci, LocalDate co) {
            return ci.isBefore(checkOut) && co.isAfter(checkIn);
        }

        // Getters
        String        getBookingId()    { return bookingId; }
        String        getGuestId()      { return guestId; }
        String        getGuestName()    { return guestName; }
        int           getRoomNo()       { return roomNo; }
        RoomType      getRoomType()     { return roomType; }
        LocalDate     getCheckIn()      { return checkIn; }
        LocalDate     getCheckOut()     { return checkOut; }
        int           getNumGuests()    { return numGuests; }
        double        getRatePerNight() { return ratePerNight; }
        String        getBookedAt()     { return bookedAt; }
        String        getSpecialReq()   { return specialReq; }
        BookingStatus getStatus()       { return status; }
        Payment       getPayment()      { return payment; }

        void setStatus (BookingStatus s) { status  = s; }
        void setPayment(Payment p)       { payment = p; }
    }

    // =========================================================
    // CLASS: Hotel  (all business logic)
    // =========================================================
    static class Hotel implements Serializable {
        private static final long serialVersionUID = 105L;

        private final Map<Integer, Room>       rooms        = new LinkedHashMap<>();
        private final Map<String, Reservation> reservations = new LinkedHashMap<>();
        private final Map<String, Guest>       guests       = new LinkedHashMap<>();
        private final Map<String, String>      emailIndex   = new LinkedHashMap<>();

        Hotel() {
            for (int n = 101; n <= 110; n++) rooms.put(n, new Room(n, RoomType.STANDARD));
            for (int n = 201; n <= 206; n++) rooms.put(n, new Room(n, RoomType.DELUXE));
            for (int n = 301; n <= 303; n++) rooms.put(n, new Room(n, RoomType.SUITE));
        }

        // ---- Guest ----
        boolean emailTaken(String email) {
            return emailIndex.containsKey(email.trim().toLowerCase());
        }

        Guest register(String name, String phone, String email, String pwd) {
            Guest g = new Guest(name, phone, email, pwd);
            guests.put(g.getGuestId(), g);
            emailIndex.put(email.trim().toLowerCase(), g.getGuestId());
            return g;
        }

        Guest login(String email, String pwd) {
            String id = emailIndex.get(email.trim().toLowerCase());
            if (id == null) return null;
            Guest g = guests.get(id);
            return (g != null && g.match(pwd)) ? g : null;
        }

        Guest           getGuest(String id)   { return guests.get(id); }
        Collection<Guest> allGuests()         { return guests.values(); }

        // ---- Availability ----
        boolean available(int roomNo, LocalDate ci, LocalDate co) {
            if (!rooms.containsKey(roomNo)) return false;
            for (Reservation r : reservations.values()) {
                if (r.getRoomNo() == roomNo
                 && r.getStatus() != BookingStatus.CANCELLED
                 && r.getStatus() != BookingStatus.CHECKED_OUT
                 && r.overlaps(ci, co)) {
                    return false;
                }
            }
            return true;
        }

        List<Room> search(RoomType type, LocalDate ci, LocalDate co, int pax) {
            List<Room> result = new ArrayList<>();
            for (Room r : rooms.values()) {
                if ((type == null || r.getType() == type)
                 && r.getCapacity() >= pax
                 && available(r.getRoomNo(), ci, co)) {
                    result.add(r);
                }
            }
            return result;
        }

        // ---- Booking ----
        Reservation book(Guest g, Room room, LocalDate ci, LocalDate co,
                         int pax, String req) {
            if (!available(room.getRoomNo(), ci, co)) return null;
            Reservation r = new Reservation(g, room, ci, co, pax, req);
            reservations.put(r.getBookingId(), r);
            return r;
        }

        String cancel(String bookingId, String guestId) {
            Reservation r = reservations.get(bookingId);
            if (r == null)                                    return "Booking not found.";
            if (!r.getGuestId().equals(guestId))              return "Booking does not belong to you.";
            if (r.getStatus() == BookingStatus.CHECKED_IN)    return "Cannot cancel — already checked in.";
            if (r.getStatus() == BookingStatus.CANCELLED)     return "Booking already cancelled.";
            if (r.getStatus() == BookingStatus.CHECKED_OUT)   return "Stay already completed.";
            r.setStatus(BookingStatus.CANCELLED);
            return "OK";
        }

        boolean checkIn(String bookingId) {
            Reservation r = reservations.get(bookingId);
            if (r == null || r.getStatus() != BookingStatus.CONFIRMED) return false;
            r.setStatus(BookingStatus.CHECKED_IN);
            return true;
        }

        boolean checkOut(String bookingId) {
            Reservation r = reservations.get(bookingId);
            if (r == null || r.getStatus() != BookingStatus.CHECKED_IN) return false;
            r.setStatus(BookingStatus.CHECKED_OUT);
            return true;
        }

        boolean pay(String bookingId, PaymentMethod method) {
            Reservation r = reservations.get(bookingId);
            if (r == null
             || r.getStatus() == BookingStatus.CANCELLED
             || r.isPaid()) return false;
            Payment p = new Payment(r.subtotal(), method);
            p.process();
            r.setPayment(p);
            return true;
        }

        // ---- Queries ----
        Reservation             getReservation(String id)    { return reservations.get(id); }
        Map<Integer, Room>      getRooms()                   { return rooms; }
        Collection<Reservation> allReservations()            { return reservations.values(); }

        List<Reservation> myBookings(String guestId) {
            List<Reservation> list = new ArrayList<>();
            for (Reservation r : reservations.values()) {
                if (r.getGuestId().equals(guestId)) list.add(r);
            }
            return list;
        }

        double totalRevenue() {
            double total = 0;
            for (Reservation r : reservations.values()) {
                if (r.isPaid()) total += r.subtotal();
            }
            return total;
        }
    }

    // =========================================================
    // CLASS: DataStore  (File I/O)
    // =========================================================
    static class DataStore {
        private static final String DIR  = "hotel_data";
        private static final String FILE = DIR + File.separator + "hotel.dat";

        static void save(Hotel h) {
            new File(DIR).mkdirs();
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(FILE)))) {
                out.writeObject(h);
            } catch (IOException e) {
                System.out.println("  [!] Save error: " + e.getMessage());
            }
        }

        static Hotel load() {
            File f = new File(FILE);
            if (!f.exists()) return new Hotel();
            try (ObjectInputStream in = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(f)))) {
                return (Hotel) in.readObject();
            } catch (Exception e) {
                System.out.println("  [!] Could not load data. Starting fresh.");
                return new Hotel();
            }
        }
    }

    // =========================================================
    // APPLICATION STATE
    // =========================================================
    static final Scanner          SC   = new Scanner(System.in);
    static final DateTimeFormatter DF  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static Hotel hotel;
    static Guest me;

    // =========================================================
    // ENTRY POINT
    // =========================================================
    public static void main(String[] args) {
        System.out.println("\n  Starting Hotel Reservation System...");
        hotel = DataStore.load();
        System.out.printf("  Rooms: %d  |  Guests: %d  |  Reservations: %d%n%n",
                hotel.getRooms().size(),
                hotel.allGuests().size(),
                hotel.allReservations().size());

        banner();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> DataStore.save(hotel)));

        boolean on = true;
        while (on) {
            on = mainMenu();
        }

        DataStore.save(hotel);
        goodbye();
    }

    // =========================================================
    // MAIN MENU
    // =========================================================
    static boolean mainMenu() {
        System.out.println();
        line('=', 56);
        System.out.println("  |        GRAND JAVA HOTEL  -  MAIN MENU              |");
        line('=', 56);
        System.out.println("  |  [1]  Guest Portal  (Book, Pay, Manage, Invoice)   |");
        System.out.println("  |  [2]  Admin Panel   (Rooms, Reports, Check-In/Out) |");
        System.out.println("  |  [0]  Exit                                          |");
        line('=', 56);

        int ch = readInt("  Choice: ");
        if (ch == 1) { guestPortal(); }
        else if (ch == 2) { adminPanel(); }
        else if (ch == 0) { return false; }
        else { err("Invalid choice."); }
        return true;
    }

    // =========================================================
    // GUEST PORTAL
    // =========================================================
    static void guestPortal() {
        boolean on = true;
        while (on) {
            System.out.println();
            line('=', 56);
            System.out.println("  |                 GUEST PORTAL                        |");
            if (me != null) {
                System.out.printf("  |  Signed in: %-38s |%n", clip(me.getName(), 38));
            }
            line('-', 56);
            System.out.println("  |  [1]  Register / Sign In                            |");
            System.out.println("  |  [2]  Search Available Rooms                         |");
            System.out.println("  |  [3]  Make a Reservation                             |");
            System.out.println("  |  [4]  View My Bookings                               |");
            System.out.println("  |  [5]  Cancel a Reservation                           |");
            System.out.println("  |  [6]  Make Payment                                   |");
            System.out.println("  |  [7]  View / Print Invoice                           |");
            System.out.println("  |  [8]  Sign Out                                       |");
            System.out.println("  |  [0]  Back to Main Menu                              |");
            line('=', 56);

            int ch = readInt("  Choice: ");
            if      (ch == 1) { authMenu(); }
            else if (ch == 2) { searchRooms(); }
            else if (ch == 3) { if (requireLogin()) makeReservation(); }
            else if (ch == 4) { if (requireLogin()) viewMyBookings(); }
            else if (ch == 5) { if (requireLogin()) cancelReservation(); }
            else if (ch == 6) { if (requireLogin()) makePayment(); }
            else if (ch == 7) { if (requireLogin()) viewInvoice(); }
            else if (ch == 8) { me = null; ok("Signed out."); }
            else if (ch == 0) { on = false; }
            else              { err("Invalid choice."); }
        }
    }

    // =========================================================
    // AUTH  (Register / Login)
    // =========================================================
    static void authMenu() {
        header("ACCOUNT ACCESS");
        System.out.println("  [1]  Sign In   (existing account)");
        System.out.println("  [2]  Register  (new guest)");
        System.out.println("  [0]  Back");
        line('-', 56);
        int ch = readInt("  Choice: ");
        if      (ch == 1) { doLogin(); }
        else if (ch == 2) { doRegister(); }
    }

    static void doLogin() {
        header("SIGN IN");
        System.out.print("  Email    : ");  String email = SC.nextLine().trim();
        System.out.print("  Password : ");  String pass  = SC.nextLine().trim();
        Guest g = hotel.login(email, pass);
        if (g == null) { err("Incorrect email or password."); return; }
        me = g;
        ok("Welcome back, " + g.getName() + "!  (ID: " + g.getGuestId() + ")");
    }

    static void doRegister() {
        header("NEW GUEST REGISTRATION");
        System.out.print("  Full Name    : ");  String name  = SC.nextLine().trim();
        if (name.isEmpty()) { err("Name cannot be empty."); return; }
        System.out.print("  Phone        : ");  String phone = SC.nextLine().trim();
        System.out.print("  Email        : ");  String email = SC.nextLine().trim();
        if (hotel.emailTaken(email)) { err("Email already registered. Please sign in."); return; }
        System.out.print("  Password     : ");  String pass  = SC.nextLine().trim();
        if (pass.length() < 4) { err("Password must be at least 4 characters."); return; }
        System.out.print("  Confirm Pass : ");  String conf  = SC.nextLine().trim();
        if (!pass.equals(conf)) { err("Passwords do not match."); return; }
        me = hotel.register(name, phone, email, pass);
        ok("Registered!  Guest ID: " + me.getGuestId() + "  Welcome, " + name + "!");
    }

    static boolean requireLogin() {
        if (me != null) return true;
        err("Please sign in first  (Option [1] from Guest Portal).");
        return false;
    }

    // =========================================================
    // SEARCH ROOMS
    // =========================================================
    static void searchRooms() {
        header("SEARCH AVAILABLE ROOMS");

        System.out.println("  Room Categories:");
        System.out.printf("  [1] %-8s  $%3.0f/night  |  %s%n",
                RoomType.STANDARD.label, RoomType.STANDARD.rate, RoomType.STANDARD.amenities);
        System.out.printf("  [2] %-8s  $%3.0f/night  |  %s%n",
                RoomType.DELUXE.label,   RoomType.DELUXE.rate,   RoomType.DELUXE.amenities);
        System.out.printf("  [3] %-8s  $%3.0f/night  |  %s%n",
                RoomType.SUITE.label,    RoomType.SUITE.rate,    RoomType.SUITE.amenities);
        System.out.println("  [4] Show All");
        int tc = readInt("  Select type: ");

        RoomType type = null;
        if      (tc == 1) type = RoomType.STANDARD;
        else if (tc == 2) type = RoomType.DELUXE;
        else if (tc == 3) type = RoomType.SUITE;

        LocalDate ci = readDate("  Check-in  (dd/MM/yyyy): ");
        if (ci == null) return;
        LocalDate co = readDate("  Check-out (dd/MM/yyyy): ");
        if (co == null) return;
        if (!co.isAfter(ci)) { err("Check-out must be after check-in."); return; }

        int pax = readInt("  Number of guests (1-4): ");
        if (pax < 1 || pax > 4) { err("Guests must be 1-4."); return; }

        long nights = ChronoUnit.DAYS.between(ci, co);
        List<Room> found = hotel.search(type, ci, co, pax);

        System.out.println();
        System.out.printf("  Results  |  %s  to  %s  |  %d night(s)  |  %d guest(s)%n",
                ci.format(DF), co.format(DF), nights, pax);
        line('-', 70);

        if (found.isEmpty()) {
            System.out.println("  No rooms found for your criteria. Try different dates or type.");
            return;
        }

        System.out.printf("  %-5s  %-6s  %-9s  %10s  %10s  %4s%n",
                "Room","Floor","Category","Rate/Night","Total","Cap");
        line('-', 70);
        for (Room r : found) {
            System.out.printf("  %-5d  %-6d  %-9s  $%9.0f  $%9.0f  %4d%n",
                    r.getRoomNo(), r.getFloor(), r.getType().label,
                    r.getRate(), r.getRate() * nights, r.getCapacity());
        }
        line('-', 70);
        System.out.println("  " + found.size() + " room(s) available.");
    }

    // =========================================================
    // MAKE RESERVATION
    // =========================================================
    static void makeReservation() {
        header("MAKE A RESERVATION");

        // Step 1 — Room type
        System.out.println("  STEP 1/4  —  Room Category");
        line('-', 56);
        System.out.printf("  [1] %-8s  $%.0f/night%n", RoomType.STANDARD.label, RoomType.STANDARD.rate);
        System.out.printf("  [2] %-8s  $%.0f/night%n", RoomType.DELUXE.label,   RoomType.DELUXE.rate);
        System.out.printf("  [3] %-8s  $%.0f/night%n", RoomType.SUITE.label,    RoomType.SUITE.rate);
        int tc = readInt("  Choose (0=cancel): ");
        if (tc == 0) return;
        RoomType type;
        if      (tc == 2) type = RoomType.DELUXE;
        else if (tc == 3) type = RoomType.SUITE;
        else              type = RoomType.STANDARD;
        System.out.println("  Amenities: " + type.amenities);

        // Step 2 — Dates
        System.out.println("\n  STEP 2/4  —  Stay Dates");
        line('-', 56);
        LocalDate ci = readDate("  Check-in  (dd/MM/yyyy): ");
        if (ci == null) return;
        LocalDate co = readDate("  Check-out (dd/MM/yyyy): ");
        if (co == null) return;
        if (!co.isAfter(ci)) { err("Check-out must be after check-in."); return; }

        // Step 3 — Guests & requests
        System.out.println("\n  STEP 3/4  —  Guest Details");
        line('-', 56);
        int pax = readInt("  Number of guests (1-4): ");
        if (pax < 1 || pax > 4) { err("Must be 1-4 guests."); return; }
        System.out.print("  Special requests (Enter to skip): ");
        String req = SC.nextLine().trim();

        // Find first available room of that type
        List<Room> avail = hotel.search(type, ci, co, pax);
        if (avail.isEmpty()) {
            err("No " + type.label + " rooms available for those dates.");
            return;
        }
        Room room   = avail.get(0);
        long nights = ChronoUnit.DAYS.between(ci, co);
        double sub  = nights * room.getRate();
        double tax  = sub * 0.12;
        double tot  = sub + tax;

        // Step 4 — Confirm
        System.out.println("\n  STEP 4/4  —  Confirm Booking");
        line('=', 56);
        System.out.printf("  %-22s : %-28s%n", "Guest",        me.getName());
        System.out.printf("  %-22s : %-28s%n", "Room",         room.getRoomNo() + "  (Floor " + room.getFloor() + ")");
        System.out.printf("  %-22s : %-28s%n", "Category",     type.label);
        System.out.printf("  %-22s : %-28s%n", "Check-in",     ci.format(DF));
        System.out.printf("  %-22s : %-28s%n", "Check-out",    co.format(DF));
        System.out.printf("  %-22s : %d night(s)%n", "Duration", nights);
        System.out.printf("  %-22s : %d%n", "Guests",          pax);
        System.out.printf("  %-22s : $%.2f%n", "Rate / Night", room.getRate());
        line('-', 56);
        System.out.printf("  %-22s : $%.2f%n", "Subtotal",     sub);
        System.out.printf("  %-22s : $%.2f%n", "Tax (12%%)",    tax);
        System.out.printf("  %-22s : $%.2f%n", "GRAND TOTAL",  tot);
        if (!req.isEmpty())
            System.out.printf("  %-22s : %s%n", "Special Requests", req);
        line('=', 56);

        System.out.print("  Confirm booking? [y/n]: ");
        if (!SC.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("  Booking cancelled.");
            return;
        }

        Reservation res = hotel.book(me, room, ci, co, pax, req);
        if (res == null) { err("Booking failed — room no longer available."); return; }

        ok("Booking CONFIRMED!\n"
         + "  Booking ID : " + res.getBookingId() + "\n"
         + "  Room       : " + room.getRoomNo() + " (" + type.label + ")\n"
         + "  Amount Due : $" + String.format("%.2f", tot) + "\n"
         + "  Use option [6] to complete payment.");
    }

    // =========================================================
    // VIEW MY BOOKINGS
    // =========================================================
    static void viewMyBookings() {
        header("MY BOOKINGS  —  " + me.getName());
        List<Reservation> list = hotel.myBookings(me.getGuestId());
        if (list.isEmpty()) { System.out.println("  No bookings on file."); return; }

        line('-', 80);
        System.out.printf("  %-10s  %-5s  %-9s  %-11s  %-11s  %7s  %10s  %-11s  %-5s%n",
                "Booking","Room","Category","Check-in","Check-out",
                "Nights","Total","Status","Paid?");
        line('-', 80);
        for (Reservation r : list) {
            System.out.printf("  %-10s  %-5d  %-9s  %-11s  %-11s  %7d  $%9.2f  %-11s  %-5s%n",
                    r.getBookingId(), r.getRoomNo(), r.getRoomType().label,
                    r.getCheckIn().format(DF), r.getCheckOut().format(DF),
                    r.nights(), r.grandTotal(), r.getStatus(),
                    r.isPaid() ? "YES" : "NO");
        }
        line('-', 80);
        System.out.println("  Total: " + list.size() + " booking(s).");
    }

    // =========================================================
    // CANCEL RESERVATION
    // =========================================================
    static void cancelReservation() {
        header("CANCEL RESERVATION");
        viewMyBookings();

        System.out.print("\n  Booking ID to cancel (0 to go back): ");
        String bid = SC.nextLine().trim().toUpperCase();
        if (bid.equals("0")) return;

        Reservation r = hotel.getReservation(bid);
        if (r == null) { err("Booking not found."); return; }
        if (!r.getGuestId().equals(me.getGuestId()))
            { err("That booking does not belong to you."); return; }
        if (r.getStatus() != BookingStatus.CONFIRMED)
            { err("Cannot cancel — status is: " + r.getStatus()); return; }

        // Cancellation refund policy
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), r.getCheckIn());
        double refund = 0;
        String policy;
        if (r.isPaid()) {
            if (daysLeft >= 7) {
                refund = r.subtotal();
                policy = "Full refund (7+ days notice)";
            } else if (daysLeft >= 3) {
                refund = r.subtotal() * 0.50;
                policy = "50% refund (3-6 days notice)";
            } else {
                refund = 0;
                policy = "No refund (less than 3 days notice)";
            }
        } else {
            policy = "No payment made — no charge";
        }

        System.out.println();
        System.out.printf("  Room %d | %s to %s | Total: $%.2f%n",
                r.getRoomNo(), r.getCheckIn().format(DF),
                r.getCheckOut().format(DF), r.grandTotal());
        System.out.printf("  Refund Policy : %s%n", policy);
        if (r.isPaid() && refund > 0)
            System.out.printf("  Refund Amount : $%.2f%n", refund);

        System.out.print("  Confirm cancellation? [y/n]: ");
        if (!SC.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("  Cancellation aborted.");
            return;
        }

        String result = hotel.cancel(bid, me.getGuestId());
        if (result.equals("OK")) {
            String msg = "Reservation " + bid + " cancelled.";
            if (r.isPaid() && refund > 0)
                msg += " Refund of $" + String.format("%.2f", refund) + " will be processed.";
            ok(msg);
        } else {
            err(result);
        }
    }

    // =========================================================
    // MAKE PAYMENT
    // =========================================================
    static void makePayment() {
        header("PAYMENT");

        // Show unpaid bookings
        List<Reservation> unpaid = new ArrayList<>();
        for (Reservation r : hotel.myBookings(me.getGuestId())) {
            if (!r.isPaid() && r.getStatus() != BookingStatus.CANCELLED) {
                unpaid.add(r);
            }
        }

        if (unpaid.isEmpty()) { System.out.println("  No unpaid reservations."); return; }

        System.out.println("  Unpaid Reservations:");
        line('-', 72);
        for (Reservation r : unpaid) {
            System.out.printf("  %-10s  Room %-3d  %-9s  %s - %s  Total: $%.2f%n",
                    r.getBookingId(), r.getRoomNo(), r.getRoomType().label,
                    r.getCheckIn().format(DF), r.getCheckOut().format(DF),
                    r.grandTotal());
        }
        line('-', 72);

        System.out.print("  Enter Booking ID to pay (0 to cancel): ");
        String bid = SC.nextLine().trim().toUpperCase();
        if (bid.equals("0")) return;

        Reservation r = hotel.getReservation(bid);
        if (r == null || !r.getGuestId().equals(me.getGuestId()))
            { err("Booking not found."); return; }
        if (r.isPaid())
            { err("This booking is already paid."); return; }
        if (r.getStatus() == BookingStatus.CANCELLED)
            { err("Cannot pay for a cancelled booking."); return; }

        System.out.println();
        line('-', 56);
        System.out.printf("  Booking   : %s%n", r.getBookingId());
        System.out.printf("  Room      : %d (%s)%n", r.getRoomNo(), r.getRoomType().label);
        System.out.printf("  Stay      : %s  to  %s  (%d nights)%n",
                r.getCheckIn().format(DF), r.getCheckOut().format(DF), r.nights());
        System.out.printf("  Subtotal  : $%.2f%n", r.subtotal());
        System.out.printf("  Tax (12%%) : $%.2f%n", r.tax());
        System.out.printf("  TOTAL DUE : $%.2f%n", r.grandTotal());
        line('-', 56);

        System.out.println("  Payment Method:");
        System.out.println("  [1] Cash");
        System.out.println("  [2] Credit Card");
        System.out.println("  [3] Debit Card");
        System.out.println("  [4] UPI / Mobile Pay");
        int pm = readInt("  Select: ");

        PaymentMethod method;
        if      (pm == 2) method = PaymentMethod.CREDIT_CARD;
        else if (pm == 3) method = PaymentMethod.DEBIT_CARD;
        else if (pm == 4) method = PaymentMethod.UPI;
        else              method = PaymentMethod.CASH;

        System.out.print("  Processing payment");
        for (int i = 0; i < 5; i++) {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            System.out.print(".");
        }
        System.out.println();

        if (hotel.pay(bid, method)) {
            Payment p = r.getPayment();
            ok("Payment Successful!");
            line('=', 56);
            System.out.printf("  Transaction ID  : %s%n", p.getTxnId());
            System.out.printf("  Amount Charged  : $%.2f%n", r.grandTotal());
            System.out.printf("  Method          : %s%n", method.name().replace('_', ' '));
            System.out.printf("  Date & Time     : %s%n", p.getPaidAt());
            System.out.printf("  Booking Status  : %s%n", r.getStatus());
            line('=', 56);
            System.out.println("  Tip: Use option [7] to view your full invoice.");
        } else {
            err("Payment failed. Please try again.");
        }
    }

    // =========================================================
    // VIEW INVOICE
    // =========================================================
    static void viewInvoice() {
        header("VIEW INVOICE");
        System.out.print("  Enter Booking ID (or 0 to list yours): ");
        String bid = SC.nextLine().trim().toUpperCase();

        if (bid.equals("0")) {
            viewMyBookings();
            System.out.print("  Enter Booking ID: ");
            bid = SC.nextLine().trim().toUpperCase();
        }

        Reservation r = hotel.getReservation(bid);
        if (r == null || !r.getGuestId().equals(me.getGuestId()))
            { err("Booking not found or not yours."); return; }

        Room room = hotel.getRooms().get(r.getRoomNo());
        printInvoice(r, room, me);
    }

    // =========================================================
    // INVOICE PRINTER  (used by guest & admin)
    // =========================================================
    static void printInvoice(Reservation r, Room room, Guest g) {
        System.out.println();
        line('=', 58);
        System.out.printf("  |%20s%-36s |%n", "", "GRAND JAVA HOTEL");
        System.out.printf("  |%20s%-36s |%n", "", "TAX INVOICE / RECEIPT");
        line('=', 58);
        System.out.printf("  | %-22s : %-29s |%n", "Booking Reference", r.getBookingId());
        System.out.printf("  | %-22s : %-29s |%n", "Booked On",         r.getBookedAt());
        System.out.printf("  | %-22s : %-29s |%n", "Status",            r.getStatus());
        line('-', 58);
        System.out.printf("  | %-52s |%n", "GUEST INFORMATION");
        line('-', 58);
        System.out.printf("  | %-22s : %-29s |%n", "Name",     g.getName());
        System.out.printf("  | %-22s : %-29s |%n", "Guest ID", g.getGuestId());
        System.out.printf("  | %-22s : %-29s |%n", "Phone",    g.getPhone());
        System.out.printf("  | %-22s : %-29s |%n", "Email",    g.getEmail());
        line('-', 58);
        System.out.printf("  | %-52s |%n", "ROOM DETAILS");
        line('-', 58);
        System.out.printf("  | %-22s : %-29s |%n", "Room Number",  r.getRoomNo());
        System.out.printf("  | %-22s : %-29s |%n", "Category",     r.getRoomType().label);
        System.out.printf("  | %-22s : %-29s |%n", "Floor",
                (room != null ? String.valueOf(room.getFloor()) : "N/A"));
        System.out.printf("  | %-22s : %-29s |%n", "Amenities",
                clip(r.getRoomType().amenities, 29));
        System.out.printf("  | %-22s : %-29s |%n", "Check-in",     r.getCheckIn().format(DF));
        System.out.printf("  | %-22s : %-29s |%n", "Check-out",    r.getCheckOut().format(DF));
        System.out.printf("  | %-22s : %-29s |%n", "Duration",     r.nights() + " night(s)");
        System.out.printf("  | %-22s : %-29s |%n", "Guests",       r.getNumGuests());
        if (r.getSpecialReq() != null && !r.getSpecialReq().isEmpty())
            System.out.printf("  | %-22s : %-29s |%n", "Special Requests",
                    clip(r.getSpecialReq(), 29));
        line('-', 58);
        System.out.printf("  | %-52s |%n", "CHARGES");
        line('-', 58);
        System.out.printf("  | %-22s : $%-28.2f |%n", "Rate / Night",   r.getRatePerNight());
        System.out.printf("  | %-22s : %-29s |%n",    "  x Nights",     r.nights());
        System.out.printf("  | %-22s : $%-28.2f |%n", "Room Charges",   r.subtotal());
        System.out.printf("  | %-22s : $%-28.2f |%n", "Tax (12%%)",      r.tax());
        line('-', 58);
        System.out.printf("  | %-22s : $%-28.2f |%n", "GRAND TOTAL",    r.grandTotal());
        line('-', 58);
        if (r.isPaid()) {
            System.out.printf("  | %-52s |%n", "PAYMENT DETAILS");
            line('-', 58);
            System.out.printf("  | %-22s : %-29s |%n", "Transaction ID",
                    r.getPayment().getTxnId());
            System.out.printf("  | %-22s : %-29s |%n", "Method",
                    r.getPayment().getMethod().name().replace('_', ' '));
            System.out.printf("  | %-22s : $%-28.2f |%n", "Amount Paid",   r.grandTotal());
            System.out.printf("  | %-22s : %-29s |%n", "Paid On",
                    r.getPayment().getPaidAt());
            line('=', 58);
            System.out.printf("  |%15s%-41s |%n", "", "**  PAYMENT CONFIRMED — THANK YOU!  **");
        } else {
            line('=', 58);
            System.out.printf("  |%15s%-41s |%n", "", "**  PAYMENT PENDING  **");
        }
        line('=', 58);
    }

    // =========================================================
    // ADMIN PANEL
    // =========================================================
    static void adminPanel() {
        System.out.print("\n  Admin Password: ");
        if (!SC.nextLine().trim().equals("admin123")) {
            err("Access denied.");
            return;
        }

        boolean on = true;
        while (on) {
            header("ADMIN PANEL");
            System.out.println("  [1]  Room Availability Board");
            System.out.println("  [2]  All Reservations");
            System.out.println("  [3]  Check-in Guest");
            System.out.println("  [4]  Check-out Guest & Print Invoice");
            System.out.println("  [5]  Occupancy Statistics");
            System.out.println("  [6]  Revenue Report");
            System.out.println("  [7]  All Registered Guests");
            System.out.println("  [0]  Back");
            line('-', 56);

            int ch = readInt("  Choice: ");
            if      (ch == 1) { adminRooms(); }
            else if (ch == 2) { adminReservations(); }
            else if (ch == 3) { adminCheckIn(); }
            else if (ch == 4) { adminCheckOut(); }
            else if (ch == 5) { adminStats(); }
            else if (ch == 6) { adminRevenue(); }
            else if (ch == 7) { adminGuests(); }
            else if (ch == 0) { on = false; }
            else              { err("Invalid choice."); }
        }
    }

    static void adminRooms() {
        header("ROOM AVAILABILITY BOARD");
        LocalDate today = LocalDate.now();
        LocalDate tmrw  = today.plusDays(1);

        System.out.printf("  %-5s  %-6s  %-9s  %10s  %4s  %-14s%n",
                "Room","Floor","Category","Rate/Night","Cap","Today");
        line('-', 60);

        int free = 0;
        for (Room r : hotel.getRooms().values()) {
            boolean avail = hotel.available(r.getRoomNo(), today, tmrw);
            if (avail) free++;
            System.out.printf("  %-5d  %-6d  %-9s  $%9.0f  %-4d  %s%n",
                    r.getRoomNo(), r.getFloor(), r.getType().label,
                    r.getRate(), r.getCapacity(),
                    avail ? "[ AVAILABLE ]" : "[ OCCUPIED  ]");
        }
        line('-', 60);
        System.out.printf("  Total: %d  |  Free today: %d  |  Occupied: %d%n",
                hotel.getRooms().size(), free, hotel.getRooms().size() - free);
    }

    static void adminReservations() {
        header("ALL RESERVATIONS");
        System.out.println("  Filter: [1]All  [2]Confirmed  [3]Checked-in  [4]Checked-out  [5]Cancelled");
        int f = readInt("  Filter: ");

        BookingStatus filter = null;
        if      (f == 2) filter = BookingStatus.CONFIRMED;
        else if (f == 3) filter = BookingStatus.CHECKED_IN;
        else if (f == 4) filter = BookingStatus.CHECKED_OUT;
        else if (f == 5) filter = BookingStatus.CANCELLED;

        line('-', 84);
        System.out.printf("  %-10s  %-18s  %-5s  %-9s  %-11s  %-11s  %10s  %-11s  %-5s%n",
                "Booking","Guest","Room","Category","Check-in","Check-out",
                "Total","Status","Paid");
        line('-', 84);

        int count = 0;
        for (Reservation r : hotel.allReservations()) {
            if (filter != null && r.getStatus() != filter) continue;
            System.out.printf("  %-10s  %-18s  %-5d  %-9s  %-11s  %-11s  $%9.2f  %-11s  %-5s%n",
                    r.getBookingId(), clip(r.getGuestName(), 18), r.getRoomNo(),
                    r.getRoomType().label, r.getCheckIn().format(DF),
                    r.getCheckOut().format(DF), r.grandTotal(),
                    r.getStatus(), r.isPaid() ? "YES" : "NO");
            count++;
        }
        line('-', 84);
        System.out.println("  Showing " + count + " reservation(s).");
    }

    static void adminCheckIn() {
        header("GUEST CHECK-IN");
        System.out.print("  Booking ID: ");
        String bid = SC.nextLine().trim().toUpperCase();
        Reservation r = hotel.getReservation(bid);
        if (r == null) { err("Booking not found."); return; }

        System.out.printf("  Guest   : %s%n",  r.getGuestName());
        System.out.printf("  Room    : %d (%s)%n", r.getRoomNo(), r.getRoomType().label);
        System.out.printf("  Dates   : %s  to  %s%n",
                r.getCheckIn().format(DF), r.getCheckOut().format(DF));
        System.out.printf("  Status  : %s%n", r.getStatus());
        System.out.printf("  Paid    : %s%n", r.isPaid() ? "YES" : "NO — payment outstanding");

        if (r.getStatus() != BookingStatus.CONFIRMED)
            { err("Cannot check in — status: " + r.getStatus()); return; }

        System.out.print("  Confirm check-in? [y/n]: ");
        if (!SC.nextLine().trim().equalsIgnoreCase("y")) return;

        if (hotel.checkIn(bid)) {
            ok(r.getGuestName() + " successfully checked into Room " + r.getRoomNo() + ".");
        } else {
            err("Check-in failed.");
        }
    }

    static void adminCheckOut() {
        header("GUEST CHECK-OUT");
        System.out.print("  Booking ID: ");
        String bid = SC.nextLine().trim().toUpperCase();
        Reservation r = hotel.getReservation(bid);
        if (r == null) { err("Booking not found."); return; }
        if (r.getStatus() != BookingStatus.CHECKED_IN)
            { err("Guest is not currently checked in."); return; }

        Guest g    = hotel.getGuest(r.getGuestId());
        Room  room = hotel.getRooms().get(r.getRoomNo());
        if (g == null) g = new Guest(r.getGuestName(), "", r.getGuestId() + "@hotel", "x");

        printInvoice(r, room, g);

        System.out.print("  Confirm check-out? [y/n]: ");
        if (!SC.nextLine().trim().equalsIgnoreCase("y")) return;

        if (hotel.checkOut(bid)) {
            ok(r.getGuestName() + " checked out of Room " + r.getRoomNo() + ". Invoice printed above.");
        } else {
            err("Check-out failed.");
        }
    }

    static void adminStats() {
        header("OCCUPANCY STATISTICS");
        LocalDate today = LocalDate.now();
        LocalDate tmrw  = today.plusDays(1);

        for (RoomType t : RoomType.values()) {
            long total = 0, free = 0;
            for (Room r : hotel.getRooms().values()) {
                if (r.getType() == t) {
                    total++;
                    if (hotel.available(r.getRoomNo(), today, tmrw)) free++;
                }
            }
            long occupied = total - free;
            double pct    = (total > 0) ? (occupied * 100.0 / total) : 0;
            String bar    = repeat("|", (int)(pct / 5)) + repeat(".", 20 - (int)(pct / 5));
            System.out.printf("  %-9s  Total: %2d  Occupied: %2d  Free: %2d  [%s]  %5.1f%%%n",
                    t.label, total, occupied, free, bar, pct);
        }

        line('-', 56);
        long checkedIn  = 0, confirmed = 0, cancelled = 0, completed = 0;
        for (Reservation r : hotel.allReservations()) {
            if (r.getStatus() == BookingStatus.CHECKED_IN)   checkedIn++;
            if (r.getStatus() == BookingStatus.CONFIRMED)    confirmed++;
            if (r.getStatus() == BookingStatus.CANCELLED)    cancelled++;
            if (r.getStatus() == BookingStatus.CHECKED_OUT)  completed++;
        }
        System.out.printf("  Guests In-House  : %d%n",  checkedIn);
        System.out.printf("  Confirmed (due)  : %d%n",  confirmed);
        System.out.printf("  Completed Stays  : %d%n",  completed);
        System.out.printf("  Cancelled        : %d%n",  cancelled);
        System.out.printf("  Registered Guests: %d%n",  hotel.allGuests().size());
    }

    static void adminRevenue() {
        header("REVENUE REPORT");
        double sub   = hotel.totalRevenue();
        double tax   = sub * 0.12;
        double gross = sub + tax;

        line('=', 56);
        System.out.printf("  %-28s : $%,.2f%n", "Room Revenue (ex-tax)",  sub);
        System.out.printf("  %-28s : $%,.2f%n", "Tax Collected (12%%)",   tax);
        System.out.printf("  %-28s : $%,.2f%n", "GROSS REVENUE",          gross);
        line('-', 56);
        System.out.println("  By Category:");
        for (RoomType t : RoomType.values()) {
            double rev  = 0;
            long   bkgs = 0;
            for (Reservation r : hotel.allReservations()) {
                if (r.getRoomType() == t && r.getStatus() != BookingStatus.CANCELLED) bkgs++;
                if (r.getRoomType() == t && r.isPaid()) rev += r.subtotal();
            }
            System.out.printf("  %-9s  Bookings: %-4d  Revenue: $%,.2f%n",
                    t.label, bkgs, rev);
        }
        line('-', 56);
        long paid  = 0, total = 0;
        for (Reservation r : hotel.allReservations()) {
            if (r.getStatus() != BookingStatus.CANCELLED) total++;
            if (r.isPaid()) paid++;
        }
        System.out.printf("  Paid: %d / %d bookings%n", paid, total);
        System.out.printf("  Avg Revenue / Booking : $%,.2f%n",
                (paid > 0 ? sub / paid : 0));
        line('=', 56);
    }

    static void adminGuests() {
        header("ALL REGISTERED GUESTS");
        if (hotel.allGuests().isEmpty()) { System.out.println("  No guests registered."); return; }

        line('-', 72);
        System.out.printf("  %-8s  %-22s  %-14s  %-22s%n","ID","Name","Phone","Email");
        line('-', 72);
        for (Guest g : hotel.allGuests()) {
            long stays = 0;
            for (Reservation r : hotel.myBookings(g.getGuestId())) {
                if (r.getStatus() != BookingStatus.CANCELLED) stays++;
            }
            System.out.printf("  %-8s  %-22s  %-14s  %-22s  (%d stay(s))%n",
                    g.getGuestId(), clip(g.getName(), 22),
                    g.getPhone(), clip(g.getEmail(), 22), stays);
        }
        line('-', 72);
        System.out.println("  Total: " + hotel.allGuests().size() + " guest(s).");
    }

    // =========================================================
    // UTILITIES
    // =========================================================
    static int readInt(String prompt) {
        while (true) {
            if (!prompt.isEmpty()) System.out.print(prompt);
            try {
                return Integer.parseInt(SC.nextLine().trim());
            } catch (NumberFormatException e) {
                err("Please enter a valid number.");
            }
        }
    }

    static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = SC.nextLine().trim();
            if (s.equals("0")) return null;
            try {
                return LocalDate.parse(s, DF);
            } catch (DateTimeParseException e) {
                err("Invalid date. Format: dd/MM/yyyy  e.g. 25/12/2025   (0 to cancel)");
            }
        }
    }

    static String clip(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max - 2) + "..";
    }

    static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    static void header(String t) {
        int pad = Math.max(2, (54 - t.length()) / 2);
        System.out.println("\n  " + repeat("=", pad) + "[ " + t + " ]" + repeat("=", pad));
    }

    static void line(char c, int n) {
        System.out.println("  " + repeat(String.valueOf(c), n));
    }

    static void err(String m) { System.out.println("\n  [!] " + m + "\n"); }
    static void ok (String m) { System.out.println("\n  [OK] " + m + "\n"); }

    static void banner() {
        System.out.println("  +============================================================+");
        System.out.println("  |                                                            |");
        System.out.println("  |               GRAND  JAVA  HOTEL                          |");
        System.out.println("  |           Hotel Reservation System  v2.0                  |");
        System.out.println("  |                                                            |");
        System.out.println("  |  19 Rooms  :  10 Standard | 6 Deluxe | 3 Suite            |");
        System.out.println("  |  Pricing   :  $80 | $150 | $300  per night  (+12% tax)   |");
        System.out.println("  |  Data File :  ./hotel_data/hotel.dat  (auto-saved)        |");
        System.out.println("  |  Admin     :  Password = admin123                         |");
        System.out.println("  |                                                            |");
        System.out.println("  +============================================================+");
    }

    static void goodbye() {
        System.out.println("  +=====================================================+");
        System.out.println("  |   Thank you for using Grand Java Hotel System!     |");
        System.out.println("  |        We hope to see you again soon.              |");
        System.out.println("  +=====================================================+");
        System.out.println();
    }

} // end class HotelApp