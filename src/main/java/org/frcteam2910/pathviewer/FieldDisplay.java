package org.frcteam2910.pathviewer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;
import org.frcteam2910.common.control.Path;
import org.frcteam2910.common.control.SplinePathBuilder;
import org.frcteam2910.common.control.SplinePathSegment;
import org.frcteam2910.common.math.Rotation2;
import org.frcteam2910.common.math.Vector2;
import org.frcteam2910.common.math.spline.CubicBezierSpline;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class FieldDisplay extends Pane {
    private static final double ANCHOR_OUTLINE_WIDTH = 0.1;
    private static final Color PRIMARY_ANCHOR_COLOR = Color.rgb(255, 255, 0);
    private static final double PRIMARY_ANCHOR_RADIUS = 0.25;
    private static final Color CONTROL_ANCHOR_COLOR = Color.rgb(13, 163, 73);
    private static final double CONTROL_ANCHOR_RADIUS = 0.15;
    private static final Color CONTROL_LINE_COLOR = Color.rgb(13, 163, 73);
    private static final double CONTROL_LINE_WIDTH = 0.1;
    private static final Color PATH_COLOR = Color.rgb(107, 82, 148);
    private static final double PATH_WIDTH = 0.5;
    private static final double PATH_INITIAL_CONTROL_DISTANCE = 1.0;

    @FXML
    private Group group;
    @FXML
    private ImageView backgroundImage;
    @FXML
    public Pane drawPane;
    @FXML
    private Group splineGroup;
    @FXML
    private Group anchorGroup;
    @FXML
    private Group controlLineGroup;

    private Field field;

    private ObservableList<PathSection> sections = FXCollections.observableArrayList();

    public FieldDisplay() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FieldDisplay.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void initialize() {
        field = new Field(new Image("org/frcteam2910/pathviewer/2019-field.jpg"), new Vector2(54.0, 27.0), new Vector2(217, 40), new Vector2(1372 - 217, 615 - 40));
        Image image = field.getImage();
        backgroundImage.setImage(image);
        Scale scale = new Scale();
        scale.xProperty().bind(Bindings.createDoubleBinding(() ->
                        Math.min(getWidth() / image.getWidth(), getHeight() / image.getHeight()),
                widthProperty(), heightProperty()));
        scale.yProperty().bind(Bindings.createDoubleBinding(() ->
                        Math.min(getWidth() / image.getWidth(), getHeight() / image.getHeight()),
                widthProperty(), heightProperty()));

        group.getTransforms().add(scale);

        drawPane.setPrefWidth(field.getSize().x);
        drawPane.setPrefHeight(field.getSize().y);
        drawPane.setLayoutX(field.getCoord().x);
        drawPane.setLayoutY(field.getCoord().y);
        drawPane.setScaleX(field.getScale());
        drawPane.setScaleY(field.getScale());

        sections.addListener((ListChangeListener<PathSection>) c -> {
            while (c.next()) {
                for (PathSection section : c.getAddedSubList()) {
                    section.onAdded(anchorGroup, controlLineGroup, splineGroup);
                }

                for (PathSection section : c.getRemoved()) {
                    section.onRemove(anchorGroup, controlLineGroup, splineGroup, c.getList());
                }
            }
        });

        drawPane.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                if (anchorGroup.getChildren().isEmpty()) {
                    Anchor anchor = new PrimaryAnchor(mouseEvent.getX(), mouseEvent.getY());
                    anchorGroup.getChildren().add(anchor);
                } else {
                    PathSection previousSection = null;
                    if (!sections.isEmpty()) {
                        previousSection = sections.get(sections.size() - 1);
                    }

                    PrimaryAnchor start = (PrimaryAnchor) anchorGroup.getChildren().get(anchorGroup.getChildren().size() - 1);
                    PrimaryAnchor end = new PrimaryAnchor(mouseEvent.getX(), mouseEvent.getY());

                    sections.add(new PathSection(start, end, previousSection));
                }
            }
        });
    }

    /**
     * Clears the current path from the display.
     */
    public void clearPath() {
        sections.clear();
    }

    /**
     * Gets the path that is currently on the display.
     *
     * @return The path or null if no path is on the display.
     */
    @CheckForNull
    public Path getPath() {
        if (sections.isEmpty()) {
            return null;
        }

        SplinePathBuilder builder = new SplinePathBuilder(
                sections.get(0).startAnchor.getCenter(),
                Rotation2.ZERO,
                Rotation2.ZERO
        );
        sections.forEach(section -> builder.bezier(
                section.controlAnchors[0].getCenter(),
                section.controlAnchors[1].getCenter(),
                section.endAnchor.getCenter()
        ));

        return builder.build();
    }

    /**
     * Sets the path that is currently on the display.
     *
     * @param path The path to display.
     */
    public void setPath(@Nonnull Path path) {
        clearPath();

        Arrays.stream(path.getSegments())
                .map(s -> (SplinePathSegment) s)
                .forEachOrdered(s -> {
                    CubicBezierSpline spline = CubicBezierSpline.convert(s.getSpline());
                    Vector2[] controlPoints = spline.getControlPoints();

                    PrimaryAnchor startAnchor;
                    if (sections.isEmpty()) {
                        startAnchor = new PrimaryAnchor(controlPoints[0].x, controlPoints[0].y);
                    } else {
                        startAnchor = sections.get(sections.size() - 1).endAnchor;
                    }

                    PrimaryAnchor endAnchor = new PrimaryAnchor(controlPoints[3].x, controlPoints[3].y);

                    PathSection lastSection = null;
                    if (!sections.isEmpty()) {
                        lastSection = sections.get(sections.size() - 1);
                    }

                    PathSection section = new PathSection(startAnchor, endAnchor, lastSection);
                    section.controlAnchors[0].setCenter(controlPoints[1]);
                    section.controlAnchors[1].setCenter(controlPoints[2]);

                    sections.add(section);
                });
    }

    private static class PathSection extends CubicCurve {
        public final PrimaryAnchor startAnchor;
        public final PrimaryAnchor endAnchor;

        public final SecondaryAnchor[] controlAnchors;
        public final ControlLine[] controlLines;

        public PathSection(PrimaryAnchor startAnchor, PrimaryAnchor endAnchor, @Nullable PathSection previousSection) {
            Vector2 start = startAnchor.getCenter();
            Vector2 end = endAnchor.getCenter();
            Vector2 delta = end.subtract(start);

            setStartX(start.x);
            setStartY(start.y);
            setControlX1(start.x + PATH_INITIAL_CONTROL_DISTANCE * delta.normal().x);
            setControlY1(start.y + PATH_INITIAL_CONTROL_DISTANCE * delta.normal().y);
            setControlX2(end.x - PATH_INITIAL_CONTROL_DISTANCE * delta.normal().x);
            setControlY2(end.y - PATH_INITIAL_CONTROL_DISTANCE * delta.normal().y);
            setEndX(end.x);
            setEndY(end.y);

            // Setup curve colors
            setStroke(PATH_COLOR);
            setStrokeWidth(PATH_WIDTH);
            setStrokeLineCap(StrokeLineCap.ROUND);
            setFill(Color.TRANSPARENT);

            // Bind end points to anchor positions
            startXProperty().bind(startAnchor.centerXProperty());
            startYProperty().bind(startAnchor.centerYProperty());
            endXProperty().bind(endAnchor.centerXProperty());
            endYProperty().bind(endAnchor.centerYProperty());

            this.startAnchor = startAnchor;
            this.endAnchor = endAnchor;

            SecondaryAnchor otherStartControlAnchor = null;
            if (previousSection != null) {
                otherStartControlAnchor = previousSection.controlAnchors[1];
            }

            controlAnchors = new SecondaryAnchor[]{
                    new SecondaryAnchor(controlX1Property(), controlY1Property(), startAnchor, otherStartControlAnchor),
                    new SecondaryAnchor(controlX2Property(), controlY2Property(), endAnchor, null)
            };

            controlLines = new ControlLine[]{
                    new ControlLine(startAnchor, controlAnchors[0]),
                    new ControlLine(controlAnchors[1], endAnchor)
            };
        }

        public void onAdded(Group anchorGroup, Group controlLineGroup, Group splineGroup) {
            if (!anchorGroup.getChildren().contains(startAnchor)) {
                anchorGroup.getChildren().add(startAnchor);
            }
            anchorGroup.getChildren().addAll(controlAnchors);
            if (!anchorGroup.getChildren().contains(endAnchor)) {
                anchorGroup.getChildren().add(endAnchor);
            }
            controlLineGroup.getChildren().addAll(controlLines);

            splineGroup.getChildren().add(this);
        }

        public void onRemove(Group anchorGroup, Group controlLineGroup, Group splineGroup, ObservableList<? extends PathSection> otherSections) {
            boolean canRemoveStart = true;
            boolean canRemoveEnd = true;
            for (PathSection s : otherSections) {
                if (s.endAnchor == startAnchor) {
                    canRemoveStart = false;
                }
                if (s.startAnchor == endAnchor) {
                    canRemoveEnd = false;
                }
            }
            if (canRemoveStart) {
                anchorGroup.getChildren().remove(startAnchor);
            }
            anchorGroup.getChildren().removeAll(controlAnchors);
            if (canRemoveEnd) {
                anchorGroup.getChildren().remove(endAnchor);
            }
            controlLineGroup.getChildren().removeAll(controlLines);

            splineGroup.getChildren().remove(this);
        }
    }

    private static class ControlLine extends Line {
        public ControlLine(Anchor start, Anchor end) {
            startXProperty().bind(start.centerXProperty());
            startYProperty().bind(start.centerYProperty());
            endXProperty().bind(end.centerXProperty());
            endYProperty().bind(end.centerYProperty());

            setStrokeWidth(CONTROL_LINE_WIDTH);
            setStroke(CONTROL_LINE_COLOR.deriveColor(0, 1, 1, 0.5));
        }
    }

    private abstract static class Anchor extends Circle {
        public Anchor(Color color, DoubleProperty x, DoubleProperty y, double radius) {
            this(color, x.get(), y.get(), radius);
            x.bind(centerXProperty());
            y.bind(centerYProperty());
        }

        public Anchor(Color color, double x, double y, double radius) {
            super(x, y, radius);
            setFill(color.deriveColor(1, 1, 1, 0.5));
            setStroke(color);
            setStrokeWidth(ANCHOR_OUTLINE_WIDTH);
            setStrokeType(StrokeType.OUTSIDE);

            setOnMouseDragged(this::onMouseDragged);
        }

        protected void onMouseDragged(MouseEvent event) {
            setCenterX(event.getX());
            setCenterY(event.getY());
        }

        public Vector2 getCenter() {
            return new Vector2(getCenterX(), getCenterY());
        }

        public void setCenter(Vector2 center) {
            setCenterX(center.x);
            setCenterY(center.y);
        }
    }

    private static class PrimaryAnchor extends Anchor {
        private List<SecondaryAnchor> secondaryAnchors = new ArrayList<>();

        public PrimaryAnchor(DoubleProperty x, DoubleProperty y) {
            super(PRIMARY_ANCHOR_COLOR, x, y, PRIMARY_ANCHOR_RADIUS);
        }

        public PrimaryAnchor(double x, double y) {
            super(PRIMARY_ANCHOR_COLOR, x, y, PRIMARY_ANCHOR_RADIUS);
        }

        @Override
        protected void onMouseDragged(MouseEvent event) {
            Vector2[] deltas = new Vector2[secondaryAnchors.size()];
            for (int i = 0; i < deltas.length; i++) {
                deltas[i] = secondaryAnchors.get(i).getCenter().subtract(getCenter());
            }

            super.onMouseDragged(event);

            for (int i = 0; i < deltas.length; i++) {
                SecondaryAnchor anchor = secondaryAnchors.get(i);
                anchor.setCenter(getCenter().add(deltas[i]));
            }
        }

        public void addSecondaryAnchor(SecondaryAnchor anchor) {
            secondaryAnchors.add(anchor);
        }
    }

    private static class SecondaryAnchor extends Anchor {
        private final PrimaryAnchor primaryAnchor;
        @Nullable
        private SecondaryAnchor otherSecondaryAnchor;

        public SecondaryAnchor(DoubleProperty x, DoubleProperty y, PrimaryAnchor primaryAnchor,
                               @Nullable SecondaryAnchor otherSecondaryAnchor) {
            super(CONTROL_ANCHOR_COLOR, x, y, CONTROL_ANCHOR_RADIUS);
            this.primaryAnchor = primaryAnchor;
            primaryAnchor.addSecondaryAnchor(this);
            this.otherSecondaryAnchor = otherSecondaryAnchor;
            if (otherSecondaryAnchor != null) {
                otherSecondaryAnchor.setOtherSecondaryAnchor(this);

                Vector2 otherDelta = primaryAnchor.getCenter().subtract(otherSecondaryAnchor.getCenter());
                setCenter(Vector2.fromAngle(otherDelta.getAngle()).scale(PATH_INITIAL_CONTROL_DISTANCE).add(primaryAnchor.getCenter()));
            }
        }

        @Override
        protected void onMouseDragged(MouseEvent event) {
            super.onMouseDragged(event);

            if (otherSecondaryAnchor == null) {
                return;
            }

            // Make sure the position of the other secondary anchor is always 180 off of this anchor
            Vector2 thisDelta = primaryAnchor.getCenter().subtract(getCenter());
            Vector2 otherDelta = primaryAnchor.getCenter().subtract(otherSecondaryAnchor.getCenter());

            otherDelta = Vector2.fromAngle(thisDelta.getAngle()).scale(otherDelta.length);
            otherSecondaryAnchor.setCenter(otherDelta.add(primaryAnchor.getCenter()));
        }

        public void setOtherSecondaryAnchor(@Nullable SecondaryAnchor otherSecondaryAnchor) {
            this.otherSecondaryAnchor = otherSecondaryAnchor;
        }
    }
}
