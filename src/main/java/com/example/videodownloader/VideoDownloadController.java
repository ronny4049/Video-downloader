import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Controller
public class VideoDownloadController {

    @PostMapping("/download")
    public String downloadVideo(@RequestParam("url") String url, Model model) {
        if (url == null || url.isEmpty()) {
            model.addAttribute("status", "URL cannot be empty.");
            return "result"; // Return the result view with an error message
        }

        String status = downloadFromUrl(url);
        model.addAttribute("status", status);
        return "result"; // Return the result view
    }

    private String downloadFromUrl(String url) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("yt-dlp", url);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            Process process = processBuilder.start();

            // Read the output from the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Wait for the process to finish and get the exit code
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return "Video downloaded successfully from " + url + "!";
            } else {
                return "Failed to download video from " + url + ". Output:\n" + output.toString();
            }
        } catch (Exception e) {
            return "Error occurred while downloading the video: " + e.getMessage();
        }
    }
}

