package xyz.easepan.wetool.plugin.countdown;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.code4everything.wetool.plugin.support.WePluginSupporter;
import org.code4everything.wetool.plugin.support.constant.AppConsts;
import org.code4everything.wetool.plugin.support.event.EventCenter;
import org.code4everything.wetool.plugin.support.event.handler.BaseNoMessageEventHandler;
import org.code4everything.wetool.plugin.support.factory.BeanFactory;
import org.code4everything.wetool.plugin.support.util.DialogWinnable;
import org.code4everything.wetool.plugin.support.util.FxDialogs;
import org.code4everything.wetool.plugin.support.util.FxUtils;
import org.code4everything.wetool.plugin.support.util.WeUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author pantao
 * @since 2021/1/6
 */
@Slf4j
public class WetoolSupporter implements WePluginSupporter {

    private static final int MINUTE = 60;

    private static final int HOUR = 60 * MINUTE;

    private static final int DAY = 24 * HOUR;

    private Label label;

    private ComboBox<String> countdownBox = null;

    private Map<String, String> dateMap;

    private boolean initialized = false;

    @Override
    public boolean initialize() {
        countdownBox = new ComboBox<>();
        registerAction();
        FxUtils.registerAction("倒计时-ease-countdown/编辑运行时倒计时", actionEvent -> {
            TextArea textArea = new TextArea();
            textArea.setPrefWidth(300);
            textArea.setPrefHeight(600);

            if (Objects.isNull(dateMap)) {
                dateMap = new LinkedHashMap<>();
            }
            dateMap.forEach((k, v) -> {
                textArea.appendText(k);
                textArea.appendText("_");
                textArea.appendText(v);
                textArea.appendText(FileUtil.getLineSeparator());
            });
            FxDialogs.showDialog("编辑运行时倒计时", textArea, new DialogWinnable<String>() {

                @Override
                public void consumeResult(String result) {
                    String text = textArea.getText();
                    String select = countdownBox.getValue();
                    dateMap.clear();
                    StrUtil.splitTrim(text, '\n').forEach(line -> {
                        String[] tokens = line.split("_");
                        if (tokens.length != 2) {
                            return;
                        }
                        dateMap.put(tokens[0], tokens[1]);
                    });
                    countdownBox.getItems().clear();
                    countdownBox.getItems().addAll(dateMap.keySet());
                    countdown();
                    if (countdownBox.getItems().contains(select)) {
                        countdownBox.getSelectionModel().select(select);
                    }
                }
            });
        });
        Object countdownDates = WeUtils.getConfig().getConfig("countdownDates");
        if (Objects.isNull(countdownDates)) {
            return true;
        }

        dateMap = JSON.parseObject(JSON.toJSONString(countdownDates), new TypeReference<>() {}, Feature.OrderedField);
        countdown();
        return true;
    }

    private void registerAction() {
        FxUtils.registerAction("倒计时-ease-countdown/新增运行时倒计时", actionEvent -> FxDialogs.showTextInput("运行时倒计时",
                "格式：name_date", s -> {
            if (StrUtil.isBlank(s)) {
                return;
            }

            String[] tokens = s.split("_");
            if (tokens.length != 2) {
                FxDialogs.showError("格式不正确");
                return;
            }

            if (Objects.isNull(dateMap)) {
                dateMap = new LinkedHashMap<>();
            }

            dateMap.put(tokens[0], tokens[1]);

            countdown();
            countdownBox.getItems().clear();
            countdownBox.getItems().addAll(dateMap.keySet());
            countdownBox.getSelectionModel().select(tokens[0]);
        }));
    }

    private void countdown() {
        if (MapUtil.isEmpty(dateMap)) {
            return;
        }

        if (initialized) {
            return;
        }

        countdownBox.setPrefWidth(150);
        HBox.setHgrow(countdownBox, Priority.NEVER);
        countdownBox.getItems().clear();
        countdownBox.getItems().addAll(dateMap.keySet());
        countdownBox.getSelectionModel().selectFirst();

        label = new Label();
        label.setText("倒计时");
        HBox.setHgrow(label, Priority.NEVER);
        HBox.setMargin(label, new Insets(4, 0, 0, 10));

        initialized = true;
        HBox titleBar = BeanFactory.get(AppConsts.BeanKey.TITLE_BAR);
        Platform.runLater(() -> {
            titleBar.getChildren().add(countdownBox);
            titleBar.getChildren().add(label);
        });

        EventCenter.subscribeEvent(EventCenter.EVENT_SECONDS_TIMER, new BaseNoMessageEventHandler() {
            @Override
            public void handleEvent0(String s, Date date) {
                if (Objects.isNull(countdownBox) || Objects.isNull(label)) {
                    return;
                }
                Stage stage = FxUtils.getStage();
                if (!stage.isShowing() || !stage.isFocused()) {
                    return;
                }

                Platform.runLater(() -> {
                    Date cd = null;
                    try {
                        cd = parseDate(dateMap.get(countdownBox.getValue()));
                    } catch (Exception e) {
                        log.error(ExceptionUtil.stacktraceToString(e, Integer.MAX_VALUE));
                    }

                    if (Objects.isNull(cd)) {
                        label.setText("日期格式不合法");
                    } else if (cd.before(date)) {
                        label.setText("倒计时已过期");
                    } else {
                        String template = "{}天{}时{}分{}秒";
                        int seconds = (int) ((cd.getTime() - date.getTime()) / 1000);
                        int day = seconds / DAY;
                        int hours = seconds % DAY;
                        int hour = hours / HOUR;
                        int minutes = hours % HOUR;
                        int minute = minutes / MINUTE;
                        int second = minutes % MINUTE;

                        label.setText(StrUtil.format(template, day, hour, minute, second));
                    }
                });
            }
        });
    }

    public Date parseDate(String dateVariable) {
        if (StrUtil.isBlank(dateVariable)) {
            return null;
        }

        try {
            return DateUtil.parse(dateVariable);
        } catch (Exception e) {
            // ignore
        }

        StringBuilder builder = new StringBuilder();
        StringBuilder variableBuilder = new StringBuilder();
        boolean openBracket = false;
        boolean alreadyOffset = false;

        Date date = null;
        for (char c : dateVariable.toCharArray()) {
            if (c == '{') {
                // 开始解析变量
                openBracket = true;
                alreadyOffset = false;
                variableBuilder.delete(0, variableBuilder.length());
            } else if (c == '}') {
                // 解析变量并保存
                openBracket = false;
                if (Objects.isNull(date)) {
                    date = parseDateWithVariable(variableBuilder.toString());
                    variableBuilder.delete(0, variableBuilder.length());
                } else {
                    try {
                        int offset = NumberUtil.parseInt(variableBuilder.toString());
                        variableBuilder.delete(0, variableBuilder.length());
                        date = DateUtil.offsetDay(date, offset);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                if (Objects.isNull(date)) {
                    return null;
                }
                String format = StrUtil.emptyToDefault(variableBuilder.toString(), "yyyy-MM-dd");
                builder.append(DateUtil.format(date, format));
            } else if (openBracket) {
                // 记录需要解析的字符
                if (c == '+') {
                    // 日期偏移
                    date = parseDateWithVariable(variableBuilder.toString());
                    variableBuilder.delete(0, variableBuilder.length());
                } else if (c == ':') {
                    // 日期格式
                    if (Objects.isNull(date)) {
                        date = parseDateWithVariable(variableBuilder.toString());
                        variableBuilder.delete(0, variableBuilder.length());
                    } else if (alreadyOffset) {
                        variableBuilder.append(c);
                    } else {
                        int offset = NumberUtil.parseInt(variableBuilder.toString());
                        date = DateUtil.offsetDay(date, offset);
                        variableBuilder.delete(0, variableBuilder.length());
                    }
                    alreadyOffset = true;
                    if (Objects.isNull(date)) {
                        return null;
                    }
                } else {
                    // 变量
                    variableBuilder.append(c);
                }
            } else {
                // 无需解析
                builder.append(c);
            }
        }
        return DateUtil.parse(builder.toString());
    }

    private Date parseDateWithVariable(String variable) {
        DateTime now = DateUtil.date();
        switch (variable) {
            case "today":
            case "now":
                return now;
            case "tomorrow":
                return DateUtil.offsetDay(now, 1);
            case "end-week":
                return DateUtil.endOfWeek(now);
            case "end-month":
                return DateUtil.endOfMonth(now);
            case "end-year":
                return DateUtil.endOfYear(now);
            default:
                return null;
        }
    }
}
