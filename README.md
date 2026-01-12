```
package com.github.smartdns;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class Single {
    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        String host = "smart.example.com";
        String host1 = "smart1.example.com";
        Thread.sleep(10_000);
        InetAddress[] addrs = InetAddress.getAllByName(host);
        System.out.println(host + ":" + addrs.length);
        InetAddress[] addrs1 = InetAddress.getAllByName(host1);
        System.out.println(host1 + ":" + addrs1.length);
    }
}

```
***Agent方式使用***
```
java -cp "target/classes;" -javaagent:target/smart-dns-manipulator-1.0-SNAPSHOT.jar -Ddcm.config.filename=dns-caches.properties com.github.smart
dns.Single
```
日志打印，已正常解析

<img width="830" height="76" alt="image" src="https://github.com/user-attachments/assets/30983d4f-2930-4564-aa06-45f134abcc2f" />

