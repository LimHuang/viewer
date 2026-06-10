package com.syspilot.viewer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.syspilot.viewer.architecture.AppArchitecture;
import com.syspilot.viewer.architecture.BaseController;
import com.syspilot.viewer.event.StepSelectedEvent;
import com.syspilot.viewer.event.TrajectoryLoadedEvent;
import com.syspilot.viewer.model.StepData;
import com.syspilot.viewer.model.ToolCallData;
import com.syspilot.viewer.utility.MarkdownRenderer;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;

public class StepDetailPanelController extends BaseController {

    @FXML private HBox stepHeader;
    @FXML private Text stepTitle;
    @FXML private Label roleBadge;
    @FXML private Text stepTimeText;
    @FXML private Text stepDurationText;

    @FXML private HBox statsRow;
    @FXML private Text tokensInDetail;
    @FXML private Text tokensOutDetail;
    @FXML private Text llmTimeText;
    @FXML private Text stepTimeDetail;

    @FXML private Separator headerSeparator;

    @FXML private SplitPane contentSplitPane;
    @FXML private StackPane contentStack;
    @FXML private WebView contentView;
    @FXML private VBox toolDetailPane;
    @FXML private Text toolDetailTitle;
    @FXML private TextArea toolArgsArea;
    @FXML private TextArea toolResultArea;
    @FXML private Text toolErrorLabel;
    @FXML private TextArea toolErrorArea;

    @FXML private VBox toolContainer;
    @FXML private ListView<ToolCallData> toolListView;

    @FXML private VBox emptyState;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private static final double LINE_HEIGHT = 18.0;
    private static final double MAX_RATIO = 0.5;
    private static final double MIN_RATIO = 0.15;
    private static final double MONOSPACE_CHAR_WIDTH = 7.5;

    private MarkdownRenderer mdRenderer = new MarkdownRenderer(false);
    private StepData currentStep;
    private boolean toolsVisible = false;
    private boolean adjustingDivider = false;

    @FXML
    public void initialize() {
        setArchitecture(AppArchitecture.getInstance());

        // Start with only the message pane in the split; tools are added on demand
        contentSplitPane.getItems().remove(toolContainer);

        // Re-measure on resize so divider stays proportional
        contentSplitPane.heightProperty().addListener((obs, old, h) -> adjustDivider());

        toolListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ToolCallData tc, boolean empty) {
                super.updateItem(tc, empty);
                if (empty || tc == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String icon = tc.hasError() ? "✗" : "✓";
                    String time = tc.getDurationMs() != null
                            ? String.format(" (%.0fms)", tc.getDurationMs()) : "";
                    String argsStr = formatArgs(tc.getArgs());
                    if (!argsStr.isEmpty()) {
                        setText(icon + " " + tc.getToolName() + " — " + argsStr + time);
                    } else {
                        setText(icon + " " + tc.getToolName() + time);
                    }
                }
            }
        });

        toolListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, tc) -> {
                    if (tc != null) {
                        showToolDetail(tc);
                    }
                });

        registerEvent(StepSelectedEvent.class, this::onStepSelected);
        registerEvent(TrajectoryLoadedEvent.class, e -> clear());
    }

    // ---- Theme ----

    public void setDarkMode(boolean dark) {
        mdRenderer = new MarkdownRenderer(dark);
        if (currentStep != null) {
            renderContent();
        }
    }

    // ---- Tool detail ↔ message view switching ----

    @FXML
    private void onToolDetailBack() {
        showMessageView();
    }

    private void showToolDetail(ToolCallData tc) {
        toolDetailTitle.setText(tc.getToolName() + " (" + (tc.getDurationMs() != null
                ? String.format("%.0fms", tc.getDurationMs()) : "?") + ")");

        toolArgsArea.setText(tc.getArgs() != null ? toPrettyJson(tc.getArgs()) : "(none)");

        String result = tc.getResult();
        if (result != null && !result.isEmpty()) {
            toolResultArea.setText(tryFormatJson(result));
        } else {
            toolResultArea.setText("(none)");
        }

        if (tc.hasError()) {
            toolErrorArea.setText(tc.getError());
            toolErrorLabel.setVisible(true);
            toolErrorLabel.setManaged(true);
            toolErrorArea.setVisible(true);
            toolErrorArea.setManaged(true);
        } else {
            toolErrorLabel.setVisible(false);
            toolErrorLabel.setManaged(false);
            toolErrorArea.setVisible(false);
            toolErrorArea.setManaged(false);
            toolErrorArea.clear();
        }

        contentView.setVisible(false);
        contentView.setManaged(false);
        toolDetailPane.setVisible(true);
        toolDetailPane.setManaged(true);

        adjustDivider();
    }

    private void showMessageView() {
        toolDetailPane.setVisible(false);
        toolDetailPane.setManaged(false);
        contentView.setVisible(true);
        contentView.setManaged(true);
        toolListView.getSelectionModel().clearSelection();

        adjustDivider();
    }

    // ---- Step selection ----

    private void onStepSelected(StepSelectedEvent event) {
        currentStep = event.getStep();
        showDetail(true);
        showMessageView();
        StepData step = currentStep;

        // Header
        stepTitle.setText("Step #" + step.getStepId());

        roleBadge.setText(getRoleLabel(step));
        roleBadge.getStyleClass().removeAll("role-badge-agent", "role-badge-subagent", "role-badge-user");
        switch (getRoleType(step)) {
            case "agent" -> roleBadge.getStyleClass().add("role-badge-agent");
            case "subagent" -> roleBadge.getStyleClass().add("role-badge-subagent");
            default -> roleBadge.getStyleClass().add("role-badge-user");
        }

        stepTimeText.setText(step.getTimestamp() != null ? step.getTimestamp() : "");
        stepDurationText.setText(step.getStepDurationMs() != null
                ? String.format("%.1fs", step.getStepDurationMs() / 1000) : "");

        // Stats row
        if (step.getModelInfo() != null) {
            tokensInDetail.setText(formatNum(step.getModelInfo().getInputTokens()));
            tokensOutDetail.setText(formatNum(step.getModelInfo().getOutputTokens()));
            statsRow.setVisible(true);
            statsRow.setManaged(true);
        } else {
            statsRow.setVisible(false);
            statsRow.setManaged(false);
        }

        double stepTime = step.getStepDurationMs() != null ? step.getStepDurationMs() / 1000 : 0;
        stepTimeDetail.setText(String.format("%.1fs", stepTime));
        llmTimeText.setText(String.format("%.1fs", stepTime));

        // Tool calls — dynamically add/remove from SplitPane (before renderContent
        // so toolsVisible is set when the async HTML load completes)
        toolListView.getItems().clear();
        boolean hasTools = step.getToolCalls() != null && !step.getToolCalls().isEmpty();
        if (hasTools) {
            toolListView.getItems().addAll(step.getToolCalls());
            setToolsVisible(true);
        } else {
            setToolsVisible(false);
        }

        // Render markdown content (async HTML load will trigger divider adjustment)
        renderContent();

        headerSeparator.setVisible(true);
        headerSeparator.setManaged(true);
    }

    private void setToolsVisible(boolean visible) {
        if (visible == toolsVisible) return;
        toolsVisible = visible;
        if (visible) {
            if (!contentSplitPane.getItems().contains(toolContainer)) {
                contentSplitPane.getItems().add(toolContainer);
            }
            contentSplitPane.setDividerPositions(0.5);
        } else {
            contentSplitPane.getItems().remove(toolContainer);
        }
    }

    // ---- Content rendering ----

    private void renderContent() {
        StepData step = currentStep;
        if (step == null) return;

        boolean isComplete = step.getMessage() != null &&
                (step.getMessage().contains("completed") || step.getMessage().contains("success"));
        String result = isComplete ? step.getMessage() : null;
        String message = isComplete ? null : step.getMessage();

        String html = mdRenderer.render(step.getReasoning(), message, result);

        // After page loads, measure content height and adjust split divider
        contentView.getEngine().getLoadWorker().stateProperty()
                .addListener((obs, old, state) -> {
                    if (state == Worker.State.SUCCEEDED) {
                        adjustDivider();
                    }
                });
        contentView.getEngine().loadContent(html);
    }

    // ---- Dynamic divider adjustment ----

    /**
     * Re-measures the visible content and adjusts the SplitPane divider so the
     * top pane takes only as much space as its content needs (clamped to
     * {@code [MIN_RATIO, MAX_RATIO]} of the SplitPane height).
     */
    private void adjustDivider() {
        if (!toolsVisible || adjustingDivider) return;
        adjustingDivider = true;

        // Defer to let the current layout pass finish before measuring
        javafx.application.Platform.runLater(() -> {
            try {
                double contentHeight;
                if (toolDetailPane.isVisible()) {
                    contentHeight = measureToolDetailHeight();
                } else {
                    contentHeight = measureWebViewHeight();
                }
                if (contentHeight <= 0) return;

                double splitHeight = contentSplitPane.getHeight();
                if (splitHeight <= 0) return;

                double ratio = contentHeight / splitHeight;
                double dividerPos = Math.min(MAX_RATIO, Math.max(MIN_RATIO, ratio));
                contentSplitPane.setDividerPositions(dividerPos);
            } finally {
                adjustingDivider = false;
            }
        });
    }

    private double measureWebViewHeight() {
        try {
            Object h = contentView.getEngine().executeScript(
                    "document.documentElement.scrollHeight");
            if (h instanceof Number) {
                return ((Number) h).doubleValue() + 30; // 30 px for body padding
            }
        } catch (Exception e) {
            // WebView scripting may not be ready
        }
        return -1;
    }

    private double measureToolDetailHeight() {
        double h = 35; // back button + title row
        h += 18; // "Arguments" label
        h += estimateTextAreaHeight(toolArgsArea, false);
        h += 18; // "Result" label
        h += estimateTextAreaHeight(toolResultArea, true);
        if (toolErrorLabel.isVisible()) {
            h += 18; // "Error" label
            h += estimateTextAreaHeight(toolErrorArea, true);
        }
        h += 40; // padding / insets
        return h;
    }

    private double estimateTextAreaHeight(TextArea ta, boolean wrapping) {
        String text = ta.getText();
        if (text == null || text.isEmpty()) return 30;

        String[] rawLines = text.split("\n", -1);
        if (!wrapping) {
            return Math.min(rawLines.length * LINE_HEIGHT + 20, 500);
        }

        // Wrapping: estimate wrapped line count based on available width
        double areaWidth = ta.getWidth();
        if (areaWidth <= 0) areaWidth = contentSplitPane.getWidth() > 0
                ? contentSplitPane.getWidth() * 0.9 : 600;
        int charsPerLine = Math.max(1, (int) (areaWidth / MONOSPACE_CHAR_WIDTH));

        int totalLines = 0;
        for (String line : rawLines) {
            totalLines += Math.max(1, (int) Math.ceil((double) line.length() / charsPerLine));
        }
        return Math.min(totalLines * LINE_HEIGHT + 20, 500);
    }

    // ---- Clear / show ----

    private void clear() {
        showDetail(false);
        showMessageView();
        setToolsVisible(false);
        currentStep = null;
        stepTitle.setText("");
        roleBadge.setText("");
        stepTimeText.setText("");
        stepDurationText.setText("");
        contentView.getEngine().loadContent("");
        toolListView.getItems().clear();
        headerSeparator.setVisible(false);
        headerSeparator.setManaged(false);
    }

    private void showDetail(boolean show) {
        stepHeader.setVisible(show);
        stepHeader.setManaged(show);
        contentSplitPane.setVisible(show);
        contentSplitPane.setManaged(show);
        emptyState.setVisible(!show);
        emptyState.setManaged(!show);
    }

    // ---- Formatting helpers ----

    private static String toPrettyJson(Object obj) {
        try {
            return JSON_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    private static String tryFormatJson(String text) {
        if (text == null || text.isEmpty()) return text;
        try {
            Object parsed = JSON_MAPPER.readValue(text, Object.class);
            return JSON_MAPPER.writeValueAsString(parsed);
        } catch (Exception e) {
            return text;
        }
    }

    private String getRoleLabel(StepData s) {
        if (s.getToolCalls() != null && s.getToolCalls().stream().anyMatch(tc -> tc.getSubagent() != null)) {
            return "SUBAGENT";
        }
        return "agent".equals(s.getType()) ? "AGENT" : "USER";
    }

    private String getRoleType(StepData s) {
        if (s.getToolCalls() != null && s.getToolCalls().stream().anyMatch(tc -> tc.getSubagent() != null)) {
            return "subagent";
        }
        return s.getType();
    }

    private static String formatNum(int n) {
        if (n >= 1000000) return String.format("%,d", n);
        if (n >= 1000) return String.format("%,d", n);
        return String.valueOf(n);
    }

    private static String formatArgs(java.util.Map<String, Object> args) {
        if (args == null || args.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (var entry : args.entrySet()) {
            if (!sb.isEmpty()) sb.append(", ");
            Object value = entry.getValue();
            String valStr = value instanceof String ? "\"" + value + "\"" : String.valueOf(value);
            sb.append(entry.getKey()).append(": ").append(valStr);
        }
        return sb.toString();
    }
}
