package main.analyzer;

import ch.bildspur.sonic.*;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.JavaSoundTarget;
import ch.fhnw.ether.audio.URLAudioSource;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import main.Main;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by cansik on 10/05/16.
 */
public class AnalyzerController {
    public Canvas visLeftLower;
    public Canvas visLeftUpper;
    public Canvas visRightUpper;
    public Canvas visRightLower;
    public Canvas visTable;
    public ProgressBar progressBar;
    public Label dataSetName;
    public TextField dataPointLabelLL;
    public TextArea tbConsole;
    public ComboBox cbAutoAlgorithm;

    LoopRingBuffer bufferLL;
    LoopRingBuffer bufferLU;
    LoopRingBuffer bufferRU;
    LoopRingBuffer bufferRL;

    ObservableList<String> algorithms;

    public void initialize() {
        Main.analyzeController = this;
        clearLog();
        clearTable();

        algorithms = FXCollections.observableArrayList();

        // add algorithms
        algorithms.add("threshold");
        algorithms.add("peek");
        algorithms.add("cross-correlation");
        algorithms.add("diagonal");

        cbAutoAlgorithm.setItems(algorithms);
        cbAutoAlgorithm.setValue(algorithms.get(0));
    }

    public void runAutoAlgorithm()
    {
        String algo = (String)cbAutoAlgorithm.getValue();
        switch (algo)
        {
            case "threshold":
                btnThreshold_Clicked(null);
                break;
            case "peek":
                btnPeek_Clicked(null);
                break;
            case "cross-correlation":
                btnCrossCorrelation_Clicked(null);
                break;
            case "diagonal":
                runDiagonal();
                break;
        }
    }

    public void runDiagonal()
    {
        DiagonalTDAO diag = new DiagonalTDAO();
        TDOAAnalyzer an = new TDOAAnalyzer();

        diag.delayAlgorithm = an::execCorrelation;

        diag.ll = bufferLL.getBuffer();
        diag.ul = bufferLU.getBuffer();
        diag.ur = bufferRU.getBuffer();
        diag.lr = bufferRL.getBuffer();

        diag.tableLength = 1.5;
        diag.tableWidth = 0.75;

        diag.canvas = visTable;

        diag.run();

        // draw center
        GraphicsContext gc = visTable.getGraphicsContext2D();
        // draw grid
        gc.setStroke(Color.DARKGRAY);
        gc.strokeLine(visTable.getWidth() / 2, 0, visTable.getWidth() / 2, visTable.getHeight());
        gc.strokeLine(0, visTable.getHeight() / 2, visTable.getWidth(), visTable.getHeight() / 2);

    }

    public void btnThreshold_Clicked(ActionEvent actionEvent) {
        float threshold = Main.inputController.getGestureRecognizer().getThreshold();
        //if(Main.inputController != null)
            //threshold = Main.inputController.getGestureRecognizer().getThreshold();

        TDOAAnalyzer an = new TDOAAnalyzer();
        runTDOAAnalyzing((a, b) -> (float) an.extendedThresholdAnalyzer(a, b, threshold));
    }

    public void btnPeek_Clicked(ActionEvent actionEvent) {
        TDOAAnalyzer an = new TDOAAnalyzer();
        runTDOAAnalyzing((a, b) -> (float)an.peekAnalyzer(a, b));
    }

    public void btnCrossCorrelation_Clicked(ActionEvent actionEvent) {
        TDOAAnalyzer an = new TDOAAnalyzer();
        //an.crossCorrelationBourke(a, b, a.length, 500)
        runTDOAAnalyzing((a, b) -> an.execCorrelation(a, b));
    }

    public void btnLoad_Clicked(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(((Node) actionEvent.getTarget()).getScene().getWindow());

        if (selectedDirectory == null) {
            System.out.println("No directory selected!");
        } else {
            loadData(selectedDirectory);
            //SwingUtilities.invokeLater(() -> loadData(selectedDirectory));
        }
    }

    void log(String message) {
        Platform.runLater(() -> {
            tbConsole.setText(tbConsole.getText() + "\n" + "> " + message);
            tbConsole.setScrollTop(Double.MAX_VALUE);
        });
    }

    void clearLog() {
        Platform.runLater(() -> {
            tbConsole.setText("Analyzer");
        });
    }

    double getPercentagePosition(float sonicSpeed, float samplingRate, float tableLength, float[] f, float[] g, Function2<float[], float[], Float> algorithm) {
        double delta = algorithm.apply(f, g);
        double fullTime = 1 / sonicSpeed * tableLength;
        double samplesForDistance = fullTime * samplingRate;
        double sampleWay = (samplesForDistance / 2) + delta;

        System.out.print("Table length (m): " + tableLength);
        System.out.print("\tDelta (smp): " + delta);
        System.out.print("\tFullTime (s): " + fullTime);
        System.out.print("\tSamples Full (smp): " + samplesForDistance);
        System.out.print("\tSamples Way (smp): " + sampleWay);
        System.out.println();

        return (sampleWay / samplesForDistance);
    }

    void updateProgress(double value) {
        Platform.runLater(() -> progressBar.setProgress(progressBar.getProgress() + value));
    }

    void resetProgress() {
        Platform.runLater(() -> progressBar.setProgress(0));
    }

    void loadData(File dir) {
        clearLog();
        log("Loading dataset '" + dir.getName() + "'...");

        for (File file : dir.listFiles()) {
            if (file.getName().equals("LL.wav"))
                bufferLL = loadWave(file);

            if (file.getName().equals("LU.wav"))
                bufferLU = loadWave(file);

            if (file.getName().equals("RU.wav"))
                bufferRU = loadWave(file);

            if (file.getName().equals("RL.wav"))
                bufferRL = loadWave(file);
        }

        // set mimium length of all
        int min = Integer.MAX_VALUE;

        if (min > bufferLL.size()) min = bufferLL.size();
        if (min > bufferLU.size()) min = bufferLU.size();
        if (min > bufferRU.size()) min = bufferRU.size();
        if (min > bufferRL.size()) min = bufferRL.size();

        // resize arrays
        bufferLL = new LoopRingBuffer(bufferLL, min);
        bufferLU = new LoopRingBuffer(bufferLU, min);
        bufferRU = new LoopRingBuffer(bufferRU, min);
        bufferRL = new LoopRingBuffer(bufferRL, min);

        drawAllBuffer(dir.getName());
    }

    void drawAllBuffer(String dataSet)
    {
        float max = 0;
        for (int i = 0; i < bufferLL.size(); i++) {
            if (max < bufferLL.get(i)) max = bufferLL.get(i);
            if (max < bufferLU.get(i)) max = bufferLU.get(i);
            if (max < bufferRU.get(i)) max = bufferRU.get(i);
            if (max < bufferRL.get(i)) max = bufferRL.get(i);
        }
        float gainFactor = 1.0f / max;

        // visualize data
        Platform.runLater(() -> {
            drawBuffer(bufferLL.getBuffer(), visLeftLower, Color.BLUE, gainFactor);
            drawBuffer(bufferLU.getBuffer(), visLeftUpper, Color.RED, gainFactor);
            drawBuffer(bufferRU.getBuffer(), visRightUpper, Color.GREEN, gainFactor);
            drawBuffer(bufferRL.getBuffer(), visRightLower, Color.ORANGE, gainFactor);

            dataSetName.setText(dataSet);

            clearTable();
        });
    }

    void clearTable() {
        GraphicsContext gc = visTable.getGraphicsContext2D();
        gc.clearRect(0, 0, visTable.getWidth(), visTable.getHeight());
        // draw border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, visTable.getWidth() - 2, visTable.getHeight() - 2);
    }

    void drawBuffer(float[] buffer, Canvas c, Color color) {
        drawBuffer(buffer, c, color, 1.0f);
    }

    void drawBuffer(float[] buffer, Canvas c, Color color, float gainFactor) {
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.clearRect(0, 0, c.getWidth(), c.getHeight());
        float space = (float) (c.getWidth() / buffer.length);

        gc.setFill(color);

        float y = (float) c.getHeight() / 2f;

        for (int i = 0; i < buffer.length - 1; i++) {
            float v = buffer[i];

            gc.fillOval(space * i, y + (y * v * gainFactor), 1, 1);
        }

        // draw border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, c.getWidth() - 2, c.getHeight() - 2);
    }

    LoopRingBuffer loadWave(File waveFile) {
        log("loading " + waveFile.getName() + "...");
        LoopRingBuffer lrb = null;

        try {
            // create source
            URLAudioSource source = new URLAudioSource(waveFile.toURL(), 1);
            long time = (long) Math.ceil(source.getLengthInSeconds() * 1000);
            AudioBufferReader bufferReader = new AudioBufferReader();
            RenderProgram<IAudioRenderTarget> program = new RenderProgram<>(source, bufferReader);

            // run audio
            IAudioRenderTarget target = new JavaSoundTarget();
            target.useProgram(program);

            target.start();
            target.sleepUntil(IScheduler.NOT_RENDERING);
            target.stop();

            //read buffer
            lrb = new LoopRingBuffer(bufferReader.getBuffer());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (RenderCommandException e) {
            e.printStackTrace();
        }

        System.out.println("loaded!");

        return lrb;
    }

    public void loadBuffer(LoopRingBuffer bufferLL, LoopRingBuffer bufferLU, LoopRingBuffer bufferRU, LoopRingBuffer bufferRL) {
        // todo: maybe memory overwrite problem
        this.bufferLL = bufferLL;
        this.bufferLU = bufferLU;
        this.bufferRU = bufferRU;
        this.bufferRL = bufferRL;

        log("loaded live input buffer (" + bufferLL.size() + ")");

        // draw visualisation
        drawAllBuffer("LIVE");
    }

    public void visMouse_Moved(MouseEvent event) {
        Canvas c = (Canvas) event.getSource();
        if (bufferLL != null) {
            int i = (int) (event.getX() / c.getWidth() * bufferLL.size());
            //System.out.println(event.getX() + ": " + bufferLL.get(i));
            dataPointLabelLL.setText(String.format("%f", bufferLL.get(i)));
        }
    }

    void runTDOAAnalyzing(Function2<float[], float[], Float> algorithm) {
        float[] f = bufferLL.getBuffer();
        float[] g = bufferLU.getBuffer();
        float[] h = bufferRU.getBuffer();
        float[] k = bufferRL.getBuffer();

        // prepare params
        float sonicSpeed = 343.2f; // m/s
        float samplingRate = 96000; // hz (iphone: 44100)

        float tableLength = 1.50f; // m (iphone: 2)
        float tableWidth = 0.75f; // m (iphone: 1)
        float tableDiag = (float)Math.sqrt(Math.pow(tableLength, 2) + Math.pow(tableWidth, 2)); // m (iphone sqrt(5))

        // calculate path percentage
        double leftPer = getPercentagePosition(sonicSpeed, samplingRate, tableWidth, f, g, algorithm);
        double rightPer = getPercentagePosition(sonicSpeed, samplingRate, tableWidth, k, h, algorithm);
        double topPer = getPercentagePosition(sonicSpeed, samplingRate, tableLength, g, h, algorithm);
        double bottomPer = getPercentagePosition(sonicSpeed, samplingRate, tableLength, f, k, algorithm);

        double diagnoal1 = getPercentagePosition(sonicSpeed, samplingRate, tableDiag, f, h, algorithm);
        double diagnoal2 = getPercentagePosition(sonicSpeed, samplingRate, tableDiag, g, k, algorithm);

        // draw result
        GraphicsContext gc = visTable.getGraphicsContext2D();
        gc.clearRect(0, 0, visTable.getWidth(), visTable.getHeight());

        // draw lines
        double width = visTable.getWidth();
        double height = visTable.getHeight();

        double size = 10;
        double hs = size / 2;

        // draw grid
        gc.setStroke(Color.DARKGRAY);
        gc.strokeLine(width / 2, 0, width / 2, height);
        gc.strokeLine(0, height / 2, width, height / 2);

        // left + top
        gc.setStroke(Color.BLUE);
        gc.strokeOval(width * topPer - hs, height * leftPer - hs, size, size);

        // left + bottom
        gc.setStroke(Color.RED);
        gc.strokeOval(width * bottomPer - hs, height * leftPer - hs, size, size);

        // right + top
        gc.setStroke(Color.CYAN);
        gc.strokeOval(width * topPer - hs, height * rightPer - hs, size, size);

        // right + bottom
        gc.setStroke(Color.ORANGE);
        gc.strokeOval(width * bottomPer - hs, height * rightPer - hs, size, size);

        // diagonal 1
        gc.setStroke(Color.MAGENTA);
        gc.strokeOval(width * diagnoal1 - hs, height * diagnoal1 - hs, size, size);

        // diagonal 2
        gc.setStroke(Color.LIMEGREEN);
        gc.strokeOval(width * diagnoal2 - hs, height * diagnoal2 - hs, size, size);

        // calculate center point
        double meanX = (width * topPer + width * bottomPer) / 2; //+ width * diagnoal1 + width * diagnoal2) / 4;
        double meanY = (height * rightPer + height * leftPer) / 2; // + height * diagnoal1 + height * diagnoal2) / 4;

        log("P: (" + meanX + "|" + meanY + ")");

        // draw arrow
        gc.setStroke(Color.BLUE);
        gc.strokeLine(meanX, meanY, width / 2, height / 2);

        // draw center
        gc.setStroke(Color.GOLD);
        gc.strokeOval(meanX - hs, meanY - hs, size, size);

        // draw border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, width - 2, height - 2);

        analyzeResult(meanX, meanY);
    }

    void analyzeResult(double x, double y) {
        double width = visTable.getWidth();
        double height = visTable.getHeight();

        Vector2d prediction = new Vector2d(x, y);

        // get fixpoints
        Map<String, Vector2d> fixPoints = new HashMap<>();
        fixPoints.put("center", new Vector2d(width / 2, height / 2));

        fixPoints.put("lower left", new Vector2d(0, height));
        fixPoints.put("upper left", new Vector2d(0, 0));
        fixPoints.put("upper right", new Vector2d(width, 0));
        fixPoints.put("lower right", new Vector2d(width, height));

        fixPoints.put("center left", new Vector2d(0, height / 2));
        fixPoints.put("center top", new Vector2d(width / 2, 0));
        fixPoints.put("center right", new Vector2d(width, height / 2));
        fixPoints.put("center bottom", new Vector2d(width / 2, height));

        // anaylze
        double minDistance = Double.MAX_VALUE;
        String minKey = "None";

        System.out.println("---");
        for (String key : fixPoints.keySet()) {
            Vector2d v = fixPoints.get(key);
            double distance = prediction.distance(v);

            System.out.println(key + ": " + distance);

            if (distance < minDistance) {
                minKey = key;
                minDistance = distance;
            }
        }

        // output result
        log("Prediction: " + minKey + " (" + minDistance + ")");
    }

    public void btnLoadJSON_Clicked(ActionEvent actionEvent) {
        // load data
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON Files", "*.json");
        fileChooser.getExtensionFilters().add(extFilter);

        File selectedFile = fileChooser.showOpenDialog(((Node) actionEvent.getTarget()).getScene().getWindow());

        if(selectedFile == null)
        {
            System.out.println("no file selected");
            return;
        }

        JSONObject root = null;

        // read file and load it into buffer
        try {
            String content = new String(Files.readAllBytes(selectedFile.toPath()));
            root = (JSONObject)new JSONParser().parse(content);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        System.out.println("Gain: " + (float)(double)root.get("gain"));
        System.out.println("Threshold: " + (float)(double)root.get("threshold"));

        Main.inputController.setGain((float)(double)root.get("gain"));
        Main.inputController.getGestureRecognizer().setThreshold((float)(double)root.get("threshold"));

        //read data into buffers
        JSONObject data = (JSONObject)root.get("data");
        JSONArray dataLL = (JSONArray)data.get("LL");
        JSONArray dataLU = (JSONArray)data.get("LU");
        JSONArray dataRU = (JSONArray)data.get("RU");
        JSONArray dataRL = (JSONArray)data.get("RL");

        bufferLL = new LoopRingBuffer(dataLL.size());
        bufferLU = new LoopRingBuffer(dataLU.size());
        bufferRU = new LoopRingBuffer(dataRU.size());
        bufferRL = new LoopRingBuffer(dataRL.size());

        for(int i = 0; i < dataLL.size(); i++)
        {
            bufferLL.put((float)(double)dataLL.get(i));
            bufferLU.put((float)(double)dataLU.get(i));
            bufferRU.put((float)(double)dataRU.get(i));
            bufferRL.put((float)(double)dataRL.get(i));
        }

        drawAllBuffer(selectedFile.getName());
    }

    public void btnSaveJSON_Clicked(ActionEvent actionEvent) {
        String sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

        // save data and algorithm results
        JSONObject root = new JSONObject();
        root.put("timestamp", sdf.toString());
        root.put("gain", Main.inputController.getGestureRecognizer().getGain());
        root.put("threshold", Main.inputController.getGestureRecognizer().getThreshold());

        JSONObject data = new JSONObject();

        JSONArray dataLL = new JSONArray();
        JSONArray dataLU = new JSONArray();
        JSONArray dataRU = new JSONArray();
        JSONArray dataRL = new JSONArray();

        for(int i = 0; i < bufferLL.size(); i++)
        {
            dataLL.add(bufferLL.get(i));
            dataLU.add(bufferLU.get(i));
            dataRU.add(bufferRU.get(i));
            dataRL.add(bufferRL.get(i));
        }

        data.put("LL", dataLL);
        data.put("LU", dataLU);
        data.put("RU", dataRU);
        data.put("RL", dataRL);

        root.put("data", data);

        // get filename
        TextInputDialog dialog = new TextInputDialog(cbAutoAlgorithm.getValue() + "_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        dialog.setTitle("Experiment Name");
        dialog.setHeaderText("To save the experiment, we need a name!");
        dialog.setContentText("Please enter your experiment name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            // save file to disk
            try {
                try (FileWriter file = new FileWriter("results/" + name + ".json")) {
                    file.write(root.toJSONString());
                    System.out.println("saved " + name + ".json");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void btnDavidInvertedWaveLocalization_Clicked(ActionEvent actionEvent) {
        DIWLAlgorithm diwl = new DIWLAlgorithm();

    }


    public void btnRunAlgo_Clicked(ActionEvent actionEvent) {
        Platform.runLater(() -> runAutoAlgorithm());
    }
}
