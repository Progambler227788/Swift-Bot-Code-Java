import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class RestartUtility {

    public static void restartApplication() throws IOException {
        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        File currentJar;
		try {
		currentJar = new File(RestartUtility.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		

        /* Check if it's a jar file */
        if (!currentJar.getName().endsWith(".jar")) {
            System.err.println("Not a JAR file. Cannot restart the application.");
            return;
        }

        /* Build the command: java -jar application.jar */
        final ArrayList<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentJar.getPath());

        /* Start a new process with the specified command */
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.start();

        /* Exit the current process */
        System.exit(0);
    }
        catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static void main(String[] args) {
        try {
            restartApplication();
        } catch (IOException e) {
            System.err.println("Error restarting application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
