<font color='red'><b><i>NEW</i></b></font> [Radio Mesh](http://radiomesh.org) is an example of RxTx hot-deploy with rupy.

Now I have added so that you can hot-deploy to your pi with rupy on projects that use a COM port integration to increase development speed of all your Java hardware projects.

Install RXTX on your pi:

sudo apt-get install librxtx-java

Example code:

```
public class Router extends Daemon.Com {
   static Daemon daemon;
   static Router router;
   
   private static InputStream in;
   private static OutputStream out;
   
   public Router(String tty) {
      try {
         System.setProperty("gnu.io.rxtx.SerialPorts", tty);
         CommPortIdentifier id = CommPortIdentifier.getPortIdentifier(tty);

         if(id.isCurrentlyOwned()) {
            System.out.println("Error: Port is currently in use.");
         } else {
            CommPort port = id.open("COM", 1000);

            if(port instanceof SerialPort) {
               SerialPort serial = (SerialPort) port;
               serial.setSerialPortParams(9600, 
                     SerialPort.DATABITS_8, 
                     SerialPort.STOPBITS_1, 
                     SerialPort.PARITY_NONE);

               in = serial.getInputStream();
               out = serial.getOutputStream();
               
               Thread thread = new Thread(new Reader(in));
               thread.start();
            }
            else {
               System.out.println("Error: Only serial ports are handled by this example.");
            }
         }
      }
      catch(Exception e) {
         e.printStackTrace();
      }
   }
   
   public static class Send extends Service {
      Receiver rec;
      
      public String path() { return "/"; }

      public void create(Daemon daemon) {
         rec = new Receiver(daemon);
         daemon.com().set(rec);
      }
      
      public void filter(Event event) throws Event, Exception {
         System.out.println(event.query().header());
         
         if(event.query().method() == Query.POST) {
            event.query().parse();
            String message = event.query().string("message");
            event.daemon().com().write(message.getBytes());
         }
      }
   }

   public static class Receiver implements Daemon.Listen {
      StringBuilder builder = new StringBuilder();
      
      Daemon daemon;
      
      public Receiver(Daemon daemon) {
         this.daemon = daemon;
      }
      
      public void read(byte[] data, int length) throws Exception {
         String in = new String(data, 0, length);
         
         System.out.println(in);
      }
   }
   
   public void write(byte[] data) throws IOException {
      out.write(data);
   }
   
   public static class Reader implements Runnable {
      InputStream in;

      public Reader(InputStream in) {
         this.in = in;
      }

      public void run() {
         byte[] data = new byte[1024];
         int length = -1;
         try {
            while((length = this.in.read(data)) > -1) {
               try {
                  router.listen.read(data, length);
               }
               catch(Exception e) {
                  e.printStackTrace();
               }
            }
         } catch(Exception e) {
            e.printStackTrace();
         }
      }
   }
   
   public static void main(String[] args) {
      daemon = Daemon.init(args);
      router = new Router("/dev/ttyAMA0");
      daemon.set(router);
      daemon.start();
   }
}
```

Then you need to add the hot-deployed jar to the classpath and run Router instead of Daemon:

java -Djava.library.path=/usr/lib/jni -classpath /usr/share/java/RXTXcomm.jar:http.jar:**app/your-app.jar Router** -pass XXXXXX