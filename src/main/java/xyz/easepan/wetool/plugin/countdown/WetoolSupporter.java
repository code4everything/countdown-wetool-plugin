package xyz.easepan.wetool.plugin.countdown;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.code4everything.wetool.plugin.support.WePluginSupporter;
import org.code4everything.wetool.plugin.support.constant.AppConsts;
import org.code4everything.wetool.plugin.support.event.EventCenter;
import org.code4everything.wetool.plugin.support.event.handler.BaseNoMessageEventHandler;
import org.code4everything.wetool.plugin.support.factory.BeanFactory;
import org.code4everything.wetool.plugin.support.util.FxDialogs;
import org.code4everything.wetool.plugin.support.util.FxUtils;
import org.code4everything.wetool.plugin.support.util.WeUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author pantao
 * @since 2021/1/6
 */
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
        Object countdownDates = WeUtils.getConfig().getConfig("countdownDates");
        if (Objects.isNull(countdownDates)) {
            return true;
        }

        dateMap = JSON.parseObject(JSON.toJSONString(countdownDates), new TypeReference<>() {});
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
                dateMap = new HashMap<>(8);
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

        Platform.runLater(() -> {
            countdownBox.setPrefWidth(150);
            HBox.setHgrow(countdownBox, Priority.NEVER);
            countdownBox.getItems().addAll(dateMap.keySet());
            countdownBox.getSelectionModel().selectFirst();

            label = new Label();
            label.setText("倒计时");
            HBox.setHgrow(label, Priority.NEVER);
            HBox.setMargin(label, new Insets(4, 0, 0, 10));

            initialized = true;
            HBox titleBar = BeanFactory.get(AppConsts.BeanKey.TITLE_BAR);
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
                    Date cd = parseDate(dateMap.get(countdownBox.getValue()));
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

        return null;
    }

    private Date parse(String variable) {
        DateTime now = DateUtil.date();
        switch (variable) {
            case "today":
                return now;
            case "tomorrow":
                return DateUtil.offsetDay(now, 1);
            default:
                return null;
        }
    }
}
