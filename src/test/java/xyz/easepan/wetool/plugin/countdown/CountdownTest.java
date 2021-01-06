package xyz.easepan.wetool.plugin.countdown;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.code4everything.wetool.plugin.support.config.WeConfig;
import org.code4everything.wetool.plugin.test.WetoolTester;

import java.util.Map;

/**
 * @author pantao
 * @since 2021/1/6
 */
public class CountdownTest {

    public static void main(String[] args) {
        WeConfig config = WetoolTester.getConfig();
        config.setDisableKeyboardMouseListener(true);
        JSONObject configJson = JSON.parseObject(JSON.toJSONString(config));
        configJson.put("countdownDates", Map.of("倒计时", "2021-12-31", "春节", "2021-02-11"));
        config.setConfigJson(configJson);
        WetoolTester.runTest(config, args);
    }
}
