import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code EnergyOptimizationLogin} class is responsible for:
 * <ul>
 *   <li>Loading user credentials from an Excel file (users.xlsx).</li>
 *   <li>Displaying a login screen where users can enter their credentials.</li>
 *   <li>Determining the access level of the logged-in user.</li>
 * </ul>
 * Access levels currently supported: {@code rich}, {@code average}, {@code poor}.
 * This class uses Apache POI to interact with Excel files.
 */
public class EnergyOptimizationLogin {

    /**
     * Path to the Excel file that stores user credentials and access levels.
     */
    private static final String EXCEL_FILE = "users.xlsx";

    /**
     * A {@code Map} storing the username as the key and the corresponding access level as the value.
     */
    private static Map<String, String> userAccessLevels = new HashMap<>();

    /**
     * Stores the current (logged-in) user's access level.
     */
    private static String currentAccessLevel;

    /**
     * Returns the current (logged-in) user's access level.
     *
     * @return {@code String} representing the access level of the user.
     */
    public static String getCurrentAccessLevel() {
        return currentAccessLevel;
    }

    /**
     * The entry point of the application.
     * <p>
     * Loads user data from the Excel file, then creates and displays the login screen.
     *
     * @param args command-line arguments (not used).
     */
    public static void main(String[] args) {
        loadUserData(); // Load user data from the Excel file.
        SwingUtilities.invokeLater(EnergyOptimizationLogin::showLoginScreen);
    }

    /**
     * Loads user credentials (username and access level) from an Excel file (users.xlsx)
     * using Apache POI. If the file is not found or fails to load, a new file is assumed
     * to be created later.
     */
    private static void loadUserData() {
        try (FileInputStream fis = new FileInputStream(EXCEL_FILE);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Get the first sheet from the workbook.
            Sheet sheet = workbook.getSheetAt(0);

            // Iterate through all rows in the sheet.
            for (Row row : sheet) {
                // Retrieve the username and access level from the first two columns.
                Cell usernameCell = row.getCell(0);
                Cell accessLevelCell = row.getCell(1);

                // Validate the cells and store the data if both are not null.
                if (usernameCell != null && accessLevelCell != null) {
                    userAccessLevels.put(usernameCell.getStringCellValue(), accessLevelCell.getStringCellValue());
                }
            }
        } catch (IOException e) {
            System.out.println("No user data found. A new file will be created.");
        }
    }

    /**
     * Displays a simple login screen built with Swing.
     * <p>
     * Users are required to input a username and an access level (rich, average, or poor).
     * If the credentials match the data loaded from the Excel file, the user is granted access.
     */
    private static void showLoginScreen() {
        // Create the main frame for the login.
        JFrame frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(4, 2, 10, 10));
        frame.getContentPane().setBackground(Color.BLACK);

        // Username label and text field.
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        JTextField userField = new JTextField();

        // Access level label and text field.
        JLabel accessLabel = new JLabel("Access Level (rich, average, poor):");
        accessLabel.setForeground(Color.WHITE);
        JTextField accessField = new JTextField();

        // Login button triggers authentication logic.
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String accessLevel = accessField.getText().trim().toLowerCase();

            // Validate credentials against data loaded from the Excel file.
            if (userAccessLevels.containsKey(username) && userAccessLevels.get(username).equals(accessLevel)) {
                currentAccessLevel = accessLevel;
                frame.dispose(); // Close the login screen.
                new EnergyOptimizationGUI(); // Proceed to the main GUI.
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid credentials!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Add components to the frame.
        frame.add(userLabel);
        frame.add(userField);
        frame.add(accessLabel);
        frame.add(accessField);
        frame.add(new JLabel()); // Empty label to maintain layout structure.
        frame.add(loginButton);

        // Set frame properties.
        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

/**
 * The {@code EnergyOptimizationGUI} class provides a graphical user interface (GUI)
 * where users can input parameters relevant to energy usage (e.g., temperature,
 * occupancy, power usage, room area, and peak hour status) and retrieve recommendations
 * from an external service via a REST API.
 * <p>
 * It includes:
 * <ul>
 *   <li>Input fields for temperature, occupancy, power usage, and room area.</li>
 *   <li>A checkbox for peak hours status.</li>
 *   <li>A dropdown (combo box) for selecting additional data (RAG data).</li>
 *   <li>A text area to display the response from the external recommendation service.</li>
 * </ul>
 */
class EnergyOptimizationGUI {

    /**
     * The main frame container for the entire GUI.
     */
    private JFrame frame;

    /**
     * Text fields for collecting numerical inputs from the user.
     */
    private JTextField tempField, occupancyField, powerField, roomAreaField;

    /**
     * Checkbox to indicate whether the current time is within peak hours.
     */
    private JCheckBox peakHoursCheck;

    /**
     * A combo box for selecting additional data options (e.g., none, energy usage trends, weather forecasts).
     */
    private JComboBox<String> ragComboBox;

    /**
     * A text area to display the result of the recommendation.
     */
    private JTextArea resultArea;

    /**
     * Constructs a new {@code EnergyOptimizationGUI}, initializing and displaying the GUI components.
     */
    public EnergyOptimizationGUI() {
        // Set up the main frame.
        frame = new JFrame("Energy Optimization Agent");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(30, 30, 30));

        // Create a panel to hold input fields.
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.setBackground(new Color(50, 50, 50));

        // Create and add the labeled text fields.
        tempField = createStyledTextField(inputPanel, "Temperature:");
        occupancyField = createStyledTextField(inputPanel, "Occupancy:");
        powerField = createStyledTextField(inputPanel, "Power Usage (W):");
        roomAreaField = createStyledTextField(inputPanel, "Room Area (m²):");

        // Create and add the peak hours checkbox.
        peakHoursCheck = new JCheckBox("Peak Hours");
        peakHoursCheck.setFont(new Font("Arial", Font.BOLD, 14));
        peakHoursCheck.setForeground(Color.WHITE);
        peakHoursCheck.setBackground(new Color(50, 50, 50));
        // Add an empty label first for layout alignment.
        inputPanel.add(new JLabel(""));
        inputPanel.add(peakHoursCheck);

        // Create and add a combo box for additional data (RAG data).
        JLabel ragLabel = new JLabel("Additional Data:");
        ragLabel.setForeground(Color.WHITE);
        String[] ragOptions = {"None", "Energy Usage Trends", "Weather Forecasts"};
        ragComboBox = new JComboBox<>(ragOptions);
        inputPanel.add(ragLabel);
        inputPanel.add(ragComboBox);

        // Add the input panel to the frame.
        frame.add(inputPanel, BorderLayout.NORTH);

        // Create a button to send the request.
        JButton sendButton = new JButton("Get Recommendation");
        sendButton.addActionListener(this::sendRequest);
        frame.add(sendButton, BorderLayout.CENTER);

        // Create a text area to display results from the server.
        resultArea = new JTextArea(6, 15);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 16));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(40, 40, 40));
        resultArea.setForeground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        frame.add(scrollPane, BorderLayout.SOUTH);

        // Set frame size and make it visible.
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Creates a labeled {@code JTextField} with a consistent style and adds it to the specified panel.
     *
     * @param panel The panel to which the label and text field will be added.
     * @param label A {@code String} representing the label text.
     * @return The created {@code JTextField} reference.
     */
    private JTextField createStyledTextField(JPanel panel, String label) {
        JLabel jLabel = new JLabel(label);
        jLabel.setForeground(Color.WHITE);
        JTextField textField = new JTextField();
        panel.add(jLabel);
        panel.add(textField);
        return textField;
    }

    /**
     * This method is triggered when the user clicks the "Get Recommendation" button.
     * <p>
     * It launches a background task (using {@code SwingWorker}) to:
     * <ol>
     *   <li>Parse the user input.</li>
     *   <li>Build a prompt incorporating the user's access level.</li>
     *   <li>Send the prompt to an external REST API endpoint.</li>
     *   <li>Receive and display the recommendation in the GUI.</li>
     * </ol>
     *
     * @param e The {@code ActionEvent} triggered by button click.
     */
    private void sendRequest(ActionEvent e) {
        // Display interim status.
        resultArea.setText("Processing...");

        // Use SwingWorker for background processing to avoid freezing the UI.
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                fetchRecommendation();
                return null;
            }
        };
        worker.execute();
    }

    /**
     * Parses user inputs and sends a POST request to a local chat API endpoint.
     * <p>
     * The request payload includes:
     * <ul>
     *   <li>User's access level (poor, average, or rich).</li>
     *   <li>Room conditions and usage data (temperature, occupancy, power usage, etc.).</li>
     *   <li>Any additional data (RAG) if selected by the user.</li>
     * </ul>
     *
     * The response from the endpoint is displayed in the result area.
     */
    private void fetchRecommendation() {
        try {
            // Retrieve and parse the numeric values.
            int temperature = Integer.parseInt(tempField.getText());
            int occupancy = Integer.parseInt(occupancyField.getText());
            int powerUsage = Integer.parseInt(powerField.getText());
            double roomArea = Double.parseDouble(roomAreaField.getText());

            // Check peak hours.
            boolean isPeakHours = peakHoursCheck.isSelected();
            String peakHoursStatus = isPeakHours ? "Yes" : "No";

            // Determine the access level from the login flow.
            String accessLevel = EnergyOptimizationLogin.getCurrentAccessLevel();
            String recommendationStyle;

            // Select the recommendation style based on the access level.
            if ("poor".equalsIgnoreCase(accessLevel)) {
                recommendationStyle = "cheap and efficient";
            } else if ("average".equalsIgnoreCase(accessLevel)) {
                recommendationStyle = "balanced between cost and performance";
            } else if ("rich".equalsIgnoreCase(accessLevel)) {
                recommendationStyle = "best performance regardless of cost";
            } else {
                // Default fallback if no recognized access level.
                recommendationStyle = "standard recommendation";
            }

            // Fetch any additional RAG data (e.g., usage trends or weather forecasts).
            String ragSelection = (String) ragComboBox.getSelectedItem();
            String additionalData = "";
            if (!"None".equalsIgnoreCase(ragSelection)) {
                additionalData = fetchRAGData(ragSelection);
            }

            // Build the prompt for the recommendation.
            String prompt = String.format(
                    "User with access level '%s' requires recommendations that are %s. " +
                            "Conditions: Temperature: %d, Occupancy: %d, Power Usage: %dW, Room Area: %.2f m², Peak Hours: %s. %s " +
                            "Please suggest the best HVAC mode and temperature.",
                    accessLevel,
                    recommendationStyle,
                    temperature,
                    occupancy,
                    powerUsage,
                    roomArea,
                    peakHoursStatus,
                    additionalData
            );

            // Prepare the API call to the local chat endpoint.
            String urlString = "http://localhost:11434/api/chat";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Construct the JSON payload.
            String payload = String.format(
                    "{\"model\": \"mistral\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}",
                    prompt
            );

            // Send the JSON payload in the request body.
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Process the response.
            if (conn.getResponseCode() == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        // Filter out empty lines.
                        if (!line.trim().isEmpty()) {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(line);
                                if (jsonNode.has("message") && jsonNode.get("message").has("content")) {
                                    response.append(jsonNode.get("message").get("content").asText()).append(" ");
                                }
                            } catch (Exception ex) {
                                // Fallback if we fail to parse JSON.
                                response.append(" Failed to parse response: ").append(line);
                            }
                        }
                    }
                    // Display the final recommendation.
                    resultArea.setText("Recommended Setting: " + response.toString().trim());
                }
            } else {
                // If the server responds with an error code.
                resultArea.setText("Error: " + conn.getResponseCode());
            }

            conn.disconnect();
        } catch (Exception ex) {
            // Catch and display any exception (e.g., parsing, network issues).
            resultArea.setText("Error: " + ex.getMessage());
        }
    }

    /**
     * Fetches additional data from the local API endpoints based on the user selection.
     * <p>
     * Supported endpoints:
     * <ul>
     *   <li>{@code /api/energy-usage-trends} for historical or aggregated energy usage data.</li>
     *   <li>{@code /api/weather-forecasts} for weather conditions that might affect energy usage.</li>
     * </ul>
     *
     * @param selection The selected RAG data option ("None", "Energy Usage Trends", or "Weather Forecasts").
     * @return A string containing the additional data to be appended to the prompt.
     */
    private String fetchRAGData(String selection) {
        String endpoint;
        if ("Energy Usage Trends".equalsIgnoreCase(selection)) {
            endpoint = "http://localhost:11434/api/energy-usage-trends";
        } else if ("Weather Forecasts".equalsIgnoreCase(selection)) {
            endpoint = "http://localhost:11434/api/weather-forecasts";
        } else {
            // Default or unsupported option.
            return "";
        }

        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            // If the request succeeds.
            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
                in.close();
                // Return the data appended with a label.
                return "Additional Data: " + result.toString();
            } else {
                // If the server responds with an error.
                return "";
            }
        } catch (Exception ex) {
            // If an exception occurs, return an empty string.
            return "";
        }
    }
}
