## WeTool倒计时插件

在`wetool`主配文件`we-config.json`中加入以下配置即可：

```json
{
    /*其他配置属性*/
    /*................*/
    "countdownDates": {
        "倒计时1": "2021-02-11",
        "倒计时2": "2021-12-31"
    }
}
```

支持动态时间，格式：{variable:format}，变量支持：today, now, tomorrow, end-week, end-month, end-year，格式化如：yyyy-MM-dd, HH:mm:ss等

举例，默认格式化：yyyy-MM-dd，年度倒计时：`{end-year} 23:59:59` 或者 `{end-year:yyyy-MM-dd} 23:59:59` 或者 `{end-year:yyyy-MM-dd HH:mm:ss.SSS}`
