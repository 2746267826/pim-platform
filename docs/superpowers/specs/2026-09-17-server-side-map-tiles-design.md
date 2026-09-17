# 服务端地图瓦片设计

## 目标

由 PIM API 负责拉取并缓存 OpenStreetMap PNG 瓦片，历史位置页只访问同源 API，避免依赖生产 nginx 的 `/tiles` 反代。

## 方案

`TileService` 接收严格校验后的 `TileCoordinate`，将请求映射为配置中的 OSM 上游基址（默认 `https://tile.openstreetmap.org`），通过命名 `HttpClient` 发出带描述性 User-Agent 的请求。默认 `HttpClientHandler` 保留系统 `HTTP_PROXY`/`HTTPS_PROXY` 行为；上游基址、超时、缓存目录和 TTL 都由 `Tiles` 配置覆盖。

缓存文件按 `z/x/y.png` 保存。服务先检查未过期文件，再以同坐标的异步锁合并并发请求；下载内容必须是 2xx 且 PNG 签名或图片 Content-Type，写入随机临时文件后原子移动。坏响应不落盘并返回 502。成功响应带 `image/png` 与长期浏览器缓存头，并通过响应头标示服务端缓存命中，便于运维验证。

端点为 `GET /api/v1/tiles/{z:int}/{x:int}/{y:int}.png`，允许匿名访问（瓦片本身不含用户数据），非法坐标返回 400。前端 Leaflet 使用 `BASE_URL` 下的 `api/v1/tiles` 相对路径；Vite 移除 `/tiles` 外部代理。

## 测试策略

纯坐标验证覆盖边界和非数字输入；服务测试使用自定义 `HttpMessageHandler` 与临时目录验证命中缓存、坏响应不缓存、并发单次上游请求和 PNG 输出；最小 DI 解析测试确保端点服务已注册。使用注入的 `TimeProvider`，不依赖真实互联网或固定墙上时间。
