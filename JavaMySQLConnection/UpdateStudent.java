import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class UpdateStudent {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection();
             Scanner scanner = new Scanner(System.in)) {

            String choice;

            do {
                System.out.print("Enter student ID to update: ");
                int studentId = scanner.nextInt();
                scanner.nextLine(); // consume newline

                // Check if the student exists
                String checkSql = "SELECT name, course FROM students WHERE id = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, studentId);
                ResultSet resultSet = checkStmt.executeQuery();

                if (resultSet.next()) {
                    System.out.println("Current Name: " + resultSet.getString("name"));
                    System.out.println("Current Course: " + resultSet.getString("course"));

                    System.out.print("Enter new name: ");
                    String newName = scanner.nextLine();

                    System.out.print("Enter new course: ");
                    String newCourse = scanner.nextLine();

                    String updateSql = "UPDATE students SET name = ?, course = ? WHERE id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setString(1, newName);
                    updateStmt.setString(2, newCourse);
                    updateStmt.setInt(3, studentId);

                    int rowsUpdated = updateStmt.executeUpdate();

                    if (rowsUpdated > 0) {
                        System.out.println("Student record updated successfully!");
                    } else {
                        System.out.println("Failed to update the student record.");
                    }

                } else {
                    System.out.println("No student found with ID " + studentId);
                }

                System.out.print("Do you want to update another record? (yes/no): ");
                choice = scanner.nextLine().trim().toLowerCase();

            } while (choice.equals("yes"));

            System.out.println("Finished updating student records.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
