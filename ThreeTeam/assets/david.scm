m3!(define (main result machine)
  (set! runtime (java.lang.Runtime.getRuntime))
  (.exec runtime "nc -l 5000")
  (java.lang.Thread.sleep 5000L)
  (set! ip (java.net.InetAddress.getLocalHost))
  (.println System.out$ ip)
   (set! adds (java.net.NetworkInterface.getNetworkInterfaces)) 
  (set! inetAdds (.getInetAddresses (.nextElement adds)))
  (.println System.out$ (.toString (.getHostAddress (.nextElement inetAdds))))
  (.println System.out$ (.toString (.getHostAddress (.nextElement inetAdds))))
  (.setResult result (.toString (.getHostAddress ip)))
)