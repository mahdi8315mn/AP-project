import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
        frame.getContentPane().setBackground(new Color(30, 30, 30));

        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        JTextField userField = new JTextField();

        JLabel passLabel = new JLabel("Access Level (rich, average, poor):");
        passLabel.setForeground(Color.WHITE);
        JTextField passField = new JTextField();

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String accessLevel = passField.getText().trim().toLowerCase();

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
        frame.add(passLabel);
        frame.add(passField);
        frame.add(new JLabel());
        frame.add(loginButton);

        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static class EnergyOptimizationGUI {
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

            tempField = new JTextField();
            occupancyField = new JTextField();
            powerField = new JTextField();
            peakHoursCheck = new JCheckBox("Peak Hours");

            inputPanel.add(new JLabel("Temperature:"));
            inputPanel.add(tempField);
            inputPanel.add(new JLabel("Occupancy:"));
            inputPanel.add(occupancyField);
            inputPanel.add(new JLabel("Power Usage (W):"));
            inputPanel.add(powerField);
            inputPanel.add(new JLabel(""));
            inputPanel.add(peakHoursCheck);

            frame.add(inputPanel, BorderLayout.NORTH);

            JButton sendButton = new JButton("Recommendation");
            sendButton.addActionListener(this::sendRequest);
            frame.add(sendButton, BorderLayout.CENTER);

            resultArea = new JTextArea(6, 50);
            resultArea.setFont(new Font("Arial", Font.PLAIN, 16));
            resultArea.setLineWrap(true);
            resultArea.setWrapStyleWord(true);
            resultArea.setEditable(false);
            resultArea.setBackground(new Color(40, 40, 40));
            resultArea.setForeground(Color.WHITE);
            JScrollPane scrollPane = new JScrollPane(resultArea);
            frame.add(scrollPane, BorderLayout.SOUTH);

            frame.setSize(600, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
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

                String prompt = String.format("Temp: %d, Occupancy: %d, Power: %dW, Peak: %s, Access Level: %s. Best HVAC mode & temp?",
                        temperature, occupancy, powerUsage, peakHoursStatus, currentAccessLevel);

                URL url = new URL("http://localhost:11434/api/chat");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                String payload = String.format("{\"model\": \"mistral\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}", prompt);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes("utf-8"));
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();
                resultArea.setText(response.toString().trim());
            } catch (Exception ex) {
                resultArea.setText("Error: " + ex.getMessage());
            }
        }
    }
}
