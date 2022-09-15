package com.vince.hotel.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vince.hotel.mapper.HotelMapper;
import com.vince.hotel.pojo.Hotel;
import com.vince.hotel.pojo.HotelDoc;
import com.vince.hotel.pojo.PageResult;
import com.vince.hotel.pojo.RequestParams;
import com.vince.hotel.service.HotelService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements HotelService {
    @Autowired
    private RestHighLevelClient client;

    /**
     * 按条件分页查询
     * @param requestParams
     * @return
     */
    @Override
    public PageResult search(RequestParams requestParams) {
        //获取请求参数

        Integer page = requestParams.getPage();
        Integer size = requestParams.getSize();
        String location = requestParams.getLocation();


        //查询
        SearchRequest request=new SearchRequest("hotel");
        FunctionScoreQueryBuilder functionScoreQueryBuilder = getFunctionScoreQueryBuilder(requestParams);
        request.source().query(functionScoreQueryBuilder);

        //排序
        if(!StringUtils.isEmpty(location))
        request.source().sort(SortBuilders.geoDistanceSort("location",new GeoPoint(location)).order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS));

        //分页
        request.source().from((page-1)*size).size(size);
        SearchResponse response =null;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //处理response
        SearchHits responseHits = response.getHits();
        PageResult pageResult = new PageResult();
        pageResult.setTotal(responseHits.getTotalHits().value);
        SearchHit[] hits = responseHits.getHits();
        List<HotelDoc> hotelDocs=new ArrayList<>();
        for(SearchHit hit:hits){
            HotelDoc hotelDoc = JSON.parseObject(hit.getSourceAsString(), HotelDoc.class);
            Object[] sortValues = hit.getSortValues();
            if(sortValues.length>0) hotelDoc.setDistance(sortValues[0]);
            hotelDocs.add(hotelDoc);
        }
        pageResult.setHotels(hotelDocs);
        return pageResult;
    }

    @NotNull
    private FunctionScoreQueryBuilder getFunctionScoreQueryBuilder(RequestParams requestParams) {
        String key= requestParams.getKey();
        String brand = requestParams.getBrand();
        String city = requestParams.getCity();
        String starName = requestParams.getStarName();
        Integer maxPrice = requestParams.getMaxPrice();
        Integer minPrice = requestParams.getMinPrice();
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        if(!StringUtils.isEmpty(key)) builder.must(QueryBuilders.matchQuery("all",key));
        else builder.must(QueryBuilders.matchAllQuery());
        if(!StringUtils.isEmpty(brand)) builder.filter(QueryBuilders.termQuery("brand",brand));
        if(!StringUtils.isEmpty(city)) builder.filter(QueryBuilders.termQuery("city",city));
        if(!StringUtils.isEmpty(starName)) builder.filter(QueryBuilders.termQuery("starName",starName));
        if(!StringUtils.isEmpty(maxPrice)) builder.filter(QueryBuilders.rangeQuery("price").lte(maxPrice));
        if(!StringUtils.isEmpty(minPrice)) builder.filter(QueryBuilders.rangeQuery("price").gte(minPrice));
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(builder,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                QueryBuilders.termQuery("isAD",true),
                                ScoreFunctionBuilders.weightFactorFunction(10))
        });
        return functionScoreQueryBuilder;
    }

    @Override
    public Map<String, List<String>> filters(RequestParams requestParams) {
        SearchRequest request = new SearchRequest("hotel");
        FunctionScoreQueryBuilder functionScoreQueryBuilder = getFunctionScoreQueryBuilder(requestParams);
        request.source().query(functionScoreQueryBuilder);
        //聚合
        request.source().size(0).aggregation(AggregationBuilders.terms("brandAgg").field("brand").size(100));
        request.source().size(0).aggregation(AggregationBuilders.terms("cityAgg").field("city").size(100));
        request.source().size(0).aggregation(AggregationBuilders.terms("starNameAgg").field("starName").size(100));
        //查询
        SearchResponse response =null;
        try {
            response= client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<String, List<String>> ans=new HashMap<>();

        ans.put("brand",getList(response,"brandAgg"));
        ans.put("city",getList(response,"cityAgg"));
        ans.put("starName",getList(response,"starNameAgg"));
        return ans;
    }

    @Override
    public List<String> getSuggestion(String prefix) {
        SearchRequest request=new SearchRequest("hotel");
        request.source().suggest(new SuggestBuilder().addSuggestion("keySuggestions",SuggestBuilders.completionSuggestion("suggestion").prefix(prefix).skipDuplicates(true).size(10)));
        SearchResponse response=null;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CompletionSuggestion keySuggestions=response.getSuggest().getSuggestion("keySuggestions");
        List<CompletionSuggestion.Entry.Option> options = keySuggestions.getOptions();
        List<String> ans=new ArrayList<>();
        for (CompletionSuggestion.Entry.Option option : options) {
            ans.add(option.getText().toString());
        }
        log.info(ans.toString());
        return ans;
    }

    @Override
    public void removeEsById(Long id) {
        DeleteRequest request=new DeleteRequest("hotel",id.toString());
        try {
            client.delete(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveEsById(Long id) {
        IndexRequest request=new IndexRequest("hotel");
        Hotel hotel = this.getById(id);
        log.info("查到要保存的：{}",hotel.toString());
        request.id(id.toString()).source(JSON.toJSONString(hotel), XContentType.JSON);
        try {
            client.index(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private List<String> getList(SearchResponse response,String aggregationName) {
        Terms terms = response.getAggregations().get(aggregationName);
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        List<String> list=new ArrayList<>();
        for(Terms.Bucket bucket:buckets){
            list.add(bucket.getKeyAsString());
        }
        return list;
    }



}
