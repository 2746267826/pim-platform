# KeyStats 本地 API 接口文档

## 概述
KeyStats 提供了一个轻量级的本地 HTTP API 服务，用于将当前软件记录的**实时键鼠统计数据**暴露给本机上的其他应用程序。该接口主要用于支持第三方桌面小部件、网页数据看板仪表盘、Stream Deck 等硬件外设进行二次开发。
安全说明：为保护用户隐私，API 服务默认绑定在 127.0.0.1，仅允许本机（Localhost）访问，局域网或外部网络无法直接获取数据。接口已默认允许跨域（CORS），支持通过浏览器前端直接拉取。
---

## 接口详情

### 1. 获取今日实时统计数据

- **接口路径**: `http://127.0.0.1:18080/api/stats/`
- **请求方式**: `GET`
- **返回格式**: `application/json`

#### 响应状态码

- `200 OK`: 请求成功并返回 JSON 数据包。
- `405 Method Not Allowed`: 使用了非 GET 的请求方法。

#### 响应字段说明 (Response Schema)

| 字段名称 | 类型 | 说明 |
| --- | --- | --- |
| date | String | 当前统计所属的日期，ISO 8601 格式（包含时区信息，如 "2026-05-09T00:00:00+08:00"）。数据会在午夜自动重置。 |
| keyPresses | Integer | 今日键盘敲击总次数。 |
| keyPressCounts | Object | 键盘按键热力图/频次明细。键名为按键名称或组合键（如 "LCtrl", "Ctrl+C", "Space"），键值为按下的总次数。 |
| leftClicks | Integer | 鼠标左键今日点击总次数。 |
| rightClicks | Integer | 鼠标右键今日点击总次数。 |
| middleClicks | Integer | 鼠标中键今日点击总次数。 |
| sideBackClicks | Integer | 鼠标侧键（后退）今日点击总次数。 |
| sideForwardClicks | Integer | 鼠标侧键（前进）今日点击总次数。 |
| mouseDistance | Float | 鼠标移动的原始像素距离。 |
| scrollDistance | Float | 鼠标滚轮滚动的原始像素距离。 |
| peakKPS | Integer | 今日峰值按键速度（KPS, Keystrokes Per Second），即今天在一秒钟内键盘敲击次数的最高记录。 |
| peakCPS | Integer | 今日峰值点击速度（CPS, Clicks Per Second），即今天在一秒钟内鼠标点击次数的最高记录。 |
| FormattedMouseDistance | String | 格式化后的鼠标移动物理距离（如 "162.2 m" 或 "1.5 km"），此数值基于软件内部设置的“每像素代表的物理距离”进行换算。 |
| FormattedScrollDistance | String | 格式化后的鼠标滚动距离（如 "7859 px" 或 "1.2 k"）。 |
| appStats | Object | 按应用程序细分的统计数据字典。键名为程序的进程名或包名（如 "chrome"）。详见下方子对象说明。 |

#### `appStats` 子对象字段说明
每个应用程序（如 `msedge`, `Code`）对应一个对象，包含该程序处于前台焦点时产生的输入统计：

| 字段名称 | 类型 | 说明 |
| --- | --- | --- |
| AppName | String | 程序的底层进程名称（如 "WindowsTerminal"、"devenv"）。 |
| DisplayName | String | 程序的友好显示名称（如 "Microsoft Visual Studio"）。 |
| KeyPresses | Integer | 在此软件中敲击键盘的总次数。 |
| LeftClicks | Integer | 在此软件中点击鼠标左键的次数。 |
| RightClicks | Integer | 在此软件中点击鼠标右键的次数。 |
| MiddleClicks | Integer | 在此软件中点击鼠标中键的次数。 |
| SideBackClicks | Integer | 在此软件中点击鼠标后退侧键的次数。 |
| SideForwardClicks | Integer | 在此软件中点击鼠标前进侧键的次数。 |
| ScrollDistance | Float | 在此软件中鼠标滚动的像素距离。 |

---

### 返回示例 (JSON)

```json
{
  "date": "2026-05-09T00:00:00+08:00",
  "keyPresses": 6421,
  "keyPressCounts": {
    "LCtrl": 160,
    "Ctrl+C": 48,
    "Space": 473,
    "Backspace": 605,
    "A": 410,
    "Enter": 155
  },
  "leftClicks": 3250,
  "rightClicks": 72,
  "middleClicks": 0,
  "sideBackClicks": 0,
  "sideForwardClicks": 0,
  "mouseDistance": 3243110.7241333937,
  "scrollDistance": 7859,
  "peakKPS": 11,
  "peakCPS": 4,
  "appStats": {
    "Code": {
      "AppName": "Code",
      "DisplayName": "Visual Studio Code",
      "KeyPresses": 30,
      "LeftClicks": 56,
      "RightClicks": 2,
      "MiddleClicks": 0,
      "SideBackClicks": 0,
      "SideForwardClicks": 0,
      "ScrollDistance": 6
    },
    "chrome": {
      "AppName": "chrome",
      "DisplayName": "Google Chrome",
      "KeyPresses": 2085,
      "LeftClicks": 378,
      "RightClicks": 8,
      "MiddleClicks": 0,
      "SideBackClicks": 0,
      "SideForwardClicks": 0,
      "ScrollDistance": 1343
    }
  },
  "FormattedMouseDistance": "162.2 m",
  "FormattedScrollDistance": "7859 px"
}

```

## 调用指南 / Tips

1. **轮询频率建议**：建议第三方客户端调用频率控制在 **1000ms（1秒）/次**。过高频率的拉取虽然不会影响统计逻辑，但会徒增无谓的 JSON 序列化性能开销。
2. **总点击量计算**：如果您需要展示“总点击量”，请将 `leftClicks` + `rightClicks` + `middleClicks` + `sideBackClicks` + `sideForwardClicks` 相加。
3. **前端调用注意**：`keyPressCounts` 中的组合键包含了 `+` 号（由于 JSON 序列化，可能会被转码为 Unicode `\u002B`，绝大多数 JSON 解析库会自动将其还原为 `+` 号）。
