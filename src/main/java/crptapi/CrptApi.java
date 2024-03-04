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
            // Чтение содержимого файла "file.json" и преобразование его в JSONObject
            Path filePath = Path.of("file.json");
            String readString = Files.readString(filePath);
            jsonDocument = new JSONObject(readString);

            // Вызов метода createDocument для создания документа с переданными параметрами
            crptApi.createDocument(jsonDocument, "signature-string");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        
        // Создание экземпляра HttpClient с настройками
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        
        // Создание семафора с ограничением на количество одновременных запросов
        this.semaphore = new Semaphore(requestLimit);
        
        // Создание и запуск отдельного потока для управления семафором
        Runnable runnable = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Освобождение семафора для дополнительных запросов и задержка на 1 единицу времени
                    semaphore.release(requestLimit - semaphore.availablePermits());
                    timeUnit.sleep(2);
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
            
            // Отправка запроса и получение ответа
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Вывод информации о статусе ответа
            System.out.println("Response status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw e;
        } finally {
            
            // Освобождение семафора после завершения запроса
            semaphore.release();
        }
    }

}
