import java.sql.*;
import java.util.*;

public class StudentApp {
    private static final String URL = "jdbc:mysql://localhost:3306/student_db";
    private static final String USER = "root";
    private static final String PASS = "root"; // ⚠️ Change to your MySQL password!

    // ==========================================
    // Q1: STUDENT CLASS (Encapsulation & Logic)
    // ==========================================
    static class Student {
        private int studentId;
        private String studentName, email, courseName;
        private double programmingScore, mathScore;
        private List<String> skills;

        // Constructor
        public Student(int studentId, String studentName, String email, String courseName, double programmingScore, double mathScore, List<String> skills) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.email = email;
            this.courseName = courseName;
            this.programmingScore = programmingScore;
            this.mathScore = mathScore;
            this.skills = skills;
        }

        // Getters and Setters (Encapsulation)
        public int getStudentId() { return studentId; }
        public void setStudentId(int studentId) { this.studentId = studentId; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        public double getProgrammingScore() { return programmingScore; }
        public void setProgrammingScore(double programmingScore) { this.programmingScore = programmingScore; }
        public double getMathScore() { return mathScore; }
        public void setMathScore(double mathScore) { this.mathScore = mathScore; }
        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills; }

        // Object Methods required by Q1
        public double getAverageScore() {
            return (programmingScore + mathScore) / 2.0;
        }

        public String getPerformanceCategory() {
            double avg = getAverageScore();
            if (avg >= 85) return "Excellent";
            else if (avg >= 70) return "Good";
            else if (avg >= 50) return "Satisfactory";
            else return "Needs Improvement";
        }

        public String getStrongerSubject() {
            if (programmingScore > mathScore) return "Programming";
            else if (mathScore > programmingScore) return "Mathematics";
            else return "Both Equal";
        }
    }

    // ==========================================
    // Q2: MAIN METHOD & DATABASE LOGIC
    // ==========================================
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Scanner sc = new Scanner(System.in)) {

            while (true) {
                System.out.println("\n--- Student Management System ---");
                System.out.println("1. Add Student (Q2)");
                System.out.println("2. Search by ID/Email/Course (Q2)");
                System.out.println("3. Generate Merit List (Q2)");
                System.out.println("4. Find Students with Same Skills (Q2)");
                System.out.println("5. Needs Improvement (Prog < 50, Math >= 75) (Q2)");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");
                int choice = sc.nextInt();
                sc.nextLine(); // consume newline

                switch (choice) {
                    case 1: addStudent(conn, sc); break;
                    case 2: searchStudent(conn, sc); break;
                    case 3: generateMeritList(conn); break;
                    case 4: findSameSkills(conn); break;
                    case 5: needsImprovement(conn); break;
                    case 6: return;
                    default: System.out.println("Invalid choice.");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // 1. Add Student (Handles Duplicates and Validation)
    private static void addStudent(Connection conn, Scanner sc) {
        System.out.print("Enter ID: "); int id = sc.nextInt(); sc.nextLine();
        System.out.print("Enter Name: "); String name = sc.nextLine();
        System.out.print("Enter Email: "); String email = sc.nextLine();
        System.out.print("Enter Course: "); String course = sc.nextLine();

        System.out.print("Enter Programming Score (0-100): "); double prog = sc.nextDouble();
        System.out.print("Enter Math Score (0-100): "); double math = sc.nextDouble();
        sc.nextLine();
        System.out.print("Enter Skills (comma separated, e.g., Java,Python): "); String skillsStr = sc.nextLine();

        // Q2: Validate scores between 0 and 100
        if (prog < 0 || prog > 100 || math < 0 || math > 100) {
            System.out.println("❌ Error: Scores must be between 0 and 100.");
            return;
        }

        String sql = "INSERT INTO StudentProfile VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id); pstmt.setString(2, name); pstmt.setString(3, email);
            pstmt.setString(4, course); pstmt.setDouble(5, prog); pstmt.setDouble(6, math);
            pstmt.setString(7, skillsStr);
            pstmt.executeUpdate();
            System.out.println("✅ Student added successfully.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("❌ Error: Duplicate Student ID!"); // Q2: Prevent duplicate IDs
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // 2. Search by ID, Email, or Course
    private static void searchStudent(Connection conn, Scanner sc) {
        System.out.print("Search by (1. ID, 2. Email, 3. Course): ");
        int type = sc.nextInt(); sc.nextLine();
        System.out.print("Enter search value: "); String val = sc.nextLine();

        String col = (type == 1) ? "student_id" : (type == 2) ? "email" : "course_name";
        String sql = "SELECT * FROM StudentProfile WHERE " + col + " = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, val);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) printStudent(rs);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // 3. Generate Merit List sorted by Average (Descending)
    private static void generateMeritList(Connection conn) {
        String sql = "SELECT * FROM StudentProfile ORDER BY (programming_score + math_score) DESC";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("🏆 MERIT LIST 🏆");
            while (rs.next()) printStudent(rs);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // 4. Identify students with same technical skills (Using HashMap)
    private static void findSameSkills(Connection conn) {
        Map<String, List<Integer>> skillMap = new HashMap<>();

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT student_id, skills FROM StudentProfile")) {
            while (rs.next()) {
                int id = rs.getInt("student_id");
                String[] skills = rs.getString("skills").split(",");
                for (String s : skills) {
                    String trimmed = s.trim();
                    skillMap.computeIfAbsent(trimmed, k -> new ArrayList<>()).add(id);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        for (Map.Entry<String, List<Integer>> entry : skillMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                System.out.println("Skill '" + entry.getKey() + "' shared by Student IDs: " + entry.getValue());
            }
        }
    }

    // 5. Needs Improvement in Programming but Math >= 75
    private static void needsImprovement(Connection conn) {
        String sql = "SELECT * FROM StudentProfile WHERE programming_score < 50 AND math_score >= 75";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) printStudent(rs);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ==========================================
    // HELPER: Extracts DB data, creates Object, and prints calculations
    // ==========================================
    private static void printStudent(ResultSet rs) throws SQLException {
        int id = rs.getInt("student_id");
        String name = rs.getString("student_name");
        String email = rs.getString("email");
        String course = rs.getString("course_name");
        double prog = rs.getDouble("programming_score");
        double math = rs.getDouble("math_score");

        // Convert comma-separated string back to List<String>
        String skillsStr = rs.getString("skills");
        List<String> skillsList = Arrays.asList(skillsStr.split(","));

        // Create Q1 Student Object
        Student s = new Student(id, name, email, course, prog, math, skillsList);

        // Print raw data + Q1/Q2 calculated Business Logic
        System.out.println("--------------------------------------------------");
        System.out.println("ID: " + s.getStudentId() + " | Name: " + s.getStudentName() + " | Course: " + s.getCourseName());
        System.out.println("Scores -> Prog: " + s.getProgrammingScore() + " | Math: " + s.getMathScore());
        System.out.println("Average Score: " + s.getAverageScore());
        System.out.println("Performance: " + s.getPerformanceCategory());
        System.out.println("Stronger Subject: " + s.getStrongerSubject());
        System.out.println("Skills: " + s.getSkills());
    }
}
