

-Dhttp.proxyHost=proxy.muc
-Dhttp.proxyPort=8080
-Dhttps.proxyHost=proxy.muc
-Dhttps.proxyPort=8080
-Dhttp.nonProxyHosts=localhost|.bmwgroup|.muc
-Dhttps.nonProxyHosts=localhost|.bmwgroup.net|.muc
-Dvertx.httpServiceFactory.httpClientOptions="{\"proxyOptions\":{\"host\":\"proxy.muc\",\"port\":8080,\"type\":\"HTTP\"}}"
