package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
public class HotelDocumentTest {
    private RestHighLevelClient client;

    @Autowired
    private IHotelService hotelService;

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.21.129:9200")
        ));
    }

    @Test
    void testAddDocument() throws IOException {
//        根据id查询酒店数据
        Hotel hotel = hotelService.getById(36934L);
//        转换为文档类型
        HotelDoc hotelDoc = new HotelDoc(hotel);

//        1.准备Request
        IndexRequest request = new IndexRequest("hotel").id(hotel.getId().toString());
//        2.准备Json文档
        request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
//        3.发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetDocumentById() throws IOException {
//        1.准备request对象
        GetRequest request = new GetRequest("hotel", "36934");
//        2.发送请求，得到相应
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
//        3.解析相应结果
        String json = response.getSourceAsString();

        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }

    @Test
    void testUpdateDocument() throws IOException {
//        1.准备request
        UpdateRequest request = new UpdateRequest("hotel", "36934");
//        2.准备请求参数
        request.doc(
                "price", "952",
                "starName", "四钻"
        );
//        3.发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteDocument() throws IOException {
        client.delete(new DeleteRequest("hotel", "36934"), RequestOptions.DEFAULT);
    }

    @Test
    void testBulkRequest() throws IOException {
//        批量查询酒店数据
        List<Hotel> hotelList = hotelService.list();


//        1.创建request
        BulkRequest request = new BulkRequest();
//        2.准备参数，添加多个新增的request
        //        转换为文档类型的hotelDoc
        for (Hotel hotel : hotelList) {
//            转换为文档类型HOTELDOC
            HotelDoc hotelDoc = new HotelDoc(hotel);
//            创建新增文档的Request对象
            request.add(new IndexRequest("hotel")
                    .id(hotelDoc.getId().toString())
                    .source(JSON.toJSONString(hotelDoc), XContentType.JSON));
        }
//        3.发送请求
        client.bulk(request, RequestOptions.DEFAULT);
    }
}
