import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.Properties;

public class Terminal {

    static String token;
    static String Owner_id;
    static HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {

        try {

            Properties props = new Properties();
            FileInputStream fis = new FileInputStream("config.properties");
            props.load(fis);
            token = props.getProperty("telegram_token");
            Owner_id = props.getProperty("admin_id");
            fis.close();
            System.out.println("Config loaded successfully");

        } catch (Exception e) {
            System.err.println("CRITICAL: Could not load config.properties!");
            System.err.println("Make sure the file exists and has 'telegram_token' and 'admin_id'.");
            return;
        }

        int offset = 0;

        while (true) {
            try {

                URI uri = URI.create("https://api.telegram.org/bot" + token + "/getUpdates?offset=" + offset);
                HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String json = response.body();

                if (json.contains("update_id")) {
                    int idIndex = json.lastIndexOf("update_id");
                    String idStr = json.substring(idIndex + 11).split(",")[0].trim().replace(":","");
                    offset = Integer.parseInt(idStr) + 1;

                    int chatIndex = json.lastIndexOf("\"chat\":{\"id\":");
                    String chatId = "";
                    if (chatIndex != -1) {
                        chatId = json.substring(chatIndex + 13).split(",")[0];
                    }

                    int textIndex = json.lastIndexOf("\"text\":\"");
                    String command = "";
                    if (textIndex != -1) {
                        command = json.substring(textIndex + 8).split("\"")[0];
                    }

                    System.out.println("---> New Message from "+ chatId + ": " + command);

                    if (command.equals("/ping")) {
                        System.out.println("  [Action] System says PONG!");
                        sendMessage(Owner_id, "Pong!");
                    }
                    else if (command.equals("/date")) {
                        var formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM dd, HH:mm");
                        String date = java.time.LocalDateTime.now().format(formatter);
                        sendMessage(Owner_id, "Server Time: " + date);
                    }
                    else if (command.equals("/status")) {
                        if (chatId.equals(Owner_id)) {
                            System.out.println("  [Action] Running System Status....");

                            String result = executeCommand("echo '--- RAM ---'; free -h; echo ''; echo '--- UPTIME ---'; uptime");
                            sendMessage(Owner_id, result);
                        } else {
                            sendMessage(Owner_id, "⛔ Access Denied.");
                        }
                    }
                    else if (command.equals("/fastfetch")) {
                        if (chatId.equals(Owner_id)) {
                            String r = executeCommand("/usr/bin/fastfetch");
                            String result = r.replaceAll("\u001B\\[[;\\d]*[a-zA-Z]", "");
                            sendMessage(Owner_id, "<pre>" + result + "</pre>");
                        } else {
                            sendMessage(Owner_id, "⛔ Access Denied.");
                        }
                    }
                    else if (command.equals("/lock")) {
                        if (chatId.equals(Owner_id)) {
                            sendMessage(Owner_id, "Locking Session....");
                            executeCommand("loginctl lock-session");
                        } else {
                            sendMessage(Owner_id, "⛔ Access Denied.");
                        }
                    }
                    else if (command.equals("/sleep")) {
                        if (chatId.equals(Owner_id)) {
                            sendMessage(Owner_id, "Going to sleep...");
                            try { Thread.sleep(2000); } catch (Exception e) {}
                            executeCommand("systemctl suspend");
                        }
                    }
                    else if (command.equals("/start")) {
                        sendMenu(Owner_id);                    }
                    else {
                        System.out.println("   [Action] Unknown command.");
                    }
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

    public static void sendMessage( String Owner_id, String message ) {

        try {
            String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8");
            String url = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + Owner_id + "&text=" + encodedMessage + "&parse_mode=HTML";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String executeCommand(String cmd) {

        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", cmd);

            builder.redirectErrorStream(true);

            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
        if (output.length() == 0) return "Done. (No output.)";
        return output.toString();
    }

    public static void sendMenu(String Owner_id) {

        try {
            String text = "Welcome to Terminal. Select a command: ";
            String encodedText = java.net.URLEncoder.encode(text, "UTF-8");

            String jsonKeyboard = "{\"keyboard\":[[" +
                                "{\"text\":\"/status\"},{\"text\":\"/fastfetch\"}]," +
                                "[{\"text\":\"/lock\"},{\"text\":\"/sleep\"}]" +
                              "],\"resize_keyboard\":true}";

           String encodedKeyboard = java.net.URLEncoder.encode(jsonKeyboard, "UTF-8");
           String url = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + Owner_id + "&text=" + encodedText + "&reply_markup=" + encodedKeyboard;

           HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
           client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
