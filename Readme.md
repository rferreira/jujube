# Jujube

Just enough logic to turn [Apache Http Core](https://hc.apache.org/httpcomponents-core-ga/index.html) into something suited for micro services.

## Features
* Optimized for data ingestion. You can accept large HTTP requests bodies (including multipart content) [nearly as fast as you can write to disk](https://github.com/rferreira/jujube/tree/master/jujube-benchmark).   
* Optimized for low memory and thread consumption. Targeted to operate well under heaps of only 64 MB.
* Security audit friendly. With minimal dependencies you are going to be less likely to be forced to upgrade due to a downstream dependency vulnerability. 
* H2 and TLS ON by default using self signed certificates (but you can bring your own cert if you would like).
* Efficient, inside the IO loop, static file handling with support for conditional retrieval. 

## Getting started
```java
package org.ophion.jujube;

import org.ophion.jujube.config.JujubeConfig;
import org.ophion.jujube.response.HttpResponse;

public class JujubeHelloWorld {
  public static void main(String[] args)  {
    var config = new JujubeConfig();
    config.route("/*", (req, ctx) -> {
      return new HttpResponse("Hello world");
    });
    Jujube server = new Jujube(config);
    server.startAndWait();
  }
}
```  

Like what you see here? Then consider [saying thanks to the Apache Software Foundation](https://donate.apache.org) or buying [Oleg Kalnichevski](https://github.com/ok2c) a coffee. 
