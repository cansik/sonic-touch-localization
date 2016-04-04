package main;

import javafx.event.ActionEvent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Controller {

    public Canvas visCanvas;

    public void btnTest_clicked(ActionEvent actionEvent) {
        System.out.println("draw visualisation");
        drawTableVisualisation();
    }

    private void drawTableVisualisation()
    {
        GraphicsContext gc = visCanvas.getGraphicsContext2D();
        gc.setFill(Color.GREEN);
        gc.fill();

        gc.setFill(Color.BLUE);
        gc.rect(10, 10, 200, 100);
    }
}
