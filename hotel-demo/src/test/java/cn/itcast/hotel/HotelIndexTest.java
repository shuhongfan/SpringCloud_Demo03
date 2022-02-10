package cn.itcast.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

import static cn.itcast.hotel.constants.HotelConstants.MAPPING_TEMPLATE;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class HotelIndexTest {
    private RestHighLevelClient client;

    @Test
    void testInit() {
        System.out.println(client);
    }

    @Test
    void createHotelIndex() throws IOException {
//        1.创建Request对象
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("hotel");
//        2.准备请求的参数：DSL语句
        createIndexRequest.source(MAPPING_TEMPLATE, XContentType.JSON);
//        3.发送请求
        client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteHotelIndex() throws IOException {
        client.indices().delete(new DeleteIndexRequest("hotel"), RequestOptions.DEFAULT);
    }

    @Test
    void testExistsHotelIndex() throws IOException {
        boolean exists = client.indices().exists(new GetIndexRequest("hotel"), RequestOptions.DEFAULT);
        System.out.println(exists ? "索引库已存在" : "索引库已删除");
    }

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.21.129:9200")
        ));
    }


    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}
