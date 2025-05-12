import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class DeleteStudent {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection();
             Scanner scanner = new Scanner(System.in)) {

            String choice;

            do {
                System.out.print("Enter student ID to delete: ");
                int studentId = scanner.nextInt();
                scanner.nextLine(); // consume newline

                // Check if the student exists
                String checkSql = "SELECT name, course FROM students WHERE id = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, studentId);
                ResultSet resultSet = checkStmt.executeQuery();

                if (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String course = resultSet.getString("course");

                    System.out.println("Student Found:");
                    System.out.println("Name: " + name);
                    System.out.println("Course: " + course);

                    System.out.print("Are you sure you want to delete this student? (yes/no): ");
                    String confirm = scanner.nextLine().trim().toLowerCase();

                    if (confirm.equals("yes")) {
                        String deleteSql = "DELETE FROM students WHERE id = ?";
                        PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                        deleteStmt.setInt(1, studentId);

                        int rowsDeleted = deleteStmt.executeUpdate();

                        if (rowsDeleted > 0) {
                            System.out.println("Student deleted successfully.");
                        } else {
                            System.out.println("Deletion failed.");
                        }
                    } else {
                        System.out.println("Deletion cancelled.");
                    }

                } else {
                    System.out.println("No student found with ID " + studentId);
                }

                System.out.print("Do you want to delete another record? (yes/no): ");
                choice = scanner.nextLine().trim().toLowerCase();

            } while (choice.equals("yes"));

            System.out.println("Finished deleting records.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
