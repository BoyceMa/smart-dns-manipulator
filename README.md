```
package com.github.smartdns;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class Single {
    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        String host = "smart.example.com";
        String host1 = "smart1.example.com";
        String host2 = "baidu.com";
        Thread.sleep(10_000);
        InetAddress[] addrs = InetAddress.getAllByName(host);
        System.out.println(host + ":" + addrs.length);
        InetAddress[] addrs1 = InetAddress.getAllByName(host1);
        System.out.println(host1 + ":" + addrs1.length);
        InetAddress[] addrs2 = InetAddress.getAllByName(host2);
        System.out.println(host2 + ":" + addrs2.length);
    }
}

```
***Agent方式使用***
```
java -cp "target/classes;" -javaagent:target/smart-dns-manipulator-1.0-SNAPSHOT.jar -Ddcm.config.filename=dns-caches.properties com.github.smart
dns.Single
```
日志打印，已正常解析
<img width="967" height="86" alt="image" src="https://github.com/user-attachments/assets/da5241f8-8b67-49cd-bfbc-a0e6fd02aa7a" />
