package org.frcteam2910.pathviewer;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.NumberStringConverter;

public class PointEditorController {
    @FXML
    private TextField txtX;
    @FXML
    private TextField txtY;
    @FXML
    private TextField txtRotationField;

    @FXML
    private void initialize() {
        txtX.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));
        txtY.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));
        txtRotationField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));

        fieldPointProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                txtX.textProperty().unbindBidirectional(oldValue.centerXProperty());
                txtY.textProperty().unbindBidirectional(oldValue.centerYProperty());
                txtRotationField.textProperty().unbindBidirectional(oldValue.rotateProperty());
                txtX.setDisable(true);
                txtY.setDisable(true);
                txtRotationField.setDisable(true);
                txtX.clear();
                txtY.clear();
                txtRotationField.clear();
            }

            if(newValue != null) {
                txtX.textProperty().bindBidirectional(newValue.centerXProperty(), new NumberStringConverter());
                txtY.textProperty().bindBidirectional(newValue.centerYProperty(), new NumberStringConverter());
                txtX.setDisable(false);
                txtY.setDisable(false);

                if (newValue instanceof FieldPrimaryControlPoint) {
                    txtRotationField.textProperty().bindBidirectional(newValue.rotateProperty(), new NumberStringConverter());
                    txtRotationField.setDisable(false);
                }
            }
        });
    }

    private final SimpleObjectProperty<FieldPoint> fieldPoint = new SimpleObjectProperty<>();

    public ObjectProperty<FieldPoint> fieldPointProperty() {
        return fieldPoint;
    }

    public FieldPoint getFieldPoint() {
        return fieldPoint.get();
    }

    public void setFieldPoint(FieldPoint fieldPoint) {
        fieldPointProperty().set(fieldPoint);
    }
}
