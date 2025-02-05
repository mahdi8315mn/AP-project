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

public class EnergyOptimizationLogin {
    private static final String EXCEL_FILE = "users.xlsx";
    private static Map<String, String> userAccessLevels = new HashMap<>();
    private static String currentAccessLevel;

    public static String getCurrentAccessLevel() {
        return currentAccessLevel;
    }

    public static void main(String[] args) {
        loadUserData();
        SwingUtilities.invokeLater(EnergyOptimizationLogin::showLoginScreen);
    }

    private static void loadUserData() {
        try (FileInputStream fis = new FileInputStream(EXCEL_FILE);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                Cell usernameCell = row.getCell(0);
                Cell accessLevelCell = row.getCell(1);
                if (usernameCell != null && accessLevelCell != null) {
                    userAccessLevels.put(usernameCell.getStringCellValue(), accessLevelCell.getStringCellValue());
                }
            }
        } catch (IOException e) {
            System.out.println("No user data found. A new file will be created.");
        }
    }

    private static void showLoginScreen() {
        JFrame frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(4, 2, 10, 10));
        frame.getContentPane().setBackground(Color.BLACK);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        JTextField userField = new JTextField();

        JLabel accessLabel = new JLabel("Access Level (rich, average, poor):");
        accessLabel.setForeground(Color.WHITE);
        JTextField accessField = new JTextField();

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String accessLevel = accessField.getText().trim().toLowerCase();

            if (userAccessLevels.containsKey(username) && userAccessLevels.get(username).equals(accessLevel)) {
                currentAccessLevel = accessLevel;
                frame.dispose();
                new EnergyOptimizationGUI();
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid credentials!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        frame.add(userLabel);
        frame.add(userField);
        frame.add(accessLabel);
        frame.add(accessField);
        frame.add(new JLabel());
        frame.add(loginButton);

        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class EnergyOptimizationGUI {
    private JFrame frame;
    private JTextField tempField, occupancyField, powerField;
    private JCheckBox peakHoursCheck;
    private JTextArea resultArea;

    public EnergyOptimizationGUI() {
        frame = new JFrame("Energy Optimization Agent");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(30, 30, 30));

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.setBackground(new Color(50, 50, 50));

        tempField = createStyledTextField(inputPanel, "Temperature:");
        occupancyField = createStyledTextField(inputPanel, "Occupancy:");
        powerField = createStyledTextField(inputPanel, "Power Usage (W):");

        peakHoursCheck = new JCheckBox("Peak Hours");
        peakHoursCheck.setFont(new Font("Arial", Font.BOLD, 14));
        peakHoursCheck.setForeground(Color.WHITE);
        peakHoursCheck.setBackground(new Color(50, 50, 50));
        inputPanel.add(new JLabel("")); // Empty label for spacing
        inputPanel.add(peakHoursCheck);

        frame.add(inputPanel, BorderLayout.NORTH);

        JButton sendButton = new JButton("Get Recommendation");
        sendButton.addActionListener(this::sendRequest);
        frame.add(sendButton, BorderLayout.CENTER);

        resultArea = new JTextArea(6, 15);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 16));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(40, 40, 40));
        resultArea.setForeground(Color.WHITE);
        frame.add(new JScrollPane(resultArea), BorderLayout.SOUTH);

        frame.setSize(700, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JTextField createStyledTextField(JPanel panel, String label) {
        JLabel jLabel = new JLabel(label);
        jLabel.setForeground(Color.WHITE);
        JTextField textField = new JTextField();
        panel.add(jLabel);
        panel.add(textField);
        return textField;
    }

    private void sendRequest(ActionEvent e) {
        resultArea.setText("Processing...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                fetchRecommendation();
                return null;
            }
        };
        worker.execute();
    }

    private void fetchRecommendation() {
        try {
            int temperature = Integer.parseInt(tempField.getText());
            int occupancy = Integer.parseInt(occupancyField.getText());
            int powerUsage = Integer.parseInt(powerField.getText());
            boolean isPeakHours = peakHoursCheck.isSelected();
            String peakHoursStatus = isPeakHours ? "Yes" : "No";

            // Retrieve the user's access level from the login class
            String accessLevel = EnergyOptimizationLogin.getCurrentAccessLevel();
            String recommendationStyle = "";

            // Determine the recommendation style based on the user's access level
            if ("poor".equalsIgnoreCase(accessLevel)) {
                recommendationStyle = "cheap and efficient";
            } else if ("average".equalsIgnoreCase(accessLevel)) {
                recommendationStyle = "balanced between cost and performance";
            } else if ("rich".equalsIgnoreCase(accessLevel)) {
                recommendationStyle = "best performance regardless of cost";
            } else {
                recommendationStyle = "standard recommendation";
            }

            // Construct the prompt with the input values and recommendation style
            String prompt = String.format(
                    "User with access level '%s' requires recommendations that are %s. Conditions: Temperature: %d, Occupancy: %d, Power Usage: %dW, Peak Hours: %s. Please suggest the best HVAC mode and temperature.",
                    accessLevel, recommendationStyle, temperature, occupancy, powerUsage, peakHoursStatus
            );

            // Setup HTTP request
            String urlString = "http://localhost:11434/api/chat";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = String.format(
                    "{\"model\": \"mistral\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}",
                    prompt
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(line);
                                if (jsonNode.has("message") && jsonNode.get("message").has("content")) {
                                    response.append(jsonNode.get("message").get("content").asText()).append(" ");
                                }
                            } catch (Exception ex) {
                                response.append(" Failed to parse response: ").append(line);
                            }
                        }
                    }
                    resultArea.setText("Recommended Setting: " + response.toString().trim());
                }
            } else {
                resultArea.setText("Error: " + conn.getResponseCode());
            }
            conn.disconnect();
        } catch (Exception ex) {
            resultArea.setText("Error: " + ex.getMessage());
        }
    }
}
