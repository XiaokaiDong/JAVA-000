package org.tt.Week_02;

//import org.apache.http.client.HttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class Application {
    public static void main(String[] args) {

        HttpGet httpGet = new HttpGet(args[0]);
        try(CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(httpGet)){
            if(response.getStatusLine().getStatusCode() == 200){
                HttpEntity entity = response.getEntity();
                String res = EntityUtils.toString(entity, "utf-8");
                System.out.println(res);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
