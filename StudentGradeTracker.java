import java.util.*;

public class StudentGradeTracker {

    static Scanner scanner = new Scanner(System.in);
    static ArrayList<Student> students = new ArrayList<>();
    static int rollCounter = 1001;

    // ╔══════════════════════════════════════════╗
    // ║           STUDENT INNER CLASS            ║
    // ╚══════════════════════════════════════════╝
    static class Student {
        String name;
        int rollNumber;
        ArrayList<Double> grades;
        ArrayList<String> subjects;

        Student(String name, int rollNumber) {
            this.name    = name;
            this.rollNumber = rollNumber;
            this.grades   = new ArrayList<>();
            this.subjects  = new ArrayList<>();
        }

        void addGrade(String subject, double grade) {
            subjects.add(subject);
            grades.add(grade);
        }

        double getAverage() {
            if (grades.isEmpty()) return 0.0;
            double sum = 0;
            for (double g : grades) sum += g;
            return sum / grades.size();
        }

        double getHighest() {
            if (grades.isEmpty()) return 0.0;
            double max = grades.get(0);
            for (double g : grades) if (g > max) max = g;
            return max;
        }

        double getLowest() {
            if (grades.isEmpty()) return 0.0;
            double min = grades.get(0);
            for (double g : grades) if (g < min) min = g;
            return min;
        }

        String getHighestSubject() {
            if (grades.isEmpty()) return "N/A";
            int idx = 0;
            for (int i = 1; i < grades.size(); i++)
                if (grades.get(i) > grades.get(idx)) idx = i;
            return subjects.get(idx);
        }

        String getLowestSubject() {
            if (grades.isEmpty()) return "N/A";
            int idx = 0;
            for (int i = 1; i < grades.size(); i++)
                if (grades.get(i) < grades.get(idx)) idx = i;
            return subjects.get(idx);
        }

        String getLetterGrade() {
            return letterGradeFor(getAverage());
        }

        String getStatus() {
            return getAverage() >= 40.0 ? "PASS" : "FAIL";
        }

        String getRemark() {
            double avg = getAverage();
            if (avg >= 90) return "Outstanding";
            if (avg >= 75) return "Excellent";
            if (avg >= 60) return "Good";
            if (avg >= 50) return "Average";
            if (avg >= 40) return "Below Average";
            return "Poor";
        }
    }

    // ╔══════════════════════════════════════════╗
    // ║                  MAIN                    ║
    // ╚══════════════════════════════════════════╝
    public static void main(String[] args) {
        printBanner();
        boolean running = true;
        while (running) {
            showMainMenu();
            int choice = getIntInput("  Enter your choice: ");
            System.out.println();
            switch (choice) {
                case 1 -> addStudent();
                case 2 -> addGrade();
                case 3 -> viewAllStudents();
                case 4 -> viewStudentReport();
                case 5 -> displaySummaryReport();
                case 6 -> searchStudent();
                case 7 -> updateGrade();
                case 8 -> deleteStudent();
                case 9 -> classStatistics();
                case 0 -> { running = false; printGoodbye(); }
                default -> printError("Invalid choice! Enter a number between 0 and 9.");
            }
        }
        scanner.close();
    }

    // ╔══════════════════════════════════════════╗
    // ║               MAIN MENU                  ║
    // ╚══════════════════════════════════════════╝
    static void showMainMenu() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║              MAIN MENU                   ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.println("  ║  [1]  Add New Student                    ║");
        System.out.println("  ║  [2]  Add Grade to Student               ║");
        System.out.println("  ║  [3]  View All Students                  ║");
        System.out.println("  ║  [4]  View Student Detail Report         ║");
        System.out.println("  ║  [5]  Display Summary Report             ║");
        System.out.println("  ║  [6]  Search Student                     ║");
        System.out.println("  ║  [7]  Update / Edit Grade                ║");
        System.out.println("  ║  [8]  Delete Student                     ║");
        System.out.println("  ║  [9]  Class Rankings & Statistics        ║");
        System.out.println("  ║  [0]  Exit                               ║");
        System.out.println("  ╚══════════════════════════════════════════╝");
    }

    // ╔══════════════════════════════════════════╗
    // ║          [1] ADD STUDENT                 ║
    // ╚══════════════════════════════════════════╝
    static void addStudent() {
        printHeader(" ADD NEW STUDENT ");

        System.out.print("  Enter student name : ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) { printError("Name cannot be empty!"); return; }

        for (Student s : students) {
            if (s.name.equalsIgnoreCase(name)) {
                printError("A student named \"" + name + "\" already exists!");
                return;
            }
        }

        Student student = new Student(name, rollCounter++);

        System.out.print("  Number of subjects  : ");
        int count = safeIntFromLine(scanner.nextLine().trim(), 0);

        for (int i = 0; i < count; i++) {
            System.out.print("  Subject " + (i + 1) + " name  : ");
            String sub = scanner.nextLine().trim();
            if (sub.isEmpty()) sub = "Subject " + (i + 1);
            double grade = getDoubleInput("  Marks  (0 - 100)  : ", 0, 100);
            student.addGrade(sub, grade);
        }

        students.add(student);
        printSuccess("Student added!  Roll No: " + student.rollNumber);
    }

    // ╔══════════════════════════════════════════╗
    // ║         [2] ADD GRADE                    ║
    // ╚══════════════════════════════════════════╝
    static void addGrade() {
        printHeader(" ADD GRADE TO STUDENT ");
        if (students.isEmpty()) { printError("No students found!"); return; }

        Student s = pickStudent();
        if (s == null) return;

        System.out.print("  Subject name : ");
        String sub = scanner.nextLine().trim();
        if (sub.isEmpty()) { printError("Subject cannot be empty!"); return; }

        for (String existing : s.subjects) {
            if (existing.equalsIgnoreCase(sub)) {
                printError("\"" + sub + "\" already exists. Use option [7] to update it.");
                return;
            }
        }

        double grade = getDoubleInput("  Marks (0 - 100)  : ", 0, 100);
        s.addGrade(sub, grade);
        printSuccess("Grade added for " + s.name + " in " + sub + "!");
    }

    // ╔══════════════════════════════════════════╗
    // ║        [3] VIEW ALL STUDENTS             ║
    // ╚══════════════════════════════════════════╝
    static void viewAllStudents() {
        printHeader(" ALL STUDENTS ");
        if (students.isEmpty()) { printError("No students found!"); return; }

        line('-', 78);
        System.out.printf("  %-6s  %-20s  %-8s  %-8s  %-8s  %-8s  %-5s  %-6s%n",
                "Roll", "Name", "Subj.", "Average", "Highest", "Lowest", "Grade", "Status");
        line('-', 78);

        for (Student s : students) {
            if (s.grades.isEmpty()) {
                System.out.printf("  %-6d  %-20s  %-8s  %-8s  %-8s  %-8s  %-5s  %-6s%n",
                        s.rollNumber, s.name, "0", "—", "—", "—", "—", "—");
            } else {
                System.out.printf("  %-6d  %-20s  %-8d  %-8.2f  %-8.2f  %-8.2f  %-5s  %-6s%n",
                        s.rollNumber, s.name, s.grades.size(),
                        s.getAverage(), s.getHighest(), s.getLowest(),
                        s.getLetterGrade(), s.getStatus());
            }
        }
        line('-', 78);
        System.out.println("  Total students: " + students.size());
    }

    // ╔══════════════════════════════════════════╗
    // ║       [4] STUDENT DETAIL REPORT          ║
    // ╚══════════════════════════════════════════╝
    static void viewStudentReport() {
        printHeader(" STUDENT DETAIL REPORT ");
        if (students.isEmpty()) { printError("No students found!"); return; }

        Student s = pickStudent();
        if (s == null) return;

        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║              REPORT CARD                     ║");
        System.out.println("  ╠══════════════════════════════════════════════╣");
        System.out.printf ("  ║  %-12s : %-30s ║%n", "Name", s.name);
        System.out.printf ("  ║  %-12s : %-30d ║%n", "Roll Number", s.rollNumber);
        System.out.printf ("  ║  %-12s : %-30d ║%n", "Subjects", s.grades.size());
        System.out.println("  ╠══════════════════════════════════════════════╣");

        if (s.grades.isEmpty()) {
            System.out.println("  ║  No grades recorded yet.                     ║");
        } else {
            System.out.printf("  ║  %-4s  %-20s  %-7s  %-5s  %-4s ║%n",
                    "No.", "Subject", "Marks", "Grade", "Bar");
            System.out.println("  ╠══════════════════════════════════════════════╣");

            for (int i = 0; i < s.grades.size(); i++) {
                double g = s.grades.get(i);
                String bar = "█".repeat((int)(g / 10)) + "░".repeat(10 - (int)(g / 10));
                System.out.printf("  ║  %-4d  %-20s  %-7.2f  %-5s  %-10s ║%n",
                        (i + 1), truncate(s.subjects.get(i), 20), g,
                        letterGradeFor(g), bar);
            }

            System.out.println("  ╠══════════════════════════════════════════════╣");
            System.out.printf ("  ║  %-12s : %-30.2f ║%n", "Average", s.getAverage());
            System.out.printf ("  ║  %-12s : %-30.2f ║%n", "Highest", s.getHighest());
            System.out.printf ("  ║  %-12s : %-30.2f ║%n", "Lowest",  s.getLowest());
            System.out.printf ("  ║  %-12s : %-30s ║%n", "Best in",  s.getHighestSubject());
            System.out.printf ("  ║  %-12s : %-30s ║%n", "Weak in",  s.getLowestSubject());
            System.out.printf ("  ║  %-12s : %-30s ║%n", "Letter",   s.getLetterGrade());
            System.out.printf ("  ║  %-12s : %-30s ║%n", "Remark",   s.getRemark());
            System.out.printf ("  ║  %-12s : %-30s ║%n", "Status",   s.getStatus());
        }
        System.out.println("  ╚══════════════════════════════════════════════╝");

        // Overall progress bar
        if (!s.grades.isEmpty()) {
            int fill = (int)(s.getAverage() / 5);
            System.out.print("\n  Progress [");
            for (int i = 0; i < 20; i++) System.out.print(i < fill ? "█" : "░");
            System.out.printf("]  %.1f / 100%n", s.getAverage());
        }
    }

    // ╔══════════════════════════════════════════╗
    // ║         [5] SUMMARY REPORT               ║
    // ╚══════════════════════════════════════════╝
    static void displaySummaryReport() {
        printHeader(" CLASS SUMMARY REPORT ");
        if (students.isEmpty()) { printError("No students found!"); return; }

        int total = students.size(), withGrades = 0, passed = 0, failed = 0;
        double classSum = 0, classHigh = Double.MIN_VALUE, classLow = Double.MAX_VALUE;
        String topName = "—", bottomName = "—";

        for (Student s : students) {
            if (!s.grades.isEmpty()) {
                withGrades++;
                double avg = s.getAverage();
                classSum += avg;
                if (s.getStatus().equals("PASS")) passed++; else failed++;
                if (avg > classHigh) { classHigh = avg; topName = s.name; }
                if (avg < classLow)  { classLow  = avg; bottomName = s.name; }
            }
        }

        double classAvg = withGrades > 0 ? classSum / withGrades : 0;
        double passRate = withGrades > 0 ? (passed * 100.0 / withGrades) : 0;

        line('=', 58);
        System.out.printf("  %-32s : %d%n",  "Total Students",          total);
        System.out.printf("  %-32s : %d%n",  "Students with Grades",    withGrades);
        System.out.printf("  %-32s : %d%n",  "Passed",                  passed);
        System.out.printf("  %-32s : %d%n",  "Failed",                  failed);
        line('-', 58);
        System.out.printf("  %-32s : %.2f %%%n","Pass Rate",             passRate);
        System.out.printf("  %-32s : %.2f%n",  "Class Average Score",   classAvg);

        if (withGrades > 0) {
            System.out.printf("  %-32s : %.2f  (%s)%n", "Highest Average", classHigh, topName);
            System.out.printf("  %-32s : %.2f  (%s)%n", "Lowest Average",  classLow,  bottomName);
        }
        line('-', 58);

        // ── Grade Distribution ──────────────────────────
        System.out.println("\n  GRADE DISTRIBUTION:");
        line('-', 58);

        String[] labels = {"A+  90-100", "A   80-89 ", "B   70-79 ",
                           "C   60-69 ", "D   40-59 ", "F   0-39  "};
        int[] counts = new int[6];

        for (Student s : students) {
            if (!s.grades.isEmpty()) {
                double avg = s.getAverage();
                if      (avg >= 90) counts[0]++;
                else if (avg >= 80) counts[1]++;
                else if (avg >= 70) counts[2]++;
                else if (avg >= 60) counts[3]++;
                else if (avg >= 40) counts[4]++;
                else                counts[5]++;
            }
        }

        for (int i = 0; i < 6; i++) {
            String bar = "■".repeat(counts[i] * 2);
            System.out.printf("  %-12s | %-30s  %d students%n",
                    labels[i], bar, counts[i]);
        }
        line('=', 58);

        // ── Per-subject class average (if subjects exist) ──
        Map<String, List<Double>> subMap = new LinkedHashMap<>();
        for (Student s : students) {
            for (int i = 0; i < s.subjects.size(); i++) {
                subMap.computeIfAbsent(s.subjects.get(i), k -> new ArrayList<>())
                      .add(s.grades.get(i));
            }
        }

        if (!subMap.isEmpty()) {
            System.out.println("\n  SUBJECT-WISE CLASS AVERAGES:");
            line('-', 58);
            System.out.printf("  %-24s  %-8s  %-6s  %s%n", "Subject", "Avg", "Grade", "Bar");
            line('-', 58);
            for (Map.Entry<String, List<Double>> e : subMap.entrySet()) {
                double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                String bar = "▓".repeat((int)(avg / 10));
                System.out.printf("  %-24s  %-8.2f  %-6s  %s%n",
                        truncate(e.getKey(), 24), avg, letterGradeFor(avg), bar);
            }
            line('=', 58);
        }
    }

    // ╔══════════════════════════════════════════╗
    // ║          [6] SEARCH STUDENT              ║
    // ╚══════════════════════════════════════════╝
    static void searchStudent() {
        printHeader(" SEARCH STUDENT ");
        if (students.isEmpty()) { printError("No students found!"); return; }

        System.out.println("  [1] Search by Name (keyword)");
        System.out.println("  [2] Search by Roll Number");
        int ch = getIntInput("  Choice: ");

        List<Student> results = new ArrayList<>();

        if (ch == 1) {
            System.out.print("  Enter keyword : ");
            String kw = scanner.nextLine().trim().toLowerCase();
            for (Student s : students)
                if (s.name.toLowerCase().contains(kw)) results.add(s);
        } else if (ch == 2) {
            int roll = getIntInput("  Enter Roll Number : ");
            for (Student s : students)
                if (s.rollNumber == roll) results.add(s);
        } else {
            printError("Invalid choice!"); return;
        }

        if (results.isEmpty()) {
            printError("No matching students found.");
        } else {
            System.out.println("\n  Found " + results.size() + " result(s):");
            line('-', 65);
            System.out.printf("  %-6s  %-22s  %-8s  %-6s  %-8s%n",
                    "Roll", "Name", "Average", "Grade", "Status");
            line('-', 65);
            for (Student s : results) {
                if (s.grades.isEmpty())
                    System.out.printf("  %-6d  %-22s  %-8s  %-6s  %-8s%n",
                            s.rollNumber, s.name, "—", "—", "—");
                else
                    System.out.printf("  %-6d  %-22s  %-8.2f  %-6s  %-8s%n",
                            s.rollNumber, s.name,
                            s.getAverage(), s.getLetterGrade(), s.getStatus());
            }
            line('-', 65);
        }
    }

    // ╔══════════════════════════════════════════╗
    // ║          [7] UPDATE GRADE                ║
    // ╚══════════════════════════════════════════╝
    static void updateGrade() {
        printHeader(" UPDATE GRADE ");
        if (students.isEmpty()) { printError("No students found!"); return; }

        Student s = pickStudent();
        if (s == null) return;

        if (s.grades.isEmpty()) {
            printError("No grades recorded for this student yet.");
            return;
        }

        System.out.println("\n  Current grades for " + s.name + ":");
        line('-', 40);
        for (int i = 0; i < s.subjects.size(); i++)
            System.out.printf("  [%d]  %-20s  %.2f%n",
                    (i + 1), s.subjects.get(i), s.grades.get(i));
        line('-', 40);

        int idx = getIntInput("  Subject number to update (0 to cancel): ");
        if (idx == 0) { System.out.println("  Cancelled."); return; }
        if (idx < 1 || idx > s.subjects.size()) { printError("Invalid number!"); return; }

        System.out.printf("  Old mark for %s: %.2f%n", s.subjects.get(idx-1), s.grades.get(idx-1));
        double ng = getDoubleInput("  New mark (0-100): ", 0, 100);
        s.grades.set(idx - 1, ng);
        printSuccess("Grade updated! " + s.subjects.get(idx-1) + " → " + ng);
    }

    // ╔══════════════════════════════════════════╗
    // ║          [8] DELETE STUDENT              ║
    // ╚══════════════════════════════════════════╝
    static void deleteStudent() {
        printHeader(" DELETE STUDENT ");
        if (students.isEmpty()) { printError("No students found!"); return; }

        Student s = pickStudent();
        if (s == null) return;

        System.out.print("  Delete \"" + s.name + "\" (Roll " + s.rollNumber + ")? [yes/no] : ");
        String ans = scanner.nextLine().trim().toLowerCase();

        if (ans.equals("yes") || ans.equals("y")) {
            students.remove(s);
            printSuccess("Student \"" + s.name + "\" deleted.");
        } else {
            System.out.println("  Delete cancelled.");
        }
    }

    // ╔══════════════════════════════════════════╗
    // ║     [9] CLASS RANKINGS & STATISTICS      ║
    // ╚══════════════════════════════════════════╝
    static void classStatistics() {
        printHeader(" CLASS RANKINGS & STATISTICS ");
        if (students.isEmpty()) { printError("No students found!"); return; }

        List<Student> ranked = new ArrayList<>();
        for (Student s : students) if (!s.grades.isEmpty()) ranked.add(s);

        if (ranked.isEmpty()) { printError("No students with grades!"); return; }

        // Sort descending by average (Collections sort)
        ranked.sort((a, b) -> Double.compare(b.getAverage(), a.getAverage()));

        System.out.println("  STUDENT RANKINGS");
        line('-', 72);
        System.out.printf("  %-5s  %-6s  %-20s  %-8s  %-8s  %-8s  %-5s  %-6s%n",
                "Rank", "Roll", "Name", "Average", "Highest", "Lowest", "Grade", "Status");
        line('-', 72);

        String[] medals = {" << 1st", " << 2nd", " << 3rd"};

        for (int i = 0; i < ranked.size(); i++) {
            Student s = ranked.get(i);
            String medal = (i < 3) ? medals[i] : "";
            System.out.printf("  %-5d  %-6d  %-20s  %-8.2f  %-8.2f  %-8.2f  %-5s  %-6s%s%n",
                    (i + 1), s.rollNumber, s.name,
                    s.getAverage(), s.getHighest(), s.getLowest(),
                    s.getLetterGrade(), s.getStatus(), medal);
        }
        line('=', 72);

        // Statistics
        double total = 0;
        for (Student s : ranked) total += s.getAverage();
        double clsAvg = total / ranked.size();

        System.out.println("\n  QUICK STATS");
        line('-', 50);
        System.out.printf("  %-28s: %d%n",   "Students Ranked",   ranked.size());
        System.out.printf("  %-28s: %.2f%n", "Class Average",     clsAvg);
        System.out.printf("  %-28s: %s (%.2f)%n", "Top Scorer",
                ranked.get(0).name, ranked.get(0).getAverage());
        System.out.printf("  %-28s: %s (%.2f)%n", "Needs Improvement",
                ranked.get(ranked.size()-1).name, ranked.get(ranked.size()-1).getAverage());

        long above = ranked.stream().filter(s -> s.getAverage() >= clsAvg).count();
        System.out.printf("  %-28s: %d%n", "Above Class Average", above);
        System.out.printf("  %-28s: %d%n", "Below Class Average", ranked.size() - above);
        line('=', 50);
    }

    // ╔══════════════════════════════════════════╗
    // ║           HELPER METHODS                 ║
    // ╚══════════════════════════════════════════╝

    static Student pickStudent() {
        System.out.println("  [1] Find by Roll Number");
        System.out.println("  [2] Find by Name");
        int ch = getIntInput("  Select: ");

        if (ch == 1) {
            int roll = getIntInput("  Roll Number: ");
            for (Student s : students) if (s.rollNumber == roll) return s;
        } else if (ch == 2) {
            System.out.print("  Student name: ");
            String name = scanner.nextLine().trim();
            for (Student s : students)
                if (s.name.equalsIgnoreCase(name)) return s;
        }
        printError("Student not found!");
        return null;
    }

    static String letterGradeFor(double score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 60) return "C";
        if (score >= 40) return "D";
        return "F";
    }

    static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    // ── Input helpers ──────────────────────────
    static int getIntInput(String prompt) {
        while (true) {
            if (!prompt.isEmpty()) System.out.print(prompt);
            try { return Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { printError("Enter a valid integer."); }
        }
    }

    static int safeIntFromLine(String line, int fallback) {
        try { int v = Integer.parseInt(line); return v >= 0 ? v : fallback; }
        catch (NumberFormatException e) { return fallback; }
    }

    static double getDoubleInput(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            try {
                double v = Double.parseDouble(scanner.nextLine().trim());
                if (v >= min && v <= max) return v;
                printError("Value must be between " + (int)min + " and " + (int)max + ".");
            } catch (NumberFormatException e) { printError("Enter a valid number."); }
        }
    }

    // ── Display helpers ────────────────────────
    static void printBanner() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════════╗");
        System.out.println("  ║                                               ║");
        System.out.println("  ║        STUDENT  GRADE  TRACKER                ║");
        System.out.println("  ║         Java Console Application              ║");
        System.out.println("  ║                                               ║");
        System.out.println("  ╚═══════════════════════════════════════════════╝");
        System.out.println("       Manage Students | Track Grades | Reports");
        System.out.println();
    }

    static void printHeader(String title) {
        System.out.println();
        line('─', 50);
        System.out.println("  >>>  " + title);
        line('─', 50);
    }

    static void line(char ch, int len) {
        System.out.println("  " + String.valueOf(ch).repeat(len));
    }

    static void printError(String msg) {
        System.out.println("\n  [!] " + msg + "\n");
    }

    static void printSuccess(String msg) {
        System.out.println("\n  [✓] " + msg + "\n");
    }

    static void printGoodbye() {
        System.out.println();
        System.out.println("  ╔════════════════════════════════════════╗");
        System.out.println("  ║   Thank you for using Grade Tracker!   ║");
        System.out.println("  ║              Goodbye!                  ║");
        System.out.println("  ╚════════════════════════════════════════╝");
        System.out.println();
    }
}