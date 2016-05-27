package main.analyzer;

import ch.bildspur.sonic.*;
import ch.bildspur.sonic.filter.MedianFilter;
import ch.bildspur.sonic.ltm.OneChannelLTM;
import ch.bildspur.sonic.ltm.util.DSP;
import ch.bildspur.sonic.tdao.BaseTDAO;
import ch.bildspur.sonic.tdao.DIWLAlgorithm;
import ch.bildspur.sonic.tdao.DiagonalTDAO;
import ch.bildspur.sonic.tdao.LinearTDAO;
import ch.bildspur.sonic.util.geometry.Vector2;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.JavaSoundTarget;
import ch.fhnw.ether.audio.URLAudioSource;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.util.Pair;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import main.Main;
import marf.FeatureExtraction.FeatureExtractionException;
import marf.Preprocessing.PreprocessingException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import sun.audio.AudioData;
import sun.audio.AudioDataStream;
import sun.audio.AudioPlayer;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by cansik on 10/05/16.
 */
public class AnalyzerController {
    public Canvas visLeftLower;
    public Canvas visLeftUpper;
    public Canvas visRightUpper;
    public Canvas visRightLower;
    public Canvas visTable;
    public Label dataSetName;
    public TextField dataPointLabelLL;
    public TextArea tbConsole;
    public ComboBox cbAutoAlgorithm;
    public ComboBox cbLagDetection;

    LoopRingBuffer bufferLL;
    LoopRingBuffer bufferLU;
    LoopRingBuffer bufferRU;
    LoopRingBuffer bufferRL;

    ObservableList<String> lagDetectionAlgorithms;
    ObservableList<String> algorithms;

    public static float SONIC_SPEED =  343.2f; //3960; //343.2f; // m/s
    public static float SAMPLING_RATE = 44100; //96000; // hz

    OneChannelLTM oneLTM = new OneChannelLTM();

    boolean isZoomed = false;

    float lastGain = 1.0f;

    public Vector2 lastPoint = Vector2.NULL;
    public String lastPrediction = "none";

    Map<String, Vector2d> fixPoints;

    public void initialize() {
        Main.analyzeController = this;
        clearLog();
        clearTable();

        double width = visTable.getWidth();
        double height = visTable.getHeight();

        // get fixpoints
        fixPoints = new HashMap<>();
        fixPoints.put("center", new Vector2d(width / 2d, height / 2d));

        fixPoints.put("lower left", new Vector2d(0, height));
        fixPoints.put("upper left", new Vector2d(0, 0));
        fixPoints.put("upper right", new Vector2d(width, 0));
        fixPoints.put("lower right", new Vector2d(width, height));

        fixPoints.put("center left", new Vector2d(0, height / 2d));
        fixPoints.put("center top", new Vector2d(width / 2d, 0));
        fixPoints.put("center right", new Vector2d(width, height / 2d));
        fixPoints.put("center bottom", new Vector2d(width / 2d, height));

        algorithms = FXCollections.observableArrayList();
        lagDetectionAlgorithms = FXCollections.observableArrayList();

        // add delay-algorithms
        lagDetectionAlgorithms.add("threshold");
        lagDetectionAlgorithms.add("peak");
        lagDetectionAlgorithms.add("cross-correlation");
        lagDetectionAlgorithms.add("xcross");

        // add algorithms
        algorithms.add("linear");
        algorithms.add("diagonal");
        algorithms.add("diwl");
        algorithms.addAll("oneLTM");

        cbAutoAlgorithm.setItems(algorithms);
        cbAutoAlgorithm.setValue(algorithms.get(0));

        cbLagDetection.setItems(lagDetectionAlgorithms);
        cbLagDetection.setValue(lagDetectionAlgorithms.get(0));
    }

    public void runAutoAlgorithm()
    {
        clearTable();
        drawGrid();
        String algo = (String)cbAutoAlgorithm.getValue();
        switch (algo)
        {
            case "linear":
                runLinear();
                break;
            case "diagonal":
                runDiagonal();
                break;
            case "diwl":
                runDIWL();
                break;
            case "oneLTM":
                runOneLTM();
                break;
        }
    }

    public Function2<float[], float[], Float> getLagAlgorithm() {
        DelayDetector an = new DelayDetector();
        Function2<float[], float[], Float> algorithm = (a, b) -> (float) an.peekAnalyzer(a, b);

        String algorithmName = (String) cbLagDetection.getValue();

        switch (algorithmName) {
            case "threshold":
                float threshold = Main.inputController.getGestureRecognizer().getThreshold();
                algorithm = (a, b) -> (float) an.extendedThresholdAnalyzer(a, b, threshold);
                break;
            case "peak":
                algorithm = (a, b) -> (float) an.peekAnalyzer(a, b);
                break;
            case "cross-correlation":
                algorithm = (a, b) -> (float) an.execCorrelation(a, b);
                break;
            case "xcross":
                algorithm = (a, b) -> an.xcrossDSPDelay(a, b);
                break;
        }

        return algorithm;
    }

    public void trainLTM()
    {
        if(oneLTM.getStep() == 3)
        {
            log("reset callibration");
            oneLTM.reset();
        }

        int step = oneLTM.getStep();

        switch (step)
        {
            case 0:
                // first init
                log("oneLTM Calibration");
                log("press on the right side");
                oneLTM.incStep();
                break;
            case 1:
                fillAlgorithmInfos(oneLTM);
                oneLTM.train("RIGHT", new Vector2(visTable.getWidth(), visTable.getHeight() / 2d), bufferLL.getBuffer());
                log("press on the left side");
                oneLTM.incStep();
                break;
            case 2:
                fillAlgorithmInfos(oneLTM);
                oneLTM.train("LEFT", new Vector2(0, visTable.getHeight() / 2d), bufferLL.getBuffer());
                log("calibration done!");
                oneLTM.incStep();
                break;
        }
    }

    public void runOneLTM()
    {
        fillAlgorithmInfos(oneLTM);
        Vector2 result = oneLTM.run();
        analyzeResult(result.x, result.y);

        GraphicsContext gc = visTable.getGraphicsContext2D();

        double halfTableWidth = visTable.getWidth() / 2d;

        if(result.x > halfTableWidth)
        {
            //RIGHT
            gc.setFill(Color.RED);
            gc.fillRect(halfTableWidth, 0, halfTableWidth, visTable.getHeight());

        }
        else
        {
            //LEFT
            gc.setFill(Color.BLUE);
            gc.fillRect(0, 0, halfTableWidth, visTable.getHeight());
        }
    }

    public void runDIWL()
    {
        BaseTDAO algo = new DIWLAlgorithm();
        fillAlgorithmInfos(algo);
        Vector2 result = algo.run();
        analyzeResult(result.x, result.y);
    }

    public void runDiagonal()
    {
        BaseTDAO algo = new DiagonalTDAO();
        fillAlgorithmInfos(algo);
        Vector2 result = algo.run();
        analyzeResult(result.x, result.y);
    }

    public void runLinear()
    {
        BaseTDAO algo = new LinearTDAO();
        fillAlgorithmInfos(algo);
        Vector2 result = algo.run();
        analyzeResult(result.x, result.y);
    }

    private void fillAlgorithmInfos(BaseTDAO algo)
    {
        algo.delayAlgorithm = getLagAlgorithm();

        algo.ll = bufferLL.getBuffer();
        algo.ul = bufferLU.getBuffer();
        algo.ur = bufferRU.getBuffer();
        algo.lr = bufferRL.getBuffer();

        algo.tableLength = 1.5;
        algo.tableWidth = 0.75;

        algo.controller = this;

        algo.canvas = visTable;
    }

    public void drawGrid() {
        GraphicsContext gc = visTable.getGraphicsContext2D();

        // draw grid
        gc.setStroke(Color.DARKGRAY);
        gc.strokeLine(visTable.getWidth() / 2d, 0, visTable.getWidth() / 2d, visTable.getHeight());
        gc.strokeLine(0, visTable.getHeight() / 2d, visTable.getWidth(), visTable.getHeight() / 2d);
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

    public void log(String message) {
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
        lastGain = gainFactor;

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
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, visTable.getWidth(), visTable.getHeight());

        // draw border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, visTable.getWidth() - 2, visTable.getHeight() - 2);
    }

    void drawBuffer(float[] buffer, Canvas c, Color color) {
        drawBuffer(buffer, c, color, 1.0f, 1);
    }

    void drawBuffer(float[] buffer, Canvas c, Color color, float gainFactor) {
        drawBuffer(buffer, c, color, gainFactor, 1);
    }

    void drawBuffer(float[] buffer, Canvas c, Color color, float gainFactor, double size) {
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.clearRect(0, 0, c.getWidth(), c.getHeight());
        float space = (float) (c.getWidth() / buffer.length);

        gc.setFill(color);

        float y = (float) c.getHeight() / 2f;

        for (int i = 0; i < buffer.length - 1; i++) {
            float v = buffer[i];

            gc.fillOval(space * i, y + (y * v * gainFactor), size, size);
        }

        // draw border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(1, 1, c.getWidth() - 2, c.getHeight() - 2);
    }

    void drawLineBuffer(float[] buffer, Canvas c, Color color, float gainFactor, double size) {
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.clearRect(0, 0, c.getWidth(), c.getHeight());
        float space = (float) (c.getWidth() / buffer.length);

        gc.setFill(color);

        float y = (float) c.getHeight() / 2f;

        double[] xValues = new double[buffer.length];
        double[] yValues = new double[buffer.length];

        for (int i = 0; i < buffer.length - 1; i++) {
            float v = buffer[i];

            xValues[i] = space * i;
            yValues[i] = y + (y * v * gainFactor);

            gc.fillOval(space * i, y + (y * v * gainFactor), size, size);
        }

        gc.setStroke(Color.LIGHTGRAY);
        gc.strokePolyline(xValues, yValues, buffer.length - 1);

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

        //log("loaded live input buffer (" + bufferLL.size() + ")");

        // draw visualisation
        drawAllBuffer("LIVE (" + bufferLL.size() + ")");
    }

    public void visMouse_Moved(MouseEvent event) {
        Canvas c = (Canvas) event.getSource();
        if (bufferLL != null) {
            int i = (int) (event.getX() / c.getWidth() * bufferLL.size());
            //System.out.println(event.getX() + ": " + bufferLL.get(i));
            dataPointLabelLL.setText(String.format("%f", bufferLL.get(i)));
        }
    }

    void analyzeResult(double x, double y) {
        double width = visTable.getWidth();
        double height = visTable.getHeight();

        Vector2d prediction = new Vector2d(x, y);

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

        // draw vector from center to prediction
        GraphicsContext gc = visTable.getGraphicsContext2D();
        gc.setStroke(Color.BLUE);
        gc.strokeLine(fixPoints.get("center").getX(), fixPoints.get("center").getY(), prediction.getX(), prediction.getY());


        // output result
        log("Prediction: " + minKey + " (" + minDistance + ")");

        lastPrediction = minKey;
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

        try {
            Main.inputController.setGain((float) (double) root.get("gain"));
            Main.inputController.getGestureRecognizer().setThreshold((float) (double) root.get("threshold"));
        } catch (Exception ex)
        {
            //ex.printStackTrace();
        }

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

    public void btnRunAlgo_Clicked(ActionEvent actionEvent) {
        Platform.runLater(() -> runAutoAlgorithm());
    }

    public void btnRunLag_Clicked(ActionEvent actionEvent) {
        StringBuilder sb = new StringBuilder();

        Function2<float[], float[], Float> lagAlgorithm = getLagAlgorithm();

        // calculate lag between all
        float[] ll = bufferLL.getBuffer();
        float[] ul = bufferLU.getBuffer();
        float[] ur = bufferRU.getBuffer();
        float[] lr = bufferRL.getBuffer();

        // calc
        sb.append("LL - UL: " + lagAlgorithm.apply(ll, ul) + "\n");
        sb.append("LL - UR: " + lagAlgorithm.apply(ll, ur) + "\n");
        sb.append("LL - LR: " + lagAlgorithm.apply(ll, lr) + "\n");
        sb.append("---\n");
        sb.append("UL - LL: " + lagAlgorithm.apply(ul, ll) + "\n");
        sb.append("UL - UR: " + lagAlgorithm.apply(ul, ur) + "\n");
        sb.append("UL - LR: " + lagAlgorithm.apply(ul, lr) + "\n");
        sb.append("---\n");
        sb.append("UR - LL: " + lagAlgorithm.apply(ur, ll) + "\n");
        sb.append("UR - UL: " + lagAlgorithm.apply(ur, ul) + "\n");
        sb.append("UR - LR: " + lagAlgorithm.apply(ur, lr) + "\n");
        sb.append("---\n");
        sb.append("LR - LL: " + lagAlgorithm.apply(lr, ll) + "\n");
        sb.append("LR - UL: " + lagAlgorithm.apply(lr, ul) + "\n");
        sb.append("LR - UR: " + lagAlgorithm.apply(lr, ur) + "\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Lag Algorithm Information");
        alert.setHeaderText("Results of '" + cbLagDetection.getValue() + "' lag algorithm:");
        alert.setContentText(sb.toString());

        System.out.println(cbLagDetection.getValue() + ":");
        System.out.println(sb.toString());

        alert.showAndWait();
    }

    public void oneLTM_Clicked(ActionEvent actionEvent) {
        trainLTM();
    }

    public void OnZoom_Clicked(ActionEvent actionEvent) {

        if(isZoomed)
        {
            drawAllBuffer("-");
            isZoomed = false;
            return;
        }

        int peak = DIWLAlgorithm.getPeekPosition(bufferLL.getBuffer());

        float gain = zoomBuffer(bufferLL, peak, visLeftLower, Color.BLUE, -1);
        zoomBuffer(bufferLU, peak, visLeftUpper, Color.RED, gain);
        zoomBuffer(bufferRU, peak, visRightUpper, Color.GREEN, gain);
        zoomBuffer(bufferRL, peak, visRightLower, Color.ORANGE, gain);
        isZoomed = true;
    }

    public float zoomBuffer(LoopRingBuffer lrp, int peak, Canvas vis, Color c, float gain)
    {
        // show zoomed view to peak point
        int sampleOffset = 100;

        float[] data = lrp.getBuffer();
        float[] peekData = getPart(data, peak - sampleOffset, sampleOffset * 2);

        // calculate own peak
        int p = DIWLAlgorithm.getPeekPosition(data);
        int peekOffset = (p - peak) + sampleOffset;

        float gainFactor = gain;
        if(gain < 0)
        {
            float max = 0;
            for (int i = 0; i < data.length; i++) {
                if (max < data[i]) max = data[i];
            }
            gainFactor = 1.0f / max;
        }

        float k = gainFactor;

        Platform.runLater(() -> {
            // draw wave
            drawLineBuffer(peekData, vis, c, k, 2);

            // draw peak indicator
            GraphicsContext gc = vis.getGraphicsContext2D();
            gc.setStroke(Color.MAGENTA);
            double space = vis.getWidth() / (float)peekData.length;
            gc.strokeLine(space * peekOffset, 0, space * peekOffset, vis.getHeight());
        });

        return k;
    }

    public float[] getPart(float[] data, int start, int length)
    {
        float[] result = new float[length];
        for(int i = start; i < start + length; i++)
        {
            if(data.length > i && i >= 0)
                result[i-start] = data[i];
            else
                result[i-start] = 0;
        }
        return result;
    }

    public void onBtnMarf_Clicked(ActionEvent actionEvent) throws PreprocessingException, FeatureExtractionException {
        AudioFeatureExtractor extractor = new AudioFeatureExtractor();
        extractor.extract(bufferLL.getBuffer());
    }

    public void onMedian_Clicked(ActionEvent actionEvent) {
        MedianFilter median = new MedianFilter(101);
        float stretchValue = 0.25f;
        float[] filteredData = median.filterAndStretch(bufferLL.getBuffer(), stretchValue);

        drawBuffer(bufferLL.getBuffer(), visLeftLower, Color.BLUE, lastGain);
        drawBuffer(filteredData, visLeftUpper, Color.BLUE, lastGain);

        // caluclate it for all
        bufferLL = new LoopRingBuffer(median.filterAndStretch(bufferLL.getBuffer(), stretchValue));
        bufferLU = new LoopRingBuffer(median.filterAndStretch(bufferLU.getBuffer(), stretchValue));
        bufferRL = new LoopRingBuffer(median.filterAndStretch(bufferRL.getBuffer(), stretchValue));
        bufferRU = new LoopRingBuffer(median.filterAndStretch(bufferRU.getBuffer(), stretchValue));

        drawAllBuffer("Median");
    }

    public void onCopyLastPoint(ActionEvent actionEvent) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(lastPoint.x + "\t" + lastPoint.y);
        clipboard.setContent(content);
    }

    public void onClearOutput(ActionEvent actionEvent) {
        clearLog();
    }

    public void onRunExperimentTest(ActionEvent actionEvent) throws InterruptedException, IOException {
        List<String> choices = fixPoints.keySet().stream().collect(Collectors.toList());

        ChoiceDialog<String> dialog = new ChoiceDialog<>("center", choices);
        dialog.setTitle("Choose Direction");
        dialog.setHeaderText("Direction where the signal was coming from.");
        dialog.setContentText("Choose your direction:");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        StringBuilder sbcsv = new StringBuilder();
        sb.append("New Experiment\n");

        for(String algorithm : algorithms.filtered(x -> !x.equals("oneLTM")))
        {
            for(String lagAlgo : lagDetectionAlgorithms.filtered(x -> !x.equals("xcross")))
            {
                sb.append(algorithm + " | " + lagAlgo + "\n");

                //Platform.runLater(() -> {
                cbAutoAlgorithm.setValue(algorithm);
                cbLagDetection.setValue(lagAlgo);
                //});

                Thread t = new Thread(() -> {
                    // wait for setting algorithm
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // run algorithm
                    runAutoAlgorithm();

                    // collect result
                    sb.append(lastPrediction + "\t");
                    sb.append(lastPoint.x + "\t");
                    sb.append(lastPoint.y + "\n");

                    // collect result for csv
                    sbcsv.append(algorithm + "\t");
                    sbcsv.append(lagAlgo + "\t");
                    sbcsv.append(lastPrediction + "\t");
                    sbcsv.append(lastPoint.x + "\t");
                    sbcsv.append(lastPoint.y + "\t");
                    sbcsv.append(result.get() + "\n");

                    //Vector2d v = fixPoints.get(result.get());
                    //sbcsv.append(v.getX() + "\t");
                    //sbcsv.append(v.getY() + "\n");
                });
                t.start();
                t.join();

                sb.append("\n");
            }
        }

        // save sb
        Files.write(Paths.get("experiment/test.txt"), sb.toString().getBytes());

        // create or append
        Path p = Paths.get("experiment/result.csv");

        if (!Files.exists(p))
            Files.write(p, "algorithm\tdelay\tprediction\tx\ty\tlabel\n".getBytes(), StandardOpenOption.CREATE);
        Files.write(Paths.get("experiment/result.csv"), sbcsv.toString().getBytes(), StandardOpenOption.APPEND);

        System.out.println("Experiment done!");
    }

    public void onPlayClicked(ActionEvent actionEvent) {
        float[] result = bufferLL.getBuffer();
        byte[] barray = new byte[result.length];
        for (int i = 0; i< result.length; i++) {
            barray[i] = (byte)result[i];
        }
        // Create the AudioData object from the byte array
        AudioData audioData = new AudioData(barray);
        // Create an AudioDataStream to play back
        AudioDataStream audioStream = new AudioDataStream(audioData);
        // Play the sound
        AudioPlayer.player.start(audioStream);
    }
}
