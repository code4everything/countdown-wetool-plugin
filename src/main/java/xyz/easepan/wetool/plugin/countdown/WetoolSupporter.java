package xyz.easepan.wetool.plugin.countdown;

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
import org.code4everything.wetool.plugin.support.util.FxUtils;
import org.code4everything.wetool.plugin.support.util.WeUtils;

import java.util.Date;
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


    @Override
    public boolean initialize() {
        countdown();
        return true;
    }

    private void countdown() {
        Object countdownDates = WeUtils.getConfig().getConfig("countdownDates");
        if (Objects.isNull(countdownDates)) {
            return;
        }

        Map<String, Date> dateMap = JSON.parseObject(JSON.toJSONString(countdownDates), new TypeReference<>() {});
        if (MapUtil.isEmpty(dateMap)) {
            return;
        }

        HBox titleBar = BeanFactory.get(AppConsts.BeanKey.TITLE_BAR);
        ComboBox<String> countdownBox = new ComboBox<>();
        countdownBox.setPrefWidth(150);
        HBox.setHgrow(countdownBox, Priority.NEVER);
        titleBar.getChildren().add(countdownBox);

        countdownBox.getItems().addAll(dateMap.keySet());
        countdownBox.getSelectionModel().selectFirst();

        Label label = new Label();
        label.setText("倒计时");
        HBox.setHgrow(label, Priority.NEVER);
        HBox.setMargin(label, new Insets(4, 0, 0, 10));
        titleBar.getChildren().add(label);

        EventCenter.subscribeEvent(EventCenter.EVENT_SECONDS_TIMER, new BaseNoMessageEventHandler() {
            @Override
            public void handleEvent0(String s, Date date) {
                Stage stage = FxUtils.getStage();
                if (!stage.isShowing() || !stage.isFocused()) {
                    return;
                }

                Date cd = dateMap.get(countdownBox.getValue());
                if (Objects.isNull(cd)) {
                    return;
                }

                Platform.runLater(() -> {
                    if (cd.before(date)) {
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
}
