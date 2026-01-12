# Smart DNS Manipulator

一个基于 JVM 的智能 DNS 缓存管理工具，支持动态 DNS 映射、健康检查和故障转移功能。

## 功能特性

- **DNS 缓存操作**：直接操作 JVM 层面的 DNS 缓存
- **智能故障转移**：自动检测 IP 健康状态并切换
- **配置驱动**：支持外部配置文件管理 DNS 规则
- **Agent 支持**：可作为 JVM Agent 注入运行
- **健康检查**：定期验证目标 IP 端口可达性

## 快速开始

### 1. 项目结构
```
src/
├── main/
│   ├── java/com/github/smartdns/
│   │   ├── DnsCacheManipulator.java    # DNS 缓存操作核心类
│   │   ├── SmartDnsManager.java       # 智能 DNS 管理器
│   │   └── agent/SmartDnsAgent.java   # Agent 入口
│   └── resources/
│       └── dns-cache.properties       # DNS 配置文件
└── test/
```

### 2. 配置文件格式

`dns-cache.properties` 配置示例：
```
domain_name=ip1,ip2,ip3:port
smart.example.com=58.251.120.143,61.241.45.156,192.168.1.1:443
baidu.com=192.168.1.1,192.168.1.12:443
```

格式说明：
- `domain_name`：目标域名
- `ip1,ip2,ip3`：候选 IP 地址列表（逗号分隔）
- `:port`：可选端口（用于健康检查，默认 443）

### 3. 使用方式

#### 方式一：编程方式
```java
SmartDnsManager.loadDnsCacheConfig();
// 或指定配置文件
SmartDnsManager.loadDnsCacheConfig('custom-config.properties');
```

#### 方式二：JVM Agent 方式
```bash
java -javaagent:smart-dns-manipulator.jar -Ddcm.config.filename=dns-cache.properties -jar your-app.jar
```

#### 方式三：直接 API 调用
```java
// 手动设置 DNS 映射
DnsCacheManipulator.setDnsCache('example.com', '192.168.1.1', '192.168.1.2');

// 移除 DNS 缓存
DnsCacheManipulator.removeDnsCache('example.com');

// 清空所有 DNS 缓存
DnsCacheManipulator.clearDnsCache();
```

### 4. 配置选项

- `dcm.config.filename`：指定配置文件名（默认 `dns-cache.properties`）
- 默认健康检查间隔：100ms
- 默认缓存过期时间：1年

## 核心组件

### DnsCacheManipulator
- **功能**：直接操作 JVM 的 DNS 缓存
- **支持**：Java 8-21 版本
- **原理**：使用反射修改 [InetAddress](file:///D:\git_code\smart-dns-manipulator\src\main\java\com\github\smartdns\DnsCacheManipulator.java#L16-L16) 内部缓存
- **方法**：
  - [setDnsCache()](file:///D:\git_code\smart-dns-manipulator\src\main\java\com\github\smartdns\DnsCacheManipulator.java#L23-L44)：设置 DNS 缓存
  - [removeDnsCache()](file:///D:\git_code\smart-dns-manipulator\src\main\java\com\github\smartdns\DnsCacheManipulator.java#L49-L61)：移除 DNS 缓存
  - [clearDnsCache()](file:///D:\git_code\smart-dns-manipulator\src\main\java\com\github\smartdns\DnsCacheManipulator.java#L66-L75)：清空 DNS 缓存

### SmartDnsManager
- **功能**：智能 DNS 管理和健康检查
- **特性**：
  - 定时健康检查（Socket 连接测试）
  - 自动故障转移
  - 配置文件热加载
- **工作流程**：
  1. 加载配置文件
  2. 定期检查 IP 健康状态
  3. 动态更新 DNS 缓存

### SmartDnsAgent
- **功能**：JVM Agent 入口
- **支持**：
  - `premain()`：JVM 启动时注入
  - `agentmain()`：运行时动态附加

## 工作原理

### DNS 缓存操作
1. 通过反射获取 [InetAddress](file:///D:\git_code\smart-dns-manipulator\src\main\java\com\github\smartdns\DnsCacheManipulator.java#L16-L16) 类内部缓存对象
2. 构造 [InetAddress](file:///D:\git_code\smart-dns-manipulator\src\main\java\com\github\smartdns\DnsCacheManipulator.java#L16-L16) 数组
3. 创建缓存条目并插入缓存表

### 健康检查机制
1. 使用 [Socket](file:///D:\git_code\smart-dns-manipulator\src\main\java\com\github\smartdns\SmartDnsManager.java#L17-L17) 连接到指定 IP 和端口
2. 1秒超时设置
3. 定期检查所有配置的 IP
4. 更新 DNS 缓存中的可用 IP 列表

## 使用场景

- **服务治理**：在微服务架构中实现智能 DNS 切换
- **故障恢复**：自动检测服务实例健康状态并切换
- **测试环境**：模拟不同 DNS 解析结果
- **负载均衡**：配合外部负载均衡器实现多活部署

## 注意事项

- **安全性**：使用反射操作内部 API，可能存在安全限制
- **兼容性**：不同 JDK 版本的内部实现可能不同
- **性能**：频繁的健康检查可能影响性能
- **权限**：需要适当的安全权限才能操作 DNS 缓存

## 测试

项目包含完整的单元测试，覆盖：
- DNS 缓存操作测试
- 故障转移测试
- 配置加载测试

## 许可证

[MIT License](https://opensource.org/licenses/MIT)"
