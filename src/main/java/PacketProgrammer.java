import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityUpdate;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;

@ExtensionInfo(
        Title = "PacketsProgrammer",
        Description = "Program with packets",
        Version = "1.0",
        Author = "DitamiDigo"
)

public class PacketProgrammer extends ExtensionForm {
    public int yourIndex = -1;
    public String yourName;
    public Text userupdate;
    public Text moveavatar;
    public Text usefurniture;
    public TableView<PacketInfo> programmer;
    public TableColumn<PacketInfo, String> docolumn;
    public TableColumn<PacketInfo, String> ifcolumn;
    public CheckBox capture;
    public CheckBox activate;
    public Button buttonclear;
    public Text sliderobjectbundle;
    public ListView<String> presetsview;
    public Button buttonsave;
    public Text objectdataupdate;
    public CheckBox statusactive;
    public TextField statusvalue;
    public Text enteronewaydoor;
    public Text wiredmovements;
    public TextField namepreset;
    public CheckBox always_on_top_cbx;
    private int userupdateX = -1, userupdateY = -1;
    private int moveavatarX = -1, moveavatarY = -1;
    private int slideobjectbundleX = -1, slideobjectbundleY = -1;
    private int slideobjectbundleID = -1;
    private int wiredmovementsX = -1, wiredmovementsY = -1;
    private int wiredmovementsID = -1;
    private boolean idReady = false;
    private String presetsFolder;

    @Override
    protected void initExtension() {
        presetsFolder = presetPath();

        File folder = new File(presetsFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        presetsview.getItems().clear();

        File presetsFolder = new File(presetPath());
        File[] presetFiles = presetsFolder.listFiles();

        if (presetFiles != null) {
            for (File presetFile : presetFiles) {
                if (presetFile.isFile() && presetFile.getName().endsWith(".json")) {
                    String presetName = presetFile.getName().replace(".json", "");
                    presetsview.getItems().add(presetName);
                }
            }
        }

        presetsview.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedPreset = presetsview.getSelectionModel().getSelectedItem();
                if (selectedPreset != null) {
                    loadPreset(selectedPreset);
                }
            }
        });

        sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));
        intercept(HMessage.Direction.TOCLIENT, "UserObject", this::inUserObject);
        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", this::outMoveAvatar);
        intercept(HMessage.Direction.TOSERVER, "UseFurniture", this::outUseFurniture);
        intercept(HMessage.Direction.TOSERVER, "EnterOneWayDoor", this::outEnterOneWayDoor);
        intercept(HMessage.Direction.TOCLIENT, "Users", this::inUsers);
        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", this::inUserUpdate);
        intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", this::InSlideObjectBundle);
        intercept(HMessage.Direction.TOCLIENT, "ObjectDataUpdate", this::InObjectDataUpdate);
        intercept(HMessage.Direction.TOCLIENT, "ObjectsDataUpdate", this::InObjectsDataUpdate);
        intercept(HMessage.Direction.TOCLIENT, "WiredMovements", this::InWiredMovements);

        Platform.runLater(this::setupTable);
    }

    public void toggleAlwaysOnTop() {
        primaryStage.setAlwaysOnTop(always_on_top_cbx.isSelected());
    }

    private void InWiredMovements(HMessage hMessage) {
        if (activate.isSelected()) {
            int count = hMessage.getPacket().readInteger();
            for (int i = 0; i < count; i++) {
                try {
                    hMessage.getPacket().readInteger();
                    hMessage.getPacket().readInteger();
                    hMessage.getPacket().readInteger();
                    int x = hMessage.getPacket().readInteger();
                    int y = hMessage.getPacket().readInteger();
                    hMessage.getPacket().readString();
                    hMessage.getPacket().readString();
                    int idWiredMoving = hMessage.getPacket().readInteger();
                    hMessage.getPacket().readInteger();
                    hMessage.getPacket().readInteger();
                    handlePacket(x, y, idWiredMoving, false);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void outEnterOneWayDoor(HMessage hMessage) {
        if (capture.isSelected()) {
            HPacket packet = hMessage.getPacket();
            hMessage.setBlocked(true);
            Platform.runLater(() -> {
                if (enteronewaydoor.getText().equals("Wait...")) {
                    int furnitureId = packet.readInteger();
                    enteronewaydoor.setText("EnterOneWayDoor");
                    PacketInfo packetInfo = new PacketInfo();
                    packetInfo.setDocolumn("EnterOneWayDoor ID: " + furnitureId);

                    for (PacketInfo info : programmer.getItems()) {
                        if (info.getDocolumn() == null) {
                            info.setDocolumn(packetInfo.getDocolumn());
                            programmer.refresh();
                            return;
                        }
                    }
                    programmer.getItems().add(packetInfo);
                    programmer.refresh();
                }
            });
        }
    }

    private void InObjectDataUpdate(HMessage hMessage) {
        if (activate.isSelected()) {
            String furnitureid = hMessage.getPacket().readString();
            hMessage.getPacket().readInteger();
            String status = hMessage.getPacket().readString();
            handlePacket(furnitureid, status);
        }
    }

    private void InObjectsDataUpdate(HMessage hMessage) {
        if (activate.isSelected()) {
            int count = hMessage.getPacket().readInteger();
            for (int i = 0; i < count; i++) {
                int furnitureId = hMessage.getPacket().readInteger();
                hMessage.getPacket().readInteger();
                String status = hMessage.getPacket().readString();
                handlePacket(String.valueOf(furnitureId), status);
            }
        }
    }

    private void handlePacket(String furnitureid, String status) {
        boolean allPreviousDone = true;
        for (PacketInfo packetInfo : programmer.getItems()) {
            if (!allPreviousDone) {
                break;
            }

            try {
                if (packetInfo.isDone()) {
                    continue;
                }

                String ifColumnValue = packetInfo.getIfcolumn();
                if (ifColumnValue == null) {
                    continue;
                }

                boolean conditionMet = false;
                if (ifColumnValue.startsWith("ObjectDataUpdate ID: ")) {
                    String expectedFurnitureId;
                    String expectedStatus = null;
                    if (ifColumnValue.contains(" Status: ")) {
                        expectedFurnitureId = ifColumnValue.substring("ObjectDataUpdate ID: ".length(), ifColumnValue.indexOf(" Status: "));
                        expectedStatus = ifColumnValue.substring(ifColumnValue.indexOf(" Status: ") + " Status: ".length());
                    } else {
                        expectedFurnitureId = ifColumnValue.substring("ObjectDataUpdate ID: ".length());
                    }

                    if (furnitureid.equals(expectedFurnitureId) && (expectedStatus == null || status.equals(expectedStatus))) {
                        conditionMet = true;
                    }
                }

                if (conditionMet) {
                    executeDoColumn(packetInfo);
                    packetInfo.setDone(true);
                    break;
                } else {
                    allPreviousDone = false;
                }
            } catch (Exception e) {
                System.out.println("Unexpected error parsing condition: " + e.getMessage());
            }
        }
    }

    public void handlesave() {
        String presetName = namepreset.getText().trim();
        if (presetName.isEmpty()) {
            return;
        }
        savePreset(presetName);
    }

    private void savePreset(String presetName) {
        JSONArray packetArray = new JSONArray();
        for (PacketInfo packetInfo : programmer.getItems()) {
            JSONObject packetObject = new JSONObject();
            packetObject.put("ifcolumn", packetInfo.getIfcolumn());
            packetObject.put("docolumn", packetInfo.getDocolumn());
            packetArray.put(packetObject);
        }

        JSONObject preset = new JSONObject();
        preset.put("packets", packetArray);

        try (FileWriter file = new FileWriter(presetsFolder + File.separator + presetName + ".json")) {
            file.write(preset.toString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Platform.runLater(() -> {
            if (!presetsview.getItems().contains(presetName)) {
                presetsview.getItems().add(presetName);
            }
        });
    }

    private void loadPreset(String presetName) {
        try (FileReader reader = new FileReader(presetsFolder + File.separator + presetName + ".json")) {
            StringBuilder jsonBuilder = new StringBuilder();
            int i;
            while ((i = reader.read()) != -1) {
                jsonBuilder.append((char) i);
            }

            JSONObject preset = new JSONObject(jsonBuilder.toString());
            JSONArray packetArray = preset.getJSONArray("packets");

            programmer.getItems().clear();
            for (int j = 0; j < packetArray.length(); j++) {
                JSONObject packetObject = packetArray.getJSONObject(j);
                PacketInfo packetInfo = new PacketInfo();
                packetInfo.setIfcolumn(packetObject.getString("ifcolumn"));
                packetInfo.setDocolumn(packetObject.getString("docolumn"));
                programmer.getItems().add(packetInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String presetPath() {
        try {
            String path = (new File(PacketProgrammer.class.getProtectionDomain().getCodeSource().getLocation().toURI()))
                    .getParentFile().toString();
            return Paths.get(path, "presets").toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void handlePacket(int x, int y, int furnitureId, boolean isUserUpdate) {
        boolean allPreviousDone = true;
        for (PacketInfo packetInfo : programmer.getItems()) {
            if (!allPreviousDone) {
                break;
            }

            try {
                if (packetInfo.isDone()) {
                    continue;
                }

                String ifColumnValue = packetInfo.getIfcolumn();
                if (ifColumnValue == null) {
                    continue;
                }

                boolean conditionMet = false;
                if (isUserUpdate) {
                    if (ifColumnValue.startsWith("UserUpdate X: ")) {
                        String[] ifCoords = ifColumnValue.replace("UserUpdate X: ", "").split(" Y: ");
                        if (ifCoords.length != 2) {
                            continue;
                        }
                        int ifX = Integer.parseInt(ifCoords[0]);
                        int ifY = Integer.parseInt(ifCoords[1]);

                        if (ifX == x && ifY == y) {
                            conditionMet = true;
                        }
                    }
                } else {
                    if (ifColumnValue.startsWith("SlideObjectBundle ID: ")) {
                        String[] ifParts = ifColumnValue.replace("SlideObjectBundle ID: ", "").split(" X: | Y: ");
                        if (ifParts.length != 3) {
                            continue;
                        }
                        int ifId = Integer.parseInt(ifParts[0]);
                        int ifX = Integer.parseInt(ifParts[1]);
                        int ifY = Integer.parseInt(ifParts[2]);

                        if (ifX == x && ifY == y && ifId == furnitureId) {
                            conditionMet = true;
                        }
                    } else if (ifColumnValue.startsWith("WiredMovements ID: ")) {
                        String[] ifParts = ifColumnValue.replace("WiredMovements ID: ", "").split(" X: | Y: ");
                        if (ifParts.length != 3) {
                            continue;
                        }
                        int ifId = Integer.parseInt(ifParts[0]);
                        int ifX = Integer.parseInt(ifParts[1]);
                        int ifY = Integer.parseInt(ifParts[2]);

                        if (ifX == x && ifY == y && ifId == furnitureId) {
                            conditionMet = true;
                        }
                    }
                }

                if (conditionMet) {
                    executeDoColumn(packetInfo);
                    packetInfo.setDone(true);
                    break;
                } else {
                    allPreviousDone = false;
                }
            } catch (NumberFormatException e) {
                System.out.println("Error parsing integer coordinates: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected error parsing coordinates: " + e.getMessage());
            }
        }
    }

    private void executeDoColumn(PacketInfo packetInfo) {
        try {
            String doColumnValue = packetInfo.getDocolumn();
            if (doColumnValue == null) {
                return;
            }
            if (doColumnValue.startsWith("MoveAvatar X: ")) {
                String[] doCoords = doColumnValue.replace("MoveAvatar X: ", "").split(" Y: ");
                if (doCoords.length != 2) {
                    return;
                }
                int doX = Integer.parseInt(doCoords[0]);
                int doY = Integer.parseInt(doCoords[1]);
                sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, doX, doY));
            } else if (doColumnValue.startsWith("UseFurniture ID: ")) {
                int id = Integer.parseInt(doColumnValue.replace("UseFurniture ID: ", ""));
                sendToServer(new HPacket("UseFurniture", HMessage.Direction.TOSERVER, id, 0));
            } else if (doColumnValue.startsWith("EnterOneWayDoor ID: ")) {
                int id = Integer.parseInt(doColumnValue.replace("EnterOneWayDoor ID: ", ""));
                sendToServer(new HPacket("EnterOneWayDoor", HMessage.Direction.TOSERVER, id));
            }
            packetInfo.setDone(true);
            programmer.refresh();
        } catch (NumberFormatException e) {
            System.out.println("Error parsing do column integer: " + e.getMessage());
        }
    }

    private void inUserObject(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        packet.readInteger();
        yourName = packet.readString();
    }

    private void inUserUpdate(HMessage hMessage) {
        if (activate.isSelected()) {
            try {
                HPacket packet = hMessage.getPacket();
                for (HEntityUpdate entityUpdate : HEntityUpdate.parse(packet)) {
                    int currentIndex = entityUpdate.getIndex();
                    if (yourIndex == currentIndex) {
                        int x = entityUpdate.getMovingTo().getX();
                        int y = entityUpdate.getMovingTo().getY();
                        handlePacket(x, y, -1, true);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void InSlideObjectBundle(HMessage hMessage) {
        if (activate.isSelected()) {
            hMessage.getPacket().readInteger();
            hMessage.getPacket().readInteger();
            int x = hMessage.getPacket().readInteger();
            int y = hMessage.getPacket().readInteger();
            hMessage.getPacket().readInteger();
            int furnitureId = hMessage.getPacket().readInteger();
            handlePacket(x, y, furnitureId, false);
        }
    }

    private void outMoveAvatar(HMessage hMessage) {
        if (capture.isSelected()) {
            HPacket packet = hMessage.getPacket();
            hMessage.setBlocked(true);
            Platform.runLater(() -> {
                if (moveavatar.getText().equals("Wait...")) {
                    moveavatarX = packet.readInteger();
                    moveavatarY = packet.readInteger();
                    moveavatar.setText("MoveAvatar");
                    PacketInfo packetInfo = new PacketInfo();
                    packetInfo.setDocolumn("MoveAvatar X: " + moveavatarX + " Y: " + moveavatarY);

                    for (PacketInfo info : programmer.getItems()) {
                        if (info.getDocolumn() == null) {
                            info.setDocolumn(packetInfo.getDocolumn());
                            programmer.refresh();
                            return;
                        }
                    }
                    programmer.getItems().add(packetInfo);
                    programmer.refresh();
                } else if (sliderobjectbundle.getText().equals("Wait...") && idReady) {
                    slideobjectbundleX = packet.readInteger();
                    slideobjectbundleY = packet.readInteger();
                    sliderobjectbundle.setText("SlideObjectBundle");
                    idReady = false;
                    PacketInfo packetInfo = new PacketInfo();
                    packetInfo.setIfcolumn("SlideObjectBundle ID: " + slideobjectbundleID + " X: " + slideobjectbundleX + " Y: " + slideobjectbundleY);

                    for (PacketInfo info : programmer.getItems()) {
                        if (info.getIfcolumn() == null) {
                            info.setIfcolumn(packetInfo.getIfcolumn());
                            programmer.refresh();
                            return;
                        }
                    }
                    programmer.getItems().add(packetInfo);
                    programmer.refresh();
                } else if (wiredmovements.getText().equals("Wait...") && idReady) {
                    wiredmovementsX = packet.readInteger();
                    wiredmovementsY = packet.readInteger();
                    wiredmovements.setText("WiredMovements");
                    idReady = false;
                    PacketInfo packetInfo = new PacketInfo();
                    packetInfo.setIfcolumn("WiredMovements ID: " + wiredmovementsID + " X: " + wiredmovementsX + " Y: " + wiredmovementsY);

                    for (PacketInfo info : programmer.getItems()) {
                        if (info.getIfcolumn() == null) {
                            info.setIfcolumn(packetInfo.getIfcolumn());
                            programmer.refresh();
                            return;
                        }
                    }
                    programmer.getItems().add(packetInfo);
                    programmer.refresh();
                } else if (userupdate.getText().equals("Wait...")) {
                    userupdateX = packet.readInteger();
                    userupdateY = packet.readInteger();
                    userupdate.setText("UserUpdate");
                    PacketInfo packetInfo = new PacketInfo();
                    packetInfo.setIfcolumn("UserUpdate X: " + userupdateX + " Y: " + userupdateY);

                    for (PacketInfo info : programmer.getItems()) {
                        if (info.getIfcolumn() == null) {
                            info.setIfcolumn(packetInfo.getIfcolumn());
                            programmer.refresh();
                            return;
                        }
                    }
                    programmer.getItems().add(packetInfo);
                    programmer.refresh();
                }
            });
        }
    }

    private void outUseFurniture(HMessage hMessage) {
        if (capture.isSelected()) {
            HPacket packet = hMessage.getPacket();
            hMessage.setBlocked(true);
            Platform.runLater(() -> {
                if (usefurniture.getText().equals("Wait...")) {
                    int furnitureId = packet.readInteger();
                    usefurniture.setText("UseFurniture");
                    PacketInfo packetInfo = new PacketInfo();
                    packetInfo.setDocolumn("UseFurniture ID: " + furnitureId);

                    for (PacketInfo info : programmer.getItems()) {
                        if (info.getDocolumn() == null) {
                            info.setDocolumn(packetInfo.getDocolumn());
                            programmer.refresh();
                            return;
                        }
                    }
                    programmer.getItems().add(packetInfo);
                    programmer.refresh();
                } else if (objectdataupdate.getText().equals("Wait...")) {
                    int furnitureId = packet.readInteger();
                    objectdataupdate.setText("ObjectDataUpdate");
                    PacketInfo packetInfo = new PacketInfo();
                    String columnText = "ObjectDataUpdate ID: " + furnitureId;

                    if (statusactive.isSelected()) {
                        columnText += " Status: " + statusvalue.getText();
                    }

                    packetInfo.setIfcolumn(columnText);

                    for (PacketInfo info : programmer.getItems()) {
                        if (info.getIfcolumn() == null) {
                            info.setIfcolumn(packetInfo.getIfcolumn());
                            programmer.refresh();
                            return;
                        }
                    }
                    programmer.getItems().add(packetInfo);
                    programmer.refresh();
                } else if (sliderobjectbundle.getText().equals("Wait...")) {
                    slideobjectbundleID = packet.readInteger();
                    idReady = true;
                } else if (wiredmovements.getText().equals("Wait...")) {
                    wiredmovementsID = packet.readInteger();
                    idReady = true;
                }
            });
        }
    }

    private void inUsers(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        HEntity[] roomUsersList = HEntity.parse(packet);
        for (HEntity entity : roomUsersList) {
            if (entity.getName().equals(yourName)) {
                yourIndex = entity.getIndex();
            }
        }
    }

    public void handleclear() {
        Platform.runLater(() -> programmer.getItems().clear());
    }

    private void setupTable() {
        docolumn.setCellValueFactory(new PropertyValueFactory<>("docolumn"));
        ifcolumn.setCellValueFactory(new PropertyValueFactory<>("ifcolumn"));

        programmer.setRowFactory(tv -> new TableRow<PacketInfo>() {
            {
                itemProperty().addListener((obs, oldItem, newItem) -> {
                    if (newItem != null && newItem.isDone()) {
                        int rowIndex = getIndex();
                        getTableView().getSelectionModel().select(rowIndex);
                    }
                });
            }
        });

        programmer.setOnDragOver(event -> {
            if (event.getGestureSource() != programmer && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        programmer.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String text = db.getString();
                switch (text) {
                    case "UserUpdate":
                        userupdate.setText("Wait...");
                        break;
                    case "MoveAvatar":
                        moveavatar.setText("Wait...");
                        break;
                    case "UseFurniture":
                        usefurniture.setText("Wait...");
                        break;
                    case "SlideObjectBundle":
                        sliderobjectbundle.setText("Wait...");
                        break;
                    case "ObjectDataUpdate":
                        objectdataupdate.setText("Wait...");
                        break;
                    case "EnterOneWayDoor":
                        enteronewaydoor.setText("Wait...");
                        break;
                    case "WiredMovements":
                        wiredmovements.setText("Wait...");
                        break;
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        userupdate.setOnDragDetected(event -> {
            Dragboard db = userupdate.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("UserUpdate");
            db.setContent(content);
            event.consume();
        });

        moveavatar.setOnDragDetected(event -> {
            Dragboard db = moveavatar.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("MoveAvatar");
            db.setContent(content);
            event.consume();
        });

        usefurniture.setOnDragDetected(event -> {
            Dragboard db = usefurniture.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("UseFurniture");
            db.setContent(content);
            event.consume();
        });

        sliderobjectbundle.setOnDragDetected(event -> {
            Dragboard db = sliderobjectbundle.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("SlideObjectBundle");
            db.setContent(content);
            event.consume();
        });

        objectdataupdate.setOnDragDetected(event -> {
            Dragboard db = objectdataupdate.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("ObjectDataUpdate");
            db.setContent(content);
            event.consume();
        });

        enteronewaydoor.setOnDragDetected(event -> {
            Dragboard db = enteronewaydoor.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("EnterOneWayDoor");
            db.setContent(content);
            event.consume();
        });

        wiredmovements.setOnDragDetected(event -> {
            Dragboard db = wiredmovements.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("WiredMovements");
            db.setContent(content);
            event.consume();
        });
    }
}