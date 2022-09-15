package com.vince.hotel;

import com.alibaba.fastjson.JSON;
import com.vince.hotel.constant.HotelConstants;
import com.vince.hotel.pojo.Hotel;
import com.vince.hotel.pojo.HotelDoc;
import com.vince.hotel.service.HotelService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest
class HotelDemoApplicationTests {
    @Autowired
    private HotelService hotelService;
    @Autowired
    private RestHighLevelClient client;
//    @BeforeEach
//    void setup(){
//        this.client=new RestHighLevelClient(RestClient.builder(
//                HttpHost.create("http://192.168.200.130:9200")
//        ));
//    }
//    @AfterEach
//    void tearDown() throws IOException {
//        this.client.close();
//    }
    @Test
    public void addIndex() throws IOException {
        CreateIndexRequest request=new CreateIndexRequest("hotel");
        request.source(HotelConstants.MAPPING_TEMPLATE, XContentType.JSON);
        client.indices().create(request, RequestOptions.DEFAULT);
    }
    @Test
    public void deleteIndex() throws IOException {
        DeleteIndexRequest request=new DeleteIndexRequest("hotel");
        client.indices().delete(request,RequestOptions.DEFAULT);
    }
    @Test
    public void getIndex() throws IOException {
        GetIndexRequest request=new GetIndexRequest("hotel");
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println("exists:"+exists);
    }
    @Test
    public void add() throws IOException {
        Hotel hotel = hotelService.getById(60922L);
        HotelDoc hotelDoc = new HotelDoc(hotel);
        IndexRequest request=new IndexRequest("hotel").id("60922L").source(JSON.toJSONString(hotelDoc),XContentType.JSON);
        client.index(request,RequestOptions.DEFAULT);
    }
    @Test
    public void delete() throws IOException {
        DeleteRequest request=new DeleteRequest("hotel","60922L");
        client.delete(request,RequestOptions.DEFAULT);
    }
    @Test
    public void update() throws IOException {
        UpdateRequest request=new UpdateRequest("hotel","60922L");
        request.doc("price",2323);
        client.update(request,RequestOptions.DEFAULT);
    }
    @Test
    public void get() throws IOException {
        GetRequest request=new GetRequest("hotel","60922L");
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String source = response.getSourceAsString();
        HotelDoc hotelDoc = JSON.parseObject(source, HotelDoc.class);
        System.out.println(hotelDoc);
    }
    @Test
    public void bulk() throws IOException {
        List<Hotel> hotels = hotelService.list();
        for(Hotel hotel:hotels){
            HotelDoc hotelDoc = new HotelDoc(hotel);
            client.index(new IndexRequest("hotel").id(hotel.getId().toString()).source(JSON.toJSONString(hotelDoc),XContentType.JSON),RequestOptions.DEFAULT);
        }

    }
    @Test
    public void bulk2() throws IOException {
        List<Hotel> hotels = hotelService.list();
        BulkRequest request=new BulkRequest("hotel");
        for(Hotel hotel:hotels){
            HotelDoc hotelDoc = new HotelDoc(hotel);
            request.add(new IndexRequest("hotel").id(hotel.getId().toString()).source(JSON.toJSONString(hotelDoc),XContentType.JSON));

        }
        client.bulk(request,RequestOptions.DEFAULT);
    }

    /**
     * 查询所有
     * @throws IOException
     */
    @Test
    public void searchAll() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchAllQuery());
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }
    @Test
    public void searchPage() throws IOException {
        int currentPage=1,size=20;
        SearchRequest request = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchQuery("all", "上海")).sort("price", SortOrder.ASC).from((currentPage-1)*size).size(size);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }
    @Test
    public void highlight() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchQuery("all","如家")).highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }
    private void handleResponse(SearchResponse response) {
        SearchHits responseHits = response.getHits();
        System.out.println("数量："+responseHits.getTotalHits().value);
        SearchHit[] hits = responseHits.getHits();
        for(SearchHit hit:hits){
            HotelDoc hotelDoc = JSON.parseObject(hit.getSourceAsString(), HotelDoc.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if(!CollectionUtils.isEmpty(highlightFields)){
                HighlightField highlightName = highlightFields.get("name");
                if(highlightName!=null){
                    hotelDoc.setName(highlightName.getFragments()[0].toString());
                }
            }
            System.out.println(hotelDoc);
        }

    }
    @Test
    public void aggregation()throws IOException{
        SearchRequest request = new SearchRequest("hotel");
        request.source().size(0).aggregation(AggregationBuilders.terms("brandAgg").field("brand").size(20));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Terms brandTerms =response.getAggregations().get("brandAgg");
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        for(Terms.Bucket bucket:buckets){
            System.out.println(bucket.getKeyAsString()+":"+bucket.getDocCount());
        }
    }
}
