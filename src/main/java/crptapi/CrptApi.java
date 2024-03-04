package crptapi;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class CrptApi {

    private final HttpClient httpClient;
    private final Semaphore semaphore;

    public static void main(String[] args) throws JSONException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 7);
        JSONObject jsonDocument;

        try {
            Path filePath = Path.of("file.json");
            String readString = Files.readString(filePath);
            jsonDocument = new JSONObject(readString);

            crptApi.createDocument(jsonDocument, "signature-string");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        this.semaphore = new Semaphore(requestLimit);

        Runnable runnable = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    semaphore.release(requestLimit - semaphore.availablePermits());
                    timeUnit.sleep(1);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        new Thread(runnable).start();
    }

    public void createDocument(JSONObject jsonDocument, String signature) throws InterruptedException, IOException {
        try {
            semaphore.acquire();//
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-tType", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonDocument.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw e;
        } finally {
            semaphore.release();
        }
    }

}
