import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class EnergyOptimizationGUI {
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
        inputPanel.add(new JLabel(""));
        inputPanel.add(peakHoursCheck);

        frame.add(inputPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(30, 30, 30));

        JButton sendButton = new JButton("New");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.setBackground(new Color(0, 123, 255));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createLineBorder(new Color(0, 174, 255), 2));
        sendButton.setPreferredSize(new Dimension(80, 30));
        sendButton.addActionListener(this::sendRequest);

        buttonPanel.add(sendButton);
        frame.add(buttonPanel, BorderLayout.CENTER);

        resultArea = new JTextArea(5, 30);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 14));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(40, 40, 40));
        resultArea.setForeground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        frame.add(scrollPane, BorderLayout.SOUTH);

        frame.pack();
        frame.setSize(600, 300);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JTextField createStyledTextField(JPanel panel, String label) {
        JLabel jLabel = new JLabel(label);
        jLabel.setFont(new Font("Arial", Font.BOLD, 14));
        jLabel.setForeground(Color.WHITE);

        JTextField textField = new JTextField();
        textField.setFont(new Font("Arial", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        textField.setBackground(new Color(40, 40, 40));
        textField.setForeground(Color.WHITE);
        textField.setCaretColor(Color.WHITE); // Set caret color to white

        panel.add(jLabel);
        panel.add(textField);
        return textField;
    }

    private void sendRequest(ActionEvent e) {
        try {
            int temperature = Integer.parseInt(tempField.getText());
            int occupancy = Integer.parseInt(occupancyField.getText());
            int powerUsage = Integer.parseInt(powerField.getText());
            boolean isPeakHours = peakHoursCheck.isSelected();
            String peakHoursStatus = isPeakHours ? "Yes" : "No";

            String prompt = String.format("Temp: %d, Occupancy: %d, Power: %dW, Peak: %s. Best HVAC mode & temp?",
                    temperature, occupancy, powerUsage, peakHoursStatus);

            String urlString = "http://localhost:11434/api/chat";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = String.format("{\"model\": \"mistral\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}",
                    prompt);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
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
                br.close();
                resultArea.setText("Recommended Setting: " + response.toString().trim());
            } else {
                resultArea.setText("Error: " + conn.getResponseCode());
            }
            conn.disconnect();
        } catch (Exception ex) {
            resultArea.setText("Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EnergyOptimizationGUI::new);
    }
}
