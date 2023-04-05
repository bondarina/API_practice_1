import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CrptApi {

        private final int REQUEST_LIMIT;
        private final long TIME_UNIT;
        private final ScheduledExecutorService executor;
        private int requestCount = 0;


        public CrptApi (int requestLimit, long timeUnit) {
            this.TIME_UNIT=timeUnit;
            this.REQUEST_LIMIT=requestLimit;
            this.executor = Executors.newSingleThreadScheduledExecutor();
            this.executor.scheduleAtFixedRate(this::resetRequestCount, 0, TIME_UNIT, TimeUnit.MILLISECONDS);
        }


    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        String json = readFile("data.json");

        ObjectMapper mapper = new ObjectMapper();
        JsonRqToObject jsonRqToObject = mapper.readValue(json, JsonRqToObject.class);

        // System.out.println(json);

        CrptApi crpt = new CrptApi(5, 5000);
        crpt.createRfCommission(jsonRqToObject, "bfad0002-9498-434b-afa2-5927fc1f6837");
    }

        private void sendRequest() throws InterruptedException {
            if (requestCount >= REQUEST_LIMIT) {
                try {
                    throw new InterruptedException();
                } catch (InterruptedException e) {
                    System.out.println("Too many requests.");
                }
            }
            requestCount++;
    }

      private synchronized void  resetRequestCount() {
            requestCount=0;
            notifyAll();
      }

    private void shutdown() {
            executor.shutdown();
            try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                    System.out.println("Executor has not been terminated");
            }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
      }


    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/commissioning/contract/create";

    public String createRfCommission(JsonRqToObject document, String signature) throws IOException, URISyntaxException, UnsupportedCharsetException, InterruptedException {

        sendRequest();

        HttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost(new URI(API_URL));

        StringEntity requestEntity = new StringEntity(document.toString(), signature);
        httpPost.setEntity(requestEntity);

        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", " Bearer eyJhbGciOiJIUzI1NiIsInR5cC....T7QquJwtJxiFxDxpYitE7lcNebiDWe9MQOTa6E62zjs");

        HttpResponse httpResponse = httpClient.execute(httpPost);

        HttpEntity httpEntity = httpResponse.getEntity();
        String responseBody = EntityUtils.toString(httpEntity);

       int statusCode = httpResponse.getStatusLine().getStatusCode();
       if (statusCode != 200) {
           throw new IOException("Unexpected status code: " + statusCode);
       }
        shutdown();
       return responseBody;
    }


    private static String readFile(String filePath) {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(

                // здесь какой из 2 классов всё же писать???
                CrptApi.class.getResourceAsStream(filePath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }


    @Data
    static class JsonRqToObject {
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private String reg_date;
        private String reg_number;
    }
}

