import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PacketProgrammerLauncher extends ExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("PacketProgrammer.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("PacketProgrammer");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, PacketProgrammerLauncher.class);
    }
}