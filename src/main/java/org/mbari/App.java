package org.mbari;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.mbari.vcr4j.sharktopoda.client.localization.IO;
import org.mbari.vcr4j.sharktopoda.client.localization.Localization;

/**
 * JavaFX App
 */
public class App extends Application {

    private Instant startTime = Instant.now();

    private Timer timer;
    private ObjectProperty<Duration> elapsedTime = new SimpleObjectProperty<>(Duration.ZERO);

    private IO io;
    private UUID videoReferenceUuid = UUID.randomUUID();


    private IO loggingIO;


    @Override
    public void start(Stage stage) {

        initComms();


        var label = new Label();
        elapsedTime.addListener((src, oldV, newV) -> {
            Platform.runLater(() -> {
                label.setText(formatDuration(newV));
            });
            
        });


        var playPauseBtn = new ToggleButton("play");
        playPauseBtn.setOnAction(evt -> {
            if (playPauseBtn.isSelected()) {
                play();
                playPauseBtn.setText("Pause");
            }
            else {
                pause();
                playPauseBtn.setText("Play");
            }
        });


        var publishBtn = new Button("new bounding box");
        publishBtn.setOnAction(evt -> {
            var x = random(0, 1918);
            var y = random(0, 1078);
            var localization = new Localization("Default Concept", 
                elapsedTime.get(),
                UUID.randomUUID(),
                videoReferenceUuid,
                x,
                y,
                random(1, 1920 - x),
                random(1, 1080 - y),
                Duration.ZERO,
                UUID.randomUUID());

            System.out.println("---> New localization published: " + localization);

            io.getController().addLocalization(localization);

        });


        

        var vbox = new VBox(label, playPauseBtn, publishBtn);
        vbox.setAlignment(Pos.BASELINE_CENTER);
        VBox.setMargin(label, new Insets(10, 10, 10, 10));
        VBox.setMargin(playPauseBtn, new Insets(10, 10, 10, 10));
        VBox.setMargin(publishBtn, new Insets(10, 10, 10, 10));

        var scene = new Scene(vbox, 200, 200);
        stage.setScene(scene);
        stage.show();
    }

    private void initComms() {
        var incomingPort = 5561;   // ZeroMQ subscriber port
        var outgoingPort = 5562;   // ZeroMQ publisher port
        var incomingTopic = "localization";
        var outgoingTopic = "localization";
        io = new IO(incomingPort, outgoingPort, incomingTopic, outgoingTopic);

        // loggingIO = new IO(outgoingPort, incomingPort, outgoingTopic, incomingTopic);
        // loggingIO.getController()
        //     .getLocalizations()
        //     .addListener((ListChangeListener.Change<? extends Localization> c) -> {
        //         System.out.println("<--- New localization received. Change: " + c);
        //     });
    }

    private void play() {
        startTime = Instant.now().minus(elapsedTime.get());
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                var dt = Duration.between(startTime, Instant.now());
                elapsedTime.set(dt);
            }
        }, 0, 100);
    }

    private void pause() {
        timer.cancel();
        // startTime = Instant.now().minus(elapsedTime.get());
    }

    private String formatDuration(Duration duration) {
        return String.format("%d:%02d:%02d:%03d", 
                                duration.toHours(), 
                                duration.toMinutesPart(), 
                                duration.toSecondsPart(),
                                duration.toMillisPart());
    }

    private int random(int min, int max) {
        var r = new Random();
        return r.nextInt(max - min + min);
    }
    
    public static void main(String[] args) {
        launch();
    }

}