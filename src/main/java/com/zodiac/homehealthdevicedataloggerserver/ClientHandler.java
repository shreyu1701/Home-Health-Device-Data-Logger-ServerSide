package com.zodiac.homehealthdevicedataloggerserver;

import com.zodiac.homehealthdevicedataloggerserver.Data.DbConnect;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class ClientHandler implements Runnable {
	DbConnect dbConnect = new DbConnect();
	private final Socket clientSocket;
	private final MainServer server;
	private BufferedReader in;
	private PrintWriter out;

	public ClientHandler(Socket socket, MainServer server) {
		this.clientSocket = socket;
		this.server = server;
	}

	@Override
	public void run() {
		try {
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			out = new PrintWriter(clientSocket.getOutputStream(), true);

			String clientMessage;
//			while ((clientMessage = in.readLine()) != null) {
//				server.updateLog("Received: " + clientMessage);
//
//				if (clientMessage.startsWith("LOGIN:")) {
//					handleLogin(clientMessage);
//				} else if ("exit".equalsIgnoreCase(clientMessage)) {
//					break;
//				} else {
//					out.println("UNKNOWN_COMMAND");
//				}
//			}

			while ((clientMessage = in.readLine()) != null) {
				server.updateLog("Received: " + clientMessage);

				if (clientMessage.startsWith("LOGIN:")) {
					handleLogin(clientMessage);
				} else if (clientMessage.startsWith("SIGNUP:")) {
					handleSignUp(clientMessage);
				} else if ("exit".equalsIgnoreCase(clientMessage)) {
					break;
				} else {
					out.println("UNKNOWN_COMMAND");
				}
			}


		} catch (IOException e) {
			server.updateLog("Error handling client: " + e.getMessage());
		} finally {
			try {
				server.updateLog("Client disconnected: " + clientSocket.getInetAddress());
				if (in != null) in.close();
				if (out != null) out.close();
				if (clientSocket != null) clientSocket.close();
			} catch (IOException e) {
				server.updateLog("Error closing client connection: " + e.getMessage());
			}
		}

	}

	///For Handling sign up
	private void handleSignUp(String clientMessage) {
		String[] parts = clientMessage.split(":");
		if (parts.length == 13) { // Ensure all fields are provided (adjusted for the new fields)
			String uniqueId = parts[1];
			String firstName = parts[2];
			String lastName = parts[3];
			String ageText = parts[4];
			String phone = parts[5];
			String gender = parts[6];
			String role = parts[7];
			String roleId = parts[8];
			String bloodGroup = parts[9];
			String email = parts[10];
			String password = parts[11];
			String confirmPassword = parts[12];

			// Validate fields
			String validationError = validateFields(uniqueId, firstName, lastName, ageText, phone, gender, role, roleId, bloodGroup, email, password, confirmPassword);
			if (validationError != null) {
				out.println("FAILURE: " + validationError);
				server.updateLog("Validation failed: " + validationError);
				return;
			}

			// Store user in the database
			if (saveUserToDatabase(uniqueId, firstName, lastName, Integer.parseInt(ageText), phone, gender, role, roleId, bloodGroup, email, password)) {
				out.println("SUCCESS");
				server.updateLog("User signed up successfully: " + email);
			} else {
				out.println("FAILURE: Database error. Please try again later.");
				server.updateLog("Database error for user: " + email);
			}
		} else {
			out.println("FAILURE: Invalid SIGNUP format");
			server.updateLog("Invalid SIGNUP request: " + clientMessage);
		}
	}

	private String validateFields(String uniqueId, String firstName, String lastName, String ageText, String phone, String gender, String role, String roleId, String bloodGroup, String email, String password, String confirmPassword) {
		if (uniqueId == null || uniqueId.isEmpty()) {
			return "Unique ID is missing.";
		}
		if (firstName == null || firstName.isEmpty() || !firstName.matches("[A-Za-z]+")) {
			return "Invalid first name.";
		}
		if (lastName == null || lastName.isEmpty() || !lastName.matches("[A-Za-z]+")) {
			return "Invalid last name.";
		}
		try {
			int age = Integer.parseInt(ageText);
			if (age < 0 || age > 120) {
				return "Invalid age.";
			}
		} catch (NumberFormatException e) {
			return "Invalid age. Must be a number.";
		}
		if (phone == null || !phone.matches("\\d{10}")) {
			return "Invalid phone number. Must be 10 digits.";
		}
		if (gender == null || (!gender.equalsIgnoreCase("Male") && !gender.equalsIgnoreCase("Female") && !gender.equalsIgnoreCase("Other"))) {
			return "Invalid gender.";
		}
		if (role == null || role.isEmpty()) {
			return "Role must be selected.";
		}
		if (roleId == null || roleId.isEmpty()) {
			return "Role ID is missing.";
		}
		if (bloodGroup == null || bloodGroup.isEmpty()) {
			return "Blood group must be selected.";
		}
		if (email == null || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
			return "Invalid email format.";
		}
		if (password == null || password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
			return "Password must be at least 8 characters long and contain both letters and numbers.";
		}
		if (!password.equals(confirmPassword)) {
			return "Passwords do not match.";
		}
		return null; // No validation errors
	}

	private boolean saveUserToDatabase(String uniqueId, String firstName, String lastName, int age, String phone, String gender, String role, String roleId, String bloodGroup, String email, String password) {
		String query = "INSERT INTO users (user_id, first_name, last_name, age, phone_number, gender, role_name, role_id, blood_group, email, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (Connection conn = dbConnect.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(query)) {

			stmt.setString(1, uniqueId);
			stmt.setString(2, firstName);
			stmt.setString(3, lastName);
			stmt.setInt(4, age);
			stmt.setString(5, phone);
			stmt.setString(6, gender);
			stmt.setString(7, role);
			stmt.setString(8, roleId);
			stmt.setString(9, bloodGroup);
			stmt.setString(10, email);
			stmt.setString(11, password); // Ensure password is hashed on the client side

			int rowsAffected = stmt.executeUpdate();
			return rowsAffected > 0;
		} catch (SQLException e) {
			server.updateLog("Database error during sign up: " + e.getMessage());
		}
		return false;
	}



	private void handleLogin(String clientMessage) {
		String[] parts = clientMessage.split(":");
		if (parts.length == 3) {
			String email = parts[1];
			String password = parts[2];

			boolean connection = validateCredentials(email);

			// Validate credentials (hardcoded for now, replace with database logic)
			if (connection) {
				out.println("SUCCESS");
				server.updateLog("Client logged in: " + email);
			} else {
				out.println("FAILURE");
				server.updateLog("Login failed for: " + email);
			}
		} else {
			out.println("ERROR: Invalid LOGIN format");
			server.updateLog("Invalid LOGIN request: " + clientMessage);
		}
	}

	private boolean validateCredentials(String email) {
		String query = "SELECT COUNT(*) FROM users WHERE email = ?";
		Connection dbConnection = dbConnect.getConnection();
		try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
			stmt.setString(1, email);
			ResultSet rs = stmt.executeQuery();
			if (rs.next() && rs.getInt(1) > 0) {
				return true;
			}
		} catch (SQLException e) {
			server.updateLog("Database error: " + e.getMessage());
		}
		return false;
	}



}
